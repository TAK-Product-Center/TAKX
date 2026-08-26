package gov.tak.platform.cot.importer;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.AbstractCotEventTypeImporter;
import gov.tak.api.cot.importer.ImportResult;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.persist.IMapObjectPersist;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Responsible for the deletion of Sensor Points of Interest (SPIs), indicated by CoT type {@link CotTypes#SENSOR_POINT_OF_INTEREST}.
 * <p>
 * An SPI message is considered to be a "delete" message if the start and stale times are equal.
 * 
 * @since 5.0
 */
@PseudoBean
public class SpiDeleteImporter extends AbstractCotEventTypeImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IMapObjectPersist mapObjectPersist;

    @Inject
    private IMapObjectManager mapObjectManager;

    public SpiDeleteImporter()
    {
        super(false, CotTypes.SENSOR_POINT_OF_INTEREST);
    }

    @Override
    public ImportResult importData(CotEvent event)
    {
        if (super.importData(event) != ImportResult.SUCCESS) return ImportResult.IGNORE;

        final var detail = event.getDetail();
        if (detail == null) return ImportResult.FAILURE;

        final var link = detail.getFirstChildByName(0, DetailConstants.LINK);
        final var contact = detail.getFirstChildByName(0, DetailConstants.CONTACT);

        if (link == null || contact == null) return ImportResult.FAILURE;

        final var spiToDelete = mapObjectPersist.findMapObjectByUid(event.getUID());

        if (spiToDelete != null && event.getStart().equals(event.getStale()))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Received SPI CoT message for deletion of {}, deleting MO", contact.getAttribute(DetailConstants.ATTR_CALLSIGN));
            }

            // We've received a digital-pointer deletion, which cannot be disabled by the MO. So, go straight to deleting the MO.
            mapObjectManager.deleteMapObject(spiToDelete);
            return ImportResult.SUCCESS;
        }

        return ImportResult.IGNORE;
    }
}
