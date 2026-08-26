package gov.takx.core.view.gui.mapobjectmanager;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.ui.event.GlobeCreatedEvent;
import gov.takx.api.ui.event.GlobeRemovedEvent;
import gov.takx.api.ui.event.MapObjectTrackingEvent;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.view.gui.DialogHeaderPanel;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.ui.FontUtils;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;

import static gov.takx.api.coordinatesystem.CoordinateSystem.MGRS;
import static gov.takx.api.coordinatesystem.CoordinateSystem.getAngleString;
import static gov.takx.api.coordinatesystem.CoordinateSystem.getElevationString;
import static gov.takx.api.coordinatesystem.CoordinateSystem.getLocationString;

/**
 * Dialog to present the list of map objects that are being tracked due to a user
 * selecting "Keep in View" or "Center On".
 *
 */
@PseudoBean(singletonObserver = true)
public class KeepInViewDialog extends AStandardDialog implements PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The properties that will be monitored to keep the items displayed in this dialog up to date.
     */
    private static final List<String> propertiesToWatch = Arrays.asList(IMapObject.LOCATION, IMapObject.NAME, IMapObject.VISIBILITY);

    public static final String ONLY_MOVING_MO_MESSAGE = " Only applies to objects on the move.";

    /**
     * The message to display when no map objects are being tracked.
     */
    private static final String NO_MAP_OBJECTS_MESSAGE =
            "<html>No map objects have been marked to be kept in view. " +
                    "Right click a map object and select \"Keep in View\" to keep it in view." + ONLY_MOVING_MO_MESSAGE;

    /**
     * The message to display when map objects are being tracked.
     */
    private static final String HAS_MAP_OBJECTS_MESSAGE =
            "<html>The map object(s) below have been marked to be kept in view. " +
                    "Select a map object from the list to display location details." + ONLY_MOVING_MO_MESSAGE;

    /**
     * A list of all the map objects (IMapObjectControllers) currently being displayed in this dialog.
     */
    private DefaultListModel<IMapObjectController> listModel;

    @Inject
    private IGlobeManager globeManager;

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    @UiEventBus
    private EventBus uiEventBus;

    private final IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor = UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, cs -> updateLocationFromSelection());

    private int currentGlobeId = 1; // The globe for which we are showing tracked objects

    /**
     * Constructor.
     */
    public KeepInViewDialog()
    {
        super(ButtonType.KEEP_IN_VIEW.getDisplayText());
    }

    @PostConstruct
    private void postConstruct()
    {
        initComponents();

        listModel = new DefaultListModel<>();
        trackedMapObjectsList.setModel(listModel);
        trackedMapObjectsList.setCellRenderer(new MapObjectListCellRenderer());
        locationPanel.setVisible(false);
        dialogHeaderPanel.setIcon(ButtonType.KEEP_IN_VIEW.getImageUrl());

        // Wire up event processing.
        uiEventBus.register(this);

        rebuildGlobeModel();

        // Add all map objects that are currently being tracked.
        initialize(currentGlobeId);

        // Make title borders bold (can't do it directly in JFD and keep the size relative)
        FontUtils.updateTitledBorderFont(mgrsPanel, Font.BOLD);
        FontUtils.updateTitledBorderFont(degreesPanel, Font.BOLD);
    }

    @Override
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);

        if (visible)
        {
            toFront();
        }
    }

    /**
     * Build and install the current list of globes as the model for the globe combo box.
     */
    private void rebuildGlobeModel()
    {
        final int globeCount = globeManager.getGlobeCount();
        final Integer[] globes = new Integer[globeCount];

        int i = 0;
        for (Integer id : globeManager.getGlobeIds())
        {
            globes[i++] = id;
        }

        globeComboBox.setModel(new DefaultComboBoxModel<>(globes));
    }

    /**
     * Handle the creation of a new globe.  Rebuild our globe model.
     *
     * @param e The globe creation event
     */
    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void onGlobeCreated(GlobeCreatedEvent e)
    {
        rebuildGlobeModel();

        globeComboBox.setSelectedItem(currentGlobeId); // And ensure the current globe is selected
    }

    /**
     * Handle the removal of a globe.  Rebuild our globe model.
     *
     * @param e The globe removal event
     */
    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void onGlobeRemoved(GlobeRemovedEvent e)
    {
        rebuildGlobeModel();

        globeComboBox.setSelectedItem(currentGlobeId); // And ensure the current globe is selected
    }

    /**
     * Handle a tracking event.  Either add or remove the map object from our list.
     *
     * @param e Event to handle
     */
    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void onTrackingEvent(MapObjectTrackingEvent e)
    {
        // If it is not on the current globe, then we don't care
        if (e.getGlobeId() != currentGlobeId)
        {
            if (logger.isDebugEnabled()) logger.debug("Not on our globe, skipping event");
            return;
        }

        final IMapObjectController moc = e.getMapObjectController();
        final IMapObject mapObject = moc.getMapObject();

        if (e.getTrackingState() == MapObjectTrackingEvent.TrackingState.STARTED)
        {
            // Add it to the list - avoid adding it twice which could happen if both center-on and keep in view are selected
            if (!listModel.contains(moc))
            {
                listModel.addElement(moc);

                // Listen for changes to the location of this map object
                mapObject.addPropertyChangeListener(propertiesToWatch, this);
            }
        } else
        {
            // Remove it from the list if none of the tracking properties are set
            final IGlobeDelegate globe = globeManager.getGlobeDelegate(currentGlobeId);

            if (!globe.isCenterOn(mapObject) && !globe.isKeepInView(mapObject))
            {
                listModel.removeElement(moc);

                // Unregister listening for changes
                mapObject.removePropertyChangeListener(propertiesToWatch, this);
            }
        }

        updateDescription();

        // Just force a repaint of the table so any text rendering changes will take effect. This comes from the
        // change in "Center on") for an object already in the list.  The list contents aren't changed, so we need
        // to force a repaint to get the new render text to show up.
        trackedMapObjectsList.repaint();
    }

    /**
     * Handle a change in the LOCATION property of one of the map objects we are tracking.  If it is the currently
     * selected object, then update the location fields. If a map object's name changes, repaint the list.
     *
     * @param evt Event to process
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        final String propertyName = evt.getPropertyName();
        final var moc = ((IMapObject) evt.getSource()).getController();

        switch (propertyName)
        {
            case IMapObject.LOCATION:
                // Only update if the current list selection is the map object that changed
                if (moc == trackedMapObjectsList.getSelectedValue())
                {
                    updateLocationValues(moc); // update the location panel
                }
                break;
            case IMapObject.NAME:
                trackedMapObjectsList.repaint();
                break;
            case IMapObject.VISIBILITY:
                updateListForVisibility(moc, (Boolean) evt.getNewValue());
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + propertyName);
        }
    }

    /**
     * Dispose the dialog.
     *
     * @since 2.37
     */
    @SuppressWarnings("unused")
    private void onProjectClosing(@SuppressWarnings("unused") @Observes ProjectClosing event)
    {
        shutdown();
    }

    @RemoveObserver
    @Override
    public void dispose()
    {
        super.dispose();
    }

    /**
     * Prepare this dialog for a given globe by adding all map objects that are currently being tracked on that globe.
     *
     * @param globeId The Id of the globe to build from
     * @since 1.14
     */
    private void initialize(int globeId)
    {
        // Get all active security filters.
        final IGlobeDelegate globe = globeManager.getGlobeDelegate(globeId);

        listModel.clear();

        // Update the list.
        for (IMapObjectController moc : mapObjectManager.getMapObjectControllers())
        {
            final IMapObject mapObject = moc.getMapObject();

            if (globe.isKeepInView(mapObject) || globe.isCenterOn(mapObject))
            {
                listModel.addElement(moc);
                mapObject.addPropertyChangeListener(propertiesToWatch, this);

                // Install the current visibility for the MO
                updateListForVisibility(moc, mapObject.isVisible());
            }
        }

        updateDescription();
    }

    /**
     * Un-wire all event processing, remove map object property change listeners and
     * shut down this dialog.
     *
     * @since 1.14
     */
    private void shutdown()
    {
        // Un-wire event processing
        uiEventBus.unregister(this);
        coordinateSystemMonitor.dispose();

        stopListening();

        dispose();
    }

    /**
     * Go through all the MOs we are handling in the current model setup and remove our listener registrations.
     */
    private void stopListening()
    {
        // Stop listening to map object properties.
        for (int i = 0; i < listModel.size(); i++)
        {
            final IMapObjectController moc = listModel.get(i);

            moc.getMapObject().removePropertyChangeListener(propertiesToWatch, this);
        }
    }

    /**
     * Add/remove a map object from the list model based on the given visibility flag.  Also update
     * the list of invisible map objects accordingly.
     *
     * @param moc     The map object controller to process
     * @param visible Pass {@code true} if the map object is visible
     */
    private void updateListForVisibility(IMapObjectController moc, boolean visible)
    {
        if (visible)
        {
            // This map object is now visible, so re-add it to the list if not already present.
            if (!listModel.contains(moc))
            {
                listModel.addElement(moc);
            }
        } else
        {
            listModel.removeElement(moc);
        }
        updateDescription();
    }

    /**
     * Update the message displayed to the user based on whether there are map objects currently being displayed in the
     * list.
     * @since 3.3
     */
    @InvokeOnEDT
    private void updateDescription()
    {
        dialogHeaderPanel.setDescription(listModel.isEmpty() ? NO_MAP_OBJECTS_MESSAGE : HAS_MAP_OBJECTS_MESSAGE);
    }

    /**
     * The list selection has changed, fire a map object selection event.
     *
     * @param e Event to process
     */
    private void trackedMapObjectsListValueChanged(ListSelectionEvent e)
    {
        if (!e.getValueIsAdjusting())
        {
            updateLocationFromSelection();
        }
    }

    /**
     * Update the location display based on the current list selection.
     */
    @InvokeOnEDT
    private void updateLocationFromSelection()
    {
        IMapObjectController moc = trackedMapObjectsList.getSelectedValue();
        if (moc != null)
        {
            // Selection, fire selection event onward
            mapObjectManager.setSelected(moc.getMapObject().getKey());

            locationPanel.setVisible(true);
        } else
        {
            locationPanel.setVisible(false);
        }

        updateLocationValues(moc); // update the location panel
    }

    /**
     * Update the location panel values from the specified map object controller, which may be {@code null}. If so,
     * then the values are cleared.
     *
     * @param moc Controller of map object for which to display the location
     */
    private void updateLocationValues(IMapObjectController moc)
    {
        final var coordinateSystem = UserPreferenceEntry.COORDINATE_SYSTEM.getValueAs(CoordinateSystem.class);

        final ILocation loc = moc != null ? moc.getMapObject().getLocation() : null;

        if (coordinateSystem == MGRS)
        {
            updateLocationValuesForMGRS(loc);
        } else
        {
            updateLocationValuesForDegrees(loc, coordinateSystem);
        }
    }

    /**
     * Update the location components with the formatted value of the location as MGRS coordinates.
     * The MGRS panel will be made visible and the Degrees panel will be hidden, if needed.
     *  @param loc Location to format, may be {@code null} in which case the location fields are cleared
     *
     */
    private void updateLocationValuesForMGRS(ILocation loc)
    {
        // Only change visible components if needed
        if (!mgrsPanel.isVisible())
        {
            mgrsPanel.setVisible(true);
            degreesPanel.setVisible(false);
            invalidate();
            pack();
        }

        if (loc == null)
        {
            mgrsCoordinatesTextField.setText("");
        } else
        {
            final String locationString = getLocationString(loc, MGRS);
            mgrsCoordinatesTextField.setText(locationString);
            mgrsCoordinatesTextField.setToolTipText(locationString);
            mgrsCoordinatesTextField.setCaretPosition(0); // Ensure start of string is visible
        }
    }

    /**
     * Update the location components with the formatted value of the location as one of the Degree formats.
     * The Degrees panel will be made visible and the MGRS panel will be hidden, if needed.
     *
     * @param loc              Location to format, may be {@code null} in which case the location fields are cleared
     * @param coordinateSystem Coordinate system to use for formatting
     */
    private void updateLocationValuesForDegrees(ILocation loc, CoordinateSystem coordinateSystem)
    {
        // Only change visible components if needed
        if (!degreesPanel.isVisible())
        {
            degreesPanel.setVisible(true);
            mgrsPanel.setVisible(false);
            invalidate();
            pack();
        }

        if (loc == null)
        {
            latitudeTextField.setText("");
            longitudeTextField.setText("");
            elevationTextField.setText("");
        } else
        {
            latitudeTextField.setText(getAngleString(loc.getLatitude(), coordinateSystem));
            longitudeTextField.setText(getAngleString(loc.getLongitude(), coordinateSystem));
            elevationTextField.setText(loc.hasElevation() ? getElevationString(loc.getElevation()) + "m" : "");
        }
    }

    /**
     * Handle a mouse release on an entry in the map object list.  Popup the context menu on right click,
     * open the map object dialog on double click.
     *
     * @param e Mouse event to handle
     */
    private void trackedMapObjectsListMouseReleased(MouseEvent e)
    {

        if (e.getClickCount() == 2)
        {   // Handle double click
            int index = trackedMapObjectsList.locationToIndex(e.getPoint());
            IMapObjectController moc = trackedMapObjectsList.getModel().getElementAt(index);
            moc.showPluginDialog();
        } else if (e.isPopupTrigger())
        {    // Handle popup invocation

            int mouseCellIndex = trackedMapObjectsList.locationToIndex(e.getPoint());
            Rectangle bounds = trackedMapObjectsList.getCellBounds(mouseCellIndex, mouseCellIndex);

            // Select the cell that the user right-clicked on if the mouse is in that cell

            // NOTE: This (selection changing) is not consistent with Windows standards, but it is
            // consistent with the rest of TAKX

            if (bounds.contains(e.getPoint()))
            {
                int selectedIndex = trackedMapObjectsList.getSelectedIndex();
                if (selectedIndex != mouseCellIndex)
                {
                    trackedMapObjectsList.setSelectedIndex(mouseCellIndex);   // Select that cell
                }

                // Now bring up the popup menu
                IMapObjectController moc = trackedMapObjectsList.getSelectedValue();
                // center the globe on the selected object
                globeManager.getLastFocusedGlobe().goToAndTranslateMouseEvent(moc.getLocationOnGlobe(), e);
                moc.createPopupMenu(e);
            }
        }
    }

    /**
     * The user selected a globe from the combo box, re-generate our display based on that globe.
     */
    private void globeComboBoxItemStateChanged(ItemEvent e)
    {
        if (e.getStateChange() == ItemEvent.SELECTED)
        {
            stopListening();

            currentGlobeId = (Integer) e.getItem();
            initialize(currentGlobeId); // Re-initialize on the selected globe
        }
    }

    private void closeButtonActionPerformed()
    {
        dispose();
    }

    /**
     * Custom cell renderer for our map object list to show the icon and the key.
     */
    private class MapObjectListCellRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus)
        {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);

            if (value instanceof IMapObjectController)
            {
                final IGlobeDelegate globe = globeManager.getGlobeDelegate(currentGlobeId);
                final IMapObject mapObject = ((IMapObjectController) value).getMapObject();

                label.setIcon(mapObject.getSizedIcon(ImageUtils.SMALL_ICON_WIDTH));

                final String name = mapObject.getName() + (globe.isCenterOn(mapObject) ? " [Center On]" : "");
                label.setText(name);

                // Add a tooltip that shows both the name (since it might be very long) and the unique key
                label.setToolTipText("<html>" + mapObject.getName() + "<br>" + mapObject.getKey() + "</html>");
            }
            return label;
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        dialogHeaderPanel = new DialogHeaderPanel();
        var contentPanel = new JPanel();
        var globeSelectionPanel = new JPanel();
        var globeLabel = new JLabel();
        globeComboBox = new JComboBox<>();
        var mapObjectsAndLocationPanel = new JPanel();
        var mapObjectsPanel = new JPanel();
        var trackedMapObjectsScrollPane = new JScrollPane();
        trackedMapObjectsList = new JList<>();
        locationPanel = new JPanel();
        mgrsPanel = new JPanel();
        var coordinatesLabel = new JLabel();
        mgrsCoordinatesTextField = new JTextField();
        degreesPanel = new JPanel();
        var latitudeLabel = new JLabel();
        latitudeTextField = new JTextField();
        var longitudeLabel = new JLabel();
        longitudeTextField = new JTextField();
        var elevationLabel = new JLabel();
        elevationTextField = new JTextField();
        var buttonBar = new JPanel();
        var closeButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setMinimumSize(new Dimension(300, 400));
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //---- dialogHeaderPanel ----
            dialogHeaderPanel.setName("dialogHeaderPanel");
            dialogPane.add(dialogHeaderPanel, BorderLayout.NORTH);

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "default, $lgap, fill:default:grow"));

                //======== globeSelectionPanel ========
                {
                    globeSelectionPanel.setName("globeSelectionPanel");
                    globeSelectionPanel.setLayout(new FormLayout(
                            "default, $lcgap, default",
                            "default"));

                    //---- globeLabel ----
                    globeLabel.setText("Globe to show:");
                    globeLabel.setLabelFor(globeComboBox);
                    globeLabel.putClientProperty("FlatLaf.styleClass", "large");
                    globeLabel.setName("globeLabel");
                    globeSelectionPanel.add(globeLabel, cc.xy(1, 1));

                    //---- globeComboBox ----
                    globeComboBox.putClientProperty("FlatLaf.styleClass", "large");
                    globeComboBox.setName("globeComboBox");
                    globeComboBox.addItemListener(e -> globeComboBoxItemStateChanged(e));
                    globeSelectionPanel.add(globeComboBox, cc.xy(3, 1));
                }
                contentPanel.add(globeSelectionPanel, cc.xy(1, 1));

                //======== mapObjectsAndLocationPanel ========
                {
                    mapObjectsAndLocationPanel.setName("mapObjectsAndLocationPanel");
                    mapObjectsAndLocationPanel.setLayout(new FormLayout(
                            "default:grow",
                            "fill:default:grow, $lgap, default"));

                    //======== mapObjectsPanel ========
                    {
                        mapObjectsPanel.setName("mapObjectsPanel");
                        mapObjectsPanel.setLayout(new FormLayout(
                                "default:grow",
                                "fill:default:grow"));

                        //======== trackedMapObjectsScrollPane ========
                        {
                            trackedMapObjectsScrollPane.setName("trackedMapObjectsScrollPane");

                            //---- trackedMapObjectsList ----
                            trackedMapObjectsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                            trackedMapObjectsList.setVisibleRowCount(-1);
                            trackedMapObjectsList.putClientProperty("FlatLaf.styleClass", "large");
                            trackedMapObjectsList.setName("trackedMapObjectsList");
                            trackedMapObjectsList.addListSelectionListener(e -> trackedMapObjectsListValueChanged(e));
                            trackedMapObjectsList.addMouseListener(new MouseAdapter()
                            {
                                @Override
                                public void mouseReleased(MouseEvent e)
                                {
                                    trackedMapObjectsListMouseReleased(e);
                                }
                            });
                            trackedMapObjectsScrollPane.setViewportView(trackedMapObjectsList);
                        }
                        mapObjectsPanel.add(trackedMapObjectsScrollPane, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));
                    }
                    mapObjectsAndLocationPanel.add(mapObjectsPanel, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

                    //======== locationPanel ========
                    {
                        locationPanel.setName("locationPanel");
                        locationPanel.setLayout(new FormLayout(
                                "default:grow",
                                "2*(default)"));

                        //======== mgrsPanel ========
                        {
                            mgrsPanel.setBorder(new TitledBorder("Location"));
                            mgrsPanel.setVisible(false);
                            mgrsPanel.setName("mgrsPanel");
                            mgrsPanel.setLayout(new FormLayout(
                                    "default, $lcgap, default:grow",
                                    "default"));

                            //---- coordinatesLabel ----
                            coordinatesLabel.setText("Coord:");
                            coordinatesLabel.setName("coordinatesLabel");
                            mgrsPanel.add(coordinatesLabel, cc.xy(1, 1));

                            //---- mgrsCoordinatesTextField ----
                            mgrsCoordinatesTextField.setEditable(false);
                            mgrsCoordinatesTextField.setName("mgrsCoordinatesTextField");
                            mgrsPanel.add(mgrsCoordinatesTextField, cc.xy(3, 1));
                        }
                        locationPanel.add(mgrsPanel, cc.xy(1, 1));

                        //======== degreesPanel ========
                        {
                            degreesPanel.setBorder(new TitledBorder("Location"));
                            degreesPanel.setName("degreesPanel");
                            degreesPanel.setLayout(new FormLayout(
                                    "right:default, $lcgap, default:grow",
                                    "2*(default, $lgap), default"));

                            //---- latitudeLabel ----
                            latitudeLabel.setText("Latitude:");
                            latitudeLabel.setName("latitudeLabel");
                            degreesPanel.add(latitudeLabel, cc.xy(1, 1));

                            //---- latitudeTextField ----
                            latitudeTextField.setEditable(false);
                            latitudeTextField.setName("latitudeTextField");
                            degreesPanel.add(latitudeTextField, cc.xy(3, 1));

                            //---- longitudeLabel ----
                            longitudeLabel.setText("Longitude:");
                            longitudeLabel.setName("longitudeLabel");
                            degreesPanel.add(longitudeLabel, cc.xy(1, 3));

                            //---- longitudeTextField ----
                            longitudeTextField.setEditable(false);
                            longitudeTextField.setName("longitudeTextField");
                            degreesPanel.add(longitudeTextField, cc.xy(3, 3));

                            //---- elevationLabel ----
                            elevationLabel.setText("Elevation:");
                            elevationLabel.setName("elevationLabel");
                            degreesPanel.add(elevationLabel, cc.xy(1, 5));

                            //---- elevationTextField ----
                            elevationTextField.setEditable(false);
                            elevationTextField.setName("elevationTextField");
                            degreesPanel.add(elevationTextField, cc.xy(3, 5));
                        }
                        locationPanel.add(degreesPanel, cc.xy(1, 2));
                    }
                    mapObjectsAndLocationPanel.add(locationPanel, cc.xy(1, 3));
                }
                contentPanel.add(mapObjectsAndLocationPanel, cc.xy(1, 3, CellConstraints.DEFAULT, CellConstraints.FILL));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $lcgap, $button",
                        "bottom:default"));

                //---- closeButton ----
                closeButton.setText("Close");
                closeButton.setName("closeButton");
                closeButton.addActionListener(e -> closeButtonActionPerformed());
                buttonBar.add(closeButton, cc.xy(3, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private DialogHeaderPanel dialogHeaderPanel;
    private JComboBox<Integer> globeComboBox;
    private JList<IMapObjectController> trackedMapObjectsList;
    private JPanel locationPanel;
    private JPanel mgrsPanel;
    private JTextField mgrsCoordinatesTextField;
    private JPanel degreesPanel;
    private JTextField latitudeTextField;
    private JTextField longitudeTextField;
    private JTextField elevationTextField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
