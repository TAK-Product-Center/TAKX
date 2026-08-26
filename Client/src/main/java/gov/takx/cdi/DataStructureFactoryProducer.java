package gov.takx.cdi;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.plugin.annotations.DataStructureFactory;
import gov.takx.api.plugin.device.IDataStructureFactory;
import gov.takx.api.plugin.device.IDevicePlugin;
import gov.takx.module.DeploymentAttachments;
import gov.takx.module.IPluginModuleManager;
import gov.takx.module.PluginDeployment;
import gov.takx.platform.utils.InjectionUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

/**
 * This class offers a producer method that can inject instances of {@link IDataStructureFactory}
 * when qualified with {@link DataStructureFactory}.
 *
 * @since 2.0.0
 */
@InjectableService
@ApplicationScoped
public class DataStructureFactoryProducer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IPluginModuleManager pluginModuleManager;

    /**
     * Produce the data structure factory for the device plugin specified in the annotation.
     *
     * @param injectionPoint Injection point so we can obtain the annotation values
     * @return Appropriate DSF
     */
    @Produces
    @DataStructureFactory(forPlugin = IDevicePlugin.class)
    private IDataStructureFactory produceDSF(InjectionPoint injectionPoint)
    {
        // Get the annotation on the injection point
        final DataStructureFactory pref =
                InjectionUtils.getAnnotation(injectionPoint, DataStructureFactory.class)
                        .orElseThrow(() -> new IllegalStateException("No @DataStructureFactory on: " + injectionPoint));

        final Class<? extends IDevicePlugin> forPlugin = pref.forPlugin();

        // Now we need to locate the instance of that class in the bean manager for the deployment.  So, we need to
        // get the deployment.

        final Optional<PluginDeployment> optionalDeployment = pluginModuleManager.findDeploymentForPlugin(forPlugin);

        if (optionalDeployment.isPresent())
        {
            final PluginDeployment deployment = optionalDeployment.get();
            final WeldContainer container = deployment.getAttachment(DeploymentAttachments.CONTAINER);

            final IDevicePlugin devicePlugin = container.select(forPlugin).get();
            if (devicePlugin != null)
            {
                return devicePlugin.getDataStructureFactory();
            } else
            {
                logger.error("Failed to locate plugin instance of [{}] for injection point: {}", forPlugin, injectionPoint);
            }
        } else
        {
            logger.error("Unable to locate deployment for class [{}] on injection point: {}", forPlugin.getName(), injectionPoint);
        }

        return null;
    }
}
