package gov.tak.platform.cot.importer;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.mapobject.IShapeEllipseMapObject;
import gov.takx.api.shapes.ShapeAlreadyExistsException;
import gov.takx.dataservices.shapes.Ellipse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static gov.tak.api.cot.importer.ImportResult.DEFERRED;

/**
 * CoT Event Importer that produces Ellipse shapes.
 *
 * @since 3.0
 */
public class EllipseImporter extends AEllipseImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // Offset to apply to the major and minor axes of an ellipse because these values are doubled in COT messages
    private static final double SCALING_FACTOR = 0.5;

    public EllipseImporter()
    {
        super(CotTypes.SHAPE_ELLIPSE);
    }

    @Override
    protected ImportResult importMapItem(MapItem existing, CotEvent event)
    {
        if (existing != null && !(existing instanceof IShapeEllipseMapObject))
        {
            logger.warn("Import failure: expected ellipse map object but was {}", existing.getClass());
            return ImportResult.FAILURE;
        }

        final ExtractedData extractedData = extractEventData(event, false);

        if (extractedData.importResult == ImportResult.FAILURE) return ImportResult.FAILURE;

        final String uid = event.getUID();
        final GeoPoint location = new GeoPoint(event.getPoint());

        final double semiMajorAxis = extractedData.baseSemiMajorAxis * SCALING_FACTOR;
        final double semiMinorAxis = extractedData.baseSemiMinorAxis * SCALING_FACTOR;
        // WorldWind measures orientation from the minor axis while the orientation will be sent relative to the major axis
        final double orientation = (extractedData.baseOrientation + 90) % 360;
        final int ringCount = extractedData.ringCount;

        IShapeEllipseMapObject ellipseMapObject = (IShapeEllipseMapObject) existing;

        if (ellipseMapObject == null) // Need to create new shape
        {
            try
            {
                ellipseMapObject = shapeService.createEllipseMapObject(new Ellipse(uid, location, orientation, semiMajorAxis, semiMinorAxis));
                ellipseMapObject.setRingCount(ringCount);
            } catch (ShapeAlreadyExistsException e)
            {
                logger.error("Failed to create Ellipse, UID={}", uid, e);
                return ImportResult.FAILURE;
            }
        } else // Update existing shape
        {
            ellipseMapObject.setLocation(location);
            ellipseMapObject.setAxes(semiMajorAxis, semiMinorAxis);
            ellipseMapObject.setOrientation(orientation);
            ellipseMapObject.setRingCount(ringCount);
        }

        processStyleLink(extractedData, ellipseMapObject);
        if (processCenterAnchor(extractedData, ellipseMapObject) == DEFERRED) return DEFERRED;

        handleBattlePositionHoldingArea(event, ellipseMapObject);

        return super.importMapItem(ellipseMapObject, event);
    }
}
