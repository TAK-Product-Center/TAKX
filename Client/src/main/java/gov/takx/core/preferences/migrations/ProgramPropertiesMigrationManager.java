package gov.takx.core.preferences.migrations;

import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.ui.IUserProxy;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.utils.SafeXmlDocumentUtils;
import gov.takx.core.configuration.BareProgramEnvironment;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages {@code LocalProgramProperties.xml} to keep it in alignment with {@code DefaultProgramProperties.xml}
 * by applying migration scripts.
 *
 * @see MigrationProcessor
 * @since 5.1
 */
@ApplicationScoped
public class ProgramPropertiesMigrationManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * XPath referring to the 'value' attribute of the 'schemaVersion' entry in the 'rx' node.
     */
    public static final String VERSION_XPATH = "//node[@name='rx']/map/entry[@key='schemaVersion']/@value";

    private static final XPath XPATH = XPathFactory.newInstance().newXPath();

    // Map from version to migration script to process that version
    private static final Map<String, String> MIGRATIONS_BY_VERSION = new HashMap<>();

    static
    {
        MIGRATIONS_BY_VERSION.put("1", "v1_to_v2.xml");
        MIGRATIONS_BY_VERSION.put("2", "v2_to_v3.xml");
        MIGRATIONS_BY_VERSION.put("3", "v3_to_v4.xml");
        MIGRATIONS_BY_VERSION.put("4", "v4_to_v5.xml");
        MIGRATIONS_BY_VERSION.put("5", "v5_to_v6.xml");
        MIGRATIONS_BY_VERSION.put("6", "v6_to_v7.xml");
        MIGRATIONS_BY_VERSION.put("7", "v7_to_v8.xml");
        MIGRATIONS_BY_VERSION.put("8", "v8_to_v9.xml");
        MIGRATIONS_BY_VERSION.put("9", "v9_to_v10.xml");
        MIGRATIONS_BY_VERSION.put("10", "v10_to_v11.xml");
        MIGRATIONS_BY_VERSION.put("11", "v11_to_v12.xml");
        MIGRATIONS_BY_VERSION.put("12", "v12_to_v13.xml");
    }

    private final File localProgramPropertiesFile;
    private final IUserProxy userProxy;

    @Inject
    ProgramPropertiesMigrationManager(IProgramEnvironment programEnvironment, IUserProxy userProxy)
    {
        localProgramPropertiesFile = programEnvironment.getLocalProgramPropertiesFile();
        this.userProxy = userProxy;
    }

    /**
     * Bring {@code LocalProgramProperties.xml} up to date by applying any needed migration scripts.  If this process fails,
     * the {@code DefaultProgramProperties.xml} file will be copied in, overwriting {@code LocalProgramProperties.xml}.
     */
    public void updateLocalProgramProperties()
    {
        try
        {
            applyMigrations();
        } catch (MigrationException e)
        {
            logger.error("Failed to update {}", localProgramPropertiesFile, e);

            // Since we failed, the LPP is in an inconsistent state.  This can cause any number of issues, so the only
            // recourse is to copy the DPP.xml file.  Yes, this will overwrite any local configuration edits made
            // by the user, but that is better than having unexpected program failures due to an inconsistent configuration.
            try
            {
                logger.info("Restoring from {}", BareProgramEnvironment.DEFAULT_PROGRAM_PROPERTIES_XML);
                userProxy.notify("Failed to update local program properties",
                        "Default properties will be restored.\nSorry, but any edits you made to " + localProgramPropertiesFile + " will be lost.");
                FileUtils.copyFileOrResource(BareProgramEnvironment.DEFAULT_PROGRAM_PROPERTIES_XML, localProgramPropertiesFile);
            } catch (IOException ioe)
            {
                final String msg = String.format("Failed to restore %s after update failure", localProgramPropertiesFile);
                logger.error(msg);

                final Throwable throwable = new Throwable("restore failure", e);
                throwable.addSuppressed(ioe); // include this associated exception as well
                throw new RuntimeException(msg, throwable); // this will lead to an application shutdown
            }
        }
    }

    /**
     * Apply any configured migration scripts to the local properties file.  After this method returns, the local properties
     * file will be at the latest version, brought into alignment with the current {@code DefaultProgramProperties.xml}.
     *
     * @throws MigrationException If anything fails
     */
    private void applyMigrations() throws MigrationException
    {
        try
        {
            final Document document = readLocalProperties();

            String version = (String) XPATH.compile(VERSION_XPATH).evaluate(document, XPathConstants.STRING);
            String scriptName;
            boolean migrationApplied = false;

            while ((scriptName = MIGRATIONS_BY_VERSION.get(version)) != null)
            {
                version = applyMigration(document, scriptName, version);
                migrationApplied = true;
            }

            // Only rewrite the local properties if we actually performed a migration
            if (migrationApplied) writeLocalProperties(document);
        } catch (SAXException | IOException | ParserConfigurationException | XPathExpressionException e)
        {
            throw new MigrationException("Failed to process: " + localProgramPropertiesFile, e);
        }
    }

    /**
     * Apply the given migration script to our document.
     *
     * @param document Document to operate upon
     * @param scriptName Name of script to apply
     * @param version The current document version
     * @return The new version, after migration
     */
    private String applyMigration(@Nonnull Document document, @Nonnull String scriptName, @Nonnull String version) throws MigrationException
    {
        logger.debug("Apply migration '{}' to version '{}'", scriptName, version);

        final MigrationProcessor processor = new MigrationProcessor(scriptName);
        processor.processMigration(document);
        return processor.getToVersion();
    }

    /**
     * @return The parsed local program properties document
     */
    private Document readLocalProperties() throws SAXException, IOException, ParserConfigurationException
    {
        // The Preferences XML uses a DOCTYPE, so we have to allow that to parse it
        return SafeXmlDocumentUtils.getDocumentBuilder(true)
                .parse(new InputSource(new FileReader(localProgramPropertiesFile)));
    }

    /**
     * Write the given document out to local program properties, with formatting.
     */
    private void writeLocalProperties(@Nonnull Document document) throws IOException
    {
        // We need to specify that the DOCTYPE should be included in the output, or it won't be loadable by Preferences
        Files.writeString(localProgramPropertiesFile.toPath(),
                SafeXmlDocumentUtils.formatXml(document, Map.of(OutputKeys.DOCTYPE_SYSTEM, document.getDoctype().getSystemId())));
    }
}
