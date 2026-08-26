package gov.takx.core.view.gui.explorer.controller;

import gov.tak.api.icons.TakColor;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.MapObjectOptions;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.core.explorer.ExplorerManager;
import gov.takx.core.explorer.IExplorer;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.test.AIntegrationTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@PseudoBean
public class TeamControllerTest extends AIntegrationTest
{
    private static final String TEAM_SNIPER = "Team/Sniper";
    private static final String TEAM_HQ = "Team/HQ";
    private static final String TEAM_TEAM_MEMBER = "Team/Team Member";
    private static final String TEAM_GREEN_TEAM = "Team/Green Team";
    private static final String TEAM_CYAN_TEAM = "Team/Cyan Team";
    private static final String TEAM_TEAM_MEMBERS = "Team/Team Members";

    @Inject
    private ExplorerManager explorerManager;

    private IExplorer explorer;
    private IMapObject mapObject;
    private IExplorable mapObjectExplorable;

    @BeforeEach
    public void beforeEach()
    {
        final var spec = MapObjectSpec.builder(IMarkerMapObject.MARKER_FAMILY, IMarkerMapObject.MARKER_TYPE)
                .uid("UID") // Using a constant UID verifies that the controller cleans up on MO deletion
                .name("Test MO")
                .mapObjectOptions(MapObjectOptions.STANDARD_RESOURCE_OPTIONS)
                .build();
        mapObject = getBean(IMapObjectManager.class).getOrCreateMapObject(spec);
        mapObjectExplorable = explorer.getExplorable(mapObject.getKey());
    }

    @AfterEach
    public void afterEach()
    {
        getBean(IMapObjectManager.class).deleteMapObject(mapObject);
    }

    @Override
    protected void postProjectOpen()
    {
        explorer = explorerManager.getDefaultExplorerDelegate();
    }

    @Test
    void changingRoleAttribute_updatesGroupLocation()
    {
        assertThat(explorer.getExplorableByPath(TEAM_SNIPER)).isNull();

        mapObject.setMetaString(StandardAttributeNames.ATAK_ROLE_TYPE, "Sniper");

        final IExplorable sniperGroup = explorer.getExplorableByPath(TEAM_SNIPER);
        assertThat(sniperGroup).isNotNull();
        assertThat(mapObjectExplorable.getGroups()).contains(sniperGroup);

        mapObject.setMetaString(StandardAttributeNames.ATAK_ROLE_TYPE, "HQ");

        assertThat(explorer.getExplorableByPath(TEAM_SNIPER)).isNull(); // Sniper group should have been removed
        final IExplorable hqGroup = explorer.getExplorableByPath(TEAM_HQ); // Should now be in HQ group
        assertThat(hqGroup).isNotNull();
        assertThat(mapObjectExplorable.getGroups()).contains(hqGroup);

        // Role "Team Member" is not added to the overlays
        mapObject.setMetaString(StandardAttributeNames.ATAK_ROLE_TYPE, "Team Member");
        assertThat(explorer.getExplorableByPath(TEAM_HQ)).isNull(); // HQ group should have been removed
        assertThat(explorer.getExplorableByPath(TEAM_TEAM_MEMBER)).isNull(); // Should not have been created
        assertThat(mapObjectExplorable.getGroups()).hasSize(1); // Should only be in "Devices" now
    }

    @Test
    void changingTeamAttribute_updatesGroupLocation()
    {
        PreferenceEntry.SA_REPORTING_TEAM.put("red"); // We are red team

        mapObject.setMetaString(StandardAttributeNames.TEAM, TakColor.GREEN.colorName);
        IExplorable teamGroup = explorer.getExplorableByPath(TEAM_GREEN_TEAM);
        assertThat(teamGroup).isNotNull();
        assertThat(mapObjectExplorable.getGroups()).contains(teamGroup);

        mapObject.setMetaString(StandardAttributeNames.TEAM, TakColor.CYAN.colorName);
        assertThat(explorer.getExplorableByPath(TEAM_GREEN_TEAM)).isNull(); // Green team group should have been removed
        teamGroup = explorer.getExplorableByPath(TEAM_CYAN_TEAM);
        assertThat(teamGroup).isNotNull();
        assertThat(mapObjectExplorable.getGroups()).contains(teamGroup);

        // Now make them a member of our team
        mapObject.setMetaString(StandardAttributeNames.TEAM, TakColor.RED.colorName);
        assertThat(explorer.getExplorableByPath(TEAM_CYAN_TEAM)).isNull(); // Cyan team group should have been removed
        teamGroup = explorer.getExplorableByPath(TEAM_TEAM_MEMBERS);
        assertThat(teamGroup).isNotNull();
        assertThat(mapObjectExplorable.getGroups()).contains(teamGroup);
    }
}
