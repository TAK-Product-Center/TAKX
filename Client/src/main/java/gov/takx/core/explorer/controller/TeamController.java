package gov.takx.core.explorer.controller;

import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.icons.RoleIconColorizer;
import gov.tak.api.icons.TeamColorMapper;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.explorer.ExplorerUtils;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorerDelegateListener;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.core.explorer.IExplorer;
import gov.takx.core.explorer.IExplorerManager;
import gov.takx.core.explorer.common.IPathExplorableGroup;
import gov.takx.core.explorer.common.MapObjectExplorable;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.commons.text.WordUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static gov.takx.core.explorer.controller.path.ExplorerPathConstants.TEAM_PATH;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_TEAM;
import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_ICONS;

/**
 * Controller to manage the location of markers based on their team and role, as specified in CoT detail {@link DetailConstants#GROUP}.
 *
 * @since 5.3
 */
@ApplicationScoped
public class TeamController implements IExplorerDelegateListener
{
    private static final String TEAM_MEMBERS = "Team Members";
    private static final List<String> TEAM_MEMBERS_PATH = ExplorerUtils.extendPath(TEAM_PATH, TEAM_MEMBERS);

    private final IExplorerManager explorerManager;

    private final Map<String, MapObjectExplorable> mapObjectExplorables = new ConcurrentHashMap<>();

    private final Map<String, String> mapObjectRoleNames = new ConcurrentHashMap<>(); // Key MO UID, MO's role name
    private final Map<String, IPathExplorableGroup> mapObjectRoleGroups = new ConcurrentHashMap<>(); // Key MO UID, MO's role group node
    private final Map<String, IPathExplorableGroup> roleGroupNodes = new ConcurrentHashMap<>(); // Key role name, role group node

    private final Map<String, String> mapObjectTeamNames = new ConcurrentHashMap<>(); // Key MO UID, MO's team name
    private final Map<String, IPathExplorableGroup> mapObjectTeamGroups = new ConcurrentHashMap<>(); // Key MO UID, MO's team group node
    private final Map<String, IPathExplorableGroup> teamGroupNodes = new ConcurrentHashMap<>(); // Key team name, team group node

    private final List<Map<String, ?>> allMapObjectMaps =
            List.of(mapObjectExplorables,
                    mapObjectRoleNames, mapObjectRoleGroups, roleGroupNodes,
                    mapObjectTeamNames, mapObjectTeamGroups, teamGroupNodes);

    private final Map<String, BufferedImage> roleImages = new ConcurrentHashMap<>();
    private final Map<String, BufferedImage> teamImages = new ConcurrentHashMap<>();

    private final IPreferenceMonitor<String> teamMonitor = SA_REPORTING_TEAM.newStringMonitor(this::teamChanged, false);

    private IExplorer explorer;
    private IPathExplorableGroup rootGroup;

    @Inject
    TeamController(IExplorerManager explorerManager)
    {
        this.explorerManager = explorerManager;
    }

    @Override
    public void onExplorerEvent(String eventName, IExplorable affectedExplorable, Object oldValue, Object newValue, boolean isGroupTreeSource)
    {
        if (eventName.equals(EXPLORABLE_ADDED_EVENT) && affectedExplorable instanceof MapObjectExplorable mapObjectExplorable)
        {
            final var mapObject = mapObjectExplorable.getMapObject();

            // Limit to just markers since that is what is created from received CoT SA messages
            if (IMarkerMapObject.MARKER_FAMILY.equals(mapObject.getFamily()) && IMarkerMapObject.MARKER_TYPE.equals(mapObject.getType()))
            {
                final String uid = mapObject.getUid();

                if (!mapObjectExplorables.containsKey(uid))
                {
                    final var attributeSet = mapObject.getAttributeSet();
                    attributeSet.addPropertyChangeListener(StandardAttributeNames.TEAM, e -> updateTeam(mapObjectExplorable));
                    attributeSet.addPropertyChangeListener(StandardAttributeNames.ATAK_ROLE_TYPE, e -> updateRole(mapObjectExplorable));
                    mapObjectExplorables.put(uid, mapObjectExplorable);
                }

                if (mapObject.hasMetaValue(StandardAttributeNames.ATAK_ROLE_TYPE)) updateRole(mapObjectExplorable);
                if (mapObject.hasMetaValue(StandardAttributeNames.TEAM)) updateTeam(mapObjectExplorable);
            }
        }
    }

    private void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        final String uid = event.getMapObject().getUid();
        allMapObjectMaps.forEach(m -> m.remove(uid));
    }

    /**
     * Update the overlays, placing the map object explorable in its appropriate role group.
     */
    private void updateRole(@Nonnull MapObjectExplorable explorable)
    {
        final var mapObject = explorable.getMapObject();
        final String role = mapObject.getMetaString(StandardAttributeNames.ATAK_ROLE_TYPE, "").trim();

        // We only create groups for roles other than "Team Member"
        if (!"Team Member".equalsIgnoreCase(role))
        {
            updateGroupLocation(mapObject, explorable, role, role, mapObjectRoleNames, mapObjectRoleGroups, roleGroupNodes, this::getRoleImage);
        } else
        {
            removePreviousGroupNode(mapObject, explorable, mapObjectRoleGroups, roleGroupNodes);
        }
    }

    /**
     * Called when our team name (color) has changed.  Rebuild the team-related nodes.
     * @since 5.5
     */
    private void teamChanged(@Nonnull String newTeam)
    {
        mapObjectTeamNames.clear(); // Team names are changing
        mapObjectExplorables.values().forEach(this::updateTeam);

        // Update the "Team Members" icon because the color will have changed
        final var group = rootGroup.getPath(TEAM_MEMBERS_PATH);
        if (group != null) group.setIconImage(getTeamImage(newTeam));
    }

    /**
     * Update the overlays, placing the map object explorable in its appropriate team group.
     */
    private void updateTeam(@Nonnull MapObjectExplorable explorable)
    {
        final var mapObject = explorable.getMapObject();
        final String team = mapObject.getMetaString(StandardAttributeNames.TEAM, "").trim().toLowerCase();
        final boolean ourTeam = SA_REPORTING_TEAM.getStringValue().equalsIgnoreCase(team);
        final String teamGroupName = ourTeam ? TEAM_MEMBERS : WordUtils.capitalize(team + " Team");

        updateGroupLocation(mapObject, explorable, team, teamGroupName, mapObjectTeamNames, mapObjectTeamGroups, teamGroupNodes, this::getTeamImage);
    }

    /**
     * Update the overlays to place the given map object explorable into its appropriate group.
     * 
     * @param mapObject Map Object to process
     * @param explorable The map object's explorable
     * @param name The team/role name
     * @param groupName The group name under which to place the explorable
     * @param mapObjectGroupNames recorded group names for the map object
     * @param mapObjectGroups recorded group (explorable) for the map object
     * @param groupNodes current group nodes for teams/roles
     * @param groupImageSupplier function to produce the appropriate image for a group node if one is created
     */
    private void updateGroupLocation(IMapObject mapObject, IExplorable explorable, String name, String groupName,
                                     Map<String, String> mapObjectGroupNames,
                                     Map<String, IPathExplorableGroup> mapObjectGroups,
                                     Map<String, IPathExplorableGroup> groupNodes,
                                     Function<String, BufferedImage> groupImageSupplier)
    {
        final String mapObjectUid = mapObject.getUid();

        if (name.equalsIgnoreCase(mapObjectGroupNames.get(mapObjectUid))) return; // No change

        removePreviousGroupNode(mapObject, explorable, mapObjectGroups, groupNodes);

        // Add it to its new location
        if (!name.isEmpty())
        {
            final var groupNode = groupNodes.computeIfAbsent(groupName, k -> {
                final var group = rootGroup.getOrCreatePath(ExplorerUtils.extendPath(TEAM_PATH, groupName));
                group.setIconImage(groupImageSupplier.apply(name));
                return group;
            });

            explorable.addGroup(groupNode);

            mapObjectGroupNames.put(mapObjectUid, name);
            mapObjectGroups.put(mapObjectUid, groupNode);
        }
    }

    private void removePreviousGroupNode(IMapObject mapObject, IExplorable explorable,
                                         Map<String, IPathExplorableGroup> mapObjectGroups,
                                         Map<String, IPathExplorableGroup> groupNodes)
    {
        // Remove it from any previous group, and remove the group if it is now empty
        final var previousGroupNode = mapObjectGroups.remove(mapObject.getUid());
        if (previousGroupNode != null)
        {
            explorable.removeGroup(previousGroupNode);
            if (!previousGroupNode.hasChildren())
            {
                groupNodes.remove(previousGroupNode.getDisplayName());
                explorer.removeExplorable(previousGroupNode, false);
            }
        }
    }

    /**
     * @return Image to use for the given role
     */
    private BufferedImage getRoleImage(String role)
    {
        return roleImages.computeIfAbsent(role, k -> {
            final String imagePath = String.format("roles/%s.png", role.replace(" ", "").toLowerCase());
            return UIResources.getImage(TAK_ASSETS_ICONS, imagePath);
        });
    }

    /**
     * @return Image to use for the given team, colorized by team name
     */
    private BufferedImage getTeamImage(String team)
    {
        return teamImages.computeIfAbsent(team, k -> {
            final int color = TeamColorMapper.teamToColor(team);
            return RoleIconColorizer.colorize("roles/team.png", new Color(color, true));
        });
    }

    /**
     * Must start after ExplorerManager.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_ExplorerManager + 1) StartProjectServices ignored)
    {
        explorer = explorerManager.getDefaultExplorerDelegate();
        rootGroup = explorer.getController().getRoot();
        explorer.addExplorerDelegateListener(this);
        teamMonitor.start();
    }

    private void stopService(@Observes @Priority(ServiceOrdering.START_GenericCoreService) StopProjectServices ignored)
    {
        explorer = null;
        allMapObjectMaps.forEach(Map::clear);
        teamMonitor.stop();
    }
}
