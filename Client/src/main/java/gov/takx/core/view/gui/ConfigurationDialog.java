package gov.takx.core.view.gui;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.reporting.IssueReportDataCollector;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 */
@PseudoBean
public class ConfigurationDialog extends AStandardDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IssueReportDataCollector dataCollector = new IssueReportDataCollector();

    private String wwConfig = "";

    public ConfigurationDialog()
    {
        initComponents();
    }

    @PostConstruct
    private void postConstruct()
    {
        // Kick off the discovery of WW/OpenGL info, we will get a callback later
        WorldWindDiagnostics.getInfo(config -> {
            wwConfig = "\n\n=======================\n" +
                    "WorldWind Configuration\n" +
                    "=======================\n\n" +
                    config;
            refreshLog();
        });

        refreshLog();
    }

    private void refreshLog()
    {
        String config = dataCollector.collectConfigurationInfo(checkBoxProject.isSelected()) + wwConfig;

        textAreaInstallation.setText(config);

        final Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        scrollPane.getViewport().setPreferredSize(new Dimension(640, bounds.height / 2));

        pack();
    }

    private void buttonExportActionPerformed()
    {
        AlwaysOnTopFileChooser chooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);

        chooser.setDialogTitle("Export Configuration Log");

        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.resetChoosableFileFilters();
        chooser.setFileFilter(new ConfigFileFilter());

        if (chooser.showDialog(this, "Save") == JFileChooser.APPROVE_OPTION)
        {
            final File f = chooser.getSelectedFile();
            final String filename = !f.getName().endsWith(".zip") ? f.getName() + ".zip" : f.getName();
            final File zipFile = new File(f.getParentFile(), filename);

            if (logger.isDebugEnabled()) logger.debug("Zip file will be written to {}", zipFile.getAbsolutePath());

            try
            {
                dataCollector.exportConfiguration(zipFile, wwConfig, checkBoxProject.isSelected(), checkBoxLogs.isSelected());

                AlwaysOnTopJOptionPane.showMessageDialog(this, "Configuration and Logs Written to " + zipFile.getAbsolutePath(),
                        "Configuration Exported", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ioe)
            {
                logger.error("Unable to export the log file", ioe);
            }
        }
    }

    private void okButtonActionPerformed()
    {
        dispose();
    }

    private void checkBoxProjectActionPerformed()
    {
        refreshLog();
    }

    /**
     * Filter to accept zip files.
     * @since 3.5
     */
    private static class ConfigFileFilter extends javax.swing.filechooser.FileFilter
    {
        @Override
        public boolean accept(File f)
        {
            return f.isDirectory() || f.getAbsolutePath().toLowerCase().endsWith("zip");
        }

        @Override
        public String getDescription()
        {
            return "Installation Configuration Archive (*.zip)";
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var label1 = new JLabel();
        scrollPane = new JScrollPane();
        textAreaInstallation = new JTextArea();
        checkBoxProject = new JCheckBox();
        checkBoxLogs = new JCheckBox();
        var buttonBar = new JPanel();
        var buttonExport = new JButton();
        var okButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Configuration Log");
        setModal(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
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
                        "default, $lgap, fill:default:grow, 2*($lgap, default)"));

                //---- label1 ----
                label1.setText("Installation and Configuration Details:");
                label1.setName("label1");
                contentPanel.add(label1, cc.xy(1, 1));

                //======== scrollPane ========
                {
                    scrollPane.setName("scrollPane");

                    //---- textAreaInstallation ----
                    textAreaInstallation.setEditable(false);
                    textAreaInstallation.setName("textAreaInstallation");
                    scrollPane.setViewportView(textAreaInstallation);
                }
                contentPanel.add(scrollPane, cc.xy(1, 3));

                //---- checkBoxProject ----
                checkBoxProject.setText("Include Project Details");
                checkBoxProject.setSelected(true);
                checkBoxProject.setName("checkBoxProject");
                checkBoxProject.addActionListener(e -> checkBoxProjectActionPerformed());
                contentPanel.add(checkBoxProject, cc.xy(1, 5));

                //---- checkBoxLogs ----
                checkBoxLogs.setText("Attach All Log Files");
                checkBoxLogs.setSelected(true);
                checkBoxLogs.setName("checkBoxLogs");
                contentPanel.add(checkBoxLogs, cc.xy(1, 7));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $lcgap, $button",
                        "pref"));

                //---- buttonExport ----
                buttonExport.setText("Export...");
                buttonExport.setName("buttonExport");
                buttonExport.addActionListener(e -> buttonExportActionPerformed());
                buttonBar.add(buttonExport, cc.xy(2, 1));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(4, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JScrollPane scrollPane;
    private JTextArea textAreaInstallation;
    private JCheckBox checkBoxProject;
    private JCheckBox checkBoxLogs;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
