package gov.takx.core.mapobject;

import gov.nasa.worldwind.geom.Sector;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.exporter.ICotExporterManager;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.commservices.ICommServiceManager;
import gov.takx.api.commservices.ISerialPortStatus;
import gov.takx.api.coordinatesystem.IExtendedLocation;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.dataportal.DefaultRaptorDataListener;
import gov.takx.api.dataportal.IRaptorDataListener;
import gov.takx.api.dataportal.IRaptorDataPortalController;
import gov.takx.api.eventlog.IRaptorEventLogManager;
import gov.takx.api.explorer.IExplorerManagerDelegate;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.gateway.IDeviceGateway;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.globe.IGlobeMarkerMapObject;
import gov.takx.api.globe.IStateIconManager;
import gov.takx.api.layer.ILayerManagerDelegate;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectControllerListener;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.ISharedMapObjectManager;
import gov.takx.api.mapobject.ITrackContextMenuListener;
import gov.takx.api.mapobject.ITrackDisplayHandler;
import gov.takx.api.mapobject.ITrackInfo;
import gov.takx.api.mapobject.ITrackSelectListener;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IPrePersistRaptorCommandMessage;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.persist.ICustomDataMonitorPersist;
import gov.takx.api.persist.IPersistService;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.persist.IPersistableState;
import gov.takx.api.plugin.IRaptorObject;
import gov.takx.api.plugin.PluginConstants;
import gov.takx.api.plugin.datamonitor.DataMonitorBuilder;
import gov.takx.api.plugin.datamonitor.DataMonitorEvent;
import gov.takx.api.plugin.datamonitor.DataMonitorEventType;
import gov.takx.api.plugin.datamonitor.StandardDataMonitorType;
import gov.takx.api.plugin.device.IDataStructureFactory;
import gov.takx.api.plugin.device.IDefaultCommsPathManager;
import gov.takx.api.plugin.device.IDevicePlugin;
import gov.takx.api.plugin.device.IDevicePluginController;
import gov.takx.api.plugin.device.IDevicePluginDelegate;
import gov.takx.api.query.IDeviceQueryService;
import gov.takx.api.shapes.IShapeService;
import gov.takx.api.surfaces.IAnalyticSurfaceService;
import gov.takx.api.tracks.IConnectingLineDefinition;
import gov.takx.api.tracks.ISmartTrackManager;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.api.tracks.ITrackStylingContext;
import gov.takx.api.ui.IUISpaceManager;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.commons.constants.State;
import gov.takx.commons.lang.IPropertyChangeDelegateSupport;
import gov.takx.commons.lang.IPropertyChangeListenerSupport;
import gov.takx.commons.security.IParserEncryptionKey;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.datamonitor.ADataMonitorServices;
import gov.takx.core.explorer.IExplorerManager;
import gov.takx.core.filesystem.VirtualFileSystemManagerProvider;
import gov.takx.core.globe.GlobeStateIconManager;
import gov.takx.core.globe.GlobeVectorPointer;
import gov.takx.core.globe.render.RenderingContext;
import gov.takx.core.mapobject.gui.CotDialog;
import gov.takx.core.mapobject.gui.MapObjectConfigurationDialog;
import gov.takx.core.mapobject.gui.MapObjectCustomAlarmsPanel;
import gov.takx.core.mapobject.gui.MapObjectHelpDialog;
import gov.takx.core.mapobject.gui.MapObjectLogDialog;
import gov.takx.core.mapobject.gui.MapObjectPopupMenu;
import gov.takx.core.mapobject.gui.primarydialog.IMapObjectPrimaryDialog;
import gov.takx.core.mapobject.gui.primarydialog.MapObjectDialogUtils;
import gov.takx.core.mapobject.undo.MapObjectUndoHandler;
import gov.takx.core.messagequery.PluginQueryService;
import gov.takx.core.platform.APluginDelegate;
import gov.takx.core.plugin.DefaultDevicePluginController;
import gov.takx.core.pluginclient.EncryptionKeyMap;
import gov.takx.core.shapes.ShapeService;
import gov.takx.core.shapes.ShapeServiceFactory;
import gov.takx.core.surfaces.SurfaceService;
import gov.takx.dataservices.persist.CustomDataMonitor;
import gov.takx.dataservices.persist.RaptorDataStructure;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.tracks.TrackUtils;
import gov.takx.platform.utils.TimeBatchedQueue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Controller for a map object.
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MapObjectController extends APluginDelegate implements IMapObjectController, PropertyChangeListener, IPropertyChangeDelegateSupport
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final LocationUpdateQueue LOCATION_UPDATER = new LocationUpdateQueue();

    // Noop can be shared across all MOCs that don't support data monitors
    private static final ADataMonitorServices<IDevicePluginDelegate> NOOP_DATA_MONITOR_SERVICES = new NoopDataMonitorServices();

    // Determine if we are batching RDM persistence operations, which means we batch MO location updates too
    @Inject
    @ProgramPreference(PreferenceEntry.RDM_BATCHING)
    private boolean doRDMBatch;

    protected final IMapObject mapObject;
    private final IDevicePlugin mapObjectPlugin;
    private final IRaptorDataPortalController dataPortalController;
    private final String mapObjectKey;

    private MapObjectUndoHandler undoHandler;
    private IDevicePluginController pluginController;
    private boolean pluginControllerShutdown;
    private IMapObjectPrimaryDialog<?> mapObjectDialog;
    private DeviceMenuSpaceManager menuSpaceManager;

    private volatile List<IMapObjectControllerListener> controllerListeners;
    private ADataMonitorServices<IDevicePluginDelegate> dataMonitorServices;
    private List<CustomDataMonitor> customDataMonitors;
    private Comparator<IDataMonitor> customComparator = Comparator.naturalOrder();
    private volatile List<ITrackSelectListener> selectListeners;
    private volatile List<ITrackContextMenuListener> contextMenuListeners;
    private volatile List<ITrackDisplayHandler> trackDisplayHandlers;

    private MapObjectConfigurationDialog configurationDialog;
    private MapObjectLogDialog rdmLogDialog;
    private Point rdmLogLastKnownPosition;
    private boolean resetRdmLogDialogPosition;
    private boolean isAudioPlaybackEnabled = true;
    private ShapeService shapeService;
    private PluginQueryService queryService;
    private IDeviceQueryService alternateQueryService;
    private CommServiceType defaultCommType = CommServiceType.NONE;
    private String defaultCommName = "None";
    private MapObjectMessageRouter messageRouter;
    private IStateIconManager stateIconManager;
    private IStateIconManager playbackStateIconManager;
    private boolean vectorTailEnabled;
    private GlobeVectorPointer globeVectorPointer;
    private IAnalyticSurfaceService analyticSurfaceService;
    private MapObjectHelpDialog helpDialog;
    private Action pluginDialogAction;

    /**
     * Handler for RDMs coming from the DPC.
     */
    private final IRaptorDataListener dataListener = new DefaultRaptorDataListener()
    {
        @Override
        public void onDataMessage(String topic, IRaptorDataMessage msg, IRaptorDataStructure struct)
        {
            receiveMessage(msg);
        }
    };

    /**
     * @param mapObjectPlugin Plugin defining the given {@code mapObject}
     * @param mapObject The Map Object we are to control
     * @param dataPortalController Data portal controller for the given {@code mapObject}
     */
    public MapObjectController(IDevicePlugin mapObjectPlugin, IMapObject mapObject, IRaptorDataPortalController dataPortalController)
    {
        super(mapObjectPlugin.getFamily(), mapObjectPlugin.getType(), mapObject.getUnitID());

        this.mapObject = mapObject;
        this.mapObjectPlugin = mapObjectPlugin;
        this.dataPortalController = dataPortalController;
        mapObjectKey = mapObject.getKey();

        if (dataPortalController != null) dataPortalController.addListener(dataListener, Integer.MAX_VALUE);
    }

    @PostConstruct
    private void postConstruct()
    {
        // TODO: SmartTracks: Is the appropriate place for this or should something be done in the SmartTrackManager?
        // We need to register the StopRecordsTrackStyleDefinition to have it applied to incoming Stop Records
        if (mapObject.canHaveStyledTrackData())
        {
            final var stopTSD = getStopRecordsTrackStyleDefinition();
            if (stopTSD != null) getBean(ISmartTrackManager.class).registerTrackStyle(stopTSD);
        }

        if (mapObject.canHaveAlarms())
        {
            customDataMonitors = new CopyOnWriteArrayList<>();

            dataMonitorServices = new MapObjectDataMonitorServices(this, mapObject);
            dataMonitorServices.addPropertyChangeListener(this); // Watch for any event on any of our data monitors

            if (mapObject.isRestoring())
            {
                final var cdms = getBean(MapObjectStateLoader.class).getCustomDataMonitors(mapObjectKey);
                if (cdms != null)
                {
                    for (CustomDataMonitor cdm : cdms)
                    {
                        // There is a chicken-and-egg problem with the way that CDMs are restored from persistence.
                        // They are reified VERY early in the startup sequence (well before the MapObjectManager has
                        // initialized), which means they can't perform lookups using an onLoad() method.
                        // So, we need to reinitialize the CDM here
                        cdm.reinitialize(getMapObject());

                        addDataMonitor(cdm.getDataMonitor());
                        customDataMonitors.add(cdm);
                    }
                }
            }
        } else
        {
            dataMonitorServices = NOOP_DATA_MONITOR_SERVICES;
        }

        // Track all changes to the MO's location, so that no matter how it changes, the new location is persisted
        if (!mapObject.isTransient()) mapObject.addPropertyChangeListener(IMapObject.LOCATION, e -> updateLocation());
    }

    @Override
    public void undoLastUserLocationChange()
    {
        final var undoHandler = getUndoHandler();
        if (undoHandler.isUndoAvailable()) undoHandler.undo();
    }

    private synchronized MapObjectUndoHandler getUndoHandler()
    {
        return undoHandler != null ? undoHandler : (undoHandler = new MapObjectUndoHandler(this));
    }

    @Override
    @Nullable
    public ITrackStylingContext getTrackStylingContext(@Nonnull IRaptorDataMessage rdm)
    {
        final Integer id = rdm.getTrackStyleDefinitionId();
        int rdmTsdId = id == null ? 0 : id;

        final RenderingContext renderingContext = getGlobe().getRenderingContext();
        return renderingContext != null
                ? renderingContext.getStyleContextManager().getTrackStylingContext(mapObject.getId(), rdmTsdId)
                : null;
    }

    @Override
    public Collection<ISerialPortStatus> listSerialPorts()
    {
        return getBean(ICommServiceManager.class).getSerialPortCommStatus();
    }

    /**
     * Handle a property change on our map object.
     *
     * @param evt property change event
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        final String propertyName = evt.getPropertyName();

        if (propertyName.equals(IDataMonitor.STATE_CHANGED))
        {
            Object source = evt.getSource();
            updateOnScreenState((IDataMonitor) source);
        } else if (propertyName.equalsIgnoreCase(IDataMonitor.MUTE))
        {
            // If any data monitor is unmuted, make sure global mute is off.
            if (!(Boolean) evt.getNewValue())
            {
                isAudioPlaybackEnabled = true;
                updateOnScreenState(null);
            }
        }
    }

    @Override
    public Collection<IRaptorDataStructure> getDataStructures()
    {
        return mapObject.getMapEntity().getRaptorDataStructures();
    }

    @Override
    public void shutdownPlugin()
    {
        try
        {
            if (!(dataMonitorServices instanceof NoopDataMonitorServices)) dataMonitorServices.shutdownDataMonitors();
            if (shapeService != null) getBean(ShapeServiceFactory.class).disposeShapeService(shapeService);
            if (dataPortalController != null) dataPortalController.removeListener(dataListener);

            // Notify the plugin that the controller is being shut down.  This will prevent any future calls to the plugin
            // from handing out the plugin controller.
            if (mapObjectPlugin != null) mapObjectPlugin.shutdownDevice(unitId);

            // Next, notify the plugin controller to shut down.  This gives it the opportunity to release any external
            // resources, e.g. external database connections, it might have established on creation
            if (pluginController != null) pluginController.shutdown();
        } catch (Exception e)
        {
            logger.warn(PluginConstants.PLUGIN_EXCEPTION + ": Exception shutting down plugin controller", e);
        }

        pluginControllerShutdown = true;
    }

    @Override
    public void shutdown()
    {
        // If we have unsaved location data, persist it now.  This is only necessary in the case where the location
        // data for a MO changes right before the project is closing and the LOCATION_UPDATER doesn't trigger until
        // after the persist service has been shutdown.
        if (LOCATION_UPDATER.batchContains(this)) persistLocationData();

        if (!pluginControllerShutdown) shutdownPlugin();

        if (undoHandler != null) undoHandler.shutdown();

        controllerListeners = null;
        selectListeners = null;
        contextMenuListeners = null;
        trackDisplayHandlers = null;
        stateIconManager = playbackStateIconManager = null;
        alternateQueryService = queryService = null;
        messageRouter = null;

        mapObject.shutdown(); // Finally, shutdown the map object itself
    }

    @Override
    public void createPopupMenu(MouseEvent mouseEvent)
    {
        MapObjectPopupMenu.showAsRadialMenu(mouseEvent, this);
    }

    @Override
    public void receiveMessage(IRaptorDataMessage rdm)
    {
        if (null != rdm)
        {
            updateTrackInfo(rdm);

            if (logger.isTraceEnabled()) logger.trace("receiveMessage() for device key: {}", rdm.getUniqueKey());

            if (rdm.hasHeading()) mapObject.setHeading(rdm.getHeading());
            if (rdm.hasSpeed()) mapObject.setSpeedInMps(rdm.getSpeedInMps());

            processLocation(rdm);

            // Don't want to process data monitors on a manually generated location RDM (esp since it could affect comms monitors).
            if (!rdm.isManuallyGeneratedLocation()) processDataMonitors(rdm);

            updateLocation(); // Get this data out to disk

            if (null != pluginController)
            {
                try
                {
                    if (rdm.getSourceDeviceType() != null && rdm.getSourceDeviceId() != null)
                    {
                        pluginController.receiveOtherDeviceMessage(rdm);
                    } else
                    {
                        pluginController.receiveMessage(rdm);
                    }
                } catch (Exception e)
                {
                    logger.warn(PluginConstants.PLUGIN_EXCEPTION + ": Exception in receiving message in plugin controller in MapObjectController ", e);
                }
            }

            if (controllerListeners != null)
            {
                for (IMapObjectControllerListener controllerListener : controllerListeners)
                {
                    controllerListener.receiveMessage(rdm);
                }
            }

            // Apply our stop record TrackStyleDefinition to the RDM if it's a stop record
            if (rdm.isPersistable() && getStopRecordsTrackStyleDefinition() != null && (!rdm.hasMotion() && rdm.isMotionSet()))
            {
                rdm.setTrackStyleDefinition(getStopRecordsTrackStyleDefinition());
            }

            // Inform the device log about the new RDM if it exists and is shown
            if (rdm.isPersistable() && null != rdmLogDialog && rdmLogDialog.isVisible())
            {
                rdmLogDialog.receiveMessage(rdm);
            }
        } else
        {
            logger.error("Received a null RaptorDataMessage!");
        }
    }

    /**
     * Internal method to set the location on a map object in the database.  Either do the update immediately,
     * or queue it up based on the doRDMBatch setting.
     */
    private void updateLocation()
    {
        if (mapObject.isTransient()) return; // Not persisted, so nothing to update

        if (doRDMBatch)
        {
            LOCATION_UPDATER.add(this); // trigger the timer
        } else
        {
            persistLocationData(); // Otherwise, persist immediately
        }
    }

    /**
     * Use a raw connection to update the location, heading, comm times, and track info for this map object.
     */
    private void persistLocationData()
    {
        final IMapObject mo = mapObject;
        final GeoPoint location = mo.getLocation();

        if (mo.getId() == null || !location.isValid()) return;

        final var persistServiceProvider = getBean(IPersistServiceProvider.class);
        if (persistServiceProvider.isShutdown()) return; // Don't try to persist data after the service is shutdown

        final var persistService = persistServiceProvider.getPersistService();

        persistService.doWork(connection -> {
            // NOTE: There is some ugly coupling between this list of properties and the operation of
            // MapObjectChangeMonitor.  That class "knows" the properties that this method updates and avoids
            // issuing an additional persist if any of these properties are modified.

            //noinspection SqlResolve
            final String moSql = "UPDATE MapObject SET " +
                    "geoPointMetaData_geopoint_latitude = ?, " +
                    "geoPointMetaData_geopoint_longitude = ?, " +
                    "geoPointMetaData_geopoint_altitude = ?, " +
                    "geoPointMetaData_geopoint_altitudeReference = ?, " +
                    "geoPointMetaData_geopoint_ce90 = ?, " +
                    "geoPointMetaData_geopoint_le90 = ?, " +
                    "heading = ?, " +
                    "speed = ?, " +
                    "mostRecentCommunicationTime = ?, " +
                    "oldestCommunicationTime = ?, " +

            // TrackInfo
                    "trackInfo_numberOfTracks = ?, " +
                    "trackInfo_trackStartDate = ?, " +
                    "trackInfo_trackEndDate = ?, " +
                    "trackInfo_corners = ? " +

                    "WHERE id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(moSql))
            {
                int i = 0;
                stmt.setDouble(++i, location.getLatitude());
                stmt.setDouble(++i, location.getLongitude());
                stmt.setDouble(++i, location.getAltitude());
                stmt.setInt(++i, location.getAltitudeReference().ordinal());
                stmt.setDouble(++i, location.getCE());
                stmt.setDouble(++i, location.getLE());
                stmt.setDouble(++i, mo.getHeading());
                stmt.setDouble(++i, mo.getSpeedInMps());
                stmt.setLong(++i, mo.getMostRecentCommunicationTime());
                stmt.setLong(++i, mo.getOldestCommunicationTime());

                // TrackInfo
                final ITrackInfo ti = mo.getTrackInfo();
                stmt.setInt(++i, ti.getNumberOfTracks());
                stmt.setLong(++i, ti.getTrackStartDate());
                stmt.setLong(++i, ti.getTrackEndDate());
                stmt.setObject(++i, getCorners(persistService, ti.getTrackExtents()));

                stmt.setInt(++i, mo.getId());

                logger.trace("{}", stmt);

                stmt.executeUpdate();

                connection.commit();
            } catch (SQLException e)
            {
                connection.rollback();
                final String message = "Failed to update location for " + mo.getKey();
                logger.error(message, e);
                throw new RuntimeException(message, e);
            }
        });
    }

    /**
     * Due to a change in Hibernate 6, and differences in the JDBC drivers for Postgres vs HSQL, we have to bind
     * different object types in the SQL used above. Postgres properly handles a type {@code double[]}, but HSQL
     * barfs on it, claiming an invalid type.  In order to make HSQL happy, we need to bind a {@code Double[]}
     * (note the wrapper type, not primitive).  This method takes care of abstracting out that value generation.
     *
     * @param persistService Persist service to use to determine native array support
     * @param sector Sector from which to generate the corners array
     * @return Appropriate object to bind
     * @since 3.4
     */
    private Object getCorners(IPersistService persistService, @Nullable Sector sector)
    {
        if (sector == null) return null;

        return persistService.databaseSupportsNativeArrays()
                ? sector.asDegreesArray()
                : ArrayUtils.toObject(sector.asDegreesArray());
    }

    @Override
    public void setPluginDialogAction(Action action)
    {
        pluginDialogAction = action;
    }

    @Override
    public void showPluginDialog()
    {
        if (pluginDialogAction == null)
        {
            if (mapObjectDialog == null)
            {
                try
                {
                    mapObjectDialog = getBean(MapObjectDialogUtils.class).createPrimaryDialog(this);
                    mapObjectDialog.addCloseHandler(d -> mapObjectDialog = null);
                } catch (Exception e)
                {
                    logger.error(PluginConstants.PLUGIN_EXCEPTION + ": Exception in getting primary plugin panel in MapObjectController ", e);
                    return;
                }
            }
            mapObjectDialog.openDialog();
        } else
        {
            pluginDialogAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Show Dialog"));
        }
    }

    @Override
    public ILocation getLocationOnGlobe()
    {
        return mapObject.getLocation();
    }

    @Override
    public ISmartTrackManager getSmartTrackManager()
    {
        return getBean(ISmartTrackManager.class);
    }

    @Override
    public void showHelpDialog()
    {
        if (null == helpDialog)
        {
            helpDialog = new MapObjectHelpDialog(this);
            helpDialog.addCloseHandler(d -> helpDialog = null);
            helpDialog.setVisible(true);
        } else
        {
            helpDialog.requestFocus();
            helpDialog.toFront();
        }
    }

    @Override
    public void showCotDialog()
    {
        final String name = mapObject.getName();

        // Export the map object in order to generate a current CoT representation of the MO.
        final CotEvent event = getBean(ICotExporterManager.class).export(mapObject);
        if (event == null)
        {
            logger.warn("No CoT event exported for map object {} with name {}; CoT dialog will not be shown", uniqueKey,
                    name);
            AlwaysOnTopJOptionPane.showErrorDialog("A CoT event could not be created to represent the map object " +
                    name, "Error Displaying CoT");
            return;
        }

        new CotDialog(this, event, "Current CoT Representation for " + name).setVisible(true);
    }

    @Override
    public void showLatestReceivedCotDialog()
    {
        final String name = mapObject.getName();
        final CotEvent event = mapObject.getLatestCotEvent();
        if (event == null)
        {
            logger.warn("No CoT event has been received for map object {} with name {}; latest received CoT dialog " +
                    "will not be shown", uniqueKey, name);
            AlwaysOnTopJOptionPane.showErrorDialog("A CoT event has not been received for map object " + name,
                    "Error Displaying CoT");
            return;
        }

        new CotDialog(this, event, "Latest CoT Received for " + name).setVisible(true);
    }

    @Override
    public void showConfigurationDialog()
    {
        if (null == configurationDialog)
        {
            configurationDialog = new MapObjectConfigurationDialog(this);
            configurationDialog.addCloseHandler(d -> configurationDialog = null);
            configurationDialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(configurationDialog));
            configurationDialog.setVisible(true);
        } else
        {
            configurationDialog.requestFocus();
            configurationDialog.toFront();
        }

        String defaultPreferencesTab = mapObject.getDefaultPreferencesTab();
        if (!StringUtils.isEmpty(defaultPreferencesTab, true))
        {
            configurationDialog.selectPageFromAll(defaultPreferencesTab);
        }
    }

    @Override
    public void showConfigurationDialog(String title)
    {
        showConfigurationDialog();
        configurationDialog.selectPageFromPluginPanels(title);
    }

    @Override
    public void setDefaultPreferencesTab(String title)
    {
        mapObject.setDefaultPreferencesTab(title);
    }

    @Override
    public void sendMessage(IPrePersistRaptorDataMessage rdm)
    {
        if (dataPortalController != null) dataPortalController.sendDataMessage(rdm);
    }

    @Override
    public void setAudioAlarmPlayback(boolean enable)
    {
        isAudioPlaybackEnabled = enable;

        // Set each DataMonitor to match the map object's
        // audio setting. If attempting to mute and one or more data
        // monitors is un-mutable, do not mark this map object as globally
        // muted (i.e. isAudioPlaybackEnabled will revert to true).
        dataMonitorServices.getDataMonitors().stream()
                .filter(IDataMonitor::isAffectsAudioAndIcons)
                .forEach(dataMonitor -> checkMuteStatus(dataMonitor, true));

        updateOnScreenState(null);
    }

    @Override
    public boolean isAudioAlarmPlaybackEnabled()
    {
        return isAudioPlaybackEnabled;
    }

    @Override
    public IDevicePlugin getDevicePlugin()
    {
        return mapObjectPlugin;
    }

    @Override
    public void sendMessage(IPrePersistRaptorCommandMessage rcm) throws IOException
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            processDataMonitors(rcm);
            String uniqueKey = rcm.getUniqueKey();
            final var controller = getBean(IDeviceGateway.class).getController(uniqueKey);
            if (controller != null)
            {
                controller.processDeviceControllerMessage(rcm);
            } else
            {
                logger.warn("Could not locate gateway controller for RCM: {}", rcm);
            }
        } else
        {
            getExecutorService().execute(() -> {
                try
                {
                    Thread.currentThread().setName("RCM dispatcher");
                    sendMessage(rcm);
                } catch (IOException e)
                {
                    logger.error("Error sending command message: {}", rcm.getUniqueKey(), e);
                }
            });
        }
    }

    @Override
    public void sendLocationChangeMessage(ILocation location)
    {
        if (location != null)
        {
            if (location instanceof IExtendedLocation) getUndoHandler().processLocationChange(location);

            mapObject.setLocation(location, true);
        }
    }

    @Override
    public void showDeviceLog()
    {
        showDeviceLog(null, null);
    }

    @Override
    public void showDeviceLog(Collection<IRaptorDataStructure> dataStructures, IRaptorDataStructure defaultFilter)
    {
        showDeviceLog(dataStructures, null, defaultFilter);
    }

    @Override
    public void showDeviceLog(Collection<IRaptorDataStructure> dataStructures, Long minReceiveTime, IRaptorDataStructure defaultFilter)
    {
        // This situation occurs when the user has a Device Log open, they run an Import plugin and add a new structure,
        // when prompted they choose 'No' to reload the device log at that moment in time to reflect the new structure
        // set. Thus, until they dispose() the window, we aren't ready to reload the structures

        if (rdmLogDialog != null && rdmLogDialog.isStructureListStale(dataStructures))
        {
            if (rdmLogDialog.isVisible()) rdmLogDialog.dispose();

            rdmLogDialog = null;
        }

        if (rdmLogDialog == null)
        {
            rdmLogDialog = createDeviceLogDialog(dataStructures, minReceiveTime, defaultFilter);
            if (rdmLogDialog != null) rdmLogDialog.addCloseHandler(d -> {
                rdmLogLastKnownPosition = new Point(rdmLogDialog.getX(), rdmLogDialog.getY());
                rdmLogDialog = null;
            });
        }

        if (rdmLogDialog != null)
        {
            rdmLogDialog.pack();

            int minWidth = rdmLogDialog.getMinimumSize().width;
            if (rdmLogDialog.getWidth() > minWidth)
            {
                rdmLogDialog.setSize(minWidth, rdmLogDialog.getHeight());
                rdmLogDialog.setLocationRelativeTo(JFrame.getFrames()[0]);
            }

            if (resetRdmLogDialogPosition)
            {
                resetRdmLogDialogPosition = false;
                rdmLogDialog.setLocation(rdmLogLastKnownPosition);
            }

            rdmLogDialog.setVisible(true);
            rdmLogDialog.refreshData();
        }
    }

    /**
     * Create a properly initialized map object log dialog.
     *
     * @param initialStructures Data structures to show
     * @param minReceiveTime Minimum receive timestamp
     * @param defaultFilter Default structure to select
     * @return newly constructed dialog, or {@code null} if proper conditions are not met
     * @since 2.29
     */
    private MapObjectLogDialog createDeviceLogDialog(Collection<IRaptorDataStructure> initialStructures,
                                                     Long minReceiveTime, IRaptorDataStructure defaultFilter)
    {
        final IDataStructureFactory factory = mapObjectPlugin.getDataStructureFactory();

        // Use local copy to avoid modifying our callers collection
        final Collection<IRaptorDataStructure> dataStructures;

        if (null == initialStructures)
        {
            logger.debug("Pulling in known data structures from DataStructureRegistry minus excluded");

            dataStructures = mapObject.getMapEntity().getAllRaptorDataStructures();
            if (factory != null)
            {
                var excluded = factory.getExcludedDeviceLogStructures();
                dataStructures.removeIf(excluded::contains);
            }
        } else
        {
            dataStructures = new ArrayList<>(initialStructures);
        }

        if (!removeManualLocationAndWarnIfNoStructures(dataStructures))
        {
            final IRaptorDataStructure filter = (null == defaultFilter && null != factory)
                    ? factory.getDefaultDeviceLogDataStructure()
                    : defaultFilter;

            final Window owner = mapObjectDialog != null
                    ? (JDialog) mapObjectDialog // Sadly, JDialog is not an interface, so we have to cast
                    : JFrame.getFrames()[0];

            return new MapObjectLogDialog(owner, this, minReceiveTime, dataStructures, filter,
                    getQueryService(), getVirtualFileSystem().getRootDir().getPath());
        } else
        {
            return null;
        }
    }

    @Override
    public void showTrackLog(List<IRaptorDataMessage> rdms, ILocation minLocation, ILocation maxLocation)
    {
        if (trackDisplayHandlers != null && !trackDisplayHandlers.isEmpty())
        {
            for (ITrackDisplayHandler displayHandler : trackDisplayHandlers)
            {
                displayHandler.onDisplay(rdms, minLocation, maxLocation);
            }
        } else
        {
            final List<IRaptorDataStructure> structs = rdms.stream()
                    .map(IRaptorDataMessage::getStructure)
                    .distinct()
                    .map(structure -> new RaptorDataStructure(family, type, structure))
                    .collect(Collectors.toList());

            if (!removeManualLocationAndWarnIfNoStructures(structs))
            {
                final Frame parentFrame = JFrame.getFrames()[0];

                var dialog = new MapObjectLogDialog(parentFrame, this, null,
                        structs, null, getQueryService(), getVirtualFileSystem().getRootDir().getPath(),
                        minLocation, maxLocation);

                dialog.setTitle("Track Details");

                if (dialog.getWidth() > 800)
                {
                    dialog.setSize(800, dialog.getHeight());
                    dialog.setPreferredSize(new Dimension(800, dialog.getPreferredSize().height));
                }

                dialog.setLocationRelativeTo(parentFrame);
                dialog.setVisible(true);
            }
        }
    }

    /**
     * Remove the "manual location" structure from the given list and if the list is then empty, warn the user
     * that there is no data to display.
     * <p/>
     * The {@link gov.takx.core.mapobject.gui.devicelog.DeviceLogPanel} automatically filters out manual location
     * structures, so this method checks for a condition where there would be no structures left in the list, and
     * thus the panel would show no data.
     *
     * @param dataStructures Structure collection to test
     * @return {@code true} if no structures remained
     * @see DataStructureBuilder#LOCATION_MESSAGE_TYPE
     * @since 3.5
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean removeManualLocationAndWarnIfNoStructures(Collection<IRaptorDataStructure> dataStructures)
    {
        dataStructures.removeIf(rds -> DataStructureBuilder.LOCATION_MESSAGE_TYPE.equals(rds.getMessageType()));

        if (dataStructures.isEmpty())
        {
            AlwaysOnTopJOptionPane.showErrorDialog(JFrame.getFrames()[0],
                    "There are no data structures defined for this map object.\n" +
                            "There is nothing for the Device Log to display.",
                    "No structures for Map Object");
            return true;
        }
        return false;
    }

    @Override
    public void logDeviceEvent(String message, String details)
    {
        logDeviceEvent(message, details, null);
    }

    @Override
    public void logDeviceEvent(String message, String details, State state)
    {
        getBean(IRaptorEventLogManager.class).logPluginEvent(new Date(), mapObject, message, details, state);
    }

    @Override
    public String toString()
    {
        return mapObjectKey;
    }

    @Override
    public IMapObject getMapObject()
    {
        return mapObject;
    }

    @Override
    public IDevicePluginController getPluginController()
    {
        return pluginController;
    }

    @Override
    public List<IDataMonitor> getDataMonitors()
    {
        return dataMonitorServices.getDataMonitors();
    }

    public List<CustomDataMonitor> getCustomDataMonitors()
    {
        return customDataMonitors != null ? Collections.unmodifiableList(customDataMonitors) : Collections.emptyList();
    }

    @Override
    public Comparator<IDataMonitor> getComparator()
    {
        return customComparator;
    }

    @Override
    public boolean isDialogOpen()
    {
        return mapObjectDialog != null;
    }

    @Override
    public void setSnapIconToSurface(boolean flag)
    {
        mapObject.setSnapIconToSurface(flag);
    }

    @Override
    public void setIconImage(URL imageURL)
    {
        try
        {
            mapObject.setIconImage(imageURL != null ? imageURL.toURI() : null);
        } catch (URISyntaxException e)
        {
            logger.error("Unable to turn URL into URI: {}", e.getMessage(), e);
        }
    }

    @Override
    public synchronized IVirtualFileSystem getVirtualFileSystem(VFSScope scope)
    {
        return getBean(VirtualFileSystemManagerProvider.class).getVirtualFileSystemManager(scope).getVirtualFileSystemForDevice(mapObject);
    }

    @Override
    public void sortDataMonitors()
    {
        dataMonitorServices.sortDataMonitors();
    }

    public void addCustomDataMonitor(CustomDataMonitor cdm)
    {
        if (mapObject.canHaveAlarms())
        {
            addDataMonitor(cdm.getDataMonitor());
            customDataMonitors.add(cdm);
            getBean(ICustomDataMonitorPersist.class).persistDataMonitor(cdm);
            // we'll leave the custom data monitors in the dataMonitors list for now.
        }
    }

    @Override
    public void addDataMonitor(@Nonnull IDataMonitor dm)
    {
        if (mapObject.canHaveAlarms()) dataMonitorServices.addDataMonitor(dm);
    }

    /**
     * Remove the given {@link CustomDataMonitor}.
     *
     * @param customDataMonitor Data monitor to remove
     * @since 3.4
     */
    public void removeCustomDataMonitor(@Nonnull CustomDataMonitor customDataMonitor)
    {
        if (mapObject.canHaveAlarms())
        {
            customDataMonitors.remove(customDataMonitor);
            customDataMonitor.shutdown();
            removeDataMonitor(customDataMonitor.getDataMonitor());
            getBean(ICustomDataMonitorPersist.class).deleteDataMonitor(customDataMonitor);
        }
    }

    @Override
    public void removeDataMonitor(@Nonnull IDataMonitor dm)
    {
        if (mapObject.canHaveAlarms()) dataMonitorServices.removeDataMonitor(dm);
    }

    @Nonnull
    @Override
    public IPropertyChangeListenerSupport getPropertyChangeListenerSupport()
    {
        return dataMonitorServices;
    }

    /**
     * Creates and returns a list of Action Objects that represent the types of alarms that a user can add via the GUI.
     *
     * @param panel The MapObjectCustomAlarmsPanel that the alarm will be added to once the user selects the alarm in the GUI.
     * @return A List of Action Objects that represent the types of alarms that a user can add via the GUI.
     */
    public List<Action> requestAddAlarmActions(final MapObjectCustomAlarmsPanel panel)
    {
        List<Action> customAlarmTypes = new ArrayList<>();

        customAlarmTypes.add(new AbstractAction(CustomDataMonitor.AlarmType.DataMonitor.name())
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                panel.addAlarm(new CustomDataMonitor(CustomDataMonitor.AlarmType.DataMonitor, mapObject));
            }
        });

        customAlarmTypes.add(new AbstractAction(CustomDataMonitor.AlarmType.Geofence.name())
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                panel.addAlarm(new CustomDataMonitor(CustomDataMonitor.AlarmType.Geofence, mapObject));
            }
        });

        return customAlarmTypes;
    }

    @Override
    public synchronized void addMapObjectControllerListener(@Nonnull IMapObjectControllerListener listener)
    {
        if (controllerListeners == null) controllerListeners = new CopyOnWriteArrayList<>();

        controllerListeners.add(listener);
    }

    @Override
    public synchronized void removeMapObjectControllerListener(@Nonnull IMapObjectControllerListener listener)
    {
        if (controllerListeners != null) controllerListeners.remove(listener);
    }

    @Override
    public synchronized IShapeService getShapeService()
    {
        if (shapeService == null) shapeService = getBean(ShapeServiceFactory.class).getOrCreateShapeService(this);

        return shapeService;
    }

    @Override
    public synchronized IAnalyticSurfaceService getAnalyticSurfaceService()
    {
        if (analyticSurfaceService == null) analyticSurfaceService = new SurfaceService(mapObjectKey);

        return analyticSurfaceService;
    }

    @Override
    public IGlobeManager getGlobeManager()
    {
        return getBean(IGlobeManager.class);
    }

    @Override
    public IDeviceQueryService getQueryService()
    {
        if (alternateQueryService != null) return alternateQueryService;

        synchronized (this)
        {
            if (queryService == null)
            {
                // We need to build a comprehensive list of all the structures. Unfortunately we still don't
                // have an absolute reference for that list. So we will pull them all in from disk.
                queryService = new PluginQueryService(mapObject, mapObject.getMapEntity().getAllRaptorDataStructures());
            }
            return queryService;
        }
    }

    @Override
    public void setAlternateQueryService(IDeviceQueryService queryService)
    {
        alternateQueryService = queryService;
    }

    @Override
    public DeviceMenuSpaceManager getMenuSpaceManager()
    {
        if (null == menuSpaceManager)
        {
            menuSpaceManager = new DeviceMenuSpaceManager(this);
        }

        return menuSpaceManager;
    }

    @Override
    public IUISpaceManager getUISpaceManager()
    {
        return getBean(IUISpaceManager.class);
    }

    @Override
    public IStateIconManager getStateIconManager()
    {
        if (!(mapObject instanceof IIconMapObject))
        {
            throw new IllegalStateException("State Icon Manager not available for: " + mapObject.getClass());
        }

        synchronized (this)
        {
            if (null == stateIconManager) stateIconManager = new GlobeStateIconManager(mapObject, false);
        }

        return stateIconManager;
    }

    @Override
    public IStateIconManager getPlaybackStateIconManager()
    {
        if (!(mapObject instanceof IIconMapObject)) throw new IllegalStateException("State Icon Manager not available for: " + mapObject.getClass());

        synchronized (this)
        {
            if (null == playbackStateIconManager) playbackStateIconManager = new GlobeStateIconManager(mapObject, true);
        }

        return playbackStateIconManager;
    }

    @Override
    public void setParserKeys(SortedSet<IParserEncryptionKey> sortedKeyList)
    {
        EncryptionKeyMap.setParserKeys(mapObjectKey, sortedKeyList);
    }

    @Override
    public String getName()
    {
        return mapObject.getName();
    }

    @Override
    public void setName(String name)
    {
        mapObject.setName(name);
    }

    @Override
    public void setDescription(String description)
    {
        mapObject.setDescription(description);
        // if (mapObjectDialog != null) mapObjectDialog.setDescription(description);
    }

    @Override
    public void setDefaultCommPath(CommServiceType commType, String commName)
    {
        //
        // Check assoc. plugin to ensure they implement the IDefaultCommsPathManager interface; if not, throw
        // exception back to caller so plugin developer knows they didn't implement the interface
        if (pluginController instanceof IDefaultCommsPathManager)
        {
            defaultCommType = commType;
            defaultCommName = commName;
        } else
        {
            throw new UnsupportedOperationException("IDevicePluginController must implement IDefaultCommsPathManager in order to invoke this method.");
        }
    }

    @Override
    @Nullable
    public IConnectingLineDefinition getConnectingLineDefinition()
    {
        return TrackUtils.getConnectingLineDefinition(mapObject);
    }

    @Override
    public void setConnectingLineDefinition(IConnectingLineDefinition connectingLineDefinition)
    {
        TrackUtils.setConnectingLineDefinition(mapObject, connectingLineDefinition);
    }

    @Override
    public void setTrackStyleDefinition(ITrackStyleDefinition trackStyleDefinition)
    {
        TrackUtils.setTrackStyleDefinition(mapObject, trackStyleDefinition);
    }

    @Override
    @Nullable
    public ITrackStyleDefinition getTrackStyleDefinition()
    {
        return TrackUtils.getTrackStyleDefinition(mapObject);
    }

    @Override
    public void setStopRecordsTrackStyleDefinition(ITrackStyleDefinition trackStyleDefinition)
    {
        TrackUtils.setStopRecordsTrackStyleDefinition(mapObject, trackStyleDefinition);
    }

    @Override
    @Nullable
    public ITrackStyleDefinition getStopRecordsTrackStyleDefinition()
    {
        return TrackUtils.getStopRecordsTrackStyleDefinition(mapObject);
    }

    @Override
    public void setAllowDrag(boolean allowDrag)
    {
        mapObject.setAllowDrag(allowDrag);
    }

    @Override
    public void setAllowContainment(boolean allowContainment)
    {
        mapObject.setAllowDeviceToBeContainer(allowContainment);
    }

    @Override
    public synchronized void addSelectListener(@Nonnull ITrackSelectListener listener)
    {
        if (selectListeners == null) selectListeners = new CopyOnWriteArrayList<>();

        selectListeners.add(listener);
    }

    @Override
    public synchronized void removeSelectListener(@Nonnull ITrackSelectListener listener)
    {
        if (selectListeners != null) selectListeners.remove(listener);
    }

    @Override
    public synchronized void addTrackContextMenuListener(@Nonnull ITrackContextMenuListener listener)
    {
        if (contextMenuListeners == null) contextMenuListeners = new CopyOnWriteArrayList<>();

        contextMenuListeners.add(listener);
    }

    @Override
    public synchronized void removeTrackContextMenuListener(@Nonnull ITrackContextMenuListener listener)
    {
        if (contextMenuListeners != null) contextMenuListeners.remove(listener);
    }

    @Override
    public void showVectorPointer(boolean show, Color color)
    {
        vectorTailEnabled = show;

        if (show && globeVectorPointer == null) globeVectorPointer = new GlobeVectorPointer(mapObject, color);
    }

    @Override
    public void setVectorPointerAttributes(double magnitude, double heading)
    {
        if (!vectorTailEnabled) throw new IllegalStateException("Vector Tail feature disabled");

        if (magnitude < 0.0) throw new IllegalArgumentException("Magnitude must not be negative");
        if (heading < 0.0 || heading > 360.0)
        {
            throw new IllegalArgumentException("Heading must be between 0.0 and 360.0, inclusive");
        }

        globeVectorPointer.setMagnitudeAndHeading(magnitude, heading);
    }

    @Override
    public boolean setVisible(boolean visible)
    {
        return getMapObjectManager().setVisible(mapObjectKey, visible);
    }

    @Override
    public void lockInvisible()
    {
        getMapObjectManager().setVisible(mapObjectKey, false, true);
    }

    @Override
    public void unlockInvisible()
    {
        getMapObjectManager().setVisible(mapObjectKey, true, false);
    }

    @Override
    public List<ITrackSelectListener> getSelectListeners()
    {
        return selectListeners == null ? Collections.emptyList() : Collections.unmodifiableList(selectListeners);
    }

    @Override
    public List<ITrackContextMenuListener> getContextMenuListeners()
    {
        return contextMenuListeners == null ? Collections.emptyList() : Collections.unmodifiableList(contextMenuListeners);
    }

    @Override
    public DataMonitorBuilder createDataMonitorBuilder()
    {
        return dataMonitorServices.createDataMonitorBuilder();
    }

    @Override
    public DataMonitorBuilder createStandardDataMonitorBuilder(StandardDataMonitorType standardDataMonitorType)
    {
        return dataMonitorServices.createStandardDataMonitorBuilder(standardDataMonitorType);
    }

    @Override
    public DataMonitorEvent sendDataMonitorCommand(IRaptorObject target, String command, Map<String, Object> properties)
    {
        return dataMonitorServices.sendDataMonitorCommand(target, command, properties);
    }

    @Override
    public void delegateSort(@Nonnull Comparator<IDataMonitor> comparator)
    {
        dataMonitorServices.delegateSort(comparator);
    }

    @Override
    public State getHighestState()
    {
        return mapObject.getHighestState();
    }

    @Override
    public String getKey()
    {
        return mapObject.getKey();
    }

    @Override
    public String getDisplayName()
    {
        return mapObject.getName();
    }

    @Override
    public String getFamily()
    {
        return mapObject.getFamily();
    }

    @Override
    public String getType()
    {
        return mapObject.getType();
    }

    @Override
    public BufferedImage getIconImage()
    {
        return mapObject.getIconImage();
    }

    /**
     * Check to see if a map object can be muted and if its current mute state matches the global state.
     * If the monitor cannot be muted and global mute is set, turn off global mute. TAKX should be updated
     * after this method is run on all relevant data monitors by calling
     * {@link #updateOnScreenState(IDataMonitor)}.
     *
     * @param dataMonitor   The data monitor to inspect.
     * @param unmuteIfMuted If the data monitor is currently muted but global mute is not set, unmute the monitor if this
     *                      value is {@code true}. Otherwise, ignore the current mute status. This handles the case
     *                      where a data monitor is muted before it is added
     * @since 1.25.6
     */
    private void checkMuteStatus(IDataMonitor dataMonitor, boolean unmuteIfMuted)
    {
        // If this monitor can't be muted, ensure that global mute is not set.
        if (!dataMonitor.canBeMuted())
        {
            isAudioPlaybackEnabled = true;
        }
        // Check to see if the data monitor mute state is different from the global mute state. If it is and global
        // mute in set to true, always mute the data monitor. If global mute is not set and the device should be unmuted
        // (it's not a new data monitor that was set to be muted by default), unmute the monitor.
        else if (dataMonitor.isMuted() == isAudioPlaybackEnabled && (!isAudioPlaybackEnabled || unmuteIfMuted))
        {
            // If the mute state of the data monitor is different from the global mute state, update it
            // via a mute toggle event.
            dataMonitor.onDataMonitorEvent(new DataMonitorEvent(this, DataMonitorEventType.ToggleMute));
        }
    }

    /**
     * Initialize the plugin controller for this map object.  Inject the delegate for the plugin to use.
     * Note: package protected access.
     */
    void initPluginController()
    {
        if (pluginController == null)
        {
            try
            {
                pluginController = mapObjectPlugin.getDeviceController(unitId);
            } catch (Exception e)
            {
                // The exception has already been reported by the AOP wrapper
                logger.error("Failed to get DeviceController from plugin: {}", mapObjectPlugin.getClass());
            }

            if (pluginController == null)
            {
                pluginController = new DefaultDevicePluginController(mapObjectPlugin.getFamily(), mapObjectPlugin.getType(), unitId);
            }

            //
            // Inject in the IDataMonitorFactory as a convenience for the plugin developer
            pluginController.setDataStructureFactory(mapObjectPlugin.getDataStructureFactory());

            try
            {
                pluginController.setDevicePluginDelegate(this);
            } catch (Exception e)
            {
                logger.warn(PluginConstants.PLUGIN_EXCEPTION + ": Exception in setting map object delegate in MapObjectController ", e);
            }

            if (mapObject instanceof IIconMapObject && mapObject.isRestoring() && mapObject.canHavePluginState())
            {
                restorePluginState();
            }
        }
    }

    /**
     * Restore the plugin state from the DB and hand it to the plugin controller.
     *
     * @since 2.29
     */
    private void restorePluginState()
    {
        // Per the interface contract, only invoke restore() when there
        // is actual non-null state that is to be restored

        final var state = getBean(MapObjectStateLoader.class).getPluginState(mapObjectKey);

        final String stateString = null != state ? state.getState().trim() : "";
        if (!stateString.isEmpty())
        {
            try
            {
                pluginController.restore(stateString);
            } catch (Exception e)
            {
                logger.warn("{}: Exception in restoring state of plugin controller: {}",
                        PluginConstants.PLUGIN_EXCEPTION, mapObjectKey, e);
            }
        }
    }

    private void processDataMonitors(IRaptorDataMessage rdm)
    {
        dataMonitorServices.getDataMonitors().forEach(idm -> idm.analyze(rdm));

        updateOnScreenState(null);
    }

    private void processDataMonitors(IPrePersistRaptorCommandMessage rcm)
    {
        if (logger.isDebugEnabled()) logger.debug("Process data monitors for {}", rcm.getUniqueKey());

        for (IDataMonitor idm : dataMonitorServices.getDataMonitors())
        {
            idm.onDataMonitorEvent(
                    new DataMonitorEvent(
                            mapObjectPlugin,
                            DataMonitorEventType.Command,
                            Map.of(DataMonitorEventType.Command.name(), rcm)));
        }
    }

    private void updateOnScreenState(IDataMonitor changedDataMonitor)
    {
        // Find the highest current state of all the Map Object data monitors

        State highestState = dataMonitorServices.getDataMonitors()
                .stream()
                .filter(IDataMonitor::isAffectsAudioAndIcons)
                .map(IDataMonitor::getCurrentState)
                .max(Comparator.naturalOrder())
                .orElse(State.Unknown);

        if (mapObject instanceof IIconMapObject)
        {
            State containedState = ((IIconMapObject) mapObject).calculateContainedHighestState();
            if (containedState.compareTo(highestState) > 0)
            {
                highestState = containedState;
            }
        }

        State currentHighestState = mapObject.getHighestState();
        mapObject.setHighestState(highestState);

        //the actual state of the map object has changed so log a state change event
        if (currentHighestState != highestState)
        {
            //string detailing the map objects transition from one state to another
            String details = mapObject.getName() + " went from " + currentHighestState + " to " + highestState;

            // CORE-2596 Allow a data monitor to decline logging of the state change event to the event log.
            // If a data monitor changed state, and it is at the highest state, allow it to decline
            if (changedDataMonitor == null || (changedDataMonitor.logStateChangeEvent(currentHighestState, highestState)))
            {
                getBean(IRaptorEventLogManager.class).logStateChange(new Date(), mapObject, highestState, details);
            }
        }
        getBean(ICoreAudioManager.class).processDataMonitors(this);
    }

    private void processLocation(IRaptorDataMessage rdm)
    {
        if (rdm.hasLocation())
        {
            mapObject.setLocation(rdm.getLocation(), rdm.getRelevantTime(), rdm.isManuallyGeneratedLocation());
        }
    }

    /**
     * Delete supplemental (non-database) data associated with the map object controlled by this controller (files on the
     * file system, etc.).
     *
     * @since 2.38 Renamed from {@code deleteExtraData()} to avoid confusion with new extraData field methods
     */
    protected void deleteSupplementalData()
    {
    }

    @Override
    public CommServiceType getDefaultCommType()
    {
        return defaultCommType;
    }

    @Override
    public String getDefaultCommName()
    {
        return defaultCommName;
    }

    @Override
    public void updateDefaultCommPath(CommServiceType commType, String commName)
    {
        if (defaultCommType == commType && Objects.equals(defaultCommName, commName)) return;

        defaultCommType = commType;
        defaultCommName = commName;

        if (pluginController instanceof IDefaultCommsPathManager commsPathManager)
        {
            try
            {
                commsPathManager.setDefaultCommPath(commType, commName);
            } catch (Exception e)
            {
                logger.warn(PluginConstants.PLUGIN_EXCEPTION + ": Exception in setting default comm path in MapObjectController ", e);
            }
        }
    }

    @Override
    public synchronized void sendToDevice(IPrePersistRaptorDataMessage message, boolean createIfNew, String passkey)
            throws IllegalArgumentException, IllegalAccessException
    {
        if (messageRouter == null) messageRouter = new MapObjectMessageRouter(family, type, unitId);
        messageRouter.sendToDevice(message, createIfNew, passkey);
    }

    @Override
    public void revokeShare(String sourceDeviceFamily, String sourceDeviceType, String sourceDeviceId)
    {
        //
        // Stop the source from sending messages to me (the target)
        //
        getBean(ISharedMapObjectManager.class).unregisterShare(sourceDeviceFamily, sourceDeviceType, sourceDeviceId, family, type, unitId);
    }

    @Override
    public void delete()
    {
        // Delete all our custom data monitors
        if (customDataMonitors != null)
        {
            customDataMonitors.forEach(this::removeCustomDataMonitor);
            customDataMonitors.clear();
        }

        mapObjectPlugin.deleteDevice(unitId);
        pluginController.delete();
        getBean(ICoreAudioManager.class).clearAudioAlarms(this);

        deleteSupplementalData();
    }

    @Override
    public synchronized void addDisplayHandler(@Nonnull ITrackDisplayHandler trackDisplayHandler)
    {
        if (trackDisplayHandlers == null) trackDisplayHandlers = new CopyOnWriteArrayList<>();
        trackDisplayHandlers.add(trackDisplayHandler);
    }

    @Override
    public synchronized void removeDisplayHandler(@Nonnull ITrackDisplayHandler trackDisplayHandler)
    {
        if (trackDisplayHandlers != null) trackDisplayHandlers.remove(trackDisplayHandler);
    }

    @Override
    public ILayerManagerDelegate getLayerManagerDelegate()
    {
        return getBean(IRaptorLayerManager.class);
    }

    @Override
    public void setIconToolTipText(String tooltip)
    {
        if (getGlobe().getGlobeMapObjectManager().getGlobeMapObject(uniqueKey) instanceof IGlobeMarkerMapObject globeMarkerMapObject)
        {
            globeMarkerMapObject.setToolTipText(tooltip);
        }
    }

    @Override
    public IExplorerManagerDelegate getExplorerManagerDelegate()
    {
        return getBean(IExplorerManager.class);
    }

    /**
     * Update the {@link ITrackInfo} object that is responsible for maintaining track metadata,
     * including track count the geographic and time boundaries of the tracks.
     * <p>
     * Please note that the map object is NOT PERSISTED in this method. It is currently persisted by the sole caller
     * of this method, {@link #receiveMessage(IRaptorDataMessage)}. If additional calls to this
     * method are added, the map object must be explicitly persisted by the caller.
     *
     * @param rdm The message from which data will be extracted to update the track info for this map object.
     * @see #receiveMessage(IRaptorDataMessage)
     * @since 1.13
     */
    private void updateTrackInfo(IRaptorDataMessage rdm)
    {
        if (rdm.isManuallyGeneratedLocation())
        {
            logger.debug("Ignored MANUAL location message on TrackInfo update");
            return;
        }

        mapObject.getTrackInfo().updateForRdm(rdm);
    }

    @Override
    public boolean canUndoLastLocation()
    {
        return getUndoHandler().isUndoAvailable();
    }

    private IMapObjectManager getMapObjectManager()
    {
        return getBean(IMapObjectManager.class);
    }

    @Override
    protected IPersistableState getPersistableState()
    {
        return pluginController;
    }

    /**
     * Timed queue to handle batch persist operations.  This is static so that we share it across all MOCs
     * and avoid allocating a TimeBoundedTask for every MO (which is what was done previously).
     *
     * @since 5.7 (made a named inner class)
     */
    private static class LocationUpdateQueue extends TimeBatchedQueue<Set<MapObjectController>, MapObjectController>
    {
        private static final int LOCATION_HANDLER_DELAY_MS = 500; // 0.5 seconds
        private static final int LOCATION_HANDLER_MAX_WAIT_MS = 2_000; // 2 seconds

        LocationUpdateQueue()
        {
            super("MOC Location Updater", LOCATION_HANDLER_DELAY_MS, LOCATION_HANDLER_MAX_WAIT_MS, HashSet::new);
        }

        @Override
        protected void runBatch(Set<MapObjectController> batch)
        {
            batch.forEach(MapObjectController::persistLocationData);
        }

        boolean batchContains(MapObjectController controller)
        {
            return batch.contains(controller);
        }
    }

    /**
     * Specific implementation of data monitor services for this Map Object.
     *
     * @since 2.29
     */
    public class MapObjectDataMonitorServices extends ADataMonitorServices<IDevicePluginDelegate>
    {
        public MapObjectDataMonitorServices(IDevicePluginDelegate delegate, IMapObject mapObject)
        {
            super(delegate, mapObject.getFamily(), mapObject.getType(), mapObject.getName(), mapObject.getKey(), mapObject.getIconImage());
        }

        @Override
        protected ADataMonitorDelegate makeDataMonitorDelegate(IDevicePluginDelegate delegate)
        {
            return new DataMonitorDelegate(delegate);
        }

        @Override
        public void addDataMonitor(@Nonnull IDataMonitor dataMonitor)
        {
            super.addDataMonitor(dataMonitor);

            // Update the mute state of the monitor and possibly this map object if it is a map object data monitor.
            if (dataMonitor.isAffectsAudioAndIcons()) checkMuteStatus(dataMonitor, false);

            updateOnScreenState(null);
        }

        @Override
        public void delegateSort(@Nonnull Comparator<IDataMonitor> comparator)
        {
            firePropertyChange(DATA_MONITOR_SORT_ORDER_CHANGED, customComparator, comparator);

            customComparator = comparator;
        }

        @Override
        public State getHighestState()
        {
            return mapObject.getHighestState();
        }

        @Override
        public void sortDataMonitors()
        {
            firePropertyChange(DATA_MONITOR_SORT_ORDER_CHANGED, customComparator, customComparator);
        }

        protected class DataMonitorDelegate extends ADataMonitorDelegate
        {
            public DataMonitorDelegate(IDevicePluginDelegate delegate)
            {
                super(delegate, delegate::getQueryService);
            }

            @Override
            public void logAlarmEvent(String message, String details)
            {
                logDeviceEvent(message, details);
            }

            @Override
            public void logDataMonitorStateChangeEvent(IDataMonitor dataMonitor, State curState, String sDetails)
            {
                eventLogManager.logDataMonitorStateChange(new Date(), mapObject, dataMonitor, curState, sDetails);
            }

            @Override
            public void setCustomIcon(URL imagePathURL)
            {
                setIconImage(imagePathURL);
            }

            @Override
            public IStateIconManager getStateIconManager()
            {
                return MapObjectController.this.getStateIconManager();
            }
        }
    }

    /**
     * Stub DM services for use when the MO has the NoAlarms option set.
     *
     * @since 2.29
     */
    public static class NoopDataMonitorServices extends ADataMonitorServices<IDevicePluginDelegate>
    {
        public NoopDataMonitorServices()
        {
            super(null, null, null, null, null, null);
        }

        @Override
        protected ADataMonitorDelegate makeDataMonitorDelegate(IDevicePluginDelegate delegate)
        {
            return null;
        }
    }
}
