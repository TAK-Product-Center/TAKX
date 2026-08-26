package gov.tak.platform.datapackage.content;

import gov.tak.api.comms.takserver.IDataPackageManager;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.icons.ICotIconResolver;
import gov.tak.api.symbology.MilSymDetailHandler;
import gov.tak.platform.datapackage.PackageReader;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.media.MediaType;
import gov.takx.api.milsym.IMilStd2525Service;
import gov.takx.api.milsym.IMilStd2525ServiceProvider;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.api.shapes.ShapeType;
import gov.takx.commons.file.SizeFormatter;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.UIResources.ACTION_BUTTON_IMAGE_SIZE;
import static gov.takx.platform.ui.UIResources.getActionButtonIcon;

/**
 * Model representing the contents of a data package.
 *
 * @see DataPackageEntry
 * @see #getEntryList()
 * @since 3.2
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class DataPackageContents
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int IMAGE_WIDTH = ACTION_BUTTON_IMAGE_SIZE.width;
    private static final Set<String> OTHER_KNOWN_EXTENSIONS = Set.of("kml", "kmz",
            "shp", "shx", "dbf", "prj", // ShapeFileLayer.SHAPE_FILE_EXTENSIONS
            "rxg", // SurfaceImageryFileImporter.GEO_FILE_EXTENSION
            "pdf", "txt");

    /**
     * Name of local import owner.
     *
     * @since 3.5
     */
    public static final String LOCAL_IMPORT_OWNER = "Local import";

    @Inject
    private IMapObjectFinder mapObjectFinder;

    @Inject
    private ICotIconResolver cotIconResolver;

    @Inject
    private IDataPackageManager dataPackageManager;

    @Inject
    private IMilStd2525ServiceProvider milStd2525ServiceProvider;

    private final File packageFile;
    private final String owner;

    private List<DataPackageEntry> entryList;
    private PackageManifest manifest;
    private PackageReader packageReader;

    /**
     * Construct on the given package file.
     *
     * @param packageFile File to process
     * @param owner Owner of the package
     */
    public DataPackageContents(@Nonnull File packageFile, @Nonnull String owner)
    {
        this.packageFile = packageFile;
        this.owner = owner;
    }

    /**
     * @return The source package file
     */
    @Nonnull
    public File getPackageFile()
    {
        return packageFile;
    }

    /**
     * @return The owner of the package
     */
    @Nonnull
    public String getOwner()
    {
        return owner;
    }

    /**
     * Note: Locally imported data packages do not know their owners.
     *
     * @return {@code true} if the package was added via local importing.
     *
     * @since 3.5
     */
    public boolean isLocalImport()
    {
        return dataPackageManager.isLocalImport(packageFile);
    }

    /**
     * @return {@code true} if the package was added via commo peer sharing.
     *
     * @since 3.5
     */
    public boolean isPeerImport()
    {
        return dataPackageManager.isPeerImport(packageFile);
    }

    /**
     * @return {@code true} if the package was added via downloading from TAK Server.
     *
     * @since 3.5
     */
    public boolean isServerDownload()
    {
        return dataPackageManager.isServerDownload(packageFile);
    }

    /**
     * @return Formatted size of the package file
     */
    @Nonnull
    public String getFormattedSize()
    {
        return SizeFormatter.formatEstimate(packageFile.length());
    }

    /**
     * @return List of package entries
     */
    @Nonnull
    public List<DataPackageEntry> getEntryList()
    {
        if (entryList == null) entryList = buildEntryList();
        return Collections.unmodifiableList(entryList);
    }

    /**
     * @return Manifest of package contents.
     *
     * @see #getEntryList() Detailed list of entries.
     * @since 3.5
     */
    @Nonnull
    public PackageManifest getManifest()
    {
        // The entry-list getter will read from the file, which populates the manifest
        if (manifest == null) getEntryList();

        return manifest;
    }

    @Override
    public String toString()
    {
        return "DataPackageContents{" +
                "packageFile=" + packageFile +
                ", owner='" + owner + '\'' +
                ", size=" + (entryList != null ? entryList.size() : "null") +
                '}';
    }

    /**
     * Read the package file and construct our entry list.
     */
    @Nonnull
    private List<DataPackageEntry> buildEntryList()
    {
        try (PackageReader reader = new PackageReader(packageFile))
        {
            packageReader = reader;
            manifest = reader.getManifest();
            return manifest.getContents().getContents().stream()
                    .filter(content -> !content.isIgnore()) // Don't process ignored entries
                    .map(this::createEntry)
                    .collect(Collectors.toList());
        } catch (IOException e)
        {
            logger.error("Failed to read data package: {}", packageFile, e);
            return Collections.emptyList();
        } finally
        {
            packageReader = null;
        }
    }

    /**
     * Create a DataPackageEntry from the manifest entry.
     */
    @Nonnull
    private DataPackageEntry createEntry(@Nonnull PackageManifestContent content)
    {
        final String zipEntry = content.getManifestUid();
        final int slashIndex = zipEntry.lastIndexOf('/');
        final String entryName = slashIndex < 0 ? zipEntry : zipEntry.substring(slashIndex + 1);
        final long entrySize = packageReader.getZipEntrySize(zipEntry);

        final String suffix;
        try
        {
            suffix = FilenameUtils.getExtension(entryName).toLowerCase();
        } catch (IllegalArgumentException e)
        {
            logger.warn("Illegal file name in package: '{}': {}", entryName, e.getMessage());
            return new DataPackageEntry(zipEntry, "UNK", entryName, entrySize);
        }

        if (suffix.equals("cot")) return createEntryFromCot(zipEntry);

        if (!MediaType.Image.getExtensions().contains(suffix)
                && !MediaType.Video.getExtensions().contains(suffix)
                && !MediaType.Audio.getExtensions().contains(suffix)
                && !OTHER_KNOWN_EXTENSIONS.contains(suffix))
        {
            logger.debug("Don't know how to process entry in data package; marking as unknown {}", zipEntry);
            return new DataPackageEntry(zipEntry, "UNK", entryName, entrySize);
        }

        return new DataPackageEntry(zipEntry, suffix, entryName, entrySize);
    }

    /**
     * Create a from the CoT message contained in the indicated package entry.
     */
    @Nonnull
    @SuppressWarnings("DataFlowIssue")
    private DataPackageEntry createEntryFromCot(@Nonnull String zipEntry)
    {
        final long size = packageReader.getZipEntrySize(zipEntry);
        final String cotMessage = packageReader.readEntryAsString(zipEntry);
        final CotEvent event = cotMessage.isEmpty() ? new CotEvent() : CotEvent.parse(cotMessage);

        // Avoid an invalid/un-parsable CoT message from causing exceptions.
        if (!event.isValid())
        {
            logger.warn("Invalid CoT event in package; IGNORING: {}", StringUtils.abbreviate(cotMessage, 100));
            return new DataPackageEntry(zipEntry, "UNK", zipEntry, size);
        }

        final IMapObject mapObject = mapObjectFinder.findMapObjectByUid(event.getUID());

        if (mapObject != null)
        {
            return new DataPackageEntry(zipEntry, mapObject, mapObject.getSizedIcon(IMAGE_WIDTH), size);
        } else
        {
            final String callsign = Objects.requireNonNullElse(event.getCallsign(), "Unknown");
            final String cotType = event.getType();
            final ImageIcon icon = switch (cotType)
            {
                // Check for known shape types
                case CotTypes.SHAPE_ARROW -> getActionButtonIcon(PLATFORM_ICONS, ShapeType.ARROW.getIconName());
                case CotTypes.SHAPE_RECTANGLE -> getActionButtonIcon(PLATFORM_ICONS, ShapeType.RECTANGLE.getIconName());
                case CotTypes.SHAPE_MULTI_POLYLINE -> getActionButtonIcon(PLATFORM_ICONS, ShapeType.POLYLINE.getIconName());
                case CotTypes.SHAPE_POLYSHAPE -> getActionButtonIcon(PLATFORM_ICONS, ShapeType.POLYGON.getIconName());
                case CotTypes.SHAPE_ELLIPSE -> getActionButtonIcon(PLATFORM_ICONS, ShapeType.ELLIPSE.getIconName());
                case CotTypes.SHAPE_CIRCLE -> getActionButtonIcon(PLATFORM_ICONS, ShapeType.CIRCLE.getIconName());
                case CotTypes.SHAPE_RANGE_CIRCLE -> getActionButtonIcon(PLATFORM_ICONS, ShapeType.BULLSEYE.getIconName());

                // For anything else, determine preview by the 2525 symbol if present, and lastly the CoT type
                default ->
                {
                    final CotDetail detail = event.getDetail();
                    CotDetail milsym = detail.getFirstChildByName(MilSymDetailHandler.DETAIL_SINGLEPOINT);
                    if (milsym == null) milsym = detail.getFirstChildByName(MilSymDetailHandler.DETAIL_MULTIPOINT);
                    if (milsym != null && milsym.hasAttribute("id"))
                    {
                        final String sidc = milsym.getAttribute("id");
                        final IMilStd2525Service milstdService = milStd2525ServiceProvider.get2525ServiceForSymbol(sidc);
                        if (!milstdService.isUnrenderableSymbol(sidc)) yield new ImageIcon(milstdService.renderSinglePointIcon(sidc, IMAGE_WIDTH));
                    }

                    // Fall back to CoT type
                    yield new ImageIcon(cotIconResolver.getIcon(IMAGE_WIDTH, cotType));
                }
            };

            return new DataPackageEntry(zipEntry, icon, callsign, size);
        }
    }
}
