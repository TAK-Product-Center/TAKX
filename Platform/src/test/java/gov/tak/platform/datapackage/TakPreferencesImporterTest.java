package gov.tak.platform.datapackage;

import gov.tak.core.comms.takserver.DataPackageManager;
import gov.takx.api.event.ShuttingDown;
import gov.takx.platform.preferences.PreferencesNode;
import gov.takx.test.AContainerTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.Zips;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import static gov.takx.platform.preferences.PreferenceEntry.TAK_PREFERENCES_NODE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.3
 */
public class TakPreferencesImporterTest extends AContainerTest
{
    private static final String MANIFEST_SRC = """
            <MissionPackageManifest version="2">
               <Configuration>
                  <Parameter name="uid" value="a647112f-ce05-4312-a2b4-208d8d8a5fa8"/>
                  <Parameter name="name" value="pref_test.zip"/>
               </Configuration>
               <Contents>
                  <Content ignore="false" zipEntry="access.pref"/>
               </Contents>
            </MissionPackageManifest>
            """;

    private static final String PREFERENCES_SRC = """
            <?xml version="1.0" standalone="yes"?>
            <preferences>
            <preference version="1" name="com.atakmap.app.civ_preferences">
            <entry key="key1" class="class java.lang.String">V1</entry>
            <entry key="key2" class="class java.lang.String">V2</entry>
            <entry key="key3" class="class java.lang.String">V3</entry>
            </preference>
            </preferences>
            """;

    private static final String CIV_PREFERENCES = "com.atakmap.app.civ_preferences";

    private static final Map<String, String> EXPECTED_VALUES = Map.of(
            "key1", "V1",
            "key2", "V2",
            "key3", "V3");

    private final PreferencesNode takPreferencesNode = TAK_PREFERENCES_NODE.node();

    @BeforeEach
    public void beforeEach()
    {
        clearPreferencesNode();
    }

    @AfterEach
    public void afterEach()
    {
        clearPreferencesNode();
    }

    private void clearPreferencesNode()
    {
        takPreferencesNode.clear();
    }

    @AfterAll
    public void afterAll()
    {
        fireEventToServiceInstance(DataPackageManager.class, new ShuttingDown(null));
    }

    @Test
    void preferencesImporter_withAppPreferences_importsEntries() throws IOException
    {
        final var preferences = new PreferencesReader().parsePreferences(PREFERENCES_SRC);
        new TakPreferencesImporter().importPreferences(preferences);

        assertExpectedKeys();
    }

    @Test
    void preferencesImporter_withEachAppPreferencesName_importsEntries() throws IOException
    {
        for (String preferenceName : TakPreferencesImporter.PREFERENCE_NAMES)
        {
            assertThat(takPreferencesNode.keys()).isEmpty();

            // Replace the initial name with the one to test
            final String source = PREFERENCES_SRC.replace(CIV_PREFERENCES, preferenceName);

            final var preferences = new PreferencesReader().parsePreferences(source);
            new TakPreferencesImporter().importPreferences(preferences);

            assertExpectedKeys();

            clearPreferencesNode();
        }
    }

    @Test
    void preferencesImporter_withNoAppPreferences_importsNothing() throws IOException
    {
        final String source = PREFERENCES_SRC.replace(CIV_PREFERENCES, "no_app_preferences");
        final var preferences = new PreferencesReader().parsePreferences(source);
        new TakPreferencesImporter().importPreferences(preferences);

        assertThat(takPreferencesNode.keys()).isEmpty();
    }

    @Test
    void dataPackage_withPreferences_isImported() throws IOException
    {
        final File packageFile = Files.createTempFile("package_", ".zip").toFile();
        packageFile.deleteOnExit();

        final Zips zip = Zips.create().destination(packageFile);
        zip.addEntry(new ByteSource("access.pref", PREFERENCES_SRC.getBytes(StandardCharsets.UTF_8)));
        zip.addEntry(new ByteSource("MANIFEST/manifest.xml", MANIFEST_SRC.getBytes(StandardCharsets.UTF_8)));
        zip.process();

        final var importer = new PackageImporter(packageFile, "TEST");
        importer.importPackage();

        assertExpectedKeys();
    }

    private void assertExpectedKeys()
    {
        final var expectedKeys = EXPECTED_VALUES.keySet();

        assertThat(takPreferencesNode.keys())
                .hasSize(expectedKeys.size())
                .containsAll(expectedKeys);

        for (String key : expectedKeys)
        {
            assertThat(takPreferencesNode.get(key, null)).isEqualTo(EXPECTED_VALUES.get(key));
        }
    }
}
