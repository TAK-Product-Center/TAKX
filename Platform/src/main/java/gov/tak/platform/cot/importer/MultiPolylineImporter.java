package gov.tak.platform.cot.importer;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.cot.importer.MapItemImporter;
import gov.tak.api.maps.MapItem;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.ICompoundMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IMultiPolylineMapObject;
import gov.takx.api.mapobject.IShapePathMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.dataservices.persist.MultiPolylineMapObject;
import gov.takx.dataservices.persist.ShapePathMapObject;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * CoT event importer that produces {@link MultiPolylineMapObject}s, including {@link ShapePathMapObject}s for the
 * included polylines.
 *
 * @since 3.0
 */
@PseudoBean
public class MultiPolylineImporter extends MapItemImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IMapObjectManager mapObjectManager;

    public MultiPolylineImporter()
    {
        super(false, CotTypes.SHAPE_MULTI_POLYLINE);
    }

    @Override
    protected ImportResult importMapItem(MapItem existing, CotEvent event)
    {
        if (existing != null && !(existing instanceof IMultiPolylineMapObject)) return ImportResult.FAILURE;

        String uid = event.getUID();
        final GeoPoint location = new GeoPoint(event.getPoint());
        String name = event.getCallsign();
        final CotDetail detail = event.getDetail();
        if (detail == null) return ImportResult.FAILURE;

        List<IShapePathMapObject> polylines = new ArrayList<>();

        final PolyShapeImporter shapeImporter = new PolyShapeImporter(ICompoundMapObject.COMPONENT_MO_OPTIONS);

        for (CotDetail link : detail.getChildrenByName(DetailConstants.LINK))
        {
            if (link.getAttribute(DetailConstants.ATTR_RELATION) != null) continue;

            final String linkLine = link.getAttribute(DetailConstants.ATTR_LINE);
            final CotEvent cotEvent = CotEvent.parse(linkLine);
            MapItem item = findItem(cotEvent);

            if (shapeImporter.importMapItem(item, cotEvent) == ImportResult.SUCCESS)
            {
                // If the item didn't already exist, find it now
                if (item == null) item = findItem(cotEvent);

                if (item instanceof IShapePathMapObject)
                {
                    polylines.add((IShapePathMapObject) item);
                } else
                {
                    logger.warn("Wrong link type included in multi-polyline: {}", item.getClass());
                }
            }
        }

        IMultiPolylineMapObject multiPolyline = (IMultiPolylineMapObject) existing;
        if (multiPolyline == null)
        {
            final MapObjectSpec spec = MapObjectSpec.builder(KeyUtils.COMPOUND_FAMILY, IMultiPolylineMapObject.MULTI_POLYLINE_TYPE, uid)
                    .uid(uid)
                    .location(location)
                    .name(name)
                    .build();
            multiPolyline = (IMultiPolylineMapObject) mapObjectManager.getOrCreateMapObject(spec);
        } else
        {
            multiPolyline.setLocation(location, false);
            multiPolyline.setName(name);
        }

        updatePolylines(multiPolyline, polylines);

        // The height element is handled by HeightDetailHandler within CotDetailManager
        cotDetailManager.processDetails(multiPolyline, event);

        multiPolyline.setLatestCotEvent(event);

        return ImportResult.SUCCESS;
    }

    /**
     * Updates the multi-polyline with the new polylines, removing any that have been deleted.
     *
     * @param multiPolyline The multi-polyline map object
     * @param polylines     The list of new/updated polylines from the CoT message
     */
    private void updatePolylines(@Nonnull IMultiPolylineMapObject multiPolyline, @Nonnull List<IShapePathMapObject> polylines)
    {
        // Remove and delete any polylines that are no longer part of the multi-polyline
        multiPolyline.getPolylines().stream().filter(p -> !polylines.contains(p))
                .forEach(polyline -> {
                    multiPolyline.removePolyline(polyline);
                    mapObjectManager.deleteMapObject(polyline);
                });

        // Add all the new ones (don't worry about checking if they are already present since multi-polyline does that)
        polylines.forEach(multiPolyline::addPolyline);
    }
}
