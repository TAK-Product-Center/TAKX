package gov.takx.core.commservices;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.plugin.commservices.ICommListener;
import gov.takx.api.plugin.commservices.ICommPath;
import gov.takx.api.project.IProjectManager;
import gov.takx.commons.constants.CommServiceType;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Provides a file logging service for all registered ICommManagers instances when
 * EnableCommLogging is set to true in ProgramProperties.xml.  Developers using
 * this experimental service are strongly encouraged to follow this procedure:
 * <p>
 * <ol>
 * <li>Edit ProgramProperties.xml by adding an entry tag under program with key="EnableCommLogging" value="true"</li>
 * <li>Launch TAKX and create a new project.</li>
 * <li>Connect your device or simulator and run your tests.</li>
 * <li>Exit TAKX.</li>
 * <li>Disable the key="EnableCommLogging" entry.</li>
 * <li>Launch TAKX and create a new project.</li>
 * <li>Using the File Connection, connect to the commOut entry under the previous project's session.</li>
 * <li>Enjoy the replay!</li>
 * </ol>
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class FileLoggerListener implements ICommListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IProjectManager projectManager;

    private final CommServiceType commType;
    private final String commName;
    private File logFile;
    private FileOutputStream dos;
    private boolean isOpen = false;

    public FileLoggerListener(CommServiceType commType, String commName)
    {
        this.commType = commType;
        this.commName = commName;
    }

    @PostConstruct
    private void postConstruct()
    {
        String projectDirectoryPath = projectManager.getProjectDirectory();

        File parentDirectory = new File(projectDirectoryPath, "commOut");

        if (!parentDirectory.isDirectory() || !parentDirectory.exists())
        {
            parentDirectory.mkdir();
        }

        String fileName = makeSafeName(commType, commName);

        logFile = new File(parentDirectory, fileName + ".bytes");

        if (logger.isDebugEnabled())
        {
            logger.debug("Logging {} / {} to log file {}", commType, commName, logFile.getAbsolutePath());
        }

        openLogFile();
    }

    private String makeSafeName(CommServiceType commType, String commName)
    {
        String combination = commType + "-" + commName;
        combination = combination.replace('\\', '_');
        combination = combination.replace('.', '_');
        combination = combination.replace(':', '_');

        return combination;
    }

    private void openLogFile()
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Opening log file {}", logFile.getAbsolutePath());
        }

        try
        {
            if (!logFile.exists())
            {
                logFile.createNewFile();
            }

            dos = new FileOutputStream(logFile, true);

            isOpen = true;
        } catch (IOException e)
        {
            logger.warn("IOException: ", e);
        }
    }

    private void closeLogFile()
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Closing log file {}", logFile.getAbsolutePath());
        }

        try
        {
            if (null != dos)
            {
                dos.flush();
                dos.close();

                if (logger.isDebugEnabled())
                {
                    logger.debug("Log file closed properly");
                }
            } else
            {
                logger.warn("DataOutputStream was null!");
            }
        } catch (IOException e)
        {
            logger.warn("IOException: ", e);
        }

        isOpen = false;
    }

    @Override
    public void parseBytes(byte[] data, int length, String source)
    {
        if (!isOpen)
        {
            openLogFile();
        }

        try
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Sending {} bytes to log file from byte[] size {}", length, data.length);
            }

            if (null != dos)
            {
                dos.write(data, 0, length);
                dos.flush();
            } else
            {
                logger.error("DataOutputStream is null - loggin skipped");
            }
        } catch (IOException e)
        {
            logger.warn("IOException ", e);
        }
    }

    @Override
    public void commPathClosed(ICommPath commPath, boolean wasExpected)
    {
        closeLogFile();
    }

    @Override
    public void commPathStopped(ICommPath commPath, boolean wasExpected)
    {
        closeLogFile();
    }
}
