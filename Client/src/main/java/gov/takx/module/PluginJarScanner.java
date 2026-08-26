package gov.takx.module;

import com.google.common.collect.ImmutableMap;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.annotations.AppPlugin;
import gov.takx.api.plugin.annotations.ConnectionPlugin;
import gov.takx.api.plugin.annotations.DefinePreconfiguredConnection;
import gov.takx.api.plugin.annotations.DefinePreconfiguredConnections;
import gov.takx.api.plugin.annotations.DevicePlugin;
import gov.takx.api.plugin.annotations.ImportExportPlugin;
import gov.takx.commons.utils.StringUtils;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

/**
 * Responsible for scanning a jar file for plugin definitions based on the annotations found.
 * <p>
 * Once constructed, call {@link #scan()} to perform the scan.  The plugins found will be added to the deployment
 * using {@link DeploymentAttachments#DISCOVERED_PLUGINS}.
 *
 * @since 1.43
 */
public class PluginJarScanner
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Annotations used to declare plugins
    private static final DotName APP_PLUGIN_TYPE = DotName.createSimple(AppPlugin.class.getName());
    private static final DotName DEVICE_PLUGIN_TYPE = DotName.createSimple(DevicePlugin.class.getName());
    private static final DotName CONNECTION_PLUGIN_TYPE = DotName.createSimple(ConnectionPlugin.class.getName());
    private static final DotName IMPORT_EXPORT_PLUGIN_TYPE = DotName.createSimple(ImportExportPlugin.class.getName());

    private static final DotName DEFINE_PRECONFIGURED_CONNECTION = DotName.createSimple(DefinePreconfiguredConnection.class.getName());
    private static final DotName DEFINE_PRECONFIGURED_CONNECTIONS = DotName.createSimple(DefinePreconfiguredConnections.class.getName());

    // Map from plugin annotation type name to plugin category
    private static final Map<DotName, PluginCategory> type2categoryMap = ImmutableMap.<DotName, PluginCategory>builder()
            .put(APP_PLUGIN_TYPE, PluginCategory.AppPlugin)
            .put(DEVICE_PLUGIN_TYPE, PluginCategory.DevicePlugin)
            .put(CONNECTION_PLUGIN_TYPE, PluginCategory.ConnectionPlugin)
            .put(IMPORT_EXPORT_PLUGIN_TYPE, PluginCategory.ImportExportPlugin)
            .build();

    private final PluginDeployment deployment;

    /**
     * Construct a jar file scanner on the given deployment.
     *
     * @param deployment The plugin deployment to process
     */
    public PluginJarScanner(PluginDeployment deployment)
    {
        this.deployment = deployment;
    }

    /**
     * Scan the jar file and extract all the plugin definitions.
     *
     * @since 1.43
     */
    public void scan()
    {
        findAnnotatedPlugins(); // Find all the ones declared using annotations
        findPreconfiguredConnections(); // Find all the preconfigured connection definitions
    }

    /**
     * Find any plugins declared using annotations.
     */
    private void findAnnotatedPlugins()
    {
        final Index index = deployment.getAttachment(DeploymentAttachments.INDEX);

        for (DotName pluginAnnotation : type2categoryMap.keySet())
        {
            // Get the list of instances (uses) of this annotation from this jar's index
            final List<AnnotationInstance> instances = index.getAnnotations(pluginAnnotation);

            if (!instances.isEmpty())
            {
                processPluginAnnotation(pluginAnnotation, instances);
            }
        }

        checkForForbiddenPackages(index);
    }

    /**
     * Determine if there are any classes within one of our forbidden packages.  We disallow
     * classes in certain packages to avoid the common mistake of copying our API classes into
     * a plugin jar (as part of an uber-jar), as this can lead to load failures since we now
     * have multiple definitions of the same class in 2 class loaders.
     *
     * @since 3.0
     */
    private void checkForForbiddenPackages(Index index)
    {
        final List<String> forbiddenPackages = ForbiddenPackages.getForbiddenPackages();

        boolean containsForbiddenPackages = index.getKnownClasses().stream()
                .anyMatch(ci -> StringUtils.startsWithAny(ci.toString(), forbiddenPackages));

        if (containsForbiddenPackages)
        {
            logger.error("""
                    Plugin jar [{}] contains a class in a forbidden package!
                    This generally indicates that you included class files from the TAKX distribution in your plugin's jar file.
                    These classes will be ignored, but you should fix your build to avoid including them in the plugin jar file.""",
                    deployment.getName());
        }
    }

    /**
     * Create plugin definitions for each class that is annotation with the given annotation.
     *
     * @param pluginAnnotation Annotation being processed
     * @param instances        List of all instances (uses) of that annotation
     */
    private void processPluginAnnotation(DotName pluginAnnotation, List<AnnotationInstance> instances)
    {
        final PluginCategory pluginCategory = type2categoryMap.get(pluginAnnotation);

        // Process each instance to add the plugin definition for that class

        for (AnnotationInstance instance : instances)
        {
            final String className = instance.target().toString();

            logger.debug("{} is a {}", className, pluginCategory);

            deployment.addToAttachmentList(DeploymentAttachments.DISCOVERED_PLUGINS, new DiscoveredPlugin(pluginCategory, className));
        }
    }

    /**
     * Find all the preconfigured connection definitions within the deployment.  Record the target class names
     * for each so we can process them later once we have a class loader in place for the deployment.
     *
     * @since 2.20
     */
    private void findPreconfiguredConnections()
    {
        final Index index = deployment.getAttachment(DeploymentAttachments.INDEX);

        // Note that we can't use IndexUtils.getAnnotationInstances here since, if there are repeated annotations
        // on a single class, the returned nested annotation instances no longer have target information (for some
        // stupid reason within Jandex).  So, process each annotation type so we can properly access the target.

        index.getAnnotations(DEFINE_PRECONFIGURED_CONNECTION).forEach(this::addPreconfiguredConnection);
        index.getAnnotations(DEFINE_PRECONFIGURED_CONNECTIONS).forEach(this::addPreconfiguredConnection);
    }

    private void addPreconfiguredConnection(AnnotationInstance annotation)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Add PC from {}: {}, target={}", deployment.getName(), annotation, annotation.target());
        }

        deployment.addToAttachmentList(DeploymentAttachments.PRECONFIGURED_CONNECTION_TARGETS, annotation.target().toString());
    }
}
