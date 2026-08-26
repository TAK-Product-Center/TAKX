package gov.tak.platform.cot.importer;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.AbstractCotEventTypeImporter;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.commons.utils.StringUtils;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * CoT Importer to handle delete tasks, with CoT type {@link CotTypes#TASK_DELETE}.
 *
 * @since 5.0
 */
@PseudoBean
public class CotDeleteImporter extends AbstractCotEventTypeImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    protected IMapObjectPersist mapObjectPersist;

    @Inject
    protected IMapObjectManager mapObjectManager;

    public CotDeleteImporter()
    {
        super(false, CotTypes.TASK_DELETE);
    }

    @Override
    public ImportResult importData(CotEvent event)
    {
        if (super.importData(event) != ImportResult.SUCCESS) return ImportResult.IGNORE;

        final var detail = event.getDetail();
        if (detail == null) return ImportResult.FAILURE;

        final var link = detail.getFirstChildByName(0, DetailConstants.LINK);
        if (link == null) return ImportResult.FAILURE;

        final String uid = link.getAttribute(DetailConstants.ATTR_UID);
        final String relation = link.getAttribute(DetailConstants.ATTR_RELATION);
        final String type = link.getAttribute(DetailConstants.ATTR_TYPE);

        if (StringUtils.isEmpty(uid) || StringUtils.isEmpty(relation) || StringUtils.isEmpty(type)) return ImportResult.FAILURE;

        var itemToDelete = mapObjectPersist.findMapObjectByUid(uid);

        // Force deleted markers to immediately be made stale (if not already stale)
        if (itemToDelete != null && !itemToDelete.getMetaBoolean(StandardAttributeNames.STALE, false))
        {
            itemToDelete.setMetaBoolean(StandardAttributeNames.FORCE_STALE, true);
        }

        final var force = detail.getFirstChildByName(0, DetailConstants.FORCE_DELETE);
        if (force != null)
        {
            logger.debug("received task 'forcedelete' for: {} of type: {}", uid, type);

            if (itemToDelete != null)
            {
                // See if the MO has disabled remote deletion
                if (!itemToDelete.getMetaBoolean(StandardAttributeNames.REMOTE_DELETE, true)) return ImportResult.FAILURE;

                mapObjectManager.deleteMapObject(itemToDelete);
                return ImportResult.SUCCESS;
            }
        }

        return ImportResult.SUCCESS;

    }
}
