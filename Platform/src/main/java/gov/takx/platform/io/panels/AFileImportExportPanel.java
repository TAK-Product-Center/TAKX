package gov.takx.platform.io.panels;

import gov.takx.api.plugin.io.AWizardPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for file import and export wizard panels.
 *
 * @since 1.14
 */
public abstract class AFileImportExportPanel extends AWizardPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The file chooser that will be used to select the file targeted for
     * import or export.
     */
    protected JFileChooser chooser;

    /**
     * A reference to the text field in the JFileChooser to detect user keystrokes
     * and to obtain the current text.
     */
    protected JTextField textField;

    /**
     * The filter used to limit the selectable files.
     */
    protected FileFilter filter;

    /**
     * Find the first JTextField in a container. Used to find the JTextField
     * in the JFileChooser being used by this panel.
     *
     * @param comp The component to recursively inspect for a JTextField.
     * @return The first JTextField found, {@code null} one cannot be found.
     */
    Component findJTextField(Component comp)
    {
        if (comp instanceof JTextField) return comp;
        if (comp instanceof Container)
        {
            Component[] components = ((Container) comp).getComponents();
            for (Component component : components)
            {
                Component child = findJTextField(component);
                if (child != null) return child;
            }
        }
        return null;
    }

    /**
     * Retrieves the file currently selected in the JTextField.<br>
     * <br>
     * This method adds functionality beyond a simple call to JFileChooser.getSelectedFile(). It will first check
     * to see if the user has entered a full path in the text field of the chooser, e.g. "c:\myfile.csv".<br>
     * <br>
     * If it is not a valid file, it will take the parent of the currently selected file and append it to the text
     * entered by the user. This will handle the case where the user selects "c:\myfile.csv" but then uses the text
     * field to change the name to "myfile2.csv". The chooser will still think "c:\myfile.csv" is selected, so it
     * takes the currently selected file's parent and appends the user's text.<br>
     * <br>
     * If that too does not exist, the method will take the current directory and append the user's text.
     *
     * @return The file selected by the user.
     */
    protected File getSelectedFile()
    {
        // Get the text from the text field.
        final String text = textField.getText().trim();
        final File selectedFile = chooser.getSelectedFile();

        // If the text field is empty, but the selected field is not, then return the selected field.  This handles
        // cases where a subclass modifies the file chooser during construction in such a way as to cause the
        // text field to be erased, for example, if they change the file selection mode.

        if (text.isEmpty() && selectedFile != null)
        {
            return selectedFile;
        }

        // Use the text to create a new file. There should always be text in the text field, even if the user
        // just clicked on an existing file via the UI.
        File actualFile = new File(text);

        // Typically, the user will just type a file name that should override whatever is selected in the UI's file
        // list. To get the file, we need to determine what directory the file chooser is in.
        if (!actualFile.isAbsolute())
        {
            // Create a file with an absolute path.
            actualFile = new File(chooser.getCurrentDirectory(), actualFile.getName());
        }

        return actualFile;
    }

    /**
     * Get the list of selected files, in the case that multi-select is enabled.
     *
     * @return the list of selected files
     * @since 1.49
     */
    protected List<File> getSelectedFiles()
    {
        List<File> selectedFiles = new ArrayList<>();
        File[] files = chooser.getSelectedFiles();
        for (File file : files)
        {
            if (file.isDirectory())
            {
                selectedFiles.addAll(filesFromDirectory(file));
            } else
            {
                selectedFiles.add(file);
            }
        }
        return selectedFiles;
    }

    /**
     * Walk the directory path and add any files found to the list.
     *
     * @param directory directory to walk
     * @return list of files in the directory
     */
    private List<File> filesFromDirectory(File directory)
    {
        Path path = Paths.get(directory.getAbsolutePath());
        final List<File> files = new ArrayList<>();
        try
        {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>()
            {
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
                {
                    if (!attrs.isDirectory())
                    {
                        files.add(file.toFile());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e)
        {
            logger.error("File scan error", e);
        }
        return files;
    }
}
