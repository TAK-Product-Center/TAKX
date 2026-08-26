package gov.takx.core.toolbar;

import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages writing/reading toolbar configurations to/from files.
 *
 * @since 2.0 (Renamed in 3.4)
 */
@ApplicationScoped
public class ToolbarFileManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // The directory where configuration files will be stored (relative to the user VFS directory)
    private static final String CONFIGURATION_DIRECTORY = "ToolbarConfigurations";

    // The file extension to use when saving a toolbar configuration to a file
    private static final String CONFIGURATION_FILE_EXTENSION = ".xml";

    // Map of configuration names to the file path to which the configuration was saved. This is used to support
    // saving an edited configuration back to the same file to avoid a proliferation of files due to multiple edits of
    // a configuration.
    private final Map<String, Path> configurationFilePathMap = new ConcurrentHashMap<>();

    @Inject
    @VFSManagerScope(VFSScope.Global)
    private IVirtualFileSystemManager globalVFSManager;

    private IVirtualFileSystem globalVFS;

    @Inject
    @ProgramPreference(PreferenceEntry.TOOLBAR_DISPLAY_DEFAULT_CONFIGURATION)
    private boolean displayDefaultConfiguration;

    @PostConstruct
    private void postConstruct()
    {
        globalVFS = globalVFSManager.getVirtualFileSystem(CONFIGURATION_DIRECTORY);

        try
        {
            // Create the directory for storing configurations if it doesn't exist
            Path configurationDirectoryPath = Paths.get(globalVFS.getRootDir().toString());
            Files.createDirectories(configurationDirectoryPath);
        } catch (IOException e)
        {
            logger.error("Creation of configurations directory failed", e);
        }
    }

    /**
     * Saves the provided configuration to a file.
     *
     * @param oldConfigurationName The old name of the configuration (if it is an existing configuration)
     * @param configuration The configuration to save
     */
    public void saveConfiguration(@Nullable String oldConfigurationName, @Nonnull ToolbarConfiguration configuration)
    {
        if (oldConfigurationName == null)
        {
            // If this is a new configuration, determine the filename to be used
            String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss").format(LocalDateTime.now());
            Path filePath = Paths.get(globalVFS.getRootDir().toString(), timestamp + CONFIGURATION_FILE_EXTENSION);
            configurationFilePathMap.put(configuration.getName(), filePath);
        } else if (!oldConfigurationName.equals(configuration.getName()))
        {
            // If the configuration name changed, update the file path map accordingly
            Path filePath = configurationFilePathMap.remove(oldConfigurationName);
            configurationFilePathMap.put(configuration.getName(), filePath);
        }

        writeConfigurationToFile(configurationFilePathMap.get(configuration.getName()).toString(), configuration);
    }

    /**
     * Loads all configuration files present in the configuration directory.
     *
     * @return The configurations read in from the files
     */
    @Nonnull
    public List<ToolbarConfiguration> loadConfigurations()
    {
        List<ToolbarConfiguration> configurationList = new ArrayList<>();

        // Retrieve configuration files and convert to configurations
        Path configurationDirectoryPath = Paths.get(globalVFS.getRootDir().toString());

        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(configurationDirectoryPath,
                "*" + CONFIGURATION_FILE_EXTENSION))
        {
            ToolbarConfiguration configuration;
            for (Path filePath : dirStream)
            {
                try
                {
                    configuration = readConfigurationFromFile(filePath.toString());

                    if (configuration != null)
                    {
                        // Only add configurations whose name does not already exist
                        if (configurationFilePathMap.containsKey(configuration.getName()))
                        {
                            logger.warn("A toolbar configuration with the name '{}' already exists. " +
                                    "Ignoring the configuration found in {}", configuration.getName(), filePath);
                        }

                        configurationFilePathMap.put(configuration.getName(), filePath);
                        configurationList.add(configuration);
                    }
                } catch (IllegalArgumentException e)
                {
                    logger.warn("Error reading configuration from file {}; ignoring configuration", filePath, e);
                }
            }
        } catch (IOException e)
        {
            logger.error("Error reading in configuration files", e);
        }

        return configurationList;
    }

    /**
     * Deletes the file containing the provided configuration.
     *
     * @param configuration The configuration to delete
     */
    public void deleteConfiguration(@Nonnull ToolbarConfiguration configuration)
    {
        final String configurationName = configuration.getName();
        final Path filePath = configurationFilePathMap.remove(configurationName);
        if (filePath != null)
        {
            try
            {
                Files.deleteIfExists(filePath);
            } catch (IOException e)
            {
                logger.error("Error deleting file for configuration {}. File: {}", configurationName, filePath, e);
            }
        }
    }

    /**
     * Reads a ToolbarConfiguration object from an XML file.
     *
     * @param fileName The name of the file from which to read
     * @return The ToolbarConfiguration read from the file, or {@code null} if the read failed.
     * @throws IllegalArgumentException if the provided file contains a configuration version that is not supported.
     */
    @Nullable
    public ToolbarConfiguration readConfigurationFromFile(@Nonnull String fileName) throws IllegalArgumentException
    {
        ToolbarConfiguration configuration = null;

        try
        {
            JAXBContext jaxbContext = JAXBContext.newInstance(ToolbarConfiguration.class);
            configuration = (ToolbarConfiguration) jaxbContext.createUnmarshaller().unmarshal(new File(fileName));

            final String version = configuration.getVersion();
            final String supportedVersion = ToolbarConfiguration.CURRENT_CONFIGURATION_VERSION;
            if (!version.equals(supportedVersion))
            {
                throw new IllegalArgumentException("Restored configuration version (" + version +
                        ") does not match current supported version for configuration files (" + supportedVersion + ").");
            }
        } catch (JAXBException e)
        {
            logger.error("Read from file failed. File name: {}", fileName, e);
        }

        return configuration;
    }

    /**
     * Writes the provided configuration to an XML file.
     *
     * @param fileName The name of the file to which to write
     * @param configuration The configuration to write to the file
     * @return True if the file was written successfully
     */
    public boolean writeConfigurationToFile(@Nonnull String fileName, @Nonnull ToolbarConfiguration configuration)
    {
        try
        {
            JAXBContext jaxbContext = JAXBContext.newInstance(ToolbarConfiguration.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            marshaller.marshal(configuration, new File(fileName));

            return true;
        } catch (JAXBException e)
        {
            logger.error("Write to file failed. File name: {}", fileName, e);
        }

        return false;
    }

    /**
     * Loads the name of the active configuration specified in the preferences. If one is not specified, the name of
     * the default TAKX configuration is returned.
     *
     * @return The name of the configuration to activate
     */
    @Nonnull
    public String loadActiveConfigurationFromPreferences()
    {
        return PreferenceEntry.TOOLBAR_ACTIVE_CONFIGURATION.getStringValue();
    }

    /**
     * Stores the provided configuration name to the preferences as the active configuration.
     *
     * @param configurationName The name of the configuration
     */
    public void storeActiveConfigurationToPreferences(@Nonnull String configurationName)
    {
        PreferenceEntry.TOOLBAR_ACTIVE_CONFIGURATION.putAndSave(configurationName);
    }

    /**
     * Determines if the default configuration should be displayed based on the preferences. If not specified, true is
     * returned.
     *
     * @return True if the default configuration should be displayed.
     */
    public boolean shouldDefaultConfigurationBeDisplayed()
    {
        return displayDefaultConfiguration;
    }
}
