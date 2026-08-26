package gov.takx.core.view.gui.menus;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.PluginAdded;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.device.IDevicePluginDefinition;
import gov.takx.api.registry.IDevicePluginRegistry;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.toolbar.TabType;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.Comparator;

/**
 */
@PseudoBean
public class DevicesToolbarTabWrapper extends AToolbarTabWrapper
{
    @Inject
    private IDevicePluginRegistry devicePluginRegistry;

    @PostConstruct
    private void postConstruct()
    {
        createTab(TabType.DEVICES, false); // Hidden by default
    }

    @Override
    protected void addComponents()
    {
        devicePluginRegistry.getPluginDefinitions().stream()
                .sorted(Comparator.comparing(IPluginDefinition::getName))
                .forEach(this::addButton);
    }

    /**
     * Handle hot deployed plugins. If it is a device plugin, add a button for it.
     * @since 5.7
     */
    private void onPluginAdded(@Observes PluginAdded pluginAdded)
    {
        if (pluginAdded.pluginDefinition() instanceof IDevicePluginDefinition devicePluginDefinition) addButton(devicePluginDefinition);
    }

    /**
     * Add a button based on the given plugin {@code definition}, if it supports manual creation.
     * @since 5.7
     */
    private void addButton(IDevicePluginDefinition definition)
    {
        if (!definition.isManualCreationAllowed()) return;

        // Make the button available to be added to toolbar configurations. Since the DEVICES tab includes this
        // button type as a dynamic button, it will also be added to any DEVICES tab instances automatically.
        final String name = definition.getName();
        configurationManager.addReferenceButton(createButton(ButtonType.DEVICE_PLUGIN, name,
                definition.getIcon(), e -> mapObjectManager.createDevice(definition, getCenterLocationOnGlobe()),
                "Add a " + name + " device to the globe."));
    }
}
