package gov.takx.core.view.gui.menus;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.toolbar.TabType;
import gov.takx.core.toolbar.ToolbarSeparator;
import gov.takx.core.view.gui.CoreComponentType;
import gov.takx.core.view.gui.plugin.PluginManagerDialog;
import jakarta.annotation.PostConstruct;

/**
 */
@PseudoBean
public class AppsToolbarTabWrapper extends AToolbarTabWrapper
{
    private static final String APPS_TOOLTIP = "View and manage the apps that you currently have installed. Add and remove apps.";
    private static final String PLUGIN_MANAGER_TOOLTIP = "Manage the plugins installed in the application and view and edit associated properties.";

    @PostConstruct
    private void postConstruct()
    {
        createTab(TabType.PLUGINS, true);
    }

    @Override
    protected void addComponents()
    {
        tab.addComponent(createTogglePanelButton(CoreComponentType.APPS_PANEL, APPS_TOOLTIP));
        tab.addComponent(new ToolbarSeparator(TabType.PLUGINS, tab.getComponents().size()));

        // The following button will be available for custom configs but not added to the default toolbar configuration
        configurationManager.addReferenceButton(createButton(ButtonType.PLUGIN_MANAGER, e -> PluginManagerDialog.showInfo(), PLUGIN_MANAGER_TOOLTIP));
    }
}
