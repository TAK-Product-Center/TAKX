package gov.takx.core.mapobject.gui.devicelog;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.render.DrawContext;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.ping.IGeoPingService;
import gov.takx.commons.swing.GraphicsUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.awt.Color;

/**
 * Simple tool to highlight (ping) a location on the globe, optionally waiting until the globe is not animating
 * before pinging (which allows a "go to" operation to complete and then pings the location).
 *
 * @since 1.49
 */
@ApplicationScoped
public class LocationHighlighter
{
    @Inject
    private IGeoPingService geoPingService;

    @Inject
    private IExtendedGlobeManager globeManager;

    /**
     * Highlight a location on the map.
     *
     * @param location The location to highlight.
     * @since 1.19
     */
    public void highlightLocation(ILocation location)
    {
        geoPingService.userPing(location.getLatitude(), location.getLongitude(), Color.CYAN, 5500, false, GraphicsUtils.Shape.TRIANGLE, true);
    }

    /**
     * If the location is in view, highlight it immediately. If it is not, wait until the view is done flying to the
     * location and then highlight it.
     *
     * @param location The location to highlight.
     * @since 1.19
     */
    public void delayedHighlightLocation(final ILocation location)
    {
        // Get the canvas. This is required to determine if the location is currently in view and when the fly-to
        // animation is complete.
        final WorldWindow wwd = globeManager.getPrimaryGlobeDelegate().getWorldWindow();
        DrawContext drawContext = wwd.getSceneController().getDrawContext();

        // Check to see if the location is in view. If so, highlight it immediately.
        if (drawContext != null && drawContext.getVisibleSector().containsDegrees(location.getLatitude(), location.getLongitude()))
        {
            highlightLocation(location);
        } else
        {
            // Otherwise, wait in 100ms intervals until navigation is complete.
            new Thread(() -> {
                while (wwd.getView().isAnimating())
                {
                    try
                    {
                        Thread.sleep(100);
                    } catch (InterruptedException ignore)
                    {
                    }
                }

                highlightLocation(location);
            }).start();
        }
    }
}
