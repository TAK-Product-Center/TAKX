package gov.takx.core.view.gui.explorer.controller;

import gov.takx.api.explorer.IGeofenceRule;
import gov.takx.api.explorer.INameRule;
import gov.takx.api.explorer.IProximityRule;
import gov.takx.core.explorer.controller.IExplorerController;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.ExplorerTestConstants;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class RuleGroupsTest extends AContainerTest
{
    @Test
    public void addRemoveRuleFactoryTest()
    {
        ExplorerTestConstants data = new ExplorerTestConstants();
        IExplorerController explorer = data.controller;

        assertThat(explorer.getRuleFactories()).isEmpty();

        // Start adding rule factories
        explorer.addRuleFactory(INameRule.class, data.NAME_RULE_FACTORY);
        explorer.addRuleFactory(IGeofenceRule.class, data.GEOFENCE_RULE_FACTORY);
        explorer.addRuleFactory(IProximityRule.class, data.PROXIMITY_RULE_FACTORY);
        assertThat(explorer.getRuleFactories()).hasSize(3);
        assertThat(explorer.getRuleFactory(data.NAME_RULE_FACTORY.getType())).isEqualTo(data.NAME_RULE_FACTORY);
        assertThat(explorer.getRuleFactories()).contains(data.NAME_RULE_FACTORY);
        assertThat(explorer.getRuleFactory(data.GEOFENCE_RULE_FACTORY.getType())).isEqualTo(data.GEOFENCE_RULE_FACTORY);
        assertThat(explorer.getRuleFactories()).contains(data.GEOFENCE_RULE_FACTORY);
        assertThat(explorer.getRuleFactory(data.PROXIMITY_RULE_FACTORY.getType())).isEqualTo(data.PROXIMITY_RULE_FACTORY);
        assertThat(explorer.getRuleFactories()).contains(data.PROXIMITY_RULE_FACTORY);

        // Remove rule factories
        explorer.removeRuleFactory(data.NAME_RULE_FACTORY);
        assertThat(explorer.getRuleFactories()).hasSize(2);
        assertThat(explorer.getRuleFactory(data.NAME_RULE_FACTORY.getType())).isNull();
        assertThat(explorer.getRuleFactories()).doesNotContain(data.NAME_RULE_FACTORY);
        explorer.removeRuleFactory(data.GEOFENCE_RULE_FACTORY);
        explorer.removeRuleFactory(data.PROXIMITY_RULE_FACTORY);
        assertThat(explorer.getRuleFactories()).isEmpty();
        assertThat(explorer.getRuleFactory(data.GEOFENCE_RULE_FACTORY.getType())).isNull();
        assertThat(explorer.getRuleFactory(data.PROXIMITY_RULE_FACTORY.getType())).isNull();
    }

    @Test
    public void rulesEnabledTest()
    {
        ExplorerTestConstants data = new ExplorerTestConstants();
        IExplorerController explorer = data.controller;
        explorer.setRulesEnabled(false);
        assertThat(explorer.areRulesEnabled()).isFalse();
        explorer.setRulesEnabled(true);
        assertThat(explorer.areRulesEnabled()).isTrue();
    }
}
