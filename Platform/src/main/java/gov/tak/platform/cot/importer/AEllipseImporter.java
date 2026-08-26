package gov.tak.platform.cot.importer;

import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.commons.utils.ParseUtils;
import gov.takx.commons.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base for CoT Event importers that produce ellipses.
 *
 * @since 3.0
 */
@PseudoBean
public abstract class AEllipseImporter extends AShapeImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Construct on the given type.
     *
     * @param types List of type codes for supported shapes
     */
    protected AEllipseImporter(String... types)
    {
        super(types);
    }

    /**
     * Extract the common data elements from this event.  Overall success/failure of the operation can be found
     * in {@link ExtractedData#importResult}.
     *
     * @param event      CoT Event to process
     * @param wantCircle {@code true} if we're looking for a circle.  If true, and any of the ellipse details
     *                   are irregular, then {@link ExtractedData#isCircle} will be set false,
     *                   and a value of {@link ImportResult#IGNORE IGNORE} will be set in
     *                   {@link ExtractedData#importResult}.
     * @return extracted data
     */
    protected ExtractedData extractEventData(CotEvent event, boolean wantCircle)
    {
        final ExtractedData extractedData = new ExtractedData(event);

        if (extractedData.importResult != ImportResult.SUCCESS) return extractedData;

        for (CotDetail ellipse : extractedData.shapeDetail.getChildrenByName(DetailConstants.ELLIPSE))
        {
            if (extractedData.addEllipse(ellipse, wantCircle) != ImportResult.SUCCESS) return extractedData;
        }

        // We require at least one ellipse element
        if (extractedData.ringCount == 0)
        {
            logger.warn("Import failure: No ellipses present in CoT message of type {}", event.getType());
            extractedData.importResult = ImportResult.FAILURE;
        }

        return extractedData;
    }

    /**
     * If a link relation for a center anchor is present, then install it.
     *
     * @param extractedData  Data extracted from CoT event
     * @param shapeMapObject Map Object being constructed
     */
    protected ImportResult processCenterAnchor(ExtractedData extractedData, IShapeMapObject<?> shapeMapObject)
    {
        final CotDetail detail = extractedData.linkRelations.get(DetailConstants.LINK_RELATION_CENTER_ANCHOR);

        if (detail != null)
        {
            final String uid = detail.getAttribute(DetailConstants.ATTR_UID);
            final IMapObject centerAnchor = mapObjectPersist.findMapObjectByUid(uid);

            if (centerAnchor != null)
            {
                shapeMapObject.setCenterAnchor(centerAnchor);
            } else
            {
                logger.warn("Unable to locate center anchor '{}' for {} - DEFER", uid, shapeMapObject.getKey());

                return ImportResult.DEFERRED;
            }
        }
        return ImportResult.SUCCESS;
    }

    /**
     * Process any KML Style detail found in the extracted data.
     *
     * @param extractedData  Extracted shape data
     * @param shapeMapObject Shape to update with extracted style data
     */
    protected void processStyleLink(ExtractedData extractedData, IShapeMapObject<?> shapeMapObject)
    {
        if (extractedData.styleDetail != null)
        {
            applyStyleDetail(extractedData.styleDetail, shapeMapObject);
        }
    }

    /**
     * Parse the color, width, and fill style details and apply it to the given shape.
     * <p>
     * Note that although ATAK 3.12 stores color details the same way as other shapes,
     * we still need to be backwards compatible.
     *
     * @param style          Style detail
     * @param shapeMapObject Shape to update
     */
    protected void applyStyleDetail(CotDetail style, IShapeMapObject<?> shapeMapObject)
    {
        if (style == null || shapeMapObject == null) return;

        final CotDetail lineStyle = style.getFirstChildByName(DetailConstants.LINE_STYLE);
        if (lineStyle != null)
        {
            final Color color = getColorFromDetail(lineStyle, WHITE_RGB);
            if (color != null) shapeMapObject.setLineColor(color);

            final CotDetail width = lineStyle.getFirstChildByName(DetailConstants.WIDTH);
            if (width != null) shapeMapObject.setLineWidth(ParseUtils.parseDouble(width.getInnerText(), 4));
        }

        final CotDetail polyStyle = style.getFirstChildByName(DetailConstants.POLY_STYLE);
        if (polyStyle != null)
        {
            final Color color = getColorFromDetail(polyStyle, TRANSLUCENT_ARGB);
            if (color != null) shapeMapObject.setFillColor(color);
        }
    }

    /**
     * Extracted shape data from a CotEvent, including all ellipse details.
     */
    protected static final class ExtractedData
    {
        ImportResult importResult = ImportResult.SUCCESS;
        final String uid;
        final CotDetail shapeDetail;
        final Map<String, CotDetail> linkRelations = new HashMap<>(); // Link details with "relation" attributes

        CotDetail styleDetail;

        // Since the rings are just multiples of the radii of the smallest circle/ellipse, we just need to determine
        // the smallest values and the number of rings; we don't need to keep the actual circles/ellipses since they
        // will always be computed and rendered based on the values of the base shape.
        double baseSemiMinorAxis = Double.MAX_VALUE;
        double baseSemiMajorAxis = Double.MAX_VALUE;
        double baseOrientation = 0;
        boolean isCircle = true;
        int ringCount = 0;

        ExtractedData(CotEvent event)
        {
            uid = event.getUID();
            shapeDetail = event.findDetail(DetailConstants.SHAPE);

            if (shapeDetail == null)
            {
                logger.warn("Import failure: No shape detail present in CoT event for type {}", event.getType());
                importResult = ImportResult.FAILURE;
            } else
            {
                processLinks();
            }
        }

        /**
         * Extract data from link child details.
         */
        private void processLinks()
        {
            for (CotDetail link : shapeDetail.getChildrenByName(DetailConstants.LINK))
            {
                if (DetailConstants.LINK_TYPE_KML_STYLE.equals(link.getAttribute(DetailConstants.ATTR_TYPE)))
                {
                    styleDetail = link.getFirstChildByName(DetailConstants.STYLE);
                } else
                {
                    final String uid = link.getAttribute(DetailConstants.ATTR_UID);
                    final String relation = link.getAttribute(DetailConstants.ATTR_RELATION);

                    if (!StringUtils.isEmpty(relation) && !StringUtils.isEmpty(uid))
                    {
                        linkRelations.put(relation, link);
                    }
                }
            }
        }

        /**
         * Process the given ellipse detail. The minimum radii values are maintained and updated as each ellipse is added.
         *
         * @param ellipse    Ellipse detail to process
         * @param wantCircle {@code true} if we're looking for a circle.  If true, and any of the ellipse details
         *                   are irregular, then {@link #isCircle} will be set false, and a value of
         *                   {@link ImportResult#IGNORE IGNORE} will be set in {@link #importResult}.
         * @return {@link ImportResult#SUCCESS SUCCESS} if all data is valid, {@link ImportResult#IGNORE IGNORE} if
         * looking for a circle, but found a non-circular ellipse, {@link ImportResult#FAILURE FAILURE} if any
         * data are missing or invalid.
         */
        ImportResult addEllipse(CotDetail ellipse, boolean wantCircle)
        {
            // Even though the values are called "major" and "minor", through testing against WinTAK, they actually
            // represent the semi-major and semi-minor axes (that is, the radii).
            final double semiMajorAxis = ParseUtils.parseDouble(ellipse.getAttribute(DetailConstants.ATTR_MAJOR));
            final double semiMinorAxis = ParseUtils.parseDouble(ellipse.getAttribute(DetailConstants.ATTR_MINOR));
            final double orientation = ParseUtils.parseDouble(ellipse.getAttribute(DetailConstants.ATTR_ANGLE));

            if (semiMinorAxis == 0.0 || semiMajorAxis == 0.0)
            {
                logger.error("Import failure: Ellipse has invalid axes! UID={}", uid);
                return importResult = ImportResult.FAILURE;
            }

            // Check for irregular ellipse. Modulus by 360 since that is what WinTAK uses for circles
            isCircle = isCircle && (semiMinorAxis == semiMajorAxis) && orientation % 360 == 0;

            if (wantCircle && !isCircle) return importResult = ImportResult.IGNORE;

            baseSemiMinorAxis = Math.min(baseSemiMinorAxis, semiMinorAxis);
            baseSemiMajorAxis = Math.min(baseSemiMajorAxis, semiMajorAxis);
            baseOrientation = orientation;
            ringCount++;

            return ImportResult.SUCCESS;
        }
    }
}
