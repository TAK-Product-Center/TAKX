package gov.tak.platform.cot.detail;

import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.tak.api.maps.Marker;
import gov.takx.commons.utils.ParseUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.utils.UnitsOfMeasureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static gov.tak.api.cot.detail.DetailConstants.TRACK;

/**
 * Handles "track" detail element.
 *
 * @since 3.0
 */
public class TrackDetailHandler extends CotDetailHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Above this speed, display heading arrow
     */
    private final static double MIN_SPEED_MPS = 3 * UnitsOfMeasureUtils.MPH_TO_MPS_MULTIPLIER; // 3 MPH in meters/sec

    TrackDetailHandler()
    {
        super(TRACK);
    }

    @Override
    public boolean isSupported(MapItem item, CotEvent event, CotDetail detail)
    {
        return item instanceof Marker;
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail)
    {
        return false; // Only exported on self SA message
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event, CotDetail detail)
    {
        final Marker marker = (Marker) item;

        try
        {
            final String headingS = detail.getAttribute(DetailConstants.ATTR_COURSE);
            double heading = Double.NaN;

            if (!StringUtils.isEmpty(headingS))
            {
                // Note: it is valid to have degrees greater than 360 since we wrap the degrees; in those cases we
                // simply mod by 360. In the field, systems have been encountered that send infinity OR a super large
                // number besides infinity. To prevent this, we bound the angle by an arbitrary amount known generally
                // not to occur in real life. The value below was copied from ATAK source and should not be interpreted
                // to have a specific meaning. It is not a typo for 360, and it does not mean to imply some kind of
                // precision preservation.
                heading = ParseUtils.parseDouble(headingS);
                if (Math.abs(heading) > 3600)
                {
                    heading = Double.NaN;
                } else
                {
                    heading = heading % 360;
                    if (heading < 0) heading += 360;
                }
            }

            final String speedS = detail.getAttribute(DetailConstants.ATTR_SPEED);
            double speed = StringUtils.isEmpty(speedS) ? 0 : ParseUtils.parseDouble(speedS);

            if (!Double.isNaN(heading) && !Double.isNaN(speed) && speed > MIN_SPEED_MPS)
            {
                // set heading style
                marker.setStyle(marker.getStyle() | Marker.STYLE_ROTATE_HEADING_MASK);
                marker.setStyle(marker.getStyle() & ~Marker.STYLE_ROTATE_HEADING_NOARROW_MASK);
            } else
            {
                // clear heading style
                marker.setStyle(marker.getStyle() & ~Marker.STYLE_ROTATE_HEADING_MASK);
            }
            marker.setTrack(heading, speed);
            return ImportResult.SUCCESS;
        } catch (Exception ex)
        {
            logger.error("Failed to parse marker track: {}", marker.getUid(), ex);
        }

        // clear data
        marker.setTrack(Double.NaN, Double.NaN);
        marker.setStyle(marker.getStyle() & ~Marker.STYLE_ROTATE_HEADING_MASK);

        return ImportResult.FAILURE;
    }
}
