package gov.takx.module;

import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Contract for plugin module manager, responsible for processing and deploying plugins.
 *
 */
public interface IPluginModuleManager
{
    /**
     * Find all the plugin jar files and process them as plugin deployments.
     */
    void loadAllPlugins();

    /**
     * Load and process all the plugins in the given jar files.
     *
     * @param jarFiles Jar files to process
     * @since 5.7
     */
    void loadPluginJars(List<File> jarFiles);

    /**
     * Create a minimal plugin deployment for the given plugin definition.  The deployment will only contain sufficient
     * information to allow for the creation of an instance of the plugin (as needed by the
     * {@code PluginDefinitionFactory}.
     *
     * @param pluginDefinition Plugin definition upon which to create a deployment
     * @since 2.18
     */
    void createTestDeployment(@Nonnull IPluginDefinition<?> pluginDefinition);

    /**
     * Search all plugins for a resource with the given name.  Return the URL of the first match.
     *
     * @param resourceName Name (path) of resource
     * @return URL of matching resource (it will be a jar: resource), {@code null} if not found
     */
    @Nullable
    URL getPluginResource(@Nonnull String resourceName);

    /**
     * Given the class loader for a plugin, locate the plugin context and return the mount point for the
     * jar containing that plugin.
     * <p>
     * Note that this is only relevant for plugin jar files that contain a nested {@code "lib"} or {@code "native"}
     * directory.  For all others, this will return {@code null}.
     *
     * @param pluginClassLoader Class loader of plugin of interest
     * @return mount point, {@code null} if not mounted or if the plugin can't be located by the class loader
     */
    @Nullable
    File getPluginJarMountPoint(@Nonnull ClassLoader pluginClassLoader);

    /**
     * Using the current threads context class loader (assuming it is a associated with a plugin), get the
     * mount point for the plugin's jar file.
     *
     * @return Plugin jar mount point, if found
     * @see #getPluginJarMountPoint(ClassLoader)
     */
    @Nullable
    File getPluginJarMountPoint();

    /**
     * Find the deployment that contains a plugin with the given plugin class.
     *
     * @param pluginClass Class of plugin to search for
     * @return Optional {@link PluginDeployment} containing the given plugin class.
     */
    @Nonnull
    Optional<PluginDeployment> findDeploymentForPlugin(@Nonnull Class<? extends IRaptorPlugin> pluginClass);

    /**
     * Find the deployment that uses the given class loader.
     *
     * @param classLoader Class loader to search for
     * @return Optional {@link PluginDeployment}
     */
    @Nonnull
    Optional<PluginDeployment> findDeploymentForClassLoader(@Nonnull ClassLoader classLoader);

    /**
     * @return All the plugin deployments that are deployable
     */
    @Nonnull
    List<PluginDeployment> getAllDeployableDeployments();

    /**
     * @return All the plugin known deployments, deployable or not
     */
    @Nonnull
    List<PluginDeployment> getAllKnownDeployments();

    /**
     * Get the list of deployments for use in scanning for service implementations.  At normal runtime, this is
     * just the list of deployable deployments.
     * <p>
     * Sadly, our current test infrastructure can't quite duplicate execution with test plugins because they
     * don't live in a deployable jar.  So, here we need to alter our logic to pull in ALL deployments, not
     * just the "deployable" ones, since test plugins are not deployable.
     * <p>
     * And, worse, since we only have a single CDI container during tests, if we process more than one deployment,
     * we'll get the same beans multiple times.  So, only process the first under test.
     * <p>
     * We seriously need to investigate something like Shrink Wrap so we can generate actual jar files from
     * arbitrary classes, and then we could perform the full deployment processing on them.
     *
     * @return All deployments to use for service scanning, may be empty
     * @see #getAllDeployableDeployments()
     * @see #getAllKnownDeployments()
     * @since 3.0
     */
    @Nonnull
    List<PluginDeployment> getDeploymentsForServiceScanning();

    /**
     * Locate all of the beans that implement the given service type, {@code <S>}, and provide them to the given
     * {@code handler} for processing.
     *
     * @param type    Service type to scan for
     * @param handler Handler to process each discovered bean of the given type
     * @param <S>     Inferred service type
     * @see #getDeploymentsForServiceScanning()
     * @since 3.0
     */
    <S> void processDeploymentServices(@Nonnull Class<S> type, @Nonnull BiConsumer<PluginDeployment, S> handler);

    /**
     * Scan the known deployments for those that match the given module id using a name prefix match.
     * This supports specifying a module id without caring about the version.  For example
     * "plugin.foo" will match any deployment who's module id starts with "plugin.foo"
     * (say "plugin.foo-1.2.0" for example). It is an error to match 0 or more than 1 plugin deployment.
     *
     * @param moduleId            Module Id to search for
     * @param referringDeployment The deployment trying to locate a match
     * @param dependencyType      The type of dependency requiring the match
     * @return Matching deployment, or {@code null} if there is not exactly one match
     * @since 2.25
     */
    @Nullable
    PluginDeployment resolveDeploymentByPartialNameMatch(@Nonnull String moduleId, @Nonnull PluginDeployment referringDeployment, @Nonnull String dependencyType);
}
