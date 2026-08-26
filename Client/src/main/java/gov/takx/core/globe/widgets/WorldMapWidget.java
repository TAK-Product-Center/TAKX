package gov.takx.core.globe.widgets;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.WorldMapLayer;
import gov.nasa.worldwind.view.orbit.OrbitView;
import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.commons.graphics.BitmapDrawable;
import gov.tak.api.widgets.IMapWidget;
import gov.tak.platform.graphics.PointF;
import gov.tak.platform.ui.MotionEvent;
import gov.tak.platform.widgets.DrawableWidget;
import gov.takx.api.globe.AWidget;
import gov.takx.api.globe.CoreWidgetNames;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IWorldWindowGL;
import jakarta.annotation.Nonnull;

import javax.imageio.ImageIO;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Widget for displaying a map overlay with the current position and visible area bounds drawn on top.
 *
 * @since 5.2
 */
public class WorldMapWidget extends AWidget
{
    private static final BasicStroke STROKE_OUTLINE_LINES = new BasicStroke(3, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL);
    private static final Color TRANSLUCENT_BLACK = new Color(0, 0, 0, 125);
    private static final int MAX_WIDTH = 512;
    private static final int MAX_HEIGHT = 256;
    private static final double MIN_VIEW_DIST = 4;
    private static final int CROSSHAIR_SIZE = 15;

    private final PropertyChangeListener listener = this::viewChanged;
    private final IGlobeDelegate globe;
    private final DrawableWidget drawableWidget;
    private final BufferedImage backgroundMap;
    private final int viewSteps = 32;

    private double currentScale;
    private int width;
    private int height;
    private int[] argb;
    private AffineTransform backgroundMapTransform;
    private BufferedImage canvas;
    private Bitmap bitmap;
    private Graphics2D graphics;
    private List<LatLon> viewPositions;

    /**
     * @param globe Target globe to draw the world map on.
     */
    public WorldMapWidget(@Nonnull IGlobeDelegate globe)
    {
        this.globe = globe;

        // Load the base earth-map image (provided by worldwind).
        try (InputStream stream = WorldMapLayer.class.getResourceAsStream("/images/earth-map-512x256.png"))
        {
            backgroundMap = ImageIO.read(stream);
        } catch (IOException ex)
        {
            throw new IllegalStateException(ex);
        }

        drawableWidget = new DrawableWidget();
        drawableWidget.setVisible(true);
        drawableWidget.setMargins(10, 10, 10, 10);
        drawableWidget.addOnClickListener(this::onClick);

        // Initialize the graphics by setting the scale for the first time.
        setScale(0.5);

        addChildWidget(drawableWidget);
        setName(CoreWidgetNames.WORLD_MAP);

        View view = globe.getWorldWindow().getView();
        view.addPropertyChangeListener(AVKey.VIEW, listener);
        setVisible(false);
    }

    @Override
    public void releaseResources()
    {
        final IWorldWindowGL wwd = globe.getWorldWindow();
        if (wwd != null)
        {
            View view = wwd.getView();
            view.removePropertyChangeListener(AVKey.VIEW, listener);
        }

        setVisible(false);
    }

    /**
     * Called when the globe view changes, updating the widget display.
     */
    private void viewChanged(PropertyChangeEvent ignored)
    {
        if (!isVisible()) return;
        computeViewFootPrint();
        update();
    }

    @Override
    public boolean setVisible(boolean visible)
    {
        boolean changed = super.setVisible(visible);
        if (changed && visible)
        {
            computeViewFootPrint();
            update();
        }
        return changed;
    }

    /**
     * Called when the map widget is clicked. Navigates to the location on the globe clicked.
     *
     * @param widget Clicked widget.
     * @param event Mouse event of the click.
     */
    private void onClick(@Nonnull IMapWidget widget, @Nonnull MotionEvent event)
    {
        if (globe.getWorldWindow().getView() instanceof OrbitView orbitView)
        {
            // Get relative coordinates of click event
            PointF widgetPos = widget.getAbsoluteWidgetPosition();
            float x = event.getX() - widgetPos.x;
            float y = event.getY() - widgetPos.y;

            // Map to position
            float latitude = (((180F * y) / height) - 90) % 90;
            float longitude = (((360F * x) / width) - 180) % 180;
            LatLon targetPos = LatLon.fromDegrees(-latitude, longitude);

            // And lastly, goto the position
            orbitView.goTo(new Position(targetPos.getLatitude(), targetPos.getLongitude(), 0), orbitView.getZoom());
        }
    }

    /**
     * Updates the widget to fit the requested scale.
     *
     * @param scale New widget scale. Values are clamped into the range {@code [0.2 - 1.0]}.
     */
    private void setScale(double scale)
    {
        if (scale == currentScale) return;
        if (scale > 1) scale = 1;
        if (scale < 0.2) scale = 0.2;
        currentScale = scale;

        // Update dimensions
        width = (int) (MAX_WIDTH * scale);
        height = (int) (MAX_HEIGHT * scale);

        // Populate the transformation we use to draw the map graphic on the screen.
        // We need to scale down the image to fit our target widget dimensions (assuming scale < 1.0).
        backgroundMapTransform = new AffineTransform();
        backgroundMapTransform.scale(scale, scale);

        // Populate the widget's backing bitmap.
        bitmap = new Bitmap(width, height);

        // Populate ARGB buffer used to transfer contents into the widget's drawable.
        argb = new int[width * height];

        // Populate the canvas/graphics we'll use to actually draw on.
        // Contents will be copied to the ARGB buffer, and then to the bitmap.
        canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        graphics = canvas.createGraphics();

        // Update the widget's size and drawable to point to the new bitmap.
        drawableWidget.setDrawable(new BitmapDrawable(bitmap));
        drawableWidget.setSize(width, height);

        // And lastly update the drawn contents
        update();
    }

    /**
     * Update the widget display.
     */
    private void update()
    {
        // Skip if we're disabled or not initialized.
        if (!isVisible() || graphics == null) return;

        // Clear display
        Graphics2D g = graphics;
        g.setBackground(TRANSLUCENT_BLACK);
        g.clearRect(0, 0, width, height);

        // Draw the background image (Equirectangular projection)
        g.drawImage(backgroundMap, backgroundMapTransform, null);

        // Draw the ring (usually, it can morph a bit) around the visible area of the globe.
        if (viewPositions != null)
        {
            g.setStroke(STROKE_OUTLINE_LINES);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Record the first position in the list. We need this for starting the first line
            // and also checking for looping around the date line for the last line drawn.
            LatLon firstPos = viewPositions.get(0);
            LatLon lastPos = firstPos;
            int firstX = (int) (width * (lastPos.getLongitude().degrees + 180) / 360);
            int firstY = (int) (height * (-lastPos.getLatitude().degrees + 90) / 180);

            // Iterate over each point, drawing a line from the last point to the current point.
            int lastX = firstX;
            int lastY = firstY;
            for (int i = 1; i < viewPositions.size() - 2; i++)
            {
                g.setColor(Color.getHSBColor((i + 0.5f) / viewSteps, 1, 1));

                LatLon curPos = viewPositions.get(i);
                int x = (int) (width * (curPos.getLongitude().degrees + 180) / 360);
                int y = (int) (height * (-curPos.getLatitude().degrees + 90) / 180);

                // Draw the line from the last point to this point, considering edge cases
                // for when the line crosses the date line (image edges in our projection)
                if (LatLon.locationsCrossDateline(lastPos, curPos))
                {
                    if (x < width / 2)
                    {
                        g.drawLine(lastX, lastY, width, y);
                        x = 0;
                    } else
                    {
                        g.drawLine(lastX, lastY, 0, y);
                        x = width;
                    }
                } else
                {
                    g.drawLine(lastX, lastY, x, y);
                }

                lastX = x;
                lastY = y;
                lastPos = curPos;
            }

            // Cap off the last line in the shape.
            if (LatLon.locationsCrossDateline(lastPos, firstPos))
            {
                if (firstX < width / 2)
                {
                    g.drawLine(lastX, lastY, width, firstY);
                } else
                {
                    g.drawLine(lastX, lastY, 0, firstY);
                }
            } else
            {
                g.drawLine(lastX, lastY, firstX, firstY);
            }
        }

        // Draw cross-hair at camera location
        Position centerPos = globe.getWorldWindow().getView().getEyePosition();
        if (centerPos != null)
        {
            float crosshairSize = (float) (CROSSHAIR_SIZE * currentScale);
            float centerX = (float) (width * (centerPos.getLongitude().degrees + 180) / 360F) - crosshairSize / 2F;
            float centerY = (float) (height * (-centerPos.getLatitude().degrees + 90) / 180F) - crosshairSize / 2F;
            g.setColor(Color.BLACK);
            g.draw(new Ellipse2D.Float(centerX, centerY, crosshairSize, crosshairSize));
            g.setColor(Color.WHITE); // We do the fill last so that we don't end up drawing just a black circle at smaller scales.
            g.fill(new Ellipse2D.Float(centerX, centerY, crosshairSize, crosshairSize));
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // Draw a border around the image
        g.setColor(Color.WHITE);
        g.drawRect(1, 1, width - 2, height - 2);

        // Update bitmap
        canvas.getRGB(0, 0, width, height, argb, 0, width);
        bitmap.setPixels(argb, 0, width, 0, 0, width, height);

        // Setting the color filter to null is the most optimal way to refresh the drawable widget
        // without allocating any new objects. Normally you'd re-wrap the bitmap, but doing this
        // still invokes a refresh even if the value does not change (always null).
        drawableWidget.setColorFilter(null);
    }

    /**
     * Computes the current positions for a ring around the camera and stores it internally. The result can be {@code null} when zoomed in too far.
     */
    private void computeViewFootPrint()
    {
        View view = globe.getWorldWindow().getView();
        Position eyePos = view.getEyePosition();

        // Normally we'd use "view.getFarClipDistance()" but it does not update beyond a specific zoom level (bug).
        // We can estimate the value pretty well by multiplying the near distance by the half the FOV.
        double farClipDistance = view.getNearClipDistance() * (view.getFieldOfView().degrees / 2);
        double elevation = eyePos.getElevation();
        Angle distance = Angle.fromRadians(Math.asin(farClipDistance / (view.getGlobe().getRadius() + elevation)));

        // Don't yield any positions if it would be too small to render anyways.
        if (distance.degrees < MIN_VIEW_DIST)
        {
            viewPositions = null;
            return;
        }

        // Compute globe positions for a ring around where the camera is located.
        double headStep = 360.0 / (double) viewSteps;
        Angle heading = view.getHeading();
        List<LatLon> positions = new ArrayList<>(viewSteps);
        for (int i = 0; i <= viewSteps; i++)
        {
            LatLon pos = LatLon.greatCircleEndPosition(eyePos, heading, distance);
            positions.add(pos);
            heading = heading.addDegrees(headStep);
        }
        viewPositions = positions;
    }
}
