package gov.takx.core.ioservice;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.io.ImportExportResult;
import gov.takx.api.logging.SystemLogLevel;
import gov.takx.api.plugin.io.ACancellableWizard;
import gov.takx.api.plugin.io.IImportExportWizard;
import gov.takx.api.plugin.io.IImportWizard;
import gov.takx.api.systemmessage.ISystemMessageManager;
import gov.takx.api.systemmessage.SystemMessage;
import gov.takx.api.tasks.ITaskStatusHandler;
import gov.takx.api.tasks.TaskCompletionStatus;
import gov.takx.api.tasks.TaskGroup;
import gov.takx.commons.wizard.IWizardModel;
import gov.takx.core.tasks.AExitAffectingTask;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.io.panels.CommonWizardSettings;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract base for import/export wizard tasks.
 *
 * @param <WT> The actual wizard type
 * @since 1.42
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class AImportExportTask<WT extends IImportExportWizard> extends AExitAffectingTask
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private ISystemMessageManager messageManager;

    protected final WT wizard;
    protected final IWizardModel model;
    protected final TaskType taskType;
    protected long startTime;
    protected final CompletableFuture<ImportExportResult> resultFuture = new CompletableFuture<>();

    /**
     * Construct an import/export task on the given wizard and model.
     *
     * @param name   Display name for task
     * @param wizard The wizard to be executed
     * @param model  The wizard model
     */
    protected AImportExportTask(String name, WT wizard, IWizardModel model)
    {
        super(TaskGroup.IMPORT_EXPORT, name);

        this.wizard = wizard;
        this.model = model;
        taskType = wizard instanceof IImportWizard ? TaskType.Import : TaskType.Export;

        setCancelable(wizard.allowCancel());
    }

    /**
     * @return The CompletableFuture holding the result, which will be completed when this task has completed execution
     * @since 2.35
     */
    public CompletableFuture<ImportExportResult> getResultFuture()
    {
        return resultFuture;
    }

    @Override
    public void doCancel()
    {
        wizard.cancel();
    }

    /**
     * Dump the contents of the wizard settings.  Useful when testing/debugging.
     *
     * @param label Label for logging output
     */
    @SuppressWarnings("unused") // Not normally used
    private void dumpModelProps(String label)
    {
        logger.warn("Model Props: {}", label);

        final String[] keys = {CommonWizardSettings.FILE, CommonWizardSettings.FILES, CommonWizardSettings.FORMAT, CommonWizardSettings.LOCATION,
                CommonWizardSettings.MAP_OBJECT_FAMILY, CommonWizardSettings.MAP_OBJECT_TYPE,
                CommonWizardSettings.MESSAGE_TYPE, CommonWizardSettings.UNIT_ID, CommonWizardSettings.VFS};

        for (String key : keys)
        {
            logger.warn("   {} = {}", key, model.getProperty(key));
        }
    }

    @Override
    public void execute()
    {
        final ITaskStatusHandler progressMonitor = getTaskStatusHandler();
        ImportExportResult result;
        Exception operationException = null;

        try
        {
            startTime = System.currentTimeMillis();
            progressMonitor.printMessage(makeStatusMessage());
            result = doOperation();
        } catch (Exception e)
        {
            logger.error("Error: ", e);

            operationException = e;
            progressMonitor.setException(e);

            result = new ImportExportResult();
            result.setSuccess(false);
            result.setMessage((taskType + " failed: ") + e.getMessage());
        }

        // If the wizard was canceled, then mark the task canceled
        if (wizard instanceof ACancellableWizard)
        {
            if (((ACancellableWizard) wizard).isCancelled())
            {
                progressMonitor.setCompletionStatus(TaskCompletionStatus.CANCELED);
            }
        }

        finalizeMapObject();

        progressMonitor.setComplete();
        progressMonitor.setProgressIndeterminate(false);

        if (result == null)
        {
            progressMonitor.printMessage("Unable to " + taskType.toString().toLowerCase() + " data, unknown error.");
        } else if (result.getSuccess())
        {
            String message = result.getMessage();

            if (message != null && !message.isEmpty())
            {
                progressMonitor.printMessage(message);
            } else
            {
                progressMonitor.printMessage(String.format("%sed %d Map Objects and %d data points",
                        taskType, result.getMapObjectCount(), result.getDataPointCount()));
            }
            messageManager.addMessage(makeSystemMessage(result));
        } else
        {
            if (wizard instanceof ACancellableWizard && ((ACancellableWizard) wizard).isCancelled())
            {
                String message = result.getMessage();

                if (message != null && !message.isEmpty())
                {
                    progressMonitor.printMessage(message);
                } else
                {
                    progressMonitor.printMessage("Cancelled. "
                            + taskType + "ed " + result.getDataPointCount() + " data points");
                }
            } else
            {
                progressMonitor.printMessage(taskType + " failed: " + result.getMessage());
                messageManager.addMessage(makeSystemMessage(result));
            }
        }

        // Now complete our result future accordingly
        if (operationException != null)
        {
            resultFuture.completeExceptionally(operationException);
        } else
        {
            resultFuture.complete(result);
        }
    }

    @Override
    public boolean allowExit()
    {
        return getTaskStatusHandler().isComplete(); // Only allow exit if we're complete
    }

    /**
     * Construct a message that describes our current progress.
     *
     * @return exit message
     */
    @Override
    public String getExitMessage()
    {
        final ITaskStatusHandler handler = getTaskStatusHandler();

        final String file = model.getStringProperty(CommonWizardSettings.FILE);
        final String filePart = file == null ? "" : new File(file).getName() + ": ";

        if (handler.isProgressIndeterminate())
        {
            return String.format("%s: %s still working", getName(), filePart);
        } else
        {
            final int max = handler.getMaxProgress();
            final int min = handler.getMinProgress();
            final int progress = handler.getProgress();
            final int percent = progress * 100 / (max - min);

            return String.format("%s: %sat %d%% (%d of %d) complete", getName(), filePart, percent, progress, max);
        }
    }

    /**
     * Construct the system message representing our results.
     *
     * @param result The import/export result
     * @return System message to post
     */
    protected SystemMessage makeSystemMessage(ImportExportResult result)
    {
        final ITaskStatusHandler handler = getTaskStatusHandler();

        if (handler.getCompletionStatus() == TaskCompletionStatus.EXECPTION)
        {
            final String msg = getName() + ": " + taskType + " failed: " + result.getMessage();
            return new SystemMessage(SystemLogLevel.SYSTEM_ERROR, msg, handler.getException());
        } else if (handler.getCompletionStatus() == TaskCompletionStatus.CANCELED)
        {
            final String msg = getName() + ": " + taskType + " CANCELED!";
            return new SystemMessage(SystemLogLevel.SYSTEM_INFO, msg);
        } else
        {
            final String file = model.getStringProperty(CommonWizardSettings.FILE);
            final String fileStr = file != null ? file + ": " : "";

            final String resultMessage = result.getMessage();
            final String resultStr = (resultMessage != null && !resultMessage.isEmpty())
                    ? resultMessage
                    : String.format("%sed %d Map Objects and %d data points", taskType, result.getMapObjectCount(), result.getDataPointCount());

            final String msg = getName() + ": " + fileStr + resultStr + String.format(" in %.2f seconds", ((System.currentTimeMillis() - startTime) / 1000.));

            final SystemMessage systemMessage = new SystemMessage(SystemLogLevel.SYSTEM_INFO, msg);

            // If we have a location, then create an action to go to that location

            if (hasLocationInfo())
            {
                systemMessage.addAction(makeGotoAction()); // Add in the goto action
            }

            return systemMessage;
        }
    }

    /**
     * Make a status message indicating the action and the source/target file if known.
     *
     * @return Status message
     */
    private String makeStatusMessage()
    {
        final String file = model.getStringProperty(CommonWizardSettings.FILE);

        return file != null
                ? String.format("%sing %s %s", taskType, taskType.verb, new File(file).getName())
                : taskType + "ing data...";
    }

    /**
     * If we have components of a MO key in the wizard model, and no location data has been set, then set it.
     */
    private void finalizeMapObject()
    {
        final ITaskStatusHandler handler = getTaskStatusHandler();

        final boolean hasLocation = handler.getLocationOfInterest() != null
                || handler.getSectorOfInterest() != null
                || handler.getMapObjectKey() != null;

        if (hasLocation) return; // The wizard already specified a location datum

        final String family = model.getStringProperty(CommonWizardSettings.MAP_OBJECT_FAMILY);
        final String type = model.getStringProperty(CommonWizardSettings.MAP_OBJECT_TYPE);
        final String unitId = model.getStringProperty(CommonWizardSettings.UNIT_ID);

        if (family != null && type != null && unitId != null)
        {
            handler.setMapObjectKey(KeyUtils.getUniqueKey(family, type, unitId));
        }
    }

    /**
     * Perform the actual import/export operation.
     *
     * @return Import/Export result
     * @throws Exception On any error during operation
     */
    protected abstract ImportExportResult doOperation() throws Exception;

    /**
     * Manage the task we're performing and how to talk about it.
     */
    protected enum TaskType
    {
        Import("from"), Export("to");

        private final String verb;

        TaskType(String verb)
        {
            this.verb = verb;
        }
    }
}
