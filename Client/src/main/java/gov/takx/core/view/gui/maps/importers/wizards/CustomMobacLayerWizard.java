package gov.takx.core.view.gui.maps.importers.wizards;

import gov.tak.platform.datapackage.ZipReader;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.layer.IMobacLayerManager;
import gov.takx.commons.file.FileUtils;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Import an XML definition of a custom MOBAC map source, creating a layer from it.
 *
 * @since 3.0
 */
@PseudoBean
public class CustomMobacLayerWizard extends AFileImportWizard
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String TYPE = "Custom MOBAC layer Importer";
    private static final URL ICON_URL = UIResources.getUrl(PLATFORM_ICONS, "imagery_server.png");
    private static final Pattern NAME_XML_PATTERN = Pattern.compile("\\<name\\>([\\S\\s]+)\\<\\/name\\>");

    @Inject
    private IMobacLayerManager mobacLayerManager;

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public String getDisplayName()
    {
        return "Custom Tile Layer";
    }

    @Override
    public String getHelpMessage()
    {
        return "Imports Custom MOBAC Layer definitions and adds them to the globe.";
    }

    @Override
    public URL getIcon()
    {
        return ICON_URL;
    }

    @Override
    @Nullable
    public Map<String, Object> showImporterWizardDialog()
    {
        return null;
    }

    @Override
    public boolean supportsMultipleFiles()
    {
        return true;
    }

    @Override
    public boolean offerToProcessFile(String fileExtension, File file)
    {
        // We support:
        // - zipped archive of mobac layers
        // - directory of mobac layers
        // - single file of a mobac layer
        return isMapSourcesZip(file) || !findMapSources(file, true).isEmpty();
    }

    @Override
    public void runOperation(@Nullable Map<String, Object> configuration)
    {
        files.stream()
                .flatMap(file -> {
                    // zip -> self
                    // file/directory -> path walk
                    return "zip".equals(FileUtils.getExtension(file)) ? Stream.of(file) : findMapSources(file, false).stream();
                })
                .forEach(file -> {
                    if ("zip".equals(FileUtils.getExtension(file)))
                    {
                        importMapSourceZip(file);
                    } else
                    {
                        mobacLayerManager.makeLayerFromFile(file);
                    }
                });
    }

    /**
     * Find potential map sources starting at the given file, which may be a directory.
     *
     * @param file  Starting file/directory
     * @param first Pass {@code true} to return the first map source found, or {@code false} to collect all sources
     * @return List of map source XML files, may be empty
     */
    private List<File> findMapSources(File file, boolean first)
    {
        // Walk the directory tree and collect any map source XML config files
        try (Stream<Path> paths = Files.walk(file.toPath()))
        {
            return paths.filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(mobacLayerManager::isMapSourceXml)
                    .limit(first ? 1 : Integer.MAX_VALUE)
                    .collect(Collectors.toList());
        } catch (IOException e)
        {
            logger.error("Failed to scan input file(s) from: {}", file, e);
            return Collections.emptyList();
        }
    }

    /**
     * @param zip Zip to read from.
     * @return Predicate matching mobac layer xml files.
     * @since 5.7
     */
    @Nonnull
    private Predicate<ZipEntry> makeSourcePredicate(ZipReader zip)
    {
        return e -> {
            String name = e.getName();
            if (name.endsWith(".xml"))
            {
                String content = zip.readEntryAsString(name);
                return mobacLayerManager.isMapSourceXml(content);
            }
            return false;
        };
    }

    /**
     * @param file File to check.
     * @return {@code true} when it is a ZIP file containing at least one valid mobac map source xml file.
     * @since 5.7
     */
    private boolean isMapSourcesZip(@Nonnull File file)
    {
        String fileExtension = FileUtils.getExtension(file);
        if ("zip".equals(fileExtension))
        {
            try (ZipReader zip = new ZipReader(file))
            {
                return !zip.findEntries(makeSourcePredicate(zip)).isEmpty();
            } catch (IOException e)
            {
                logger.warn("File '{}' with 'zip' extension was not a zip?", file.getName(), e);
                return false;
            }
        }
        return false;
    }

    /**
     * @param file File to import, assumed to have passed {@link #isMapSourcesZip(File)}
     * @since 5.7
     */
    private void importMapSourceZip(@Nonnull File file)
    {
        try (ZipReader zip = new ZipReader(file))
        {
            zip.findEntries(makeSourcePredicate(zip)).forEach(entry -> {
                // Used for layer name fallback:
                //  some/dir/filename.xml --> filename
                String entryFileName = entry.contains("/") ? entry.substring(entry.lastIndexOf('/') + 1) : entry;
                String layerXml = zip.readEntryAsString(entry);

                // Extract layer name from XML, falling back to file name if not found
                Matcher nameMatcher = NAME_XML_PATTERN.matcher(layerXml);
                String layerName = nameMatcher.find() ? nameMatcher.group(1) : FileUtils.getNameWithoutExtension(entryFileName);

                mobacLayerManager.makeLayerFromConfigXml(layerName, layerXml);
            });
        } catch (IOException e)
        {
            // Should not happen since we previously passed 'isMapSourcesZip'
            logger.error("Assumed map-sources zip '{}' was not a zip?", file.getName(), e);
        }
    }
}
