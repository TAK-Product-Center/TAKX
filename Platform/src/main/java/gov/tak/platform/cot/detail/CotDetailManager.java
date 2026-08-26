package gov.tak.platform.cot.detail;

import gov.tak.api.cot.detail.CotDetailHandler;
import gov.tak.api.cot.detail.ICotDetailManager;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Implementation of {@link ICotDetailManager}.  Since 5.5.1, delegates all operations to the kernel's
 * {@link gov.tak.api.cot.detail.CotDetailManager CotDetailManager}.
 *
 * @since 3.0
 */
@ApplicationScoped
public class CotDetailManager implements ICotDetailManager
{
    private gov.tak.api.cot.detail.CotDetailManager kernelDetailManager;

    /**
     * Start our service, register default handlers.
     */
    public void startService()
    {
        kernelDetailManager = new gov.tak.api.cot.detail.CotDetailManager();
        registerDefaultHandlers();
    }

    /**
     * Stop this service, tear down all resources.
     */
    public void stopService()
    {
        kernelDetailManager = null;
    }

    @Override
    public void registerHandler(CotDetailHandler handler)
    {
        kernelDetailManager.registerHandler(handler);
    }

    @Override
    public void unregisterHandler(CotDetailHandler handler)
    {
        kernelDetailManager.unregisterHandler(handler);
    }

    @Override
    public ImportResult processDetails(MapItem item, CotEvent event)
    {
        return ImportResult.convertKernelResult(kernelDetailManager.processDetails(item, item.getAttributeSet(), event));
    }

    @Override
    public boolean addDetails(@Nonnull MapItem mapItem, @Nonnull CotEvent event)
    {
        return kernelDetailManager.addDetails(mapItem, mapItem.getAttributeSet(), event);
    }

    /**
     * Register our default (core) detail handlers.
     */
    private void registerDefaultHandlers()
    {
        registerHandler(new ArchiveDetailHandler());
        registerHandler(new AttachmentListDetailHandler());
        registerHandler(new BreadCrumbsDetailHandler());
        registerHandler(new ColorDetailHandler());
        registerHandler(new ContactDetailHandler());
        registerHandler(new EmergencyDetailHandler());
        registerHandler(new GeofenceDetailHandler());
        registerHandler(new HeightDetailHandler());
        registerHandler(new ImageDetailHandler());
        registerHandler(new LinkAttributeDetailHandler());
        registerHandler(new LinkDetailHandler());
        registerHandler(new MedicalDetailHandler());
        registerHandler(new PrecisionLocationDetailHandler());
        registerHandler(new RemarksDetailHandler());
        registerHandler(new SensorDetailHandler());
        registerHandler(new StatusDetailHandler());
        registerHandler(new StrokeFillDetailHandler());
        registerHandler(new TrackDetailHandler());
        registerHandler(new UserIconDetailHandler());
        registerHandler(new VehicleModelDetailHandler());
        registerHandler(new VideoDetailHandler());
    }
}
