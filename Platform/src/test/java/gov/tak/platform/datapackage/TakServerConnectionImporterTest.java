package gov.tak.platform.datapackage;

import gov.tak.api.comms.security.ISslCertificateManager;
import gov.tak.api.comms.takserver.ITakServerConnectionManager;
import gov.tak.blacksmith.Blacksmith;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.blacksmith.mock.strategy.MockStrategy;
import gov.tak.blacksmith.mock.strategy.MockitoStrategy;
import gov.tak.core.comms.security.SslCertificateManager;
import gov.tak.core.comms.takserver.TakServerConnectionManager;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.core.view.gui.maps.importers.wizards.TakServerImportWizard;
import gov.takx.platform.io.panels.CommonWizardSettings;
import gov.takx.test.AContainerTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.Zips;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @since 3.4
 */
@SuppressWarnings({"ResultOfObjectAllocationIgnored", "ConstantConditions"})
public class TakServerConnectionImporterTest extends AContainerTest
{
    private static final String MANIFEST_SRC = """
            <MissionPackageManifest version="2">
               <Configuration>
                  <Parameter name="uid" value="a647112f-ce05-4312-a2b4-208d8d8a5fa8"/>
                  <Parameter name="name" value="yeti-2020.zip"/>
                  <Parameter name="onReceiveDelete" value="true"/>
               </Configuration>
               <Contents>
                  <Content ignore="false" zipEntry="ATAK2020.p12"/>
                  <Content ignore="false" zipEntry="Mindvolt-CA.p12"/>
                  <Content ignore="false" zipEntry="Yeti.pref"/>
               </Contents>
            </MissionPackageManifest>
            """;

    private static final String PREFERENCES_SRC = """
            <?xml version='1.0' standalone='yes'?>
            <preferences>
            <preference version="1" name="cot_streams">
            <entry key="count" class="class java.lang.Integer">1</entry>
            <entry key="description0" class="class java.lang.String">Yeti</entry>
            <entry key="enabled0" class="class java.lang.Boolean">false</entry>
            <entry key="connectString0" class="class java.lang.String">yeti.pargovernment.net:8089:ssl</entry>
            <entry key="caLocation0" class="class java.lang.String">cert/Mindvolt-CA.p12</entry>
            <entry key="certificateLocation0" class="class java.lang.String">cert/ATAK2020.p12</entry>
            <entry key="clientPassword0" class="class java.lang.String">password</entry>
            <entry key="caPassword0" class="class java.lang.String">password</entry>
            </preference>
            <preference version="1" name="com.atakmap.app_preferences">
            </preference>
            </preferences>
            """;

    private static final byte[] MANIFEST_BYTES = MANIFEST_SRC.getBytes(StandardCharsets.UTF_8);
    private static final byte[] PREFERENCES_BYTES = PREFERENCES_SRC.getBytes(StandardCharsets.UTF_8);
    private static final byte[] FAKE_CERT_BYTES = "fake".getBytes(StandardCharsets.UTF_8);

    @Mocked(parentTypes = ISslCertificateManager.class)
    private SslCertificateManager certificateManager;

    @Mocked(parentTypes = ITakServerConnectionManager.class)
    private TakServerConnectionManager connectionManager;

    private File packageFile;
    private File certFile;

    private PackageImporter importer;

    static
    {
        MockStrategy mockStrategy = new MockitoStrategy().cached();
        BlacksmithExtension.setBlacksmithFactory(classes -> new Blacksmith(classes)
                .withMockStrategy(mockStrategy));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @AfterEach
    public void cleanup()
    {
        if (packageFile != null && packageFile.exists()) packageFile.delete();
        if (certFile != null && certFile.exists()) certFile.delete();
    }

    /**
     * Test the "happy path" for importing a package that contains a TAK Server connection configuration.
     * Note this uses {@link PackageImporter#importTakServerConnection()} directly, to mimic how it would be
     * called during a file import (as opposed to a data package download/import).
     */
    @Test
    public void addTakServerConnection_isCalled_WhenValidConnectionImported() throws IOException
    {
        makePackageZip(false, true, true);
        setupSuccessExpectations();
        importer.importTakServerConnection();
        verifyConnection();
    }

    /**
     * Test the "happy path" for importing a package that contains a TAK Server connection configuration.
     * This calls {@link PackageImporter#importPackage()} to mimic how it would be used as part of a data
     * package download/import.
     */
    @Test
    public void addTakServerConnection_isCalled_WhenValidPackageImported() throws IOException
    {
        makePackageZip(true, true, true);
        setupSuccessExpectations();
        importer.importPackage();
        verifyConnection();
    }

    /**
     * Test the "happy path" for importing a package that contains a TAK Server connection configuration.
     * This invokes the import via the {@link TakServerImportWizard}.
     */
    @Test
    public void addTakServerConnection_isCalled_fromWizard_WhenValidPackageImported() throws IOException
    {
        makePackageZip(true, true, true);
        setupSuccessExpectations();

        final TakServerImportWizard wizard = new TakServerImportWizard();
        wizard.preconfigureWizard(Map.of(CommonWizardSettings.FILES, List.of(packageFile)));
        wizard.runOperation(null);

        verifyConnection();
    }

    /**
     * Data Package import requires a manifest (while just TAK server connection zips do not).
     */
    @Test
    public void exceptionThrown_whenManifestMissing() throws IOException
    {
        makePackageZip(false, false, false);

        assertThatIOException()
                .isThrownBy(() -> importer.importPackage())
                .withMessageContaining("No manifest found");
    }

    @Test
    public void exceptionThrown_whenPreferencesMissing() throws IOException
    {
        makePackageZip(false, false, false);

        assertThatIOException()
                .isThrownBy(() -> importer.importTakServerConnection())
                .withMessageContaining("no preferences file found");
    }

    @Test
    @SuppressLogging(TakServerImportWizard.class)
    public void exceptionThrown_fromWizard_whenPreferencesMissing() throws IOException
    {
        makePackageZip(false, false, false);

        final TakServerImportWizard wizard = new TakServerImportWizard();
        wizard.preconfigureWizard(Map.of(CommonWizardSettings.FILES, List.of(packageFile)));

        assertThatRuntimeException()
                .isThrownBy(() -> wizard.runOperation(null))
                .withMessageContaining("no preferences file found");
    }

    @Test
    public void exceptionThrown_whenCertsMissing() throws IOException
    {
        makePackageZip(false, true, false);

        when(connectionManager.findConnectionByName(anyString())).thenReturn(null);

        assertThatIOException()
                .isThrownBy(() -> importer.importTakServerConnection())
                .withMessageContaining("CA Cert not found");
    }

    /**
     * Regression test for TAKX-1313, ensure that an exception is thrown and a server connection node is not left in
     * the preferences.
     */
    @Test
    public void regression_TAKX1313() throws IOException, BackingStoreException
    {
        final String preferences = """
                <?xml version='1.0' standalone='yes'?>
                <preferences>
                <preference version="1" name="cot_streams">
                <entry key="count" class="class java.lang.Integer">1</entry>
                <entry key="description0" class="class java.lang.String">CTI Server 3</entry>
                <entry key="enabled0" class="class java.lang.Boolean">true</entry>
                <entry key="connectString0" class="class java.lang.String">takserver3.eng.ctic-dev.com:8089:ssl</entry>
                <entry key="caLocation" class="class java.lang.String">cert/truststore-root.p12</entry>
                <entry key="certificateLocation" class="class java.lang.String">cert/CTIClient.p12</entry>
                <entry key="clientPassword" class="class java.lang.String">pw</entry>
                <entry key="caPassword" class="class java.lang.String">pw</entry>
                </preference>
                <preference version="1" name="com.atakmap.app_preferences">
                </preference>
                </preferences>
                """;

        packageFile = Files.createTempFile("package_", ".zip").toFile();

        Zips.create().destination(packageFile)
                .addEntry(new ByteSource("CTITAKServer3.pref", preferences.getBytes(StandardCharsets.UTF_8)))
                .addEntry(new ByteSource("CTIClient.p12", FAKE_CERT_BYTES))
                .addEntry(new ByteSource("truststore-root.p12", FAKE_CERT_BYTES))
                .process();

        when(connectionManager.findConnectionByName(anyString())).thenReturn(null);

        importer = new PackageImporter(packageFile, "TEST");

        // Grab the existing TAK Server nodes
        final Preferences serversNode = Preferences.userRoot().node("rx/program/CommonCommo/TakServers");
        final String[] childrenNames = serversNode.childrenNames();

        assertThatIOException()
                .isThrownBy(() -> importer.importTakServerConnection())
                .withMessageContaining("Missing preference 'caLocation'");

        // Make sure that no new node was added
        assertThat(serversNode.childrenNames()).containsOnly(childrenNames);
    }

    /**
     * Manufacture a data package zip, including specified entries.
     *
     * @param includeManifest    {@code true} to include the manifest entry
     * @param includePreferences {@code true} to include the preferences entry
     * @param includeCerts       {@code true} to include the certificate entries
     */
    private void makePackageZip(boolean includeManifest, boolean includePreferences, boolean includeCerts) throws IOException
    {
        packageFile = Files.createTempFile("package_", ".zip").toFile();
        importer = new PackageImporter(packageFile, "TEST");

        final Zips zip = Zips.create().destination(packageFile);

        if (includeManifest) zip.addEntry(new ByteSource("MANIFEST/manifest.xml", MANIFEST_BYTES));
        if (includePreferences) zip.addEntry(new ByteSource("Yeti.pref", PREFERENCES_BYTES));

        if (includeCerts)
        {
            zip.addEntry(new ByteSource("ATAK2020.p12", FAKE_CERT_BYTES));
            zip.addEntry(new ByteSource("Mindvolt-CA.p12", FAKE_CERT_BYTES));
        }

        zip.process();
    }

    /**
     * Configure the mocked services to allow a successful execution of the package import.
     */
    private void setupSuccessExpectations() throws IOException
    {
        certFile = Files.createTempFile("cert_", ".p12").toFile();

        when(certificateManager.createCert(anyString(), any())).thenReturn(certFile);
        when(certificateManager.certExists(anyString())).thenReturn(true);
        when(certificateManager.getCertFile(anyString())).thenReturn(certFile);
        when(connectionManager.findConnectionByName(anyString())).thenReturn(null);
    }

    private void verifyConnection()
    {
        verify(connectionManager, times(1)).addTakServerConnection(any());
        clearInvocations(connectionManager);
    }
}