package gov.takx.api.plugin.io;

import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.io.Format;
import gov.takx.api.io.ImportExportResult;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.platform.NameFixingRunnable;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.wizard.IWizardPanelDescriptor;
import gov.takx.commons.wizard.IWizardProperties;
import gov.takx.platform.io.panels.CommonWizardSettings;
import gov.takx.platform.io.panels.FileToImportPanel;
import gov.takx.platform.io.panels.ImportProblemsDialog;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base class for a Cancellable wizard providing some common functionality:
 * <ol>
 * <li>A static, common thread pool is supplied to parse multiple files concurrently</li>
 * <li>This importer is declared to support multiple files and override default TAKX importers</li>
 * <li>Handles the cancellation of a parser request from the operator by shutting down the parser threads</li>
 * <li>Basic offering to process files based on the declared file format</li>
 * </ol>
 * <p>
 * The main purpose of this abstract class is to assist the developer in writing an importer by removing the
 * need to continuously write boiler plate code for methods like
 * {@link #doImport(IWizardProperties, IImportExportProgressMonitor)}.
 * <p>
 * In most cases, only 3 methods need to be implemented (see Acme example):
 * <ol>
 * <li>{@link #validateFileForImport(String)} - Return true in result if this file can be parsed by this importer</li>
 * <li>{@link #preParseFile(String)} - Set number of counts in result and return true in result if counts &gt; 0</li>
 * <li>{@link #createImportParser(String, Map, IWizardProperties)} - return a parser instance to do the actual import.</li>
 * </ol>
 *
 * @since 2.14; renamed from ACancellableImporter in 3.0
 */
@SuppressWarnings("unused") // called from plugins
@PseudoBean
public abstract class ACancellableImportWizard extends ACancellableWizard implements IImportWizard, IImportMonitor, IImportFileValidator
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Maximum number of threads in the executor pool.  Use a minimum of 4 since a lot of the work is reading
     * from files, and that can overlap well, even on small machines.
     */
    protected static final int maxThreads = Math.max(4, Runtime.getRuntime().availableProcessors());

    /**
     * Manages the set of parser runnables.
     */
    protected static final ThreadPoolExecutor executorService =
            new ThreadPoolExecutor(maxThreads, maxThreads, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    /**
     * Progress monitor.
     */
    protected IImportExportProgressMonitor progressMonitor;

    /**
     * How frequently to update the progress monitor.  Default to every 200 if we don't get a valid count to start.
     */
    protected int progressUpdateInterval = 200;

    /**
     * Track the set of parsers that are running so that their results can be checked.
     */
    private final List<ParserRunnable> parserRunnables = new ArrayList<>();

    /**
     * The total number of messages that have been parsed.
     */
    protected AtomicInteger messageCount = new AtomicInteger();

    /**
     * The number of Map Objects created by this import.
     */
    protected AtomicInteger mapObjectCount = new AtomicInteger();

    @Inject
    @Headless
    private boolean headless;

    static
    {
        executorService.allowCoreThreadTimeOut(true); // Allow the threads to time out
    }

    protected ACancellableImportWizard()
    {
    }

    /**
     * Cancel parsing.  Cancels any running parsers and shuts the executor service down.
     */
    @Override
    public void cancel()
    {
        super.cancel();

        // This is necessary because the PersistService clears the interrupt flag.
        for (ParserRunnable parserRunnable : parserRunnables)
        {
            parserRunnable.cancel();
        }

        parserRunnables.clear();
    }

    /**
     * Callback when files are added through drag and drop or through the file chooser to allow the importer to
     * scan and declare that the file is parsable by this importer
     *
     * @param fileExtension file extension
     * @param file          the file
     * @return {@code true} if the importer determines the file can be parsed.
     */
    @Override
    public boolean offerToProcessFile(String fileExtension, File file)
    {
        if (file.isDirectory())
        {
            try
            {
                ImportFileIterator iterator = new ImportFileIterator(file.getCanonicalPath(), getFormats(), this, true);
                return iterator.isValid();
            } catch (Exception e)
            {
                return false;
            }
        } else
        {
            return super.offerToProcessFile(fileExtension, file) && validateFile(file.getAbsolutePath(), true);
        }
    }

    /**
     * Do the import.  Scans the list of files that were deemed "importable" by the validation methods and tries
     * to import them.
     *
     * @param props           properties, including the list of files to parse
     * @param progressMonitor progress monitor, to update status while parsing.
     * @return import result
     * @throws Exception exception during import
     */
    @SuppressWarnings("unchecked")
    @Override
    public ImportExportResult doImport(IWizardProperties props, IImportExportProgressMonitor progressMonitor)
            throws Exception
    {
        this.progressMonitor = progressMonitor;

        if (!parserRunnables.isEmpty())
        {
            logger.error("Reusing an importer while we have parsers still running: {}", getClass().getSimpleName());
        }

        // Because this importer has indicated that it supports multiple files,
        // get the user selection from the FILES entry rather than the FILE entry.
        final List<File> files = (List<File>) getPreconfigurationSettings()
                .getOrDefault(CommonWizardSettings.FILES, props.getProperty(CommonWizardSettings.FILES));

        final FilePreCheckState preCheckState = preCheckFiles(files);

        reportAnyPreCheckFailures(preCheckState); // The user may cancel us here

        final ImportExportResult result = performImport(props, progressMonitor, preCheckState);

        notifyResultListeners(result);
        return result;
    }

    /**
     * process all the input files and run a preCheck on them.
     *
     * @param files Input files to process
     * @return PreCheckState
     * @throws IOException if walking the file tree fails
     * @since 2.18
     */
    protected FilePreCheckState preCheckFiles(List<File> files) throws IOException
    {
        final List<Format> formats = getFormats();
        final List<File> expandedFileList = new ArrayList<>();

        // Expand the list by walking file trees and adding all files that match our formats

        for (File file : files)
        {
            if (file.isDirectory())
            {
                // Collect all files with acceptable file extensions (no other validation is performed)
                final ImportFileIterator iterator = new ImportFileIterator(file.getCanonicalPath(), formats);
                expandedFileList.addAll(iterator.getFileList());
            } else
            {
                expandedFileList.add(file);
            }
        }

        // Now run the pre-check on all the files

        final FilePreCheckState preCheckState = new FilePreCheckState();

        if (!expandedFileList.isEmpty())
        {
            // Process each item in the directory in parallel
            expandedFileList.parallelStream().forEach(f -> preCheckFile(f, preCheckState));
        }

        return preCheckState;
    }

    /**
     * Perform the import on all valid files.
     *
     * @param props           properties from the wizard panels
     * @param progressMonitor Progress monitor to update
     * @param preCheckState   pre-check state indicating the valid files to process
     * @return Import result
     * @since 2.18; parameter for wizard properties added in 3.0
     */
    protected ImportExportResult performImport(IWizardProperties props, IImportExportProgressMonitor progressMonitor, FilePreCheckState preCheckState)
    {
        final ImportExportResult result = new ImportExportResult();
        result.setSuccess(false);

        if (isCancelled()) return result; // If we've already been canceled, then bail

        if (progressMonitor != null)
        {
            progressMonitor.setProgressIndeterminate(false);
            progressMonitor.setProgress(0);

            final int count = preCheckState.tally.dataPointCount;
            progressUpdateInterval = Math.max(10, count / 100); // Roughly at 1% intervals
            progressMonitor.setMaxProgress(count);
        }

        messageCount.set(0);

        // In the case of concurrent processing, store the running list of valid files and objects
        List<File> fileList = new ArrayList<>();
        Map<String, Object> affectedObjects = new HashMap<>();
        boolean supportsConcurrent = supportsConcurrentProcessing();

        // Create all the runnables to process the given files
        for (Map.Entry<File, Map<String, Object>> entry : preCheckState.validFiles.entrySet())
        {
            final File file = entry.getKey();

            if (isValidExtension(FileUtils.getExtension(file)))
            {
                if (supportsConcurrent)
                {
                    fileList.add(file);
                    affectedObjects.putAll(entry.getValue());
                } else
                {
                    parserRunnables.add(new ParserRunnable(file, entry.getValue(), props));
                }
            }
        }

        // Create a single runnable for all files in the case of concurrent processing
        if (supportsConcurrent && !fileList.isEmpty())
        {
            parserRunnables.add(new ParserRunnable(fileList, affectedObjects, props));
        }

        // Create futures on each parser runnable, so we can await their completion
        final CompletableFuture<?>[] futures = parserRunnables.stream()
                .map(parserRunnable -> CompletableFuture.runAsync(parserRunnable, executorService))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join(); // And, we wait ...

        // All done now, so we can clear out parserRunnables
        final List<ParserRunnable> runnables = new ArrayList<>(parserRunnables);
        parserRunnables.clear();

        // Check to see if all the parsers completed successfully.
        int failures = processParserStatuses(runnables);

        result.setSuccess(failures == 0);

        final int rdmCount = messageCount.get();
        final String rdmCountStr = String.format("%,d", rdmCount);

        // Done.
        if (!isCancelled())
        {
            final int moCount = mapObjectCount.get();

            final StringBuilder sb = new StringBuilder();
            sb.append("Imported ");
            if (moCount > 0) sb.append(moCount).append(" Map Objects");
            if (moCount > 0 && rdmCount > 0) sb.append(" and ");
            if (moCount == 0 || rdmCount > 0) sb.append(rdmCountStr).append(" data points");
            if (failures > 0) sb.append(" with ").append(failures).append(" errors");

            result.setMessage(sb.toString());
            result.setMapObjectCount(moCount);
            result.setDataPointCount(rdmCount);

            if (progressMonitor != null)
            {
                progressMonitor.setProgress(rdmCount);
                progressMonitor.setComplete();
            }
        } else
        {
            result.setMessage("Imported " + rdmCountStr + " data points");
        }

        return result;
    }

    /**
     * If any invalid files were recorded in the pre-check state, then show them to the user and give them the option
     * to cancel the import. If we're running in headless mode, the dialog will be skipped and continuation of the import
     * will instead depend on the value provided by {@link #continueOnPreValidationFailure()}.
     *
     * @param preCheckState Pre-check state
     * @since 2.18
     */
    protected void reportAnyPreCheckFailures(FilePreCheckState preCheckState)
    {
        // If there were invalid files in the set, check to see if the user wants to import them.
        if (!preCheckState.invalidFiles.isEmpty())
        {
            final StringBuilder builder = new StringBuilder();

            for (Map.Entry<File, String> entry : preCheckState.invalidFiles.entrySet())
            {
                builder.append(entry.getKey().getName());
                if (entry.getValue() != null)
                {
                    builder.append(": ");
                    builder.append(entry.getValue());
                } else
                {
                    builder.append("\n");
                }
            }

            logger.warn("{}: Invalid files {}", getClass().getName(), builder);

            if (headless)
            {
                if (!continueOnPreValidationFailure())
                {
                    cancel();
                }
            } else
            {
                try
                {
                    SwingUtilities.invokeAndWait(() -> {
                        final Frame[] frames = JFrame.getFrames();
                        if (frames.length > 0) // Prevent test failures from cascading to ArrayIndexOutOfBounds here
                        {
                            ImportProblemsDialog dialog = new ImportProblemsDialog(frames[0], builder.toString());
                            dialog.setVisible(true);

                            if (!dialog.continueImport())
                            {
                                cancel();
                            }
                        }
                    });
                } catch (InterruptedException | InvocationTargetException e)
                {
                    logger.error("Failed to show user pre-check validation state", e);
                }
            }
        }
    }

    /**
     * Get the first panel, which in this default case will be the file selection dialog, UNLESS the files were
     * dropped directly on the globe, in which case no panel is necessary.  Override when a different first panel
     * is required.
     *
     * @return the string key for the first wizard panel, in this case the FileToImportPanel identifier.
     */
    @Override
    public String getFirstWizardPanel()
    {
        return getPreconfigurationSettings().get(CommonWizardSettings.FILES) != null
                ? IWizardPanelDescriptor.FINISH
                : FileToImportPanel.IDENTIFIER;
    }

    /**
     * Notify support of processing multiple files at the same time.  Override to deny support of multiple files
     *
     * @return support of multiple files.  This default implementation declares support of multiple files
     */
    @Override
    public boolean supportsMultipleFiles()
    {
        return true;
    }

    /**
     * Notify that this parser should take precedence over stock TAKX importers. That is, if this importer
     * and a stock importer both signal support, only this one will be used. Override to use the same
     * priority as the stock TAKX importers and return {@code false}
     *
     * @return {@code true} iff this importer should override TAKX stock importers, the default case here.
     */
    @Override
    public boolean overrideDefaultImporters()
    {
        return true;
    }

    /**
     * Increment the Map Object count by 1.
     *
     * @since 2.16
     */
    protected void incrementMapObjectCount()
    {
        mapObjectCount.getAndIncrement();
    }

    /**
     * Increment the Map Object count by {@code count}.
     *
     * @param count Delta by which to increment the MO count
     * @since 2.16
     */
    protected void incrementMapObjectCount(int count)
    {
        mapObjectCount.getAndAdd(count);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Our default implementation is to update the progress monitor at roughly 1% intervals (based on the initial
     * counts obtained from validating the file).  Updating every data point actually consumes a significant amount
     * of time for larger files and is not distinguishable on the progress control.
     */
    @Override
    public ImportDirective onDataMessage(IPrePersistRaptorDataMessage msg)
    {
        final int count = messageCount.incrementAndGet();

        // Setting the progress on every message slows things down considerably.
        if (progressMonitor != null && count % progressUpdateInterval == 0)
        {
            progressMonitor.setProgress(count);
        }

        return isCancelled() ? ImportDirective.ABORT : ImportDirective.CONTINUE;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Base class implementation does nothing since in general, and importer is going to behave the same no
     * matter what the format is.  Override if it is necessary to perform different logic depending on the
     * file extension / type
     */
    @Override
    public void setFormat(Format format)
    {
    }

    @Override
    public boolean validateFile(String filename, boolean simpleValidation)
    {
        ImportExportResult result = validateFileForImport(filename);
        return result.getSuccess();
    }

    /**
     * Callback requesting validation for a file.  Importer should check to see if it can process this file and
     * if so, return a result of success.  This only need be a very simple validation to make sure the file is
     * of the correct format, i.e. "Can we parse at least one record?"
     *
     * @param filename filename
     * @return result of validation
     */
    protected abstract ImportExportResult validateFileForImport(String filename);

    /**
     * Pre-process step for validating a file and the opportunity to build up a map of data object that might be
     * useful during the parse step.  Can be the same type of validation as {@link #validateFileForImport(String)}
     * if no pre-processing is needed, however, it is recommended that the number of total counts that will
     * be parsed be returned in the {@link ImportExportResult}.
     *
     * @param filename filename of file to pre-process
     * @return import result of pre-processing step.
     */
    protected abstract ImportExportResult preParseFile(String filename);

    /**
     * Callback requesting a parser be created for the given file and map.  Map is generated in the pre-process step
     * when {@link #preParseFile(String)} is run.  Plugins are required to override this method, but note that it will
     * be ignored if the wizard also overrides and returns {@code true} for
     * {@link IExtendedImportExportWizard#supportsConcurrentProcessing()}. In that case, the method
     * {@link #createConcurrentImportParser(List, Map, IWizardProperties)} would be called instead.
     *
     * @param filePath        String path to file
     * @param affectedObjects map of affected objects (created during pre-process step)
     * @param props           properties from the wizard panels (so import parsers can handle any settings from the wizard)
     * @return the parser
     * @see #createConcurrentImportParser(List, Map, IWizardProperties)
     * @since 3.0 (wizard properties parameter added)
     */
    protected abstract ICancellableImportParser createImportParser(String filePath, Map<String, Object> affectedObjects,
                                                                   IWizardProperties props);

    /**
     * Callback requesting a parser be created for the given files and map.  Plugins should override this if
     * returning {@code true} for {@link IExtendedImportExportWizard#supportsConcurrentProcessing()} and
     * {@link IExtendedImportExportWizard#supportsMultipleFiles()}. Concurrent processing assures that a plugin's
     * parser is given all the files dropped on the globe at once, instead of one at a time.  This supports the need
     * to tie together a list of files, in case the parsing of one file relies on the parsing of another.
     *
     * @param filePathList    String list of file paths
     * @param affectedObjects map of affected objects (created during pre-process step)
     * @param props           properties from the wizard panels (so import parsers can handle any settings from the wizard)
     * @return the parser
     * @see #createImportParser(String, Map, IWizardProperties)
     * @since 5.8
     */
    protected ICancellableImportParser createConcurrentImportParser(List<String> filePathList, Map<String, Object> affectedObjects,
                                                                    IWizardProperties props)
    {
        return null;    // default implementation
    }

    /**
     * Determine if the extension for a file is supported by file formats declared by this importer
     *
     * @param ext file extension
     * @return {@code true} iff extension is supported
     */
    private boolean isValidExtension(String ext)
    {
        return getFormats().stream().flatMap(format -> Arrays.stream(format.getFileTypes())).anyMatch(ext::equals);
    }

    /**
     * Check a file to see if it validates. If it does, add it to the list of valid files. If it doesn't, add
     * it to the map of invalid files along with the error message.
     *
     * @param f             The file to check.
     * @param preCheckState pre-check state container
     */
    private void preCheckFile(File f, FilePreCheckState preCheckState)
    {
        ImportExportResult result = preParseFile(f.getAbsolutePath());

        // We are called from multiple threads so synchronize on the state prior to updating it

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (preCheckState)
        {
            if (result.getSuccess())
            {
                preCheckState.tally.dataPointCount += result.getDataPointCount();
                preCheckState.validFiles.put(f, result.getAffectedObjects());
            } else
            {
                preCheckState.tally.invalidFileCount++;
                preCheckState.invalidFiles.put(f, result.getMessage());
            }
        }
    }

    /**
     * Get the result from each parser and check for errors. If there was an error, create and log an error message.
     *
     * @param runnables The list of runnables to process
     * @return The number of failures.
     */
    private int processParserStatuses(List<ParserRunnable> runnables)
    {
        int failures = 0;

        for (ParserRunnable parserRunnable : runnables)
        {
            ImportExportResult result = parserRunnable.getResult();

            if (result == null || !result.getSuccess())
            {
                failures++;

                if (reportErrorOnFailure())
                {
                    StringBuilder builder = new StringBuilder();
                    builder.append(parserRunnable.getFileName()).append(" failed to complete.");

                    if (result != null)
                    {
                        builder.append(" ");
                        builder.append(result.getMessage());
                    }

                    logger.error(builder.toString());
                }
            } else if (result.getDataPointCount() == 0 && result.getMapObjectCount() == 0)
            {
                // Output a warning if there were no valid messages in the file but the import result was successful.
                // This occurs when all records were imported successfully but were not sent out as RDM's,
                // or if no map objects were placed on the globe.
                logger.warn("No valid records found in file {}", parserRunnable.getFileName());
            }
        }

        return failures;
    }

    /**
     * Indicates if an error should be logged in this class or not. Some importers have custom error logging and can override
     * this method to return false to avoid redundant errors.
     *
     * @return True if an error should be reported
     * @since 2.31
     */
    protected boolean reportErrorOnFailure()
    {
        return true;
    }

    /**
     * Indicates if the import process should continue even if pre-validation failures occur. This is only applicable to
     * headless mode. When headless mode is off, a dialog is presented to the user instead.
     *
     * @return {@code True} if the import process should continue after pre-validation failures
     * @since 2.36.3
     */
    protected boolean continueOnPreValidationFailure()
    {
        return false;
    }

    /**
     * Runnable responsible for parsing a single data file.
     */
    private class ParserRunnable extends NameFixingRunnable
    {
        /**
         * The wizard properties.
         */
        private final IWizardProperties props;

        /**
         * The result statistics generated by the parser.
         */
        private ImportExportResult result;

        /**
         * In the case of a parser that processes all files concurrently, a list of those files.
         */
        private final List<File> fileList;

        /**
         * A reference to the parser kept so that it can be cancelled.
         */
        private ICancellableImportParser parser;

        private final Map<String, Object> affectedObjects;

        /**
         * Constructor.
         *
         * @param file            The file to process
         * @param affectedObjects map of affected objects (created during pre-process step)
         * @param props           properties from the wizard panels
         * @since 3.0 (wizard properties parameter added)
         */
        ParserRunnable(File file, Map<String, Object> affectedObjects, IWizardProperties props)
        {
            super("File Parser " + file.getName());

            this.affectedObjects = affectedObjects;
            this.props = props;
            fileList = new ArrayList<>();
            fileList.add(file);
        }

        /**
         * Constructor, overloaded to contain a list of files for concurrent processing of files chosen for import.
         *
         * @param fileList        The list of files to process
         * @param affectedObjects map of affected objects (created during pre-process step)
         * @param props           properties from the wizard panels
         * @since 5.8
         */
        ParserRunnable(List<File> fileList, Map<String, Object> affectedObjects, IWizardProperties props)
        {
            super("File Parser " + fileList.get(0).getName());

            this.affectedObjects = affectedObjects;
            this.props = props;
            this.fileList = fileList;
        }

        @Override
        public void doRun()
        {
            try
            {
                result = parseFile();
            } catch (Exception e)
            {
                List<String> filenameList = fileList.stream().map(File::getName).toList();
                logger.error("Error parsing file(s) {}", String.join(" : ", filenameList), e);
            }
        }

        /**
         * Parse the file(s).  Calls back to the concrete implementation to ask for an import parser to use.  When
         * supporting concurrent processing, a list of files will be used.  Otherwise, just as single file.
         *
         * @return the result of parsing the file(s).
         */
        private ImportExportResult parseFile()
        {
            boolean concurrent = supportsConcurrentProcessing();
            final String filePath = fileList.get(0).getAbsolutePath();

            if (concurrent)
            {
                var pathList = fileList.stream().map(File::getAbsolutePath).toList();
                parser = createConcurrentImportParser(pathList, affectedObjects, props);
            } else
            {
                parser = createImportParser(filePath, affectedObjects, props);
            }

            if (parser == null)
            {
                return concurrent ? createFailureResult("No parser created for list of files, starting with: " + filePath, null) : createFailureResult("No parser created for file: " + filePath, null);
            }

            try
            {
                parser.setMonitor(ACancellableImportWizard.this);
                return parser.parse();
            } catch (IOException e)
            {
                return createFailureResult("Unable to import file: " + filePath, e);
            }
        }

        /**
         * Create a "failure" result using the given message and log a warning.
         *
         * @param message Message to set on result
         * @param t       Exception to report along with message, may be {@code null}
         * @return New result with success set to false
         * @since 2.18
         */
        private ImportExportResult createFailureResult(String message, Throwable t)
        {
            logger.warn(message, t);

            final ImportExportResult result = new ImportExportResult();
            result.setSuccess(false);
            result.setMessage(message);
            return result;
        }

        /**
         * Cancel parsing.
         */
        private void cancel()
        {
            if (parser != null) parser.cancel();
        }

        private String getFileName()
        {
            return fileList.get(0).getName();
        }

        private ImportExportResult getResult()
        {
            return result;
        }
    }

    /**
     * Holder for data related to the "pre-check" phase of import handling.  Consumer is responsible for all
     * synchronization to safeguard concurrent use.
     *
     * @since 2.18
     */
    protected static final class FilePreCheckState
    {
        // Collection of all the files that are valid mapped to the "affected objects" recorded by the parser
        final Map<File, Map<String, Object>> validFiles = new HashMap<>();

        // Collection of files the failed validation mapped to error message
        final Map<File, String> invalidFiles = new HashMap<>();

        // Accumulated import data counts
        final Tally tally = new Tally();
    }

    /**
     * Simple class used to tally the number of valid and invalid (un-parsable) entries.
     */
    protected static final class Tally
    {
        int dataPointCount;
        int invalidFileCount;
    }
}
