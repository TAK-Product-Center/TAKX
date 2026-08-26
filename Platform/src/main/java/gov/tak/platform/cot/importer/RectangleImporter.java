package gov.tak.platform.cot.importer;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.shapes.IMutableRectangle;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.shapes.ShapeAlreadyExistsException;
import gov.takx.dataservices.shapes.Rectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * CoT Event Importer that produces Rectangle shapes.
 *
 * @since 3.0
 */
public class RectangleImporter extends ACorneredShapeImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public RectangleImporter()
    {
        super(CotTypes.SHAPE_RECTANGLE);
    }

    @Override
    protected ImportResult importMapItem(MapItem existing, CotEvent event)
    {
        if (existing != null && !(existing instanceof IRectangle))
        {
            logger.warn("Import failure: expected rectangle map object but was {}", existing.getClass());
            return ImportResult.FAILURE;
        }

        final String uid = event.getUID();

        final CotDetail detail = event.getDetail();
        if (detail == null)
        {
            logger.warn("Import failure: detail was null.");
            return ImportResult.FAILURE;
        }

        List<GeoPointMetaData> points = getPoints(detail, true);

        if (points.size() < 4)
        {
            logger.error("Rectangle only has {} points! UID={}", points.size(), uid);
            return ImportResult.FAILURE;
        } else if (points.size() > 4) // Don't process more than 4 points
        {
            logger.warn("Ignoring extra points in Rectangle definition; UID={}", uid);
            points = points.subList(0, 4);
        }

        IShapeMapObject<?> rectangleMapObject;
        // CoT vertices are ordered NW, SW, SE, NE
        List<GeoPoint> unwrapped = GeoPointMetaData.unwrap(points);
        // Rectangle vertices are ordered SW, SE, NE, NW
        List<ILocation> vertices = List.of(unwrapped.get(1), unwrapped.get(2), unwrapped.get(3), unwrapped.get(0));

        if (existing == null)
        {
            // Need to create new shape
            try
            {
                final Rectangle rectangle = new Rectangle(uid, vertices);
                rectangleMapObject = shapeService.createRectangleMapObject(rectangle);
            } catch (ShapeAlreadyExistsException e)
            {
                logger.error("Failed to create Rectangle, UID={}", uid, e);
                return ImportResult.FAILURE;
            }
        } else
        {
            // Update existing shape
            rectangleMapObject = (IShapeMapObject<?>) existing;
            IMutableRectangle mutableRectangle = (IMutableRectangle) existing;
            mutableRectangle.setVertices(vertices);
        }

        handleBattlePositionHoldingArea(event, rectangleMapObject);

        return super.importMapItem(rectangleMapObject, event);
    }
}
