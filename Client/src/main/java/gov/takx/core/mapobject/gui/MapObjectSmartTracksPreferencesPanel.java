package gov.takx.core.mapobject.gui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.commons.swing.IPanelStateListener;
import gov.takx.core.view.gui.preferences.SmartTracksPreferencesPanel;
import gov.takx.platform.ui.UIResources;

import java.net.URL;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 */
public class MapObjectSmartTracksPreferencesPanel extends MapObjectMainPreferencesPanel
{
    public static final String TITLE = "Tracks";

    public MapObjectSmartTracksPreferencesPanel(IMapObjectController mapObjectController, IPanelStateListener panelListener)
    {
        super(TITLE, mapObjectController, panelListener);

        initComponents();
        smartTracksPreferencesPanel.setMapObjectController(mapObjectController);
        smartTracksPreferencesPanel.initializeFields();
        smartTracksPreferencesPanel.setPanelListener(panelListener);
    }

    @Override
    public URL getIcon()
    {
        return UIResources.getUrl(PLATFORM_ICONS, "tracks.png");
    }

    @Override
    protected void applyActionPerformed()
    {
        smartTracksPreferencesPanel.applyAction();
    }

    @Override
    public String validateContents()
    {
        return null;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        smartTracksPreferencesPanel = new SmartTracksPreferencesPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow"));

        //---- smartTracksPreferencesPanel ----
        smartTracksPreferencesPanel.setName("smartTracksPreferencesPanel");
        add(smartTracksPreferencesPanel, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private SmartTracksPreferencesPanel smartTracksPreferencesPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
