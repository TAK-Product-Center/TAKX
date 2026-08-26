package gov.tak.platform.cot.importer;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.AbstractCotEventTypeImporter;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.PointMapItem;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.emergency.IEmergencyAlertManager;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.persist.IMapObjectPersist;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static gov.tak.api.cot.CotTypes.EMERGENCY_CANCEL;

/**
 * CoT Importer to handle emergency alert cancel tasks, with CoT type {@link CotTypes#EMERGENCY_CANCEL}.
 *
 * @since 5.7
 */
@PseudoBean
public class EmergencyCancelImporter extends AbstractCotEventTypeImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IEmergencyAlertManager emergencyAlertManager;

    @Inject
    protected IMapObjectPersist mapObjectPersist;

    @Inject
    protected IMapObjectManager mapObjectManager;

    public EmergencyCancelImporter()
    {
        super(false, EMERGENCY_CANCEL);
    }

    @Override
    public ImportResult importData(CotEvent event)
    {
        if (super.importData(event) != ImportResult.SUCCESS) return ImportResult.IGNORE;

        final var detail = event.getDetail();
        if (detail == null) return ImportResult.FAILURE;

        final var emergency = detail.getFirstChildByName(0, "emergency");
        if (emergency == null) return ImportResult.FAILURE;

        final String uid = event.getUID();
        final boolean cancel = Boolean.parseBoolean(emergency.getAttribute("cancel"));

        if (!cancel)
        {
            logger.warn("CoT message of type {} received; but 'cancel' was missing/false; ignoring event for UID {}", EMERGENCY_CANCEL, uid);
            return ImportResult.FAILURE;
        }

        logger.debug("Remove emergency marker for {}", uid);

        var itemToDelete = mapObjectPersist.findMapObjectByUid(uid);
        if (!(itemToDelete instanceof PointMapItem pointMapItem))
        {
            logger.warn("No Marker found for emergency cancellation, UID={}", uid);
            return ImportResult.FAILURE;
        }

        emergencyAlertManager.removeAlert(pointMapItem);

        return ImportResult.SUCCESS;

    }
}
