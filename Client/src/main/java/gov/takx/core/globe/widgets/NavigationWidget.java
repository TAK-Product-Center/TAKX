package gov.takx.core.globe.widgets;

import com.atakmap.math.MathUtils;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.commons.graphics.BitmapDrawable;
import gov.tak.api.commons.graphics.Drawable;
import gov.tak.api.widgets.IMapWidget;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.ui.MotionEvent;
import gov.tak.platform.view.Gravity;
import gov.tak.platform.widgets.DrawableWidget;
import gov.tak.platform.widgets.RootLayoutWidget;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.AWidget;
import gov.takx.api.globe.CoreWidgetNames;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.core.globe.ApplicationGlobe;
import gov.takx.core.globe.GlobeViewController;
import gov.takx.core.globe.tak.ww.ApplicationOrbitView;
import gov.takx.core.timer.TimerService;
import gov.takx.platform.ui.FontUtils;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.TimerTask;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * A TME widget implementation of the Navigation controls (zoom, tilt indication, and tilt slider)
 * @since 5.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class NavigationWidget extends AWidget
{
    private static final int WIDGET_SPACING = 15;
    private static final int TILT_INDICATOR_STROKE_WIDTH = 2;
    private static final Font TILT_FONT = FontUtils.DEFAULT_FONT.deriveFont(Font.BOLD, FontUtils.DEFAULT_FONT_SIZE);
    private static final Dimension TILT_INDICATOR_DIMENSION = new Dimension(30, 30);
    private static final Dimension TILT_SLIDER_DIMENSION = new Dimension(30, 120);
    private static final Dimension TILT_SLIDER_DOT_DIMENSION = new Dimension(30, 23);
    private static final double HALF_TILT_SLIDER_DOT_HEIGHT = TILT_SLIDER_DOT_DIMENSION.height / 2.0;
    private static final Dimension ZOOM_BUTTON_DIMENSION = new Dimension(30, 30);
    private static final BufferedImage TILT_BACKGROUND;
    private static final BufferedImage TILT_SLIDER_BAR;
    private static final BufferedImage TILT_SLIDER_DOT;
    private static final BufferedImage ZOOM_IN_BUTTON;
    private static final BufferedImage ZOOM_OUT_BUTTON;

    private final ApplicationGlobe globe;
    private final PropertyChangeListener viewListener = this::viewChanged;

    @Inject
    private TimerService timerService;

    private DrawableWidget tiltIndicatorWidget;
    private BufferedImage tiltIndicatorImage;
    private DrawableWidget tiltSliderWidget;
    private BufferedImage tiltSliderImage;
    private BufferedImage zoomButtonsImage;

    // Widget MotionEvents are a little strange compared to normal MouseEvents.  If the user clicks and drags
    // on a widget, keeps the mouse down, and drags over another widget, that second widget starts receiving
    // "move" events (which we use to handle zoom and tilt).  So, we need to track the actual mouse down event
    // and track which widget is "active" in order to avoid that problem.
    private boolean zoomActive;
    private boolean tiltActive;

    static
    {
        TILT_BACKGROUND = UIResources.getSizedImage(PLATFORM_ICONS, "navigationImages/tilt-indicator.svg", TILT_INDICATOR_DIMENSION, true);
        TILT_SLIDER_BAR = UIResources.getSizedImage(PLATFORM_ICONS, "navigationImages/tilt-slider-bar.svg", new Dimension(TILT_SLIDER_DIMENSION.width, TILT_SLIDER_DIMENSION.height - TILT_SLIDER_DOT_DIMENSION.height), true);
        TILT_SLIDER_DOT = UIResources.getSizedImage(PLATFORM_ICONS, "navigationImages/tilt-slider-dot.svg", TILT_SLIDER_DOT_DIMENSION, true);
        ZOOM_IN_BUTTON = UIResources.getSizedImage(PLATFORM_ICONS, "navigationImages/zoom-in.svg", ZOOM_BUTTON_DIMENSION, true);
        ZOOM_OUT_BUTTON = UIResources.getSizedImage(PLATFORM_ICONS, "navigationImages/zoom-out.svg", ZOOM_BUTTON_DIMENSION, true);
    }

    public NavigationWidget(@Nonnull IGlobeDelegate globe)
    {
        this.globe = (ApplicationGlobe) globe;
        setGravity(Gravity.CENTER);
        setOrientation(VERTICAL);
        setPadding(15, 0, 0, 0);
        setName(CoreWidgetNames.NAVIGATION);
    }

    @PostConstruct
    private void postConstruct()
    {
        View view = globe.getWorldWindow().getView();
        view.addPropertyChangeListener(AVKey.VIEW, viewListener);
        stitchAndSetImages(view.getPitch().getDegrees());
        setVisible(true);
        updateWidget();
    }

    private void viewChanged(PropertyChangeEvent event)
    {
        if (isVisible())
        {
            stitchAndSetImages(((ApplicationOrbitView) event.getSource()).getPitch().getDegrees());
            updateWidget();
        }
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
     * Sets the {@link Drawable}s for all child widgets based on the current state of their {@link BufferedImage}s.
     * <p/>
     * Marshals the BufferedImages into {@link BitmapDrawable}s, and then either updates the existing widgets or creates
     * new ones if they don't exist.
     */
    private void updateWidget()
    {
        BitmapDrawable tiltIndicatorBitmap = new BitmapDrawable(MarshalManager.marshal(tiltIndicatorImage, BufferedImage.class, Bitmap.class));
        BitmapDrawable tiltSliderBitmap = new BitmapDrawable(MarshalManager.marshal(tiltSliderImage, BufferedImage.class, Bitmap.class));

        if (!getChildren().isEmpty()) // widgets have already been initialized, just update the images
        {
            tiltIndicatorWidget.setDrawable(tiltIndicatorBitmap);
            tiltSliderWidget.setDrawable(tiltSliderBitmap);
        } else // there's no inner widgets, initialize new ones
        {
            BitmapDrawable zoomButtonsBitmap = new BitmapDrawable(MarshalManager.marshal(zoomButtonsImage, BufferedImage.class, Bitmap.class));
            DrawableWidget zoomButtonsWidget = new DrawableWidget(zoomButtonsBitmap);
            zoomButtonsWidget.setSize(ZOOM_BUTTON_DIMENSION.width, ZOOM_BUTTON_DIMENSION.height * 2); // *2 to include both buttons
            ZoomListener zoomListener = new ZoomListener();
            zoomButtonsWidget.addOnPressListener(zoomListener);
            zoomButtonsWidget.addOnUnpressListener(zoomListener);
            zoomButtonsWidget.setMargins(0, WIDGET_SPACING, 0, 0);
            addChildWidget(zoomButtonsWidget);

            tiltIndicatorWidget = new DrawableWidget(tiltIndicatorBitmap);
            tiltIndicatorWidget.setSize(TILT_INDICATOR_DIMENSION.width, TILT_INDICATOR_DIMENSION.height);
            tiltIndicatorWidget.setMargins(0, WIDGET_SPACING, 0, 0);
            addChildWidget(tiltIndicatorWidget);

            tiltSliderWidget = new DrawableWidget(tiltSliderBitmap);
            tiltSliderWidget.setSize(TILT_SLIDER_DIMENSION.width, TILT_SLIDER_DIMENSION.height);
            SliderListener sliderListener = new SliderListener();
            tiltSliderWidget.addOnClickListener(sliderListener);
            tiltSliderWidget.addOnMoveListener(sliderListener);
            tiltSliderWidget.addOnPressListener(sliderListener);
            tiltSliderWidget.addOnUnpressListener(sliderListener);
            tiltSliderWidget.setMargins(0, WIDGET_SPACING, 0, 0);
            addChildWidget(tiltSliderWidget);
        }
    }

    @Override
    public void releaseResources()
    {
        if (globe == null || globe.getWorldWindow() == null) return;
        globe.getWorldWindow().getView().removePropertyChangeListener(AVKey.VIEW, viewListener);
    }

    private void stitchAndSetImages(double viewAngle)
    {
        tiltIndicatorImage = stitchTiltIndicator(viewAngle);
        tiltSliderImage = stitchTiltSlider(viewAngle);
        zoomButtonsImage = stitchZoomButtons();
    }

    /**
     * Puts together the zoom buttons with coloring on hover and press
     * @return An updated set of zoom buttons in a {@link BufferedImage}
     */
    @Nonnull
    private BufferedImage stitchZoomButtons()
    {
        BufferedImage returnImage = new BufferedImage(ZOOM_BUTTON_DIMENSION.width, ZOOM_BUTTON_DIMENSION.height * 2, BufferedImage.TYPE_INT_ARGB);  // *2 to include both buttons
        Graphics2D g = getGraphics2D(returnImage);

        g.drawImage(ZOOM_IN_BUTTON, null, 0, 0);
        g.drawImage(ZOOM_OUT_BUTTON, null, 0, ZOOM_BUTTON_DIMENSION.height);
        g.dispose();
        return returnImage;
    }

    /**
     * Puts together the background, the angle indicator + tilt text
     * @param viewAngle The current angle to indicate
     * @return An updated tilt indicator in a {@link BufferedImage}
     */
    @Nonnull
    private BufferedImage stitchTiltIndicator(double viewAngle)
    {
        BufferedImage returnImage = new BufferedImage(TILT_INDICATOR_DIMENSION.width, TILT_INDICATOR_DIMENSION.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = getGraphics2D(returnImage);
        g.drawImage(TILT_BACKGROUND, null, 0, 0);

        // calculate the end points of the tilt angle indicator line (red line)
        double lineLengthModifier = .875; // slightly shorten the line to keep it on the indicator
        double tiltAngle = 90 - viewAngle; // inverted because the view gives angle off vertical
        double tiltAngleRadians = Math.toRadians(tiltAngle);
        int angleLineEndXPos = (int) (Math.cos(tiltAngleRadians) * TILT_INDICATOR_DIMENSION.width * lineLengthModifier);
        int angleLineEndYPos = TILT_INDICATOR_DIMENSION.height - (int) (Math.sin(tiltAngleRadians) * TILT_INDICATOR_DIMENSION.height * lineLengthModifier);

        // draw tilt line w/o going past the image edges
        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(TILT_INDICATOR_STROKE_WIDTH));
        int halfStrokeWidth = TILT_INDICATOR_STROKE_WIDTH / 2;
        g.drawLine(halfStrokeWidth, TILT_INDICATOR_DIMENSION.height - halfStrokeWidth, angleLineEndXPos + halfStrokeWidth, angleLineEndYPos - halfStrokeWidth);

        // calculate the location of the tilt angle text
        g.setFont(TILT_FONT);
        // Using 1/15th off the edges (left, bottom) to most closely mimic WinTAK positioning
        int xPos = (int) (TILT_INDICATOR_DIMENSION.width / 15.0);
        int yPos = (int) (TILT_INDICATOR_DIMENSION.height * (14.0 / 15.0));
        String formattedAngle = String.format("%.0f°", tiltAngle);

        g.setColor(Color.BLACK);
        g.drawString(formattedAngle, xPos + 1, yPos + 1); // tilt angle text shadow
        g.setColor(Color.WHITE);
        g.drawString(formattedAngle, xPos, yPos);
        g.dispose();
        return returnImage;
    }

    /**
     * Puts together the slider bar + slider dot at the correct location
     * @param viewAngle The current angle to dictate the slider dot position
     * @return An updated tilt slider in a {@link BufferedImage}
     */
    @Nonnull
    private BufferedImage stitchTiltSlider(double viewAngle)
    {
        double currentPos = convertAngleToSliderPos(viewAngle);
        BufferedImage returnImage = new BufferedImage(TILT_SLIDER_DIMENSION.width, TILT_SLIDER_DIMENSION.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = getGraphics2D(returnImage);
        g.drawImage(TILT_SLIDER_BAR, null, 0, (int) HALF_TILT_SLIDER_DOT_HEIGHT);
        g.drawImage(TILT_SLIDER_DOT, null, 0, (int) (currentPos - HALF_TILT_SLIDER_DOT_HEIGHT));
        g.dispose();

        return returnImage;
    }

    /**
     * Maps the current pitch/tilt of the view to the correct slider dot position on the slider bar
     * @param viewAngle The angle reported by the view directly (e.g. - from {@code globe.getWorldWindow().getView().getPitch()})
     * @return The relative position the slider dot should be on the slider bar
     */
    private double convertAngleToSliderPos(double viewAngle)
    {
        return rangeMap(90 - viewAngle, TILT_SLIDER_DIMENSION.height - HALF_TILT_SLIDER_DOT_HEIGHT, HALF_TILT_SLIDER_DOT_HEIGHT, 90, 0);
    }

    /**
     * Maps the current position of the slider dot on the slider bar to the appropriate view angle
     * @param sliderPos The current slider position
     * @return The angle based on the relative position of the slider dot, for use in {@link GlobeViewController#setTilt(double)}
     */
    private double convertSliderPosToTiltAngle(double sliderPos)
    {
        final double tilt = 90 - rangeMap(sliderPos, 90, 0, TILT_SLIDER_DIMENSION.height - HALF_TILT_SLIDER_DOT_HEIGHT, HALF_TILT_SLIDER_DOT_HEIGHT);
        return MathUtils.clamp(tilt, 0, 90);
    }

    /**
     * Maps a value between {@code (oldBottom, oldTop)} onto its relative position in the range {@code (newBottom, newTop)}
     * @param value The value to be mapped
     * @param newTop The top value in the new range
     * @param newBottom The bottom value in the new range
     * @param oldTop The top value in the old range
     * @param oldBottom The bottom value in the old range
     * @return The value mapped from the old range to its relative position in the new range
     */
    private double rangeMap(double value, double newTop, double newBottom, double oldTop, double oldBottom)
    {
        return ((value - oldBottom) / (oldTop - oldBottom)) * (newTop - newBottom) + newBottom;
    }

    /**
     * Calculate the Y position of the {@link IMapWidget} inside the {@link RootLayoutWidget} allowing the user to get the
     * precise location of {@link MotionEvent} interactions with the widget
     * @param widget The widget to calculate the position for
     * @return The Y position of the widget inside the root widget, not including padding or margins
     */
    private double getWidgetPosition(@Nonnull IMapWidget widget)
    {
        // TODO: when padding is fixed in ENGINE-728 remove " + widget.getPadding()[3]"
        return widget.getAbsoluteWidgetPosition().y + widget.getPadding()[1] + widget.getPadding()[3];
    }

    private Graphics2D getGraphics2D(BufferedImage image)
    {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return g;
    }

    /**
     * A listener to capture the OnClick and OnMove events for the tilt slider and handle them appropriately
     */
    private class SliderListener implements IMapWidget.OnClickListener, IMapWidget.OnMoveListener, OnPressListener, OnUnpressListener
    {
        @Override
        public void onMapWidgetPress(IMapWidget widget, MotionEvent event)
        {
            if (zoomActive || event.getActionMasked() != MotionEvent.ACTION_DOWN) return;
            tiltActive = true;
        }

        @Override
        public void onMapWidgetUnpress(IMapWidget widget, MotionEvent event)
        {
            tiltActive = zoomActive = false;
        }

        @Override
        public void onMapWidgetClick(IMapWidget widget, MotionEvent event)
        {
            if (event.getActionMasked() == MotionEvent.ACTION_UP) setGlobeTilt(widget, event);
        }

        @Override
        public boolean onMapWidgetMove(IMapWidget widget, MotionEvent event)
        {
            if (tiltActive) setGlobeTilt(widget, event);
            return true;
        }

        private void setGlobeTilt(IMapWidget widget, MotionEvent event)
        {
            globe.getGlobeViewController().setTilt(convertSliderPosToTiltAngle(event.getY() - getWidgetPosition(widget)));
        }
    }

    /**
     * A listener to capture the OnPress and OnUnpress events for the zoom buttons and handle them appropriately
     */
    private class ZoomListener implements IMapWidget.OnPressListener, IMapWidget.OnUnpressListener
    {
        private static final double ZOOM_SCALE = .03; // the percentage of the current zoom level to change by (% per ZOOM_PERIOD ms)
        private static final long ZOOM_PERIOD = 50;  // milliseconds delay
        private TimerTask zoomTask;

        @Override
        public void onMapWidgetPress(IMapWidget widget, MotionEvent event)
        {
            if (tiltActive || event.getActionMasked() != MotionEvent.ACTION_DOWN) return;
            zoomActive = true;

            final var viewController = globe.getGlobeViewController();
            final var view = ((BasicOrbitView) globe.getWorldWindow().getView());

            boolean zoomIn = (event.getY() - getWidgetPosition(widget)) <= ZOOM_BUTTON_DIMENSION.height;
            double zoomMultiplier = zoomIn ? (1 - ZOOM_SCALE) : (1 + ZOOM_SCALE);
            zoomTask = new TimerTask()
            {
                @Override
                public void run()
                {
                    viewController.setEyeAltitude(view.getZoom() * zoomMultiplier, true);
                }
            };

            timerService.scheduleAtFixedRate(zoomTask, 0, ZOOM_PERIOD);
        }

        @Override
        public void onMapWidgetUnpress(IMapWidget widget, MotionEvent event)
        {
            zoomActive = tiltActive = false;
            if (zoomTask != null) zoomTask.cancel();
        }
    }
}
