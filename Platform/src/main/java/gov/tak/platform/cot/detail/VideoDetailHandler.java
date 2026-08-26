package gov.tak.platform.cot.detail;

import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.tak.api.video.IVideoConnectionManager;
import gov.takx.api.annotation.PseudoBean;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import static gov.tak.api.video.cot.VideoDetailHandler.DETAIL;

/**
 * Processes the {@code <__video>} detail tag, as well as its inner {@code <ConnectionEntry .../>} tag
 * @since 5.3
 */
@PseudoBean
public class VideoDetailHandler extends CotDetailHandler
{
    private gov.tak.api.video.cot.VideoDetailHandler videoDetailHandler;

    @Inject
    private IVideoConnectionManager videoConnectionManager;

    protected VideoDetailHandler()
    {
        super(DETAIL);
    }

    @PostConstruct
    private void postConstruct()
    {
        videoDetailHandler = new gov.tak.api.video.cot.VideoDetailHandler(videoConnectionManager);
    }

    @Override
    public boolean toCotDetail(MapItem item, CotEvent event, CotDetail root)
    {
        return videoDetailHandler.toCotDetail(item, item.getAttributeSet(), event, root);
    }

    @Override
    public ImportResult toItemMetadata(MapItem item, CotEvent event, CotDetail detail)
    {
        return ImportResult.convertKernelResult(videoDetailHandler.toItemMetadata(item, item.getAttributeSet(), event, detail));
    }
}
