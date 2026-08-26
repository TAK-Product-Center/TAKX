package gov.takx.core.mapobject.gui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.maps.Marker;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ExtendedLocation;
import gov.takx.api.coordinatesystem.IExtendedLocation;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.mapobject.ICoreMapObjectManager;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.mapobject.IShapeRangeFanMapObject;
import gov.takx.api.mapobject.MapObjectGroupNames;
import gov.takx.api.ui.DevicePanelMetadataKeys;
import gov.takx.commons.swing.ChipField;
import gov.takx.commons.swing.CombinedDocumentListener;
import gov.takx.commons.swing.FormLayoutUtils;
import gov.takx.commons.swing.IPanelStateListener;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.swing.documents.LimitedDocument;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.ui.location.LocationField;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;

import java.awt.Dimension;
import java.awt.event.HierarchyEvent;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static gov.takx.commons.swing.ImageUtils.SMALL_ICON_WIDTH;
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
    private ICoreMapObjectManager mapObjectManager;

    private boolean updating;
    private boolean allowDrag;
    private boolean snapIconToSurface;
    private boolean allowDeviceContainment;
    private boolean useCotType;

    private final boolean immediate;
    private final List<String> propertyList = Arrays.asList(
            IMapObject.NAME,
            IMapObject.LOCATION,
            IMapObject.REMARKS,
            IMapObject.DESCRIPTION,
            IIconMapObject.CONTAINED_ADDED,
            IIconMapObject.CONTAINED_REMOVED,
            IIconMapObject.ALLOW_DEVICE_CONTAINMENT);

    public static final String TITLE = "General";

    public MapObjectGeneralPreferencesPanel(IMapObjectController mapObjectController, IPanelStateListener panelListener, boolean immediate)
    {
        super(TITLE, mapObjectController, panelListener);
        this.immediate = immediate;

        initComponents();

        updating = true;
        containedField.setTextProvider(IMapObject::getName);
        containedField.setIconProvider(mo -> mo.getSizedIcon(SMALL_ICON_WIDTH));

        final IMapObject cmo = getMapObject();

        centerOnField.setSelectedGlobeIds(cmo, immediate);

        iconLabel.setIcon(cmo.getSizedIcon(ImageUtils.DEFAULT_ICON_WIDTH));
        textFieldName.setDocument(new LimitedDocument(255));
        textFieldName.setText(cmo.getTitle());
        textFieldName.getDocument().addDocumentListener((CombinedDocumentListener) e -> setPanelDirty());
        unitIdField.setText(mapObjectController.getMapObject().getUnitID());
        unitTypeField.setText(mapObjectController.getMapObject().getType());
        descriptionTextArea.setDocument(new LimitedDocument(255));
        descriptionTextArea.setText(cmo.getDescription());
        descriptionTextArea.getDocument().addDocumentListener((CombinedDocumentListener) e -> setPanelDirty());
        remarksField.setText(cmo.getRemarks());
        remarksField.setCaretPosition(0); // Scrolls to the top
        remarksField.getDocument().addDocumentListener((CombinedDocumentListener) e -> setPanelDirty());
        locationField.setLocationValue(cmo.getLocation());

        allowDrag = cmo.isAllowDrag();
        allowDragCheckBox.setSelected(allowDrag);

        allowDeviceContainment = cmo.isAllowDeviceToBeContainer();
        containedField.addRemoveListener(e -> setPanelDirty());
        updateForContainment();

        // If it's a shape, then remove the "allow drag" checkbox, since the proper control is really "editable".
        if (cmo instanceof IShapeMapObject)
        {
            allowDragCheckBox.setVisible(false);
        } else
        {
            allowDragCheckBox.setVisible(true);
        }

        if (cmo.isDevice() || cmo.getGroupName().equalsIgnoreCase(MapObjectGroupNames.FILES_GROUP))
        {
            allowContainmentCheckBox.setSelected(allowDeviceContainment);
            FormLayoutUtils.restoreRow(allowContainmentCheckBox);
        } else
        {
            FormLayoutUtils.hideRow(allowContainmentCheckBox);
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

        if (!mapObject.getMetaBoolean(DevicePanelMetadataKeys.SHOW_RANGE_AND_BEARING, true))
        {
            FormLayoutUtils.hideRow(containedField);
            FormLayoutUtils.hideRow(remarksPane);
            FormLayoutUtils.hideRow(checkboxPanel);
        }

        updating = false;
        // Note: our property change/event listeners won't be registered until we are visible
        // See thisHierarchyChanged().
    }

    @Override
    public void releaseResources()
    {
        locationField.releaseResources();
        super.releaseResources();
    }

    private void updateForContainment()
    {
        if (mapObject.isAllowDeviceToBeContainer() && mapObject instanceof IIconMapObject iconMapObject)
        {
            containedField.setModelElements(iconMapObject.getContainedMapObjects());
            FormLayoutUtils.restoreRow(containedField);
        } else
        {
            containedField.setModelElements(null);
            FormLayoutUtils.hideRow(containedField);
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
        if (!updating)
        {
            updating = true;
            final IMapObject cmo = getMapObject();

            final String newName = textFieldName.getText().trim();
            if (cmo instanceof IMarkerMapObject) cmo.setTitle(newName);
            cmo.setName(newName);

            String description = descriptionTextArea.getText().trim();
            cmo.setDescription(description);

            String itext = remarksField.getText().trim();
            cmo.setRemarks(itext);

            if (!getMapObject().getLocation().equals(locationField.getLocationValue()))
            {
                //noinspection DataFlowIssue location is non null once map object has been selected in the panel
                IExtendedLocation extendedLocation = new ExtendedLocation(locationField.getLocationValue(), true, false);
                getMapObjectController().sendLocationChangeMessage(extendedLocation);
            }

            // Apply center on values
            var centerOnGlobes = centerOnField.getSelectedGlobeIds();
            globeManager.getAllGlobeDelegates().forEach(globe -> {
                if (globe.isCenterOn(mapObject) && !centerOnGlobes.contains(globe.getGlobeId()))
                {
                    globe.stopCenterOn();
                } else if (centerOnGlobes.contains(globe.getGlobeId()) && !globe.isCenterOn(mapObject))
                {
                    globe.startCenterOn(mapObject);
                }
            });

            // PLUGIN-969: range fans should have the same allowDrag value as ordinary map objects
            if (cmo instanceof IShapeMapObject<?> shapeMapObject && !(cmo instanceof IShapeRangeFanMapObject))
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

            if (cmo.isAllowDeviceToBeContainer() && cmo instanceof IIconMapObject iconMapObject)
            {
                var modelList = containedField.getModelElements();
                iconMapObject.getContainedMapObjects().stream().filter(Predicate.not(modelList::contains)).forEach(mapObjectManager::removeFromContainer);
            }
            updateForContainment();

            updating = false;
        }
    }

    @Override
    protected void setPanelDirty()
    {
        super.setPanelDirty();
        if (immediate) applyActionPerformed();
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

    private void locationFieldChanged()
    {
        setPanelDirty();
    }

    private void centerOnFieldChanged()
    {
        setPanelDirty();
    }

    @Override
    @InvokeOnEDT
    public void propertyChange(final PropertyChangeEvent propertyChangeEvent)
    {
        if (!updating && immediate)
        {
            updating = true;
            final IMapObject mo = getMapObject();
            final String propertyName = propertyChangeEvent.getPropertyName();
            switch (propertyName)
            {
                case IMapObject.NAME -> textFieldName.setText(mo.getName());
                case IMapObject.LOCATION -> locationField.setLocationValue(mo.getLocation());
                case IMapObject.DESCRIPTION -> descriptionTextArea.setText(mo.getDescription());
                case IMapObject.REMARKS -> remarksField.setText(mo.getRemarks());
                case StandardAttributeNames.USE_COT_TYPE_ICON -> useCotIconCheckBox.setSelected(mo.getAttributeSet().getBooleanAttribute(StandardAttributeNames.USE_COT_TYPE_ICON, false));
                case IIconMapObject.CONTAINED_ADDED, IIconMapObject.CONTAINED_REMOVED, IIconMapObject.ALLOW_DEVICE_CONTAINMENT -> updateForContainment();
                default -> logger.warn("Unhandled property change: {}", propertyChangeEvent);
            }
            updating = false;
        }
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
    }

    /**
     * Cleanup our listener registrations.
     */
    private void unregisterListeners()
    {
        if (logger.isDebugEnabled()) logger.debug("Cleaning up");

        getMapObject().removePropertyChangeListener(propertyList, this);
    }

    @SuppressWarnings({"Convert2MethodRef", "RedundantExplicitVariableType"})
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panel1 = new JPanel();
        var nameLabel = new JLabel();
        textFieldName = new JTextField();
        iconLabel = new JLabel();
        var descriptionLabel = new JLabel();
        var descriptionScrollPane = new JScrollPane();
        descriptionTextArea = new JTextArea();
        var unitIdLabel = new JLabel();
        var panel2 = new JPanel();
        unitTypeField = new JTextField();
        unitIdField = new JTextField();
        var locationLabel = new JLabel();
        locationField = new LocationField();
        var label4 = new JLabel();
        remarksPane = new JScrollPane();
        remarksField = new JTextPane();
        checkboxPanel = new JPanel();
        allowDragCheckBox = new JCheckBox();
        centerOnField = new CenterOnField();
        snapIconToSurfaceCheckBox = new JCheckBox();
        useCotIconCheckBox = new JCheckBox();
        allowContainmentCheckBox = new JCheckBox();
        var containsLabel = new JLabel();
        containedField = new ChipField<>();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        addHierarchyListener(e -> thisHierarchyChanged(e));
        setLayout(new FormLayout(
                "default, $lcgap, default:grow",
                "3*(default, $lgap), fill:[default,35dlu]:grow, 2*($lgap, default)"));

        //======== panel1 ========
        {
            panel1.setName("panel1");
            panel1.setLayout(new FormLayout(
                    "2*(default, $lcgap), default:grow",
                    "default, $lgap, default"));

            //---- nameLabel ----
            nameLabel.setText("Name:");
            nameLabel.setName("nameLabel");
            panel1.add(nameLabel, cc.xy(3, 1));

            //---- textFieldName ----
            textFieldName.setName("textFieldName");
            panel1.add(textFieldName, cc.xy(5, 1));

            //---- iconLabel ----
            iconLabel.setName("iconLabel");
            panel1.add(iconLabel, cc.xywh(1, 1, 1, 3, CellConstraints.CENTER, CellConstraints.CENTER));

            //---- descriptionLabel ----
            descriptionLabel.setText("Description:");
            descriptionLabel.setName("descriptionLabel");
            panel1.add(descriptionLabel, cc.xy(3, 3, CellConstraints.DEFAULT, CellConstraints.CENTER));

            //======== descriptionScrollPane ========
            {
                descriptionScrollPane.setName("descriptionScrollPane");

                //---- descriptionTextArea ----
                descriptionTextArea.setName("descriptionTextArea");
                descriptionScrollPane.setViewportView(descriptionTextArea);
            }
            panel1.add(descriptionScrollPane, cc.xy(5, 3));
        }
        add(panel1, cc.xywh(1, 1, 3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

        //---- unitIdLabel ----
        unitIdLabel.setText("Type / ID:");
        unitIdLabel.setName("unitIdLabel");
        add(unitIdLabel, cc.xy(1, 3));

        //======== panel2 ========
        {
            panel2.setName("panel2");
            panel2.setLayout(new FormLayout(
                    "center:default, $lcgap, default:grow",
                    "default"));

            //---- unitTypeField ----
            unitTypeField.setEditable(false);
            unitTypeField.setName("unitTypeField");
            panel2.add(unitTypeField, cc.xy(1, 1));

            //---- unitIdField ----
            unitIdField.setEditable(false);
            unitIdField.setName("unitIdField");
            panel2.add(unitIdField, cc.xy(3, 1));
        }
        add(panel2, cc.xy(3, 3));

        //---- locationLabel ----
        locationLabel.setText("Location:");
        locationLabel.setName("locationLabel");
        add(locationLabel, cc.xy(1, 5, CellConstraints.DEFAULT, CellConstraints.CENTER));

        //---- locationField ----
        locationField.setLocationLabelText(null);
        locationField.setName("locationField");
        locationField.addChangeListener(e -> locationFieldChanged());
        add(locationField, cc.xy(3, 5));

        //---- label4 ----
        label4.setText("Remarks:");
        label4.setName("label4");
        add(label4, cc.xy(1, 7, CellConstraints.DEFAULT, CellConstraints.CENTER));

        //======== remarksPane ========
        {
            remarksPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            remarksPane.setMinimumSize(new Dimension(50, 15));
            remarksPane.setName("remarksPane");

            //---- remarksField ----
            remarksField.putClientProperty("JTextPane.HONOR_DISPLAY_PROPERTIES", true);
            remarksField.setName("remarksField");
            remarksPane.setViewportView(remarksField);
        }
        add(remarksPane, cc.xy(3, 7));

        //======== checkboxPanel ========
        {
            checkboxPanel.setName("checkboxPanel");
            checkboxPanel.setLayout(new FormLayout(
                    "left:default, 2*($lcgap, default)",
                    "fill:default:grow, 2*($lgap, default)"));

            //---- allowDragCheckBox ----
            allowDragCheckBox.setText("Allow Drag");
            allowDragCheckBox.setSelected(true);
            allowDragCheckBox.setName("allowDragCheckBox");
            allowDragCheckBox.addItemListener(e -> allowDragCheckBoxItemStateChanged(e));
            checkboxPanel.add(allowDragCheckBox, cc.xy(1, 1, CellConstraints.LEFT, CellConstraints.CENTER));

            //---- centerOnField ----
            centerOnField.setName("centerOnField");
            centerOnField.addChangeListener(e -> centerOnFieldChanged());
            checkboxPanel.add(centerOnField, cc.xy(3, 1));

            //---- snapIconToSurfaceCheckBox ----
            snapIconToSurfaceCheckBox.setText("Snap Icon To Surface");
            snapIconToSurfaceCheckBox.setName("snapIconToSurfaceCheckBox");
            snapIconToSurfaceCheckBox.addItemListener(e -> snapIconToSurfaceCheckBoxItemStateChanged(e));
            checkboxPanel.add(snapIconToSurfaceCheckBox, cc.xy(1, 3, CellConstraints.LEFT, CellConstraints.CENTER));

            //---- useCotIconCheckBox ----
            useCotIconCheckBox.setText("Use CoT Icon");
            useCotIconCheckBox.setName("useCotIconCheckBox");
            useCotIconCheckBox.addItemListener(e -> useCotIconCheckBoxItemStateChanged(e));
            checkboxPanel.add(useCotIconCheckBox, cc.xy(3, 3));

            //---- allowContainmentCheckBox ----
            allowContainmentCheckBox.setText("Allow Containment");
            allowContainmentCheckBox.setName("allowContainmentCheckBox");
            allowContainmentCheckBox.addItemListener(e -> allowContainmentCheckBoxItemStateChanged(e));
            checkboxPanel.add(allowContainmentCheckBox, cc.xy(1, 5));
        }
        add(checkboxPanel, cc.xy(3, 9));

        //---- containsLabel ----
        containsLabel.setText("Contains:");
        containsLabel.setName("containsLabel");
        add(containsLabel, cc.xy(1, 11));

        //---- containedField ----
        containedField.setName("containedField");
        add(containedField, cc.xy(3, 11));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTextField textFieldName;
    private JLabel iconLabel;
    private JTextArea descriptionTextArea;
    private JTextField unitTypeField;
    private JTextField unitIdField;
    private LocationField locationField;
    private JScrollPane remarksPane;
    private JTextPane remarksField;
    private JPanel checkboxPanel;
    private JCheckBox allowDragCheckBox;
    private CenterOnField centerOnField;
    private JCheckBox snapIconToSurfaceCheckBox;
    private JCheckBox useCotIconCheckBox;
    private JCheckBox allowContainmentCheckBox;
    private ChipField<IIconMapObject> containedField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
