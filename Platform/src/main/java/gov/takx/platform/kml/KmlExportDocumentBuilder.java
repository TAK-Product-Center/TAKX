package gov.takx.platform.kml;

import gov.nasa.worldwindx.examples.kml.KMLDocumentBuilder;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.utils.SafeXmlDocumentUtils;
import gov.takx.commons.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import javax.xml.stream.XMLStreamException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;

/**
 * Extension of {@link gov.nasa.worldwindx.examples.kml.KMLDocumentBuilder} to provide the ability
 * to group exports in a KML Folder tag.
 */
public class KmlExportDocumentBuilder extends KMLDocumentBuilder implements IExportDocumentBuilder
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // I suppose we could have nested folders...
    private int folderOpen;
    private final File kmlFile;
    private final StringWriter stringWriter;

    /**
     * Create a KML document using a Writer.
     *
     * @param writer Writer to receive KML output.
     * @throws javax.xml.stream.XMLStreamException If an error is encountered while writing KML.
     */
    public KmlExportDocumentBuilder(StringWriter writer, File file) throws XMLStreamException
    {
        super(writer);
        kmlFile = file;
        stringWriter = writer;
    }

    /**
     * Open a Folder tag in KML
     *
     * @param name name of the folder
     * @throws XMLStreamException If an error is encountered while writing KML.
     */
    @Override
    public void openFolder(String name) throws XMLStreamException
    {
        writer.writeStartElement("Folder");
        writer.writeStartElement("name");
        writer.writeCharacters(name);
        writer.writeEndElement();
        folderOpen++;
    }

    /**
     * Open a Folder tag in KML, with an added description tag.
     *
     * @param name        name of the folder
     * @param description description tag for the folder
     * @throws XMLStreamException If an error is encountered while writing KML.
     */
    @Override
    public void openFolder(String name, String description) throws XMLStreamException
    {
        writer.writeStartElement("Folder");
        if (name != null)
        {
            writer.writeStartElement("name");
            writer.writeCharacters(name);
            writer.writeEndElement();
        }
        if (!StringUtils.isEmpty(description))
        {
            writer.writeStartElement("description");
            writer.writeCharacters(description);
            writer.writeEndElement();
        }
        folderOpen++;
    }

    /**
     * Close the folder.  No magic here, just writes an end element.  If you want magic, go to the circus.
     *
     * @throws XMLStreamException If an error is encountered while writing KML.
     */
    @Override
    public void closeFolder() throws XMLStreamException
    {
        if (folderOpen > 0)
        {
            writer.writeEndElement();
            folderOpen--;
        } else
        {
            throw new IllegalStateException("Attempt to close a Folder tag when there aren't any open");
        }
    }

    /**
     * Returns if there are any open folders.
     *
     * @return boolean value if open folders exist.
     */
    @Override
    public boolean isFolderOpen()
    {
        return folderOpen > 0;
    }

    @Override
    public void close() throws XMLStreamException
    {
        super.close();

        // Format the KML
        String xmlToWrite = stringWriter.toString();
        try
        {
            xmlToWrite = SafeXmlDocumentUtils.formatXml(new InputSource(new StringReader(xmlToWrite)));
        } catch (IOException ignored)
        {
            // Will just write unformatted XML if we fail
        }
        logger.trace("XML: {}", xmlToWrite);

        // write the chosen kml file
        try (var out = new BufferedWriter(new FileWriter(kmlFile)))
        {
            out.write(xmlToWrite);
        } catch (IOException e)
        {
            logger.error("Error writing to {}", kmlFile.getAbsolutePath(), e);
            FileUtils.deleteDirectory(kmlFile);
        }
    }

    @Override
    public String getKmzTempFolder()
    {
        throw new UnsupportedOperationException("Unsupported operation for the KmlExportDocumentBuilder");
    }
}
