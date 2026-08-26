package gov.takx.core.view.gui.maps.terraindiscovery.view;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.ITerrainDiscoveryController;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.ui.UIResources;

import javax.swing.JPanel;

import java.awt.BorderLayout;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 */
public class TerrainDiscoveryDialog extends AStandardDialog
{
    private final ITerrainDiscoveryController controller;
    private final TerrainDiscoveryPanel terrainDiscoveryPanel;
    private final TerrainDiscoveryPositionListener positionListener;

    public TerrainDiscoveryDialog(ITerrainDiscoveryController controller,
                                  TerrainDiscoveryPanel terrainDiscoveryPanel,
                                  TerrainDiscoveryPositionListener positionListener)
    {
        setIconImage(UIResources.getImage(PLATFORM_ICONS, "terrain.png")); // TODO KMB: either set the dialog icon consistently or not at all
        this.controller = controller;
        this.terrainDiscoveryPanel = terrainDiscoveryPanel;
        this.positionListener = positionListener;
        initComponents();
        terrainDiscoveryPanel.setDialog(this);
        contentPane.add(terrainDiscoveryPanel, new CellConstraints(1, 1, CellConstraints.FILL, CellConstraints.FILL));
        setVisible(true);
    }

    @Override
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);
        positionListener.update(visible);
        terrainDiscoveryPanel.getGeofencePanel().dialogVisible(visible);
        if (!visible) controller.clearTemp();
    }

    @Override
    protected void onWindowClosing()
    {
        positionListener.update(false);
        terrainDiscoveryPanel.getGeofencePanel().dialogVisible(false);
        controller.clearTemp();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        contentPane = new JPanel();

        //======== this ========
        setTitle("Terrain Discovery");
        setName("this");
        var contentPane2 = getContentPane();
        contentPane2.setLayout(new BorderLayout());

        //======== contentPane ========
        {
            contentPane.setBorder(Borders.DIALOG_BORDER);
            contentPane.setName("contentPane");
            contentPane.setLayout(new FormLayout(
                    "default:grow",
                    "fill:default:grow"));
        }
        contentPane2.add(contentPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel contentPane;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
