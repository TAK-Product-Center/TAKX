package gov.tak.platform.cot.detail;

import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.takx.api.mapobject.IShapeMapObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Handler for commonly used stroke and fill details:
 * <pre>
 * {@code
 *    <fillColor value="color int"/>
 *    <strokeColor value="color int"/>
 *    <strokeWeight value="double"/>}
 * </pre>
 *
 * @since 3.0
 */
public class StrokeFillDetailHandler extends CotDetailHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int WHITE_RGB = Color.white.getRGB();

    StrokeFillDetailHandler()
    {
        super(new HashSet<>(Arrays.asList(DetailConstants.FILL_COLOR, DetailConstants.STROKE_COLOR, DetailConstants.STROKE_WEIGHT)));
    }

    @Override
    public boolean isSupported(MapItem item, CotEvent event, CotDetail detail)
    {
        return item instanceof IShapeMapObject;
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail)
    {
        final var shape = (IShapeMapObject<?>) item;

        final String fillColor = Integer.toString(shape.getFillColor().getRGB());
        detail.addChild(new CotDetail(DetailConstants.FILL_COLOR, DetailConstants.ATTR_VALUE, fillColor));

        final String lineColor = Integer.toString(shape.getLineColor().getRGB());
        detail.addChild(new CotDetail(DetailConstants.STROKE_COLOR, DetailConstants.ATTR_VALUE, lineColor));

        final String strokeWeight = Double.toString(shape.getLineWidth());
        detail.addChild(new CotDetail(DetailConstants.STROKE_WEIGHT, DetailConstants.ATTR_VALUE, strokeWeight));

        return true;
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event, CotDetail detail)
    {
        final var shape = (IShapeMapObject<?>) item;

        final String name = detail.getElementName();
        final String value = detail.getAttribute(DetailConstants.ATTR_VALUE);

        switch (name)
        {
            case DetailConstants.STROKE_COLOR -> shape.setLineColor(new Color(parseInt(value, WHITE_RGB), true));
            case DetailConstants.FILL_COLOR -> shape.setFillColor(new Color(parseInt(value, WHITE_RGB), true));
            case DetailConstants.STROKE_WEIGHT -> shape.setLineWidth(parseDouble(value, 6.0));
            default ->
            {
                logger.error("Unexpected CoT detail element: {}", name);
                return ImportResult.FAILURE;
            }
        }

        return ImportResult.SUCCESS;
    }
}
