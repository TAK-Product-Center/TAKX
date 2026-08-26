package gov.takx.core.view.gui.favorites;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.core.view.gui.DialogHeaderPanel;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.JButton;
import javax.swing.JPanel;

import java.awt.BorderLayout;

/**
 */
public class ManageFavoritesDialog extends AStandardDialog
{
    public ManageFavoritesDialog()
    {
        initComponents();
        dialogHeaderPanel.setDescription("<html>Add and remove favorites by selecting a favorite and using the Add" +
                " and Delete buttons. Edit a favorite to change the name or to change a user favorite to a project " +
                "favorite and vice versa. Select a favorite and click Go To to zoom to that location." +
                "<br><br>Favorites may be reordered in this dialog by dragging and dropping them to a different " +
                "position within their list.</html>");
        dialogHeaderPanel.setIcon(ButtonType.MANAGE_FAVORITES.getImageUrl());
    }

    private void closeButtonActionPerformed()
    {
        dispose();
    }

    /**
     * Handles disposing of the contained panel as part of the window being disposed.
     * @since 3.3
     */
    @Override
    protected void onWindowClosing()
    {
        favoritesPanel.dispose();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        dialogHeaderPanel = new DialogHeaderPanel();
        favoritesPanel = new FavoritesPanel();
        var buttonBar = new JPanel();
        var closeButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Manage Favorites");
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //---- dialogHeaderPanel ----
            dialogHeaderPanel.setName("dialogHeaderPanel");
            dialogPane.add(dialogHeaderPanel, BorderLayout.NORTH);

            //---- favoritesPanel ----
            favoritesPanel.setName("favoritesPanel");
            dialogPane.add(favoritesPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "pref"));

                //---- closeButton ----
                closeButton.setText("Close");
                closeButton.setName("closeButton");
                closeButton.addActionListener(e -> closeButtonActionPerformed());
                buttonBar.add(closeButton, cc.xy(4, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private DialogHeaderPanel dialogHeaderPanel;
    private FavoritesPanel favoritesPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
