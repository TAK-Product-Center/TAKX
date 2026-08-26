package gov.takx.core.globe.widgets;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.avlist.AVKey;
import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.commons.graphics.BitmapDrawable;
import gov.tak.api.widgets.IMapWidget;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.ui.MotionEvent;
import gov.tak.platform.widgets.DrawableWidget;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.AWidget;
import gov.takx.api.globe.CoreWidgetNames;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.timer.ITimerService;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.core.globe.tak.ww.ApplicationOrbitView;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.TimerTask;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * A TME Widget implementation of a Compass
 * </p>
 * Includes the following functionality:
 * <ul>
 *     <li>Double click to set current view heading to north</li>
 *     <li>Single click to cycle the following settings:
 *          <ul>
 *              <li>Free look</li>
 *              <li><i>(NYI)</i> North view lock: locks heading to north</li>
 *              <li><i>(NYI)</i> User heading lock: keeps the view heading locked to the user's heading</li>
 *          </ul>
 *     </li>
 * </ul>
 *
 * @since 5.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class CompassWidget extends AWidget
{
    private static final int WIDGET_SIZE = 40;
    private static final int WIDGET_PADDING = 10;
    private static final BufferedImage COMPASS_IMAGE = UIResources.getSvg(null, PLATFORM_ICONS, "navigationImages/north-arrow.svg", WIDGET_SIZE, WIDGET_SIZE, true);

    private final PropertyChangeListener viewListener = this::viewChanged;
    private final IApplicationGlobe globe;

    private BufferedImage rotatedCompassImage;

    @Inject
    private ITimerService timerService;

    /**
     * @param globe The globe this is applied to
     */
    public CompassWidget(@Nonnull IApplicationGlobe globe)
    {
        this.globe = globe;

        setOrientation(VERTICAL);
        setPadding(WIDGET_PADDING, WIDGET_PADDING, 0, 0);
        setName(CoreWidgetNames.COMPASS);
    }

    @PostConstruct
    private void postConstruct()
    {
        View view = globe.getWorldWindow().getView();
        rotateCompassToNorth(view.getHeading().getDegrees());
        view.addPropertyChangeListener(AVKey.VIEW, viewListener);
        setVisible(true);
        updateWidget();
    }

    private void viewChanged(PropertyChangeEvent event)
    {
        rotateCompassToNorth(((ApplicationOrbitView) event.getSource()).getHeading().getDegrees());
        updateWidget();
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

    @InvokeOnEDT
    private void updateWidget()
    {
        BitmapDrawable compassBitmap = new BitmapDrawable(MarshalManager.marshal(rotatedCompassImage, BufferedImage.class, Bitmap.class));

        List<IMapWidget> children = getChildren();
        if (!children.isEmpty()) // just update the compass image (the only child of "currentWidget")
        {
            ((DrawableWidget) children.get(0)).setDrawable(compassBitmap);
        } else // there's no inner widget, initialize a new one
        {
            DrawableWidget compassWidget = new DrawableWidget(compassBitmap);
            compassWidget.setSize(WIDGET_SIZE, WIDGET_SIZE);
            compassWidget.addOnClickListener(new WidgetClickListener());
            addChildWidgetAt(0, compassWidget);
        }
    }

    /**
     * Rotate the compass image <b>to</b> the provided angle (<b>not</b> the angle to rotate by) relative to 0* at the top of the screen
     * @param angle The angle to rotate the image to
     */
    private void rotateCompassToNorth(double angle)
    {
        rotatedCompassImage = GraphicsUtils.rotate(COMPASS_IMAGE, -angle);
    }

    @Override
    public void releaseResources()
    {
        if (globe == null || globe.getWorldWindow() == null) return;
        globe.getWorldWindow().getView().removePropertyChangeListener(AVKey.VIEW, viewListener);
    }

    private class WidgetClickListener implements IMapWidget.OnClickListener
    {
        // max millisecond window for double click, pulled from OS
        private static final Object MULTI_CLICK_INTERVAL_PROPERTY = Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");
        private static final int CLICK_WINDOW_MS = MULTI_CLICK_INTERVAL_PROPERTY == null ? 500 : (int) MULTI_CLICK_INTERVAL_PROPERTY;
        private TimerTask clickTask;
        private int clickCount;
        private long startTime; // holds start of potential double-click window

        /**
         * Override of the default click listener to capture double clicks to reset the view heading
         *
         * @param widget The compass widget
         * @param event  The {@link MotionEvent} that occurred
         */
        @Override
        public void onMapWidgetClick(IMapWidget widget, MotionEvent event)
        {
            // only count UP events for clicking, motionEvent doesn't capture DOWN events
            if (event.getActionMasked() != MotionEvent.ACTION_UP) return;
            if (System.currentTimeMillis() - startTime > CLICK_WINDOW_MS)
            {
                clickTask = timerService.createTask(() -> clickCount = 0, CLICK_WINDOW_MS + 1);
                startTime = System.currentTimeMillis();
                clickCount = 1;
            } else if (clickCount == 1)
            {
                clickTask.cancel();
                clickCount = 0;
                globe.resetHeading();
            }
        }
    }
}
