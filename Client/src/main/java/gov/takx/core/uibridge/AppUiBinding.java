package gov.takx.core.uibridge;

import gov.takx.api.plugin.app.IAppPlugin;
import gov.takx.api.ui.IAppSwingUIProvider;
import gov.takx.platform.cdi.NoopInvocationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 * Binding for an App UI provider.
 * @see gov.takx.api.plugin.annotations.AppSwingUIProvider
 * @since 5.1
 */
public class AppUiBinding extends AUiBinding<IAppSwingUIProvider>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static final IAppSwingUIProvider EMPTY_APP_SWING_UI_PROVIDER = NoopInvocationHandler.forType(IAppSwingUIProvider.class);

    AppUiBinding(Class<? extends IAppSwingUIProvider> uiProviderClass)
    {
        super(uiProviderClass);
    }

    /**
     * Construct the UI provider for the given plugin.
     */
    IAppSwingUIProvider constructUi(IAppPlugin plugin)
    {
        try
        {
            return constructUi(uiProviderClass, Map.of(IAppPlugin.class, plugin));
        } catch (Exception e)
        {
            logger.error("Failed to construct Device UI provider of type: {}", uiProviderClass, e);
        }

        return EMPTY_APP_SWING_UI_PROVIDER;
    }
}
