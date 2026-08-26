package gov.takx.core.view.gui.maps.importers;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.takx.core.globe.imagerywizard.AVTable;
import gov.takx.core.globe.imagerywizard.FileSet;
import gov.takx.core.globe.imagerywizard.FileSetFilter;
import gov.takx.core.globe.imagerywizard.ImagerySetFilter;
import gov.takx.core.globe.imagerywizard.InstallConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @since 3.4.1 (renamed from SimpleConvertMapsImporter)
 */
public class SimpleMapFilesImporter
{
    protected static final String[] FILE_SET_SORT_KEYS = {AVKey.TITLE};
    private final ImageryFilesImporter mapFilesImporter = new ImageryFilesImporter();
    private ExecutorService updateService;
    private final Object updateLock = new Object();
    private boolean isUpdating;
    private final List<File> selectedFiles;
    private final FileSetFilter fileSetFilter = new ImagerySetFilter();
    private String title = "";
    private final Map<String, Object> dataMap = mapFilesImporter.getDataMap();

    public SimpleMapFilesImporter(File path, String title)
    {
        selectedFiles = new ArrayList<>();
        dataMap.put(AVKey.PRODUCER_ENABLE_FULL_PYRAMID, true);
        dataMap.put(InstallConstants.INSTALL_LOCATION, path.getParentFile().getPath());
        setImagePath(path, title);
    }

    /**
     * Set the path to image(s) to convert and the title of the layer.
     *
     * @param path  path to images to convert.
     * @param title title of the layer.
     */
    private void setImagePath(File path, String title)
    {
        setTitle(title);
        selectedFiles.clear();
        if (path == null || !path.exists()) return;

        if (path.isFile())
        {
            selectedFiles.add(path);
        } else if (path.isDirectory())
        {
            selectedFiles.addAll(Arrays.asList(path.listFiles()));
        }
    }

    /**
     * Set the title of the layer.
     *
     * @param title title of the layer.
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * Are the file sets of image files currently being updated?
     *
     * @return {@code true} if the file sets are being updated, or {@code false} otherwise.
     */
    private synchronized boolean isFileSetsUpdating()
    {
        return isUpdating;
    }

    /**
     * Get the selected files.
     *
     * @return the selected files.
     */
    private List<File> getSelectedFiles()
    {
        return selectedFiles;
    }

    /**
     * Get the file set filter.
     *
     * @return the file set filter.
     */
    private FileSetFilter getFilter()
    {
        return fileSetFilter;
    }

    /**
     * Locate the image files and create file sets for these files.  Once the file sets are created, begin
     * the import/conversion process.  This work is done on a separate thread (via an ExecutorService).
     */
    public void updateFileSetsAndImport()
    {
        if (isFileSetsUpdating()) return;

        synchronized (updateLock)
        {
            if (updateService == null || updateService.isShutdown())
            {
                updateService = Executors.newSingleThreadExecutor();
            }

            updateService.execute(() -> {
                setFileSetsUpdating(true);
                doUpdateFileSets();
                setFileSetsUpdating(false);

                mapFilesImporter.beginImport();
            });
            updateService.shutdown();
        }
    }

    /**
     * Determine whether the files are usable or valid.
     *
     * @return {@code true} if the files are valid, or {@code false} otherwise.
     */
    private boolean isSelectedFilesValid()
    {
        List<File> files = getSelectedFiles();
        if (files == null || files.isEmpty()) return false;

        for (File f : files)
        {
            if (!f.exists() || !f.canRead()) return false;
        }

        return true;
    }

    /**
     * Determine whether files have been selected.
     *
     * @return {@code true} if files have been selected, or {@code false} otherwise.
     */
    private boolean isSelectedFilesInitialized()
    {
        List<File> files = getSelectedFiles();
        return (files != null) && (!files.isEmpty());
    }

    /**
     * Recursively filter files from the image file path and create a file set.
     *
     * @return a list of denied/invalid files (if any exist).
     */
    private List<File> doUpdateFileSets()
    {
        if (!isSelectedFilesInitialized() || !isSelectedFilesValid()) return null;

        List<File> selected = getSelectedFiles();

        // Filter the selected files, and recursively filter files beneath selected directories.
        // This provides a list of files that were explicitly selected by the user, and implicitly selected
        // by a directory search.
        List<File> files = new ArrayList<>();
        List<File> deniedFiles = new ArrayList<>();
        for (File f : selected)
        {
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

        FileSet fileSet = createFileSet(files);

        setFileSets(List.of(fileSet));
        return deniedFiles;
    }

    /**
     * Create a FileSet from a list of files.
     *
     * @param files a list of files.
     * @return new FileSet from list of files.
     */
    protected FileSet createFileSet(List<File> files)
    {
        FileSet fileSet = new FileSet();
        fileSet.setFiles(files);

        fileSet.setValue(AVKey.TITLE, title);
        fileSet.setValue(AVKey.DISPLAY_NAME, title);

        Object o = FileSet.makePath(fileSet);
        fileSet.setValue(InstallConstants.PATH, o);

        return fileSet;
    }

    private synchronized void setFileSetsUpdating(boolean isUpdating)
    {
        this.isUpdating = isUpdating;
    }

    private void setFileSets(final List<? extends AVList> fileSets)
    {
        AVTable.sortValues(fileSets, FILE_SET_SORT_KEYS);
        dataMap.put(InstallConstants.FILE_SETS, fileSets);
    }
}
