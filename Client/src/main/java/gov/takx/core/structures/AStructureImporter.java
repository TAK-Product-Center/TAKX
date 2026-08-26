package gov.takx.core.structures;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.persist.ILayerPersist;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.structures.IPersistableSurfaceStructure;
import gov.takx.api.structures.ISurfaceStructure;
import gov.takx.api.structures.ISurfaceStructureCallback;
import gov.takx.api.tasks.ATaskRunnable;
import gov.takx.api.tasks.ITaskManager;
import gov.takx.api.tasks.ITaskProgressMonitor;
import gov.takx.api.tasks.TaskGroup;
import gov.takx.commons.file.FileUtils;
import gov.takx.core.tasks.AExitAffectingTask;
import gov.takx.core.tasks.MapsImporterTask;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract base for structure importers.  This handles the primary structural operations, delegating to subclass
 * implementations for the actual parsing of the file to be imported and generation of the surface structure
 * instances.
 * <p>
 * Derived classes must implement the following:
 * <ul>
 *     <li>{@link #prepareForImport()}</li>
 *     <li>{@link #countShapes()}</li>
 *     <li>{@link #loadStructures()}</li>
 *     <li>{@link #finalizeImport()}</li>
 * </ul>
 *
 * @since 3.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class AStructureImporter<L extends IStructureLayer> extends AExitAffectingTask implements IStructureImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    protected ITaskManager taskManager;

    @Inject
    protected ICoreSurfaceStructureService service;

    @Inject
    protected IRaptorLayerManager raptorLayerManager;

    @Inject
    private IPersistServiceProvider persistServiceProvider;

    @Inject
    private ILayerPersist layerPersist;

    @Inject
    private INotificationService notificationService;

    @Inject
    @VFSManagerScope(VFSScope.Project)
    protected IVirtualFileSystemManager projectVFSManager;

    protected final ISurfaceStructureCallback callback;

    protected File rootVfsImportDir; // Location in VFS where source file and associated files are placed
    protected final File originalFile;
    protected final IRaptorLayer raptorLayer;
    protected final L structureLayer;
    protected final List<ISurfaceStructure> importedStructures = new ArrayList<>();
    private final BackgroundTask backgroundTask = new BackgroundTask();

    protected File copiedFile;
    protected int numberOfShapes;
    protected int currentCount;

    /**
     * @param originalFile The file selected for import
     * @param raptorLayer  The Raptor layer that will hold the WW layer for the structures
     * @param callback     Callback to notify of import operations
     */
    protected AStructureImporter(File originalFile, IRaptorLayer raptorLayer, ISurfaceStructureCallback callback)
    {
        super(TaskGroup.SURFACE_STRUCTURE_IMPORT, "Structure File Importer");

        this.callback = callback;
        this.originalFile = originalFile;
        this.raptorLayer = raptorLayer;

        //noinspection unchecked
        structureLayer = (L) raptorLayer.getLayer();
    }

    /**
     * Prepare files for import.
     *
     * @return {@code true} if ready to import, {@code false} if something went wrong, or the import isn't supported.
     */
    protected abstract boolean prepareForImport();

    /**
     * Determine the number of valid shapes in the file.
     *
     * @return number of valid shapes in the file.
     */
    protected abstract int countShapes();

    /**
     * Load/create structures defined in our source file.
     */
    protected abstract void loadStructures();

    /**
     * Import operation is essentially complete, so perform any finalization needed.  This is typically when the
     * renderables created for the structures are installed into their respective structure layer.
     */
    protected abstract void finalizeImport();

    @Override
    public CompletableFuture<Integer> beginImport()
    {
        taskManager.execute(new MapsImporterTask(backgroundTask));

        return backgroundTask.getCompletionFuture();
    }

    /**
     * This method does most of the work importing shapes.
     */
    private void doImport()
    {
        final ITaskProgressMonitor monitor = backgroundTask.getMonitor();

        monitor.setMinProgress(0);
        monitor.setProgress(0);
        monitor.setProgressIndeterminate(true);

        try
        {
            if (!prepareForImport()) return;

            numberOfShapes = countShapes();

            if (callback != null) callback.onCountComplete(numberOfShapes);
            monitor.setProgressIndeterminate(false);
            monitor.setMaxProgress(numberOfShapes);

            testAborted();

            loadStructures();

            // From here, either the load will be aborted or the structures will be persisted, and we'll complete the import
            persistStructures();
            finalizeImport();
            notifyListenersOfCompletion();

            notify("Imported " + numberOfShapes + " shapes as structures", Notification.Type.Success);
        } catch (ImportAbortedException e)
        {
            layerPersist.deleteLayer(raptorLayer);
            raptorLayerManager.removeLayer(raptorLayer, false);
        } catch (Throwable e)
        {
            onError(e);
        } finally
        {
            monitor.setComplete();
        }
    }

    protected void notify(@Nonnull String message, @Nonnull Notification.Type type)
    {
        notificationService.show(Notification.builder(message, type).build());
    }

    @Override
    public void addImportedStructure(ISurfaceStructure structure)
    {
        testAborted();

        importedStructures.add(structure);

        updateProgress();
    }

    /**
     * Update the progress to reflect the state of the import and notify listeners.
     */
    protected void updateProgress()
    {
        final int count = ++currentCount;

        backgroundTask.getMonitor().setProgress(count);

        if (callback != null)
        {
            if (numberOfShapes > 0) callback.onProgress((count / ((double) numberOfShapes)) * 100);
        }
    }

    /**
     * Persist structures after loading from disk.
     */
    protected void persistStructures()
    {
        final List<IPersistableSurfaceStructure> entities = importedStructures.stream()
                .peek(structure -> structure.setLayer(raptorLayer))
                .map(ISurfaceStructure::getPersistable)
                .toList();

        testAborted();

        persistServiceProvider.getPersistService().batchPersistEntityList(entities);
    }

    /**
     * Notify any listeners of import completion.
     */
    protected void notifyListenersOfCompletion()
    {
        final int importedStructureCount = importedStructures.size();

        if (callback != null)
        {
            callback.onLoadComplete(importedStructureCount);
        }

        backgroundTask.getCompletionFuture().complete(importedStructureCount);

        service.importComplete(originalFile, structureLayer, importedStructures);
    }

    protected boolean makeVfsRootDir(String namespace)
    {
        final IVirtualFileSystem fileSystem = projectVFSManager.getVirtualFileSystem(namespace);

        final File rootDir = fileSystem.getRootDir();

        // Take the base name of original file and use that to create the directory.
        final String basename = FileUtils.getNameWithoutExtension(originalFile);
        final String targetDirName = FileUtils.getNextNameIteration(rootDir, basename); // Ensure a unique name
        rootVfsImportDir = new File(rootDir, targetDirName);

        if (!rootVfsImportDir.mkdirs())
        {
            logger.warn("Failed to make VFS import directory: {}", rootVfsImportDir);
            return false;
        }

        return true;
    }

    /**
     * Test if the user has aborted the import, and throw an exception if so.
     *
     * @throws ImportAbortedException If the user has aborted the import
     */
    protected void testAborted() throws ImportAbortedException
    {
        if (backgroundTask.isAborted()) throw new ImportAbortedException();
    }

    @Override
    public void execute()
    {
        // Nothing to do
    }

    /**
     * General error handling method.  Logs the error, notifies the callback, and shows the user the error.
     *
     * @param e throwable/exception.
     */
    protected void onError(Throwable e)
    {
        logger.error("Error importing: {}", originalFile, e);

        if (callback != null) callback.onError(e);

        backgroundTask.getCompletionFuture().completeExceptionally(e);

        notify("Error importing structures: " + e.getMessage(), Notification.Type.Error);
    }

    @Override
    public void doCancel()
    {
        backgroundTask.doCancel();
    }

    @Override
    public String getName()
    {
        return "Importing \"" + originalFile.getName() + "\"";
    }

    /**
     * A task runnable to tie the import work into the background task manager.
     */
    private class BackgroundTask extends ATaskRunnable
    {
        private final CompletableFuture<Integer> completionFuture = new CompletableFuture<>();
        private boolean aborted;

        @Override
        public void run()
        {
            doImport();
        }

        @Nonnull
        @Override
        public String getName()
        {
            return AStructureImporter.this.getName();
        }

        @Override
        public void doCancel()
        {
            aborted = true;
        }

        /**
         * @return true if the process is aborted
         */
        public boolean isAborted()
        {
            return aborted;
        }

        /**
         * @return the task progress monitor
         */
        public ITaskProgressMonitor getMonitor()
        {
            return monitor;
        }

        /**
         * @return Future completed when the import finishes, normally or exceptionally
         */
        public CompletableFuture<Integer> getCompletionFuture()
        {
            return completionFuture;
        }
    }
}
