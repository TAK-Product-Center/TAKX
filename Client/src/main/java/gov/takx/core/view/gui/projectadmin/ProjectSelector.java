package gov.takx.core.view.gui.projectadmin;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.project.IProjectManager;
import gov.takx.api.project.ProjectConstants;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.SwingFileFilter;
import jakarta.inject.Inject;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import java.awt.Component;
import java.io.File;

/**
 * Present the user with a file selection dialog to allow the selection of a project to open.
 */
@PseudoBean
public class ProjectSelector
{
    private static final FileFilter PROJECT_FILE_FILTER =
            new SwingFileFilter(ProjectConstants.PROJECT_EXTENSION, ProjectConstants.PACKAGE_EXTENSION);

    @Inject
    private IProjectManager projectManager;

    public ProjectSelector()
    {
    }

    public String selectNewProjectFilePath()
    {
        String projectName = projectManager.getDefaultProjectName();
        String inputString = "Please enter a name for this project session";
        boolean loop;
        do
        {
            //prompt user for new Project name
            projectName = AlwaysOnTopJOptionPane.showInputDialog(inputString, "Project Name", projectName);

            // Null indicates cancellation.
            // (Empty string indicates user did not enter a name.)
            if (projectName == null)
            {
                return null;
            } else if (projectName.isEmpty())
            {
                inputString = "No name was given. Please enter another name.";
            } else if (projectManager.doesProjectExist(projectName))
            {
                Object[] options = {"Yes", "No"};
                int result = AlwaysOnTopJOptionPane.showOptionDialog(null, "Project name " + projectName + " already exists.  Overwrite?",
                        "Overwrite Project", JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION, null, options, options[0]);

                if (result == 0)
                {
                    return projectManager.getProjectFilePath(projectName);
                } else
                {
                    return null;
                }
            }

            // Test for project name validity and loop if not valid.
            if (loop = !projectManager.isValidNewProjectName(projectName))
            {
                inputString = "Invalid name given.  Please enter another name.";
            }
        } while (loop);

        return projectManager.getProjectFilePath(projectName);
    }

    public String selectProjectToOpen(Component owner)
    {
        String result = null;

        File currentDirectory = projectManager.getProjectsDirectory();

        final AlwaysOnTopFileChooser chooser = AlwaysOnTopFileChooserFactory.getFileChooser(owner, currentDirectory);

        chooser.addChoosableFileFilter(PROJECT_FILE_FILTER);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int returnVal = chooser.showDialog(owner, "Open");

        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            File file = chooser.getSelectedFile();

            if (file == null)
            {
                result = chooser.getCurrentDirectory().getAbsolutePath();
            } else
            {
                result = file.getAbsolutePath();
            }
        }

        return result;
    }
}
