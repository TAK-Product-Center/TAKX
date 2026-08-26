package gov.takx.core.view.gui.projectadmin;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.project.IProjectManager;
import gov.takx.api.project.ProjectConstants;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.commons.date.DateFormatter;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.file.FileUtils.DirInfo;
import gov.takx.commons.file.SizeFormatter;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.documents.LimitedDocument;
import gov.takx.core.view.gui.DialogHeaderPanel;
import gov.takx.core.view.gui.ProgressDialog;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * Dialog for packaging a project.
 *
 */
@PseudoBean
public class ExportProjectGUI extends AStandardDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IProjectManager projectManager;

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    private IPersistServiceProvider persistServiceProvider;

    private Thread packageInfoLabelThread;
    private SwingWorker<Void, Object> worker;

    public ExportProjectGUI()
    {
        initComponents();
        dialogHeaderPanel.setDescription("<html>Save all the data, attachments, files, drawings, and other " +
                "information added to the current project into a single Package file.</html>");

        // TODO TAKX-1686 - restore this description when we can properly include imagery
//        dialogHeaderPanel.setDescription("<html>Save all the data, attachments, files, drawings, and other " +
//                "information added to the current project into a single Package file. The imagery and elevation " +
//                "caches can optionally be included in the package file.</html>");

        dialogHeaderPanel.setIcon(ButtonType.PACKAGE_PROJECT.getImageUrl());
    }

    @PostConstruct
    private void postConstruct()
    {
        // We can't hard-code "My Documents" here because it will fail on other operating systems.
        final String locationToWrite = new JFileChooser().getFileSystemView().getDefaultDirectory().getAbsolutePath();

        exportProjectNameTextField.setText(projectManager.getProjectName());
        exportProjectNameTextField.setDocument(new LimitedDocument(255, ProjectConstants.PROJECT_NAME_CHAR_VALIDATOR));
        locationTextField.setText(locationToWrite);
        packageInfoLabel.setText("Determining export details...");
        packageInfoLabelThread = new Thread(this::setPackageInfoLabel);
        packageInfoLabelThread.start();

        // Remove the "Package Imagery" check box until we can add support for imagery to the package mechanism.
        // TODO TAKX-1686

        exportImageryCheckBox.setVisible(false);
    }

    private void okButtonActionPerformed()
    {
        final String location = locationTextField.getText().trim();
        final String packageName = exportProjectNameTextField.getText().trim();

        if (validateFullPackagePath(packageName, location))
        {
            final boolean includeImagery = exportImageryCheckBox.isSelected();

            setVisible(false);

            final ProgressDialog pd = new ProgressDialog(this);
            pd.setTitle("Packaging Project");
            pd.setText("Creating package (This may take several minutes)");
            pd.setSubstatus("Preparing to zip project...");
            pd.pack();
            pd.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            pd.addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent e)
                {
                    thisWindowClosing(pd);
                }
            });
            pd.pack();
            createWorkerThread(packageName, location, includeImagery, pd);
            worker.execute();
            dispose();
        } else
        {
            JOptionPane.showMessageDialog(this, "Invalid location or the file already exists at this location.",
                    "Invalid package configuration", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void thisWindowClosing(ProgressDialog pd)
    {
        Object[] dlgButtons = {"Yes", "No"};

        int result = AlwaysOnTopJOptionPane.showOptionDialog(
                "Quit and remove all data? ",     // message
                "Confirm Quit Package Creation",  // title
                JOptionPane.WARNING_MESSAGE,      // message type
                JOptionPane.YES_NO_OPTION,        // option type
                null,                             // icon
                dlgButtons,                       // options
                dlgButtons[1]);

        if (result == 0)
        {
            removeDataAndClose(pd);
        }
    }

    private void createWorkerThread(final String name, final String location, final boolean includeImagery, final ProgressDialog pd)
    {
        worker = new SwingWorker<>()
        {
            @Override
            protected Void doInBackground()
            {
                // Show the progress dialog.
                pd.setMaximum(IProgressMonitor.INDETERMINATE);
                pd.setVisible(true);

                projectManager.createPackage(name, location, includeImagery, pd);

                // Check to see if the background thread has been cancelled.
                // If so, delete whatever progress has been made thus far.
                if (isCancelled())
                {
                    File file = new File(location + "/" + name + ".pkgx");

                    if (file.exists() && !file.delete())
                    {
                        logger.warn("Failed to delete partial package: {}", file);
                    }

                    return null;
                }

                return null;
            }

            @Override
            protected void done()
            {
                try
                {
                    try
                    {
                        get();
                        logger.debug("Package Created");
                    } catch (InterruptedException e)
                    {
                        logger.error("Interrupted in Done", e);
                    } catch (ExecutionException e)
                    {
                        Throwable cause = e.getCause();
                        logger.error("Failed to export package", cause);

                        String msg = "Failure during package export, errors were: " + ExceptionUtils.getMessageWithCauses("   ", cause);
                        JOptionPane.showMessageDialog(ExportProjectGUI.this, msg, "Failed to export package", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (CancellationException ignore)
                {
                    // Cancellation is acceptable. The background thread running doInBackground()
                    // will automatically receive an interrupt via the original cancel() call.
                } finally
                {
                    // Ensure that this occurs after the "setVisible" call, which was also called using invokeLater
                    SwingUtilities.invokeLater(() -> {
                        pd.setVisible(false);
                        pd.dispose();
                    });
                }
            }
        };
    }

    private void removeDataAndClose(final ProgressDialog pd)
    {
        pd.setVisible(false);
        pd.dispose();
        worker.cancel(true);
    }

    private boolean validateFullPackagePath(String packageName, String packageLocation)
    {
        if (!packageName.isEmpty() && !packageLocation.isEmpty())
        {
            File packagePath = new File(packageLocation, packageName + ".pkgx");
            if (!packagePath.exists())
            {
                File packageDirectory = new File(packageLocation);
                return packageDirectory.isDirectory() && packageDirectory.exists();
            }
        }
        return false;
    }

    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    @Override
    protected void onWindowClosing()
    {
        if (packageInfoLabelThread != null && packageInfoLabelThread.isAlive())
        {
            packageInfoLabelThread.interrupt();
        }
    }

    private void exportImageryCheckBoxItemStateChanged()
    {
        packageInfoLabel.setText("<html><b>Determining time to export project...</b></html>");

        if (packageInfoLabelThread.isAlive())
        {
            packageInfoLabelThread.interrupt();
        }
        packageInfoLabelThread = new Thread(this::setPackageInfoLabel);
        packageInfoLabelThread.start();
    }

    /**
     * This method is always invoked on a separate/new thread.
     */
    private void setPackageInfoLabel()
    {
        Thread.currentThread().setName("GetPackageInfo");

        final File projectPath = new File(projectManager.getProjectDirectory());
        final File additionalDirectory = exportImageryCheckBox.isSelected()
                ? globeManager.getPrimaryGlobeDelegate().getImageryDirectoryFile()
                : null;

        final String labelText = "<html><b>" + getPackageSizeDescription(projectPath, additionalDirectory) + "</b></html>";

        SwingUtilities.invokeLater(() -> {
            packageInfoLabel.setText(labelText);
            revalidate();
            pack();
        });
    }

    private String getPackageSizeDescription(File projectDirectory, File additionalDirectory)
    {
        // Count all the files and directories in the project
        final DirInfo projectInfo = FileUtils.getDirectoryInfo(projectDirectory);
        final DirInfo additionalInfo = additionalDirectory != null
                ? FileUtils.getDirectoryInfo(additionalDirectory)
                : new DirInfo();

        projectInfo.size += additionalInfo.size;
        projectInfo.fileCount += additionalInfo.fileCount;

        // Subtract out info for any files/directories excluded by our DB provider
        persistServiceProvider.getPersistService().getDatabaseProvider().getCoreServer().getPackageExcludes().stream()
                .map(filename -> new File(projectDirectory, filename))
                .forEach(file -> {
                    final var dirInfo = FileUtils.getDirectoryInfo(file);
                    projectInfo.size -= dirInfo.size;
                    projectInfo.fileCount -= dirInfo.fileCount;
                });

        final long fileTimeEst = projectInfo.size / (512 * 1024); // Assume 512 kB/ms write speed

        return "Estimated files to export: " + String.format("%,d", projectInfo.fileCount) +
                "<br>" +
                "Disk space required: ~" + SizeFormatter.formatEstimate(projectInfo.size) +
                "<br>" +
                "Processing time: " + DateFormatter.formatEstimate(fileTimeEst);
    }

    private void browseButtonActionPerformed()
    {
        final JFileChooser chooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = chooser.showDialog(this, "Package Project Directory");
        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            File file = chooser.getSelectedFile();

            if (file != null)
            {
                locationTextField.setText(file.getAbsolutePath());
            }
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        dialogHeaderPanel = new DialogHeaderPanel();
        var contentPanel = new JPanel();
        var nameLabel = new JLabel();
        exportProjectNameTextField = new JTextField();
        var saveLabel = new JLabel();
        locationTextField = new JTextField();
        var browseButton = new JButton();
        packageInfoLabel = new JLabel();
        var buttonBar = new JPanel();
        exportImageryCheckBox = new JCheckBox();
        var okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Create Package");
        setModal(true);
        setName("dialogExportProjectGUI");
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
                        "2*(default, $lcgap), default:grow",
                        "default, $lgap, default, $rgap, default, $lgap, default:grow"));

                //---- nameLabel ----
                nameLabel.setText("Name:");
                nameLabel.setName("nameLabel");
                contentPanel.add(nameLabel, cc.xy(1, 1));

                //---- exportProjectNameTextField ----
                exportProjectNameTextField.setName("exportProjectNameTextField");
                contentPanel.add(exportProjectNameTextField, cc.xywh(3, 1, 3, 1));

                //---- saveLabel ----
                saveLabel.setText("Save to:");
                saveLabel.setName("saveLabel");
                contentPanel.add(saveLabel, cc.xy(1, 3));

                //---- locationTextField ----
                locationTextField.setName("locationTextField");
                contentPanel.add(locationTextField, cc.xywh(3, 3, 3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

                //---- browseButton ----
                browseButton.setText("Change");
                browseButton.setName("browseButton");
                browseButton.addActionListener(e -> browseButtonActionPerformed());
                contentPanel.add(browseButton, cc.xy(3, 5));

                //---- packageInfoLabel ----
                packageInfoLabel.setVerticalAlignment(SwingConstants.TOP);
                packageInfoLabel.setName("packageInfoLabel");
                contentPanel.add(packageInfoLabel, cc.xywh(1, 7, 5, 1));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "default, $glue, default, $lcgap, default",
                        "default"));

                //---- exportImageryCheckBox ----
                exportImageryCheckBox.setText("Package Imagery");
                exportImageryCheckBox.setName("exportImageryCheckBox");
                exportImageryCheckBox.addItemListener(e -> exportImageryCheckBoxItemStateChanged());
                buttonBar.add(exportImageryCheckBox, cc.xy(1, 1));

                //---- okButton ----
                okButton.setText("Package");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(3, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(5, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        setSize(575, 365);
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private DialogHeaderPanel dialogHeaderPanel;
    private JTextField exportProjectNameTextField;
    private JTextField locationTextField;
    private JLabel packageInfoLabel;
    private JCheckBox exportImageryCheckBox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
