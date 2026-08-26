package gov.tak.core.icons;

import com.atakmap.filesystem.HashingUtils;
import gov.tak.api.icons.IIconSetManager;
import gov.tak.api.icons.IUserIcon;
import gov.tak.api.icons.IUserIconSet;
import gov.tak.platform.datapackage.ZipReader;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.milsym.CotSidcAdapter;
import gov.takx.api.milsym.IMilStd2525Service;
import gov.takx.api.milsym.IMilStd2525ServiceProvider;
import gov.takx.api.plugin.synthetic.ISyntheticPluginDefinition;
import gov.takx.api.pointdropper.CotIcon;
import gov.takx.api.registry.ISyntheticPluginRegistry;
import gov.takx.api.url.Schemes;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.plugin.synthetic.SyntheticPluginDefinition;
import gov.takx.module.PluginDefinitionFactory;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import static gov.takx.api.mapobject.MapObjectOptions.STANDARD_RESOURCE_OPTIONS;
import static gov.takx.api.milsym.MilStd2525Variant.CHARLIE;
import static gov.takx.commons.swing.ImageUtils.DEFAULT_ICON_WIDTH;
import static gov.takx.core.project.ServiceOrdering.START_MapObjectManager;
import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;

/**
 * Service managing custom Icon Sets.
 *
 * @since 3.1
 */
@ApplicationScoped
public class IconSetManager implements IIconSetManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Path ASSETS_DIR = Paths.get("resources", "tak-assets", "iconsets");
    private static final Set<String> ACCEPTED_IMAGE_TYPES = Set.of("jpg", "jpeg", "png", "bmp", "gif");
    private static final String ICONSET_XML = "iconset.xml";

    private static byte[] DEFAULT_IMAGE_BYTES;
    private static JAXBContext jaxbContext = null;

    static
    {
        try
        {
            jaxbContext = JAXBContext.newInstance(UserIconSet.class);
        } catch (JAXBException e)
        {
            logger.error("Failed to create JAXBContext", e);
        }

        try
        {
            // Obtain the bytes for our default icon
            final BufferedImage image = ImageUtils.getScaledInstance(ImageUtils.getDefaultImage(), DEFAULT_ICON_WIDTH);

            final var buffer = new ByteArrayOutputStream();
            ImageIO.write(image, "png", buffer);

            DEFAULT_IMAGE_BYTES = buffer.toByteArray();

        } catch (IOException e)
        {
            DEFAULT_IMAGE_BYTES = new byte[0];
        }
    }

    // Icon sets keyed by UID
    private final Map<String, UserIconSet> iconSetsByUid = new HashMap<>();

    // Cache from UserIcon to it's image
    private final Map<IUserIcon, BufferedImage> iconImageCache = new HashMap<>();

    // CoT plugin definitions by CoT type
    private final Map<String, ISyntheticPluginDefinition> definitionsByCotType = new HashMap<>();

    private final ISyntheticPluginRegistry syntheticPluginRegistry;
    private final PluginDefinitionFactory pluginDefinitionFactory;
    private final IMilStd2525Service milStd2525cService;

    @Inject
    IconSetManager(ISyntheticPluginRegistry syntheticPluginRegistry, PluginDefinitionFactory pluginDefinitionFactory, IMilStd2525ServiceProvider milStd2525ServiceProvider)
    {
        this.syntheticPluginRegistry = syntheticPluginRegistry;
        this.pluginDefinitionFactory = pluginDefinitionFactory;
        this.milStd2525cService = milStd2525ServiceProvider.get2525Service(CHARLIE);
    }

    /**
     * Start our service.  Load all the standard icon sets.
     * <p/>
     * Note: we must start before the MapObjectManager since these assets are used when restoring MOs.
     */
    private void startService(@Observes @Priority(START_MapObjectManager - 1) StartProjectServices event)
    {
        loadIconSets();
    }

    /**
     * Stop and cleanup.
     */
    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices event)
    {
        iconSetsByUid.clear();
        iconImageCache.clear();
    }

    @Nonnull
    @Override
    public List<IUserIconSet> getAllIconSets()
    {
        return new ArrayList<>(ensureLoaded().values());
    }

    @Nullable
    @Override
    public IUserIconSet getIconSetByUid(@Nonnull String uid)
    {
        return ensureLoaded().get(uid);
    }

    @Nullable
    @Override
    public IUserIconSet getIconSetByName(@Nonnull String name)
    {
        return ensureLoaded().values().stream()
                .filter(iconSet -> iconSet.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    @Override
    public IUserIcon getIcon(@Nonnull String iconSetUid, @Nonnull String iconName)
    {
        final UserIconSet iconSet = ensureLoaded().get(iconSetUid);

        if (iconSet == null)
        {
            logger.warn("Unknown Icon Set {} for icon {}", iconSetUid, iconName);
            return null;
        }
        return iconSet.getIcon(iconName);
    }

    @Nullable
    @Override
    public IUserIcon getIcon(URI uri)
    {
        if (!isIconSetUri(uri)) throw new IllegalArgumentException("Not an icon set URI: " + uri);

        return getIconFromPath(uri.getSchemeSpecificPart());
    }

    @Nullable
    @Override
    public IUserIcon getIconFromPath(@Nonnull String iconSetPath)
    {
        final Path path = Paths.get(iconSetPath);
        final int nameCount = path.getNameCount();

        if (nameCount < 2 || nameCount > 3) return null; // Not a valid path

        final String uid = path.getName(0).toString();
        final String iconName = path.getName(nameCount - 1).toString();

        return getIcon(uid, iconName);
    }

    @Nonnull
    @Override
    public BufferedImage getIconImage(@Nonnull IUserIcon icon)
    {
        return iconImageCache.computeIfAbsent(icon, k -> loadIconImage(icon));
    }

    @Override
    @Nullable
    public BufferedImage getIconImage(@Nonnull String iconSetUid, @Nonnull String iconName)
    {
        final var icon = getIcon(iconSetUid, iconName);
        return icon != null ? getIconImage(icon) : null;
    }

    @Override
    @Nonnull
    public synchronized ISyntheticPluginDefinition getOrCreatePluginDefinition(@Nonnull IUserIcon userIcon)
    {
        final String family = KeyUtils.RESOURCE_ICON_FAMILY + "_" + userIcon.getIconsetUid();
        final String type = FileUtils.getNameWithoutExtension(userIcon.getFileName());
        final String group = userIcon.getGroup();
        final var definition = syntheticPluginRegistry.getPluginDefinition(family, type);

        return definition.orElseGet(() -> {
            logger.debug("create SPD in group {} using URI {}", group, userIcon.toUri());
            final var pluginDefinition = pluginDefinitionFactory.syntheticDefinitionFor(
                    family, type, group, userIcon.toUri(), STANDARD_RESOURCE_OPTIONS, null);
            syntheticPluginRegistry.addPlugin(pluginDefinition);
            return pluginDefinition;
        });
    }

    @Nonnull
    @Override
    public ISyntheticPluginDefinition getOrCreateCotDefinition(@Nonnull String cotType)
    {
        // TODO (TAKX-3244): May want to change how these are created if it is not strictly
        //  handled as an edge case in the UI.

        // Generate synthetic plugins associated with the CoT type that will generate icons representing the CoT item.
        // This supports any CoT value that can be mapped to a supported SIDC supported by the MilStd2525 service.
        return definitionsByCotType.computeIfAbsent(cotType, t -> new SyntheticPluginDefinition(KeyUtils.RESOURCE_ICON_FAMILY, CotIcon.TYPE, t, null, null, STANDARD_RESOURCE_OPTIONS)
        {
            private BufferedImage image;

            @Override
            public synchronized BufferedImage getIcon()
            {
                if (image == null)
                {
                    String symbolCode = CotSidcAdapter.mil2525CFromCotType(cotType);
                    image = symbolCode == null ? ImageUtils.getDefaultImage() : milStd2525cService.renderSinglePointIcon(symbolCode);
                }
                return image;
            }
        });
    }

    @Override
    public boolean isIconSetUri(@Nullable URI uri)
    {
        if (uri == null || !Schemes.ICONSET.equals(uri.getScheme())) return false;

        final int nameCount = Paths.get(uri.getSchemeSpecificPart()).getNameCount();
        return nameCount == 2 || nameCount == 3; // The group portion is optional
    }

    @Nonnull
    @Override
    public ISyntheticPluginDefinition getOrCreatePluginDefinition(@Nonnull URI uri) throws IllegalArgumentException
    {
        final IUserIcon userIcon = getIcon(uri);

        if (userIcon == null) throw new IllegalArgumentException("Invalid URI path; Unknown icon: " + uri);

        return getOrCreatePluginDefinition(userIcon);
    }

    private synchronized Map<String, UserIconSet> ensureLoaded()
    {
        if (iconSetsByUid.isEmpty()) loadIconSets();
        return iconSetsByUid;
    }

    /**
     * Load the image for the given icon.
     */
    @Nonnull
    private BufferedImage loadIconImage(IUserIcon icon)
    {
        if (!icon.isValid()) return ImageUtils.getDefaultImage();

        try
        {
            return ImageIO.read(new ByteArrayInputStream(icon.getImageBytes()));
        } catch (IOException e)
        {
            return ImageUtils.getDefaultImage();
        }
    }

    /**
     * Scan the tak-assets for icon sets, and load them.
     */
    private void loadIconSets()
    {
        if (!iconSetsByUid.isEmpty()) return; // Already initialized

        // The ASSETS_DIR directory will not be present during plugin integration tests
        if (!ASSETS_DIR.toFile().exists())
        {
            logger.debug("Assets dir is missing: {}", ASSETS_DIR);
            return;
        }

        try (Stream<Path> stream = Stream.concat(Files.list(ASSETS_DIR), Stream.of(Paths.get("resources", "TAKX_legacy.zip"))))
        {
            stream.filter(path -> !Files.isDirectory(path))
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".zip"))
                    .forEach(this::loadIconSet);

        } catch (IOException e)
        {
            logger.warn("Failed to scan assets in {}", ASSETS_DIR, e);
        }
    }

    /**
     * Load the given icon set.  Parse the {@code iconset.xml} into a {@link UserIconSet}, and then
     * load up all the entries.
     */
    private void loadIconSet(Path iconSetPath)
    {
        logger.debug("Load {}", iconSetPath);

        try (ZipReader reader = new ZipReader(iconSetPath.toFile()))
        {
            final UserIconSet iconSet = parseIconSetXml(reader);
            if (iconSet != null)
            {
                iconSet.setIconSetPath(iconSetPath);
                loadIconSet(iconSet, reader);
            } else
            {
                logger.warn("NULL iconset from {}", iconSetPath);
            }
        } catch (IOException e)
        {
            logger.error("Failed to load IconSet {}", iconSetPath, e);
        }
    }

    /**
     * Read and Parse the {@code iconset.xml} into a {@link UserIconSet}.  If it can't be read, or it is invalid,
     * generate a new instance from the filename and the hash of the file content as the UID.
     */
    @Nullable
    private UserIconSet parseIconSetXml(ZipReader reader)
    {
        final File zipFilePath = reader.getZipFilePath();
        UserIconSet userIconSet = null;

        if (reader.hasEntry(ICONSET_XML))
        {
            try (InputStream inputStream = reader.getInputStream(ICONSET_XML))
            {
                try
                {
                    userIconSet = (UserIconSet) jaxbContext.createUnmarshaller().unmarshal(inputStream);
                } catch (JAXBException e)
                {
                    logger.error("Parsing " + ICONSET_XML + " from {} failed", zipFilePath, e);
                }
            } catch (IOException e)
            {
                logger.warn("Failed to read {} from {}: {}", ICONSET_XML, zipFilePath, e.getMessage());
            }
        } else
        {
            logger.warn("Icon set {} is missing entry {}", zipFilePath, ICONSET_XML);
        }

        if (userIconSet == null || !userIconSet.isValid())
        {
            final String uid = HashingUtils.sha256sum(zipFilePath);
            if (StringUtils.isEmpty(uid))
            {
                logger.warn("Failed to compute hash for iconset zip: {}", zipFilePath);
                return null;
            }

            logger.debug("Generated UserIconSet '{}' with UUID '{}'", zipFilePath, uid);
            userIconSet = new UserIconSet(FilenameUtils.getBaseName(zipFilePath.getName()), uid);
        }
        return userIconSet;
    }

    /**
     * Load the contents of the icon set zip file, augmenting the supplied {@link UserIconSet iconSet}.
     *
     * @param iconSet Parsed Icon set
     * @param reader Reader to use to access the contents of the icon set zip file
     */
    private void loadIconSet(UserIconSet iconSet, ZipReader reader)
    {
        logger.debug("load {}", iconSet);

        final String iconSetUid = iconSet.getUid();

        // Track used filenames, allow only once per iconset (even across zip groups/directories)
        final Set<String> fileNamesSeen = new HashSet<>();

        // Process all the entries in the zip
        final var entries = reader.entries();
        while (entries.hasMoreElements())
        {
            final ZipEntry zipEntry = entries.nextElement();
            final String entryName = zipEntry.getName();

            if (zipEntry.isDirectory()) continue; // We only process files

            if (!ACCEPTED_IMAGE_TYPES.contains(FilenameUtils.getExtension(entryName.toLowerCase())))
            {
                logger.debug("Ignoring non-image entry: {}", zipEntry);
                continue;
            }

            // Extract filename, group is top level directory name, default to "Other" if the file is in top level of zip
            final String[] tokens = entryName.split("/");
            final String group = tokens.length == 1 ? "Other" : tokens[0];
            final String filename = tokens[tokens.length - 1];

            if (!fileNamesSeen.add(filename.toLowerCase()))
            {
                logger.debug("Skipping duplicate filename: {}, for zip entry: {}", filename, zipEntry.getName());
                continue;
            }

            logger.trace("Add UserIcon: {} in group {} path {}", filename, group, entryName);
            iconSet.addOrUpdateIcon(filename, group, entryName, readImageBytes(reader, zipEntry, iconSet.isSkipResize()));
        }

        iconSetsByUid.put(iconSetUid, iconSet);
    }

    /**
     * Get the bytes for the icon image, resizing it as needed.
     * @since 5.2
     */
    @Nonnull
    private byte[] readImageBytes(ZipReader reader, ZipEntry zipEntry, boolean skipResize)
    {
        try (InputStream inputStream = reader.getInputStream(zipEntry.getName()))
        {
            if (skipResize)
            {
                return inputStream.readAllBytes();
            } else
            {
                // Scale the image
                final BufferedImage image = ImageUtils.getScaledInstance(ImageIO.read(inputStream), DEFAULT_ICON_WIDTH);

                final var buffer = new ByteArrayOutputStream();
                ImageIO.write(image, "png", buffer);

                return buffer.toByteArray();
            }
        } catch (IOException e)
        {
            logger.warn("failed to get image bytes for '{}'", zipEntry.getName(), e);
            return DEFAULT_IMAGE_BYTES;
        }
    }
}
