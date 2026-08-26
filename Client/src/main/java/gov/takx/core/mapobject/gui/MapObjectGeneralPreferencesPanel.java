package gov.takx.core.mapobject.gui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.maps.Marker;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ExtendedLocation;
import gov.takx.api.coordinatesystem.IExtendedLocation;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.mapobject.IShapeRangeFanMapObject;
import gov.takx.api.mapobject.MapObjectGroupNames;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.ui.event.GlobeCreatedEvent;
import gov.takx.api.ui.event.GlobeRemovedEvent;
import gov.takx.api.ui.event.MapObjectTrackingEvent;
import gov.takx.commons.swing.IPanelStateListener;
import gov.takx.commons.swing.SplitButton;
import gov.takx.commons.swing.documents.LimitedDocument;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.units.AltitudeUnits;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MapObjectGeneralPreferencesPanel extends MapObjectMainPreferencesPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    @UiEventBus
    private EventBus uiEventBus;

    private final IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor =
            UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, this::coordinateSystemChanged);
    private final IPreferenceMonitor<AltitudeUnits> altitudeUnitsMonitor =
            UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS.newMonitor(AltitudeUnits.class, u -> setLocationField());

    // This map holds the center on settings keyed by globe id
    private final Map<Integer, Boolean> centerOnByGlobe = new HashMap<>();

    // This map gets us from a globe Id to the checkbox controlling the center-on state for that globe
    private final Map<Integer, AbstractButton> centerOnControlsByGlobe = new HashMap<>();

    private JComponent currentCenterOnControl; // Which control is currently in use?

    private boolean allowDrag;
    private boolean snapIconToSurface;
    private boolean allowDeviceContainment;
    private ILocation origLocation;
    private boolean locationTextChanged;
    private boolean useCotType;

    private final java.util.List<String> propertyList =
            Arrays.asList(IMapObject.NAME, IMapObject.LOCATION, IMapObject.REMARKS, IMapObject.DESCRIPTION);

    public static final String TITLE = "General";

    public MapObjectGeneralPreferencesPanel(IMapObjectController mapObjectController, IPanelStateListener panelListener)
    {
        super(TITLE, mapObjectController, panelListener);

        initComponents();
        buttonEdit.setIcon(UIResources.LOCKED_ICON);

        currentCenterOnControl = btnCenterOn; // This is the initial control as setup in initComponents

        final IMapObject cmo = getMapObject();

        textFieldName.setDocument(new LimitedDocument(255));
        textFieldName.setText(cmo.getTitle());
        unitIdField.setText(mapObjectController.getMapObject().getUnitID());
        descriptionTextArea.setDocument(new LimitedDocument(255));
        descriptionTextArea.setText(cmo.getDescription());
        textAreaInfoText.setText(cmo.getRemarks());

        origLocation = cmo.getLocation();
        setLocationField();

        allowDrag = cmo.isAllowDrag();
        allowDragCheckBox.setSelected(allowDrag);

        allowDeviceContainment = cmo.isAllowDeviceToBeContainer();

        // If it's a shape, then remove the "allow drag" checkbox, since the proper control is really "editable".
        if (cmo instanceof IShapeMapObject)
        {
            allowDragCheckBox.setVisible(false);
            unitIdField.setVisible(false);
            unitIdLabel.setVisible(false);
        } else
        {
            allowDragCheckBox.setVisible(true);
        }

        if (cmo.isDevice() || cmo.getGroupName().equalsIgnoreCase(MapObjectGroupNames.FILES_GROUP))
        {
            allowContainmentCheckBox.setSelected(allowDeviceContainment);
            allowContainmentCheckBox.setVisible(true);
        } else
        {
            allowContainmentCheckBox.setVisible(false);
        }

        if (cmo.supportsSnapIconToSurface())
        {
            snapIconToSurface = cmo.isSnapIconToSurface();
            snapIconToSurfaceCheckBox.setSelected(snapIconToSurface);
            snapIconToSurfaceCheckBox.setVisible(true);
        } else
        {
            snapIconToSurfaceCheckBox.setVisible(false);
        }

        if (cmo instanceof Marker)
        {
            useCotType = cmo.getAttributeSet().getBooleanAttribute(StandardAttributeNames.USE_COT_TYPE_ICON, false);
            useCotIconCheckBox.setSelected(useCotType);
        } else
        {
            useCotIconCheckBox.setVisible(false);
        }

        // Note: our property change/event listeners won't be registered until we are visible
        // See thisHierarchyChanged().
    }

    @PostConstruct
    private void postConstruct()
    {
        initCenterOnControls(); // Special handling for center on based upon whether we have multiple globes or not
    }

    @Override
    public void releaseResources()
    {
        coordinateSystemMonitor.dispose();
        altitudeUnitsMonitor.dispose();

        super.releaseResources();
    }

    /**
     * Prepare the center on control, either a simple checkbox if we have one globe or a menu of checkboxes
     * if we have more than one globe.
     */
    private void initCenterOnControls()
    {
        final IApplicationGlobe globe = globeManager.getPrimaryGlobeDelegate();

        pnlProperties.remove(currentCenterOnControl); // Remove the existing control

        centerOnByGlobe.clear(); // Clear the maps
        centerOnControlsByGlobe.clear();

        if (globe.getGlobeCount() > 1)
        {
            btnCenterOn.setPopupMenu(createCenterOnSubMenu());
            currentCenterOnControl = btnCenterOn;
        } else
        {
            boolean centerOn = globe.isCenterOn(mapObjectController.getMapObject());
            centerOnCheckBox.setSelected(centerOn);

            centerOnByGlobe.put(IApplicationGlobe.PRIMARY_GLOBE_ID, centerOn);
            centerOnControlsByGlobe.put(IApplicationGlobe.PRIMARY_GLOBE_ID, centerOnCheckBox);

            currentCenterOnControl = centerOnCheckBox;
        }

        // And place the control into the containing panel
        final CellConstraints cc = new CellConstraints();
        pnlProperties.add(currentCenterOnControl, cc.xy(1, 1));
    }

    /**
     * Recreate the controls used to manage the center on across the globes.  Force a re-validation and repaint.
     */
    private void recreateCenterOnControls()
    {
        initCenterOnControls();

        revalidate();
        repaint();
    }

    /**
     * Handle the creation of a new globe.  Instantiate the proper UI controls.
     *
     * @param e The globe creation event
     */
    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void onGlobeCreated(GlobeCreatedEvent e)
    {
        recreateCenterOnControls();
    }

    /**
     * Handle the removal of a globe.  Instantiate the proper UI controls.
     *
     * @param e The globe removal event
     */
    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void onGlobeRemoved(GlobeRemovedEvent e)
    {
        recreateCenterOnControls();
    }

    /**
     * Handle a tracking event.  Depending on the event, set/clear the center on UI control for the appropriate globe.
     *
     * @param e Event to handle
     */
    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void onTrackingEvent(MapObjectTrackingEvent e)
    {
        // If it's not a "center on" event or it's not aimed at our MO, then we don't care about it

        if (e.getTrackingType() != MapObjectTrackingEvent.TrackingType.CENTER_ON
                || e.getMapObjectController() != getMapObjectController())
        {
            return;
        }

        final int globeId = e.getGlobeId();
        final AbstractButton button = centerOnControlsByGlobe.get(globeId);

        if (button != null)
        {
            // Set the selected tate based upon the start/stop indicated in the event
            button.setSelected(e.getTrackingState() == MapObjectTrackingEvent.TrackingState.STARTED);
        } else
        {
            logger.error("Failed to locate Center-On control for globe: {}", globeId);
        }
    }

    @Override
    public URL getIcon()
    {
        return UIResources.getUrl(PLATFORM_ICONS, "settings.png");
    }

    @Override
    public String validateContents()
    {
        String feedbackMessage = null;

        String name = textFieldName.getText().trim();
        if (name.isEmpty())
        {
            feedbackMessage = "Name must not be empty.";
        } else if (mapObjectManager.nameExists(name) && !name.equals(getMapObjectController().getName()))
        {
            feedbackMessage = "An object named '" + name + "' already exists. Please select a unique name.";
        }
        return feedbackMessage;
    }

    @Override
    protected void applyActionPerformed()
    {
        final IMapObject cmo = getMapObject();

        final String newName = textFieldName.getText().trim();
        if (cmo instanceof IMarkerMapObject) cmo.setTitle(newName);
        cmo.setName(newName);

        buttonEdit.setIcon(UIResources.LOCKED_ICON);
        locationTextField.setEditable(false);
        buttonEdit.setEnabled(true);

        String description = descriptionTextArea.getText().trim();
        cmo.setDescription(description);

        String itext = textAreaInfoText.getText().trim();
        cmo.setRemarks(itext);

        final String locationText = locationTextField.getText().trim();
        final ILocation location = CoordinateSystem.computeLatLonFromString(locationText);

        if (locationTextChanged && (origLocation == null || !origLocation.equals(location)))
        {
            if (location == null)
            {
                // If the original cmo had no location and the text field is empty, then don't report this as an error.
                // If the text field is not empty, then report anyway since the user is trying to specify a new location
                // for the object.
                if (mapObject.hasLocation() || !locationText.isEmpty())
                {
                    JOptionPane.showMessageDialog(this, "Invalid Coordinates", "Invalid Coordinates", JOptionPane.ERROR_MESSAGE);
                }
            } else
            {
                IExtendedLocation extendedLocation = new ExtendedLocation(location, true, false);
                getMapObjectController().sendLocationChangeMessage(extendedLocation);
                locationTextChanged = false;
                origLocation = location;
            }
        }

        applyCenterOn(); // Special handling for center on

        // PLUGIN-969: range fans should have the same allowDrag value as ordinary map objects
        if (cmo instanceof IShapeMapObject shapeMapObject && !(cmo instanceof IShapeRangeFanMapObject))
        {
            allowDrag = shapeMapObject.isEditable();
        }
        cmo.setAllowDrag(allowDrag);
        cmo.setSnapIconToSurface(snapIconToSurface);
        cmo.setAllowDeviceToBeContainer(allowDeviceContainment);
        if (cmo instanceof Marker)
        {
            useCotType = useCotIconCheckBox.isSelected();
            cmo.getAttributeSet().setAttribute(StandardAttributeNames.USE_COT_TYPE_ICON, useCotType);
            if (useCotType) cmo.setMetaString(StandardAttributeNames.LAST_TEAM_ICON_AND_COLOR, ""); // needed to allow toggle back to skittle icon
        }
    }

    /**
     * Apply the current center-on configuration to the globe(s).  Install the center on setting according to each
     * globe id in the map.
     */
    private void applyCenterOn()
    {
        final IMapObject mo = getMapObject();

        for (Map.Entry<Integer, Boolean> entry : centerOnByGlobe.entrySet())
        {
            final Integer globeId = entry.getKey();
            final Boolean centerOn = entry.getValue();
            final IGlobeDelegate globe = globeManager.getGlobeDelegate(globeId);

            if (centerOn)
            {
                if (logger.isDebugEnabled()) logger.debug("Start centerOn on globe: {}", globeId);

                globe.startCenterOn(mo);
            } else if (globe.isCenterOn(mo))
            {
                if (logger.isDebugEnabled()) logger.debug("Stop centerOn on globe: {}", globeId);

                globe.stopCenterOn();
            }
        }
    }

    private void coordinateSystemChanged(CoordinateSystem coordinateSystem)
    {
        setLocationField();
    }

    @InvokeOnEDT
    private void setLocationField()
    {
        if (origLocation != null)
        {
            locationTextField.setText(CoordinateSystem.getLocationString(origLocation,
                    coordinateSystemMonitor.getCurrentValue(), origLocation.hasElevation(), altitudeUnitsMonitor.getCurrentValue()));
        }
    }

    private void locationInputKeyTyped(KeyEvent e)
    {
        if (!e.isActionKey())
        {
            locationTextChanged = true;
            setPanelDirty();
        }
    }

    private void locationTextFieldMousePressed()
    {
        locationTextChanged = true;
        setPanelDirty();
    }

    private void descriptionInputKeyTyped(KeyEvent e)
    {
        if (!e.isActionKey()) setPanelDirty();
    }

    private void centerOnCheckBoxItemStateChanged(ItemEvent e)
    {
        centerOnByGlobe.put(IApplicationGlobe.PRIMARY_GLOBE_ID, (e.getStateChange() == ItemEvent.SELECTED));
        setPanelDirty();
    }

    private void allowDragCheckBoxItemStateChanged(ItemEvent e)
    {
        allowDrag = (e.getStateChange() == ItemEvent.SELECTED);
        setPanelDirty();
    }

    private void snapIconToSurfaceCheckBoxItemStateChanged(ItemEvent e)
    {
        snapIconToSurface = (e.getStateChange() == ItemEvent.SELECTED);
        setPanelDirty();
    }

    private void textFieldNameKeyTyped(KeyEvent e)
    {
        if (!e.isActionKey()) setPanelDirty();
    }

    private void textAreaInfoTextKeyTyped(KeyEvent e)
    {
        if (!e.isActionKey()) setPanelDirty();
    }

    private void allowContainmentCheckBoxItemStateChanged(ItemEvent e)
    {
        allowDeviceContainment = (e.getStateChange() == ItemEvent.SELECTED);
        setPanelDirty();
    }

    private void useCotIconCheckBoxItemStateChanged(ItemEvent e)
    {
        useCotType = (e.getStateChange() == ItemEvent.SELECTED);
        setPanelDirty();
    }

    private void buttonEditActionPerformed()
    {
        buttonEdit.setIcon(UIResources.UNLOCKED_ICON);
        buttonEdit.setEnabled(false);
        locationTextField.setEditable(true);
        locationTextField.requestFocus();
        setPanelDirty();
    }

    @Override
    @InvokeOnEDT
    public void propertyChange(final PropertyChangeEvent propertyChangeEvent)
    {
        if (!locationTextField.isEditable())
        {
            final IMapObject mo = getMapObject();
            final String propertyName = propertyChangeEvent.getPropertyName();

            switch (propertyName)
            {
                case IMapObject.NAME -> textFieldName.setText(mo.getName());
                case IMapObject.LOCATION ->
                {
                    origLocation = mo.getLocation();
                    setLocationField();
                }
                case IMapObject.DESCRIPTION -> descriptionTextArea.setText(mo.getDescription());
                case IMapObject.REMARKS -> textAreaInfoText.setText(mo.getRemarks());
                case StandardAttributeNames.USE_COT_TYPE_ICON -> useCotIconCheckBox.setSelected(mo.getAttributeSet().getBooleanAttribute(StandardAttributeNames.USE_COT_TYPE_ICON, false));
                default -> logger.warn("Unhandled property change: {}", propertyChangeEvent);
            }
        }
    }

    /**
     * Construct a sub-menu that contains "center on" controls for each globe.
     */
    private JPopupMenu createCenterOnSubMenu()
    {
        JPopupMenu centerOnPopupMenu = new JPopupMenu();

        final IApplicationGlobe primaryGlobe = globeManager.getPrimaryGlobeDelegate();

        // Get the list of current globe ids sorted
        final Set<Integer> idSet = primaryGlobe.getGlobeIds();
        final Integer[] ids = idSet.toArray(new Integer[0]);

        Arrays.sort(ids);

        final IMapObject mo = getMapObject();

        // Fill the menu with checkbox menu items for each globe
        for (final Integer globeId : ids)
        {
            if (logger.isDebugEnabled()) logger.debug("Make menu item for globe: {}", globeId);

            final String menuItemText = "on Globe " + globeId;
            final JMenuItem menuItem = new JCheckBoxMenuItem(menuItemText);

            final IGlobeDelegate globe = globeManager.getGlobeDelegate(globeId);
            menuItem.setSelected(globe.isCenterOn(mo));

            final Action action = new AbstractAction(menuItemText)
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    centerOnByGlobe.put(globeId, menuItem.isSelected());
                    setPanelDirty();
                }
            };

            menuItem.setAction(action);

            centerOnPopupMenu.add(menuItem);
            centerOnControlsByGlobe.put(globeId, menuItem);
        }

        return centerOnPopupMenu;
    }

    /**
     * We need to watch for the hierarchy "showing state" to change.  When we are no longer showing, we need to
     * cleanup our listener registrations.  Icky, but there is no better way given our current APIs.
     * When we are visible, add our listeners.
     *
     * @param e Hierarchy event
     */
    private void thisHierarchyChanged(HierarchyEvent e)
    {
        // Detect when our showing state changes
        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0)
        {
            if (isShowing())
            {
                registerListeners();
            } else
            {
                unregisterListeners();
            }
        }
    }

    /**
     * Register the listeners on the MapObject and event bus
     */
    private void registerListeners()
    {
        // Add our listener when we become visible.
        getMapObject().addPropertyChangeListener(propertyList, this);

        uiEventBus.register(this);
    }

    /**
     * Cleanup our listener registrations.
     */
    private void unregisterListeners()
    {
        if (logger.isDebugEnabled()) logger.debug("Cleaning up");

        getMapObject().removePropertyChangeListener(propertyList, this);

        if (uiEventBus.isRegistered(this)) uiEventBus.unregister(this);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var label1 = new JLabel();
        textFieldName = new JTextField();
        unitIdLabel = new JLabel();
        unitIdField = new JTextField();
        var label2 = new JLabel();
        var descriptionScrollPane = new JScrollPane();
        descriptionTextArea = new JTextArea();
        var label3 = new JLabel();
        var locationPanel = new JPanel();
        locationTextField = new JTextField();
        buttonEdit = new JButton();
        pnlProperties = new JPanel();
        btnCenterOn = new SplitButton();
        allowDragCheckBox = new JCheckBox();
        snapIconToSurfaceCheckBox = new JCheckBox();
        useCotIconCheckBox = new JCheckBox();
        allowContainmentCheckBox = new JCheckBox();
        var label4 = new JLabel();
        var scrollPane1 = new JScrollPane();
        textAreaInfoText = new JTextArea();
        centerOnCheckBox = new JCheckBox();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        addHierarchyListener(e -> thisHierarchyChanged(e));
        setLayout(new FormLayout(
                "default, $lcgap, default:grow",
                "2*(default, $lgap), fill:default, $lgap, top:default, $lgap, fill:default:grow"));

        //---- label1 ----
        label1.setText("Name:");
        add(label1, cc.xy(1, 1));

        //---- textFieldName ----
        textFieldName.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyTyped(KeyEvent e)
            {
                textFieldNameKeyTyped(e);
            }
        });
        add(textFieldName, cc.xy(3, 1));

        //---- unitIdLabel ----
        unitIdLabel.setText("Unit ID:");
        add(unitIdLabel, cc.xy(1, 3));

        //---- unitIdField ----
        unitIdField.setEditable(false);
        add(unitIdField, cc.xy(3, 3));

        //---- label2 ----
        label2.setText("Description:");
        add(label2, cc.xy(1, 5, CellConstraints.DEFAULT, CellConstraints.TOP));

        //======== descriptionScrollPane ========
        {

            //---- descriptionTextArea ----
            descriptionTextArea.addKeyListener(new KeyAdapter()
            {
                @Override
                public void keyTyped(KeyEvent e)
                {
                    descriptionInputKeyTyped(e);
                }
            });
            descriptionScrollPane.setViewportView(descriptionTextArea);
        }
        add(descriptionScrollPane, cc.xy(3, 5));

        //---- label3 ----
        label3.setText("Location:");
        add(label3, cc.xy(1, 7, CellConstraints.DEFAULT, CellConstraints.TOP));

        //======== locationPanel ========
        {
            locationPanel.setLayout(new FormLayout(
                    "default:grow, $lcgap, default",
                    "default, $rgap, top:default"));

            //---- locationTextField ----
            locationTextField.setToolTipText("Click on the lock button to unlock for editing");
            locationTextField.setEditable(false);
            locationTextField.addKeyListener(new KeyAdapter()
            {
                @Override
                public void keyTyped(KeyEvent e)
                {
                    locationInputKeyTyped(e);
                }
            });
            locationTextField.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    locationTextFieldMousePressed();
                }
            });
            locationPanel.add(locationTextField, cc.xy(1, 1));

            //---- buttonEdit ----
            buttonEdit.setToolTipText("Lock or Unlock Editing Ability");
            buttonEdit.addActionListener(e -> buttonEditActionPerformed());
            locationPanel.add(buttonEdit, cc.xy(3, 1));

            //======== pnlProperties ========
            {
                pnlProperties.setLayout(new FormLayout(
                        "left:default, 2*($lcgap, default)",
                        "fill:default:grow, 2*($lgap, default)"));

                //---- btnCenterOn ----
                btnCenterOn.setText("Center On");
                btnCenterOn.setToolTipText("Press for a menu of globe options");
                pnlProperties.add(btnCenterOn, cc.xy(1, 1));

                //---- allowDragCheckBox ----
                allowDragCheckBox.setText("Allow Drag");
                allowDragCheckBox.setSelected(true);
                allowDragCheckBox.addItemListener(e -> allowDragCheckBoxItemStateChanged(e));
                pnlProperties.add(allowDragCheckBox, cc.xy(3, 1, CellConstraints.LEFT, CellConstraints.CENTER));

                //---- snapIconToSurfaceCheckBox ----
                snapIconToSurfaceCheckBox.setText("Snap Icon To Surface");
                snapIconToSurfaceCheckBox.addItemListener(e -> snapIconToSurfaceCheckBoxItemStateChanged(e));
                pnlProperties.add(snapIconToSurfaceCheckBox, cc.xy(1, 3, CellConstraints.LEFT, CellConstraints.CENTER));

                //---- useCotIconCheckBox ----
                useCotIconCheckBox.setText("Use CoT Icon");
                useCotIconCheckBox.addItemListener(e -> useCotIconCheckBoxItemStateChanged(e));
                pnlProperties.add(useCotIconCheckBox, cc.xy(3, 3));

                //---- allowContainmentCheckBox ----
                allowContainmentCheckBox.setText("Allow Containment");
                allowContainmentCheckBox.addItemListener(e -> allowContainmentCheckBoxItemStateChanged(e));
                pnlProperties.add(allowContainmentCheckBox, cc.xy(1, 5));
            }
            locationPanel.add(pnlProperties, cc.xywh(1, 3, 3, 1));
        }
        add(locationPanel, cc.xy(3, 7));

        //---- label4 ----
        label4.setText("Remarks:");
        add(label4, cc.xy(1, 9, CellConstraints.DEFAULT, CellConstraints.TOP));

        //======== scrollPane1 ========
        {

            //---- textAreaInfoText ----
            textAreaInfoText.addKeyListener(new KeyAdapter()
            {
                @Override
                public void keyTyped(KeyEvent e)
                {
                    textAreaInfoTextKeyTyped(e);
                }
            });
            scrollPane1.setViewportView(textAreaInfoText);
        }
        add(scrollPane1, cc.xy(3, 9));

        //---- centerOnCheckBox ----
        centerOnCheckBox.setText("Center On");
        centerOnCheckBox.addItemListener(e -> centerOnCheckBoxItemStateChanged(e));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTextField textFieldName;
    private JLabel unitIdLabel;
    private JTextField unitIdField;
    private JTextArea descriptionTextArea;
    private JTextField locationTextField;
    private JButton buttonEdit;
    private JPanel pnlProperties;
    private SplitButton btnCenterOn;
    private JCheckBox allowDragCheckBox;
    private JCheckBox snapIconToSurfaceCheckBox;
    private JCheckBox useCotIconCheckBox;
    private JCheckBox allowContainmentCheckBox;
    private JTextArea textAreaInfoText;
    private JCheckBox centerOnCheckBox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
