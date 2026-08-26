package gov.takx.platform.kml;

import javax.xml.stream.XMLStreamException;

import java.io.IOException;

/**
 * Common interface for the KML/KMZ document builders.  Allows for some abstraction whether you are exporting
 * to a KML or a KMZ file.
 *
 * @since 1.34
 */
public interface IExportDocumentBuilder
{
    /**
     * Returns if there are any open KML folders.
     *
     * @return boolean value if open folders exist.
     */
    boolean isFolderOpen();

    /**
     * Open a Folder tag in KML, with an added description tag.
     *
     * @param name        name of the folder
     * @param description description tag for the folder
     * @throws XMLStreamException If an error is encountered while writing KML.
     */
    void openFolder(String name, String description) throws XMLStreamException;

    /**
     * Open a Folder tag in KML
     *
     * @param name name of the folder
     * @throws XMLStreamException If an error is encountered while writing KML.
     */
    void openFolder(String name) throws XMLStreamException;

    /**
     * Write the exportable to KML.
     *
     * @param exportable the exportable
     * @throws IOException exception during the write
     */
    void writeObject(gov.nasa.worldwind.Exportable exportable) throws IOException;

    /**
     * Close the folder.  No magic here, just writes an end element.  If you want magic, go to the circus.
     *
     * @throws XMLStreamException If an error is encountered while writing KML.
     */
    void closeFolder() throws XMLStreamException;

    /**
     * Return the temp folder specified for this document builder, set in the constructor.  Useful for when
     * contributing additional files to the overall kmz file, such as icons and images.  Use this path to save
     * the images to.
     *
     * @return the temp folder for files to be included with the KMZ document.
     */
    String getKmzTempFolder();

    /**
     * Close the document.  Performs any necessary steps that involve writing to the end of the document, or
     * zipping the document(s) depending on the implementation.
     *
     * @throws XMLStreamException
     */
    void close() throws XMLStreamException;
}
