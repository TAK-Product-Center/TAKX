package gov.takx.url;

import android.util.Base64;
import gov.tak.api.icons.IIconSetManager;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.url.Schemes;
import gov.takx.commons.file.FilePath;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import gov.takx.test.AIntegrationTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test our custom URL protocol handlers.
 * @since 5.6
 */
public class UrlProtocolHandlerTest extends AIntegrationTest
{
    @Test
    void test_base64() throws IOException
    {
        // Generate a 2^16 byte sequence (seeded random ensures consistency)
        var r = new Random(0);
        byte[] bytes = new byte[65536];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) r.nextInt();

        // Create two forms of expected Base64 inputs, one for java.util and android.util
        var encoded1 = java.util.Base64.getEncoder().encodeToString(bytes);
        var encoded2 = Base64.encodeToString(bytes, Base64.NO_WRAP | Base64.URL_SAFE);
        var url1 = new URL(Schemes.BASE64 + "://" + encoded1);
        var url2 = new URL(Schemes.BASE64 + "://" + encoded2);

        // The java.util.Base64 class for some inputs will include '+' and '/' characters.
        // For our URL handler we use the android.util.Base64 class with URL_SAFE as a flag.
        // This does not allow those characters to be present. We want them to be here for the
        // first encoded message though so that we prove our URL handler properly adapts this case
        // since some plugin developers may not be aware of this fact.
        assertThat(encoded1)
                .withFailMessage("java.util.Base64 does not contain '+' or '/' characters")
                .contains("+", "/");
        assertThat(encoded2)
                .withFailMessage("android.util.Base64 contains '+' or '/' characters")
                .doesNotContain("+", "/");

        // Both URLs when read from should yield the exact same byte sequence.
        validateUrlContents(url1, bytes);
        validateUrlContents(url2, bytes);
    }

    @Test
    void test_iconset() throws IOException
    {
        final var iconSet = getBean((IIconSetManager.class)).getIconSetByName("Default");
        assertThat(iconSet).isNotNull();

        final var icon = iconSet.getIcon("ant.png");
        assertThat(icon).isNotNull();

        validateUrlContents(icon.toUri().toURL(), icon.getImageBytes());
    }

    /** @since 5.7.1 */
    @SuppressWarnings("DataFlowIssue")
    @Test
    void test_ui_resource() throws IOException
    {
        final var uiResourceUrl = UIResources.getUrl(ResourceDirectory.PLATFORM_ICONS, "open.png");
        final var nativeResourceUrl = UiResourceUrlConnection.getNativeUrl(ResourceDirectory.PLATFORM_ICONS, "open.png", false);

        try (var is = nativeResourceUrl.openStream())
        {
            validateUrlContents(uiResourceUrl, is.readAllBytes());
        }
    }

    @Test
    void test_vfs() throws IOException
    {
        validateVfsUrl(VFSManagerScope.Literal.GLOBAL_INSTANCE);
        validateVfsUrl(VFSManagerScope.Literal.PROJECT_INSTANCE);
    }

    /**
     * Create a file in the specified VFS scope, write some data to it, and then verify that
     * the URL handler properly reads that data back.
     */
    private void validateVfsUrl(VFSManagerScope scope) throws IOException
    {
        final var vfsManager = getBean(IVirtualFileSystemManager.class, scope);
        final boolean global = vfsManager.getVfsScope() == VFSScope.Global;

        final String vfsContent = scope + " vfs test content";
        final var vfs = vfsManager.getVirtualFileSystem("test");
        final var testFile = vfs.createFile(new FilePath("test_file.txt")).getFile();
        Files.writeString(testFile.toPath(), vfsContent);

        final var vfsPrefix = Schemes.VFS_PREFIX + (global ? VfsUrlConnection.GLOBAL : "");
        final var vfsUrl = new URL(vfsPrefix + "test/" + testFile.getName());
        validateUrlContents(vfsUrl, vfsContent.getBytes());
    }

    /**
     * Compare the bytes read from the URL against the given expected bytes.
     */
    private static void validateUrlContents(URL url, byte[] expected) throws IOException
    {
        try (final var is = url.openStream())
        {
            assertThat(is.readAllBytes()).isEqualTo(expected);
        }
    }
}
