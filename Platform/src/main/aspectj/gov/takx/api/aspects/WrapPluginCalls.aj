package gov.takx.api.aspects;

import gov.takx.platform.utils.ClassLoaderUtil;
import org.aspectj.lang.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This aspect is responsible for wrapping every call out to a method implemented by a plugin.
 * The following operations are performed:
 * <ul>
 *     <li>log call/return of plugin method</li>
 *     <li>install the proper thread context class loader (for the plugin)</li>
 *     <li>report all exceptions</li>
 * </ul>
 *
 * @since 1.36
 */
public aspect WrapPluginCalls
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Set<String> mutedMethods = new HashSet<>(Arrays.asList("hashCode", "getFamily", "getType",
            "getVersion", "getName"));

    public pointcut IRPCall(Object callee): execution(public * gov.takx.api.plugin.IRaptorPlugin.*(..)) && this(callee);

    public pointcut appPluginCall(Object callee): execution(public * gov.takx.api.plugin.app.IAppPlugin.*(..)) && this(callee);

    public pointcut commserviceCall(Object callee): execution(public * gov.takx.api.commservices.ICommService.*(..)) && this(callee);

    public pointcut connectionCall(Object callee): execution(public * gov.takx.api.plugin.connection.IConnectionPlugin.*(..)) && this(callee);
    public pointcut connectionPluginCall(Object callee): execution(public * gov.takx.api.plugin.connection.IConnection.*(..)) && this(callee);
    public pointcut connectionPluginControllerCall(Object callee): execution(public * gov.takx.api.plugin.connection.IConnectionPluginController.*(..)) && this(callee);
    public pointcut connectionPanelCall(Object callee): execution(public * gov.takx.api.plugin.connection.IConnectionPanel.*(..)) && this(callee);

    public pointcut devicePluginCall(Object callee): execution(public * gov.takx.api.plugin.device.IDevicePlugin.*(..)) && this(callee);
    public pointcut devicePluginControllerCall(Object callee): execution(public * gov.takx.api.plugin.device.IDevicePluginController.*(..)) && this(callee);
    public pointcut devicePrefPanel(Object callee): execution(public * gov.takx.api.plugin.device.IDevicePluginPreferencesPanel.*(..)) && this(callee);
    public pointcut deviceConfigPanel(Object callee): execution(public * gov.takx.api.plugin.device.IDevicePluginConfigurationPanel.*(..)) && this(callee);
    public pointcut dataStructureFactoryCall(Object callee): execution(public * gov.takx.api.plugin.device.IDataStructureFactory.*(..)) && this(callee);

    public pointcut gwControllerCall(Object callee): execution(public * gov.takx.api.plugin.gateway.IGatewayController.*(..)) && this(callee);
    public pointcut gwFramer(Object callee): execution(public * gov.takx.api.plugin.gateway.IGatewayFramer.*(..)) && this(callee);
    public pointcut gwParser(Object callee): execution(public * gov.takx.api.plugin.gateway.IGatewayParser.*(..)) && this(callee);

    public pointcut ioPluginCall(Object callee): execution(public * gov.takx.api.plugin.io.IImportExportPlugin.*(..)) && this(callee);
    public pointcut importWizardCall(Object callee): execution(public * gov.takx.api.plugin.io.IImportWizard.*(..)) && this(callee);
    public pointcut ioWizardCall(Object callee): execution(public * gov.takx.api.plugin.io.IImportExportWizard.*(..)) && this(callee);

    public pointcut markerCall(Object callee): execution(public * gov.takx.api.playback.IMarker.*(..)) && this(callee);
    public pointcut globeComponentListenerCall(Object callee): execution(public * gov.takx.api.globe.IGlobeComponentListener.*(..)) && this(callee);

    Object around(Object callee): IRPCall(callee)
            || appPluginCall(callee)
            || commserviceCall(callee)
            || connectionCall(callee) || connectionPluginCall(callee) || connectionPluginControllerCall(callee) || connectionPanelCall(callee)
            || devicePluginCall(callee) || devicePluginControllerCall(callee) || devicePrefPanel(callee) || deviceConfigPanel(callee) || dataStructureFactoryCall(callee)
            || gwControllerCall(callee) || gwFramer(callee) || gwParser(callee)
            || ioPluginCall(callee) || importWizardCall(callee) || ioWizardCall(callee)
            || markerCall(callee) || globeComponentListenerCall(callee) {

        // There is some odd logic here to keep overhead to the absolute minimum.  So, most info needed
        // to perform tracing is not initialized if "enabled" is false.

        final boolean enabled = logger.isDebugEnabled();

        // Suppress logging of static returning methods that we call a lot unless we're tracing.
        final Signature signature = enabled ? thisJoinPointStaticPart.getSignature() : null;

        final boolean show = enabled && (logger.isTraceEnabled() || !mutedMethods.contains(signature.getName()));
        final String classAndMethod = show ? signature.getDeclaringType().getName() + "." + signature.getName() : "";

        if (show)
        {
            logger.trace("BEGIN_PLUGIN_CALL: {} -> {}", AspectUtils.findCallerOfExecutionJoinPoint(), classAndMethod);
        }

        // Install the context class loader for the plugin class being executed
        ClassLoaderUtil.pushContextClassLoader(callee.getClass().getClassLoader());

        try
        {
            Object returnValue = proceed(callee);
            if (show) logger.trace("RETURN_PLUGIN_CALL: {}", classAndMethod);
            return returnValue;
        } catch (RuntimeException e)
        {
            logger.error("PLUGIN_EXCEPTION: {} -> {}: {}",
                    AspectUtils.findCallerOfExecutionJoinPoint(), callee.getClass(), thisJoinPointStaticPart.getSignature(), e);
            throw e;
        } finally
        {
            ClassLoaderUtil.popContextClassLoader(); // Always pop it
        }
    }
}
