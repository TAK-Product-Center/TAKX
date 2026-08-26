package gov.takx.api.plugin.device;

import gov.takx.commons.swing.IPanelStateListener;
import jakarta.annotation.Nonnull;

import javax.swing.JPanel;

import java.net.URL;

/**
 * Base class for {@link IPluginConfigurationPanel}s. Most {@link IPluginConfigurationPanel} implementations should
 * extend this class and override its methods as necessary.
 *
 * @since 1.0 (renamed from IDevicePluginPreferencesPanel in 5.0 and refactored)
 */
public abstract class APluginConfigurationPanel extends JPanel implements IPluginConfigurationPanel
{
    protected IPanelStateListener listener;
    protected String title;

    protected APluginConfigurationPanel(@Nonnull String title)
    {
        this.title = title;
    }

    @Override
    public void setPanelListener(IPanelStateListener listener)
    {
        this.listener = listener;
    }

    @Override
    public JPanel getPanel()
    {
        return this;
    }

    @Override
    public String validateContents()
    {
        return null;
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    @Override
    public URL getIcon()
    {
        return null;
    }

    @Override
    public void applyAction()
    {
        // Default to empty
    }

    /**
     * Convenience method so plugins don't need to reference the listener.
     */
    protected void setPanelDirty()
    {
        if (listener != null) listener.setPanelDirty();
    }
}
