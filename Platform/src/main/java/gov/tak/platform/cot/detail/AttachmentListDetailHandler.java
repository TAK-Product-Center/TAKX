package gov.tak.platform.cot.detail;

import com.atakmap.filesystem.HashingUtils;
import gov.tak.api.comms.takserver.IDataSyncManager;
import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.takx.api.annotation.PseudoBean;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Processes the {@code <attachment_list hashes="[...]""/>} detail tag.
 *
 * @since 5.3
 */
@PseudoBean
public class AttachmentListDetailHandler extends CotDetailHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String DETAIL_NAME = "attachment_list";
    private static final String ATTRIBUTE_HASHES_NAME = "hashes";

    private static final long UNMATCHED_WARNING_PERIOD = 2000;
    private long lastUnmatchedWarning;

    @Inject
    private IDataSyncManager dataSyncManager;

    AttachmentListDetailHandler()
    {
        super(DETAIL_NAME);
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail detail)
    {
        List<File> attachments = item.getAttachments();
        if (attachments.isEmpty()) return false;

        final String json = "[\"" + attachments.stream().map(HashingUtils::sha256sum).collect(Collectors.joining("\",\"")) + "\"]";

        final CotDetail attachmentListDetail = new CotDetail(DETAIL_NAME);
        attachmentListDetail.setAttribute(ATTRIBUTE_HASHES_NAME, json);
        detail.addChild(attachmentListDetail);

        return true;
    }

    @Override
    public ImportResult toItemMetadata(MapItem mapItem, CotEvent event, CotDetail detail)
    {
        String attributeListJson = detail.getAttribute(ATTRIBUTE_HASHES_NAME);
        if (attributeListJson == null) return ImportResult.IGNORE;

        String[] potentialHashes = attributeListJson.split("\\W+");
        if (potentialHashes.length == 0) return ImportResult.IGNORE;

        Set<String> unmatchedAttachments = new HashSet<>();
        for (String potentialHash : potentialHashes)
        {
            // Skip matches that cannot be sha256 hashes
            if (potentialHash.length() != 64) continue;

            // Attach the file if data sync is aware of a file by the given hash
            dataSyncManager.attachResourceByHash(mapItem, potentialHash).whenComplete((file, error) -> {
                if (error != null || file == null) unmatchedAttachments.add(potentialHash);
            });
        }

        // Notify users of any missing attachments
        long now = System.currentTimeMillis();
        if (!unmatchedAttachments.isEmpty() && now - lastUnmatchedWarning > UNMATCHED_WARNING_PERIOD)
        {
            logger.warn("Could not locate {} data-sync resources when attaching to map item '{}'", unmatchedAttachments.size(), mapItem.getTitle());
            lastUnmatchedWarning = now;
        }

        return ImportResult.SUCCESS;
    }
}
