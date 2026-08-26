package gov.takx.core.project;

import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.api.project.IProjectMapObjectMetadata;
import gov.takx.api.project.IProjectMetadata;
import gov.takx.commons.utils.ParseUtils;
import gov.takx.commons.utils.PlatformVersion;
import gov.takx.platform.preferences.ProjectPreferenceEntry;
import jakarta.annotation.Nonnull;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Parsing of the project XML file stored within a project or package.
 *
 * @since 1.40
 */
public class ProjectFileParser
{
    private final XPath xpath = XPathFactory.newInstance().newXPath();

    private Document document;

    /**
     * Parse the given projx file content, producing the {@link IProjectMetadata project metadata}.
     *
     * @param projectName  The name of the project
     * @param projxContent The content of the {@code .projx} file to parse
     * @param fromPackage  Pass {@code true} if this file content came from a package
     * @return IProjectMetadata instance
     */
    public IProjectMetadata parseProjectFile(@Nonnull String projectName, @Nonnull String projxContent, boolean fromPackage) throws IOException
    {
        final ProjectMetadata pmd = new ProjectMetadata();

        pmd.setPackage(fromPackage);
        pmd.setProjectName(projectName);

        // Pull the .projx xml file into a dom document and extract the required metadata.
        try
        {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));

            document = builder.parse(new InputSource(new StringReader(projxContent)));

            pmd.setProjectUuid(UUID.fromString(getStringValue(ProjectPreferenceEntry.UUID).trim()));
            pmd.setPlatformVersion(new PlatformVersion(getStringValue(ProjectPreferenceEntry.VERSION)));

            pmd.setCreationDate(Long.parseLong(getStringValue(ProjectPreferenceEntry.CREATION_TIME)));
            pmd.setStartDate(ParseUtils.parseLong(getStringValue(ProjectPreferenceEntry.START_DATE), 0));
            pmd.setEndDate(ParseUtils.parseLong(getStringValue(ProjectPreferenceEntry.END_DATE), 0));

            pmd.setSize(getStringValue(ProjectPreferenceEntry.SIZE));
            pmd.setVfsSize(getStringValue(ProjectPreferenceEntry.VFS));
            pmd.setTitle(getStringValue(ProjectPreferenceEntry.TITLE));
            pmd.setDescription(getStringValue(ProjectPreferenceEntry.DESCRIPTION));
            pmd.setLocation(getStringValue(ProjectPreferenceEntry.LOCATION));
            pmd.setNotes(getStringValue(ProjectPreferenceEntry.NOTES));
            pmd.setWmsOfflineMode(Boolean.parseBoolean(getStringValue(ProjectPreferenceEntry.WMS_OFFLINE_MODE)));
            pmd.setDbType(getStringValue(ProjectPreferenceEntry.DATABASE_TYPE));
            pmd.setDbVersion(getStringValue(ProjectPreferenceEntry.DATABASE_VERSION));

            pmd.setMapObjects(makeMapObjectList());
        } catch (Exception e)
        {
            throw new IOException("Parsing failed on project file: " + projectName, e);
        }

        return pmd;
    }

    /**
     * Get a string value from the XPath for a given preference.
     *
     * @param preference Preference from which to generate the XPath
     * @return String value
     * @since 3.0
     */
    private String getStringValue(IPreferenceEntry preference) throws XPathExpressionException
    {
        return (String) xpath.evaluate(makeValueNodePath(preference), document, XPathConstants.STRING);
    }

    /**
     * @return List of MapObject metadata, or empty list if no Map Objects
     * @since 3.0
     */
    @Nonnull
    private List<IProjectMapObjectMetadata> makeMapObjectList() throws XPathExpressionException
    {
        final String moNodePath = "//node[@name='" + ProjectPreferenceEntry.MAP_OBJECTS_NODE.getKey() + "']/map/entry";
        final NodeList mobs = (NodeList) xpath.evaluate(moNodePath, document, XPathConstants.NODESET);
        if (mobs != null)
        {
            final int length = mobs.getLength();
            final List<IProjectMapObjectMetadata> mapObjects = new ArrayList<>(length);

            for (int i = 0; i < length; i++)
            {
                final String v = mobs.item(i).getAttributes().getNamedItem("value").getNodeValue();
                mapObjects.add(new ProjectMapObjectMetadata(v));
            }

            return mapObjects;
        }

        return Collections.emptyList();
    }

    /**
     * Construct the xpath for the value of a node with the given key.
     *
     * @param preferenceEntry Preference entry from which to extract the node key
     * @return Value of that node as a string
     * @since 2.35
     */
    private String makeValueNodePath(IPreferenceEntry preferenceEntry)
    {
        return "//node[@name='project']/map/entry[@key='" + preferenceEntry.getKey() + "']/@value";
    }
}
