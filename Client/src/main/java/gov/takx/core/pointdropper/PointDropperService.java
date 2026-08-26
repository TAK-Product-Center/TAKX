package gov.takx.core.pointdropper;

import com.atakmap.coremap.maps.time.CoordinatedTime;
import gov.tak.api.icons.IIconSetManager;
import gov.tak.api.maps.Marker;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.platform.comms.commo.DeviceDisplayType;
import gov.takx.api.annotation.NodeUUID;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.pointdropper.PointDropperActivation;
import gov.takx.api.globe.IGlobeClickService;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.pointdropper.IPointDropperService;
import gov.takx.api.pointdropper.PointDropperConfiguration;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.URI;

import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;

/**
 * Service responsible for allowing user to drop points by clicking on a location on the globe, or via the API, to
 * drop a point at a specified location.
 *
 * @since 3.0
 */
@ApplicationScoped
public class PointDropperService implements IPointDropperService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String USER_PROMPT = "Click map to place points. Press ESC to cancel.";

    private final IMapObjectManager mapObjectManager;
    private final IIconSetManager iconSetManager;
    private final IGlobeClickService globeClickService;
    private final String nodeUuid;
    private final Event<PointDropperActivation> pointDropperActivationEvent;

    private PointDropperConfiguration configuration;
    private boolean pointDropperActive;

    @Inject
    public PointDropperService(IMapObjectManager mapObjectManager, IIconSetManager iconSetManager,
                               IGlobeClickService globeClickService, @NodeUUID String nodeUuid,
                               Event<PointDropperActivation> pointDropperActivationEvent)
    {
        this.mapObjectManager = mapObjectManager;
        this.iconSetManager = iconSetManager;
        this.globeClickService = globeClickService;
        this.nodeUuid = nodeUuid;
        this.pointDropperActivationEvent = pointDropperActivationEvent;
    }

    /**
     * Stop this service by unregistering from all globes.
     */
    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices event)
    {
        deactivatePointDropper();
    }

    @Override
    public void activatePointDropper(@Nonnull PointDropperConfiguration configuration)
    {
        activatePointDropper(configuration, true);
    }

    /**
     * Activate this service using the given configuration.
     *
     * @param configuration Configuration to install
     * @param interactive Pass {@code true} to prepare for interactive (user) use, {@code false} for programmatic use
     * @since 5.3
     */
    private void activatePointDropper(@Nonnull PointDropperConfiguration configuration, boolean interactive)
    {
        logger.debug("activate: interactive {} using {}", interactive, configuration);

        if (pointDropperActive) deactivatePointDropper();

        this.configuration = configuration;
        pointDropperActive = true;

        if (interactive)
        {
            globeClickService.deactivate(); // Just in case a plugin has one active
            globeClickService.activate(null, USER_PROMPT, this::dropPoint, configuration.deactivator(), this::deactivatePointDropper);
        }

        pointDropperActivationEvent.fire(new PointDropperActivation(true));
    }

    @Override
    public void deactivatePointDropper()
    {
        logger.debug("deactivate: active {}", pointDropperActive);

        // Firing this event can create an infinite loop of events to be fired. This check breaks the loop.
        if (pointDropperActive)
        {
            pointDropperActive = false;
            globeClickService.deactivate();
            pointDropperActivationEvent.fire(new PointDropperActivation(false));
            configuration.deactivationHandler().run();
        }
    }

    @Override
    public boolean isActive()
    {
        return pointDropperActive;
    }

    @Override
    public PointDropperConfiguration getConfiguration()
    {
        return configuration;
    }

    @Override
    @Nonnull
    public IMapObject dropPoint(@Nonnull PointDropperConfiguration configuration, @Nonnull ILocation location)
    {
        activatePointDropper(configuration, false);
        final var mapObject = dropPoint(location);
        deactivatePointDropper();

        //noinspection DataFlowIssue - can't be null in this use case
        return mapObject;
    }

    @Override
    @Nullable
    public IMapObject dropPoint(@Nonnull ILocation location)
    {
        if (!pointDropperActive) return null;

        final var pluginDefinition = configuration.pluginDefinition();

        final var specBuilder = MapObjectSpec.builder(pluginDefinition)
                .location(location)
                .cotType(configuration.cotType())
                .initializer(mo -> {
                    // If we've configured a specific icon, then prevent any further adaptation
                    //noinspection DataFlowIssue
                    final String uriString = pluginDefinition.getIconURI().map(URI::toString).orElse(null);
                    if (!StringUtils.isEmpty(uriString)) mo.setMetaBoolean(StandardAttributeNames.ADAPT_MARKER_ICON, false);

                    // All user created markers should be tagged "archive", so other EUDs will not apply stale processing
                    mo.setMetaString(StandardAttributeNames.ARCHIVE, "");

                    // Ensure that points dropped from an icon set properly transfer across CoT
                    final URI imageUri = mo.getIconImageURI();
                    if (iconSetManager.isIconSetUri(imageUri)) mo.setMetaString(StandardAttributeNames.ICONSET_PATH, imageUri.getSchemeSpecificPart());

                    mo.setMetaString(StandardAttributeNames.MIL_SYM, configuration.sidc());
                    mo.setMetaString(StandardAttributeNames.PARENT_UID, nodeUuid);
                    mo.setMetaString(StandardAttributeNames.PARENT_TYPE, PreferenceEntry.SA_REPORTING_DISPLAY_TYPE.getValueAs(DeviceDisplayType.class).getCotType());
                    mo.setMetaString(StandardAttributeNames.PARENT_CALLSIGN, PreferenceEntry.CALL_SIGN.getStringValue());
                    mo.setMetaString(StandardAttributeNames.PRODUCTION_TIME, new CoordinatedTime().toString());
                    if (mo instanceof Marker marker)
                    {
                        marker.setStyle(marker.getStyle() | Marker.STYLE_ROTATE_HEADING_MASK);
                        marker.setStyle(marker.getStyle() & ~Marker.STYLE_ROTATE_HEADING_NOARROW_MASK);
                    }
                    configuration.initializer().accept(mo);
                });

        final String customName = configuration.nameGenerator().generateName();
        if (customName != null) specBuilder.name(customName);

        final IMapObject mapObject = mapObjectManager.getOrCreateMapObject(specBuilder.build());
        configuration.dropHandler().accept(mapObject);
        return mapObject;
    }
}
