package gov.takx.core.uibridge;

import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.plugin.device.IDevicePluginController;
import gov.takx.api.plugin.device.IDevicePluginDelegate;
import gov.takx.api.ui.IDeviceSwingUIProvider;
import gov.takx.platform.cdi.NoopInvocationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 * Binding for a Device UI provider.
 * @see gov.takx.api.plugin.annotations.DeviceSwingUIProvider
 * @since 5.1
 */
public class DeviceUiBinding extends AUiBinding<IDeviceSwingUIProvider>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static final IDeviceSwingUIProvider EMPTY_DEVICE_SWING_UI_PROVIDER = NoopInvocationHandler.forType(IDeviceSwingUIProvider.class);

    DeviceUiBinding(Class<? extends IDeviceSwingUIProvider> uiProviderClass)
    {
        super(uiProviderClass);
    }

    /**
     * Construct the UI provider for the given MapObject controller.
     */
    IDeviceSwingUIProvider constructUi(IMapObjectController mapObjectController)
    {
        final IDevicePluginController pluginController = mapObjectController.getPluginController();

        try
        {
            return constructUi(uiProviderClass,
                    Map.of(String.class, pluginController.getUnitId(),
                            IDevicePluginController.class, pluginController,
                            IDevicePluginDelegate.class, mapObjectController,
                            IMapObjectController.class, mapObjectController,
                            IMapObject.class, mapObjectController.getMapObject()));
        } catch (Exception e)
        {
            logger.error("Failed to construct Device UI provider of type: {}", uiProviderClass, e);
        }

        return EMPTY_DEVICE_SWING_UI_PROVIDER;
    }
}
