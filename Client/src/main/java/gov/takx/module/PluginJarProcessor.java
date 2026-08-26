package gov.takx.module;

import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.annotations.DefinePreconfiguredConnection;
import gov.takx.api.plugin.connection.PreconfiguredConnection;
import gov.takx.commons.utils.PlatformVersion;
import gov.takx.core.registry.PluginRegistryManager;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Modifier;

/**
 * This class handles the processing of plugin definitions located by the {@link PluginJarScanner} to
 * get those plugins internalized and registered.
 *
 * @since 1.43
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class PluginJarProcessor
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final PluginDeployment deployment;
    private final File jarFile;
    private final ClassLoader classLoader;

    @Inject
    @Headless
    private boolean headless;

    @Inject
    private PlatformVersion platformVersion;

    @Inject
    private PluginRegistryManager pluginRegistryManager;

    @Inject
    private PluginDefinitionFactory pluginDefinitionFactory;

    /**
     * Construct a plugin jar processor.
     *
     * @param deployment The plugin deployment to process
     */
    protected PluginJarProcessor(PluginDeployment deployment)
    {
        this.deployment = deployment;

        classLoader = deployment.getAttachment(DeploymentAttachments.CLASS_LOADER);
        jarFile = deployment.getJarPhysicalFile();
    }

    /**
     * Process all the plugins discovered by the jar scanner.
     *
     * @throws ClassNotFoundException If the plugin class is not found
     */
    public void processPlugins() throws ClassNotFoundException
    {
        // Can't use a stream here because processPlugin throws exceptions

        for (DiscoveredPlugin plugin : deployment.getAttachmentList(DeploymentAttachments.DISCOVERED_PLUGINS))
        {
            processPlugin(plugin);
        }

        // Now intern all the preconfigured connection declarations discovered
        for (String targetClassName : deployment.getAttachmentList(DeploymentAttachments.PRECONFIGURED_CONNECTION_TARGETS))
        {
            processPreconfiguredConnectionTarget(targetClassName);
        }
    }

    /**
     * Given a target class name, load the class and obtain all the uses of the {@code DefinePreconfiguredConnection}
     * annotation.  For each of those, construct an instance of {@code PreconfiguredConnection} from the annotation
     * and add it to the list attached to this deployment.
     *
     * @param targetClassName Name of class on which to process annotations
     * @throws ClassNotFoundException If the target class can't be loaded
     * @see DefinePreconfiguredConnection
     * @see PreconfiguredConnection
     * @since 2.20
     */
    private void processPreconfiguredConnectionTarget(String targetClassName) throws ClassNotFoundException
    {
        final Class<?> pluginClass = classLoader.loadClass(targetClassName);
        final DefinePreconfiguredConnection[] annotations = pluginClass.getAnnotationsByType(DefinePreconfiguredConnection.class);

        for (DefinePreconfiguredConnection annotation : annotations)
        {
            final PreconfiguredConnection preconfiguredConnection = PreconfiguredConnection.fromAnnotation(annotation, classLoader);

            if (logger.isDebugEnabled()) logger.debug("Created: {}", preconfiguredConnection);

            deployment.addToAttachmentList(DeploymentAttachments.PRECONFIGURED_CONNECTIONS, preconfiguredConnection);
        }
    }

    /**
     * Process a single discovered plugin within this jar file.
     *
     * @param discoveredPlugin Plugin to process
     * @throws ClassNotFoundException If the class specified in the manifest is not found
     */
    private void processPlugin(DiscoveredPlugin discoveredPlugin) throws ClassNotFoundException
    {
        final PluginCategory pluginCategory = discoveredPlugin.pluginCategory();
        final String pluginClassName = discoveredPlugin.pluginClassName();

        if (logger.isDebugEnabled())
        {
            logger.debug("Processing {} {} from jar {}", pluginCategory, pluginClassName, jarFile.getName());
        }

        // Try to make a plugin definition from the discovered information
        final IPluginDefinition<?> pluginDefinition = makePluginDefinition(discoveredPlugin);

        // If we managed to get a valid plugin instance, ensure that it can run in this platform and register it if so

        if (null != pluginDefinition && testMinimumSupportedVersion(pluginDefinition))
        {
            if (logger.isDebugEnabled())
            {
                final Class<? extends IRaptorPlugin> pluginClass = pluginDefinition.getPluginClass();
                logger.debug("Register {} plugin [{}] from [{}] cl={}", pluginCategory,
                        pluginClass, jarFile, pluginClass.getClassLoader());
            }

            // Register the definition.  Only add this definition to the deployment if it is actually registered.
            if (pluginRegistryManager.registerPlugin(pluginDefinition))
            {
                deployment.addToAttachmentList(DeploymentAttachments.PLUGIN_DEFINITIONS, pluginDefinition);
            }
        }
    }

    /**
     * Construct the plugin definition from a discovered plugin.  Verify the class details:
     * <ol>
     * <li>Is the class concrete</li>
     * <li>The class has a no-arg constructor</li>
     * <li>The class is assignable to the required base type</li>
     * </ol>
     *
     * @param discoveredPlugin Plugin to process
     * @return Constructed plugin definition
     * @throws ClassNotFoundException If the class specified in the manifest is not found
     * @since 2.25
     */
    private IPluginDefinition<?> makePluginDefinition(DiscoveredPlugin discoveredPlugin) throws ClassNotFoundException
    {
        final PluginCategory pluginCategory = discoveredPlugin.pluginCategory();
        final String pluginClassName = discoveredPlugin.pluginClassName();

        IPluginDefinition<?> pluginDefinition = null;

        try
        {
            final Class<?> pluginClass = classLoader.loadClass(pluginClassName);

            // Test the plugin constructor and type
            if (!testConcrete(pluginClass)) return null;
            if (!testAssignability(pluginCategory, pluginClass)) return null;

            warnIfPseudoBean(pluginClass);

            // Create the PluginDefinition
            final Class<? extends IRaptorPlugin> subclass = pluginClass.asSubclass(IRaptorPlugin.class);

            pluginDefinition = pluginDefinitionFactory.definitionFor(pluginCategory, subclass, jarFile);

            final String type = pluginDefinition.getType();
            final String family = pluginDefinition.getFamily();

            if (family.contains("/") || type.contains("/") || family.contains("\\") || type.contains("\\"))
            {
                throw new DeploymentException(String.format("Plugin family and type must not contain '/' or '\\' characters:" +
                        " plugin [%s] with family='%s' and type='%s'", pluginClass.getName(), family, type));
            }
        } catch (LinkageError e)
        {
            final Throwable cause = e.getCause();

            if (cause != null)
            {
                logger.error("""
                        Failed to load [{}]
                           from [{}]
                           due to missing class [{}]
                           error: {}""",
                        pluginClassName, jarFile, cause.getMessage(), e.getMessage());
            } else
            {
                logger.error("Failed to load {} from {}", pluginClassName, jarFile, e);
            }
        }

        return pluginDefinition;
    }

    /**
     * Emit an error message if ths plugin class is annotated with @PseudoBean, which is a known "bad" thing.
     * It can lead to strange injection problems at runtime (like being injected twice, which can lead to multiple
     * invocations of @PostConstruct methods).
     *
     * @param pluginClass Plugin class to inspect
     * @since 2.23
     */
    private void warnIfPseudoBean(Class<?> pluginClass)
    {
        if (pluginClass.getAnnotation(PseudoBean.class) != null)
        {
            logger.error("""
                    Plugin class [{}] is annotated with @PseudoBean
                       from [{}]
                       This can cause subtle injection problems at runtime.  Please remove the @PseudoBean annotation.""",
                    pluginClass.getName(), jarFile);
        }
    }

    /**
     * Verify that the given class is concrete (not abstract).
     *
     * @param pluginClass Plugin class to inspect
     * @return {@code true} if the class is concrete
     * @since 2.22
     */
    private boolean testConcrete(Class<?> pluginClass)
    {
        if (Modifier.isAbstract(pluginClass.getModifiers()))
        {
            logger.error("""
                    Unable to load plugin class [{}]
                       from [{}]
                       because it is not a concrete class (it is abstract).""",
                    pluginClass.getName(), jarFile);
            return false;
        }
        return true;
    }

    /**
     * Ensure that a plugin implements the required interfaces to work properly within TAKX.
     *
     * @param pluginCategory The category of plugin being processed
     * @param pluginClass    The plugin class (from the jar file)
     * @return {@code true} if all good, {@code false} otherwise
     */
    protected boolean testAssignability(PluginCategory pluginCategory, Class<?> pluginClass)
    {
        if (!pluginCategory.getRequiredInterface().isAssignableFrom(pluginClass))
        {
            logger.error("""
                    Unable to load plugin class [{}] of type [{}]
                       from [{}]
                       because it does not implement the required interface [{}].""",
                    pluginClass, pluginCategory, jarFile.getName(), pluginCategory.getRequiredInterface());
            return false;
        }

        return true;
    }

    /**
     * Determine if the plugin definition is consistent with the current platform version.
     *
     * @param pluginDefinition Plugin definition to test
     * @return {@code true} if we meet the required minimum version
     */
    private boolean testMinimumSupportedVersion(IPluginDefinition<?> pluginDefinition)
    {
        final PlatformVersion minRequired = pluginDefinition.getMinimumSupportedVersion();
        boolean supported = minRequired != null && platformVersion.compareTo(minRequired) >= 0;

        if (logger.isDebugEnabled())
        {
            logger.debug("Plugin {} can {}run on current platform: requires {}", pluginDefinition.getUniqueKey(), supported ? "" : "NOT ", minRequired);
        }

        if (!supported)
        {
            logger.error("{} Plugin {} was not registered.\nThe plugin requires a newer version of the platform: {} > {}", pluginDefinition.getPluginCategory(), pluginDefinition.getUniqueKey(), minRequired, platformVersion);
        }

        return supported;
    }
}
