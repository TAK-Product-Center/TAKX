package gov.takx.core.surfaces;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import gov.nasa.worldwind.Exportable;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.WWMath;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceAttributes;
import gov.nasa.worldwindx.examples.analytics.ExportableAnalyticSurface;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.surfaces.IAnalyticSurface;
import gov.takx.api.surfaces.IAnalyticSurfaceLegend;
import gov.takx.api.surfaces.ISurfaceAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Font;
import java.lang.invoke.MethodHandles;
import java.text.Format;

/**
 * Concrete implementation of an analytic surface.
 *
 * @since 1.4
 */
public class AnalyticSurface
        extends ExportableAnalyticSurface
        implements IAnalyticSurface, Exportable
{

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private String displayName;
    private boolean useDisplayList;

    /**
     * Constructs a new AnalyticSurface with the specified min and max corners, base altitude in meters, and grid dimensions.
     * The new AnalyticSurface has the default {@link IAnalyticSurface.GridPointAttributes}.
     *
     * @param displayName    the name of the surface (used in the layer table)
     * @param minCorner      the min corner of the surface's geographic region.
     * @param maxCorner      the max corner of the surface's geographic region.
     * @param altitude       the base altitude to place the surface at, in meters.
     * @param width          the surface grid width, in number of grid points.
     * @param height         the surface grid height, in number of grid points.
     * @param iterable       the attributes associated with each grid point.
     * @param useDisplayList use OpenGL display lists to render the surface rather than a vertex array pointer if the graphics
     *                       card fails to render correctly using vertex array pointers
     * @throws IllegalArgumentException if the minCorner or MaxCorner are {@code null}, if the width is less than 1,
     *                                  or if the height is less than 1.
     * @see #setSurfaceValues(Iterable)
     */
    public AnalyticSurface(String displayName, LatLon minCorner, LatLon maxCorner, double altitude, int width,
                           int height, Iterable<? extends IAnalyticSurface.GridPointAttributes> iterable, boolean useDisplayList)
    {
        this.useDisplayList = useDisplayList;
        setExportImageName(displayName);
        setMinCorner(minCorner);
        setMaxCorner(maxCorner);
        setAltitude(altitude);
        super.width = width;
        super.height = height;
        if (iterable == null)
        {
            setValues(createDefaultValues(width * height));
        } else
        {
            setValues(iterable);
        }
        setExpired(true);

        this.displayName = displayName;
    }

    /**
     * Constructs a new AnalyticSurface with the specified min and max corners, base altitude in meters,
     * grid dimensions, and iterable of GridPointAttributes. The iterable should contain at least
     * {@code with * height} non-null GridPointAttributes.
     *
     * @param displayName the name of the surface (used in the layer table)
     * @param minCorner   the min corner of the surface's geographic region.
     * @param maxCorner   the max corner of the surface's geographic region.
     * @param altitude    the base altitude to place the surface at, in meters.
     * @param width       the surface grid width, in number of grid points.
     * @param height      the surface grid height, in number of grid points.
     * @param iterable    the attributes associated with each grid point.
     * @throws IllegalArgumentException if the minCorner or MaxCorner are {@code null}, if the width is less than 1,
     *                                  if the height is less than 1, or if the iterable is {@code null}.
     */
    public AnalyticSurface(String displayName, LatLon minCorner, LatLon maxCorner, double altitude, int width, int height,
                           Iterable<? extends IAnalyticSurface.GridPointAttributes> iterable)
    {
        this(displayName, minCorner, maxCorner, altitude, width, height, iterable, false);
    }

    @Override
    protected void doUpdate(DrawContext dc)
    {
        referencePos = new Position(sector.getCentroid(), altitude);
        referencePoint = dc.getGlobe().computePointFromPosition(referencePos);

        if (surfaceRenderInfo == null ||
                surfaceRenderInfo.getGridWidth() != width ||
                surfaceRenderInfo.getGridHeight() != height)
        {
            surfaceRenderInfo = new RobustRenderInfo(width, height, isUseDisplayList());
        }

        updateSurfacePoints(dc, surfaceRenderInfo);
        updateSurfaceNormals(surfaceRenderInfo);
    }

    @Override
    public void deleteSurface()
    {
        if (surfaceRenderInfo instanceof RobustRenderInfo)
        {
            ((RobustRenderInfo) surfaceRenderInfo).delete();
            surfaceRenderInfo = null;
        }
    }

    /**
     * Constructs a new AnalyticSurface with the specified min and max corners, base altitude in meters, and grid dimensions.
     * The new AnalyticSurface has the default {@link IAnalyticSurface.GridPointAttributes}.
     *
     * @param displayName the name of the surface (used in the layer table)
     * @param minCorner   the min corner of the surface's geographic region.
     * @param maxCorner   the max corner of the surface's geographic region.
     * @param altitude    the base altitude to place the surface at, in meters.
     * @param width       the surface grid width, in number of grid points.
     * @param height      the surface grid height, in number of grid points.
     * @throws IllegalArgumentException if the minCorner or MaxCorner are {@code null}, if the width is less than 1,
     *                                  or if the height is less than 1.
     * @see #setSurfaceValues(Iterable)
     */
    public AnalyticSurface(String displayName, LatLon minCorner, LatLon maxCorner, double altitude, int width, int height)
    {
        this(displayName, minCorner, maxCorner, altitude, width, height, null, false);
    }

    public AnalyticSurface()
    {
        super();
    }

    /**
     * Set the base altitude of the analytic surface.
     *
     * @param altitude the new base altitude, in meters.
     * @since 1.5
     */
    @Override
    public void setAltitude(double altitude)
    {
        super.setAltitude(altitude);
    }

    @Override
    public void setAltitudeMode(int mode)
    {
        super.setAltitudeMode(mode);
    }

    /**
     * @return the base altitude of the analytic surface
     * @since 1.5
     */
    @Override
    public double getAltitude()
    {
        return super.getAltitude();
    }

    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;

        // TODO: wire things up so this causes a name change in the layer table
    }

    @Override
    public LatLon getMinCorner()
    {
        return LatLon.fromDegrees(sector.getMinLatitude().degrees, sector.getMinLongitude().degrees);
    }

    @Override
    public void setMinCorner(LatLon corner)
    {
        // Merge the new min corner data into the existing sector definition
        Sector oldSector = getSector();
        Sector newSector = Sector.fromDegrees(corner.getLatitude().degrees, oldSector.getMaxLatitude().degrees,
                corner.getLongitude().degrees, oldSector.getMaxLongitude().degrees);
        setSector(newSector);
    }

    @Override
    public LatLon getMaxCorner()
    {
        return LatLon.fromDegrees(sector.getMaxLatitude().degrees, sector.getMaxLongitude().degrees);
    }

    @Override
    public void setMaxCorner(LatLon corner)
    {
        // Merge the new max corner data into the existing sector definition
        Sector oldSector = getSector();
        Sector newSector = Sector.fromDegrees(oldSector.getMinLatitude().degrees, corner.getLatitude().degrees,
                oldSector.getMinLongitude().degrees, corner.getLongitude().degrees);
        setSector(newSector);
    }

    /**
     * Sets this surface's geographic boundary, as a {@link Sector}.  Override this so we can force
     * invocation onto the EDT.
     *
     * @param sector this surface's new geographic boundary, as a Sector.
     */
    @Override
    @InvokeOnEDT
    public void setSector(final Sector sector)
    {
        super.setSector(sector);
    }

    @Override
    @InvokeOnEDT
    public void setSurfaceValues(final Iterable<? extends IAnalyticSurface.GridPointAttributes> iterable)
    {
        super.setValues(iterable);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterable<? extends IAnalyticSurface.GridPointAttributes> getSurfaceValues()
    {
        // Yes, this cast is really ugly.  But trying to facade all the WW classes is difficult
        // given the way they were written.  So, we will force the developers to use our GridPointAttributes
        // type and this cast should always be valid.  Well, until it breaks anyway. ;-)
        return (Iterable<? extends IAnalyticSurface.GridPointAttributes>) super.getValues();
    }

    @InvokeOnEDT
    @Override
    public void setAnalyticSurfaceAttributes(final ISurfaceAttributes attributes)
    {
        super.setSurfaceAttributes(((SurfaceAttributes) attributes).attributes);
        if (isUseDisplayList())
        {
            if (surfaceRenderInfo != null && surfaceRenderInfo instanceof RobustRenderInfo)
            {
                ((RobustRenderInfo) surfaceRenderInfo).delete();
            }
        }
    }

    @Override
    public void refreshSurface()
    {
        if (getClientLayer() != null)
        {
            getClientLayer().firePropertyChange(AVKey.LAYER, null, getClientLayer());
        }
    }

    @Override
    public IAnalyticSurface.GridPointAttributes createColorGradientGridPointAttributes(final double value, double minValue, double maxValue, double minHue, double maxHue)
    {
        double hueFactor = WWMath.computeInterpolationFactor(value, minValue, maxValue);
        Color color = Color.getHSBColor((float) WWMath.mixSmooth(hueFactor, minHue, maxHue), 1f, 1f);
        double opacity = WWMath.computeInterpolationFactor(value, minValue, minValue + (maxValue - minValue) * 0.1);
        Color rgbaColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (255 * opacity));

        return createSurfaceGridPointAttributes(value, rgbaColor);
    }

    /**
     * Returns a new instance of {@link IAnalyticSurface.GridPointAttributes} with the specified value and color.
     *
     * @param value the new GridPointAttributes' value.
     * @param color the new GridPointAttributes' color.
     * @return a new GridPointAttributes defined by the specified value and color.
     */
    public IAnalyticSurface.GridPointAttributes createSurfaceGridPointAttributes(final double value, final Color color)
    {
        return new IAnalyticSurface.GridPointAttributes()
        {
            public double getValue()
            {
                return value;
            }

            public Color getColor()
            {
                return color;
            }
        };
    }

    public ISurfaceAttributes getAnalyticSurfaceAttributes()
    {
        // Yes, this cast is really ugly.  See the note in getValues above.
        // return (AnalyticSurfaceAttributes) super.getSurfaceAttributes();

        // Worse than being ugly, this cast really won't work:
        // java.lang.ClassCastException:
        // gov.nasa.worldwind.examples.analytics.AnalyticSurfaceAttributes cannot be cast to gov.takx.api.surfaces.AnalyticSurfaceAttributes

        AnalyticSurfaceAttributes attribs = super.getSurfaceAttributes();
        return new SurfaceAttributes(attribs);
    }

    @Override
    public IAnalyticSurfaceLegend createLegendFromColorGradient(double minValue, double maxValue, double minHue,
                                                                double maxHue,
                                                                Iterable<? extends IAnalyticSurfaceLegend.LabelAttributes> labels,
                                                                IAnalyticSurfaceLegend.LabelAttributes titleLabel)
    {

        AnalyticSurfaceLegend legend = AnalyticSurfaceLegend.fromColorGradient(minValue, maxValue, minHue, maxHue, labels, titleLabel);
        ((RenderableLayer) getClientLayer()).addRenderable(legend);

        return legend;
    }

    @Override
    public IAnalyticSurfaceLegend createLegendFromColorGradient(int width, int height, double minValue, double maxValue,
                                                                double minHue, double maxHue, Color borderColor,
                                                                Iterable<? extends IAnalyticSurfaceLegend.LabelAttributes> labels,
                                                                IAnalyticSurfaceLegend.LabelAttributes titleLabel)
    {

        return AnalyticSurfaceLegend.fromColorGradient(width, height, minValue, maxValue, minHue, maxHue, borderColor, labels, titleLabel);
    }

    @Override
    public Iterable<? extends IAnalyticSurfaceLegend.LabelAttributes> createDefaultColorGradientLabels(double minValue,
                                                                                                       double maxValue,
                                                                                                       Format format)
    {

        return AnalyticSurfaceLegend.createDefaultColorGradientLabels(minValue, maxValue, format);
    }

    @Override
    public IAnalyticSurfaceLegend.LabelAttributes createDefaultTitle(String text)
    {
        return AnalyticSurfaceLegend.createDefaultTitle(text);
    }

    @Override
    public IAnalyticSurfaceLegend.LabelAttributes createLegendLabelAttributes(double value, String text, Font font,
                                                                              Color color, double xOffset, double yOffset)
    {

        return AnalyticSurfaceLegend.createLegendLabelAttributes(value, text, font, color, xOffset, yOffset);
    }

    public boolean isUseDisplayList()
    {
        return useDisplayList;
    }

    /**
     * This is an enhanced RenderInfo class supports the rendering of the interior elements as OpenGL display lists
     * as well as the vertex array pointer. This capability was added to deal with rendering problems that occurs on
     * some Intel HD graphics cards when a 2D shape is selected after an analytic surface is displayed.
     */
    protected static class RobustRenderInfo extends AnalyticSurface.RenderInfo
    {

        private boolean useDisplayList = false;
        GL2 glContext;
        int listIndex = 0;

        public RobustRenderInfo(int gridWidth, int gridHeight, boolean useDisplayList)
        {
            super(gridWidth, gridHeight);
            this.useDisplayList = useDisplayList;
        }

        public boolean isUseDisplayList()
        {
            return useDisplayList;
        }

        public void delete()
        {
            if (isUseDisplayList() && glContext != null && listIndex > 0)
            {
                try
                {
                    glContext.glDeleteLists(listIndex, 1);
                } catch (Exception e)
                {
                    logger.warn("Error deleting display list: {} due to {}", listIndex, e.getMessage());
                } finally
                {
                    listIndex = 0;
                }
            }
        }

        @Override
        public void drawInterior(DrawContext dc)
        {
            if (isUseDisplayList())
            {
                drawInteriorAsDisplayList(dc);
            } else
            {
                drawInteriorFast(dc);
            }
        }

        private void drawInteriorFast(DrawContext dc)
        {
            GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
            gl.glDrawElements(GL.GL_TRIANGLE_STRIP, interiorIndexBuffer.remaining(), GL.GL_UNSIGNED_INT, interiorIndexBuffer);
        }

        private void drawInteriorAsDisplayList(DrawContext dc)
        {
            GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
            if (glContext == null)
            {
                glContext = gl;
            }
            if (listIndex == 0)
            {
                listIndex = gl.glGenLists(1);
                if (listIndex > 0)
                {
                    gl.glNewList(listIndex, GL2.GL_COMPILE);
                    drawInteriorElements(dc);
                    gl.glEndList();
                }
            } else
            {
                gl.glCallList(listIndex);
            }
        }

        private void drawInteriorElements(DrawContext dc)
        {
            GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
            gl.glBegin(GL.GL_TRIANGLE_STRIP);
            int size = interiorIndexBuffer.remaining();
            for (int i = 0; i < size; i++)
            {
                int index = interiorIndexBuffer.get(i);
                int i4 = index * 4;
                byte r = colorBuffer.get(i4);
                byte g = colorBuffer.get(i4 + 1);
                byte b = colorBuffer.get(i4 + 2);
                byte a = colorBuffer.get(i4 + 3);
                gl.glColor4ub(r, g, b, a);
                int i3 = index * 3;
                float x = geographicVertexBuffer.get(i3);
                float y = geographicVertexBuffer.get(i3 + 1);
                float z = geographicVertexBuffer.get(i3 + 2);
                gl.glVertex3f(x, y, z);
            }
            gl.glEnd();
        }

        @Override
        public void drawOutline(DrawContext dc)
        {
            GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
            gl.glDrawElements(GL.GL_LINE_LOOP, outlineIndexBuffer.remaining(), GL.GL_UNSIGNED_INT, outlineIndexBuffer);
        }
    }
}
