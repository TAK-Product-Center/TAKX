package gov.takx.core.placemarkoverlay;

import au.com.bytecode.opencsv.CSVWriter;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.swing.ImageIcon;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gov.takx.commons.swing.ImageUtils.DEFAULT_ICON_WIDTH;

/**
 */
public class PlacemarkOverlayUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The name of the placemark overlays metadata file. A distinct metadata file will exist for
     * each scope (Global, User and Project) and will contain configuration info for all placemark
     * overlay layers in that scope.
     *
     * @since 1.16
     */
    public static final String PERSIST_FILENAME = "placemarkOverlayPersistedData.xml";

    /**
     * Determines is a column name qualifies as a standard latitude column.
     *
     * @param columnName The name of the column.
     * @return {@code true} if the column qualifies as a latitude column, {@code false} otherwise.
     * @since 1.16
     */
    public static boolean isLatitudeColumn(String columnName)
    {
        return columnName.equalsIgnoreCase("lat") || columnName.equalsIgnoreCase("latitude");
    }

    /**
     * Determines is a column name qualifies as a standard longitude column.
     *
     * @param columnName The name of the column.
     * @return {@code true} if the column qualifies as a longitude column, {@code false} otherwise.
     * @since 1.16
     */
    public static boolean isLongitudeColumn(String columnName)
    {
        return columnName.equalsIgnoreCase("lon") || columnName.equalsIgnoreCase("long") || columnName.equalsIgnoreCase("longitude");
    }

    public static String generateTooltip(String displayName, Collection<String> tooltipColumns, Map<String, String> placemark)
    {
        StringBuilder buffer = new StringBuilder();

        buffer.append("<b>");
        buffer.append(displayName);
        buffer.append("</b>");

        for (String nextKey : tooltipColumns)
        {
            buffer.append("<br>");
            buffer.append(nextKey);
            buffer.append(": ");
            buffer.append(placemark.get(nextKey));
        }

        return buffer.toString();
    }

    /**
     * Write a PlacemarkOverlay object to disk as a CSV file.  This method is used when comments are added or edited
     * in placemark data.
     *
     * @param placemarkOverlay a PlacemarkOverlay to write to disk as a CSV file.
     * @param csvFile          a file handle to write the CSV data to.
     * @throws IOException If an error occurs attempting to write to the file
     */
    public static void writePlacemarkOverlayCSVFile(PlacemarkOverlay placemarkOverlay, File csvFile) throws IOException
    {
        CSVWriter csvWriter = new CSVWriter(new FileWriter(csvFile));

        //write the headers line to the CSV file
        List<String> headers = placemarkOverlay.getHeaders();
        String[] headerArray = new String[headers.size()];
        for (int i = 0; i < headers.size(); i++)
        {
            headerArray[i] = headers.get(i);
        }
        csvWriter.writeNext(headerArray);

        //write each placemark's data to the CSV file
        for (CSVPlaceMark placemark : placemarkOverlay.getData())
        {
            Map<String, String> placemarkData = placemark.getCSVMap();
            String[] dataArray = new String[headerArray.length];

            //iterate through the headers and pull their associated values from the data map
            for (int i = 0; i < headerArray.length; i++)
            {
                String value = placemarkData.get(headerArray[i]);
                if (value == null)
                {
                    value = "";
                }
                dataArray[i] = value;
            }
            csvWriter.writeNext(dataArray);
        }
        csvWriter.close();
    }

    /**
     * Write the metadata for each layer in a given scope (global, user, project) to a file.
     *
     * @param persistFile          The file to write to.
     * @param placemarkOverlayData The metadata for each layer.
     * @throws Exception Thrown on errors writing to the file.
     * @since 1.16
     */
    public static void writePlacemarkOverlayMetadata(File persistFile, List<Map<String, String>> placemarkOverlayData) throws Exception
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.newDocument();
        Node baseNode = document.appendChild(document.createElement("placemarkOverlays"));
        for (Map<String, String> overlay : placemarkOverlayData)
        {
            Node overlayNode = baseNode.appendChild(document.createElement("overlay"));

            Node overlayNameNode = document.createElement(PlacemarkOverlay.PLACEMARK_OVERLAY_NAME);
            overlayNameNode.appendChild(document.createTextNode(overlay.get(PlacemarkOverlay.PLACEMARK_OVERLAY_NAME)));
            overlayNode.appendChild(overlayNameNode);

            Node fileNameNode = document.createElement(PlacemarkOverlay.FILE_NAME);
            fileNameNode.appendChild(document.createTextNode(overlay.get(PlacemarkOverlay.FILE_NAME)));
            overlayNode.appendChild(fileNameNode);

            Node imageNameNode = document.createElement(PlacemarkOverlay.ICON_NAME);
            imageNameNode.appendChild(document.createTextNode(overlay.get(PlacemarkOverlay.ICON_NAME)));
            overlayNode.appendChild(imageNameNode);

            Node visibleNode = document.createElement(PlacemarkOverlay.LAYER_VISIBLE);
            visibleNode.appendChild(document.createTextNode(overlay.get(PlacemarkOverlay.LAYER_VISIBLE)));
            overlayNode.appendChild(visibleNode);

            String idColumn = overlay.get(PlacemarkOverlay.ID_COLUMN);
            if (!StringUtils.isEmpty(idColumn))
            {
                Node idColumnNode = document.createElement(PlacemarkOverlay.ID_COLUMN);
                idColumnNode.appendChild(document.createTextNode(idColumn));
                overlayNode.appendChild(idColumnNode);
            }

            String imageColumn = overlay.get(PlacemarkOverlay.IMAGE_COLUMN);
            if (!StringUtils.isEmpty(imageColumn))
            {
                Node imageColumnNode = document.createElement(PlacemarkOverlay.IMAGE_COLUMN);
                imageColumnNode.appendChild(document.createTextNode(imageColumn));
                overlayNode.appendChild(imageColumnNode);
            }

            String tooltipColumns = overlay.get(PlacemarkOverlay.TOOLTIP_COLUMNS);
            if (!StringUtils.isEmpty(tooltipColumns))
            {
                Node tooltipColumnNodes = document.createElement(PlacemarkOverlay.TOOLTIP_COLUMNS);
                tooltipColumnNodes.appendChild(document.createTextNode(tooltipColumns));
                overlayNode.appendChild(tooltipColumnNodes);
            }

            Node editableNode = document.createElement(PlacemarkOverlay.IS_EDITABLE);
            editableNode.appendChild(document.createTextNode(overlay.get(PlacemarkOverlay.IS_EDITABLE)));
            overlayNode.appendChild(editableNode);
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(new DOMSource(document), new StreamResult(new FileWriter(persistFile)));
    }

    /**
     * Load a PlacemarkOverlay object from a set metadata.
     *
     * @param persistableData The metadata defining the placemark layer.
     * @param scope           The scope of the layer: global, user of project.
     * @return The overlay created based on the metadata.
     * @since 1.16
     */
    public static PlacemarkOverlay loadFromPersistedData(Map<String, String> persistableData, VFSScope scope, IVirtualFileSystem vfs)
    {
        if (persistableData == null ||
                !persistableData.containsKey(PlacemarkOverlay.PLACEMARK_OVERLAY_NAME) ||
                !persistableData.containsKey(PlacemarkOverlay.ICON_NAME) ||
                !persistableData.containsKey(PlacemarkOverlay.FILE_NAME))
        {
            logger.error("placemark persisted data is invalid");
            return null;
        }

        String placemarksPath = getPlacemarksPath(persistableData.get(PlacemarkOverlay.PLACEMARK_OVERLAY_NAME), vfs);
        File csvFile = new File(placemarksPath + persistableData.remove(PlacemarkOverlay.FILE_NAME));
        File imageFile = new File(placemarksPath + persistableData.remove(PlacemarkOverlay.ICON_NAME));
        String idColumn = persistableData.get(PlacemarkOverlay.ID_COLUMN);
        String imageColumn = persistableData.get(PlacemarkOverlay.IMAGE_COLUMN);
        Collection<String> tooltipColumns = getTooltipColumnsFromString(persistableData.get(PlacemarkOverlay.TOOLTIP_COLUMNS));
        boolean editable = Boolean.parseBoolean(persistableData.get(PlacemarkOverlay.IS_EDITABLE));

        if (!csvFile.canRead() || !imageFile.canRead())
        {
            logger.error("persisted files are not readable");
            return null;
        }

        try
        {

            return new PlacemarkOverlay(
                    persistableData.remove(PlacemarkOverlay.PLACEMARK_OVERLAY_NAME),
                    csvFile,
                    ',',
                    imageFile.getAbsolutePath(),
                    idColumn,
                    imageColumn,
                    tooltipColumns,
                    scope,
                    editable,
                    false);
        } catch (IOException e)
        {
            logger.error("Unable to restore placemark layer. IOException instantiating Placemark Overlay.", e);
            return null;
        }
    }

    /**
     * Read in the metadata for all of the layers in a scope.
     *
     * @param persistFile The file containing the metadata.
     * @return A list of layer metadata mappings.
     * @throws Exception Thrown on errors reading the metadata document.
     * @since 1.16
     */
    public static List<Map<String, String>> readPlacemarkOverlayMetadata(File persistFile) throws Exception
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.parse(new InputSource(new FileReader(persistFile)));
        List<Map<String, String>> metadata = new ArrayList<>();

        NodeList overlayNodes = document.getElementsByTagName("overlay");
        for (int i = 0; i < overlayNodes.getLength(); i++)
        {
            Node overlayNode = overlayNodes.item(i);
            String overlayName = getText(PlacemarkOverlay.PLACEMARK_OVERLAY_NAME, overlayNode);
            String overlayFile = getText(PlacemarkOverlay.FILE_NAME, overlayNode);
            String overlayImage = getText(PlacemarkOverlay.ICON_NAME, overlayNode);
            String overlayVisible = getText(PlacemarkOverlay.LAYER_VISIBLE, overlayNode);
            String editable = getText(PlacemarkOverlay.IS_EDITABLE, overlayNode);

            String idColumn = nodeExists(PlacemarkOverlay.ID_COLUMN, overlayNode) ? getText(PlacemarkOverlay.ID_COLUMN, overlayNode) : null;
            String imageColumn = nodeExists(PlacemarkOverlay.IMAGE_COLUMN, overlayNode) ? getText(PlacemarkOverlay.IMAGE_COLUMN, overlayNode) : null;
            String tooltipColumns = nodeExists(PlacemarkOverlay.TOOLTIP_COLUMNS, overlayNode) ? getText(PlacemarkOverlay.TOOLTIP_COLUMNS, overlayNode) : null;

            if (overlayName != null && overlayFile != null && overlayImage != null && overlayVisible != null)
            {
                Map<String, String> overlayData = new HashMap<>();
                overlayData.put(PlacemarkOverlay.PLACEMARK_OVERLAY_NAME, overlayName);
                overlayData.put(PlacemarkOverlay.FILE_NAME, overlayFile);
                overlayData.put(PlacemarkOverlay.ICON_NAME, overlayImage);
                overlayData.put(PlacemarkOverlay.LAYER_VISIBLE, overlayVisible);
                overlayData.put(PlacemarkOverlay.ID_COLUMN, idColumn);
                overlayData.put(PlacemarkOverlay.IMAGE_COLUMN, imageColumn);
                overlayData.put(PlacemarkOverlay.TOOLTIP_COLUMNS, tooltipColumns);
                overlayData.put(PlacemarkOverlay.IS_EDITABLE, editable);

                metadata.add(overlayData);
            }
        }

        return metadata;
    }

    /**
     * Returns an ImageIcon, or {@code null} if the path was invalid.
     *
     * @param path        The root path to the icon.
     * @param description Icon description.
     * @return An image icon.
     * @since 1.16
     */
    public static ImageIcon createImageIcon(String path, String description)
    {
        return new ImageIcon(path, description);
    }

    /**
     * Determine the scaling factor for an image based on the static icon width.
     *
     * @param imageAddress The image's location.
     * @return The scaling factor.
     */
    public static double calculateScaleFactor(String imageAddress)
    {
        ImageIcon image = createImageIcon(imageAddress, "");

        return DEFAULT_ICON_WIDTH / (double) image.getIconWidth();
    }

    /**
     * Generate a single tooltip String from all the columns specified to be included in the tooltip.
     *
     * @param tooltipColumns The columns to use to generate the tooltip.
     * @return The tooltip String.
     * @since 1.16
     */
    public static String getTooltipColumnsString(Collection<String> tooltipColumns)
    {
        if (tooltipColumns != null)
        {
            StringBuilder buffer = new StringBuilder();
            for (String columnName : tooltipColumns)
            {
                if (!buffer.isEmpty()) buffer.append(';');
                buffer.append(columnName);
            }
            return buffer.toString();
        } else
        {
            return null;
        }
    }

    /**
     * Split persisted tooltip column names out of a single String.
     *
     * @param tooltipColumns The String containing all the semicolon delimited tooltip columns.
     * @return A collection of tooltip column names.
     * @since 1.16
     */
    private static Collection<String> getTooltipColumnsFromString(String tooltipColumns)
    {
        if (tooltipColumns != null)
        {
            return Arrays.asList(tooltipColumns.split(";"));
        } else
        {
            return null;
        }
    }

    /**
     * Get the path to the placemark overlay directory.
     *
     * @param name The name of the placemark overlay
     * @param vfs  VFS for the scope of the placemark overlay
     * @return The path to the overlay's directory.
     * @since 1.16
     */
    public static String getPlacemarksPath(String name, IVirtualFileSystem vfs)
    {
        String placemarksPath = vfs.getRootDir().getPath() + name + File.separator;
        File path = new File(placemarksPath);
        if (!path.exists())
        {
            //noinspection ResultOfMethodCallIgnored
            path.mkdirs();
        }
        return placemarksPath;
    }

    /**
     * Check to see if a file is a readable image.
     *
     * @param f The file to check.
     * @return {@code true} if the file is readable, {@code false} otherwise.
     */
    public static boolean checkImage(File f)
    {
        boolean success = true;

        if (ImageUtils.getImage(f, false) == null)
        {
            success = false;
            AlwaysOnTopJOptionPane.showErrorDialog("The selected file is not a valid image.", "Image Selection Error");
        }

        return success;
    }

    /**
     * Check to see if a node exists in a DOM document.
     *
     * @param nodeName The name of the node.
     * @param parent   The parent node.
     * @return {@code true} if the node exists, {@code false} otherwise.
     * @throws XPathExpressionException Thrown if there is an exception navigating the document.
     * @since 1.16
     */
    private static boolean nodeExists(String nodeName, Node parent)
            throws XPathExpressionException
    {
        XPath xpath = XPathFactory.newInstance().newXPath();
        return xpath.evaluate(nodeName, parent, XPathConstants.NODE) != null;
    }

    /**
     * Get the text value of a node in a DOM document.
     *
     * @param nodeName The name of the node.
     * @param parent   The node's parent.
     * @return The text contained in the node.
     * @throws XPathExpressionException Thrown if there is an exception navigating the document.
     * @since 1.16
     */
    private static String getText(String nodeName, Node parent)
            throws XPathExpressionException
    {
        XPath xpath = XPathFactory.newInstance().newXPath();
        return (String) xpath.evaluate(nodeName + "/text()", parent, XPathConstants.STRING);
    }
}
