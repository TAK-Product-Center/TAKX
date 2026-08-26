package gov.takx.url;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * URL Connection for the {@code vfs} protocol/scheme.
 *
 * @since 5.1
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class VfsUrlConnection extends URLConnection
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Prefix indicating a path in Global VFS vs. Project VFS, no prefix indicates project.
     */
    static final String GLOBAL = "global:";

    @Inject
    @VFSManagerScope(VFSScope.Global)
    private IVirtualFileSystemManager globalVfsManager;

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVfsManager;

    private File vfsLocation;

    /**
     * Constructs a URL connection to the specified URL.
     *
     * @param url the specified URL
     */
    public VfsUrlConnection(URL url)
    {
        super(url);
    }

    @Override
    public void connect() throws IOException
    {
        vfsLocation = getVfsLocation();
        connected = true;
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        connect();
        return new FileInputStream(vfsLocation);
    }

    /**
     * @return Reference to the actual file in VFS, which must exist or a {@link FileNotFoundException} is thrown
     * @throws FileNotFoundException If the file indicated by the URL path does not exist in the selected VFS
     * @see VfsUrlStreamHandler#parseURL(URL, String, int, int)
     */
    @Nonnull
    private File getVfsLocation() throws FileNotFoundException
    {
        // Pick our VFS manager based on the authority of the URL
        final var vfsManager = GLOBAL.equals(url.getAuthority()) ? globalVfsManager : projectVfsManager;

        final File file = new File(URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8));
        final File vfsDir = vfsManager.getVirtualFileSystem(file.getParent()).getRootDir();
        final File fileInVfs = new File(vfsDir, file.getName());

        logger.debug("URL '{}' => {}", url, fileInVfs.getAbsolutePath());

        if (!fileInVfs.exists()) throw new FileNotFoundException(fileInVfs.toString());

        return fileInVfs;
    }
}
