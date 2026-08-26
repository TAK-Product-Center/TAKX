package gov.takx.test.mock.plugin.io;

import gov.takx.api.io.ImportExportResult;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.plugin.annotations.DataStructureFactory;
import gov.takx.api.plugin.device.IDataStructureFactory;
import gov.takx.api.plugin.io.ICancellableImportParser;
import gov.takx.api.plugin.io.IImportMonitor;
import gov.takx.commons.wizard.IWizardProperties;
import gov.takx.test.mock.plugin.device.TestDevicePluginWithOptions;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 * Test importer/wizard for handling files of type "fakefile".
 *
 * @since 2.30
 */
@Dependent
public class FakeImportWizard extends TestCancellableImportWizard
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IDataStructureFactory dataStructureFactory;

    @Inject
    FakeImportWizard(@DataStructureFactory(forPlugin = TestDevicePluginWithOptions.class) IDataStructureFactory dataStructureFactory)
    {
        this.dataStructureFactory = dataStructureFactory;
    }

    @Override
    protected ICancellableImportParser createImportParser(String filePath, Map<String, Object> affectedObjects, IWizardProperties props)
    {
        return new FakeFileParser(filePath);
    }

    @Override
    public boolean offerToProcessFile(String fileExtension, File file)
    {
        return fileExtension.equals(FakeImportExportPlugin.FILE_EXTENSION);
    }

    /**
     * Parser to handle our "fake" file format.
     */
    private class FakeFileParser implements ICancellableImportParser
    {
        private final File fileToImport;

        public FakeFileParser(String fileToImport)
        {
            this.fileToImport = new File(fileToImport);
        }

        @Override
        public ImportExportResult parse()
        {
            final ImportExportResult result = new ImportExportResult();
            result.setSuccess(true);

            try (BufferedReader reader = new BufferedReader(new FileReader(fileToImport)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    try
                    {
                        parseLineAndSendRdm(line);
                        result.incrementDataPointCount();
                    } catch (Exception e)
                    {
                        logger.warn("Failed parsing data line: {}", line, e);
                    }
                }
            } catch (IOException e)
            {
                final String message = "Failed reading file " + fileToImport;
                logger.error(message, e);
                result.setMessage(message + ". " + e.getMessage());
                result.setSuccess(false);
            }

            return result;
        }

        private void parseLineAndSendRdm(String line)
        {
            final IRaptorDataStructure messageType0 = dataStructureFactory.getDataStructure(TestDevicePluginWithOptions.MESSAGE_TYPE_0);
            final IPrePersistRaptorDataMessage rdm = messageType0.createRaptorDataMessage("FakeUnitId");

            final String[] lineParts = line.split(",");

            rdm.setLocation(Double.parseDouble(lineParts[0]), Double.parseDouble(lineParts[1]), Double.parseDouble(lineParts[2]));
            rdm.setDouble("name0", Double.parseDouble(lineParts[3]));

            delegate.sendRaptorDataMessage(rdm);
        }

        @Override
        public void cancel()
        {
        }

        @Override
        public void setMonitor(IImportMonitor monitor)
        {
        }
    }
}
