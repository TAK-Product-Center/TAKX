package gov.takx.core.view.gui.takserver;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.IExtendedDataPackageManager;
import gov.tak.core.comms.takserver.DataPackageManager;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collection;

/**
 * A dialog to prompt the user to export their list of CotEvents as a new Data Package, or add to an existing one.
 *
 * @since 3.5
 */
@PseudoBean(singletonObserver = true)
public class DataPackageAddCreateDialog extends AStandardDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IExtendedDataPackageManager dataPackageManager;

    private final Collection<IMapObject> mapObjectCollection;

    /**
     * A dialog to interact with the {@link DataPackageManager} to create or update data packages with the passed in list of {@link IMapObject}s.
     *
     * @param mapObjectCollection a non-null collection of map objects to be used in creating a new data package, or adding to an existing data package
     */
    public DataPackageAddCreateDialog(@Nonnull Collection<IMapObject> mapObjectCollection)
    {
        initComponents();
        toggleVisiblePanels(); // ensures the right panel is visible first
        this.mapObjectCollection = mapObjectCollection;
        existingPackagesDropdown.setRenderer(new FilenameCellRenderer());
    }

    private void buttonGroupStateChanged()
    {
        toggleVisiblePanels();
    }

    @InvokeOnEDT
    private void toggleVisiblePanels()
    {
        if (createNew.isSelected())
        {
            addExistingPanel.setVisible(false);
            createNewPanel.setVisible(true);
            if (isDataPackageNameEmpty()) okButton.setEnabled(false);
            dataPackageNameTextField.requestFocus();
        } else
        {
            addExistingPanel.setVisible(true);
            createNewPanel.setVisible(false);
            dataPackageManager.getAllDataPackageContents().forEach(dataPackage -> existingPackagesDropdown.addItem(dataPackage.getPackageFile()));
            existingPackagesDropdown.setSelectedIndex(-1);
            okButton.setEnabled(false);
            existingPackagesDropdown.revalidate();
            existingPackagesDropdown.requestFocus();
        }
    }

    private void okButtonClicked()
    {
        if (createNew.isSelected())
        {
            try
            {
                File dataPackage = dataPackageManager.createNewDataPackage(dataPackageNameTextField.getText().trim());
                dataPackageManager.addMapObjectsToDataPackage(dataPackage, mapObjectCollection);
                dispose();
            } catch (IOException e)
            {
                AlwaysOnTopJOptionPane.showErrorDialog(
                        "Could not create the new data package:\n" + e.getMessage(),
                        "Data Package Creation Error");

                logger.warn("Error creating the new data package", e);
            }

        } else
        {
            File dataPackage = (File) existingPackagesDropdown.getSelectedItem();
            if (dataPackage == null)
            {
                logger.warn("Cannot proceed with a non-selection"); //shouldn't be able to be reached but as a safeguard
            } else
            {
                try
                {
                    dataPackageManager.addMapObjectsToDataPackage(dataPackage, mapObjectCollection);
                    dispose();
                } catch (IOException e)
                {
                    AlwaysOnTopJOptionPane.showErrorDialog(
                            "Could not update the data package:\n" + e.getMessage(),
                            "Data Package Update Error");
                    logger.error("Error adding objects to {}", ((File) existingPackagesDropdown.getSelectedItem()).getName(), e);
                }
            }

        }
    }

    private void cancelButtonClicked()
    {
        dispose();
    }

    private void dataPackageNameTextFieldKeyTyped()
    {
        okButton.setEnabled(!isDataPackageNameEmpty());
    }

    private void existingPackagesDropdownItemStateChanged()
    {
        okButton.setEnabled(existingPackagesDropdown.getSelectedItem() != null);
    }

    private boolean isDataPackageNameEmpty()
    {
        return dataPackageNameTextField.getText().isBlank();
    }

    private boolean isExistingDataPackageSelected()
    {
        return existingPackagesDropdown.getSelectedItem() != null;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        createNew = new JRadioButton();
        createNewPanel = new JPanel();
        var label1 = new JLabel();
        dataPackageNameTextField = new JTextField();
        addExisting = new JRadioButton();
        addExistingPanel = new JPanel();
        existingPackagesDropdown = new JComboBox<>();
        var buttonBar = new JPanel();
        okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Export Data Package");
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.createEmptyBorder("7dlu, 7dlu, 7dlu, 7dlu"));
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new FormLayout(
                    "default:grow",
                    "fill:default:grow, fill:default"));

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "3*(default, $lgap), default"));

                //---- createNew ----
                createNew.setText("Create new Data Package");
                createNew.setSelected(true);
                createNew.setName("createNew");
                createNew.addItemListener(e -> buttonGroupStateChanged());
                contentPanel.add(createNew, cc.xy(1, 1));

                //======== createNewPanel ========
                {
                    createNewPanel.setName("createNewPanel");
                    createNewPanel.setLayout(new FormLayout(
                            "default:grow",
                            "default, $lgap, default"));

                    //---- label1 ----
                    label1.setText("Data Package Name:");
                    label1.setName("label1");
                    createNewPanel.add(label1, cc.xy(1, 1));

                    //---- dataPackageNameTextField ----
                    dataPackageNameTextField.setName("dataPackageNameTextField");
                    dataPackageNameTextField.addKeyListener(new KeyAdapter()
                    {
                        @Override
                        public void keyTyped(KeyEvent e)
                        {
                            dataPackageNameTextFieldKeyTyped();
                        }
                    });
                    createNewPanel.add(dataPackageNameTextField, cc.xy(1, 3));
                }
                contentPanel.add(createNewPanel, cc.xy(1, 3));

                //---- addExisting ----
                addExisting.setText("Add to existing Data Package");
                addExisting.setName("addExisting");
                contentPanel.add(addExisting, cc.xy(1, 5));

                //======== addExistingPanel ========
                {
                    addExistingPanel.setName("addExistingPanel");
                    addExistingPanel.setLayout(new FormLayout(
                            "default:grow",
                            "default"));

                    //---- existingPackagesDropdown ----
                    existingPackagesDropdown.setName("existingPackagesDropdown");
                    existingPackagesDropdown.addItemListener(e -> existingPackagesDropdownItemStateChanged());
                    addExistingPanel.add(existingPackagesDropdown, cc.xy(1, 1));
                }
                contentPanel.add(addExistingPanel, cc.xy(1, 7));
            }
            dialogPane.add(contentPanel, cc.xy(1, 1));

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.createEmptyBorder("5dlu, 0dlu, 0dlu, 0dlu"));
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "pref"));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonClicked());
                buttonBar.add(okButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonClicked());
                buttonBar.add(cancelButton, cc.xy(4, 1));
            }
            dialogPane.add(buttonBar, cc.xy(1, 2));
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());

        //---- buttonGroup1 ----
        var buttonGroup1 = new ButtonGroup();
        buttonGroup1.add(createNew);
        buttonGroup1.add(addExisting);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JRadioButton createNew;
    private JPanel createNewPanel;
    private JTextField dataPackageNameTextField;
    private JRadioButton addExisting;
    private JPanel addExistingPanel;
    private JComboBox<File> existingPackagesDropdown;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on

    private static class FilenameCellRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof File file) setText(file.getName());
            return this;
        }
    }
}
