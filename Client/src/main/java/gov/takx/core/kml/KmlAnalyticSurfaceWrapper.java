package gov.takx.core.kml;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.ImageUtil;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface.GridPointAttributes;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceAttributes;
import gov.takx.core.surfaces.AnalyticSurface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

/**
 */
public class KmlAnalyticSurfaceWrapper extends AKmlWrapper
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static int analyticSurfaceImageNumber = 1;
    private final AnalyticSurface analyticSurface;
    private int exportImageWidth = 1024;
    private int exportImageHeight = 1024;

    public KmlAnalyticSurfaceWrapper(AnalyticSurface analyticSurface, String kmlFolder)
    {
        super(kmlFolder);
        this.analyticSurface = analyticSurface;
    }

    /**
     * Specifies the image width of the ground overlay image when this surface is exported to KML. The default is 1024.
     *
     * @param exportImageWidth The export image width.
     */
    public void setExportImageWidth(int exportImageWidth)
    {
        this.exportImageWidth = exportImageWidth;
    }

    /**
     * Specifies the image width of the ground overlay image when this surface is exported to KML. The default is 1024.
     *
     * @param exportImageHeight The export image height.
     */
    public void setExportImageHeight(int exportImageHeight)
    {
        this.exportImageHeight = exportImageHeight;
    }

    /**
     * Export the surface image to KML as a {@code <GroundOverlay>} element. The {@code output} object will receive the
     * data. This object must be one of: java.io.Writer java.io.OutputStream javax.xml.stream.XMLStreamWriter
     *
     * @param output Object to receive the generated KML.
     * @throws XMLStreamException If an exception occurs while writing the KML
     */
    @Override
    protected void exportAsKML(Object output) throws XMLStreamException
    {
        XMLStreamWriter xmlWriter = createStreamWriter(output);

        final StringBuilder imagePath = new StringBuilder();
        imagePath.append(kmlFolder)
                .append(File.separator)
                .append("AnalyticSurface")
                .append(analyticSurfaceImageNumber++)
                .append(".png");
        File file = new File(imagePath.toString());
        BufferedImage image = createImage(exportImageWidth, exportImageHeight);
        try
        {
            String suffix = WWIO.getSuffix(file.getName());
            ImageIO.write(image, suffix, file);
        } catch (IOException e)
        {
            logger.warn("Unable to export image source: {}", file.getAbsolutePath(), e);
        }

        xmlWriter.writeStartElement("GroundOverlay");

        String analyticSurfaceName = analyticSurface.getDisplayName();

        if (!analyticSurfaceName.isEmpty())
        {
            xmlWriter.writeStartElement("name");
            xmlWriter.writeCharacters(analyticSurfaceName);
            xmlWriter.writeEndElement();
        }
        // Write geometry
        writeIconHref(xmlWriter, file.getName());

        xmlWriter.writeStartElement("altitudeMode");
        xmlWriter.writeCharacters("clampToGround");
        xmlWriter.writeEndElement();

        exportKMLLatLonBox(xmlWriter);

        xmlWriter.writeEndElement(); // GroundOverlay
        xmlWriter.flush();
    }

    protected void exportKMLLatLonBox(XMLStreamWriter xmlWriter) throws XMLStreamException
    {
        Sector sector = analyticSurface.getSector();

        xmlWriter.writeStartElement("LatLonBox");
        xmlWriter.writeStartElement("north");
        xmlWriter.writeCharacters(Double.toString(sector.getMaxLatitude().getDegrees()));
        xmlWriter.writeEndElement();

        xmlWriter.writeStartElement("south");
        xmlWriter.writeCharacters(Double.toString(sector.getMinLatitude().getDegrees()));
        xmlWriter.writeEndElement(); // south

        xmlWriter.writeStartElement("east");
        xmlWriter.writeCharacters(Double.toString(sector.getMinLongitude().getDegrees()));
        xmlWriter.writeEndElement();

        xmlWriter.writeStartElement("west");
        xmlWriter.writeCharacters(Double.toString(sector.getMaxLongitude().getDegrees()));
        xmlWriter.writeEndElement(); // west
        xmlWriter.writeEndElement(); // LatLonBox
    }

    /**
     * Create an exportable image from this surface's colors and attributes. Note that only the surface's color values
     * used to draw the interior, the surface's interior material attribute is ignored. Also ignored are the surface's
     * opacity and shadow attributes.
     *
     * @param imageWidth  The width of the image to create.
     * @param imageHeight The height of the image to create.
     * @return An ARGB image reflecting this surface's color values and outline attributes.
     */
    public BufferedImage createImage(int imageWidth, int imageHeight)
    {
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);

        AnalyticSurfaceAttributes surfaceAttributes = analyticSurface.getSurfaceAttributes();

        Iterable<? extends GridPointAttributes> values = analyticSurface.getValues();
        int[] dimensions = analyticSurface.getDimensions();
        int width = dimensions[0];
        int height = dimensions[1];

        Sector sector = analyticSurface.getSector();
        if (surfaceAttributes.isDrawInterior() && values != null)
        {
            ArrayList<Color> colorGrid = new ArrayList<>(width * height);
            for (GridPointAttributes gridPoint : values)
            {
                colorGrid.add(gridPoint.getColor());
            }

            double sectorHeight = sector.getDeltaLat().radians;
            double sectorWidth = sector.getDeltaLon().radians;
            double pixelWidth = sectorHeight / imageHeight;
            double pixelHeight = sectorWidth / imageWidth;
            double colorCellWidth = sectorWidth / (width - 1);
            double colorCellHeight = sectorHeight / (height - 1);

            for (int y = 0; y < imageHeight; y++)
            {
                double lat = (y + 0.5) * pixelWidth;

                int row = (int) (lat / colorCellHeight);

                for (int x = 0; x < imageWidth; x++)
                {
                    double lon = (x + 0.5) * pixelHeight;

                    int col = (int) (lon / colorCellWidth);

                    Color sw = colorGrid.get((row + 1) * width + col);
                    Color se = colorGrid.get((row + 1) * width + col + 1);
                    Color ne = colorGrid.get(row * width + col + 1);
                    Color nw = colorGrid.get(row * width + col);

                    double s = (lon - col * colorCellWidth) / colorCellWidth;
                    double t = (lat - row * colorCellHeight) / colorCellHeight;

                    int colorInt = ImageUtil.interpolateColor(s, 1.0 - t, sw.getRGB(), se.getRGB(), nw.getRGB(),
                            ne.getRGB());
                    image.setRGB(x, y, colorInt);
                }
            }
        }

        if (surfaceAttributes.isDrawOutline())
        {
            Graphics2D g = image.createGraphics();

            g.setPaint(surfaceAttributes.getOutlineMaterial().getDiffuse());
            g.setStroke(new BasicStroke((float) surfaceAttributes.getOutlineWidth()));
            g.drawRect(0, 0, imageWidth - 1, imageHeight - 1);
        }

        return image;
    }
}
