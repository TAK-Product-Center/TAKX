package gov.takx.core.emergency;

import gov.tak.api.maps.PointMapItem;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.emergency.IEmergencyAlertManager;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.core.explorer.ExplorerManager;
import gov.takx.core.explorer.IExplorer;
import gov.takx.core.globe.emergency.EmergencyAlertAnimator;
import gov.takx.test.AIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.7
 */
@PseudoBean
public class EmergencyOverlaysControllerTest extends AIntegrationTest
{
    private final IEmergencyAlertManager emergencyAlertManager = getBean(IEmergencyAlertManager.class);
    private final ExplorerManager explorerManager = getBean(ExplorerManager.class);

    private IExplorer explorer;
    private PointMapItem pointMapItem;

    @Override
    protected void postProjectOpen()
    {
        explorer = explorerManager.getDefaultExplorerDelegate();
    }

    @BeforeEach
    void beforeEach()
    {
        final var spec = MapObjectSpec.builder("TEST", "TEST").name("Duck").build();
        pointMapItem = (PointMapItem) getBean(IMapObjectManager.class).getOrCreateMapObject(spec);
    }

    @Test
    @SuppressLogging(EmergencyAlertAnimator.class)
    void addAlert_createsAlertNode()
    {
        final var alert = emergencyAlertManager.addAlert(pointMapItem, "911 Alert", "message");
        assertThat(alert).isNotNull();
        assertThat(alert.mapItem()).isSameAs(pointMapItem);
        assertThat(alert.callsign()).isEqualTo("Duck");
        assertThat(alert.type()).isEqualTo("911 Alert");
        assertThat(alert.message()).isEqualTo("message");

        final var alertNode = explorer.getExplorable(EmergencyOverlaysController.makeAlertUniqueKey(alert));
        assertThat(alertNode).isNotNull();
        assertThat(alertNode.getDisplayName()).isEqualTo(alert.callsign());
        assertThat(alertNode.createQuickActions()).hasSize(1);
    }

    @Test
    @SuppressLogging(EmergencyAlertAnimator.class)
    void removeAlertByValues_removesAlertNode()
    {
        final var alert = emergencyAlertManager.addAlert(pointMapItem, "911 Alert", "message");
        emergencyAlertManager.removeAlert(pointMapItem);

        assertThat(explorer.getExplorable(EmergencyOverlaysController.makeAlertUniqueKey(alert))).isNull();
    }

    @Test
    @SuppressLogging(EmergencyAlertAnimator.class)
    void removeAlertByAlert_removesAlertNode()
    {
        final var alert = emergencyAlertManager.addAlert(pointMapItem, "911 Alert", "message");
        emergencyAlertManager.removeAlert(alert);

        assertThat(explorer.getExplorable(EmergencyOverlaysController.makeAlertUniqueKey(alert))).isNull();
    }
}
