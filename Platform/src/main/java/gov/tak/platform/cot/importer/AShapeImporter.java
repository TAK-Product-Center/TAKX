package gov.tak.platform.cot.importer;

import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.cot.importer.MapItemImporter;
import gov.tak.api.maps.MapItem;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.shapes.IShapeService;
import gov.takx.commons.utils.ParseUtils;
import jakarta.inject.Inject;

import java.awt.Color;

/**
 * Abstract base for CoT Event importers that produce shapes.
 *
 * @since 3.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class AShapeImporter extends MapItemImporter
{
    protected static final int WHITE_RGB = Color.white.getRGB();
    protected static final int TRANSLUCENT_ARGB = 0;

    @Inject
    protected IShapeService shapeService;

    /**
     * Construct on the given type.
     *
     * @param types List of type codes for supported shapes
     */
    protected AShapeImporter(String... types)
    {
        super(false, types);
    }

    @Override
    protected ImportResult importMapItem(MapItem existing, CotEvent event)
    {
        if (!(existing instanceof final IShapeMapObject<?> shape)) return ImportResult.IGNORE;

        // Use callsign as the title.  If no callsign, default to the original name (which is the UID).
        final String callsign = event.getCallsign();
        shape.setTitle(callsign != null ? callsign : shape.getName());

        final AttributeSet attrs = event.getAttributeSet();

        // Let other tools know where this item is coming from
        if (attrs.containsAttribute(StandardAttributeNames.FROM))
        {
            shape.setMetaString(StandardAttributeNames.FROM, attrs.getStringAttribute(StandardAttributeNames.FROM));
        }

        // Set visibility
        shape.setVisible(attrs.getBooleanAttribute(StandardAttributeNames.VISIBLE, shape.isVisible()));

        // Add to group (if needed)
//        addToGroup(existing); TODO

        // Add an empty precision location so the center is processed correctly.
        // Free-form shapes sent from WinTAK (and pre 3.12 version of ATAK) do not have a precision location.
        if (event.findDetail(DetailConstants.PRECISION_LOCATION) == null)
        {
            CotDetail detail = event.getDetail();

            // Create top-level detail if needed
            if (detail == null) event.setDetail(detail = new CotDetail());

            detail.addChild(new CotDetail(DetailConstants.PRECISION_LOCATION));
        }

        // Process detail nodes
        cotDetailManager.processDetails(shape, event);

        shape.setLatestCotEvent(event);

        return ImportResult.SUCCESS;
    }

    /**
     * Extract the ARGB color specified in the {@code color} detail within the given parent detail.
     *
     * @param detail       Detail that should have a {@code color} child detail
     * @param defaultColor The default ARGB color to use if the detail value can't be parsed
     * @return Extracted color, or {@code null} if detail is missing
     */
    protected Color getColorFromDetail(CotDetail detail, int defaultColor)
    {
        final CotDetail color = detail.getFirstChildByName(DetailConstants.COLOR);
        return color != null
                ? new Color(ParseUtils.parseUnsignedInt(color.getInnerText(), 16, defaultColor), true)
                : null;
    }

    /**
     * Add the Battle Position Holding Area metadata to the shape if it is present on the event.
     *
     * @param event          CoT Event to process
     * @param shapeMapObject map object to update
     */
    protected void handleBattlePositionHoldingArea(CotEvent event, IShapeMapObject<?> shapeMapObject)
    {
        if (event.findDetail(DetailConstants.BPHA) != null)
        {
            shapeMapObject.setMetaString(DetailConstants.BPHA, DetailConstants.BPHA);
        }
    }
}
