package gov.takx.core.view.gui.takserver;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import jakarta.annotation.Nonnull;

import javax.swing.JPanel;

/**
 * Holds {@link DataFeedInfoPanel}s for display as a docked panel.
 *
 * @since 5.0
 */
public class DataFeedInfoPanelHolder extends JPanel
{
    private static final CellConstraints CELL_CONSTRAINTS = new CellConstraints(1, 1);
    private DataFeedInfoPanel panel;

    public DataFeedInfoPanelHolder()
    {
        initComponents();
    }

    /**
     * @param panel The panel to display
     */
    public void setPanel(@Nonnull DataFeedInfoPanel panel)
    {
        this.panel = panel;
        removeAll();
        add(panel, CELL_CONSTRAINTS);

        revalidate();
        repaint();
    }

    /**
     * @param panel The panel to remove if it is currently being displayed
     * @return {@code true} if the panel was being displayed and was removed, {@code false} otherwise
     */
    public boolean removeIfActive(@Nonnull DataFeedInfoPanel panel)
    {
        if (this.panel == panel)
        {
            this.panel = null;
            removeAll();
            return true;
        }

        return false;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off

        //======== this ========
        setLayout(new FormLayout(
                "[250dlu,default]:grow",
                "fill:default:grow"));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
