package gov.takx.core.globe.widgets;

import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.LegacyAdapters;
import com.atakmap.map.MapCamera;
import com.atakmap.map.MapSceneModel;
import com.atakmap.map.layer.control.SurfaceRendererControl;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import com.atakmap.math.PointD;
import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.commons.graphics.BitmapDrawable;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.widgets.IMapWidget;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.util.LimitingThread;
import gov.tak.platform.view.Gravity;
import gov.tak.platform.widgets.DrawableWidget;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.globe.AWidget;
import gov.takx.api.globe.CoreWidgetNames;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.platform.ui.FontUtils;
import jakarta.annotation.Nonnull;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Widget that displays the elevation heatmap legend for the {@link gov.takx.core.globe.tak.layer.HeatMapLayer HeatMapLayer}.
 * <p/>
 * This includes a color gradient bar and elevation values colored according to the bar colors to give values to the
 * isoplethic heatmap areas delineating areas of similar elevation.
 * 
 * @since 5.8
 */
public class IsoKeyWidget extends AWidget implements MapRenderer.OnCameraChangedListener
{
    private static final Font TOP_LABEL_FONT = FontUtils.DEFAULT_FONT.deriveFont(Font.BOLD, FontUtils.DEFAULT_FONT_SIZE);
    private static final Font SCALE_LABEL_FONT = FontUtils.DEFAULT_FONT.deriveFont(Font.BOLD, FontUtils.DEFAULT_FONT_SIZE);

    // scale generation
    private static final int ISO_SCALE_MARKS = 20; // number of segments in the iso bar
    private static final int ISO_SCALE_START = 5; // starting default for minHeat for iso bar
    private static final int ISO_SCALE_INCREMENTS = 25; // increments between altitude iso lines
    private static final int BAR_WIDTH = 20;
    private static final int BAR_HEIGHT = 200;
    private static final int BUFFER_SPACE = 5;
    private static final int NUM_LABELS = 6;
    private static final int KEY_HEIGHT = BAR_HEIGHT / ISO_SCALE_MARKS;
    private static final String RELATIVE = "RELATIVE";
    // TODO: TAKX-4455, implement absolute and gradient features
    private static final String ABSOLUTE = "ABSOLUTE";
    private static final String GRADIENT_ABSOLUTE = "ABS GRADIENT";
    private static final String GRADIENT_RELATIVE = "REL GRADIENT";
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    private final IGlobeDelegate globe;
    private final LimitingThread updateTaskThread = new LimitingThread("IsoKeyWidget.UpdateLegend", () -> {
        updateLegendImmediate();
        updateWidget();
    });

    // TODO: TAKX-4455, implement absolute and gradient features
    private String isoDisplayMode = RELATIVE;
    private boolean autoComputeMinMaxHeat = true;
    private double minHeat = ISO_SCALE_START;
    private double maxHeat = (ISO_SCALE_MARKS * ISO_SCALE_INCREMENTS) + ISO_SCALE_START;

    private BufferedImage legendImage;
    private BufferedImage barImage; // bar image stays the same no matter the current view
    private int drawVersion = -1;
    private int terrainVersion = -1;

    public IsoKeyWidget(@Nonnull IGlobeDelegate globe)
    {
        this.globe = globe;
        setGravity(Gravity.CENTER);
        setOrientation(VERTICAL);
        setName(CoreWidgetNames.ELEVATION_HEATMAP);
        setPadding(4, 0, 0, 15);
        setVisible(false);
        globe.getPortableMapRenderer().addOnCameraChangedListener(this);
        createBarGraphic();
    }

    @Override
    public void releaseResources()
    {
        updateTaskThread.dispose();
    }

    @Override
    public boolean showInWidgetList()
    {
        return false;
    }

    @Override
    public boolean setVisible(boolean visible)
    {
        if (super.setVisible(visible))
        {
            if (visible)
            {
                updateLegendImmediate();
                updateWidget();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onCameraChanged(MapRenderer renderer)
    {
        updateLegendQueue();
    }

    /**
     * Update the widget with the most recent version of the legend.
     */
    @InvokeOnEDT
    private void updateWidget()
    {
        if (!isVisible() || legendImage == null) return;
        BitmapDrawable legendBitmap = new BitmapDrawable(MarshalManager.marshal(legendImage, BufferedImage.class, Bitmap.class));

        List<IMapWidget> children = getChildren();
        DrawableWidget legendWidget;
        if (!children.isEmpty()) // update existing widget
        {
            legendWidget = (DrawableWidget) children.get(0);
            legendWidget.setDrawable(legendBitmap);
        } else // no inner widget, initialize one
        {
            legendWidget = new DrawableWidget(legendBitmap);
            addChildWidgetAt(0, legendWidget);
        }
        legendWidget.setSize(legendImage.getWidth(), legendImage.getHeight()); // widget size may vary slightly depending on elevation numbers
        recalcSize();
    }

    /**
     * Queue a request to update the legend and widget.
     */
    private void updateLegendQueue()
    {
        if (isVisible()) updateTaskThread.exec();
    }

    /**
     * Immediately update the legend. Query all the elevation data and create the appropriate graphics.
     */
    private void updateLegendImmediate()
    {
        if (!isVisible()) return;
        updateMinMaxHeat();
        if (!GeoPoint.isAltitudeValid(minHeat) || !GeoPoint.isAltitudeValid(maxHeat)) return;

        // draw scale labels
        BufferedImage scaleLabelsImage = new BufferedImage(300, BAR_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        int scaleLabelsWidth = -1;
        Graphics2D scaleGraphics = scaleLabelsImage.createGraphics();
        FontMetrics scaleFontMetrics = scaleGraphics.getFontMetrics();
        scaleGraphics.setFont(SCALE_LABEL_FONT);
        int labelHeight = (BAR_HEIGHT - scaleFontMetrics.getHeight()) / (NUM_LABELS - 1) + 1;
        double curElev = minHeat;
        double incElev = (maxHeat - minHeat) / (NUM_LABELS - 1);

        for (int j = 0; j < NUM_LABELS; j++)
        {
            int yPos = BAR_HEIGHT - j * labelHeight - scaleFontMetrics.getMaxDescent();
            Color labelColor = colorFromElevation(curElev, minHeat, maxHeat);
            String elev = String.format("%.0f", SpanUtilities.convert(curElev, Span.METER, Span.FOOT));
            scaleLabelsWidth = Math.max(scaleLabelsWidth, scaleFontMetrics.stringWidth(elev));
            drawShadowedString(scaleGraphics, elev, labelColor, yPos);
            curElev += incElev;
        }
        scaleLabelsImage = scaleLabelsImage.getSubimage(0, 0, scaleLabelsWidth, BAR_HEIGHT);
        scaleGraphics.dispose();

        // draw topLabels
        BufferedImage topLabelImage = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D topLabelGraphics = topLabelImage.createGraphics();
        topLabelGraphics.setFont(TOP_LABEL_FONT);
        FontMetrics topLabelFontMetrics = topLabelGraphics.getFontMetrics();
        int topLabelHeight = topLabelFontMetrics.getHeight();
        drawShadowedString(topLabelGraphics, isoDisplayMode, Color.WHITE, topLabelHeight);
        drawShadowedString(topLabelGraphics, "(ft msl)", Color.WHITE, 2 * topLabelHeight);
        topLabelImage = topLabelImage.getSubimage(0, 0, topLabelFontMetrics.stringWidth(isoDisplayMode) + 1, topLabelHeight * 2 + 1);
        topLabelGraphics.dispose();

        // stitch images together
        int topLabelImageHeight = topLabelImage.getHeight();
        int width = Math.max(topLabelImage.getWidth(), scaleLabelsWidth + barImage.getWidth() + BUFFER_SPACE);
        legendImage = new BufferedImage(width, topLabelImageHeight + barImage.getHeight() + BUFFER_SPACE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D legendGraphics = legendImage.createGraphics();
        legendGraphics.drawImage(topLabelImage, 0, 0, null);
        legendGraphics.drawImage(barImage, 0, topLabelImageHeight + BUFFER_SPACE, null);
        legendGraphics.drawImage(scaleLabelsImage, barImage.getWidth() + BUFFER_SPACE, topLabelImageHeight + BUFFER_SPACE, null);
        legendGraphics.dispose();
    }

    /**
     * Query the screen position and all visible tiles to determine the min and max elevation visible.
     * <p/>
     * Must be {@code @InvokeOnEdt} or it will not function.
     */
    @InvokeOnEDT
    private void updateMinMaxHeat()
    {
        GLMapView orthoView = LegacyAdapters.adapt(globe.getPortableMapRenderer());
        final int terrainVersion = orthoView.getTerrainVersion();
        final SurfaceRendererControl surfaceControl = orthoView.getControl(SurfaceRendererControl.class);

        if (autoComputeMinMaxHeat && surfaceControl != null && (orthoView.currentScene.drawVersion != this.drawVersion || terrainVersion != this.terrainVersion))
        {
            this.drawVersion = orthoView.currentScene.drawVersion;
            this.terrainVersion = terrainVersion;

            MapSceneModel scene = orthoView.currentScene.scene;
            MapCamera camera = scene.camera;
            final PointD nominalMetersScale = new PointD(scene.displayModel.projectionXToNominalMeters,
                    scene.displayModel.projectionYToNominalMeters, scene.displayModel.projectionZToNominalMeters);

            final double camtgt = MathUtils.distance(camera.location.x * nominalMetersScale.x, camera.location.y * nominalMetersScale.y, camera.location.z * nominalMetersScale.z,
                    camera.target.x * nominalMetersScale.x, camera.target.y * nominalMetersScale.y, camera.target.z * nominalMetersScale.z);

            // establish min/max
            double minEl = Double.NaN;
            double maxEl = Double.NaN;
            for (Envelope aabbWgs84 : surfaceControl.getSurfaceBounds())
            {
                // exclude any tiles that are sufficiently far away
                PointD tileCentroid = new PointD(0d, 0d, 0d);
                scene.mapProjection.forward(new GeoPoint((aabbWgs84.minY + aabbWgs84.maxY) / 2.0,
                        (aabbWgs84.minX + aabbWgs84.maxX) / 2.0, (aabbWgs84.minZ + aabbWgs84.maxZ) / 2.0),
                        tileCentroid);
                final double slant = MathUtils.distance(
                        camera.location.x * nominalMetersScale.x, camera.location.y * nominalMetersScale.y, camera.location.z * nominalMetersScale.z,
                        tileCentroid.x * nominalMetersScale.x, tileCentroid.y * nominalMetersScale.y, tileCentroid.z * nominalMetersScale.z);

                PointD tileMin = new PointD(0d, 0d, 0d);
                scene.mapProjection.forward(new GeoPoint(aabbWgs84.minY, aabbWgs84.minX, aabbWgs84.minZ), tileMin);
                final double radius = MathUtils.distance(tileCentroid.x * nominalMetersScale.x, tileCentroid.y * nominalMetersScale.y, tileCentroid.z * nominalMetersScale.z,
                        tileMin.x * nominalMetersScale.x, tileMin.y * nominalMetersScale.y, tileMin.z * nominalMetersScale.z);

                if (Math.log((slant - radius) / camtgt) / Math.log(2.0) > 3.0) continue; // Parity with ATAK
                final double skirtHeight = 500.0;
                if (Double.isNaN(minEl) || (aabbWgs84.minZ + skirtHeight) < minEl) minEl = aabbWgs84.minZ + skirtHeight;
                if (Double.isNaN(maxEl) || aabbWgs84.maxZ > maxEl) maxEl = aabbWgs84.maxZ;
            }
            if (!Double.isNaN(minEl)) minHeat = minEl;
            if (!Double.isNaN(maxEl)) maxHeat = maxEl;
        }
    }

    /**
     * Utility method to convert an elevation to a specific HSV color.
     * @param elev the elevation to compare to minHeat and maxHeat
     * @param minHeat the minimum elevation to consider
     * @param maxHeat the maximum elevation to consider
     * @return the RGB color of that elevation mapped between minHeat and maxHeat
     */
    private Color colorFromElevation(double elev, double minHeat, double maxHeat)
    {
        // Special case for NaN (no elevation data available), completely transparent
        if (Double.isNaN(elev)) return TRANSPARENT;

        // Use HSV because it's convenient for hue to represent elevation as it spans the entire primary color spectrum with a single field
        // rather than the three separate fields in RGB.
        //
        // Normalize using (elevation - minElev) / (maxElev - minElev), puts elevation in the [0-1] range
        float hue = (float) ((elev - minHeat) / (maxHeat - minHeat));
        hue = (1f - hue) * (255f / 360f); // Flip so red is the highest elevation, blue is lowest. Hue is 0-360 but red flips at 255, so clamp
        return Color.getHSBColor(hue, 1, 1);
    }

    /**
     * Utility method to draw a string with a 1px offset black shadow to the provided graphics context. Assumes drawing on left (x=0) side of image.
     * @param g the graphics context to draw on
     * @param text the text to draw
     * @param textColor the top layer color of the text
     * @param y the y-position to draw the string at
     */
    private void drawShadowedString(Graphics2D g, String text, Color textColor, int y)
    {
        g.setColor(Color.BLACK);
        g.drawString(text, 1, y + 1);
        g.setColor(textColor);
        g.drawString(text, 0, y);
    }

    /**
     * Create the bar graphic with generic data, as real data is not required.
     */
    private void createBarGraphic()
    {
        var maxHeat = 100;
        var minHeat = 0;
        barImage = new BufferedImage(BAR_WIDTH, BAR_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D barGraphics = barImage.createGraphics();
        var keyY = KEY_HEIGHT;
        var heatDiff = maxHeat - minHeat;
        for (int x = 0; x < ISO_SCALE_MARKS; x++)
        {
            double keyAlt = ((double) x / (double) IsoKeyWidget.ISO_SCALE_MARKS) * heatDiff + minHeat;
            barGraphics.setColor(colorFromElevation(keyAlt, minHeat, maxHeat));
            barGraphics.fillRect(0, BAR_HEIGHT - keyY, BAR_WIDTH, KEY_HEIGHT);
            keyY += KEY_HEIGHT;
        }
        barGraphics.dispose();
    }
}
