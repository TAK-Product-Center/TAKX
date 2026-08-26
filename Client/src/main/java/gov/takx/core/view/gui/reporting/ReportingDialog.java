package gov.takx.core.view.gui.reporting;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.ErrorReportingSwingWorker;
import gov.takx.core.reporting.IssueReportDataCollector;
import gov.takx.core.view.gui.DialogHeaderPanel;
import gov.takx.platform.ui.AStandardDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * @since 2.0.0
 */
public class ReportingDialog extends AStandardDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public ReportingDialog()
    {
        initComponents();

        File userDirectory = new File(System.getProperty("user.home"));
        String displayText = "<html>To report an issue, enter a summary and description of the issue and press Finish. " +
                "An .rpt file containing this information along with TAKX logs and system details will be generated. " +
                "Email this resulting .rpt file to supporting engineers or TAKX developers for assistance. " +
                "The .rpt file can be opened by changing the extension to .zip. " +
                "Report files are added to %s.</html>";

        dialogHeaderPanel.setDescription(String.format(displayText, userDirectory.getAbsolutePath()));
        summaryTextField.requestFocus();
    }

    /**
     * Returns the report summary
     *
     * @return report summary
     */
    public String getReportSummary()
    {
        return summaryTextField.getText();
    }

    /**
     * Returns the report description
     *
     * @return report description
     */
    public String getReportDescription()
    {
        return descriptionTextArea.getText();
    }

    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    private void finishButtonActionPerformed()
    {
        IssueReportDataCollector collector = new IssueReportDataCollector();

        ErrorReportingSwingWorker<File, Object> worker = new ErrorReportingSwingWorker<>("Report Generation Error", "Failed to generate the report file.")
        {
            @Override
            protected File doInBackground() throws Exception
            {
                return collector.collectErrorReport(getReportSummary(), getReportDescription());
            }

            @Override
            protected void onFailure()
            {
                logger.error("Unable to generate report file {}", elist.getMessage());

                AlwaysOnTopJOptionPane.showErrorDialog(ReportingDialog.this,
                        "Unable to generate report file: " + elist.getMessage(), "Report Failure");
            }

            @Override
            protected void onSuccess(File result)
            {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
                {
                    try
                    {
                        Desktop.getDesktop().open(result.getParentFile());
                    } catch (IOException e)
                    {
                        logger.error("Unable to open windows explorer for file {}", result.getAbsolutePath());

                        AlwaysOnTopJOptionPane.showErrorDialog(ReportingDialog.this,
                                "Unable to open explorer for file: " + result.getAbsolutePath(),
                                "Report Failure");
                    }
                }
            }

            @Override
            protected void cleanup()
            {
                dispose();
            }
        };
        worker.execute();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        dialogHeaderPanel = new DialogHeaderPanel();
        var contentPanel = new JPanel();
        var summaryLabel = new JLabel();
        summaryTextField = new JTextField();
        var descriptionLabel = new JLabel();
        var descriptionScrollPane = new JScrollPane();
        descriptionTextArea = new JTextArea();
        var buttonPanel = new JPanel();
        var finishButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Report Issue");
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
                        "default, $lcgap, default:grow",
                        "2*(default, $lgap), default:grow"));

                //---- summaryLabel ----
                summaryLabel.setText("Summary:");
                summaryLabel.setName("summaryLabel");
                contentPanel.add(summaryLabel, cc.xy(1, 1));

                //---- summaryTextField ----
                summaryTextField.setName("summaryTextField");
                contentPanel.add(summaryTextField, cc.xy(3, 1));

                //---- descriptionLabel ----
                descriptionLabel.setText("Description:");
                descriptionLabel.setName("descriptionLabel");
                contentPanel.add(descriptionLabel, cc.xy(1, 3));

                //======== descriptionScrollPane ========
                {
                    descriptionScrollPane.setName("descriptionScrollPane");

                    //---- descriptionTextArea ----
                    descriptionTextArea.setName("descriptionTextArea");
                    descriptionScrollPane.setViewportView(descriptionTextArea);
                }
                contentPanel.add(descriptionScrollPane, cc.xywh(3, 3, 1, 3));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonPanel ========
            {
                buttonPanel.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonPanel.setName("buttonPanel");
                buttonPanel.setLayout(new FormLayout(
                        "$glue, $button, $lcgap, $button",
                        "fill:default:grow"));

                //---- finishButton ----
                finishButton.setText("Finish");
                finishButton.setName("finishButton");
                finishButton.addActionListener(e -> finishButtonActionPerformed());
                buttonPanel.add(finishButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonPanel.add(cancelButton, cc.xy(4, 1));
            }
            dialogPane.add(buttonPanel, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private DialogHeaderPanel dialogHeaderPanel;
    private JTextField summaryTextField;
    private JTextArea descriptionTextArea;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
