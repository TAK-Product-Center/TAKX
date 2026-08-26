package gov.takx.core.placemarkoverlay;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.core.view.gui.CoreComponentType;
import gov.takx.core.view.gui.IExtendedUISpaceManager;
import gov.takx.core.view.gui.propertiespanel.CSVPlacemarkPropertiesItem;
import gov.takx.platform.ui.UIResources;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;

import java.awt.AWTEvent;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Listens for clicks related to point placemarks.
 *
 * @since 1.16
 */
@PseudoBean
public class PlacemarkSelectListener extends MouseAdapter implements SelectListener, AWTEventListener
{

    /**
     * The custom cursor to use when selecting a point placemark location.
     */
    private static final Cursor PLACEMARK_CURSOR;

    /**
     * The currently (or previously) selected placemark.
     */
    private CSVPlaceMark selectedPlacemark;

    /**
     * Boolean indicating if the user is currently selecting a new placemark location.
     */
    private boolean listeningForNewPlacemarkLocation;

    /**
     * The start location of a click.
     */
    private boolean isDrag;

    @Inject
    private IExtendedUISpaceManager uiSpaceManager;

    @Inject
    private PlacemarkOverlayManager placemarkOverlayManager;

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    @UiEventBus
    private EventBus uiEventBus;

    static
    {
        BufferedImage image = UIResources.getImage(PLATFORM_ICONS, "placemark.png");
        final Toolkit toolkit = Toolkit.getDefaultToolkit();

        // The cursor will be scaled to the best native size, so use those dimensions to set the hotspot; otherwise, the
        // hotspot could be out of bounds if the cursor is scaled down.
        Dimension nativeSize = toolkit.getBestCursorSize(image.getWidth(), image.getHeight());
        PLACEMARK_CURSOR = toolkit.createCustomCursor(image, new Point(nativeSize.width / 2, nativeSize.height - 1), "Placemark Cursor");
    }

    /**
     * Notify this select listener that the user is currently choosing a location for a new point placemark.
     */
    public void armNewPlacemarkLocationListener()
    {
        startListeningForPlacemarkLocationClick();
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        if (!e.isConsumed())
        {
            if (listeningForNewPlacemarkLocation && !isDrag)
            {
                stopListeningForPlacemarkLocationClick();
                final WorldWindow wwd = (WorldWindow) e.getSource();
                final IApplicationGlobe globe = globeManager.getGlobeForComponent(wwd);
                placemarkOverlayManager.showAddNewPlacemarkDialog(globe.getGlobeLocationFromPoint(e.getPoint()));
                e.consume();
            }

            isDrag = false;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        if (!e.isConsumed())
        {
            isDrag = true;
        }
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        if (!e.isConsumed())
        {
            if (!listeningForNewPlacemarkLocation)
            {
                WorldWindow wwd = (WorldWindow) e.getSource();

                //if in selection mode AND terrain is the only objects at current position, then deselect the placemark.
                if (wwd.getObjectsAtCurrentPosition() != null && wwd.getObjectsAtCurrentPosition().size() < 2)
                {
                    if (selectedPlacemark != null)
                    {
                        selectedPlacemark.setSelected(false);
                    }
                }
            }
        }
    }

    @Override
    public void selected(SelectEvent event)
    {
        if (event.isConsumed())
        {
            return;
        }

        if (event.isLeftPress())
        {
            select(getTopPlaceMark(event));
        }
    }

    /**
     * If we have a mouse pressed event, and the press did not occur within the bounds of the globe window,
     * disable placemark location selection.
     *
     * @param event AWTEvent.MOUSE_EVENT_MASK
     */
    @Override
    public void eventDispatched(AWTEvent event)
    {
        if (listeningForNewPlacemarkLocation)
        {
            if (event.getID() == MouseEvent.MOUSE_PRESSED)
            {
                //If the relative point is not within the bounds of any globe window, stop listening
                if (!globeManager.mouseLocationInAnyGlobe())
                {
                    stopListeningForPlacemarkLocationClick();
                }
            }
        }
    }

    public void select(CSVPlaceMark placemark)
    {
        if (selectedPlacemark != null)
        {
            selectedPlacemark.setSelected(false);
        }

        selectedPlacemark = placemark;

        if (selectedPlacemark != null)
        {
            clearAllMapObjectSelections();
            uiSpaceManager.showComponent(CoreComponentType.PROPERTIES_PANEL);
            uiEventBus.post(new CSVPlacemarkPropertiesItem(placemark));
            selectedPlacemark.setSelected(true);
        }
    }

    private void startListeningForPlacemarkLocationClick()
    {
        listeningForNewPlacemarkLocation = true;
        setCursor(PLACEMARK_CURSOR);
        globeManager.setSelectListenersActive(false);
    }

    private void stopListeningForPlacemarkLocationClick()
    {
        listeningForNewPlacemarkLocation = false;
        setCursor(Cursor.DEFAULT_CURSOR);
        globeManager.setSelectListenersActive(true);
        isDrag = false;
    }

    private void setCursor(int cursorType)
    {
        setCursor(Cursor.getPredefinedCursor(cursorType));
    }

    private void setCursor(Cursor cursor)
    {
        globeManager.setCursorOnAllGlobes(cursor);
    }

    private CSVPlaceMark getTopPlaceMark(SelectEvent event)
    {
        Object topObject = event.getTopObject();
        if (topObject instanceof CSVPlaceMark)
        {
            return (CSVPlaceMark) topObject;
        }
        return null;
    }

    private void clearAllMapObjectSelections()
    {
        mapObjectManager.setSelected(null);
    }
}
