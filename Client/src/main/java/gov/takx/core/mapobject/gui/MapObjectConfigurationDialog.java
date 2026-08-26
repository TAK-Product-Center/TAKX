package gov.takx.core.mapobject.gui;

import gov.tak.api.maps.MapItem;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IAnnotationMapObject;
import gov.takx.api.mapobject.IFlatAnnotationMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.IShapeBullseyeMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.mapobject.IShapeRangeFanMapObject;
import gov.takx.api.plugin.PluginConstants;
import gov.takx.api.plugin.device.IDefaultCommsPathManager;
import gov.takx.api.plugin.device.IDeviceConfigurationDelegate;
import gov.takx.api.plugin.device.IDevicePluginController;
import gov.takx.api.plugin.device.IPluginConfigurationPanel;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.uibridge.SwingUIBinder;
import gov.takx.core.view.gui.maptools.AnnotationPanel;
import gov.takx.core.view.gui.maptools.BullseyeEditorPanel;
import gov.takx.core.view.gui.maptools.FlatAnnotationPanel;
import gov.takx.core.view.gui.maptools.RangeFanEditorPanel;
import gov.takx.core.view.gui.maptools.ShapePropertiesEditorPanel;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.ListSelectionListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Dialog used to display map object configuration options. Contains a list of core-provided and plugin-provided
 * configuration panels.
 *
 * @since 1.0 (renamed from MapObjectPreferencesDialog in 5.0)
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MapObjectConfigurationDialog extends APluginConfigurationDialog implements IDeviceConfigurationDelegate, PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private SwingUIBinder swingUIBinder;

    private final IMapObjectController mapObjectController;
    private final Consumer<MapItem> deletionHandler = mo -> dispose();

    /**
     * Constructor. Takes a Frame owner and map object controller.
     *
     * @param mapObjectController map object controller
     */
    public MapObjectConfigurationDialog(@Nonnull IMapObjectController mapObjectController)
    {
        super("Device Configuration - " + mapObjectController.getName());

        this.mapObjectController = mapObjectController;

        final var mapObject = mapObjectController.getMapObject();
        mapObject.addDeletionHandler(deletionHandler);
        mapObject.addPropertyChangeListener(IMapObject.NAME, this);
    }

    /**
     * Initialize the map object preference dialog.
     */
    @PostConstruct
    private void postConstruct()
    {
        final IMapObject mapObject = mapObjectController.getMapObject();

        // We can't just use a list of IMutablePanels b/c ShapePropertiesEditorPanel doesn't provide the shape's icon
        final List<CorePanelAndIcon> corePanelAndIcons = new ArrayList<>();

        corePanelAndIcons.add(new CorePanelAndIcon(new MapObjectGeneralPreferencesPanel(mapObjectController, panelStateListener), null));
        corePanelAndIcons.add(new CorePanelAndIcon(new MapObjectAttachmentsPanel(mapObject, false), null));

        if (mapObject instanceof IMarkerMapObject)
        {
            corePanelAndIcons.add(new CorePanelAndIcon(new MapObjectVisualAttributesPanel(mapObjectController, panelStateListener), null));
        } else if (mapObject instanceof IShapeRangeFanMapObject rangeFan)
        {
            corePanelAndIcons.add(new CorePanelAndIcon(new RangeFanEditorPanel(rangeFan, panelStateListener), null));
        } else if (mapObject instanceof IShapeBullseyeMapObject bullseye)
        {
            corePanelAndIcons.add(new CorePanelAndIcon(new BullseyeEditorPanel(bullseye, panelStateListener),
                    mapObject.getSizedIcon(ImageUtils.MEDIUM_ICON_WIDTH)));
        } else if (mapObject instanceof IShapeMapObject<?> shape)
        {
            corePanelAndIcons.add(new CorePanelAndIcon(new ShapePropertiesEditorPanel(shape, panelStateListener),
                    mapObject.getSizedIcon(ImageUtils.MEDIUM_ICON_WIDTH)));
        } else if (mapObject instanceof IAnnotationMapObject annotation)
        {
            corePanelAndIcons.add(new CorePanelAndIcon(new AnnotationPanel(annotation, panelStateListener), null));
        } else if (mapObject instanceof IFlatAnnotationMapObject annotation)
        {
            corePanelAndIcons.add(new CorePanelAndIcon(new FlatAnnotationPanel(annotation, panelStateListener), null));
        }

        if (mapObject.canHaveStyledTrackData())
        {
            corePanelAndIcons.add(new CorePanelAndIcon(new MapObjectSmartTracksPreferencesPanel(mapObjectController, panelStateListener), null));
        }

        if (mapObject.canHaveAlarms())
        {
            corePanelAndIcons.add(new CorePanelAndIcon(new MapObjectCustomAlarmsPanel(mapObjectController, panelStateListener), null));

            // Since this is a device, add the comms panel if available
            IDevicePluginController pluginController = mapObjectController.getPluginController();

            if (pluginController instanceof IDefaultCommsPathManager)
            {
                corePanelAndIcons.add(new CorePanelAndIcon(new DeviceCommsPreferencesPanel(mapObjectController, panelStateListener), null));
            }
        }

        try
        {
            final var uiProvider = swingUIBinder.forDevice(mapObjectController);
            uiProvider.setMapObjectConfigurationDelegate(this);
            boolean excludeCorePanels = uiProvider.excludeCoreConfigurationPanels();
            final var pluginPanels = uiProvider.getConfigurationPanels();
            final var primaryConfigurationPanel = uiProvider.getPrimaryConfigurationPanel();

            if (excludeCorePanels) corePanelAndIcons.clear();

            // Add a panel showing the attribute set key/values when developer mode is enabled.
            if (PreferenceEntry.DEV_MODE_SETTINGS_ENABLED.getBooleanValue())
            {
                corePanelAndIcons.add(new CorePanelAndIcon(new AttributeSetPanel(mapObject.getAttributeSet()), null));
            }

            init(corePanelAndIcons, primaryConfigurationPanel, pluginPanels != null ? pluginPanels : Collections.emptyList());
        } catch (Exception e)
        {
            logger.error(PluginConstants.PLUGIN_EXCEPTION + ": Exception in getting custom configuration panels from: {}",
                    mapObjectController.getKey(), e);
        }
    }

    /**
     * Remove the map object property change listener
     */
    @Override
    protected void onWindowClosing()
    {
        super.onWindowClosing();
        final var mapObject = mapObjectController.getMapObject();
        mapObject.removeDeletionHandler(deletionHandler);
        mapObject.removePropertyChangeListener(IMapObject.NAME, this);
    }

    /**
     * Handle a property change event for the map object.
     *
     * @param propertyChangeEvent property change event.
     */
    @Override
    @InvokeOnEDT
    public void propertyChange(final PropertyChangeEvent propertyChangeEvent)
    {
        setTitle("Device Configuration - " + propertyChangeEvent.getNewValue());
    }

    @Override
    public void registerPanelSelectListener(@Nonnull ListSelectionListener listener)
    {
        pluginConfigurationListPanel.addListSelectionListener(listener);
    }

    @Override
    public void unregisterPanelSelectListener(ListSelectionListener listener)
    {
        pluginConfigurationListPanel.removeListSelectionListener(listener);
    }

    @Override
    public void addConfigurationPanel(@Nonnull IPluginConfigurationPanel configurationPanel)
    {
        pluginConfigurationListPanel.addPanel(configurationPanel);
        configurationPanel.setPanelListener(panelStateListener);
        pluginPanels.add(configurationPanel);
        repack();
    }

    @Override
    public void removeConfigurationPanel(@Nonnull IPluginConfigurationPanel configurationPanel)
    {
        pluginPanels.remove(configurationPanel);
        pluginConfigurationListPanel.removePanel(configurationPanel);
    }

    @Override
    public void changePanelTitle(@Nonnull IPluginConfigurationPanel configPanel, @Nonnull String newTitle)
    {
        pluginConfigurationListPanel.changePanelTitle(configPanel, newTitle);
    }

    @Nullable
    @Override
    public IPluginConfigurationPanel getSelectedConfigurationPanel()
    {
        Optional<IPluginConfigurationPanel> configurationPanel =
                pluginPanels.stream().filter(panel -> panel.getTitle().equals(pluginConfigurationListPanel.getSelectedPanelTitle())).findFirst();
        return configurationPanel.orElse(null);
    }

    @Override
    public void setConfigurationDirty()
    {
        panelStateListener.setPanelDirty();
    }

    @Override
    public void selectPanel(@Nullable String panelName)
    {
        pluginConfigurationListPanel.selectPanel(panelName);
    }
}
