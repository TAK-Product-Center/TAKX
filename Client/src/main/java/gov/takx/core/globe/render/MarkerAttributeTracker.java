package gov.takx.core.globe.render;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.api.globe.IGlobeMapObjectChangeListener;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.api.tracks.internal.ISmartTrackAssignmentManager;
import gov.takx.core.globe.utils.StylingContextKey;
import gov.takx.core.mapdata.MapPoint;
import gov.takx.core.mapdata.MapPointGroup;
import gov.takx.platform.globe.renderer.ARenderingEngine;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provide a single location where GlobeMarkerAttribute instances can be handed out.  This enables sharing of the
 * GMA instances across all the objects that share the same "equivalence set" for rendering.  This class also
 * handles notifications of globe highlighting and passes that off to the GMA (for secondary highlight handling).
 * <p>
 * This class is used as a singleton as only one instance is created by the rendering engine.
 *
 * @see ARenderingEngine
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MarkerAttributeTracker
        implements IMarkerAttributeTracker, IGlobeMapObjectChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private ISmartTrackAssignmentManager smartTrackAssignmentManager;

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    private IExtendedGlobeManager globeManager;

    private final RenderingStyleContextManager scManager;

    // Map from TSD/MO unique key to marker attributes
    private final Map<StylingContextKey, GlobeMarkerAttributes> tsdMarkerAttributes = new ConcurrentHashMap<>();

    // Map from TSD/MO unique key to group marker attributes
    private final Map<StylingContextKey, GlobeMarkerAttributes> tsdGroupMarkerAttributes = new ConcurrentHashMap<>();

    private static boolean constructed = false;

    /**
     * Constructor.  This should only be called once per project.
     *
     * @param scManager Styling context manager to use to access styling context keys
     */
    public MarkerAttributeTracker(RenderingStyleContextManager scManager)
    {
        if (constructed)
        {
            logger.error("Illegal construction of a second MarkerAttributeTracker", new RuntimeException());
        }

        constructed = true;

        this.scManager = scManager;
    }

    @PostConstruct
    private void postConstruct()
    {
        // Register as a globe map object change listener (so we get notified of map object creation/deletion)
        globeManager.getPrimaryGlobeDelegate().addGlobeMapObjectChangeListener(this);
    }

    @Override
    public void onShutdown(IProgressMonitor progressMonitor)
    {
        constructed = false;

        globeManager.getPrimaryGlobeDelegate().removeGlobeMapObjectChangeListener(this);

        tsdMarkerAttributes.values().forEach(GlobeMarkerAttributes::cleanup);
        tsdMarkerAttributes.clear();

        tsdGroupMarkerAttributes.values().forEach(GlobeMarkerAttributes::cleanup);
        tsdGroupMarkerAttributes.clear();
    }

    @Override
    public void globeMapObjectChanged(IGlobeMapObject globeMapObject, MapObjectChangeEvent event)
    {
        if (event == MapObjectChangeEvent.REMOVED) globeMapObjectRemoved(globeMapObject);
    }

    /**
     * A map object has gone away (or gone offline).  If we are working with it, remove our listener registration.
     *
     * @param globeMapObject The globe map object going away
     */
    public synchronized void globeMapObjectRemoved(IGlobeMapObject globeMapObject)
    {
        final IMapObject mapObject = globeMapObject.getMapObject();
        if (mapObject == null || !mapObject.canHaveStyledTrackData()) return;

        int moId = mapObject.getId();

        // And clean up all the marker attrs that refer to this MO

        tsdMarkerAttributes.keySet().stream()
                .filter(scKey -> scKey.mapObjectId == moId)
                .forEach(scKey -> {
                    logger.debug("Cleanup markerAttrs for: {}", scKey);
                    tsdMarkerAttributes.remove(scKey).cleanup();
                });

        tsdGroupMarkerAttributes.keySet().stream()
                .filter(scKey -> scKey.mapObjectId == moId)
                .forEach(scKey -> {
                    logger.debug("Cleanup groupMarkerAttrs for: {}", scKey);
                    tsdGroupMarkerAttributes.remove(scKey).cleanup();
                });
    }

    @Override
    public synchronized GlobeMarkerAttributes getMarkerAttributesForMapPoint(MapPoint mapPoint)
    {
        final ITrackStyleDefinition rdmTsd = smartTrackAssignmentManager.getActiveTSDforRDM(mapPoint.getRdmId());
        final int rdmTsdId = rdmTsd != null ? rdmTsd.getId() : 0;

        return getMarkerAttributesForMapPoint(mapPoint.getMoId(), rdmTsdId);
    }

    @Override
    public GlobeMarkerAttributes getMarkerAttributesForMapPoint(MapPoint mapPoint, Integer tsdId, Integer overrideTsdId)
    {
        final Integer activeTsdId = overrideTsdId != null ? overrideTsdId : tsdId;
        final int rdmTsdId = activeTsdId != null ? activeTsdId : 0;

        return getMarkerAttributesForMapPoint(mapPoint.getMoId(), rdmTsdId);
    }

    /**
     * Get the attributes for the given MO Id and TSD Id.
     *
     * @param moId     MapObject Id
     * @param rdmTsdId active TSD Id, 0 if none
     * @return Marker attributes for this configuration
     */
    private GlobeMarkerAttributes getMarkerAttributesForMapPoint(int moId, int rdmTsdId)
    {
        final IMapObject mapObject = getMapObject(moId);

        // If we don't already have it in the map, then create it
        return tsdMarkerAttributes.computeIfAbsent(scManager.getStylingContextKey(moId, rdmTsdId),
                k -> new GlobeMarkerAttributes(mapObject, scManager.getTrackStylingContext(moId, rdmTsdId)));
    }

    @Override
    public synchronized GlobeMarkerAttributes getMarkerAttributesForMapPointGroup(MapPointGroup mapPointGroup, ITrackStyleDefinition rdmTsd)
    {
        final int moId = mapPointGroup.getMoId();
        final IMapObject mapObject = getMapObject(moId);
        final int rdmTsdId = rdmTsd != null ? rdmTsd.getId() : 0;

        if (mapObject != null)
        {
            // If we don't already have it in the map, then create it
            return tsdGroupMarkerAttributes.computeIfAbsent(scManager.getStylingContextKey(moId, rdmTsdId),
                    k -> new GlobeMarkerGroupAttributes(mapObject, scManager.getTrackStylingContext(moId, rdmTsdId)));
        } else
        {
            logger.error("Missing map object, ID = {}", moId);
        }

        return null;
    }

    /**
     * Given a MapObject Id, return the map object.
     *
     * @param id MapObject id
     * @return MapObject for this id
     */
    private IMapObject getMapObject(Integer id)
    {
        final IMapObjectController moc = mapObjectManager.getMapObjectController(id);

        return moc != null ? moc.getMapObject() : null;
    }
}
