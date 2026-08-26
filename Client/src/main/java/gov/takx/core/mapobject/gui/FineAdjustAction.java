package gov.takx.core.mapobject.gui;

import com.atakmap.map.Globe;
import com.atakmap.map.elevation.ElevationManager;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.util.RestorableSupport;
import gov.nasa.worldwind.view.ViewPropertyLimits;
import gov.nasa.worldwind.view.orbit.OrbitView;
import gov.nasa.worldwind.view.orbit.OrbitViewLimits;
import gov.tak.api.commons.graphics.Font;
import gov.tak.api.commons.graphics.FontMetrics;
import gov.tak.api.commons.graphics.TextFormat;
import gov.tak.api.engine.map.IMapRendererEnums;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.engine.map.MapSceneModel;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.platform.widgets.TextWidget;
import gov.takx.api.globe.AWidget;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IWidgetInstance;
import gov.takx.api.globe.IWidgetLayerManager;
import gov.takx.api.globe.IWidgetLayerManager.IWidgetFactory;
import gov.takx.api.globe.WidgetLayoutPosition;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.swing.EscapeKeyListener;
import gov.takx.core.globe.ApplicationGlobe;
import gov.takx.core.globe.tak.layer.MapTargetBubble;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.awt.Color;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows a magnified view of the globe with a cross-hair for the user to use to precisely move map objects.
 *
 * @since 5.1.0
 */
@ApplicationScoped
public class FineAdjustAction
{
    private static final int CONFIRM_COLOR = 0xFF9EFFAF; // Pale green, sampled from ATAK
    private static final int CANCEL_COLOR = 0xFFFF9D9C; // Pale red, sampled from ATAK

    private final IWidgetLayerManager widgetLayerManager;
    private final IExtendedGlobeManager globeManager;

    private final KeyListener keyListener = new EscapeKeyListener(this::hide);

    private IWidgetFactory confirmWidgetFactory;
    private IWidgetFactory cancelWidgetFactory;
    private final List<IWidgetInstance> widgetsMadeInvisible = new ArrayList<>();

    private ApplicationGlobe applicationGlobe;
    private IMapObject targetMapObject;
    private volatile boolean active;

    private int viewMargin;
    private RestorableSupport viewLimits;
    private MapTargetBubble mapTargetBubble;
    private float savedPanScrollSpeed;

    @Inject
    FineAdjustAction(IWidgetLayerManager widgetLayerManager, IExtendedGlobeManager globeManager)
    {
        this.widgetLayerManager = widgetLayerManager;
        this.globeManager = globeManager;
    }

    /**
     * Activate the operation.
     *
     * @param applicationGlobe Globe on which to operate
     * @param targetMapObject The map object to operate upon
     */
    public void activate(@Nonnull ApplicationGlobe applicationGlobe, @Nonnull IMapObject targetMapObject)
    {
        if (active) return; // Only allow one invocation at a time
        active = true;

        this.targetMapObject = targetMapObject;
        this.applicationGlobe = applicationGlobe;

        // only show buttons on the map the adjustment is happening on
        confirmWidgetFactory = globeDelegate -> globeDelegate.equals(applicationGlobe) ? new ConfirmWidget() : null;
        cancelWidgetFactory = globeDelegate -> globeDelegate.equals(applicationGlobe) ? new CancelWidget() : null;

        show();
    }

    private void show()
    {
        MapRenderer<?> renderer = applicationGlobe.getPortableMapRenderer();
        MapSceneModel mapSceneModel = renderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);
        int threeQuartersHeight = 3 * mapSceneModel.height / 4;
        int threeQuartersWidth = 3 * mapSceneModel.width / 4;
        int squareSide = Math.min(threeQuartersWidth, threeQuartersHeight);

        // Record the view margin so our control widgets can determine which size to use
        viewMargin = (mapSceneModel.width - threeQuartersWidth) / 2;

        int x = (int) (mapSceneModel.focusx - squareSide / 2f);
        int y = (int) (mapSceneModel.focusy - squareSide / 2f);

        // the perspective camera has additional effective zoom based on the
        // AGL. Since the bubble is always an ortho rendering, we will need to
        // adjust the scale factor to account for this.
        double bubbleResolution = mapSceneModel.gsd;
        if (mapSceneModel.camera.perspective)
        {
            GeoPoint cam = GeoPoint.createMutable();
            if (mapSceneModel.mapProjection.inverse(mapSceneModel.camera.location, cam))
            {
                final double localElevation = ElevationManager.getElevation(cam.getLatitude(), cam.getLongitude(), null);
                if (!Double.isNaN(localElevation))
                {
                    bubbleResolution = Math.abs(cam.getAltitude() - localElevation) * Math.tan(mapSceneModel.camera.fov / 2d) / (mapSceneModel.height / 2d);
                }
            }
        }
        bubbleResolution /= 4d;
        double scale = Globe.getMapScale(mapSceneModel.dpi, bubbleResolution);
        mapTargetBubble = new MapTargetBubble(renderer, applicationGlobe.getNativeGlobe().getLayers(), x, y, squareSide, squareSide, scale);
        applicationGlobe.getNativeGlobe().addLayer(0, mapTargetBubble);

        // Set the center, tilt, and heading to nadir/north
        var viewController = applicationGlobe.getGlobeViewController();
        var location = targetMapObject.getLocation();
        viewController.centerOn(location.getLatitude(), location.getLongitude(), location.getAltitude(), false);
        viewController.setTilt(0, false);
        viewController.setHeading(0, false);

        // Save current limits, and then restrict zoom, tilt, heading
        OrbitView view = (OrbitView) applicationGlobe.getWorldWindow().getView();
        OrbitViewLimits viewPropertyLimits = (OrbitViewLimits) view.getViewPropertyLimits();
        viewLimits = RestorableSupport.newRestorableSupport();
        viewPropertyLimits.getRestorableState(viewLimits, null);
        viewPropertyLimits.setHeadingLimits(Angle.ZERO, Angle.ZERO);
        viewPropertyLimits.setPitchLimits(Angle.ZERO, Angle.ZERO);
        viewPropertyLimits.setZoomLimits(view.getZoom(), view.getZoom());

        savedPanScrollSpeed = applicationGlobe.getViewInputHandler().getPanScrollSpeed();
        applicationGlobe.getViewInputHandler().setPanScrollSpeed(0.1f);
        applicationGlobe.getWorldWindow().addKeyListener(keyListener);

        globeManager.setSelectListenersActive(false);

        hideOtherWidgets();
        setWidgetsVisible(true);
    }

    private void hide()
    {
        if (!active) return;

        applicationGlobe.getNativeGlobe().removeLayer(mapTargetBubble);
        mapTargetBubble = null;
        View view = applicationGlobe.getWorldWindow().getView();
        ViewPropertyLimits viewPropertyLimits = view.getViewPropertyLimits();
        viewPropertyLimits.restoreState(viewLimits, null);
        viewLimits = null;
        applicationGlobe.getViewInputHandler().setPanScrollSpeed(savedPanScrollSpeed);
        applicationGlobe.getWorldWindow().removeKeyListener(keyListener);
        setWidgetsVisible(false);
        restoreOtherWidgets();
        globeManager.setSelectListenersActive(true);
        active = false;
    }

    private synchronized void setWidgetsVisible(boolean visible)
    {
        if (visible)
        {
            widgetLayerManager.addWidgetFactory(confirmWidgetFactory, WidgetLayoutPosition.LEFT_EDGE);
            widgetLayerManager.addWidgetFactory(cancelWidgetFactory, WidgetLayoutPosition.RIGHT_EDGE);
        } else
        {
            widgetLayerManager.removeWidgetFactory(confirmWidgetFactory);
            widgetLayerManager.removeWidgetFactory(cancelWidgetFactory);
            widgetLayerManager.removeWidgets(ConfirmWidget.class);
            widgetLayerManager.removeWidgets(CancelWidget.class);
        }
    }

    /**
     * Hide all other widgets on the globe, recording which ones were affected.
     */
    private void hideOtherWidgets()
    {
        widgetLayerManager.getAllWidgetInstances().forEach(instance -> {
            if (instance.isVisible())
            {
                widgetsMadeInvisible.add(instance);
                instance.setVisible(false);
            }
        });
    }

    /**
     * Restore all widgets that were hidden by {@link #hideOtherWidgets()}.
     */
    private void restoreOtherWidgets()
    {
        widgetsMadeInvisible.forEach(instance -> instance.setVisible(true));
        widgetsMadeInvisible.clear();
    }

    private void apply()
    {
        final double latitude = mapTargetBubble.getLatitude();
        final double longitude = mapTargetBubble.getLongitude();
        final double elevation = ElevationManager.getElevation(latitude, longitude, null);
        var gp = new gov.tak.api.maps.coords.GeoPoint(latitude, longitude, elevation);
        targetMapObject.setLocation(gp, true);
        hide();
    }

    private void cancel()
    {
        hide();
    }

    /**
     * Common base for control buttons.
     * @since 5.2
     */
    private class ControlWidget extends AWidget
    {
        private static final Font BIG_FONT = new Font("Segoe UI", Font.Style.Normal, 48);
        private static final FontMetrics BIG_FONT_METRICS = new FontMetrics(BIG_FONT);

        private static final Font SMALL_FONT = new Font("Segoe UI", Font.Style.Normal, 32);
        private static final FontMetrics SMALL_FONT_METRICS = new FontMetrics(SMALL_FONT);

        private static final String TEMPLATE_TEXT = "Confirm";
        private static final float SMALL_THRESHOLD = BIG_FONT_METRICS.measureTextWidth(TEMPLATE_TEXT) / 2;

        /**
         * @param text Text to display, centered in button
         * @param backgroundColor button background color
         * @param handler Handler to run when the button is clicked
         */
        ControlWidget(String text, int backgroundColor, Runnable handler)
        {
            // If the view margin (the distance between the map border and the outer edge of the magnifier) is less
            // than our small threshold, use a smaller configuration for the text widget.
            final var widget = makeTextWidget(text, backgroundColor, viewMargin < SMALL_THRESHOLD);
            widget.addOnClickListener((w, e) -> handler.run());
            setName(text + " Button");
            addChildWidget(widget);
        }

        /**
         * @return Configured text widget, if {@code small} is {@code true} use a smaller font for the text.
         */
        private TextWidget makeTextWidget(String text, int backgroundColor, boolean small)
        {
            final var font = small ? SMALL_FONT : BIG_FONT;
            final var metrics = small ? SMALL_FONT_METRICS : BIG_FONT_METRICS;

            final var widget = new TextWidget(text, new TextFormat(font, 0), backgroundColor);
            widget.setColor(Color.BLACK.getRGB());

            final float width = metrics.measureTextWidth(TEMPLATE_TEXT) + 16;
            final float height = metrics.measureTextHeight(TEMPLATE_TEXT);

            widget.setHeight(height);
            final float textWidth = metrics.measureTextWidth(text);
            final float padding = (width - textWidth) / 2; // To center the text
            widget.setWidth(width - padding * 2);
            widget.setPadding(padding, 8, padding, 8);
            return widget;
        }

        @Override
        public void releaseResources()
        {
            //
        }
    }

    private class ConfirmWidget extends ControlWidget
    {
        ConfirmWidget()
        {
            super("Confirm", CONFIRM_COLOR, FineAdjustAction.this::apply);
        }
    }

    private class CancelWidget extends ControlWidget
    {
        CancelWidget()
        {
            super("Cancel", CANCEL_COLOR, FineAdjustAction.this::cancel);
        }
    }
}
