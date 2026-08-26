package gov.takx.core.messagequery;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.cdi.InstanceManager;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.plugin.device.IDevicePluginController;
import gov.takx.api.query.IDeviceQueryService;
import gov.takx.api.query.IExtendedQueryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Producer of query service instances.  Instances of {@link IExtendedQueryService} and {@link IDeviceQueryService}
 * can be produced.
 *
 * @since 2.0.0
 */
@SuppressWarnings("unused")
@InjectableService
@ApplicationScoped
public class QueryServiceProducer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private InstanceManager instanceManager;

    @Inject
    private IMapObjectManager mapObjectManager;

    /**
     * @return Extended query service instance
     */
    @Produces
    public IExtendedQueryService produceExtendedQueryService(InjectionPoint injectionPoint)
    {
        if (logger.isDebugEnabled()) logger.debug("produce EQS for: {}", injectionPoint);

        return new ExtendedQueryService();
    }

    /**
     * Get the query service scoped to the Map Object for the injection target.  Since we need to be able to
     * ask for a Map Object's unique key, we only allow this injection to occur within an instance of
     * {@link IDevicePluginController}.
     *
     * @return Device-specific query service instance
     * @throws DeploymentException If the injection point is on something other than a device plugin controller
     */
    @Produces
    public IDeviceQueryService produceDeviceQueryService(InjectionPoint injectionPoint)
    {
        final Object instance = instanceManager.getCurrentInjectionInstance();

        if (logger.isDebugEnabled()) logger.debug("produce DQS for: {}", instance);

        // See if the instance being injected can give us the Map Object key we need.

        if (instance instanceof IDevicePluginController)
        {
            IDevicePluginController controller = (IDevicePluginController) instance;
            final String key = controller.getUniqueKey();

            if (logger.isDebugEnabled()) logger.debug("Get query service for key: {}", key);

            final IMapObjectController moc = mapObjectManager.getMapObjectController(key);
            if (moc != null)
            {
                return moc.getQueryService();
            } else
            {
                logger.error("Failed to locate Map Object with key: {} to inject {}", key, injectionPoint);
            }
        } else
        {
            throw new DeploymentException("@Inject IDeviceQueryService was used somewhere other than within a DevicePluginController: " + injectionPoint);
        }

        return null;
    }
}
