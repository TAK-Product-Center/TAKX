package gov.takx.api.plugin.io;

import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.io.Format;
import gov.takx.api.io.ImportExportResult;
import gov.takx.commons.wizard.IWizardProperties;
import gov.takx.core.view.gui.wizard.WizardProperties;
import gov.takx.platform.io.panels.CommonWizardSettings;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the {@link ACancellableImportWizard} abstract class. Uses a minimal implementation of that class to
 * test that the {@link ACancellableImportWizard#continueOnPreValidationFailure()} override works as expected, and indirectly
 * validates that headless mode is appropriately taken into account.
 *
 * @since 2.36.3
 */
public class ACancellableImportWizardTest
{
    /**
     * Tests that the {@link ACancellableImportWizard#cancel()} method is called when configured to do so. Indirectly tests
     * that the Swing dialog is not shown in headless mode - if a dialog is shown, cancel will never be called and the
     * test will timeout and fail. Headless mode is configured for the overall test suite.
     * <p>
     * While not entirely necessary for the implementation as it's currently written, the timeout serves as a backstop
     * in case the implementation changes and the dialog is displayed during a test run.
     *
     * @throws IOException if a temporary file cannot be created while building pre-check state
     */
    @Test
    @SuppressLogging(ACancellableImportWizard.class)
    // the reportAnyPreCheckFailures method emits an expected warning log entry
    public void testImportCancelledOnPreCheckFailureInHeadlessMode() throws IOException
    {
        TestableCancellableImportWizard objectUnderTest = new TestableCancellableImportWizard(false);
        objectUnderTest.reportAnyPreCheckFailures(buildFailedFilePreCheckState());

        assertThat(objectUnderTest.getCancelInvocations()).withFailMessage("cancel() should have been called once.").isEqualTo(1);
    }

    /**
     * Tests that the {@link ACancellableImportWizard#cancel()} method is not called when thus configured. Indirectly tests
     * that the Swing dialog is not shown in headless mode - if a dialog is shown, cancel will never be called and the
     * test will timeout and fail. Headless mode is configured for the overall test suite.
     * <p>
     * While not entirely necessary for the implementation as it's currently written, the timeout serves as a backstop
     * in case the implementation changes and the dialog is displayed during a test run.
     *
     * @throws IOException if a temporary file cannot be created while building pre-check state
     */
    @Test
    @SuppressLogging(ACancellableImportWizard.class)
    // the reportAnyPreCheckFailures method emits an expected warning log entry
    public void testImportNotCancelledOnPreCheckFailureInHeadlessModeAndContinueIsTrue() throws IOException
    {
        TestableCancellableImportWizard objectUnderTest = new TestableCancellableImportWizard(true);
        objectUnderTest.reportAnyPreCheckFailures(buildFailedFilePreCheckState());

        assertThat(objectUnderTest.getCancelInvocations()).withFailMessage("cancel() should not have been called.").isEqualTo(0);
    }

    /**
     * Verify that an import wizard that supports concurrent file processing (all files are handed off at the same
     * time and executed using a single ParserRunnable) will receive all files at once when overriding
     * {@link ACancellableImportWizard#createConcurrentImportParser}.
     *
     * @throws Exception exception creating a temp file or performing the import
     * @since 5.8
     */
    @Test
    @SuppressLogging(ACancellableImportWizard.class)
    public void concurrentWizard_multipleFiles_filesAllProcessedAtOnce() throws Exception
    {
        TestableConcurrentCancellableImportWizard objectUnderTest = new TestableConcurrentCancellableImportWizard();

        Map<String, Object> propertyMap = new HashMap<>();
        File tempFile1 = File.createTempFile("ACancellableImportWizardTest-temp1.tak", null);
        File tempFile2 = File.createTempFile("ACancellableImportWizardTest-temp2.tak", null);
        tempFile1.deleteOnExit();
        tempFile2.deleteOnExit();
        Format format = new Format("test", "test", "test", "tmp");
        objectUnderTest.setFormats(List.of(format));
        var fileList = Arrays.asList(tempFile1, tempFile2);
        WizardProperties properties = new WizardProperties(propertyMap);
        properties.setProperty(CommonWizardSettings.FILES, fileList);
        objectUnderTest.doImport(properties, null);

        assertThat(objectUnderTest.isConcurrentParserCreated()).isTrue();
        assertThat(objectUnderTest.getNumberOfConcurrentFiles()).isEqualTo(2);
    }

    /**
     * @return A {@link ACancellableImportWizard.FilePreCheckState} object with one invalid file entry.
     * @throws IOException if a temporary file cannot be created
     */
    private ACancellableImportWizard.FilePreCheckState buildFailedFilePreCheckState() throws IOException
    {
        File tempFile = File.createTempFile("ACancellableImportWizardTest-temp", null);
        tempFile.deleteOnExit();

        ACancellableImportWizard.FilePreCheckState state = new ACancellableImportWizard.FilePreCheckState();
        state.invalidFiles.put(tempFile, "foo");

        return state;
    }

    /**
     * Minimal testable implementation of {@link ACancellableImportWizard}. Overrides {@link #continueOnPreValidationFailure()}
     * so that we can test different configurations, and provides a {@link #getCancelInvocations()} method that allows
     * tests to monitor how many times the {@link #cancel()} method is called.
     */
    private static class TestableCancellableImportWizard extends ACancellableImportWizard
    {
        private final boolean continueOnPreCheckFailure;

        private int cancelInvocations = 0;

        /**
         * Initializes a new {@link TestableCancellableImportWizard} instance a configuration value for the
         * {@link #continueOnPreValidationFailure()} override.
         *
         * @param continueOnPreCheckFailure The value to return for the {@link #continueOnPreValidationFailure()} override.
         */
        public TestableCancellableImportWizard(boolean continueOnPreCheckFailure)
        {
            this.continueOnPreCheckFailure = continueOnPreCheckFailure;
        }

        @Override
        protected boolean continueOnPreValidationFailure()
        {
            return continueOnPreCheckFailure;
        }

        @Override
        public void cancel()
        {
            cancelInvocations++;
        }

        /**
         * @return The number of times {@link #cancel()} was invoked.
         */
        public int getCancelInvocations()
        {
            return cancelInvocations;
        }

        @Override
        protected ImportExportResult validateFileForImport(String filename)
        {
            return null;
        }

        @Override
        protected ImportExportResult preParseFile(String filename)
        {
            return null;
        }

        @Override
        protected ICancellableImportParser createImportParser(String filePath, Map<String, Object> affectedObjects, IWizardProperties props)
        {
            return null;
        }
    }

    /**
     * Test wizard that declares it supports concurrent parsing in
     * {@link IExtendedImportExportWizard#supportsConcurrentProcessing()}.
     * Overrides {@link ACancellableImportWizard#createConcurrentImportParser(List, Map, IWizardProperties)} to test
     * that multiple files are passed in, giving the import parser a chance to parse all the files simultaneously.
     *
     * @since 5.8
     */
    private static class TestableConcurrentCancellableImportWizard extends TestableCancellableImportWizard
    {
        private boolean concurrentParserCreated = false;
        private int numberOfConcurrentFiles = 0;

        /**
         * Initializes a new {@link TestableCancellableImportWizard} instance a configuration value for the
         * {@link #continueOnPreValidationFailure()} override.
         */
        public TestableConcurrentCancellableImportWizard()
        {
            super(true);
        }

        @Override
        public boolean supportsConcurrentProcessing()
        {
            return true;
        }

        @Override
        protected ICancellableImportParser createConcurrentImportParser(List<String> filePathList, Map<String, Object> affectedObjects, IWizardProperties props)
        {
            concurrentParserCreated = true;
            numberOfConcurrentFiles = filePathList.size();
            return super.createConcurrentImportParser(filePathList, affectedObjects, props);
        }

        @Override
        protected ImportExportResult validateFileForImport(String filename)
        {
            ImportExportResult result = new ImportExportResult();
            result.setSuccess(true);
            return result;
        }

        @Override
        protected ImportExportResult preParseFile(String filename)
        {
            ImportExportResult result = new ImportExportResult();
            result.setSuccess(true);
            return result;
        }

        /**
         * @return {@code True} if the method to create the concurrent parser was called
         */
        private boolean isConcurrentParserCreated()
        {
            return concurrentParserCreated;
        }

        /**
         * @return the number of files handed off to the concurrent parser
         */
        private int getNumberOfConcurrentFiles()
        {
            return numberOfConcurrentFiles;
        }
    }
}
