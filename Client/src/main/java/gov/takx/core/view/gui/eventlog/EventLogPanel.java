package gov.takx.core.view.gui.eventlog;

import com.formdev.flatlaf.FlatClientProperties;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.eventlog.EventLogType;
import gov.takx.api.eventlog.IRaptorEventLog;
import gov.takx.api.eventlog.IRaptorEventLogListener;
import gov.takx.api.eventlog.IRaptorEventLogManager;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.IRaptorLifecycleListener;
import gov.takx.api.plugin.device.IDevicePluginDefinition;
import gov.takx.api.registry.IDevicePluginRegistry;
import gov.takx.api.timer.ITimerService;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.ui.event.MapObjectSelectedEvent;
import gov.takx.commons.constants.State;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.eventlog.EventLogTableModel;
import gov.takx.core.eventlog.RaptorEventLogManager;
import gov.takx.core.view.DeleteKeyListener;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.preferences.PreferencesNode;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.DataMonitorUtils;
import gov.takx.platform.utils.StandardTimeFormatting;
import gov.takx.platform.utils.StateIconUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumnModel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimerTask;

import static gov.takx.commons.swing.ImageUtils.SMALL_ICON_WIDTH;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 */
@PseudoBean
public class EventLogPanel extends ATitledPanel implements IRaptorLifecycleListener, PropertyChangeListener,
        IRaptorEventLogListener, TableModelListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int BATCH_UPDATE_INTERVAL_MS = 2500;

    private static final String USER_SOURCE = "USER";
    public static final String TITLE = ButtonType.EVENT_LOG.getDisplayText();
    private static final Icon defaultIcon = ImageUtils.getScaledDefaultIcon(SMALL_ICON_WIDTH);

    @Inject
    private EventLogTableModel model;

    @Inject
    private IRaptorEventLogManager eventLogManager;

    @Inject
    private ITimerService timerService;

    @Inject
    private IDevicePluginRegistry devicePluginRegistry;

    @Inject
    private StateIconUtils stateIconUtils;

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    private StandardTimeFormatting timeFormatting;

    @Inject
    @UiEventBus
    private EventBus uiEventBus;

    private final List<EventLogType> activeFilters = new ArrayList<>(Arrays.asList(EventLogType.values()));
    private RowFilter<EventLogTableModel, Integer> currentFilter;
    private boolean refreshNeeded;
    private EventLogColorPreferences eventLogColorPreferences;
    private EventLogFiltersDialog filterDialog;
    private final Set<String> listenersRegistered = new HashSet<>(); // Which MOs are we registered on?
    private String selectedKey;
    private Icon userIcon;

    /**
     * Currently unused, but retained description for posterity or eventual resurrection.
     */
    public String getDescription()
    {
        return "<html>A list of all the events recorded in the current project is displayed below. Add a user-defined event into the log by clicking the Add button. Export the entire Event Log to a .CSV file using the Export button. Filter the types of events shown in the log using the Filter function.</html>";
    }

    /**
     * Allows event log panel instance to only be instantiated once the class is accessed.
     */
    private static class EventLogPanelSingleton
    {
        public static final EventLogPanel instance = new EventLogPanel();
    }

    /**
     * Provide a singleton instance of the event log panel.
     *
     * @return PropertiesPanel
     */
    public static EventLogPanel getInstance()
    {
        return EventLogPanelSingleton.instance;
    }

    private EventLogPanel()
    {
        super(TITLE);
        initComponents();
    }

    @PostConstruct
    private void postConstruct()
    {
        clearButton.putClientProperty(FlatClientProperties.MINIMUM_WIDTH, 0);

        buttonAdd.setIcon(UIResources.ADD_ACTION_BUTTON_ICON);
        buttonExport.setIcon(UIResources.EXPORT_ACTION_BUTTON_ICON);
        filterButton.setIcon(UIResources.getActionButtonIcon(PLATFORM_ICONS, "filter.png"));

        userIcon = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "user.png");

        logTable.addMouseListener(new LogTableMouseListener());
        logTable.addKeyListener(new DeleteKeyListener());

        logTable.setModel(model);

        final TableColumnModel columnModel = logTable.getColumnModel();
        logTable.removeColumn(columnModel.getColumn(EventLogTableModel.ColumnNames.STATE.ordinal()));
        logTable.removeColumn(columnModel.getColumn(EventLogTableModel.ColumnNames.MAP_OBJECT_NAME.ordinal()));
        logTable.removeColumn(columnModel.getColumn(EventLogTableModel.ColumnNames.TARGET.ordinal()));
        logTable.removeColumn(columnModel.getColumn(EventLogTableModel.ColumnNames.TYPE.ordinal()));

        logTable.getColumnExt(EventLogTableModel.ColumnNames.DETAILS.toString()).setVisible(true);
        logTable.getColumnExt(EventLogTableModel.ColumnNames.DATE.toString()).setVisible(false);
        logTable.getColumnExt(EventLogTableModel.ColumnNames.MAP_OBJECT_FAMILY.toString()).setVisible(false);
        logTable.getColumnExt(EventLogTableModel.ColumnNames.MAP_OBJECT_TYPE.toString()).setVisible(false);
        logTable.getColumnExt(EventLogTableModel.ColumnNames.MAP_OBJECT_UNIT_ID.toString()).setVisible(false);

        columnModel.getColumn(EventLogTableModel.ColumnNames.SOURCE.ordinal()).setCellRenderer(new IconDefaultTableCellRenderer());
        columnModel.getColumn(logTable.convertColumnIndexToView(EventLogTableModel.ColumnNames.MESSAGE.ordinal())).setCellRenderer(new MessageDefaultTableCellRenderer());

        logTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Don't allow more than one row

        logTable.setAutoCreateRowSorter(true);

        initHighlighters();

        logTable.doLayout();

        model.addTableModelListener(this);

        // Ok, this little bit of craziness is all to make the scrollbar start at the bottom.  No other normal
        // attempts worked.  It seems that after all the hierarchy adjustments are made, that a pending repaint
        // comes along an causes the scroll pane to redo the layout.  So, we catch that initial resize and scroll
        // to the bottom at that point.

        scrollPane1.addComponentListener(new java.awt.event.ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                scrollToBottom();
                scrollPane1.removeComponentListener(this); // Only do it the first time
            }
        });

        uiEventBus.register(this);
    }

    private void initHighlighters()
    {
        // Construct the highlighter that will handle showing the currently selected map object-related rows
        HighlightPredicate matchSelectedMapObject = (renderer, adapter) -> {
            int modelRow = logTable.convertRowIndexToModel(adapter.row);
            final String key = getUniqueKey(modelRow);

            return key.equals(selectedKey);
        };

        Color highlightColor = logTable.getSelectionBackground();
        ColorHighlighter logHighlighter = new ColorHighlighter(matchSelectedMapObject, highlightColor, null, null, null);
        logTable.addHighlighter(logHighlighter);
    }

    private void installNeededFilters()
    {
        // Build the filter pipeline.  Only include the filters if they will actually do something.
        //
        // The filter pipeline consists of up to two filters:
        //
        // 1. One that includes rows based on the EventLogType - only installed if an event log type has been excluded
        // 2. One driven by the search text field

        List<RowFilter<EventLogTableModel, Integer>> filterList = new ArrayList<>();

        // The EventLogFilter filters against the selected event log types.
        if (activeFilters.size() != EventLogType.values().length)
        {
            // Make a new event type filter since reusing one on a JXTable causes problems (I would have to uninstall
            // it before I can reinstall it).

            filterList.add(new EventLogFilter(activeFilters));
        }

        final String searchText = searchTextField.getText();
        if (!searchText.isEmpty())
        {
            filterList.add(new MultiColumnPatternFilter(searchText, logTable));
        }

        // Construct the final filter from the list, or use null if we have no filter
        currentFilter = !filterList.isEmpty()
                ? RowFilter.andFilter(filterList)
                : null;

        logTable.setRowFilter(currentFilter);

        // After installing filters, scroll the table to the bottom
        scrollToBottom();
    }

    /**
     * Set the list of active filters.  These are the types that are to be included in the retained entries.  Meaning
     * that if the event type is not in this list, then it is not shown in the event log.
     *
     * @param activeFilters new list of active filters
     */
    public void setActiveFilters(Collection<EventLogType> activeFilters)
    {
        this.activeFilters.clear();
        this.activeFilters.addAll(activeFilters);

        installNeededFilters();
    }

    /**
     * Determine if the given event type is currently active (selected).
     *
     * @param eventType The event type to test
     * @return {@code true} if it is in the active set, {@code false} otherwise
     */
    public boolean isActiveEventFilter(EventLogType eventType)
    {
        return activeFilters.contains(eventType);
    }

    /**
     * Get the data monitor icon.
     *
     * @param moc     map object that owns the data monitor
     * @param message message containing the display name of the data monitor
     * @param state   state of the data monitor used to determine what color to make the icon
     * @return icon or {@code null} if no custom icon was found, or there was an issue creating the icon
     */
    private Icon getDataMonitorIcon(IMapObjectController moc, String message, State state)
    {
        try
        {
            //The display name of the data monitor display name is contained within brackets in the data
            //monitor state change event message. This attempts to parse out the display name, and then
            //looks up the actual data monitor from the list of available data monitors in the map object controller.

            //parse the display name out
            String dataMonitorDisplayName = message.substring(message.indexOf('[') + 1, message.indexOf(']'));

            //look through the list of data monitors and compare their display names to the name we parsed out
            for (IDataMonitor dataMonitor : moc.getDataMonitors())
            {
                //found the data monitor, now attempt to overlay its custom icon on the appropriate background
                if (dataMonitor.getDisplayName().equals(dataMonitorDisplayName))
                {
                    URL customURL = dataMonitor.getCustomIcon();

                    if (customURL != null)
                    {
                        //create a composite image of the custom icon overlayed onto the appropriate colored shape for its state
                        BufferedImage image = createOverlayedImage(ImageIO.read(customURL), state);
                        return new ImageIcon(image);
                    } else
                    {
                        return null; // no custom data monitor icon
                    }
                }
            }

            // couldn't find a matching data monitor
            return null;
        } catch (StringIndexOutOfBoundsException | IOException e)
        {
            return null; // not a data monitor message or problem reading image, return null
        }
    }

    /**
     * Create an image consisting of the custom data monitor icon image overlayed on it's status background.
     *
     * @param image    custom buffered icon image
     * @param newState state to determine the color of the status background
     * @return overlayed image
     */
    private BufferedImage createOverlayedImage(BufferedImage image, State newState)
    {
        // get the state color and shape
        Color stateColor = stateIconUtils.getColorForState(newState);
        GraphicsUtils.Shape shape = stateIconUtils.getShapeForState(newState);

        //create the image that the custom icon will be overlay
        BufferedImage statusImage = GraphicsUtils.createShape(shape, SMALL_ICON_WIDTH, 200, stateColor);

        //use the data monitor utilities to overlay the image
        DataMonitorUtils.overlayImage(statusImage, image);
        return statusImage;
    }

    @Override
    public void onClearState(IProgressMonitor progressMonitor)
    {
        if (logger.isDebugEnabled()) logger.debug("Clear State invoked on EventLogPanel");

        removePropertyChangeListeners();

        eventLogManager.removeEventLogListener(this);
        timerService.destroyTimer("eventLogTask");
    }

    @Override
    public void onLoadState(IProgressMonitor progressMonitor)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Load State invoked on EventLogPanel");
        }

        eventLogManager.addEventLogListener(this);

        installNeededFilters();

        eventLogColorPreferences = UserPreferenceEntry.EVENT_LOG_NODE.getNodeAs(EventLogColorPreferences.class);

        // Start a timer to check every xx seconds to see if the table needs to be redrawn.  Better than
        // multiple threads hitting the update request while there are lots of rows selected, etc.
        // okay, its a hack.

        timerService.scheduleAtFixedRate("eventLogTask", new RefreshTask(), BATCH_UPDATE_INTERVAL_MS, BATCH_UPDATE_INTERVAL_MS);
    }

    @Override
    public void onShutdown(IProgressMonitor progressMonitor)
    {
        uiEventBus.unregister(this);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @Subscribe
    public void selectionChanged(final MapObjectSelectedEvent event)
    {
        IMapObjectController mapObjectController = event.getMapObjectController();

        final String uniqueKey = mapObjectController != null ? mapObjectController.getMapObject().getKey() : null;
        if (logger.isDebugEnabled()) logger.debug("Selection Changed on Key: {}", uniqueKey);

        logTable.clearSelection(); // Keep the selection clear (we use highlighting)
        setSelectedKey(uniqueKey);
    }

    /**
     * Handle a change to the underlying table model.  The main point is that we want to keep the scrollbar at the
     * bottom unless the user has manually scrolled away from that position.
     *
     * @param e The change event
     */
    @Override
    public void tableChanged(TableModelEvent e)
    {
        switch (e.getType())
        {
            case TableModelEvent.INSERT:
                // On a row insert at the end of the table, stay scrolled to the bottom if we are currently there
                JScrollBar vsb = scrollPane1.getVerticalScrollBar();

                // Only move the scrollbar if it was already showing the bottom row
                // i.e. Subtract the knob size from the bar height. If the result is the current location then do it.

                boolean autoScroll = (vsb.getValue() == (vsb.getMaximum() - vsb.getVisibleAmount()));
                if (autoScroll)
                {
                    scrollToBottom();
                }
                break;

            case TableModelEvent.UPDATE:
                // On any massive change, scroll to the bottom
                if (e.getColumn() == TableModelEvent.ALL_COLUMNS)
                {
                    scrollToBottom();
                }
                break;
        }
    }

    /**
     * Scroll the event log table to show the bottom entry.
     */
    private void scrollToBottom()
    {
        // Have to do this to get it off the "current" invocation thread.  If we don't the UI components may not have
        // all updated and the cell calculations might be wrong.

        SwingUtilities.invokeLater(() -> {
            // Scroll to show the last cell
            logTable.scrollRectToVisible(logTable.getCellRect(logTable.getRowCount() - 1, 0, true));
        });
    }

    @Override
    public void logAdded(final IRaptorEventLog newLog)
    {
        // Add EventLogPanel as a property change listener to the mapObject if not already done
        final String moKey = newLog.getSource();

        if (!listenersRegistered.contains(moKey))
        {
            final var mapObject = mapObjectManager.getMapObject(moKey);
            if (mapObject != null) mapObject.addPropertyChangeListener(IMapObject.NAME, this);

            listenersRegistered.add(moKey);

            // Refresh on next timer event
            refreshNeeded = true;
        }
    }

    @Override
    public void wipeData(IMapObject mapObject)
    {
        // Safely ignore- table model will handle this for us
    }

    @Override
    public void propertyChange(final PropertyChangeEvent propertyChangeEvent)
    {
        if (propertyChangeEvent.getPropertyName().equals(IMapObject.NAME))
        {
            refresh();
        }
    }

    /**
     * Set the selected row key to the given value and repaint the table so the new selection takes effect.
     *
     * @param key Key of rows to select
     */
    private void setSelectedKey(String key)
    {
        selectedKey = key;
        logTable.repaint();
    }

    private String createToolTipText(MouseEvent e)
    {
        Point p = e.getPoint();
        int rowIndex = logTable.rowAtPoint(p);

        String toolTip = null;

        if (rowIndex >= 0)
        {
            int colIndex = logTable.columnAtPoint(p);
            int modelColumn = logTable.convertColumnIndexToModel(colIndex);
            int modelRow = logTable.convertRowIndexToModel(rowIndex);

            switch (modelColumn)
            {
                case 0 ->
                { // Source
                 // Show the name and description (if set) as the tooltip

                    final String key = model.getValueAt(modelRow, modelColumn).toString();
                    final var mapObject = mapObjectManager.getMapObject(key);

                    if (mapObject != null)
                    {
                        String description = mapObject.getDescription();
                        if (description != null && !description.trim().isEmpty())
                        {
                            toolTip = "<html>" + mapObject.getName() + "<br>" + description + "</html>";
                        } else
                        {
                            toolTip = mapObject.getName();
                        }
                    } else
                    {
                        toolTip = key;  // Can't get a map object, default to the key
                    }
                }
                case 4 -> toolTip = model.getValueAt(modelRow, EventLogTableModel.ColumnNames.DETAILS.ordinal()).toString();
                default -> toolTip = model.getValueAt(modelRow, modelColumn).toString();
            }
        }

        return toolTip;
    }

    private String getUniqueKey(int row)
    {
        Object moFamilyO = model.getValueAt(row, EventLogTableModel.ColumnNames.MAP_OBJECT_FAMILY.ordinal());
        Object moTypeO = model.getValueAt(row, EventLogTableModel.ColumnNames.MAP_OBJECT_TYPE.ordinal());
        Object moUnitIDO = model.getValueAt(row, EventLogTableModel.ColumnNames.MAP_OBJECT_UNIT_ID.ordinal());

        String moFamily = moFamilyO != null ? moFamilyO.toString() : "";
        String moType = moTypeO != null ? moTypeO.toString() : "";
        String moUnitID = moUnitIDO != null ? moUnitIDO.toString() : "";

        return KeyUtils.getUniqueKey(moFamily, moType, moUnitID);
    }

    /**
     * Unregister as a property change listener from all the map objects we have registered on.
     */
    private void removePropertyChangeListeners()
    {
        for (String key : listenersRegistered)
        {
            final var mapObject = mapObjectManager.getMapObject(key);
            if (mapObject != null) mapObject.removePropertyChangeListener(IMapObject.NAME, this);
        }

        listenersRegistered.clear(); // None now
    }

    private void userEventButtonActionPerformed()
    {
        final UserEventLogDialog dialog = new UserEventLogDialog();

        dialog.addCloseHandler(d -> {
            if (dialog.wasLogRequested())
            {
                eventLogManager.logUserEvent(new Date(), USER_SOURCE, USER_SOURCE, USER_SOURCE, dialog.getMessage(), dialog.getDetails());
            }
        });

        dialog.setVisible(true);
    }

    /**
     * Refresh the filters and repaint the table.
     */
    @InvokeOnEDT
    public void refresh()
    {
        // The only way I can find to force a re-filter is to re-install the filter
        logTable.setRowFilter(currentFilter);
        logTable.repaint();

        refreshNeeded = false; // Just did it
    }

    /**
     * Export the event log in CSV format. Log entries are requested through the {@link RaptorEventLogManager}.
     * This is the handler for the Export Log button.
     */
    private void exportEventsButtonActionPerformed()
    {
        final JFileChooser chooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        chooser.setDialogTitle("Export Event Log");
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.resetChoosableFileFilters();
        FileFilter csvType = new FileNameExtensionFilter("Comma Separated Value (.csv)", "csv");
        chooser.addChoosableFileFilter(csvType);
        int result = chooser.showSaveDialog(getParent());

        if (JFileChooser.APPROVE_OPTION == result)
        {
            int overwriteFile = JOptionPane.YES_OPTION;

            if (logger.isDebugEnabled())
            {
                logger.debug("Exporting CSV to file {}", chooser.getSelectedFile().getAbsolutePath());
            }

            File chooserFile = chooser.getSelectedFile();

            boolean addSuffix = !chooserFile.getAbsolutePath().toLowerCase().endsWith("csv");

            final File file = (addSuffix)
                    ? new File(chooserFile.getAbsolutePath() + ".csv")
                    : chooserFile;

            if (file.exists())
            {
                Object[] options = {"Yes", "No"};
                overwriteFile = AlwaysOnTopJOptionPane.showOptionDialog(chooser, "A file already exists at " + file.getAbsolutePath() + ".\nAre you sure you want to overwrite it?", "Confirm Overwrite", JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION, null, options, JOptionPane.NO_OPTION);
            }

            if (overwriteFile == JOptionPane.YES_OPTION)
            {
                SwingWorker<Object, Void> worker = new SwingWorker<>()
                {
                    @Override
                    protected Object doInBackground()
                    {
                        try (BufferedWriter out = new BufferedWriter(new FileWriter(file)))
                        {
                            out.write("Date, Family, Type, ID, Name, Message, Message Type, Details, Target");

                            IRaptorEventLog[] log = eventLogManager.getLogEntries();

                            for (IRaptorEventLog entry : log)
                            {
                                out.write("\r\n");
                                String dateString = timeFormatting.formatWithDefault(entry.getDate().getTime());

                                out.write(dateString + ",");
                                out.write(entry.getMapObjectFamily() == null ? "," : entry.getMapObjectFamily() + ",");
                                out.write(entry.getMapObjectType() == null ? "," : entry.getMapObjectType() + ",");
                                out.write(entry.getMapObjectUnitId() == null ? "," : entry.getMapObjectUnitId() + ",");
                                out.write(entry.getName() == null ? "," : entry.getName() + ",");
                                out.write(entry.getMessage() == null ? "," : entry.getMessage() + ",");
                                out.write(EventLogType.values()[entry.getType()] == null ? ","
                                        : EventLogType.values()[entry.getType()] + ",");
                                out.write(entry.getDetails() == null ? "," : entry.getDetails() + ",");
                                out.write(entry.getTarget() == null ? "" : entry.getTarget());

                                if (logger.isTraceEnabled())
                                {
                                    logger.trace("date: {} family: {} type: {} unitId: {} message: {} type: {} details: {} target: {}", dateString, entry.getMapObjectFamily(), entry.getMapObjectType(), entry.getMapObjectUnitId(), entry.getMessage(), EventLogType.values()[entry.getType()], entry.getDetails(), entry.getTarget());
                                }
                            }
                        } catch (IOException e)
                        {
                            logger.error("Error writing event log file", e);
                            AlwaysOnTopJOptionPane.showErrorDialogLater("Error export event log.\n" + e.getMessage(), "Export Error");
                        }
                        return null;
                    }

                    @Override
                    protected void done()
                    {
                        AlwaysOnTopJOptionPane.showMessageDialog("Event log entries written to " + file.getAbsolutePath(), "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                    }
                };
                worker.execute();
            }
        }
    }

    private void filterButtonMouseReleased(MouseEvent e)
    {
        if (filterDialog == null)
        {
            filterDialog = new EventLogFiltersDialog(this);
        }

        if (!filterDialog.isVisible())
        {
            //Get the current mouse click location & shift left 250 pixels
            Point pointLoc = e.getLocationOnScreen();
            pointLoc.x = Math.max(pointLoc.x - 250, 10);

            //Show the dialog
            filterDialog.setVisible(true);
            filterDialog.setLocation(pointLoc);
        }
    }

    private void searchTextFieldCaretUpdate()
    {
        // I can't find a more efficient mechanism to update a single filter, so just re-create the filters
        installNeededFilters();
    }

    private void clearButtonActionPerformed()
    {
        searchTextField.setText("");
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panel1 = new JPanel();
        buttonAdd = new JButton();
        buttonExport = new JButton();
        filterButton = new JButton();
        var panel2 = new JPanel();
        var label1 = new JLabel();
        searchTextField = new JTextField();
        clearButton = new JButton();
        scrollPane1 = new JScrollPane();
        logTable = new JXTable()
        {
            @Override
            public String getToolTipText(MouseEvent event)
            {
                return createToolTipText(event);
            }
        };
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(null);
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default, $lgap, default, $lgap, fill:default:grow"));

        //======== panel1 ========
        {
            panel1.setName("panel1");
            panel1.setLayout(new FormLayout(
                    "2*($button, $lcgap), $button",
                    "fill:default"));

            //---- buttonAdd ----
            buttonAdd.setText("Add");
            buttonAdd.setVerticalTextPosition(SwingConstants.BOTTOM);
            buttonAdd.setHorizontalTextPosition(SwingConstants.CENTER);
            buttonAdd.setIconTextGap(1);
            buttonAdd.setToolTipText("Manually enter an Event. ");
            buttonAdd.setName("buttonAdd");
            buttonAdd.addActionListener(e -> userEventButtonActionPerformed());
            panel1.add(buttonAdd, cc.xy(1, 1));

            //---- buttonExport ----
            buttonExport.setText("Export");
            buttonExport.setVerticalTextPosition(SwingConstants.BOTTOM);
            buttonExport.setHorizontalTextPosition(SwingConstants.CENTER);
            buttonExport.setIconTextGap(1);
            buttonExport.setToolTipText("Export the Event Log to a .csv file. ");
            buttonExport.setName("buttonExport");
            buttonExport.addActionListener(e -> exportEventsButtonActionPerformed());
            panel1.add(buttonExport, cc.xy(3, 1));

            //---- filterButton ----
            filterButton.setText("Filter");
            filterButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            filterButton.setHorizontalTextPosition(SwingConstants.CENTER);
            filterButton.setIconTextGap(1);
            filterButton.setToolTipText("Filter the Event Log to show only certain events.");
            filterButton.setName("filterButton");
            filterButton.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseReleased(MouseEvent e)
                {
                    filterButtonMouseReleased(e);
                }
            });
            panel1.add(filterButton, cc.xy(5, 1));
        }
        add(panel1, cc.xy(1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));

        //======== panel2 ========
        {
            panel2.setName("panel2");
            panel2.setLayout(new FormLayout(
                    "left:default, $lcgap, default:grow, $lcgap, default",
                    "default"));

            //---- label1 ----
            label1.setText("Search:");
            label1.setName("label1");
            panel2.add(label1, cc.xy(1, 1));

            //---- searchTextField ----
            searchTextField.setToolTipText("Enter a word to search");
            searchTextField.setName("searchTextField");
            searchTextField.addCaretListener(e -> searchTextFieldCaretUpdate());
            panel2.add(searchTextField, cc.xy(3, 1));

            //---- clearButton ----
            clearButton.setToolTipText("Clear Search");
            clearButton.setText("Clear");
            clearButton.setName("clearButton");
            clearButton.addActionListener(e -> clearButtonActionPerformed());
            panel2.add(clearButton, cc.xy(5, 1));
        }
        add(panel2, cc.xy(1, 3));

        //======== scrollPane1 ========
        {
            scrollPane1.setName("scrollPane1");

            //---- logTable ----
            logTable.setColumnControlVisible(true);
            logTable.setShowHorizontalLines(false);
            logTable.setPreferredScrollableViewportSize(new Dimension(100, 200));
            logTable.setName("logTable");
            scrollPane1.setViewportView(logTable);
        }
        add(scrollPane1, cc.xy(1, 5));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JButton buttonAdd;
    private JButton buttonExport;
    private JButton filterButton;
    private JTextField searchTextField;
    private JButton clearButton;
    private JScrollPane scrollPane1;
    private JXTable logTable;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    /**
     * This is a hack, obviously, like everything else in this class, to slow down
     * updates to the Event Log.  Otherwise with lots of entries you will get thrashing.
     */
    private class RefreshTask extends TimerTask
    {
        @Override
        public void run()
        {
            if (refreshNeeded) refresh();
        }
    }

    private class LogTableMouseListener extends MouseAdapter
    {
        @Override
        public void mousePressed(MouseEvent e)
        {
            showPopup(e);
        }

        @Override
        public void mouseReleased(final MouseEvent e)
        {
            showPopup(e);
        }

        @Override
        public void mouseClicked(MouseEvent e)
        {
            int row = logTable.rowAtPoint(e.getPoint());
            if (row >= 0)
            {
                int modelRow = logTable.convertRowIndexToModel(row);

                // Select the map object for this row (if any) and set the highlighter key
                // Set our selected key second since the first call can result in the MO selection being set null
                String key = getUniqueKey(modelRow);
                mapObjectManager.setSelected(key);
                setSelectedKey(key);

                if (e.getClickCount() > 1)
                {
                    String moFamily = model.getValueAt(modelRow, EventLogTableModel.ColumnNames.MAP_OBJECT_FAMILY.ordinal()).toString();

                    if (moFamily.equals(USER_SOURCE))
                    {
                        String titleMsg = model.getValueAt(modelRow, EventLogTableModel.ColumnNames.MESSAGE.ordinal()).toString();
                        String details = model.getValueAt(modelRow, EventLogTableModel.ColumnNames.DETAILS.ordinal()).toString();

                        AlwaysOnTopJOptionPane.showMessageDialog(details, titleMsg, JOptionPane.INFORMATION_MESSAGE);
                    } else
                    {
                        final var moc = mapObjectManager.getMapObjectController(key);
                        if (moc != null) moc.showPluginDialog();
                    }
                }
            }
        }

        /**
         * If this is a popup trigger and we're on a row that represents a map object, then show it's popup menu.
         *
         * @param e mouse event
         */
        private void showPopup(MouseEvent e)
        {
            if (e.isPopupTrigger() && e.getClickCount() == 1)
            {
                int rowNumber = logTable.rowAtPoint(e.getPoint());
                if (rowNumber >= 0)
                {
                    String key = getUniqueKey(logTable.convertRowIndexToModel(rowNumber));
                    setSelectedKey(key);

                    IMapObjectController controller = mapObjectManager.getMapObjectController(key);
                    if (controller != null)
                    {
                        // center the globe on the selected object
                        final IGlobeDelegate globe = controller.getGlobeManager().getLastFocusedGlobe();
                        globe.goToAndTranslateMouseEvent(controller.getLocationOnGlobe(), e);
                        controller.createPopupMenu(e);
                    }
                }
            }
        }
    }

    private class IconDefaultTableCellRenderer extends DefaultTableRenderer
    {
        private final Map<String, Icon> pluginKey2IconMap = new HashMap<>();
        private JLabel iconLabel;

        public IconDefaultTableCellRenderer()
        {
            iconLabel = new JLabel();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (component instanceof JLabel)
            {
                iconLabel = (JLabel) component;
            }

            final Icon icon;

            // NOTE: Don't try to set tooltip text here, it won't work.
            // Go see createToolTipText if you need to control the tool tips

            final int modelRowIndex = table.convertRowIndexToModel(row);
            final String name = (String) model.getValueAt(modelRowIndex, EventLogTableModel.ColumnNames.MAP_OBJECT_NAME);

            String mapObjectFamily = model.getValueAt(modelRowIndex, EventLogTableModel.ColumnNames.MAP_OBJECT_FAMILY).toString();
            // TODO: CORE-4545 should make it so that icons are directly associated with log entries. Until that is
            // TODO: completed, we use these ad-hoc conditionals to set the appropriate icon
            if (mapObjectFamily.equals(USER_SOURCE)) // USER event is not a cmo, need to add its icon here
            {
                icon = userIcon;
            } else
            {
                final var mapObject = value != null ? mapObjectManager.getMapObject(value.toString()) : null;
                if (mapObject != null)
                {
                    icon = mapObject.getSizedIcon(SMALL_ICON_WIDTH);
                } else
                {
                    logger.debug("Plugin Controller doesn't exist at this time");

                    String mapObjectType = model.getValueAt(modelRowIndex, EventLogTableModel.ColumnNames.MAP_OBJECT_TYPE).toString();
                    if (mapObjectType != null)
                    {
                        logger.debug("Looking for icon for {}", name);

                        icon = getPluginIcon(mapObjectFamily, mapObjectType);
                    } else
                    {
                        // Resort to the default icon; this is likely a plugin event, and the original plugin that
                        // produced the event no longer exists in the plugin registry.
                        icon = defaultIcon;
                    }
                }
            }
            iconLabel.setIcon(icon);
            iconLabel.setText(name);
            return iconLabel;
        }

        /**
         * Get the icon to use for a given plugin. Try to lookup the plugin; if it is found then use the icon it
         * specifies in the configuration. Cache the resulting icon result to avoid repeated lookups.
         *
         * @param family plugin family
         * @param type   plugin type
         * @return Icon to use
         */
        @Nonnull
        private Icon getPluginIcon(String family, String type)
        {
            final String key = KeyUtils.getUniqueKey(family, type);

            return pluginKey2IconMap.computeIfAbsent(key, k -> {
                final Optional<IDevicePluginDefinition> optionalDefinition = devicePluginRegistry.getPluginDefinition(family, type);
                if (optionalDefinition.isPresent())
                {
                    final BufferedImage iconImage = optionalDefinition.get().getIcon();
                    return new ImageIcon(ImageUtils.getScaledInstance(iconImage, SMALL_ICON_WIDTH));
                } else
                {
                    logger.debug("Plugin doesn't exist in this installation- using default icon");
                    return defaultIcon;
                }
            });
        }
    }

    /**
     * Custom renderer for the message column. This takes care of the different color options that a message
     * may have from being set in the user preferences. Additionally, if the message is from a data monitor state
     * change event, this will take care of the rendering the custom icon of a data monitor if it has one.
     */
    private class MessageDefaultTableCellRenderer extends DefaultTableRenderer
    {
        private JLabel messageLabel;

        public MessageDefaultTableCellRenderer()
        {
            messageLabel = new JLabel();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (component instanceof JLabel)
            {
                messageLabel = (JLabel) component;
            }

            if (logger.isTraceEnabled()) logger.trace("Value: {}", value);

            int modelRowIndex = table.convertRowIndexToModel(row);

            if (value != null && !value.toString().contains("null"))
            {
                String message = model.getValueAt(modelRowIndex, EventLogTableModel.ColumnNames.MESSAGE.ordinal()).toString();

                //get the event log type, and find the associated color for it in the user preferences, defaulting to white
                final Object eventType = model.getValueAt(modelRowIndex, EventLogTableModel.ColumnNames.TYPE.ordinal());

                Color stateColor;

                Icon icon = null;

                //if an alarm state change event or map object state chang event, get the associated color for state change from the user preferences
                if (eventType == EventLogType.ALARM_STATE_CHANGE || eventType == EventLogType.STATE_CHANGE)
                {
                    String moKey = (String) model.getValueAt(modelRowIndex, EventLogTableModel.ColumnNames.SOURCE.ordinal());
                    IMapObjectController moc = mapObjectManager.getMapObjectController(moKey);

                    String stateString = (String) model.getValueAt(modelRowIndex, EventLogTableModel.ColumnNames.STATE.ordinal());
                    State state = State.stringToState(stateString);

                    stateColor = stateIconUtils.getColorForState(state);

                    //additionally, if it's an alarm state change event, try to look up the custom icon for the data monitor
                    if (eventType == EventLogType.ALARM_STATE_CHANGE && moc != null)
                    {
                        icon = getDataMonitorIcon(moc, message, state);
                    }
                } else if (eventType == EventLogType.PLUGIN_EVENT)
                {
                    String stateString = (String) model.getValueAt(modelRowIndex, EventLogTableModel.ColumnNames.STATE.ordinal());
                    State state = State.stringToState(stateString);

                    stateColor = stateIconUtils.getColorForState(state);
                } else
                {
                    stateColor = eventLogColorPreferences.getColorForType(eventType.toString());
                }

                //set the icon and text color
                messageLabel.setIcon(icon);

                if (stateColor != null)
                {
                    messageLabel.setText(String.format("<html><font color=#%s>%s</font></html>", Integer.toHexString(stateColor.getRGB() & 0x00ffffff), message));
                } else
                {
                    messageLabel.setText(message);
                }
            }

            return messageLabel;
        }
    }

    /**
     * Interns the user preferences node defining the color choices for various event log types.
     *
     * @since 2.35
     */
    private static final class EventLogColorPreferences extends PreferencesNode
    {
        private final Map<String, Color> colorPreference;

        /**
         * Construct from the given preferences node.
         *
         * @param prefs Node to internalize
         */
        EventLogColorPreferences(PreferencesNode prefs)
        {
            super(prefs);

            // Don't store a color if the color name is "none"
            colorPreference = childrenToMap(child -> !"none".equals(getColorName(child)),
                    child -> GraphicsUtils.getColorByName(getColorName(child), Color.white));
        }

        String getColorName(PreferencesNode node)
        {
            return node.get("Color", "white");
        }

        /**
         * Get the color with which to render the given event log type.
         *
         * @param logType Event log type name
         * @return Color to use for rendering, {@code null} if preference was not specified or was "none"
         */
        Color getColorForType(String logType)
        {
            return colorPreference.get(logType);
        }
    }
}
