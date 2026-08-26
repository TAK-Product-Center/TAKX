package gov.takx.core.view.gui.propertiespanel;

import com.jgoodies.forms.factories.Borders;
import gov.tak.api.location.ILocationTracker;
import gov.tak.api.maps.Marker;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IAnnotationMapObject;
import gov.takx.api.mapobject.IFlatAnnotationMapObject;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.IMultiPolylineMapObject;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.mapobject.IShapeBullseyeMapObject;
import gov.takx.api.mapobject.IShapeRangeFanMapObject;
import gov.takx.api.plugin.IRaptorPanel;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.api.ui.DevicePanelMetadataKeys;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.swing.CollapsiblePanel;
import gov.takx.core.mapobject.gui.DeviceDetailsPanel;
import gov.takx.core.mapobject.gui.LastReportDetailsPanel;
import gov.takx.core.mapobject.gui.LocationAndMovementDetailsPanel;
import gov.takx.core.mapobject.gui.MapObjectGeneralPreferencesPanel;
import gov.takx.core.mapobject.gui.SensorDetailsPanel;
import gov.takx.core.mapobject.gui.primarydialog.MapObjectDialogUtils;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.DataMonitorsDetailsPanel;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.FileMapObjectPrimaryPanel;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.MarkerMapObjectPrimaryPanel;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.MultiPolylineMapObjectPanel;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.RouteMapObjectPrimaryPanel;
import gov.takx.core.uibridge.SwingUIBinder;
import gov.takx.core.view.gui.maptools.APropertiesEditorPanel;
import gov.takx.core.view.gui.maptools.AnnotationPanel;
import gov.takx.core.view.gui.maptools.BullseyeEditorPanel;
import gov.takx.core.view.gui.maptools.FlatAnnotationPanel;
import gov.takx.core.view.gui.maptools.IconPropertiesEditorPanel;
import gov.takx.core.view.gui.maptools.RangeFanEditorPanel;
import gov.takx.core.view.gui.maptools.ShapePropertiesEditorPanel;
import gov.takx.dataservices.persist.FileMapObject;
import gov.takx.dataservices.persist.IconMapObject;
import gov.takx.dataservices.persist.MarkerMapObject;
import gov.takx.dataservices.persist.MultiPolylineMapObject;
import gov.takx.dataservices.persist.RouteMapObject;
import gov.takx.platform.ui.location.RangeAndBearingDetailsPanel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.Border;

import java.util.ArrayList;
import java.util.List;

/**
 * Only used by {@link PropertiesPanel}.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MapObjectPropertiesItem extends APropertiesItem
{
    private final IMapObjectController controller;
    private final List<CollapsiblePanel> panels = new ArrayList<>();
    private final Border panelContentBorder = Borders.createEmptyBorder("4dlu, 6dlu, 4dlu, 6dlu");

    @Inject
    private MapObjectDialogUtils mapObjectDialogUtils;
    @Inject
    private SwingUIBinder swingUIBinder;
    @Inject
    private ILocationTracker locationTracker;
    private final IMapObject mapObject;
    private final String label;

    public MapObjectPropertiesItem(IMapObjectController controller)
    {
        super("Map Object Properties Item");
        this.controller = controller;
        mapObject = controller.getMapObject();
        label = mapObject.getName();
    }

    @Override
    public void releaseResources()
    {
        panels.stream().filter(IResourceCleaner.class::isInstance).map(IResourceCleaner.class::cast).forEach(IResourceCleaner::releaseResources);
    }

    @Override
    public JPanel getPanel()
    {
        releaseResources();
        panels.clear();
        JPanel propertiesPanel = new JPanel();
        propertiesPanel.setLayout(new BoxLayout(propertiesPanel, BoxLayout.Y_AXIS));

        if (mapObject.getMetaBoolean(DevicePanelMetadataKeys.SHOW_HEADER, true))
        {
            var generalPanel = new MapObjectGeneralPreferencesPanel(controller, null, true);
            panels.add(new CollapsiblePanel("General", false, generalPanel));
            if (mapObject instanceof IMarkerMapObject)
            {
                if (mapObject.getMetaBoolean(DevicePanelMetadataKeys.SHOW_RANGE_AND_BEARING, true))
                {
                    var fromYouField = new RangeAndBearingDetailsPanel();
                    fromYouField.setTarget(mapObject);
                    fromYouField.setSource(locationTracker.getMarkerMapObject());
                    fromYouField.setSourceName("Self");
                    panels.add(new CollapsiblePanel("From You", false, fromYouField, fromYouField.isError()));
                }

                if (mapObject.getMetaBoolean(DevicePanelMetadataKeys.SHOW_MOVEMENT_DETAILS, true))
                {
                    var locationAndMovementField = new LocationAndMovementDetailsPanel();
                    locationAndMovementField.setMapObject(mapObject);
                    panels.add(new CollapsiblePanel("Location and Movement", false, locationAndMovementField, locationAndMovementField.isEmpty()));
                }
            }
        }

        if (mapObject instanceof Marker && mapObject.getMetaBoolean(DevicePanelMetadataKeys.SHOW_SENSOR_DETAILS, true))
        {
            var sensorDetailsPanel = new SensorDetailsPanel();
            sensorDetailsPanel.setMapObject(mapObject);
            panels.add(new CollapsiblePanel("Sensor", false, sensorDetailsPanel, mapObject.getMetaBoolean(StandardAttributeNames.SENSOR_FOV, true)));
        }

        if (mapObject.getMetaBoolean(DevicePanelMetadataKeys.SHOW_LAST_REPORT_DETAILS, true) && mapObject.getMostRecentCommunicationTime() > -1 && mapObject.getLatestCotEvent() != null)
        {
            var lastReportField = new LastReportDetailsPanel();
            lastReportField.setMapObject(mapObject);
            panels.add(new CollapsiblePanel("Last Report", false, lastReportField, lastReportField.isEmpty()));
        }

        if (!controller.getDataMonitors().isEmpty() && mapObject.getMetaBoolean(DevicePanelMetadataKeys.SHOW_DATA_MONITORS, true))
        {
            var dataMonitorsPanel = new DataMonitorsDetailsPanel();
            dataMonitorsPanel.setMapObject(mapObject);
            panels.add(new CollapsiblePanel("Data Monitors", false, dataMonitorsPanel));
        }

        if (mapObject.isDevice() && mapObject.getMetaBoolean(DevicePanelMetadataKeys.SHOW_DEVICE_DETAILS, true))
        {
            var deviceDetailsPanel = new DeviceDetailsPanel();
            deviceDetailsPanel.setMapObject(mapObject);
            panels.add(new CollapsiblePanel("Device", false, deviceDetailsPanel, deviceDetailsPanel.isEmpty()));
        }

        var editorPanel = getEditorPanel();
        if (editorPanel != null) panels.add(editorPanel);

        var primaryPanel = getPrimaryPanel();
        if (primaryPanel != null) panels.add(primaryPanel);

        panels.forEach(panel -> {
            panel.getContentView().setBorder(panelContentBorder);
            propertiesPanel.add(panel);
        });

        return propertiesPanel;
    }

    @Override
    public String getLabelText()
    {
        return label;
    }

    @Override
    public Object getIdentityObject()
    {
        return controller;
    }

    @Override
    public boolean isDialogOpen()
    {
        return controller.isDialogOpen();
    }

    @Override
    public boolean supportsAttachments()
    {
        return true;
    }

    /**
     * Only certain map object types have a "primary" panel.
     *
     * @return a panel to display data for a map object
     */
    @Nullable
    private CollapsiblePanel getPrimaryPanel()
    {
        IRaptorPanel primaryPanel = null;
        String primaryTitle = null;
        // Order here seems to be relatively important, given that some of the conditionals overlap
        if (mapObject.isDevice())
        {
            primaryPanel = createPluginPrimaryPanel(controller);
        } else if (mapObject instanceof FileMapObject)
        {
            primaryPanel = new FileMapObjectPrimaryPanel(controller);
            primaryTitle = FileMapObject.FILE_TYPE;
        } else if (mapObject instanceof IconMapObject)
        {
            primaryPanel = createPluginPrimaryPanel(controller);
        } else if (mapObject instanceof RouteMapObject)
        {
            primaryPanel = new RouteMapObjectPrimaryPanel(controller);
            primaryTitle = IRouteMapObject.ROUTE_TYPE;
        } else if (mapObject instanceof MarkerMapObject)
        {
            primaryPanel = new MarkerMapObjectPrimaryPanel(controller);
            primaryTitle = "Marker";
        } else if (mapObject instanceof MultiPolylineMapObject)
        {
            primaryPanel = new MultiPolylineMapObjectPanel(controller);
            primaryTitle = IMultiPolylineMapObject.MULTI_POLYLINE_TYPE;
        }

        if (primaryPanel != null)
        {
            if (primaryTitle == null) primaryTitle = primaryPanel.getTitle();
            // Check to see if the developer has properly implemented the IRaptorPanel to return a JPanel.
            // If not, check to see if the primary panel is the JPanel. We used to assume that the primary
            // panel was a JPanel and have coded our sample plugins that way, but that is not a valid assumption
            // given our API contract.
            final var panelFromPanel = primaryPanel.getPanel();
            var panel = panelFromPanel == null && primaryPanel instanceof JPanel
                    ? (JPanel) primaryPanel
                    : panelFromPanel;

            return new CollapsiblePanel(primaryTitle, false, panel);
        }
        return null;
    }

    @Nullable
    private IRaptorPanel createPluginPrimaryPanel(@Nonnull IMapObjectController mapObjectController)
    {
        return swingUIBinder.forDevice(mapObjectController).getPrimaryPanel();
    }

    /**
     * Only certain map object types have an "editor" panel.
     *
     * @return a panel for editing map object properties
     */
    @Nullable
    private CollapsiblePanel getEditorPanel()
    {
        APropertiesEditorPanel<?> editorPanel = null;
        if (mapObject instanceof IMutableShape shape)
        {
            if (mapObject instanceof IShapeRangeFanMapObject rangeFan)
            {
                editorPanel = new RangeFanEditorPanel(rangeFan);
            } else if (mapObject instanceof IShapeBullseyeMapObject bullseye)
            {
                editorPanel = new BullseyeEditorPanel(bullseye);
            } else
            {
                editorPanel = new ShapePropertiesEditorPanel(shape);
            }
        } else if (mapObject instanceof IAnnotationMapObject annotation)
        {
            editorPanel = new AnnotationPanel(annotation);
        } else if (mapObject instanceof IFlatAnnotationMapObject flatAnnotation)
        {
            editorPanel = new FlatAnnotationPanel(flatAnnotation);
        } else if (mapObject instanceof IIconMapObject icon)
        {
            if (mapObject.getMetaBoolean(DevicePanelMetadataKeys.SHOW_ICON_OPTIONS, true))
            {
                editorPanel = new IconPropertiesEditorPanel(icon);
            }
        }
        return editorPanel != null ? new CollapsiblePanel(editorPanel.getTitle(), false, editorPanel, true) : null;
    }
}
