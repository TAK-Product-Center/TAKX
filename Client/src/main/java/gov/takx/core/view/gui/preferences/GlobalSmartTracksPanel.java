package gov.takx.core.view.gui.preferences;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.ui.AProgramPreferencesPanel;
import gov.takx.commons.swing.IPanelStateListener;

/**
 */
public class GlobalSmartTracksPanel extends AProgramPreferencesPanel
{
    public GlobalSmartTracksPanel()
    {
        initComponents();
        smartTracksPreferencesPanel.setGlobalMode();
        smartTracksPreferencesPanel.initializeFields();
    }

    @Override
    public void setPanelListener(IPanelStateListener panelListener)
    {
        super.setPanelListener(panelListener);
        smartTracksPreferencesPanel.setPanelListener(panelListener);
    }

    @Override
    public void setPanelDirty()
    {
        super.setPanelDirty();
        smartTracksPreferencesPanel.setPanelDirty();
    }

    @Override
    public boolean applyAction()
    {
        return smartTracksPreferencesPanel.applyAction();
    }

    @Override
    public void restoreDefaults()
    {
        smartTracksPreferencesPanel.restoreDefaults();
        // smartTracksPreferencesPanel.applyAction();
    }

    @Override
    public void releaseResources()
    {
        smartTracksPreferencesPanel.releaseResources();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        smartTracksPreferencesPanel = new SmartTracksPreferencesPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("panelGlobalSmartTracks");
        setLayout(new FormLayout(
                "default:grow",
                "default"));

        //---- smartTracksPreferencesPanel ----
        smartTracksPreferencesPanel.setName("smartTracksPreferencesPanel");
        add(smartTracksPreferencesPanel, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private SmartTracksPreferencesPanel smartTracksPreferencesPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
