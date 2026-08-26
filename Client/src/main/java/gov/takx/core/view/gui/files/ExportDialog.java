package gov.takx.core.view.gui.files;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.filesystem.IVFSFile;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.mapobject.IFileMapObject;
import gov.takx.commons.file.IFileProgress;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.YesNoToAll;
import gov.takx.platform.ui.AStandardDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import static gov.takx.commons.swing.YesNoToAll.YES;

/**
 */
public class ExportDialog extends AStandardDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<IFileMapObject> fileMapObjects;
    private final IVirtualFileSystem fileSystem;
    private final File target;

    public ExportDialog(IFileMapObject file, IVirtualFileSystem fileSystem, File target)
    {
        this(Collections.singletonList(file), fileSystem, target);
    }

    public ExportDialog(List<IFileMapObject> files, IVirtualFileSystem fileSystem, File target)
    {
        setTitle("[ File ] Export Progress");
        initComponents();

        fileMapObjects = Collections.unmodifiableList(files);
        this.fileSystem = fileSystem;
        this.target = target;
    }

    @Override
    public void setVisible(boolean visible)
    {
        if (visible)
        {
            new Thread()
            {
                private YesNoToAll overwrite = YES;

                @Override
                public void run()
                {
                    int count = 1;
                    int size = fileMapObjects.size();
                    for (final IFileMapObject fileMapObject : fileMapObjects)
                    {
                        exportLabel.setText("Processing " + count + " of " + size);
                        exportProgressBar.setIndeterminate(true);
                        exportProgressBar.setValue(0);

                        final IVFSFile vfsFile = fileMapObject.getVfsFile();
                        final String fileName = vfsFile.getFileName();

                        if (new File(target, fileName).exists())
                        {
                            if (!overwrite.isToAll())
                            {
                                try
                                {
                                    SwingUtilities.invokeAndWait(() -> overwrite =
                                            AlwaysOnTopJOptionPane.showYesNoToAllDialog(ExportDialog.this,
                                                    "The file '" + fileName + "' already exists. Do you want to overwrite it?",
                                                    "Overwrite File?"));
                                } catch (InterruptedException | InvocationTargetException exception)
                                {
                                    logger.error("Error:", exception);
                                }
                            }

                            if (overwrite.isNo())
                            {
                                continue;
                            }
                        }

                        exportProgressBar.setIndeterminate(false);

                        try
                        {
                            fileSystem.exportFile(vfsFile, target, new IFileProgress()
                            {
                                @Override
                                public void setFileMaximum(int maximum)
                                {
                                    exportProgressBar.setMaximum(maximum);
                                }

                                @Override
                                public void setFileMinimum(int minimum)
                                {
                                    exportProgressBar.setMinimum(minimum);
                                }

                                @Override
                                @InvokeOnEDT
                                public void setFileProgress(final int progress)
                                {
                                    exportProgressBar.setValue(progress);
                                }
                            });
                        } catch (IOException exception)
                        {
                            logger.error("Error:", exception);
                        }

                        count++;
                    }

                    dispose();
                }
            }.start();
        }

        super.setVisible(visible);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        exportProgressBar = new JProgressBar();
        exportLabel = new JLabel();
        var panel1 = new JPanel();
        var okCancelButton = new JButton();
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

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default, $lgap, default:grow, $ugap, default"));

                //---- exportProgressBar ----
                exportProgressBar.setName("exportProgressBar");
                contentPanel.add(exportProgressBar, cc.xy(1, 1));

                //---- exportLabel ----
                exportLabel.setText("Processing 55 of 115");
                exportLabel.setName("exportLabel");
                contentPanel.add(exportLabel, cc.xy(1, 3, CellConstraints.CENTER, CellConstraints.DEFAULT));

                //======== panel1 ========
                {
                    panel1.setName("panel1");
                    panel1.setLayout(new FormLayout(
                            "default:grow, $lcgap, default",
                            "default"));

                    //---- okCancelButton ----
                    okCancelButton.setText("Cancel");
                    okCancelButton.setName("okCancelButton");
                    panel1.add(okCancelButton, cc.xy(3, 1));
                }
                contentPanel.add(panel1, cc.xy(1, 5));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JProgressBar exportProgressBar;
    private JLabel exportLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
