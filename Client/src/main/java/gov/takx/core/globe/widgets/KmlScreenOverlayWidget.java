package gov.takx.core.globe.widgets;

import de.micromata.opengis.kml.v_2_2_0.ScreenOverlay;
import de.micromata.opengis.kml.v_2_2_0.Vec2;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.ogc.kml.impl.KMLUtil;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.Size;
import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.commons.graphics.BitmapDrawable;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.widgets.DrawableWidget;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.AWidget;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.kml.KmlIconFetcher;
import gov.takx.core.kml.KmlModel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;

/**
 * A widget to display a KML {@link ScreenOverlay}.
 *
 * @since 5.5
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class KmlScreenOverlayWidget extends AWidget
{
    @Inject
    private KmlIconFetcher iconFetcher;

    private final IGlobeDelegate globe;
    private final KmlModel model;
    private final ScreenOverlay screenOverlay;

    private final PropertyChangeListener viewListener = e -> updateWidget();
    private final DrawableWidget drawableWidget = new DrawableWidget();
    private final Offset imageOffset;
    private final Offset screenOffset;
    private final Size imageSize;
    private final boolean sizeIsDynamic;

    private BufferedImage originalScreenImage;
    private BufferedImage screenImage;
    private boolean initialResizeDone;
    private Rectangle lastViewport = new Rectangle();

    /**
     * @param globe         The globe this is applied to
     * @param model         KML model containing the screen overlay
     * @param screenOverlay Screen overlay this widget will represent
     * @param name          Name for this widget
     */
    public KmlScreenOverlayWidget(@Nonnull IGlobeDelegate globe, @Nonnull KmlModel model, @Nonnull ScreenOverlay screenOverlay, @Nonnull String name)
    {
        this.globe = globe;
        this.model = model;
        this.screenOverlay = screenOverlay;

        imageOffset = getOffset(screenOverlay.getOverlayXY());
        screenOffset = getOffset(screenOverlay.getScreenXY());
        imageSize = getSize(screenOverlay.getSize());

        // Determine if the size of the rendered image is dynamic, changing as the globe viewport changes
        sizeIsDynamic = Size.EXPLICIT_DIMENSION.equals(imageSize.getHeightMode()) && AVKey.FRACTION.equals(imageSize.getHeightUnits())
                || Size.EXPLICIT_DIMENSION.equals(imageSize.getWidthMode()) && AVKey.FRACTION.equals(imageSize.getWidthUnits());

        setVisible(false); // Visibility will be set later by the overlays controller based on the KML model (avoids flashing)
        setName(name);
        setTouchable(false);

        drawableWidget.setTouchable(false);
        addChildWidget(drawableWidget);

        final var wwd = globe.getWorldWindow();
        if (wwd != null) wwd.getView().addPropertyChangeListener(AVKey.VIEW, viewListener);
    }

    @PostConstruct
    private void postConstruct()
    {
        // Load the image in the background since it can involve network fetches
        iconFetcher.fetch(model, screenOverlay.getIcon(), screenOverlay.getName()).thenAccept(image -> {
            originalScreenImage = image;
            installImage(originalScreenImage);
            updateWidget(); // Now that we have the image, update the widget
        });
    }

    /**
     * Install the given image as our drawable.
     */
    private void installImage(BufferedImage image)
    {
        screenImage = image;
        drawableWidget.setDrawable(new BitmapDrawable(MarshalManager.marshal(image, BufferedImage.class, Bitmap.class)));
        drawableWidget.setSize(image.getWidth(), image.getHeight());
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
            if (visible) updateWidget();
            return true;
        }
        return false;
    }

    /**
     * Update the location and size of our image according to the new globe viewport.
     */
    @InvokeOnEDT
    private void updateWidget()
    {
        if (screenImage == null) return; // Skip this until we have an image to work with

        final var wwd = globe.getWorldWindow();
        if (wwd == null) return;

        final var viewport = wwd.getView().getViewport();

        // Only update our image size/location if the viewport changed
        if (!lastViewport.equals(viewport))
        {
            lastViewport = viewport;

            if (sizeIsDynamic)
            {
                resizeImage(viewport); // Resize first since position depends on the image size
            } else if (!initialResizeDone)
            {
                resizeImage(viewport); // Do an initial resize in case the overlay dimensions don't match the image dimensions
                initialResizeDone = true;
            }
            setWidgetPosition(viewport);
        }
    }

    /**
     * Resize our image according to the new viewport size and the screen overlay definition.
     */
    private void resizeImage(Rectangle viewport)
    {
        final var size = imageSize.compute(originalScreenImage.getWidth(), originalScreenImage.getHeight(), viewport.width, viewport.height);
        installImage(ImageUtils.getScaledInstance(originalScreenImage, size.width, size.height));
    }

    /**
     * Set the location of this widget according to the new viewport size, image size, and the screen overlay definition.
     */
    private void setWidgetPosition(Rectangle viewport)
    {
        final int viewportWidth = viewport.width;
        final int viewportHeight = viewport.height;

        final Point screenLocation;
        if (screenOffset != null)
        {
            var screenPoint = screenOffset.computeOffset(viewportWidth, viewportHeight, null, null);
            screenLocation = new Point((int) screenPoint.x, (int) (viewportHeight - screenPoint.y));
        } else
        {
            screenLocation = new Point(viewportWidth / 2, viewportHeight / 2);
        }

        final var overlayPoint = imageOffset != null
                ? imageOffset.computeOffset(screenImage.getWidth(), screenImage.getHeight(), null, null)
                : new Point.Double(screenImage.getWidth() / 2.0, screenImage.getHeight() / 2.0);

        final var widgetPoint = new Point2D.Float(screenLocation.x - (int) overlayPoint.x, screenLocation.y - (screenImage.getHeight() - (int) overlayPoint.y));
        setPoint(widgetPoint.x, widgetPoint.y);
    }

    /**
     * @return The given Vec2 converted to an {@link Offset}
     */
    @Nullable
    private static Offset getOffset(@Nullable Vec2 vec2)
    {
        return vec2 == null
                ? null
                : new Offset(vec2.getX(), vec2.getY(),
                        KMLUtil.kmlUnitsToWWUnits(vec2.getXunits().value()),
                        KMLUtil.kmlUnitsToWWUnits(vec2.getYunits().value()));
    }

    /**
     * @return The given Vec2 converted to a {@link Size}
     */
    @Nonnull
    private static Size getSize(Vec2 kmlSize)
    {
        if (kmlSize == null)
        {
            // If no size specified, use the native size
            return new Size(Size.NATIVE_DIMENSION, 0, AVKey.FRACTION, Size.NATIVE_DIMENSION, 0, AVKey.FRACTION);
        } else
        {
            Size size = new Size();
            size.setWidth(getSizeMode(kmlSize.getX()), kmlSize.getX(), KMLUtil.kmlUnitsToWWUnits(kmlSize.getXunits().value()));
            size.setHeight(getSizeMode(kmlSize.getY()), kmlSize.getY(), KMLUtil.kmlUnitsToWWUnits(kmlSize.getYunits().value()));
            return size;
        }
    }

    /**
     * Get the size mode for a size parameter. The KML size tag takes a numeric size attribute, but certain values of
     * this attribute change the interpretation of the tag.
     * <ul>
     *     <li> A value of -1 indicates to use the native dimension.</li>
     *     <li> A value of 0 indicates to maintain the aspect ratio.</li>
     *     <li> A value of n sets the value of the dimension.</li>
     * </ul>
     * Note: this was mostly copied from WW.
     *
     * @param size The KML size attribute
     *
     * @return One of {@link gov.nasa.worldwind.render.Size#NATIVE_DIMENSION}, {@link gov.nasa.worldwind.render.Size#MAINTAIN_ASPECT_RATIO},
     *         or {@link gov.nasa.worldwind.render.Size#EXPLICIT_DIMENSION}.
     */
    private static String getSizeMode(Double size)
    {
        // KML spec requires a value, but if there isn't one, use the image's native size.
        if (size == null) return Size.NATIVE_DIMENSION;

        if ((int) size.doubleValue() == -1)
        {
            return Size.NATIVE_DIMENSION;
        } else if (size == 0)
        {
            return Size.MAINTAIN_ASPECT_RATIO;
        } else
        {
            return Size.EXPLICIT_DIMENSION;
        }
    }

    @Override
    public void releaseResources()
    {
        final var wwd = globe.getWorldWindow();
        if (wwd != null) wwd.getView().removePropertyChangeListener(AVKey.VIEW, viewListener);
    }
}
