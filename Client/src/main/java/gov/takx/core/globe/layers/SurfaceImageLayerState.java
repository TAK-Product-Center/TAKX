package gov.takx.core.globe.layers;

import gov.nasa.worldwind.geom.LatLon;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.commons.utils.SafeXmlDocumentUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the state of a surface image layer. The state includes the path to the image used for the layer,
 * the position of the corners of the image, and any control points that have been used to geo-rectify the image.
 * <p/>
 * This class provides methods to serialize and deserialize this state to/from XML format.
 *
 * @param filePath      The path of the image for this layer.
 *                      Can be relative to the VFS root or an absolute path.
 * @param corners       The positions of the corners of the image.
 * @param controlPoints The control points used to geo-rectify the image.
 */
public record SurfaceImageLayerState(String filePath, List<LatLon> corners, List<ControlPoint> controlPoints)
{
    /**
     * Create an empty state with an image file name.
     *
     * @param filePath The path of the image for this layer.
     */
    public SurfaceImageLayerState(String filePath)
    {
        this(filePath, new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Create a state with an image and a list of corner locations.
     *
     * @param filePath The path of the image for this layer.
     * @param corners The positions of the corners of the image.
     */
    public SurfaceImageLayerState(String filePath, List<LatLon> corners)
    {
        this(filePath, List.copyOf(corners), List.of());
    }

    /**
     * Create a state with an image, corners, and control points used to geo-rectify the image
     *
     * @param filePath The path of the image for this layer.
     * @param corners The positions of the corners of the image.
     * @param controlPoints The control points used to geo-rectify the image.
     * @since 5.1
     */
    public SurfaceImageLayerState(String filePath, List<LatLon> corners, List<ControlPoint> controlPoints)
    {
        this.filePath = filePath;
        this.corners = List.copyOf(corners);
        this.controlPoints = List.copyOf(controlPoints);
    }

    /**
     * @return Serialized state in XML format
     */
    public String buildRestorableState()
    {
        try
        {
            Document document = SafeXmlDocumentUtils.getDocumentBuilder().newDocument();
            Node restorableState = document.appendChild(document.createElement("restorableState"));

            var stateObject = (Element) restorableState.appendChild(document.createElement("stateObject"));
            stateObject.setAttribute("name", "filePath");
            stateObject.appendChild(document.createTextNode(filePath));

            createCornerNodes(document, restorableState);
            createControlPointNodes(document, restorableState);

            return SafeXmlDocumentUtils.formatXml(document);
        } catch (IOException | ParserConfigurationException e)
        {
            throw new UnsupportedOperationException(e);
        }
    }

    /**
     * Create nodes for the control points under the given {@code parent} node.
     *
     * @param parent Node to which to append the created control point nodes
     * @param document Document being processed
     * @since 5.1
     */
    private void createControlPointNodes(Document document, Node parent)
    {
        for (ControlPoint controlPoint : controlPoints)
        {
            final var node = (Element) parent.appendChild(document.createElement("control-point"));

            final var location = controlPoint.location();
            node.setAttribute("lat", String.valueOf(location.getLatitude()));
            node.setAttribute("lon", String.valueOf(location.getLongitude()));

            final var point = controlPoint.point();
            node.setAttribute("pointX", String.valueOf(point.getX()));
            node.setAttribute("pointY", String.valueOf(point.getY()));
        }
    }

    /**
     * Create nodes for the corners under the given {@code parent} node.
     *
     * @param document Document being processed
     * @param parent Node to which to append the created corner nodes
     */
    private void createCornerNodes(Document document, Node parent)
    {
        for (LatLon corner : corners)
        {
            final var node = (Element) parent.appendChild(document.createElement("corner"));
            node.setAttribute("lat", String.valueOf(corner.getLatitude().getDegrees()));
            node.setAttribute("lon", String.valueOf(corner.getLongitude().getDegrees()));
        }
    }

    /**
     * Factory for creating a new {@link SurfaceImageLayerState} from a given XmL string.
     * @since 5.1
     */
    private static SurfaceImageLayerState parseRestorableState(String stateInXml) throws IOException
    {
        try
        {
            final Document document = SafeXmlDocumentUtils.parseXML(stateInXml);
            final XPath xpath = XPathFactory.newInstance().newXPath();

            final String imagePath = (String) xpath.evaluate("/restorableState/stateObject[@name='filePath']/text()", document, XPathConstants.STRING);

            final var corners = new ArrayList<LatLon>();
            final var controlPoints = new ArrayList<ControlPoint>();

            final var cornerNodes = (NodeList) xpath.evaluate("/restorableState/corner", document, XPathConstants.NODESET);
            final var controlPointNodes = (NodeList) xpath.evaluate("/restorableState/control-point", document, XPathConstants.NODESET);

            if (cornerNodes != null)
            {
                for (int i = 0; i < cornerNodes.getLength(); i++)
                {
                    final var corner = (Element) cornerNodes.item(i);

                    final double lat = Double.parseDouble(corner.getAttribute("lat"));
                    final double lon = Double.parseDouble(corner.getAttribute("lon"));

                    corners.add(LatLon.fromDegrees(lat, lon));
                }
            }

            if (controlPointNodes != null)
            {
                for (int i = 0; i < controlPointNodes.getLength(); i++)
                {
                    final var controlPoint = (Element) controlPointNodes.item(i);

                    final var location = new RaptorLocation(
                            Double.parseDouble(controlPoint.getAttribute("lat")),
                            Double.parseDouble(controlPoint.getAttribute("lon")));

                    final var point = new Point2D.Double(
                            Double.parseDouble(controlPoint.getAttribute("pointX")),
                            Double.parseDouble(controlPoint.getAttribute("pointY")));

                    controlPoints.add(new ControlPoint(point, location));
                }
            }

            return new SurfaceImageLayerState(imagePath, corners, controlPoints);
        } catch (Exception e)
        {
            throw new IOException(e);
        }
    }

    /**
     * Create a new layer state by parsing a serialized state.
     *
     * @param restorableState Serialized state string to parse.
     * @return The new layer state object.
     * @throws IOException If the string cannot be parsed.
     */
    public static SurfaceImageLayerState createFromString(String restorableState) throws IOException
    {
        return parseRestorableState(restorableState);
    }

    /**
     * Create a layer state object by reading serialized state from a file.
     *
     * @param file File to read.
     * @return The new state object.
     * @throws IOException If the file cannot be read or parsed.
     */
    public static SurfaceImageLayerState createFromFile(File file) throws IOException
    {
        return createFromString(Files.readString(file.toPath()));
    }

    /**
     * Holds a geolocation and screen coordinate location for denoting a geo-rectification point on a surface image
     * (which requires a pixel image point + geo point)
     */
    public record ControlPoint(Point2D point, ILocation location)
    {
    }
}
