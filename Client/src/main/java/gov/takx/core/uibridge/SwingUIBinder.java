package gov.takx.core.uibridge;

import gov.takx.api.connection.IComposedConnectionController;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.plugin.annotations.ImportExportSwingUIProvider;
import gov.takx.api.plugin.app.IAppPlugin;
import gov.takx.api.plugin.connection.IConnectionPluginController;
import gov.takx.api.plugin.io.IImportExportWizard;
import gov.takx.api.ui.IAppSwingUIProvider;
import gov.takx.api.ui.IConnectionSwingUIProvider;
import gov.takx.api.ui.IDeviceSwingUIProvider;
import gov.takx.api.ui.IImportExportSwingUIProvider;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.module.DeploymentAttachments;
import gov.takx.module.IPluginModuleManager;
import gov.takx.platform.attachments.AttachmentKey;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.weld.bean.proxy.ProxyObject;

import javax.swing.JOptionPane;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Binder for accessing UI components in Swing-land.  This class offers methods to locate the UI providers in a plugin deployment.
 * <p/>
 * The original need for this level of abstraction for plugin UIs was due to RaptorWeb, which is now defunct.  However,
 * removing this mechanism would break nearly every plugin in existence, so we're retaining it.
 *
 * @since 2.0.0
 */
@ApplicationScoped
public class SwingUIBinder
{
    private final IPluginModuleManager pluginModuleManager;
    private final Map<ConnectionUiKey, IConnectionSwingUIProvider> connectionProviderCache = new ConcurrentHashMap<>();

    @Inject
    SwingUIBinder(IPluginModuleManager pluginModuleManager)
    {
        this.pluginModuleManager = pluginModuleManager;
    }

    /**
     * Obtain the Swing UI provider for the given App plugin.
     *
     * @param plugin App plugin of interest
     * @return Swing UI provider
     */
    @Nonnull
    public IAppSwingUIProvider forApp(IAppPlugin plugin)
    {
        // If the plugin implements the provider interface, then just return it.
        // Otherwise, lookup a binding and use that to produce the provider.
        return plugin instanceof IAppSwingUIProvider appSwingUIProvider
                ? appSwingUIProvider
                : getUIBinding(DeploymentAttachments.APP_SWING_UI_BINDINGS, getPluginClass(plugin))
                        .map(uiBinding -> uiBinding.constructUi(plugin))
                        .orElse(AppUiBinding.EMPTY_APP_SWING_UI_PROVIDER); // Offer up a stub to keep everything else working
    }

    /**
     * Obtain the Swing UI provider for the given map object controller.
     *
     * @param mapObjectController Map object controller for the device of interest
     * @return Swing UI provider
     */
    @Nonnull
    public IDeviceSwingUIProvider forDevice(IMapObjectController mapObjectController)
    {
        var pluginController = mapObjectController.getPluginController();

        // If the controller implements the provider interface, then just return it.
        // Otherwise, lookup a binding and use that to produce the provider.
        return pluginController instanceof IDeviceSwingUIProvider deviceSwingUIProvider
                ? deviceSwingUIProvider
                : getUIBinding(DeploymentAttachments.DEVICE_SWING_UI_BINDINGS, pluginController.getClass())
                        .map(uiBinding -> uiBinding.constructUi(mapObjectController))
                        .orElse(DeviceUiBinding.EMPTY_DEVICE_SWING_UI_PROVIDER); // Offer up a stub to keep everything else working
    }

    /**
     * Obtain the Swing UI provider for the given connection plugin controller.  Handling of connection UI providers
     * is unique in that we maintain a cache of created UI providers, and we never create a provider for a given
     * connection controller more than once.
     *
     * @param connectionController Composed controller for the connection of interest
     * @return Swing UI provider
     */
    @Nonnull
    public IConnectionSwingUIProvider forConnection(IComposedConnectionController connectionController)
    {
        var pluginController = connectionController.getPluginConnectionController();

        // If the controller implements the provider interface, then just return it.
        if (pluginController instanceof IConnectionSwingUIProvider connectionSwingUIProvider) return connectionSwingUIProvider;

        // If we have a binding, use that to create the provider, otherwise return a stub
        return getUIBinding(DeploymentAttachments.CONNECTION_SWING_UI_BINDINGS, pluginController.getClass())
                .map(uiBinding -> {
                    final var key = new ConnectionUiKey(uiBinding.getUiProviderClass(), pluginController.getUniqueKey());
                    return connectionProviderCache.computeIfAbsent(key, k -> uiBinding.constructUi(connectionController));
                })
                .orElse(ConnectionUiBinding.EMPTY_CONNECTION_SWING_UI_PROVIDER);
    }

    /**
     * Obtain the Swing UI provider for the given Import Export Wizard.
     *
     * @param wizard Wizard of interest
     * @return Swing UI Provider
     * @see ImportExportSwingUIProvider
     * @since 2.13
     */
    @Nonnull
    public IImportExportSwingUIProvider forWizard(IImportExportWizard wizard)
    {
        return getUIBinding(DeploymentAttachments.IMPORT_EXPORT_SWING_UI_BINDINGS, wizard.getClass())
                .map(uiBinding -> uiBinding.constructUi(wizard))
                .orElse(WizardUiBinding.EMPTY_IMPORT_EXPORT_SWING_UI_PROVIDER);
    }

    /**
     * Determines if a UI provider is present for the specified connection controller class.
     *
     * @param controllerClass The connection controller class to check for a related UI provider
     * @return True if a UI provider is present
     * @since 2.18
     */
    public boolean isUIProviderPresent(Class<? extends IConnectionPluginController> controllerClass)
    {
        return IConnectionSwingUIProvider.class.isAssignableFrom(controllerClass)
                || getUIBinding(DeploymentAttachments.CONNECTION_SWING_UI_BINDINGS, controllerClass).isPresent();
    }

    /**
     * Determines if a UI provider is present for the specified import/export wizard.
     *
     * @param wizard The wizard to check for a related UI provider
     * @return True if a UI provider is present
     * @since 2.18
     */
    public boolean isUIProviderPresent(IImportExportWizard wizard)
    {
        return getUIBinding(DeploymentAttachments.IMPORT_EXPORT_SWING_UI_BINDINGS, wizard.getClass()).isPresent();
    }

    /**
     * Show a dialog that there is no Swing UI provided.
     * <p>
     * Since there isn't a good way to know ahead of creating the connection controller or I/O wizard
     * if a Swing UI has been created for a given plugin, this is how we will notify them of the issue.
     *
     * @param message The message fragment to include, should include an identifying name and the plugin type
     * @since 2.18
     */
    public void showNoSwingUIDialog(String message)
    {
        AlwaysOnTopJOptionPane.showMessageDialog(
                "The plugin did not provide a way for users to create/configure " + message + ".",
                "No User Interface Provided",
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Get the UI binding (if present) for the given source class.
     *
     * @param attachmentKey Deployment attachment key for the UI bindings desired
     * @param sourceClass   The class of the source (usually a plugin controller) needing a UI
     * @param <T>           UI Binding type
     * @return Optional UI Binding
     */
    private <T> Optional<T> getUIBinding(AttachmentKey<Map<Class<?>, T>> attachmentKey, Class<?> sourceClass)
    {
        return pluginModuleManager.findDeploymentForClassLoader(sourceClass.getClassLoader())
                .map(pluginDeployment -> pluginDeployment.getAttachment(attachmentKey).get(sourceClass));
    }

    /**
     * Get the class of a plugin instance. Handle the case where the plugin is a Weld proxy.
     *
     * @param plugin Plugin of interest
     * @return actual plugin class (de-proxied as needed)
     */
    protected Class<? extends IRaptorPlugin> getPluginClass(IRaptorPlugin plugin)
    {
        final var pluginClass = plugin.getClass();

        return ProxyObject.class.isAssignableFrom(pluginClass)
                ? pluginClass.getSuperclass().asSubclass(IRaptorPlugin.class) // The proxy is a subclass of the actual class
                : pluginClass;
    }

    /**
     * Clear our connection UI provider cache on project closing.
     * @since 5.1
     */
    private void onProjectClosing(@Observes ProjectClosing unused)
    {
        connectionProviderCache.clear();
    }

    private record ConnectionUiKey(Class<? extends IConnectionSwingUIProvider> providerClass, String uniqueKey)
    {
    }
}
