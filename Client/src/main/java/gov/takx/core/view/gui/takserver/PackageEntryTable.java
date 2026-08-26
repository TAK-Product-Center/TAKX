package gov.takx.core.view.gui.takserver;

import gov.nasa.worldwind.util.WWIO;
import gov.tak.api.comms.takserver.IDataPackageManager;
import gov.tak.platform.datapackage.content.DataPackageContents;
import gov.tak.platform.datapackage.content.DataPackageEntry;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.kml.IKmlLayerService;
import gov.takx.api.layer.IMobacLayerManager;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.IRaptorLayerManagerListener;
import gov.takx.api.media.IMedia;
import gov.takx.api.media.IMediaManager;
import gov.takx.api.media.IMediaPlayerManager;
import gov.takx.api.media.MediaType;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.ButtonTableCellEditor;
import gov.takx.commons.swing.ButtonTableCellRenderer;
import gov.takx.core.globe.layers.LayerImportParameters;
import gov.takx.core.globe.layers.ShapefileImportHandler;
import gov.takx.core.view.gui.maps.AddShapeOverlayController;
import gov.takx.core.view.gui.maps.importers.OverlayImageImporter;
import gov.takx.core.view.gui.maps.importers.SurfaceImageryFileImporter;
import gov.takx.platform.ui.UIResources;
import jakarta.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_PNG;
import static gov.takx.platform.ui.UIResources.ACTION_BUTTON_IMAGE_SIZE;

/**
 * Table to hold a collection of data package entries.
 *
 * @since 3.2
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class PackageEntryTable extends JTable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String MEDIA_MANAGER_KEY = "data-package-table";

    private static final Icon OPEN_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "file-open.png");
    private static final Icon OPEN_AUDIO_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "audio.png");
    private static final Icon OPEN_IMAGE_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "imagery_overlay.png");
    private static final Icon OPEN_VIDEO_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "video.png");
    private static final Icon GOTO_ICON = UIResources.getActionButtonIcon(TAK_ASSETS_PNG, "PanTo_32x32.png");
    private static final Icon DELETE_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "delete.png");

    private static final int BUTTON_COL_SIZE = ACTION_BUTTON_IMAGE_SIZE.width + 4; // A little padding
    private static final int ICON_COL_WIDTH = 40;
    private static final int SIZE_COL_WIDTH = 60;

    private final DataPackageContents packageContents;
    private final File packageFile;

    @Inject
    private IGlobeManager globeManager;

    @Inject
    private IDataPackageManager dataPackageManager;

    @Inject
    private IMediaManager mediaManager;

    @Inject
    private IMediaPlayerManager mediaPlayerManager;

    @Inject
    private IKmlLayerService kmlLayerService;

    @Inject
    private IMobacLayerManager mobacLayerManager;

    @Inject
    private IRaptorLayerManager layerManager;

    @Inject
    private ICoreExecutorService executorService;

    /**
     * Construct on the entries in the given package contents.
     */
    public PackageEntryTable(DataPackageContents packageContents)
    {
        this.packageContents = packageContents;

        packageFile = packageContents.getPackageFile();

        final var model = new EntryTableModel(packageContents.getEntryList());
        setModel(model);

        setTableHeader(null);
        setRowHeight(BUTTON_COL_SIZE);
        setDefaultRenderer(DataPackageEntry.class, new EntryCellRenderer());
        setShowGrid(false);

        final TableColumnModel cm = getColumnModel();

        final TableColumn iconColumn = cm.getColumn(Columns.iconOrType.ordinal());
        iconColumn.setResizable(false);
        iconColumn.setMaxWidth(ICON_COL_WIDTH);

        final TableColumn sizeColumn = cm.getColumn(Columns.size.ordinal());
        sizeColumn.setResizable(false);
        sizeColumn.setMaxWidth(SIZE_COL_WIDTH);

        configureButtonColumn(cm.getColumn(Columns.actionButton.ordinal()), PackageEntryTable::computeIcon, PackageEntryTable::computeTooltip,
                model.entries::contains, this::action);
        configureButtonColumn(cm.getColumn(Columns.deleteButton.ordinal()), entry -> DELETE_ICON, entry -> "Delete",
                model.entries::contains, this::deleteEntry);
    }

    /**
     * @return Appropriate tooltip for the given package entry
     * @since 5.5.1
     */
    private static String computeTooltip(DataPackageEntry entry)
    {
        return entry.hasLocation() ? "Go To" : "Open";
    }

    /**
     * Different entries represent different data types, which we'll want to show different icons for.
     *
     * @param entry Entry to map to an icon.
     *
     * @return Icon for the 'open' action.
     */
    private static Icon computeIcon(DataPackageEntry entry)
    {
        if (entry.hasLocation()) return GOTO_ICON;
        String extension = FileUtils.getExtension(entry.zipEntryPath);
        return switch (MediaType.getMediaTypeForExtension(extension))
        {
            case Image -> OPEN_IMAGE_ICON;
            case Video -> OPEN_VIDEO_ICON;
            case Audio -> OPEN_AUDIO_ICON;
            default -> OPEN_ICON;
        };
    }

    /**
     * Generically handle data package entries.
     * <p/>
     * Map items will have a 'go-to' and files will be 'open'-able.
     */
    private void action(DataPackageEntry entry)
    {
        if (entry.hasLocation())
        {
            gotoEntry(entry);
        } else
        {
            extractEntry(entry);
        }
    }

    /**
     * Navigate the globe to the location from the given entry, if it has one.
     */
    private void gotoEntry(DataPackageEntry entry)
    {
        if (entry.hasLocation()) globeManager.getLastFocusedGlobe().goTo(entry.getLocation());
    }

    /**
     * Extracts the entry and opens it.
     */
    private void extractEntry(DataPackageEntry entry)
    {
        String zipEntryPath = entry.zipEntryPath;
        File extractedFile;
        try
        {
            extractedFile = dataPackageManager.extractDataPackageFile(packageFile, zipEntryPath);
        } catch (IOException ex)
        {
            logger.error("Failed to extract '{}' from '{}'", zipEntryPath, packageFile.getName(), ex);
            return;
        }

        String extension = FileUtils.getExtension(zipEntryPath);
        MediaType mediaType = MediaType.getMediaTypeForExtension(extension);
        IMedia media = null;
        switch (mediaType)
        {
            case Audio, Video, Image ->
            {
                media = mediaManager.getMedia(MEDIA_MANAGER_KEY, extractedFile.getName());
                if (media == null) media = mediaManager.addMediaFromLocalFile(extractedFile, MEDIA_MANAGER_KEY);
            }
            default ->
            {
                // Handle imagery and overlay content.
                if ("kml".equals(extension) || "kmz".equals(extension))
                {
                    // File should be importable as a KML layer.
                    try
                    {
                        kmlLayerService.loadKml(new FileInputStream(extractedFile), FilenameUtils.getName(entry.name));
                    } catch (IOException ex)
                    {
                        logger.error("Failed to read KML/KMZ from package entry '{}'", zipEntryPath, ex);
                    }
                    return;
                } else if (ShapefileImportHandler.SHAPE_FILE_EXTENSIONS.contains(extension))
                {
                    addGotoNewLayerOnceListener();
                    AddShapeOverlayController controller = new AddShapeOverlayController(extractedFile);
                    controller.doImportFile(false);
                    return;
                } else if (SurfaceImageryFileImporter.GEO_FILE_EXTENSION.equals(extension))
                {
                    // Surface imagery importing is done in reverse. It takes in the image file then looks up the geo-file.
                    // But from the UI we want to do this from the geo-file itself, so we're going backwards here.
                    List<DataPackageEntry> entryList = packageContents.getEntryList();
                    DataPackageEntry associatedImageEntry = entryList.stream()
                            .filter(e -> {
                                for (String imageFileExtension : MediaType.Image.getExtensions())
                                {
                                    if (e.name.equals(WWIO.replaceSuffix(entry.name, "." + imageFileExtension)))
                                        return true;
                                }
                                return false;
                            })
                            .findFirst().orElse(null);

                    // If we found the associated image, then import that. It'll be used to resolve the geo-file.
                    if (associatedImageEntry != null)
                    {
                        try
                        {
                            addGotoNewLayerOnceListener();
                            File extractedAssociatedImage = dataPackageManager.extractDataPackageFile(packageFile, associatedImageEntry.zipEntryPath);
                            OverlayImageImporter importer = new OverlayImageImporter(extractedAssociatedImage, new LayerImportParameters(true));
                            importer.beginImport();
                        } catch (IOException ex)
                        {
                            logger.error("Failed to extract/read geo-file from package entry '{}'", zipEntryPath, ex);
                        }
                    }
                    return;
                } else
                {
                    // Check the file for the mobac header, and then parse it in full if found.
                    // If no such header is found we'll move on to standard handling.
                    try (InputStream inputStream = new BufferedInputStream(new FileInputStream(extractedFile)))
                    {
                        inputStream.mark(Integer.MAX_VALUE);
                        if (mobacLayerManager.isMapSourceXml(inputStream))
                        {
                            inputStream.reset();
                            String configXml = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                            if (!configXml.isEmpty())
                            {
                                mobacLayerManager.makeLayerFromConfigXml(FilenameUtils.getName(entry.name), configXml);
                            }

                            // Should be a mobac layer, do not continue to default handling below.
                            return;
                        }
                    } catch (IOException ex)
                    {
                        logger.error("Failed to read Mobac layer from package entry '{}'", zipEntryPath, ex);
                    }
                }

                // Unknown content, open it externally.
                int result = AlwaysOnTopJOptionPane.showConfirmDialog(this,
                        "You are about to open an external program\nDo you want to continue?", "Open File?", JOptionPane.WARNING_MESSAGE,
                        JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION)
                {
                    FileUtils.browseFile(extractedFile);
                }
            }
        }
        if (media != null) mediaPlayerManager.createDefaultPlayer(media);
    }

    /**
     * Adds a layer listener which goes to the location of the next created layer.
     */
    private void addGotoNewLayerOnceListener()
    {
        if (layerManager.getLayers().stream().anyMatch(l -> l instanceof GotoNewestLayerListener)) return;
        layerManager.addRaptorLayerManagerListener(new GotoNewestLayerListener());
    }

    /**
     * Delete the entry from the data package.
     */
    private void deleteEntry(DataPackageEntry entry)
    {
        try
        {
            // Removing the entry will trigger the parent panel to refresh the display for the whole data package
            // so, we do not need to remove the file from the local model since it's going to be discarded anyways.
            dataPackageManager.removeFilesFromDataPackage(packageFile, entry.zipEntryPath);
        } catch (IOException ex)
        {
            logger.error("Failed deleting data package entry '{}'", entry.zipEntryPath, ex);
        }
    }

    /**
     * Configure a column with a button that invokes the given handler when clicked.
     *
     * @param column Column to configure
     * @param iconFunction Icon mapper to use for created buttons
     * @param tipFunction Tool tip mapper to use for created buttons
     * @param enabledPredicate Predicate to determine if the button should be enabled
     * @param handler Handler accepting a connection to invoke on button click
     */
    private void configureButtonColumn(TableColumn column, Function<DataPackageEntry, Icon> iconFunction,
                                       Function<DataPackageEntry, String> tipFunction,
                                       Predicate<DataPackageEntry> enabledPredicate,
                                       Consumer<DataPackageEntry> handler)
    {
        column.setResizable(false);
        column.setMaxWidth(BUTTON_COL_SIZE);
        column.setCellRenderer(new EntryButtonRenderer(iconFunction, tipFunction, enabledPredicate));
        column.setCellEditor(new EntryButtonEditor(iconFunction, enabledPredicate, handler));
    }

    /**
     * Cell renderer to handle various fields in the data package entry.
     */
    private static class EntryCellRenderer extends DefaultTableCellRenderer
    {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            final DataPackageEntry entry = (DataPackageEntry) value;

            setIcon(null);
            setIconTextGap(0);
            setToolTipText(null);

            switch (Columns.values[column])
            {
                case iconOrType:
                    if (entry.icon != null)
                    {
                        setIcon(entry.icon);
                        setText(null);
                    } else
                    {
                        setText(entry.type);
                        setToolTipText(entry.type);
                    }
                    break;
                case name:
                    setText(entry.name);
                    setToolTipText(entry.name);
                    break;
                case size:
                    setText(entry.size);
                    break;
                default:
                    return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
            return this;
        }
    }

    /**
     * A layer manager listener which goes to the next newly created layer just once.
     */
    private class GotoNewestLayerListener implements IRaptorLayerManagerListener
    {
        @Override
        public void raptorLayerAdded(IRaptorLayer raptorLayer)
        {
            // The go-to has to be run on a delay because the sector is assigned to the layer asynchronously
            executorService.schedule(() -> {
                globeManager.getLastFocusedGlobe().goTo(raptorLayer);
            }, 500, TimeUnit.MILLISECONDS);

            // We only want to run this once.
            layerManager.removeRaptorLayerManagerListener(this);
        }
    }

    /**
     * Specialized {@link ButtonTableCellRenderer} to enable/disable the button according to a supplied predicate.
     */
    private static class EntryButtonRenderer extends ButtonTableCellRenderer<DataPackageEntry>
    {
        private final Function<DataPackageEntry, Icon> iconFunction;
        private final Predicate<DataPackageEntry> enabledPredicate;
        private final Function<DataPackageEntry, String> tipFunction;

        /**
         * Renderer to handle enabled state of the button for a {@link DataPackageEntry}.
         *
         * @param iconFunction Icon mapper to provide the button's icon.
         * @param tipFunction String mapper to provide the button's tooltip
         * @param enabledPredicate Predicate to determine if the button should be enabled
         */
        public EntryButtonRenderer(Function<DataPackageEntry, Icon> iconFunction, Function<DataPackageEntry, String> tipFunction,
                                   Predicate<DataPackageEntry> enabledPredicate)
        {
            super(new JButton());
            this.iconFunction = iconFunction;
            this.enabledPredicate = enabledPredicate;
            this.tipFunction = tipFunction;
        }

        @Override
        protected void updateButtonForCell(JButton button, JTable table, DataPackageEntry value, boolean isSelected, int row, int column)
        {
            final boolean enabled = enabledPredicate.test(value);
            button.setIcon(iconFunction.apply(value));
            button.setEnabled(enabled);
            button.setToolTipText(enabled ? tipFunction.apply(value) : "Unavailable");
        }
    }

    /**
     * Specialized {@link ButtonTableCellEditor} to enable/disable the button according to a supplied predicate.
     */
    private static class EntryButtonEditor extends ButtonTableCellEditor<DataPackageEntry>
    {
        private final Function<DataPackageEntry, Icon> iconFunction;
        private final Predicate<DataPackageEntry> enabledPredicate;

        /**
         * Editor to handle enabled state of the button for a {@link DataPackageEntry}.
         *
         * @param iconFunction Icon mapper to provide the button's icon.
         * @param enabledPredicate Predicate to determine if the button should be enabled
         * @param handler Handler invoked with cell value when the button is clicked
         */
        public EntryButtonEditor(Function<DataPackageEntry, Icon> iconFunction, Predicate<DataPackageEntry> enabledPredicate,
                                 Consumer<DataPackageEntry> handler)
        {
            super(new JButton(), handler);
            this.iconFunction = iconFunction;
            this.enabledPredicate = enabledPredicate;
        }

        @Override
        protected void updateButtonForCell(JButton button, JTable table, DataPackageEntry value, boolean isSelected, int row, int column)
        {
            button.setIcon(iconFunction.apply(value));
            button.setEnabled(enabledPredicate.test(value));
        }
    }

    /**
     * Minimal table model to hold a list of {@link DataPackageEntry} objects.
     */
    private static class EntryTableModel extends AbstractTableModel
    {
        private final ArrayList<DataPackageEntry> entries = new ArrayList<>();

        public EntryTableModel(List<DataPackageEntry> packageEntries)
        {
            packageEntries.forEach(this::addEntry);
        }

        void addEntry(DataPackageEntry entry)
        {
            entries.add(entry);
        }

        @Override
        public int getRowCount()
        {
            return entries.size();
        }

        @Override
        public int getColumnCount()
        {
            return Columns.values.length;
        }

        @Override
        public Class<?> getColumnClass(int column)
        {
            return DataPackageEntry.class;
        }

        @Override
        public Object getValueAt(int row, int column)
        {
            return entries.get(row);
        }

        @Override
        public boolean isCellEditable(int row, int column)
        {
            return column == Columns.actionButton.ordinal() || column == Columns.deleteButton.ordinal();
        }
    }

    private enum Columns
    {
        iconOrType,
        name,
        size,
        actionButton,
        deleteButton;

        static final Columns[] values = values();
    }
}
