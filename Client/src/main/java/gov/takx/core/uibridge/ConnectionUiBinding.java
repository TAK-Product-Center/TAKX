package gov.takx.core.uibridge;

import gov.takx.api.connection.IComposedConnectionController;
import gov.takx.api.plugin.connection.IConnectionPluginController;
import gov.takx.api.plugin.connection.IConnectionPluginDelegate;
import gov.takx.api.ui.IConnectionSwingUIProvider;
import gov.takx.platform.cdi.NoopInvocationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 * Binding for a Connection UI provider.
 * @see gov.takx.api.plugin.annotations.ConnectionSwingUIProvider
 * @since 5.1
 */
public class ConnectionUiBinding extends AUiBinding<IConnectionSwingUIProvider>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static final IConnectionSwingUIProvider EMPTY_CONNECTION_SWING_UI_PROVIDER = NoopInvocationHandler.forType(IConnectionSwingUIProvider.class);

    ConnectionUiBinding(Class<? extends IConnectionSwingUIProvider> uiProviderClass)
    {
        super(uiProviderClass);
    }

    /**
     * Construct the UI provider for the given connection controller.
     */
    IConnectionSwingUIProvider constructUi(IComposedConnectionController connectionController)
    {
        IConnectionPluginController pluginController = connectionController.getPluginConnectionController();

        try
        {
            return constructUi(uiProviderClass,
                    Map.of(IConnectionPluginController.class, pluginController,
                            IConnectionPluginDelegate.class, connectionController));
        } catch (Exception e)
        {
            logger.error("Failed to construct Connection UI provider of type: {}", uiProviderClass, e);
        }

        return EMPTY_CONNECTION_SWING_UI_PROVIDER;
    }
}
