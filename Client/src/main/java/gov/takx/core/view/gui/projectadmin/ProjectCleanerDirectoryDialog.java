package gov.takx.core.view.gui.projectadmin;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ProjectCleanerDirectoryDialog extends AStandardDialog
{
    private DefaultListModel<Path> directoryListModel;
    private boolean directoriesChanged = false;
    private final List<Path> directories;

    public ProjectCleanerDirectoryDialog(Dialog owner, Collection<Path> directories)
    {
        super(owner);
        this.directories = new ArrayList<>(directories);
        initComponents();
        customizeComponents();
    }

    private void customizeComponents()
    {
        setModal(true);
        directoryListModel = new DefaultListModel<>();
        directoryList.setModel(directoryListModel);
        directories.forEach(directoryListModel::addElement);
        updateRemoveButton();
    }

    public boolean getDirectoriesChanged()
    {
        return directoriesChanged;
    }

    public List<Path> getDirectories()
    {
        return directories;
    }

    private void updateRemoveButton()
    {
        Path selectedDirectory = directoryList.getSelectedValue();
        removeButton.setEnabled(selectedDirectory != null);
    }

    private void addButtonActionPerformed()
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = chooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION)
        {
            directoryListModel.addElement(chooser.getSelectedFile().toPath());
            directoriesChanged = true;
        }
    }

    private void removeButtonActionPerformed()
    {
        Path selection = directoryList.getSelectedValue();
        if (selection != null)
        {
            directoryListModel.removeElement(selection);
            directoriesChanged = true;
        }
    }

    private void directoryListValueChanged()
    {
        updateRemoveButton();
    }

    private void doneButtonActionPerformed()
    {
        directories.clear();
        for (int i = 0; i < directoryListModel.getSize(); ++i)
        {
            Path directory = directoryListModel.getElementAt(i).toAbsolutePath();
            directories.add(directory);
        }
        dispose();
    }

    private void cancelButtonActionPerformed()
    {
        directoriesChanged = false;
        dispose();
    }

    /**
     * Handles cleanup for when the dialog was closed using the X button.
     *
     * @since 3.3
     */
    @Override
    protected void onClosedByXButton()
    {
        cancelButtonActionPerformed();
    }

    /**
     * Handles cleanup for when the dialog was closed using the ESC key.
     *
     * @since 3.3
     */
    @Override
    protected void onEscape()
    {
        cancelButtonActionPerformed();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var label1 = new JLabel();
        var scrollPane1 = new JScrollPane();
        directoryList = new JList<>();
        var addButton = new JButton();
        removeButton = new JButton();
        var buttonBar = new JPanel();
        var doneButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Project Directory Locations");
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
                        "default:grow, $lcgap, $button",
                        "default, $lgap, fill:default:grow, 2*($lgap, default), $lgap, default:grow"));

                //---- label1 ----
                label1.setText("Where should the Project Cleaner look for directories to delete?");
                label1.setName("label1");
                contentPanel.add(label1, cc.xywh(1, 1, 3, 1));

                //======== scrollPane1 ========
                {
                    scrollPane1.setName("scrollPane1");

                    //---- directoryList ----
                    directoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    directoryList.setName("directoryList");
                    directoryList.addListSelectionListener(e -> directoryListValueChanged());
                    scrollPane1.setViewportView(directoryList);
                }
                contentPanel.add(scrollPane1, cc.xywh(1, 3, 1, 7));

                //---- addButton ----
                addButton.setText("Add");
                addButton.setName("addButton");
                addButton.addActionListener(e -> addButtonActionPerformed());
                contentPanel.add(addButton, cc.xy(3, 5));

                //---- removeButton ----
                removeButton.setText("Remove");
                removeButton.setName("removeButton");
                removeButton.addActionListener(e -> removeButtonActionPerformed());
                contentPanel.add(removeButton, cc.xy(3, 7));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "pref"));

                //---- doneButton ----
                doneButton.setText("Save");
                doneButton.setName("doneButton");
                doneButton.addActionListener(e -> doneButtonActionPerformed());
                buttonBar.add(doneButton, cc.xy(2, 1));

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
    private JList<Path> directoryList;
    private JButton removeButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
