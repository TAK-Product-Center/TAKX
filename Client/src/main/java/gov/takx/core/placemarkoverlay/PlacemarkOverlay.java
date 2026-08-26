package gov.takx.core.placemarkoverlay;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.filesystem.IVFSFile;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.timer.ITimerService;
import gov.takx.commons.file.FilePath;
import gov.takx.commons.file.FileUtils;
import gov.takx.core.filesystem.VirtualFileSystemManagerProvider;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

/**
 * Maintains all the data required for rendering a layer of point placemarks.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class PlacemarkOverlay
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Name of the XML element containing the name of the overlay.
     */
    public static final String PLACEMARK_OVERLAY_NAME = "overlayName";

    /**
     * Name of the XML element containing the name of the icon to be used for the point placemarks.
     */
    public static final String ICON_NAME = "imageName";

    /**
     * Name of the XML element containing the name of the file that defines the data points.
     */
    public static final String FILE_NAME = "fileName";

    /**
     * Boolean indicating if the layer is currently visible.
     */
    public static final String LAYER_VISIBLE = "layerVisible";

    /**
     * Name of the XML element containing the editable flag for this layer.
     */
    public static final String IS_EDITABLE = "isEditable";

    /**
     * The name of the user-selected column that will be displayed as the roll-over tool tip.
     * The value may be {@code null} if the user did not configure the advanced options for the layer.
     *
     * @since 1.16
     */
    public static final String ID_COLUMN = "idColumn";

    /**
     * The user-selected, semi-colon delimited set of column names that will be displayed as the hover tool tip.
     * The value may be {@code null} if the user did not configure the advanced options for the layer.
     *
     * @since 1.16
     */
    public static final String TOOLTIP_COLUMNS = "tooltipColumns";

    /**
     * The name of the user-selected column that from which a relative path to an image will be obtained.
     * The value may be {@code null} if the user did not configure the advanced options for the layer.
     *
     * @since 1.16
     */
    public static final String IMAGE_COLUMN = "imageColumn";

    /**
     * The time to delay before committing an update the the CSV file, in milliseconds. This allows many property
     * change events to be batched into a single file update.
     */
    private static final int CSV_UPDATE_DELAY = 1000;

    /**
     * The name of the layer.
     */
    private final String layerName;

    /**
     * All point placemarks associated with this layer.
     */
    private final List<CSVPlaceMark> placeMarks = new ArrayList<>();

    /**
     * Layer attributes.
     */
    private final PointPlacemarkAttributes attributes;

    /**
     * The CSV file containing the point placemark data.
     */
    private final File csvFile;

    /**
     * The header names extracted from the CSV file.
     */
    private List<String> headers;

    /**
     * The custom column from which the placemarks' display name can be extracted. May be {@code null}.
     *
     * @since 1.16
     */
    private final String idColumn;

    /**
     * The name of the column from which a relative image path will be extracted. May be {@code null}.
     *
     * @since 1.16
     */
    private String imageColumn;

    /**
     * The name of the columns to be included in the hover tooltip. May be {@code null}.
     *
     * @since 1.16
     */
    private final Collection<String> tooltipColumns;

    /**
     * The scope of this layer.
     *
     * @since 1.16
     */
    private final VFSScope scope;

    /**
     * The current dataset for this layer.
     *
     * @since 1.16
     */
    private PlacemarkOverlayData data;

    /**
     * Delimiter to use in parsing the CSV file.
     *
     * @since 2.7.4
     */
    private final char delimiter;

    /**
     * Flag indicating if this is a new overlay.
     *
     * @since 2.7.4
     */
    private final boolean newOverlay;

    /**
     * Indicates if this layer is editable.
     *
     * @since 1.16
     */
    private final boolean editable;

    /**
     * The virtual file system used to store this overlay's files.
     */
    private IVirtualFileSystem vfs;

    /**
     * A map of this overlay's metadata, to be stored in the master placemark file by the
     * {@link PlacemarkOverlayManager}
     */
    private Map<String, String> metadata;

    /**
     * Listens for changes to placemarks and updates the CSV file accordingly.
     */
    private final PlacemarkPropertyChangeListener placemarkPropertyChangeListener;

    /**
     * Timer task used to batch CSV file updates.
     */
    private TimerTask csvFileUpdateTask;

    @Inject
    private PlacemarkOverlayManager placemarkOverlayManager;

    @Inject
    private ITimerService timerService;

    @Inject
    private VirtualFileSystemManagerProvider virtualFileSystemManagerProvider;

    /**
     * Constructor.
     *
     * @param layerName    The name of the layer.
     * @param inCSVFile    The CSV file containing the point placemark data.
     * @param imageAddress The name of the image to be used for the point placemarks.
     * @param newOverlay   Flag indicating if this is a new overlay.
     * @throws IOException Thrown if there is an error reading the CSV file.
     */
    public PlacemarkOverlay(String layerName, File inCSVFile, String imageAddress, boolean newOverlay) throws IOException
    {
        this(layerName, inCSVFile, ',', imageAddress, null, null, null, VFSScope.Project, false, newOverlay);
    }

    /**
     * Constructor.
     *
     * @param layerName      The name of the layer.
     * @param inCSVFile      The CSV file containing the point placemark data.
     * @param delimiter      The delimiter char
     * @param imageAddress   The name of the image to be used for the point placemarks.
     * @param idColumn       The name of the column from which the point placemark ids will be extracted.
     * @param imageColumn    The name of the column from which a relative path to an image will be extracted.
     * @param tooltipColumns The set of columns from which the tooltip data will be extracted.
     * @param scope          The scope of this layer: global, user or project.
     * @param editable       Indicates if this layer may be edited.
     * @param newOverlay     Flag indicating if this is a new overlay.
     * @throws IOException Thrown if there is an error reading the CSV file.
     * @since 1.16
     */
    public PlacemarkOverlay(String layerName, File inCSVFile, char delimiter, String imageAddress, String idColumn,
                            String imageColumn, Collection<String> tooltipColumns,
                            VFSScope scope, boolean editable, boolean newOverlay)
            throws IOException
    {
        attributes = new PointPlacemarkAttributes();
        attributes.setImageOffset(new Offset(0.5, 0.0, AVKey.FRACTION, AVKey.FRACTION));
        placemarkPropertyChangeListener = new PlacemarkPropertyChangeListener();

        this.layerName = layerName;
        csvFile = inCSVFile;
        setIconImageAddress(imageAddress);
        this.idColumn = idColumn;
        this.imageColumn = imageColumn;
        this.tooltipColumns = tooltipColumns;
        this.scope = scope;
        this.editable = editable;
        this.delimiter = delimiter;
        this.newOverlay = newOverlay;
    }

    @PostConstruct
    private void postConstruct() throws IOException
    {
        // Initialize the VFS that will be used to store this overlay's files.
        vfs = virtualFileSystemManagerProvider.getVirtualFileSystemManager(scope)
                .getVirtualFileSystem(PlacemarkOverlayManager.OVERLAY_VFS_NAMESPACE, PlacemarkOverlayManager.PLACEMARKS_VFS_SUBDIRECTORY + layerName);

        // Import the provided CSV fileCSVInput and attempt to extract the column headers (keyMap) and the contents into a list of maps
        data = new PlacemarkOverlayData(csvFile, delimiter, idColumn, tooltipColumns);

        // Keep a reference to the CSV headers for access through the properties panel
        headers = data.getHeaders();

        // Add all placemarks to this layer.
        for (int i = 0; i < data.getSize(); i++)
        {
            createPlacemark(data, i);
        }

        // Create config files, copy files referenced by the layer to the VFS and build the metadata map.
        metadata = createAndCopyFiles(newOverlay);
    }

    /**
     * Return the scope of this layer.
     *
     * @return The scope of this layer.
     * @since 1.16
     */
    public VFSScope getScope()
    {
        return scope;
    }

    /**
     * Return the map that contains this overlay's metadata.
     *
     * @return The overlay's metadata map.
     */
    public Map<String, String> getMetadata()
    {
        // Update the dynamic value(s).
        metadata.put(LAYER_VISIBLE, Boolean.toString(placemarkOverlayManager.isLayerVisible(this)));

        return metadata;
    }

    /**
     * The directory location of the CSV file. This will also be the path that will be used as the
     * starting point for relative paths to images.
     *
     * @return The directory location for the CSV file.
     * @since 1.16
     */
    public String getDirectory()
    {
        return csvFile.getParentFile().getPath();
    }

    /**
     * Returns the name of this layer.
     *
     * @return The name of this layer.
     */
    public String getLayerName()
    {
        return layerName;
    }

    /**
     * Return all the headers extracted from the CSV file.
     *
     * @return All the headers extracted from the CSV file.
     */
    public List<String> getHeaders()
    {
        return headers;
    }

    /**
     * Return the name of the header for the column containing latitude data.
     *
     * @return The name of the header for the column containing latitude data.
     */
    public String getLatitudeHeader()
    {
        return data.getLatitudeHeader();
    }

    /**
     * Return the name of the header for the column containing longitude data.
     *
     * @return The name of the header for the column containing longitude data.
     */
    public String getLongitudeHeader()
    {
        return data.getLongitudeHeader();
    }

    public String getDisplayNameHeader()
    {
        return data.getDisplayNameHeader();
    }

    public Collection<String> getTooltipColumns()
    {
        return tooltipColumns;
    }

    public String getImageColumn()
    {
        return imageColumn;
    }

    public String addImageColumn()
    {
        // If an image column is already specified, throw an exception.
        if (imageColumn != null && !imageColumn.isEmpty())
        {
            return imageColumn;
        }

        // Make sure the image column name doesn't exist. If so,
        // append an ascending int until we find a unique name.
        imageColumn = "image";
        int i = 0;
        while (headers.contains(imageColumn))
        {
            imageColumn = imageColumn + i++;
        }

        headers.add(imageColumn);

        return imageColumn;
    }

    public boolean isEditable()
    {
        return editable;
    }

    public CSVPlaceMark addPlacemark(Map<String, String> map, String imagePath)
    {
        // If an image was supplied with the placemark, make sure there's
        // an image column to add it to and add it.
        if (imagePath != null && !imagePath.isEmpty())
        {
            if (imageColumn == null || imageColumn.isEmpty())
            {
                addImageColumn();
            }

            map.put(imageColumn, imagePath);
        }

        // Add the placemark to the data set.
        data.addPlacemark(map);

        // Write the new entry to the CSV file.
        updateCSVFile();

        return createPlacemark(data, data.getSize() - 1);
    }

    /**
     * Create and add a new placemark to this layer.
     *
     * @param data The dataset.
     * @param row  The row in the dataset to create a placemark from.
     * @return Return the newly created point placemark.
     */
    private CSVPlaceMark createPlacemark(PlacemarkOverlayData data, int row)
    {
        CSVPlaceMark pp = null;

        try
        {
            pp = new CSVPlaceMark(this, Position.fromDegrees(data.getLatitude(row), data.getLongitude(row), 0), data.getCSVMap(row));

            String tooltip = data.getToolTip(row);
            pp.setValue(AVKey.ROLLOVER_TEXT, tooltip);
            pp.setValue(AVKey.NAME, data.getDisplayName(row));
            pp.setLabelText(data.getDisplayName(row));
            pp.setAttributes(attributes);
            pp.addPropertyChangeListener(placemarkPropertyChangeListener);
            placeMarks.add(pp);
        } catch (Exception e)
        {
            logger.warn("Unable to create placement for row {}", row, e);
        }

        return pp;
    }

    /**
     * Get all the point placemarks contained in this layer.
     *
     * @return All the point placemarks contained in this layer.
     */
    public Iterable<CSVPlaceMark> getData()
    {
        return placeMarks;
    }

    /**
     * Set the location of the icon to be used by each point placemark in this layer.
     *
     * @param imageAddress The address of the icon.
     */
    private void setIconImageAddress(String imageAddress)
    {
        double scaleFactor = PlacemarkOverlayUtils.calculateScaleFactor(imageAddress);
        attributes.setImageAddress(imageAddress);
        attributes.setScale(scaleFactor);
    }

    /**
     * Return the location of the icon image that is to be used on the map for all point placemarks belonging to this
     * layer.
     *
     * @return The location of the icon image.
     */
    private String getImageAddress()
    {
        return attributes.getImageAddress();
    }

    /**
     * Converts the metadata values in this layer into a set of key value pairs that can be used to generate a
     * persistable XML file. If there are files that must be copied into the VFS (imagery, icon, etc.) it will
     * be done in this method as well.
     *
     * @return A set of key/value pairs representing the metadata to be persisted in an XML file.
     */
    private Map<String, String> createAndCopyFiles(boolean newOverlay)
    {
        String csvFilename = csvFile.getName();
        String imageFilename = new File(getImageAddress()).getName();

        if (newOverlay)
        {
            try
            {
                // copy the icon file into the project
                File targetImageFile = vfs.createFile(new FilePath(imageFilename)).getFile();
                FileUtils.copy(new File(getImageAddress()), targetImageFile);
                setIconImageAddress(targetImageFile.getAbsolutePath());

                if (imageColumn != null)
                {
                    for (CSVPlaceMark placemark : placeMarks)
                    {
                        String imagePath = placemark.getCSVMap().get(imageColumn);

                        if (imagePath != null && !imagePath.isEmpty())
                        {
                            File originalImageFile = new File(imagePath);

                            if (originalImageFile.isAbsolute())
                            {
                                IVFSFile vfsFile = vfs.createFile(new FilePath(originalImageFile.getName()));
                                FileUtils.copy(originalImageFile, vfsFile.getFile());
                                placemark.getCSVMap().put(imageColumn, "/" + originalImageFile.getName());
                            } else
                            {
                                IVFSFile vfsFile = vfs.createFile(new FilePath(imagePath));
                                FileUtils.copy(new File(placemark.getDirectory(), imagePath), vfsFile.getFile());
                            }
                        }
                    }
                }
            } catch (Exception e)
            {
                logger.error("createAndCopyFiles can not copy data files", e);
                return null;
            }
        } else if (imageColumn != null)
        {
            for (CSVPlaceMark placemark : placeMarks)
            {
                File imageFile = new File(placemark.getCSVMap().get(imageColumn));

                if (imageFile.isAbsolute() && imageFile.exists())
                {
                    try
                    {
                        IVFSFile vfsFile = vfs.createFile(new FilePath(imageFile.getName()));
                        FileUtils.copy(imageFile, vfsFile.getFile());
                        placemark.getCSVMap().put(imageColumn, "/" + imageFile.getName());
                    } catch (IOException ioe)
                    {
                        logger.error("Error copying image file {} for point placemark.", imageFile.getPath(), ioe);
                    }
                } else
                {
                    logger.error("Could not find image file {}", imageFile.getPath());
                }
            }
        }

        if (newOverlay)
        {
            updateCSVFile();
        }

        HashMap<String, String> persistableData = new HashMap<>();
        persistableData.put(PLACEMARK_OVERLAY_NAME, layerName);
        persistableData.put(ICON_NAME, imageFilename);
        persistableData.put(FILE_NAME, csvFilename);
        persistableData.put(LAYER_VISIBLE, Boolean.toString(placemarkOverlayManager.isLayerVisible(this)));
        persistableData.put(ID_COLUMN, idColumn);
        persistableData.put(IMAGE_COLUMN, imageColumn);
        persistableData.put(TOOLTIP_COLUMNS, PlacemarkOverlayUtils.getTooltipColumnsString(tooltipColumns));
        persistableData.put(IS_EDITABLE, String.valueOf(editable));

        return persistableData;
    }

    /**
     * Update the CSV file that defines this overlay.
     * <p>
     * todo: The VFS should back up the original file in case the user exits without saving. No support for this yet.
     */
    private void updateCSVFile()
    {
        try
        {
            FilePath filePath = new FilePath(csvFile.getName());
            IVFSFile file = vfs.getFile(filePath);
            File csvFile = file != null
                    ? file.getFile()
                    : vfs.createFile(filePath).getFile();

            PlacemarkOverlayUtils.writePlacemarkOverlayCSVFile(this, csvFile);
        } catch (IOException e)
        {
            logger.error("createAndCopyFiles can not write placemark overlay data to file: {}", csvFile, e);
        }
    }

    /**
     * Delete all data associated with with layer from the VFS.
     *
     * @since 1.16
     */
    public void deletePersistedData()
    {
        vfs.listFiles().stream()
                .filter(file -> !vfs.deleteFile(file))
                .forEach(file -> logger.warn("Could not delete placemark file {}", file.getFileName()));

        // todo: mark the root directory for deletion too when supported by the VFS
    }

    /**
     * Schedule a delayed CSV file update, interrupting the currently scheduled update if necessary.
     */
    private void scheduleCsvFileUpdate()
    {
        // Cancel existing.
        if (csvFileUpdateTask != null)
        {
            csvFileUpdateTask.cancel();
        }

        // Schedule a new one.
        csvFileUpdateTask = timerService.createTask(this::updateCSVFile, CSV_UPDATE_DELAY);
    }

    /**
     * Listen for changes to placemarks that indicate that we need to update the CSV file.
     */
    private class PlacemarkPropertyChangeListener implements PropertyChangeListener
    {
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            scheduleCsvFileUpdate();
        }
    }
}
