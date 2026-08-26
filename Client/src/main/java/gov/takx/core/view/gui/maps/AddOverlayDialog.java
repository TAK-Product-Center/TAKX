package gov.takx.core.view.gui.maps;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.core.view.gui.DialogHeaderPanel;
import gov.takx.core.view.gui.ProgressDialog;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.inject.Inject;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import java.awt.BorderLayout;
import java.io.File;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class AddOverlayDialog extends AStandardDialog
{
    @Inject
    @ProgramPreference(PreferenceEntry.ENABLE_2D_KML_SHAPE_IMPORT_AS_STRUCTURE)
    private boolean enable2DKMLShapeImportAsStructure;

    private IOverlayDialogListener listener = null;

    public AddOverlayDialog(String title)
    {
        initComponents();
        extrasPanel.setVisible(false);
        setTitle(title);
        fileChooser.setControlButtonsAreShown(false);
        setModal(true);
    }

    public void setFileFilter(FileFilter filter)
    {
        fileChooser.setFileFilter(filter);
    }

    @Override
    public void setVisible(boolean visible)
    {
        if (visible)
        {
            fileChooser.cancelSelection(); // Clear any selection from a previous use

            // We have to repack with an invokeLater() to be sure all of the HTML
            // wrapping is complete and the wrapped text sizes have been calculated.
            SwingUtilities.invokeLater(this::pack);
        } else
        {
            dispose();
        }
        super.setVisible(visible);
    }

    /**
     * @param iconFileName The name of the icon file within {@link gov.takx.platform.ui.ResourceDirectory#PLATFORM_ICONS}
     * @since 3.3
     */
    public void setIcon(String iconFileName)
    {
        dialogHeaderPanel.setIcon(iconFileName);
    }

    public void setOverlayDialogListener(IOverlayDialogListener listener)
    {
        this.listener = listener;
    }

    public void setDescription(String description)
    {
        if (description != null) dialogHeaderPanel.setDescription(description);
    }

    public void showOnTop()
    {
        setVisible(true);
        toFront();
    }

    @InvokeOnEDT
    public void enableExtrasPanel(final boolean enable)
    {
        extrasPanel.setVisible(enable);
    }

    @InvokeOnEDT
    public void setExtrasPanel(final JPanel extras)
    {
        extrasPanel.removeAll();
        extrasPanel.add(extras, new CellConstraints(1, 1));
    }

    public interface IOverlayDialogListener
    {
        void addOverlayFile(File overlayFile);
    }

    /**
     * Detect 3D geometry in an overlay file. The extending class may optionally use this method to display a
     * progress dialog for detecting 3D shapes while a background thread does the work.  Currently SHP and
     * KML/KMZ files support this functionality. Used in conjunction with doesFileHave3DGeometry and doImportFile.
     */
    protected void detect3DOverlayFileAndImport(final AddOverlayController controller)
    {
        // Display a dialog indicating that the shape file is being parsed (for 3D shapes).
        final ProgressDialog detect3DDialog = new ProgressDialog(this);
        String type = controller.getType();
        detect3DDialog.setTitle("Reading " + type);
        detect3DDialog.setText("Checking " + type + " for 3D Shapes...");
        detect3DDialog.setMaximum(-1);
        detect3DDialog.setModal(false);
        detect3DDialog.setVisible(true);

        Thread t = new Thread("Import overlay " + type + " Thread")
        {
            @Override
            public void run()
            {
                final boolean has3D = controller.doesFileHave3DGeometry();

                // Check is complete, so hide the progress dialog.
                detect3DDialog.setVisible(false);

                if (has3D)
                {
                    // Import the shape file (optionally as 3D structures).
                    OverlayUtils.handleOverlayInput(controller);
                } else
                {
                    // Only show the structure dialog if it's enabled from the ProgramProperties.xml file
                    if (enable2DKMLShapeImportAsStructure)
                    {
                        OverlayUtils.handleOverlayInput(controller);
                    } else
                    {
                        controller.doImportFile(false);
                    }
                }
            }
        };
        t.start();
    }

    private void okButtonActionPerformed()
    {
        File selectedFile = fileChooser.getSelectedFile();

        // Must have a non-null and non-empty file to be usable

        // NOTE: the "empty" path was happening because code in the setVisible method was actually
        // selecting "new File("")" to clear the selection.  I changed that to just cancel the selection, but I
        // think I'll leave this code here just to be safer.

        if (selectedFile != null && !selectedFile.getPath().isEmpty())
        {
            setVisible(false);
            listener.addOverlayFile(selectedFile);
        } else
        {
            JOptionPane.showMessageDialog(this, "Please select a valid file");
        }
    }

    private void cancelButtonActionPerformed()
    {
        setVisible(false);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        dialogHeaderPanel = new DialogHeaderPanel();
        var contentPanel = new JPanel();
        fileChooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        extrasPanel = new JPanel();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
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

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default:grow, $lgap, default"));

                //---- fileChooser ----
                fileChooser.setName("fileChooser");
                contentPanel.add(fileChooser, cc.xy(1, 1));

                //======== extrasPanel ========
                {
                    extrasPanel.setName("extrasPanel");
                    extrasPanel.setLayout(new FormLayout(
                            "default:grow",
                            "fill:default:grow"));
                }
                contentPanel.add(extrasPanel, cc.xy(1, 3));
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
                okButton.setText("Add");
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
    private DialogHeaderPanel dialogHeaderPanel;
    private JFileChooser fileChooser;
    private JPanel extrasPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
