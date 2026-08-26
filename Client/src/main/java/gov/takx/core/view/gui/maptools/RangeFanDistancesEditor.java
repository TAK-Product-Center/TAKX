package gov.takx.core.view.gui.maptools;

import com.formdev.flatlaf.FlatClientProperties;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.ui.UIResources;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Window;
import java.util.List;

/**
 * Dialog to allow the user to edit the discrete range ring distances and associated labels.
 *
 * @since 1.49
 */
public class RangeFanDistancesEditor extends AStandardDialog
{
    private boolean canceled = true;
    private CustomDistancesTable tblDistances;

    private final Border errorBorder = new LineBorder(Color.RED, 1);
    private final Border warningBorder = new LineBorder(Color.YELLOW, 1);

    /**
     * Create an editor parented by a dialog.
     *
     * @param owner     Parent dialog
     * @param distances List of distances
     * @param labels    List of labels
     */
    public RangeFanDistancesEditor(Window owner, List<Double> distances, List<String> labels)
    {
        super(owner);
        init(distances, labels);
    }

    /**
     * Initialize the editor with the given data.
     *
     * @param distances List of distances
     * @param labels    List of labels
     */
    private void init(List<Double> distances, List<String> labels)
    {
        initComponents();

        // Make the buttons appear flat. This is achieved in FlatLAF by making them borderless
        btnAddRow.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        btnRemoveRow.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);

        // Allow buttons to shrink
        btnAddRow.putClientProperty(FlatClientProperties.MINIMUM_WIDTH, 0);
        btnRemoveRow.putClientProperty(FlatClientProperties.MINIMUM_WIDTH, 0);

        btnAddRow.setIcon(UIResources.ADD_ICON);
        btnRemoveRow.setIcon(UIResources.REMOVE_ICON);

        tblDistances = new CustomDistancesTable(distances, labels);
        scrollDistancesTable.setViewportView(tblDistances);

        tblDistances.getModel().addTableModelListener(e -> validateModel());

        chkUseCustomLabels.setSelected(labels != null);
        chkUseCustomLabelsActionPerformed();

        tblDistances.setRowSelectionAllowed(true);
        tblDistances.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        invalidate();
        pack();
    }

    /**
     * @return {@code true} if the user canceled or closed the dialog
     */
    public boolean isCanceled()
    {
        return canceled;
    }

    /**
     * @return {@code true} if the user has configured to use custom labels
     */
    public boolean isUsingCustomLabels()
    {
        return tblDistances.isUsingCustomLabels();
    }

    /**
     * @return The list of distances as edited by the user
     */
    public List<Double> getDistances()
    {
        return tblDistances.getDistances();
    }

    /**
     * @return The list of labels as edited by the user, {@code null} if labels are not in use
     * @see #isUsingCustomLabels()
     */
    public List<String> getLabels()
    {
        return tblDistances.isUsingCustomLabels() ? tblDistances.getLabels() : null;
    }

    /**
     * Set whether custom labels are being used.  Toggle the visibility of the label column appropriately.
     */
    private void setUsingCustomLabels(boolean usingCustomLabels)
    {
        tblDistances.setUsingCustomLabels(usingCustomLabels);
        validateModel();
    }

    /**
     * Add a new row immediately after the currently selected row.  If no row is selected, then add a row at the end
     * of the table.
     */
    private void btnAddRowActionPerformed()
    {
        int row = tblDistances.getSelectedRow();

        if (row < 0) row = tblDistances.getRowCount() - 1;

        tblDistances.addRowAfter(row);
    }

    /**
     * Remove the currently selected row from the model.
     */
    private void btnRemoveRowActionPerformed()
    {
        final int[] rows = tblDistances.getSelectedRows();

        if (rows.length > 0)
        {
            for (int i = rows.length - 1; i >= 0; i--)
            {
                tblDistances.removeRow(rows[i]);
            }
        }
    }

    /**
     * Toggle the "use custom labels" setting.
     */
    private void chkUseCustomLabelsActionPerformed()
    {
        setUsingCustomLabels(chkUseCustomLabels.isSelected());
    }

    /**
     * Validate the contents of the model and display any warnings/errors found.
     *
     * @return {@code true} if no errors found
     */
    private boolean validateModel()
    {
        // Validate that the ranges are monotonically increasing

        final List<Double> distances = tblDistances.getDistances();

        if (distances.size() < 1)
        {
            setError("You must provide at least 1 distance");
            return false;
        }

        double lastDistance = 0;
        for (double distance : distances)
        {
            if (distance <= 0)
            {
                setError("distances must be > 0: " + distance);
                return false;
            }

            if (distance <= lastDistance)
            {
                setError(String.format("<html>distances must be strictly increasing:<br> %.1f &lt;= %.1f</html>", distance, lastDistance));
                return false;
            }
            lastDistance = distance;
        }

        // Show warnings for missing labels (if labels are in use)
        if (isUsingCustomLabels())
        {
            for (String label : tblDistances.getLabels())
            {
                if (StringUtils.isEmpty(label))
                {
                    setWarning("<html>Some labels are empty;<br>is that what you really want?</html>");
                    return true; // still valid
                }
            }
        }

        clearMessage();
        return true;
    }

    /**
     * Clear out the error/warning message and enable the OK button.
     */
    private void clearMessage()
    {
        lblMessage.setText("");
        lblMessage.setBorder(null);
        okButton.setEnabled(true);
    }

    /**
     * Display the given message, set the border to "error" and disable the OK button.
     *
     * @param msg Message to display
     */
    private void setError(String msg)
    {
        lblMessage.setText(msg);
        lblMessage.setBorder(errorBorder);
        okButton.setEnabled(false);
    }

    /**
     * Display the given message, set the border to "warning" and enable the OK button.
     *
     * @param msg Message to display
     */
    private void setWarning(String msg)
    {
        lblMessage.setText(msg);
        lblMessage.setBorder(warningBorder);
        okButton.setEnabled(true);
    }

    /**
     * Cancel the dialog.
     */
    private void cancelButtonActionPerformed()
    {
        canceled = true;
        setVisible(false);
    }

    /**
     * User is happy, moving on.
     */
    private void okButtonActionPerformed()
    {
        // This shouldn't be enabled if there are validation errors, but just in case...
        if (!validateModel()) return; // No bueno

        canceled = false; // Success!
        setVisible(false);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        chkUseCustomLabels = new JCheckBox();
        var panel1 = new JPanel();
        scrollDistancesTable = new JScrollPane();
        var panel2 = new JPanel();
        btnAddRow = new JButton();
        btnRemoveRow = new JButton();
        lblMessage = new JLabel();
        var buttonBar = new JPanel();
        okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Edit Range Distances/Labels");
        setModal(true);
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "default, $lgap, fill:default:grow, $lgap, default"));

                //---- chkUseCustomLabels ----
                chkUseCustomLabels.setText("Use Custom Labels     ");
                chkUseCustomLabels.setToolTipText("Check to display the area to enter custom ring labels");
                chkUseCustomLabels.setName("chkUseCustomLabels");
                chkUseCustomLabels.addActionListener(e -> chkUseCustomLabelsActionPerformed());
                contentPanel.add(chkUseCustomLabels, cc.xy(1, 1));

                //======== panel1 ========
                {
                    panel1.setName("panel1");
                    panel1.setLayout(new FormLayout(
                            "default:grow, $lcgap, center:min",
                            "fill:default:grow"));

                    //======== scrollDistancesTable ========
                    {
                        scrollDistancesTable.setName("scrollDistancesTable");
                    }
                    panel1.add(scrollDistancesTable, cc.xy(1, 1));

                    //======== panel2 ========
                    {
                        panel2.setName("panel2");
                        panel2.setLayout(new FormLayout(
                                "center:min",
                                "default, $lgap, default"));

                        //---- btnAddRow ----
                        btnAddRow.setFocusPainted(false);
                        btnAddRow.setFocusable(false);
                        btnAddRow.setToolTipText("Click to add a row after the current row");
                        btnAddRow.setName("btnAddRow");
                        btnAddRow.addActionListener(e -> btnAddRowActionPerformed());
                        panel2.add(btnAddRow, cc.xy(1, 1));

                        //---- btnRemoveRow ----
                        btnRemoveRow.setFocusPainted(false);
                        btnRemoveRow.setFocusable(false);
                        btnRemoveRow.setToolTipText("Click to delete the currently selected row");
                        btnRemoveRow.setName("btnRemoveRow");
                        btnRemoveRow.addActionListener(e -> btnRemoveRowActionPerformed());
                        panel2.add(btnRemoveRow, cc.xy(1, 3));
                    }
                    panel1.add(panel2, cc.xy(3, 1));
                }
                contentPanel.add(panel1, cc.xy(1, 3));

                //---- lblMessage ----
                lblMessage.setName("lblMessage");
                contentPanel.add(lblMessage, cc.xy(1, 5));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "pref"));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(4, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JCheckBox chkUseCustomLabels;
    private JScrollPane scrollDistancesTable;
    private JButton btnAddRow;
    private JButton btnRemoveRow;
    private JLabel lblMessage;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
