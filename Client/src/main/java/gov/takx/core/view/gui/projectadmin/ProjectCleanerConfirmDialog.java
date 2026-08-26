package gov.takx.core.view.gui.projectadmin;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.platform.ui.AStandardDialog;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ProjectCleanerConfirmDialog extends AStandardDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<Path> paths;
    private volatile boolean cancel = false;
    private boolean deletionRunning = false;
    private boolean projectsChanged = false;

    public ProjectCleanerConfirmDialog(Dialog owner, List<Path> paths)
    {
        super(owner);
        this.paths = new ArrayList<>(paths);
        initComponents();
        customizeComponents();
    }

    private void customizeComponents()
    {
        setModal(true);
        paths.forEach(p -> textArea.append(p.toString() + "\n"));
        progressBar.setMinimum(0);
        progressBar.setMaximum(paths.size());
        progressBar.setValue(0);
    }

    public boolean projectsChanged()
    {
        return projectsChanged;
    }

    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    @Override
    protected void onWindowClosing()
    {
        if (deletionRunning)
        {
            cancel = true;
        }
    }

    private void deleteButtonActionPerformed()
    {
        projectsChanged = true;
        label2.setText("Deleting projects...");
        textArea.setText("");
        deleteButton.setEnabled(false);
        progressBar.setString("0" + " / " + progressBar.getMaximum());
        progressBar.setStringPainted(true);
        progressBar.setEnabled(true);
        progressBar.setIndeterminate(false);
        deletionRunning = true;

        SwingWorker<Void, String> worker = new SwingWorker<>()
        {
            @Override
            protected Void doInBackground()
            {
                for (Path path : paths)
                {
                    if (cancel)
                    {
                        break;
                    }
                    try
                    {
                        FileUtils.deleteDirectory(path.toFile());
                        publish("Deleted " + path);
                    } catch (IOException e)
                    {
                        logger.error("IOException deleting {}: {}", path, e.getMessage());
                        publish("Error deleting " + path + "\n" + e.getMessage());
                    }
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks)
            {
                for (String message : chunks)
                {
                    textArea.append(message + "\n");
                    progressBar.setValue(progressBar.getValue() + 1);
                    progressBar.setString("" + progressBar.getValue() + " / " + progressBar.getMaximum());
                }
            }

            @Override
            protected void done()
            {
                deletionRunning = false;
                cancelButton.setText("Finish");

                if (cancel)
                {
                    label2.setText("Aborted.");
                    textArea.append("Aborted.");
                } else
                {
                    label2.setText("Done.");
                    textArea.append("Done.");
                }
            }
        };
        worker.execute();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        label2 = new JLabel();
        var scrollPane1 = new JScrollPane();
        textArea = new JTextArea();
        var buttonBar = new JPanel();
        progressBar = new JProgressBar();
        deleteButton = new JButton();
        cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Delete Projects Confirmation");
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
                        "default, $lgap, fill:default:grow"));

                //---- label2 ----
                label2.setText("<html>\nDelete the following projects and all associated data? \n<p>\n<b>This cannot be undone.</b>\n</html>");
                label2.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
                label2.setName("label2");
                contentPanel.add(label2, cc.xy(1, 1));

                //======== scrollPane1 ========
                {
                    scrollPane1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                    scrollPane1.setName("scrollPane1");

                    //---- textArea ----
                    textArea.setRows(10);
                    textArea.setColumns(50);
                    textArea.setEditable(false);
                    textArea.setName("textArea");
                    scrollPane1.setViewportView(textArea);
                }
                contentPanel.add(scrollPane1, cc.xy(1, 3));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$lcgap, default:grow, $glue, $button, $rgap, $button",
                        "pref"));

                //---- progressBar ----
                progressBar.setName("progressBar");
                buttonBar.add(progressBar, cc.xy(2, 1));

                //---- deleteButton ----
                deleteButton.setText("Delete All");
                deleteButton.setName("deleteButton");
                deleteButton.addActionListener(e -> deleteButtonActionPerformed());
                buttonBar.add(deleteButton, cc.xy(4, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(6, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel label2;
    private JTextArea textArea;
    private JProgressBar progressBar;
    private JButton deleteButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
