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
 * to group exports in a KML Folder tag.  In this case, we are treating the document as a KMZ document
 * where additional files can be contributed to the final zipped KMZ file.  Plugins that export their own
 * state can make use of this to do so.
 */
public class KmzExportDocumentBuilder extends KMLDocumentBuilder implements IExportDocumentBuilder
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private int folderOpen;
    private final File kmzFile;
    private final File kmzFileDir;
    private final String kmzTempFolder;
    private final StringWriter stringWriter;

    /**
     * Create a KML document using a Writer.
     *
     * @param kmzFile the KMZ file
     * @param writer  the string writer to use for writing the KML data.
     * @throws XMLStreamException If an error is encountered while writing KML.
     */
    public KmzExportDocumentBuilder(File kmzFile, StringWriter writer) throws XMLStreamException, IOException
    {
        super(writer);
        stringWriter = writer;
        this.kmzFile = kmzFile;

        kmzTempFolder = KmzUtils.getKMZTempFolder();
        kmzFileDir = new File(kmzTempFolder);

        // Just in case the temp files did not previously get cleaned up for some reason...
        if (kmzFileDir.exists())
        {
            FileUtils.deleteDirectory(kmzFileDir);
        }

        boolean success = (new File(kmzTempFolder)).mkdir();
        logger.debug("Success = {} creating {}", success, kmzTempFolder);

        if (!success)
        {
            throw new IOException("Error creating KML temporary directory: " + kmzTempFolder);
        }
    }

    /**
     * Return the temp folder specified for this document builder, set in the constructor.  Useful for when
     * contributing additional files to the overall kmz file, such as icons and images.  Use this path to save
     * the images to.
     *
     * @return the temp folder for files to be included with the KMZ document.
     */
    @Override
    public String getKmzTempFolder()
    {
        return kmzTempFolder;
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

    /**
     * Close the KMZ document.  Also zips up all the files copied to the temp folder and includes them in the
     * overall KMZ file.
     */
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

        // write the main kml doc file
        final var mainKml = new File(kmzTempFolder, "doc.kml");
        try (var out = new BufferedWriter(new FileWriter(mainKml)))
        {
            out.write(xmlToWrite);
        } catch (IOException e)
        {
            logger.error("Error writing to {}", mainKml.getAbsolutePath(), e);
            FileUtils.deleteDirectory(kmzFileDir);
            return;
        }

        // zip the whole directory into the kmz file and then delete the temp files.
        FileUtils.zipDirectory(kmzTempFolder, kmzFile.getAbsolutePath(), false);
        FileUtils.deleteDirectory(kmzFileDir);
    }
}
