package gov.takx.core.preferences.migrations;

import com.google.common.io.Resources;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.bootstrap.PreferencesFactoryConstants;
import gov.takx.commons.utils.LoggingSaxErrorHandler;
import gov.takx.commons.utils.SafeXmlDocumentUtils;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.transform.OutputKeys;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

/**
 * Validations for {@link MigrationProcessor}.
 *
 * @since 5.1
 */
@SuppressWarnings({"ResultOfObjectAllocationIgnored", "DataFlowIssue"})
class MigrationProcessorTest
{
    static
    {
        // Configure our custom Preferences factory
        System.setProperty("java.util.prefs.PreferencesFactory", PreferencesFactoryConstants.PREFERENCES_FACTORY_CLASS);
    }

    private static final String TEST_SCRIPT = "test_migration.xml";
    private static final XPath XPATH = XPathFactory.newInstance().newXPath();

    @Test
    @SuppressLogging({LoggingSaxErrorHandler.class})
    void whenInvalidScript_constructor_throws()
    {
        assertThatExceptionOfType(MigrationException.class).isThrownBy(() -> new MigrationProcessor("invalid_migration.xml"));
    }

    @Test
    void whenValidScript_getFromVersion_returnsCorrectVersion() throws MigrationException
    {
        final MigrationProcessor processor = new MigrationProcessor(TEST_SCRIPT);
        assertThat(processor.getFromVersion()).isEqualTo("1");
    }

    @Test
    void whenValidScript_getToVersion_returnsCorrectVersion() throws MigrationException
    {
        final MigrationProcessor processor = new MigrationProcessor(TEST_SCRIPT);
        assertThat(processor.getToVersion()).isEqualTo("2");
    }

    @Test
    void whenValidScript_processMigration_producesCorrectContent() throws Exception
    {
        final Document document = readTestProperties();
        final MigrationProcessor processor = new MigrationProcessor(TEST_SCRIPT);
        processor.processMigration(document);

        // Now turn the document back into XML so we can perform some validations
        // We need to specify that the DOC_TYPE should be included in the output
        final String result = SafeXmlDocumentUtils.formatXml(document, Map.of(OutputKeys.DOCTYPE_SYSTEM, document.getDoctype().getSystemId()));

        assertThat(result).contains("<node name=\"rx\">");

        // RibbonBarConfiguration should have been added after "locationProvider"
        final int rbcIndex = result.indexOf("<node name=\"RibbonBarConfiguration\">");
        final int lpIndex = result.indexOf("<node name=\"locationProvider\">");
        assertThat(rbcIndex).isGreaterThan(lpIndex);

        // Comment should have been added after "locationProvider"
        final int showIndex = result.indexOf("<entry key=\"showCurrentLocationMarker\"");
        assertThat(result.indexOf("<!-- another comment -->")).isGreaterThan(showIndex);

        // "supportEmail" should have been removed along with its preceding comment
        assertThat(result.indexOf("<entry key=\"supportEmail\"")).isLessThan(0);
        assertThat(result.indexOf("<!-- Email address to show")).isLessThan(0);

        // new map entry prepended before comment on reference node
        final int commentIndex = result.indexOf("<!-- Enable the setting");
        final int newEntryIndex = result.indexOf("<entry key=\"newKey\" ");
        assertThat(newEntryIndex).isLessThan(commentIndex);

        // new map entry before properly skips comment
        final int nfkIndex = result.indexOf("<entry key=\"newFirstKey\"");
        final int pcIndex = result.indexOf("<!-- prepended comment -->");
        assertThat(nfkIndex).isGreaterThan(0).isLessThan(pcIndex);

        // Verify that it is still legal Preferences XML by importing it.  But, change the 'rx' node to something
        // else so we don't corrupt any preferences loaded during tests.
        final String forImport = result.replace("<node name=\"rx\">", "<node name=\"rx_for_test\">");

        Preferences.importPreferences(new ByteArrayInputStream(forImport.getBytes(StandardCharsets.UTF_8)));
        final Preferences userRoot = Preferences.userRoot();

        // Ensure the correct version has been set
        final String newVersion = userRoot.node("rx_for_test").get("schemaVersion", "none");
        assertThat(newVersion).isEqualTo("2");

        final Preferences node = userRoot.node("rx_for_test/program/locationProvider");
        assertThat(node).isNotNull();
        assertThat(node.get("selectedProvider", "missing")).isEqualTo("FixedLocationProvider");

        // A new map entry was created, and it's comment is present
        final Preferences configNode = userRoot.node("rx_for_test/program/locationProvider/configurations");
        assertThat(configNode).isNotNull();
        assertThat(configNode.get("someKey", "missing")).isEqualTo("someValue");
        assertThat(result.indexOf("<!-- a new map entry -->")).isGreaterThanOrEqualTo(0);
    }

    @Test
    void whenIncorrectFromVersion_processMigration_throws() throws Exception
    {
        final Document document = readTestPropertiesWithChanges(
                s -> s.replace("key=\"schemaVersion\" value=\"1\"", "key=\"schemaVersion\" value=\"2\""));

        final MigrationProcessor processor = new MigrationProcessor(TEST_SCRIPT);
        assertThatExceptionOfType(MigrationException.class)
                .isThrownBy(() -> processor.processMigration(document))
                .withMessageContaining("wrong version");
    }

    @Test
    void whenDifferentOldValue_processMigration_doesNotChangeValue() throws Exception
    {
        // Change the value so that the migration step won't match, and leaves the value intact
        final Document document = readTestPropertiesWithChanges(
                s -> s.replace("<entry key=\"selectedProvider\" value=\"NoLocationProvider\"/>",
                        "<entry key=\"selectedProvider\" value=\"MapObjectLocationProvider\"/>"));

        final MigrationProcessor processor = new MigrationProcessor(TEST_SCRIPT);
        processor.processMigration(document);

        final String expression = "//node[@name='locationProvider']/map/entry[@key='selectedProvider']/@value";
        final Node node = (Node) XPATH.compile(expression).evaluate(document, XPathConstants.NODE);

        assertThat(node).isNotNull();
        assertThat(node.getTextContent()).isEqualTo("MapObjectLocationProvider");
    }

    @Test
    void whenMissingReferenceXpath_processMigration_throws() throws Exception
    {
        final MigrationProcessor processor = new MigrationProcessor("missing_ref_xpath.xml");
        assertThatExceptionOfType(MigrationException.class)
                .isThrownBy(() -> processor.processMigration(readTestProperties()))
                .withMessageContaining("Missing reference node");
    }

    /**
     * @return Parsed test properties document
     */
    private Document readTestProperties() throws Exception
    {
        return readTestPropertiesWithChanges(s -> s);
    }

    /**
     * Read and parse our test properties, applying a given mutator to change the XML content before parsing.
     *
     * @param mutator function to modify the XML string before parsing
     * @return parsed document
     */
    private Document readTestPropertiesWithChanges(Function<String, String> mutator) throws Exception
    {
        final URL propertiesUrl = getClass().getResource("test_properties.xml");
        final String propertiesXml = mutator.apply(Resources.toString(propertiesUrl, StandardCharsets.UTF_8));

        // The Preferences XML uses a DOCTYPE, so we have to allow it to parse properly
        return SafeXmlDocumentUtils.getDocumentBuilder(true)
                .parse(new InputSource(new StringReader(propertiesXml)));
    }
}
