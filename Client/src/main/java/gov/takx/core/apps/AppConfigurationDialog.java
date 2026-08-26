package gov.takx.core.apps;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.apps.IAppInstance;
import gov.takx.api.plugin.app.IAppPluginManager.AutoStartMode;
import gov.takx.api.plugin.device.IPluginConfigurationPanel;
import gov.takx.core.mapobject.gui.APluginConfigurationDialog;
import gov.takx.core.uibridge.SwingUIBinder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.JCheckBox;

import java.util.List;

/**
 * Dialog to hold app configuration panels.
 *
 * @since 1.0 (renamed from ConfigurationDialog in 5.0)
 */
@SuppressWarnings({"CdiManagedBeanInconsistencyInspection"})
@PseudoBean
public class AppConfigurationDialog extends APluginConfigurationDialog
{
    @Inject
    private SwingUIBinder swingUIBinder;

    private final IAppInstance app;
    private final JCheckBox autoStartCheckBox;

    /**
     * Create a dialog for an app.
     *
     * @param app App to configure.
     */
    public AppConfigurationDialog(@Nonnull IAppInstance app)
    {
        super(app.getName() + " " + app.getVersion());
        this.app = app;
        autoStartCheckBox = new JCheckBox("Automatically start this app");
        autoStartCheckBox.addActionListener(e -> panelStateListener.setPanelDirty());
    }

    @PostConstruct
    private void postConstruct()
    {
        final List<IPluginConfigurationPanel> configurationPanels = swingUIBinder.forApp(app.getApp()).getConfigurationPanels();

        // Check for null to protect against plugins not respecting the nullability annotation
        if (configurationPanels != null && !configurationPanels.isEmpty()) init(configurationPanels);

        autoStartCheckBox.setSelected(app.isAutoStart());
        setCustomComponent(autoStartCheckBox);
    }

    @Override
    public void onApply()
    {
        super.onApply();
        app.setAutoStartMode(autoStartCheckBox.isSelected() ? AutoStartMode.start : AutoStartMode.none);
    }
}
