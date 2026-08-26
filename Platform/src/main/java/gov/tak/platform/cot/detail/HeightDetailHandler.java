package gov.tak.platform.cot.detail;

import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.takx.api.mapobject.IMultiPolylineMapObject;
import gov.takx.api.mapobject.IShapeMapObject;

/**
 * Handler for {@code <height/>} detail tag that is used in multi-polylines and shapes.
 * <p/>
 * The CoT message will also have a {@code height_unit} detail that has a value from 0 to 5 (for KM, M, MI, YD, FT NM),
 * indicating the unit of measure the user selected when entering the height value.  However, the {@code height} value
 * is always in meters in the CoT message.  So, we ae ignoring {@code height_unit} at this time.
 *
 * @since 3.0
 */
public class HeightDetailHandler extends CotDetailHandler
{
    protected HeightDetailHandler()
    {
        super(DetailConstants.HEIGHT);
    }

    @Override
    public boolean isSupported(MapItem item, CotEvent event, CotDetail detail)
    {
        // TODO KMB: Technically, the height detail also exists in the polylines that make up the multi-polyline, but it
        //  always has a value of 0 and we don't really have a concept of height on polylines, so I'm not going to worry
        //  about it right now.
        return item instanceof IShapeMapObject<?> || item instanceof IMultiPolylineMapObject;
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event, CotDetail detail)
    {
        final double heightMeters = parseDouble(detail.getAttribute(DetailConstants.ATTR_VALUE), Double.NaN);
        if (Double.isNaN(heightMeters)) return ImportResult.IGNORE;

        if (item instanceof IMultiPolylineMapObject multiPolyline)
        {
            multiPolyline.setHeightMeters(heightMeters);
        } else if (item instanceof IShapeMapObject<?> shape && shape.canUseVerticalHeight())
        {
            if (heightMeters != 0)
            {
                shape.setVerticalHeightEnabled(true);
                shape.setVerticalHeight(heightMeters);
            } else
            {
                shape.setVerticalHeightEnabled(false);
            }
        }

        return ImportResult.SUCCESS;
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail)
    {
        if (item instanceof IMultiPolylineMapObject multiPolyline)
        {
            if (multiPolyline.hasHeight())
            {
                addHeightDetail(detail, multiPolyline.getHeightMeters());
                return true;
            }
        } else if (item instanceof IShapeMapObject<?> shape)
        {
            if (shape.isExtruded())
            {
                addHeightDetail(detail, shape.getVerticalHeight());
                return true;
            }
        }

        return false;
    }

    /**
     * Create and add the {@code height} detail to the root detail.
     * @since 3.5
     */
    private void addHeightDetail(CotDetail rootDetail, double height)
    {
        rootDetail.addChild(new CotDetail(DetailConstants.HEIGHT, DetailConstants.ATTR_VALUE, Double.toString(height)));
    }
}
