package gov.tak.platform.cot.importer;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.mapobject.IShapePathMapObject;
import gov.takx.api.mapobject.MapObjectOptions;
import gov.takx.api.shapes.IShapeService.ShapeOptions;
import gov.takx.api.shapes.ShapeAlreadyExistsException;
import gov.takx.dataservices.shapes.Polygon;
import gov.takx.dataservices.shapes.Polyline;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * CoT Event Importer that produces Polygon/Polyline shapes.
 *
 * @since 3.0
 */
public class PolyShapeImporter extends ACorneredShapeImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final MapObjectOptions mapObjectOptions;

    public PolyShapeImporter()
    {
        this(null);
    }

    /**
     * Constructor which supports specifying custom map object options.
     *
     * @param mapObjectOptions The custom map object options to use when creating the shapes
     */
    public PolyShapeImporter(@Nullable MapObjectOptions mapObjectOptions)
    {
        super(CotTypes.SHAPE_POLYSHAPE);
        this.mapObjectOptions = mapObjectOptions;
    }

    @Override
    protected ImportResult importMapItem(MapItem existing, CotEvent event)
    {
        if (existing != null && !(existing instanceof IShapePathMapObject))
        {
            logger.warn("Import failure: expected path map object but was {}", existing.getClass());
            return ImportResult.FAILURE;
        }

        final CotDetail detail = event.getDetail();
        if (detail == null)
        {
            logger.warn("Import failure: detail was null.");
            return ImportResult.FAILURE;
        }

        final List<GeoPointMetaData> points = getPoints(detail, false);

        if (points.size() < 2)
        {
            logger.error("Too few points to create polyline/polygon; from {}", event);
            return ImportResult.FAILURE;
        } else
        {
            return importPolyShape(event, existing, points);
        }
    }

    private ImportResult importPolyShape(CotEvent event, MapItem existing, List<GeoPointMetaData> points)
    {
        IShapePathMapObject<?> polyShapeMapObject = (IShapePathMapObject<?>) existing;

        if (polyShapeMapObject == null) // Need to create new shape
        {
            final String uid = event.getUID();
            final boolean closed = points.get(0).get().equals(points.get(points.size() - 1).get());

            try
            {
                polyShapeMapObject = closed
                        ? shapeService.createPolygonMapObject(new Polygon(uid, GeoPointMetaData.unwrap(points)), new ShapeOptions(false, mapObjectOptions))
                        : shapeService.createPolylineMapObject(new Polyline(uid, GeoPointMetaData.unwrap(points)), new ShapeOptions(false, mapObjectOptions));

            } catch (ShapeAlreadyExistsException e)
            {
                logger.error("Failed to create {}, UID={}", closed ? "polygon" : "polyline", uid, e);
                return ImportResult.FAILURE;
            }
        } else // Update existing shape
        {
            polyShapeMapObject.setVertices(GeoPointMetaData.unwrap(points));
        }

        return super.importMapItem(polyShapeMapObject, event);
    }
}
