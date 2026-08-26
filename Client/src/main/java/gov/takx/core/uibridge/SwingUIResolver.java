package gov.takx.core.uibridge;

import gov.takx.api.plugin.annotations.AppSwingUIProvider;
import gov.takx.api.plugin.annotations.ConnectionSwingUIProvider;
import gov.takx.api.plugin.annotations.DeviceSwingUIProvider;
import gov.takx.api.plugin.annotations.ImportExportSwingUIProvider;
import gov.takx.api.ui.IAppSwingUIProvider;
import gov.takx.api.ui.IConnectionSwingUIProvider;
import gov.takx.api.ui.IDeviceSwingUIProvider;
import gov.takx.api.ui.IImportExportSwingUIProvider;
import gov.takx.module.DeploymentAttachments;
import gov.takx.module.PluginDeployment;
import gov.takx.platform.attachments.AttachmentKey;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * This class resolves the Swing UI bindings for a plugin and adds those bindings to the deployment.
 *
 * @see DeploymentAttachments#APP_SWING_UI_BINDINGS
 * @see DeploymentAttachments#CONNECTION_SWING_UI_BINDINGS
 * @see DeploymentAttachments#DEVICE_SWING_UI_BINDINGS
 * @see DeploymentAttachments#IMPORT_EXPORT_SWING_UI_BINDINGS
 * @since 2.0.0
 */
public class SwingUIResolver
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final DotName APP_SWING_UI_PROVIDER_TYPE = DotName.createSimple(AppSwingUIProvider.class.getName());
    private static final DotName CONNECTION_SWING_UI_PROVIDER_TYPE = DotName.createSimple(ConnectionSwingUIProvider.class.getName());
    private static final DotName DEVICE_SWING_UI_PROVIDER_TYPE = DotName.createSimple(DeviceSwingUIProvider.class.getName());
    private static final DotName IMPORT_EXPORT_SWING_UI_PROVIDER_TYPE = DotName.createSimple(ImportExportSwingUIProvider.class.getName());

    private final PluginDeployment deployment;

    /**
     * @param deployment Deployment to process for UI bindings
     */
    public SwingUIResolver(PluginDeployment deployment)
    {
        this.deployment = deployment;
    }

    /**
     * Resolve all the UI binding types and populate the attachments on the deployment.
     */
    public void resolveUIs()
    {
        this.<IAppSwingUIProvider, AppUiBinding>resolveUiBindings(DeploymentAttachments.APP_SWING_UI_BINDINGS,
                IAppSwingUIProvider.class, APP_SWING_UI_PROVIDER_TYPE, "forPlugin", AppUiBinding::new);

        this.<IDeviceSwingUIProvider, DeviceUiBinding>resolveUiBindings(DeploymentAttachments.DEVICE_SWING_UI_BINDINGS,
                IDeviceSwingUIProvider.class, DEVICE_SWING_UI_PROVIDER_TYPE, "forPluginController", DeviceUiBinding::new);

        this.<IConnectionSwingUIProvider, ConnectionUiBinding>resolveUiBindings(DeploymentAttachments.CONNECTION_SWING_UI_BINDINGS,
                IConnectionSwingUIProvider.class, CONNECTION_SWING_UI_PROVIDER_TYPE, "forPluginController", ConnectionUiBinding::new);

        this.<IImportExportSwingUIProvider, WizardUiBinding>resolveUiBindings(DeploymentAttachments.IMPORT_EXPORT_SWING_UI_BINDINGS,
                IImportExportSwingUIProvider.class, IMPORT_EXPORT_SWING_UI_PROVIDER_TYPE, "forWizard", WizardUiBinding::new);
    }

    /**
     * Resolve the UI binding declarations for a given type.  The deployment's Jandex index is used to locate the
     * annotated provider classes.  The discovered providers will be added to the given attachment key.
     *
     * @param attachmentKey The attachment key to use for storing the map of discovered UI providers
     * @param providerClass The type of the UI provider
     * @param providerAnnotationName The name of the annotation used to link provider to plugin
     * @param annotationFieldName The name of the field within the annotation that specifies the target plugin class
     * @param bindingGenerator Function to generate the binding from the provider UI class
     * @param <P> UI Provider type
     * @param <B> Binding type
     * @since 5.1
     */
    private <P, B extends AUiBinding<P>> void resolveUiBindings(AttachmentKey<Map<Class<?>, B>> attachmentKey, Class<P> providerClass,
                                                                DotName providerAnnotationName, String annotationFieldName,
                                                                Function<Class<? extends P>, B> bindingGenerator)
    {
        final Map<Class<?>, B> bindings = new HashMap<>();
        deployment.putAttachment(attachmentKey, bindings);

        final Index index = deployment.getAttachment(DeploymentAttachments.INDEX);
        final ClassLoader classLoader = deployment.getAttachment(DeploymentAttachments.CLASS_LOADER);

        for (AnnotationInstance annotationInstance : index.getAnnotations(providerAnnotationName))
        {
            final String providerClassName = annotationInstance.target().toString();
            final String bindingTargetClassName = annotationInstance.value(annotationFieldName).asString();

            try
            {
                final var uiClass = classLoader.loadClass(providerClassName).asSubclass(providerClass);
                final var bindingTargetClass = classLoader.loadClass(bindingTargetClassName);

                logger.debug("uiClass={} for: {}", uiClass, bindingTargetClass);

                final var previousBinding = bindings.put(bindingTargetClass, bindingGenerator.apply(uiClass));

                if (previousBinding != null) logDuplicateProviderWarning(bindingTargetClass, previousBinding.getUiProviderClass(), uiClass);
            } catch (ClassNotFoundException e)
            {
                logger.error("Annotation [{}] on class [{}] specifies a class [{}] that couldn't be found",
                        providerClass.getSimpleName(), providerClassName, bindingTargetClassName);
            }
        }
    }

    /**
     * Logs a warning for a UI provider that is registered for a plugin class that already has a UI provider.
     *
     * @param targetClass           The plugin class that the UI provider is targeting
     * @param existingProviderClass The class of the existing UI provider binding
     * @param newProviderClass      The class of the new UI provider class
     */
    private void logDuplicateProviderWarning(Class<?> targetClass, Class<?> existingProviderClass, Class<?> newProviderClass)
    {
        logger.warn("""
                Redefinition of Swing UI provider for {}:
                   had   {}   <== Using this one
                   found {}   <== Ignoring this one""",
                targetClass, existingProviderClass, newProviderClass);
    }
}
