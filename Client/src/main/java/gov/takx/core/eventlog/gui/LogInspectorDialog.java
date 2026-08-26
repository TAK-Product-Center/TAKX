package gov.takx.core.eventlog.gui;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.configuration.ProgramEnvironment;
import gov.takx.platform.logging.LoggerUtils;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.utils.StandardTimeFormatting;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.OutputStreamAppender;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import java.awt.BorderLayout;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.OutputStream;

/**
 * Dynamic Log4j logger inspection dialog.  When the dialog is instantiated, a new temporary Log4j appender
 * is added to the root logger.  As long as the window is open, anything heading to the actual Log4j log is echoed on
 * this dialog.  As soon as this dialog is disposed (user hits the Close button), the temporary appender
 * is removed and the echo terminates immediately.
 *
 * @since 1.3 (renamed in 3.2)
 */
@PseudoBean
public class LogInspectorDialog extends AStandardDialog implements ClipboardOwner
{
    private final Appender textAreaAppender = OutputStreamAppender.newBuilder()
            .setTarget(new TextAreaOutputStream())
            .setLayout(LoggerUtils.getConsoleLayout())
            .setName("LogInspectorAppender")
            .build();

    @Inject
    private ProgramEnvironment programEnvironment;

    @Inject
    private StandardTimeFormatting timeFormatting;

    public LogInspectorDialog()
    {
        initComponents();
    }

    @PostConstruct
    private void postConstruct()
    {
        labelTime.setText(timeFormatting.formatWithCurrent(System.currentTimeMillis()));

        LoggerUtils.addAppender(textAreaAppender, Level.INFO);

        updateTextArea("===== LOG INSPECTOR DOES NOT DISPLAY HISTORICAL LOG ENTRIES =====\n" +
                programEnvironment.getProgramNameVersionAndBuild() + "\n\n");
    }

    private void closeButtonActionPerformed()
    {
        LoggerUtils.removeAppender(textAreaAppender);
        dispose();
    }

    private void copyButtonActionPerformed()
    {
        getToolkit().getSystemClipboard().setContents(new StringSelection(logText.getText()), this);
    }

    @InvokeOnEDT
    private void updateTextArea(String text)
    {
        logText.append(text);
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents)
    {
        // Ignore
    }

    /**
     * OutputStream that writes to our text area.
     * @since 3.2
     */
    private class TextAreaOutputStream extends OutputStream
    {
        @Override
        public void write(int b)
        {
            updateTextArea(String.valueOf((char) b));
        }

        @Override
        public void write(@Nonnull byte[] b, int off, int len)
        {
            updateTextArea(new String(b, off, len));
        }

        @Override
        public void write(@Nonnull byte[] b)
        {
            write(b, 0, b.length);
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var label1 = new JLabel();
        labelTime = new JLabel();
        var scrollPane1 = new JScrollPane();
        logText = new JTextArea();
        var buttonBar = new JPanel();
        var copyButton = new JButton();
        var closeButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Application Log");
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
                        "default:grow, 2*($lcgap, default), $lcgap, default:grow",
                        "default, $lgap, fill:default:grow"));

                //---- label1 ----
                label1.setText("Only displaying log entries after:");
                label1.setName("label1");
                contentPanel.add(label1, cc.xy(3, 1));

                //---- labelTime ----
                labelTime.setName("labelTime");
                contentPanel.add(labelTime, cc.xy(5, 1));

                //======== scrollPane1 ========
                {
                    scrollPane1.setName("scrollPane1");

                    //---- logText ----
                    logText.setName("logText");
                    scrollPane1.setViewportView(logText);
                }
                contentPanel.add(scrollPane1, cc.xywh(1, 3, 7, 1));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, 2*($lcgap, $button)",
                        "fill:default"));

                //---- copyButton ----
                copyButton.setText("Copy");
                copyButton.setName("copyButton");
                copyButton.addActionListener(e -> copyButtonActionPerformed());
                buttonBar.add(copyButton, cc.xy(3, 1));

                //---- closeButton ----
                closeButton.setText("Close");
                closeButton.setName("closeButton");
                closeButton.addActionListener(e -> closeButtonActionPerformed());
                buttonBar.add(closeButton, cc.xy(5, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel labelTime;
    private JTextArea logText;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
