package gov.takx.commons.swing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileSystemView;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.HeadlessException;
import java.io.File;
import java.lang.invoke.MethodHandles;

/**
 */
public class AlwaysOnTopFileChooser extends JFileChooser
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private JPanel headerPanel;

    public AlwaysOnTopFileChooser()
    {
        super();
        setAcceptAllFileFilterUsed(false);
    }

    public AlwaysOnTopFileChooser(String currentDirectoryPath)
    {
        super(currentDirectoryPath);
        setAcceptAllFileFilterUsed(false);
    }

    public AlwaysOnTopFileChooser(File currentDirectory)
    {
        super(currentDirectory);
        setAcceptAllFileFilterUsed(false);
    }

    public AlwaysOnTopFileChooser(FileSystemView fsv)
    {
        super(fsv);
        setAcceptAllFileFilterUsed(false);
    }

    public AlwaysOnTopFileChooser(File currentDirectory, FileSystemView fsv)
    {
        super(currentDirectory, fsv);
        setAcceptAllFileFilterUsed(false);
    }

    public AlwaysOnTopFileChooser(String currentDirectoryPath, FileSystemView fsv)
    {
        super(currentDirectoryPath, fsv);
        setAcceptAllFileFilterUsed(false);
    }

    /**
     * Clear the selected file before opening the dialog.
     */
    @Override
    public int showOpenDialog(Component parent) throws HeadlessException
    {
        cancelSelection();
        return super.showOpenDialog(parent);
    }

    /**
     * Clear the selected file before opening the dialog.
     */
    @Override
    public int showDialog(Component parent, String approveButtonText) throws HeadlessException
    {
        cancelSelection();
        return super.showDialog(parent, approveButtonText);
    }

    /**
     * Clear the selected file before opening the dialog.
     */
    @Override
    public int showSaveDialog(Component parent) throws HeadlessException
    {
        cancelSelection();
        return super.showSaveDialog(parent);
    }

    @Override
    public void approveSelection()
    {
        File file = getSelectedFile();

        if (file == null) return;

        if ((!isDirectorySelectionEnabled() || !file.isDirectory()) && file.exists() && getDialogType() == SAVE_DIALOG)
        {
            int result = AlwaysOnTopJOptionPane.showConfirmDialog(getTopLevelAncestor(), "The selected file '" + file.getName() + "' already exists. Do you want to overwrite it?",
                    "The file already exists",
                    JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.YES_NO_CANCEL_OPTION);

            switch (result)
            {
                case JOptionPane.YES_OPTION:
                    break;
                case JOptionPane.NO_OPTION:
                    return;
                case JOptionPane.CANCEL_OPTION:
                    cancelSelection();
                    return;
                default:
                    logger.error("Unhandled result: {}", result);
                    return;
            }
        }

        super.approveSelection();
    }

    // Overriding to enable alwaysOnTop
    @Override
    protected JDialog createDialog(Component parent) throws HeadlessException
    {
        JDialog dialog = super.createDialog(parent);
        dialog.setModal(true);

        if (headerPanel != null)
        {
            final Container contentPane = dialog.getContentPane();
            contentPane.add(headerPanel, BorderLayout.NORTH);
        }

        return dialog;
    }

    /**
     * Add the provided panel as a header for the file chooser dialog.
     *
     * @param headerPanel The panel to use as the header
     * @since 3.3
     */
    public void addHeaderPanel(JPanel headerPanel)
    {
        this.headerPanel = headerPanel;
    }
}
