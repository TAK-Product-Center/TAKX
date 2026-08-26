package gov.takx.core.view.gui.maptools.elevationtools;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.core.view.gui.maptools.elevationtools.viewshed.ViewshedPanel;

import javax.swing.JTabbedPane;

/**
 * A container panel for various elevation tools
 * @since 5.7
 */
public class ElevationToolsPanel extends ATitledPanel
{
    public ElevationToolsPanel()
    {
        super(ButtonType.ELEVATION_TOOLS.getDisplayText());
        initComponents();
    }

    @Override
    public void releaseResources()
    {
        viewshedPanel.releaseResources();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        tabPane = new JTabbedPane();
        viewshedPanel = new ViewshedPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setMinimumSize(null);
        setPreferredSize(null);
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow"));

        //======== tabPane ========
        {
            tabPane.setMinimumSize(null);
            tabPane.setPreferredSize(null);
            tabPane.setName("tabPane");

            //---- viewshedPanel ----
            viewshedPanel.setPreferredSize(null);
            viewshedPanel.setMinimumSize(null);
            viewshedPanel.setName("viewshedPanel");
            tabPane.addTab("Viewshed", viewshedPanel);
        }
        add(tabPane, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JTabbedPane tabPane;
    private ViewshedPanel viewshedPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
