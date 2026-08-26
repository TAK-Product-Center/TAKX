package gov.takx.core.globe;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectControllerListener;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.playback.IPlaybackSystem;
import gov.takx.api.query.param.HQLParameter;
import gov.takx.api.tracks.IConnectingLineDefinition;
import gov.takx.api.tracks.IExtendedSmartTrackManager;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.api.tracks.ITrackStylingContext;
import gov.takx.core.globe.tak.RenderableManager;
import gov.takx.core.mapdata.MapDataUtils;
import gov.takx.core.mapdata.MapPosition;
import gov.takx.platform.tracks.LineStyle;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * This class represents a single connecting line on the globe. It is driven by an {@link IConnectingLineDefinition}
 * whose properties control the connecting line rendering.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class ConnectingLine implements PropertyChangeListener, IMapObjectControllerListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    private IExtendedSmartTrackManager smartTrackManager;

    @Inject
    private MapDataUtils mapDataUtils;

    @Inject
    private IPersistServiceProvider persistServiceProvider;

    @Inject
    private ICoreExecutorService executorService;

    @Inject
    private IPlaybackSystem playbackSystem;

    @Inject
    private RenderableManager renderableManager;

    private final IIconMapObject mapObject;
    private IConnectingLineDefinition cld;
    private ITrackStylingContext tsc;
    private boolean visible;
    private boolean populated;
    private boolean nonZeroElevation;
    private boolean snapToSurface;
    private boolean forceSnapToSurface;

    private static final List<String> moProperties = Arrays.asList(IMapObject.CONNECTING_LINE, IMapObject.TRACK_STYLE);
    private static final Comparator<MapPosition> MAP_POSITION_TIME_COMPARATOR = new MapPositionTimeComparator();

    private ConnectingLinePath path;

    private final SortedSet<MapPosition> positions = new ConcurrentSkipListSet<>(MAP_POSITION_TIME_COMPARATOR);

    /**
     * Constructor using a given map object.
     *
     * @param mapObject The map object for whom we are rendering a connecting line.
     */
    public ConnectingLine(IIconMapObject mapObject)
    {
        if (logger.isDebugEnabled()) logger.debug("Initialize ConnectingLine for MO: {}", mapObject);

        this.mapObject = mapObject;
        cld = mapObject.getConnectingLineDefinition();
    }

    @PostConstruct
    private void postConstruct()
    {
        // If the MO had no CLD, then use the global one
        if (cld == null) cld = smartTrackManager.getGlobalConnectingLineDefinition();

        // Add listeners so we see new messages that come in and for changes in the CLD
        mapObject.getController().addMapObjectControllerListener(this);

        cld.addPropertyChangeListener(this);

        // We need to monitor the TrackStylingContext as well to know if "snap to surface" is on
        // Avoid creating a TSC if the MO has no TSD
        final ITrackStyleDefinition tsd = mapObject.getTrackStyleDefinition();
        if (tsd != null)
        {
            tsc = smartTrackManager.getTrackStylingContext(mapObject);
            tsc.addPropertyChangeListener(this, ITrackStylingContext.SNAP_TO_SURFACE);
            snapToSurface = tsc.isSnappedToSurface();
        } else
        {
            snapToSurface = smartTrackManager.getGlobalTrackStyleDefinition().isSnappedToSurface();
        }

        // And monitor the map object for replacement of the TSD or CLD
        mapObject.addPropertyChangeListener(moProperties, this);

        if (isCldVisible()) createOrUpdatePathAsNeeded();
    }

    /**
     * Create or update the underlying path object based on our current settings.
     *
     * @since 2.24
     */
    private void createOrUpdatePathAsNeeded()
    {
        if (path == null)
        {
            // Create and configure the object that will actually render the connecting lines (path)
            path = new ConnectingLinePath();

            // Create and set an attribute bundle
            ShapeAttributes attrs = new BasicShapeAttributes();
            path.setAttributes(attrs);
            path.setPositions(positions);
            path.setFollowTerrain(true);
        }

        updateAltitudeMode();
        initFromCLD();
    }

    /**
     * @return {@code true} If the current CLD settings indicate that we should render (be visible)
     */
    private boolean isCldVisible()
    {
        return cld.getLineStyle() != LineStyle.None; // Are we drawing or not
    }

    /**
     * Initialize our state and path attributes based on the current CLD settings.
     */
    private void initFromCLD()
    {
        visible = isCldVisible();

        if (visible)
        {
            path.setDrawArrowHead(cld.showDirectionalArrow());

            final ShapeAttributes attrs = path.getAttributes();
            attrs.setOutlineMaterial(new Material(cld.getLineColor()));
            attrs.setOutlineWidth(cld.getLineWidth() / 2.);

            // If we've become visible and we're not populated, then populate now
            if (visible && !populated)
            {
                populate();
            }
        }
    }

    /**
     * Render this connecting line.  If we are visible, just hand the rendering off to our directed path.  If not,
     * then simply do nothing.
     *
     * @param dc Draw context
     */
    public void render(DrawContext dc)
    {
        if (visible && mapDataUtils.isMapDataViewable(mapObject))
        {
            renderableManager.offerRenderable(path, dc);
        }
    }

    /**
     * Set the override snap to surface.  This is typically used when the current rendering engine doesn't support
     * full 3D elevations).
     *
     * @param forceSnapToSurface Pass {@code true} to force the connecting lines to render on the surface
     */
    public void setForceSnapToSurface(boolean forceSnapToSurface)
    {
        this.forceSnapToSurface = forceSnapToSurface;
        updateAltitudeMode();
    }

    @Override
    @InvokeOnEDT
    public void propertyChange(final PropertyChangeEvent evt)
    {
        final String propName = evt.getPropertyName();

        switch (propName)
        {
            case IConnectingLineDefinition.LINE_STYLE:
                final LineStyle style = (LineStyle) evt.getNewValue();
                final boolean newVisible = style != LineStyle.None;

                if (newVisible) createOrUpdatePathAsNeeded();
                visible = newVisible;
                break;
            case IConnectingLineDefinition.LINE_COLOR:
                if (path != null) path.getAttributes().setOutlineMaterial(new Material((Color) evt.getNewValue()));

                break;
            case IConnectingLineDefinition.DIRECTIONAL_ARROW:
                if (path != null) path.setDrawArrowHead((Boolean) evt.getNewValue());

                break;
            case IConnectingLineDefinition.LINE_WIDTH:
                if (path != null) path.getAttributes().setOutlineWidth(cld.getLineWidth() / 2.);

                break;
            case ITrackStylingContext.SNAP_TO_SURFACE:
                snapToSurface = (Boolean) evt.getNewValue();
                if (path != null) updateAltitudeMode();

                break;
            case IMapObject.CONNECTING_LINE:
                cld = mapObject.getConnectingLineDefinition();
                if (isCldVisible()) createOrUpdatePathAsNeeded();
                break;
            case IMapObject.TRACK_STYLE:
                if (tsc == null) smartTrackManager.getGlobalTrackStyleDefinition().removePropertyChangeListener(this);

                tsc = smartTrackManager.getTrackStylingContext(mapObject);
                tsc.addPropertyChangeListener(this, ITrackStylingContext.SNAP_TO_SURFACE);
                snapToSurface = tsc.isSnappedToSurface();
                updateAltitudeMode();
                break;
        }
    }

    /**
     * Update our altitude mode based on whether the map object has any non-zero elevation data or not and whether
     * the map object styling context is "snap to ground" or not.
     */
    private void updateAltitudeMode()
    {
        if (path != null)
        {
            path.setAltitudeMode((nonZeroElevation && !(forceSnapToSurface || snapToSurface))
                    ? WorldWind.ABSOLUTE
                    : WorldWind.CLAMP_TO_GROUND);
        }
    }

    /**
     * Handle a new RDM coming in - we need to update our connecting line vertices.
     *
     * @param rdm The new RDM to add to the line
     */
    @Override
    public void receiveMessage(IRaptorDataMessage rdm)
    {
        if (rdm.isManuallyGeneratedLocation() || !rdm.hasLocation())
        {
            return; // Ignore manually generated messages and those with no location
        }

        // If we're not visible, just blow this off.  We'll repopulate the next time we're made visible.
        if (visible)
        {
            if (rdm.hasElevation() && rdm.getElevation() > 0)
            {
                nonZeroElevation = true;
            }

            updateAltitudeMode();

            // Just add the new data to the positions set
            MapPosition pos = new MapPosition(rdm.getLatitude(), rdm.getLongitude(), rdm.getElevation(), rdm.getRelevantTime());
            positions.add(pos);
            updatePositions(); // Update the path so it knows the positions have changed
        } else
        {
            populated = false; // We no longer have all the data
        }
    }

    /**
     * Cleanup, remove all our listeners
     */
    void cleanup()
    {
        mapObject.getController().removeMapObjectControllerListener(this);
        cld.removePropertyChangeListener(this);

        if (tsc != null) tsc.removePropertyChangeListener(this, ITrackStylingContext.SNAP_TO_SURFACE);

        mapObject.removePropertyChangeListener(moProperties, this);

        positions.clear();
    }

    /**
     * Perform a "fast" cleanup when the project is closing.  We don't need to remove all our listener registrations
     * since the objects to which we are listening are all going away.  Just cleanup to ensure object references
     * are cleared to ensure GC can do its job.
     *
     * @since 3.0
     */
    void cleanupFast()
    {
        positions.clear();
        path = null;
        cld = null;
        tsc = null;
    }

    /**
     * Select all the map positions for this map object and add them to the positions list.
     */
    private void populate()
    {
        Runnable populator = () -> {
            Thread.currentThread().setName("ConnectingLine populator for " + mapObject.getKey());

            String hql = "select new " + MapPosition.class.getName()
                    + "(rdm.latitude, rdm.longitude, rdm.elevation, rdm.relevantTime)"
                    + " from RaptorDataMessage rdm"
                    + " where rdm.hasLocation=true"
                    + "   and rdm.isManualLocation=false"
                    + "   and rdm.mapObject_id = :mo";

            if (logger.isDebugEnabled()) logger.debug("populator running");

            List<MapPosition> positionList = persistServiceProvider.getPersistService()
                    .queryFromRdm(hql, new HQLParameter("mo", mapObject));

            if (logger.isDebugEnabled())
            {
                logger.debug("MO: {}: has {} map positions", mapObject, positionList.size());
            }

            // Now that we have the list, add it to the position set
            positions.clear(); // Start empty
            positions.addAll(positionList);

            // Update our nonZeroElevation indicator
            for (MapPosition pos : positionList)
            {
                if (pos.getElevation() > 0)
                {
                    nonZeroElevation = true;
                    break;
                }
            }

            updateAltitudeMode(); // Update altitude mode with new data
            updatePositions(); // Update the path so it knows the positions have changed

            populated = true; // it is now
        };

        // Submit it for execution
        executorService.execute(populator);
    }

    @Override
    public String toString()
    {
        return "[ConnectingLine: " + mapObject;
    }

    /**
     * Update the position list on the path. Pay attention to playback as needed.
     * <p>
     * Do the path position setting on the EDT so it doesn't cause exceptions when interleaved with rendering.
     */
    void updatePositions()
    {
        if (path == null)
        {
            return;
        }

        // We need to check if the connecting line is affected by playback first
        final long playbackCurrentTime = playbackSystem.getCurrentPlaybackPosition();
        final List<MapPosition> updatedPositions;
        if (!playbackSystem.isLive() && !positions.isEmpty() && positions.last().getTime() > playbackCurrentTime)
        {
            // Update the path for playback if relevant
            updatedPositions = new ArrayList<>();
            for (final MapPosition position : positions)
            {
                if (position.getTime() <= playbackCurrentTime)
                {
                    updatedPositions.add(position);
                } else
                {
                    // The positions are sorted by time. So as soon as we get one larger than the playback time
                    // we can bail out of the loop.
                    break;
                }
            }
        } else
        {
            // Unfortunately, due to the way that the underlying path does its rendering, we have to make a copy
            // of our positions.  It has to remain independent of our position updates while it is rendering.
            final MapPosition[] newPositions = positions.toArray(new MapPosition[0]);
            updatedPositions = Arrays.asList(newPositions);
        }

        SwingUtilities.invokeLater(() -> path.setPositions(updatedPositions));
    }

    /**
     * Comparator for ordering MapPosition objects by the time field.  Only compare equal if they are the same
     * object instance.
     *
     * @since 2.29 (moved here from MapDataUtils)
     */
    private static final class MapPositionTimeComparator implements Comparator<MapPosition>
    {
        @Override
        public int compare(MapPosition mp1, MapPosition mp2)
        {
            final int cmp = Long.compare(mp1.getTime(), mp2.getTime());

            return cmp == 0
                    ? Integer.compare(System.identityHashCode(mp1), System.identityHashCode(mp2)) // Compare hashcodes if times are equal
                    : cmp;
        }
    }
}
