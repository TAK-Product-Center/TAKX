package gov.takx.module;

import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.plugin.annotations.PluginDependency;
import gov.takx.api.plugin.connection.PreconfiguredConnection;
import gov.takx.bootstrap.Verification;
import gov.takx.core.uibridge.AppUiBinding;
import gov.takx.core.uibridge.ConnectionUiBinding;
import gov.takx.core.uibridge.DeviceUiBinding;
import gov.takx.core.uibridge.WizardUiBinding;
import gov.takx.platform.attachments.AttachmentKey;
import gov.takx.platform.attachments.AttachmentList;
import gov.takx.platform.attachments.AttachmentSet;
import org.jboss.jandex.Index;
import org.jboss.modules.ResourceLoader;
import org.jboss.vfs.TempFileProvider;
import org.jboss.weld.environment.se.WeldContainer;

import java.io.File;
import java.util.Map;
import java.util.jar.JarFile;

/**
 * Attachments added to plugin deployments as they are processed.
 *
 * @since 2.0.0
 */
public class DeploymentAttachments
{
    /**
     * The ID to use when generating the module definition.
     */
    public static final AttachmentKey<String> MODULE_ID = AttachmentKey.create(String.class);

    /**
     * The module builder handling this deployment.
     */
    public static final AttachmentKey<IDeploymentModuleBuilder> MODULE_BUILDER = AttachmentKey.create(IDeploymentModuleBuilder.class);

    /**
     * The module loader to use for creating plugin modules.
     */
    public static final AttachmentKey<PluginModuleLoader> PLUGIN_MODULE_LOADER = AttachmentKey.create(PluginModuleLoader.class);

    /**
     * The Index for the jar contents.
     */
    public static final AttachmentKey<Index> INDEX = AttachmentKey.create(Index.class);

    /**
     * The JarFile open on the jar being processed.
     */
    public static final AttachmentKey<JarFile> JAR_FILE = AttachmentKey.create(JarFile.class);

    /**
     * The list of discovered plugins found in the deployment.
     */
    public static final AttachmentKey<AttachmentList<DiscoveredPlugin>> DISCOVERED_PLUGINS = AttachmentKey.createList(DiscoveredPlugin.class);

    /**
     * The list of discovered plugins found in the deployment.
     */
    public static final AttachmentKey<AttachmentList<IPluginDefinition<? extends IRaptorPlugin>>> PLUGIN_DEFINITIONS = AttachmentKey.createList(IPluginDefinition.class);

    /**
     * The list of the target class of discovered "preconfigured connections" annotations found in the deployment.
     *
     * @since 2.20
     */
    public static final AttachmentKey<AttachmentList<String>> PRECONFIGURED_CONNECTION_TARGETS = AttachmentKey.createList(String.class);

    /**
     * The list of "preconfigured connections" found in the deployment.
     *
     * @since 2.20
     */
    public static final AttachmentKey<AttachmentList<PreconfiguredConnection>> PRECONFIGURED_CONNECTIONS = AttachmentKey.createList(PreconfiguredConnection.class);

    /**
     * The set of module Ids of plugin dependencies found in this deployment.
     */
    public static final AttachmentKey<AttachmentSet<String>> PLUGIN_DEPENDENCIES = AttachmentKey.createSet(String.class);

    /**
     * The set of plugin deployments of plugin dependencies found in this deployment (corollary to PLUGIN_DEPENDENCIES).
     * @since 3.4
     */
    public static final AttachmentKey<AttachmentSet<PluginDeployment>> PLUGIN_DEPENDENCY_DEPLOYMENTS = AttachmentKey.createSet(PluginDeployment.class);

    /**
     * The set of plugin deployments that directly depend on this deployment, using {@link PluginDependency}.
     *
     * @since 2.35
     */
    public static final AttachmentKey<AttachmentSet<PluginDeployment>> PLUGIN_DEPENDENTS = AttachmentKey.createSet(PluginDeployment.class);

    /**
     * Indicates if the deployment is actually deployable (all validations passed).
     */
    public static final AttachmentKey<Boolean> DEPLOYABLE = AttachmentKey.create(Boolean.class);

    /**
     * Indicates the jar verification status of the deployment.
     */
    public static final AttachmentKey<Verification> VERIFICATION = AttachmentKey.create(Verification.class);

    /**
     * Indicates if the deployment's jar contains nested resources (e.g., nested lib directory)
     */
    public static final AttachmentKey<Boolean> HAS_NESTED_RESOURCES = AttachmentKey.create(Boolean.class);

    /**
     * The ID to use when generating the "lib" module definition.  Only valid if JAR_OF_JARS is true.
     */
    public static final AttachmentKey<String> LIB_MODULE_ID = AttachmentKey.create(String.class);

    /**
     * The list of additional ResourceLoader's to use to access nested resources.  Only valid if JAR_OF_JARS is true. Optional.
     */
    public static final AttachmentKey<AttachmentList<ResourceLoader>> RESOURCE_LOADERS = AttachmentKey.createList(ResourceLoader.class);

    /**
     * The list of Resource Roots to use to access nested resources.  Only valid if JAR_OF_JARS is true. Optional.
     */
    public static final AttachmentKey<AttachmentList<File>> RESOURCE_ROOTS = AttachmentKey.createList(File.class);

    /**
     * List of jar files located within the lib directory in a "jar of jars" deployment.
     */
    public static final AttachmentKey<AttachmentList<JarFile>> SUB_JARS = AttachmentKey.createList(JarFile.class);

    /**
     * List of jar files located within the lib directory that should be processed by our Aspects.
     * This essentially removes them from the libs sub-module and adds them directly to the deployment's module.
     *
     * @since 2.18
     */
    public static final AttachmentKey<AttachmentSet<JarFile>> SUB_JARS_NEEDING_ASPECTS = AttachmentKey.createSet(JarFile.class);

    /**
     * The temp file provider to use for working with the Jboss VFS.  Not used during testing.
     */
    public static final AttachmentKey<TempFileProvider> TEMP_FILE_PROVIDER = AttachmentKey.create(TempFileProvider.class);

    /**
     * The class loader to use to instantiate classes and retrieve resources from the deployment.
     */
    public static final AttachmentKey<ClassLoader> CLASS_LOADER = AttachmentKey.create(ClassLoader.class);

    /**
     * The CDI container for a plugin deployment.
     */
    public static final AttachmentKey<WeldContainer> CONTAINER = AttachmentKey.create(WeldContainer.class);

    /**
     * Indicates whether this deployment has had cross container event delivery configured.
     * @since 5.7
     */
    public static final AttachmentKey<Boolean> CROSS_CONTAINER_EVENTS_CONFIGURED = AttachmentKey.create(Boolean.class);

    /**
     * The list of plugin deployments that hold implementations for types defined in this deployment.
     *
     * @since 2.25
     */
    public static final AttachmentKey<AttachmentSet<PluginDeployment>> IMPLEMENTATION_DEPLOYMENTS = AttachmentKey.createSet(PluginDeployment.class);

    /**
     * The list of plugin deployments for which this deployment is an implementation (via @ImplementedBy annotations).
     *
     * @since 2.25
     */
    public static final AttachmentKey<AttachmentSet<PluginDeployment>> IMPLEMENTATION_FOR = AttachmentKey.createSet(PluginDeployment.class);

    /**
     * The map of App Swing UI bindings.
     * <p>
     * The key is the plugin class and the value is the class that implements the Swing UI for that plugin.
     */
    public static final AttachmentKey<Map<Class<?>, AppUiBinding>> APP_SWING_UI_BINDINGS = AttachmentKey.create(Map.class);

    /**
     * The map of Device Swing UI bindings.
     * <p>
     * The key is the plugin controller class and the value is the binding that generates the Swing UI for that plugin controller.
     */
    public static final AttachmentKey<Map<Class<?>, DeviceUiBinding>> DEVICE_SWING_UI_BINDINGS = AttachmentKey.create(Map.class);

    /**
     * The map of Connection Swing UI bindings.
     * <p>
     * The key is the plugin controller class and the value is the class that implements the Swing UI for that plugin controller.
     */
    public static final AttachmentKey<Map<Class<?>, ConnectionUiBinding>> CONNECTION_SWING_UI_BINDINGS = AttachmentKey.create(Map.class);

    /**
     * The map of Import/Export Swing UI bindings.
     * <p>
     * The key is the wizard class and the value is the class that implements the Swing UI for that wizard.
     *
     * @since 2.13
     */
    public static final AttachmentKey<Map<Class<?>, WizardUiBinding>> IMPORT_EXPORT_SWING_UI_BINDINGS = AttachmentKey.create(Map.class);
}
