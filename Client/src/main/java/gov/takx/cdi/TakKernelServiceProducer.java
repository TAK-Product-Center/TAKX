package gov.takx.cdi;

import gov.tak.api.chat.IChatService;
import gov.tak.api.contact.IContactService;
import gov.tak.api.vehicle.VehicleModelAssets;
import gov.tak.api.vehicle.VehicleModelCache;
import gov.tak.platform.chat.ChatService;
import gov.tak.platform.contact.Contacts;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.event.ShuttingDown;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;

/**
 * Produces takkernel services for injection.
 *
 * @since 3.4
 */
@InjectableService
@ApplicationScoped
public class TakKernelServiceProducer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IProgramEnvironment programEnvironment;
    private VehicleModelCache vehicleModelCache;

    @Inject
    private TakKernelServiceProducer(IProgramEnvironment programEnvironment)
    {
        this.programEnvironment = programEnvironment;
    }

    private void shutdown(@Observes ShuttingDown event)
    {
        if (vehicleModelCache != null) vehicleModelCache.dispose();
    }

    @SuppressWarnings("unused")
    @Produces
    private IChatService produceChatService()
    {
        return ChatService.getInstance();
    }

    @SuppressWarnings("unused")
    @Produces
    private IContactService produceContactStore()
    {
        return Contacts.getDefaultContactStore();
    }

    @Produces
    @Singleton
    private VehicleModelCache produceVehicleModelCache()
    {
        File offlineCacheModelsDir = new File(programEnvironment.getProjectDataDirectory(), "OfflineCache/Vehicles");
        VehicleModelAssets vehicleModelAssets = new VehicleModelAssets(offlineCacheModelsDir, TakKernelServiceProducer::getAssetStream);
        vehicleModelCache = new VehicleModelCache(vehicleModelAssets);
        return vehicleModelCache;
    }

    /**
     * Get an {@link InputStream} for the provided vehicle model asset path.
     * @param assetPath the asset path to the vehicle model
     * @return an {@link InputStream}, or null if the asset can not be found
     * @since 5.8
     */
    @Nullable
    private static InputStream getAssetStream(@Nonnull String assetPath)
    {
        URL assetUrl = UIResources.getUrl(ResourceDirectory.TAK_ASSETS_VEHICLE_MODELS, assetPath);
        try
        {
            return assetUrl.openStream();
        } catch (IOException e)
        {
            logger.error("Error loading asset file: {}", assetUrl, e);
        }
        return null;
    }
}
