package gov.tak.platform.cot.detail;

import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.tak.api.maps.PointMapItem;
import gov.tak.api.maps.coords.GeoPointMetaData;

import static gov.tak.api.cot.detail.DetailConstants.PRECISION_LOCATION;

/**
 * Handles {@code <precisionlocation>} detail element.
 *
 * @since 3.4.1
 */
public class PrecisionLocationDetailHandler extends CotDetailHandler
{
    PrecisionLocationDetailHandler()
    {
        super(PRECISION_LOCATION);
    }

    @Override
    public boolean isSupported(MapItem item, CotEvent event, CotDetail detail)
    {
        return item instanceof PointMapItem;
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail)
    {
        if (item instanceof PointMapItem pointMapItem)
        {
            final GeoPointMetaData metaData = pointMapItem.getGeoPointMetaData();
            final String geopointSource = metaData.getGeopointSource();
            final String altitudeSource = metaData.getAltitudeSource();

            // Only add the detail if we have something other than UNKNOWN values
            if (!geopointSource.equals(GeoPointMetaData.UNKNOWN) || !altitudeSource.equals(GeoPointMetaData.UNKNOWN))
            {
                final CotDetail childDetail = new CotDetail(PRECISION_LOCATION);

                childDetail.setAttribute(GeoPointMetaData.GEOPOINT_SOURCE, geopointSource);
                childDetail.setAttribute(GeoPointMetaData.ALTITUDE_SOURCE, altitudeSource);
                detail.addChild(childDetail);
                return true;
            }
        }
        return false;
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event, CotDetail detail)
    {
        final GeoPointMetaData metaData = ((PointMapItem) item).getGeoPointMetaData();

        metaData.setGeoPointSource(detail.getAttribute(GeoPointMetaData.GEOPOINT_SOURCE, GeoPointMetaData.UNKNOWN));
        metaData.setAltitudeSource(detail.getAttribute(GeoPointMetaData.ALTITUDE_SOURCE, GeoPointMetaData.UNKNOWN));
        return ImportResult.SUCCESS;
    }
}
