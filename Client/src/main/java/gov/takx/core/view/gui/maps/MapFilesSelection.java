package gov.takx.core.view.gui.maps;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.util.WWIO;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.globe.imagerywizard.AVTable;
import gov.takx.core.globe.imagerywizard.FileSet;
import gov.takx.core.globe.imagerywizard.FileSetFilter;
import gov.takx.core.globe.imagerywizard.FileSetTable;
import gov.takx.core.globe.imagerywizard.IndeterminateProgressMonitor;
import gov.takx.core.globe.imagerywizard.InstallConstants;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class is a refactor of FileSetSelectionPage - it's simpler than the old class, but needs more work
 * on streamlining and fixes.
 * @since 3.4.1 (renamed from ConvertMapsSelection)
 */
public class MapFilesSelection
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private JFileChooser localPathFileChooser;
    private final FileSetTable table;
    private final FileSetFilter filter;
    private final FileFilter extensionFilter;
    private final List<File> selectedFiles = new ArrayList<>();
    private ExecutorService updateService;
    private boolean isUpdating = false;
    private final Object updateLock = new Object();

    private final Map<String, Object> wizardData = new HashMap<>();
    private final JDialog owner;
    private IndeterminateProgressMonitor progressMonitor = null;
    private String selectionPath = "";

    public MapFilesSelection(@Nonnull FileSetFilter filter, @Nullable JDialog owner, @Nonnull FileSetTable table,
                             @Nonnull FileFilter extensionFilter)
    {
        this.filter = filter;
        this.owner = owner;
        this.table = table;
        this.extensionFilter = extensionFilter;

        this.table.getModel().addTableModelListener(e -> updateSelectedFiles());
    }

    private void updateSelectedFiles()
    {
        List<FileSet> fileSets = getSelectedFileSets();
        wizardData.put(InstallConstants.FILE_SETS, fileSets);
    }

    @Nonnull
    public FileSetFilter getFilter()
    {
        return filter;
    }

    public boolean isSelectedFilesInitialized()
    {
        return !getSelectedFiles().isEmpty();
    }

    public boolean isSelectedFilesValid()
    {
        List<File> files = getSelectedFiles();
        if (files.isEmpty()) return false;

        for (File f : files)
        {
            if (!f.exists() || !f.canRead())
            {
                return false;
            }
        }

        return true;
    }

    /**
     * @return The path to the selected file or a string with the parent directory if multiple files were selected; if
     * no files are selected, an empty string is returned.
     * @since 3.4.1
     */
    @Nonnull
    public String getSelectionPath()
    {
        return selectionPath;
    }

    @Nonnull
    public Map<String, Object> getWizardDataMap()
    {
        return wizardData;
    }

    @Nonnull
    public List<File> getSelectedFiles()
    {
        return Collections.unmodifiableList(selectedFiles);
    }

    @Nonnull
    public List<FileSet> getSelectedFileSets()
    {
        return table.getAllValues(InstallConstants.SELECTED, true);
    }

    /**
     * Prompt the user to select files with map data.
     *
     * @param singleDirectoryOnly Whether the file chooser should only allow selection of a single directory
     * @return The path to the selected file or a string with the parent directory and an indication that multiple files
     * were selected; if no files are selected, an empty string is returned. Null will be returned in the case the user
     * cancelled the file selection.
     * @since 3.4.1 (parameter added)
     */
    @Nullable
    public String browseLocalPath(boolean singleDirectoryOnly)
    {
        if (localPathFileChooser == null) localPathFileChooser = createLocalPathFileChooser(singleDirectoryOnly);

        int retVal = localPathFileChooser.showOpenDialog(owner);
        if (retVal == JFileChooser.CANCEL_OPTION) return null;

        File[] selectedFiles = singleDirectoryOnly ? new File[]{localPathFileChooser.getSelectedFile()} : localPathFileChooser.getSelectedFiles();
        if (!confirmLargeImport(Arrays.asList(selectedFiles))) return "";

        return doFileCheck(selectedFiles);
    }

    /**
     * Checks the files to ensure that they are all valid imagery or elevation files.
     *
     * @param selectedFiles The selected files
     * @return The path to the selected file or a string with the parent directory and an indication that multiple files
     * were selected; if no files are selected, an empty string is returned.
     */
    @Nonnull
    public String doFileCheck(File[] selectedFiles)
    {
        String multipleSelectionPath = null;

        // If nothing is selected, then clear the path.
        if (selectedFiles == null || selectedFiles.length < 1)
        {
            selectionPath = "";
            this.selectedFiles.clear();
        }

        // If a single file or directory is selected, then return that as the path.
        else if (selectedFiles.length == 1)
        {
            selectionPath = selectedFiles[0].getPath();
            this.selectedFiles.clear();
            this.selectedFiles.addAll(Arrays.asList(selectedFiles));
        }

        // If multiple files and/or directories are selected, return the parent directory plus an indication that
        // multiple files were selected.
        else
        {
            StringBuilder sb = new StringBuilder();

            if (localPathFileChooser != null)
            {
                selectionPath = WWIO.stripTrailingSeparator(localPathFileChooser.getCurrentDirectory().getPath());
                sb.append(selectionPath);
            }

            sb.append("/... (multiple selected)");

            multipleSelectionPath = sb.toString();
            this.selectedFiles.clear();
            this.selectedFiles.addAll(Arrays.asList(selectedFiles));
        }

        if (!confirmLargeImport(this.selectedFiles))
        {
            this.selectedFiles.clear();
            return "";
        }

        updateFileSets();
        return multipleSelectionPath == null ? selectionPath : multipleSelectionPath;
    }

    /**
     * Checks to see if the import is large enough to warrant a confirmation from the user, and returns false
     * if and only if this condition is true and the user selects to cancel.
     *
     * @param files files to count
     * @return false on user cancellation, true otherwise.
     */
    private boolean confirmLargeImport(List<File> files)
    {
        int count = FileUtils.countFiles(files, 2500);
        if (count >= 2500)
        {
            Object[] options = {"Yes", "Cancel"};
            int result = AlwaysOnTopJOptionPane.showOptionDialog(JFrame.getFrames()[0],
                    "Warning!  You are about to import more than " + count +
                            " files which may result in making the application unresponsive.",
                    "Continue?",
                    JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION, null, options, options[0]);
            return result != 1;
        }
        return true;
    }

    /**
     * Create and get a file chooser for browsing directories for conversion imagery/elevation sources.  Uses the
     * filename filter to quickly filter files based on filename only.  More in-depth inspection of files occurs in the
     * doUpdateFileSets method after file selection.
     *
     * @param singleDirectoryOnly Whether the file chooser should only allow selection of a single directory
     * @return a file chooser for browsing imagery/elevation sources.
     */
    @Nonnull
    private JFileChooser createLocalPathFileChooser(boolean singleDirectoryOnly)
    {
        JFileChooser fc = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        fc.setFileSelectionMode(singleDirectoryOnly ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_AND_DIRECTORIES);
        fc.setAcceptAllFileFilterUsed(true);
        fc.setMultiSelectionEnabled(!singleDirectoryOnly);
        fc.resetChoosableFileFilters();
        fc.addChoosableFileFilter(extensionFilter);
        return fc;
    }

    private void setFileSets(final List<? extends FileSet> fileSets)
    {
        // Go ahead and do the sorting on the current thread in case the list is large
        String[] sortKeys = {AVKey.TITLE};
        AVTable.sortValues(fileSets, sortKeys);

        // Then install the sorted values using the EDT
        setFileSetsOnEDT(fileSets);
    }

    /**
     * Set the table values on the EDT.
     *
     * @param fileSets File sets to install in table
     */
    @InvokeOnEDT
    private void setFileSetsOnEDT(List<? extends FileSet> fileSets)
    {
        table.setValues(fileSets);
    }

    /**
     * If a list of denied files has been returned from the doUpdateFileSets method, this method will display
     * a dialog to notify the user of the rejected files.
     *
     * @param deniedFiles a list of files denied by the file examination in doUpdateFileSets method
     */
    @InvokeOnEDT
    private void checkForDeniedFiles(final List<File> deniedFiles)
    {
        // If there is at least one denied file, display the notification dialog
        if (deniedFiles != null && !deniedFiles.isEmpty())
        {
            StringBuilder sb = new StringBuilder("<html>The following selected file(s) are not ");
            final String sourceType = extensionFilter.getDescription();
            sb.append(sourceType.toLowerCase());
            sb.append(" source(s) or do not have geo-referencing information:<br>");
            sb.append("<ul>");

            deniedFiles.forEach(file -> sb.append("<li>").append(file.getName()).append("</li>"));

            sb.append("</ul></html>");

            AlwaysOnTopJOptionPane.showMessageDialog(owner, sb.toString(), "Unusable " + sourceType + " Files", JOptionPane.WARNING_MESSAGE);
        }
    }

    private synchronized boolean isFileSetsUpdating()
    {
        return isUpdating;
    }

    private synchronized void setFileSetsUpdating(boolean isUpdating)
    {
        this.isUpdating = isUpdating;
        table.setEnabled(!isUpdating);
    }

    /**
     * Updates the file sets based on the selected files in a separate thread.
     */
    private void updateFileSets()
    {
        if (isFileSetsUpdating()) return;

        synchronized (updateLock)
        {
            if (updateService == null || updateService.isShutdown())
            {
                updateService = Executors.newSingleThreadExecutor();
            }

            updateService.execute(() -> {
                try
                {
                    // Display the update progress monitor.
                    progressMonitor = new IndeterminateProgressMonitor(
                            owner, "Searching for data...", true,
                            this::cancelFileSetUpdate);

                    progressMonitor.setVisible(true);

                    setFileSetsUpdating(true);
                    List<File> deniedFiles = doUpdateFileSets();

                    progressMonitor.setVisible(false);
                    checkForDeniedFiles(deniedFiles);

                    setFileSetsUpdating(false);
                } catch (Exception e)
                {
                    logger.warn("Error: ", e);
                }
            });
            updateService.shutdown();
        }
    }

    /**
     * Updates the file sets based on the selected files.
     *
     * @return List of files that didn't pass the filter, or {@code null} if an error occurs or the thread was interrupted.
     */
    @Nullable
    private List<File> doUpdateFileSets()
    {
        if (!isSelectedFilesInitialized() || !isSelectedFilesValid()) return null;

        List<File> selected = getSelectedFiles();
        int numFiles = selected.size();

        // Filter the selected files, and recursively filter files beneath selected directories.
        // This provides a list of files that were explicitly selected by the user, and implicitly selected
        // by a directory search.
        List<File> files = new ArrayList<>();
        List<File> deniedFiles = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++)
        {
            File f = selected.get(i);
            if (progressMonitor != null) progressMonitor.setProgressMessage("Validating: " + (i + 1) + "/" + numFiles);
            if (Thread.interrupted()) return null;

            if (f.isDirectory())
            {
                Iterator<File> iter = getFilter().filterDirectory(f);
                while (iter.hasNext())
                {
                    if (Thread.interrupted()) return null;
                    files.add(iter.next());
                }
            } else if (getFilter().accept(f))
            {
                if (Thread.interrupted()) return null;
                files.add(f);
            } else
            {
                deniedFiles.add(f);
            }
        }

        if (Thread.interrupted()) return null;

        List<FileSet> fileSets = getFilter().createFileSets(files);
        fileSets.forEach(fileSet -> fileSet.setValue(InstallConstants.SELECTED, true));

        setFileSets(fileSets);
        return deniedFiles;
    }

    public void cancelFileSetUpdate()
    {
        if (!isFileSetsUpdating()) return;

        synchronized (updateLock)
        {
            if (updateService != null)
            {
                updateService.shutdownNow();
                updateService = null;
                setFileSetsUpdating(false);
            }
        }
    }

    public void mergeSelectedFileSets(String title)
    {
        if (title == null) return;

        title = title.trim();

        List<FileSet> selected = getSelectedFileSets();
        if (selected.size() < 2) return;

        FileSet newFileSet = new FileSet();
        newFileSet.setValue(AVKey.TITLE, title);
        newFileSet.setValue(AVKey.DISPLAY_NAME, title);
        createMergedFileSet(selected, newFileSet);
        newFileSet.setValue(InstallConstants.SELECTED, true);

        List<FileSet> newFileSets = new ArrayList<>(table.getValues());
        newFileSets.removeAll(selected);
        newFileSets.add(newFileSet);

        setFileSets(newFileSets);
    }

    private static void createMergedFileSet(Iterable<? extends FileSet> fileSets, FileSet newFileSet)
    {
        Set<File> mergedFiles = new HashSet<>();
        for (FileSet fileSet : fileSets)
        {
            mergedFiles.addAll(fileSet.getFiles());
        }
        newFileSet.setFiles(mergedFiles);

        Object o = newFileSet.getValue(AVKey.TITLE);
        if (!(o instanceof String) || (((String) o).length() < 1) || o.equals(""))
        {
            o = FileSet.makeTitle(newFileSet);
            newFileSet.setValue(AVKey.TITLE, o);
        }

        o = FileSet.makePath(newFileSet);
        newFileSet.setValue(InstallConstants.PATH, o);
    }
}
