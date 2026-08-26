package gov.takx.core.view.gui.explorer.controller;

import gov.takx.api.explorer.IRuleGroupController;
import gov.takx.core.explorer.controller.IExplorerController;
import gov.takx.core.explorer.model.IExplorerModel;
import gov.takx.test.AContainerTest;
import gov.takx.test.mock.explorer.TestExplorerPropertyChangeListener;
import gov.takx.test.utils.ExplorerTestConstants;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GroupsExplorerModelTest extends AContainerTest
{
    @Test
    public void groupsModelRuleGroupAddRemoveTest()
    {
        ExplorerTestConstants data = new ExplorerTestConstants();
        IExplorerController explorerController = data.controller;

        testAddRuleGroup(explorerController, data.RULE_GROUP_1);
        testAddRuleGroup(explorerController, data.RULE_GROUP_2);
        testAddRuleGroup(explorerController, data.RULE_GROUP_3);

        testRemoveRuleGroup(explorerController, data.RULE_GROUP_3);
        testRemoveRuleGroup(explorerController, data.RULE_GROUP_2);
        testRemoveRuleGroup(explorerController, data.RULE_GROUP_1);
    }

    private void testAddRuleGroup(IExplorerController explorerController, IRuleGroupController ruleGroupToAdd)
    {
        assertThat(explorerController.getRuleGroups()).doesNotContain(ruleGroupToAdd);
        TestExplorerPropertyChangeListener eventTester = new TestExplorerPropertyChangeListener(IExplorerModel.EXPLORABLE_ADDED,
                null,
                ruleGroupToAdd,
                explorerController.getModel());
        explorerController.addExplorable(ruleGroupToAdd);
        assertThat(eventTester.hasOccurred()).isTrue();
        assertThat(eventTester.isExpectedNewValue()).isTrue();
        assertThat(explorerController.getRuleGroups()).contains(ruleGroupToAdd);
    }

    private void testRemoveRuleGroup(IExplorerController explorerController, IRuleGroupController ruleGroupToRemove)
    {
        assertThat(explorerController.getRuleGroups()).contains(ruleGroupToRemove);
        TestExplorerPropertyChangeListener eventTester = new TestExplorerPropertyChangeListener(
                IExplorerModel.EXPLORABLE_REMOVED,
                ruleGroupToRemove,
                null,
                explorerController.getModel());
        explorerController.removeExplorable(ruleGroupToRemove);
        assertThat(eventTester.hasOccurred()).isTrue();
        assertThat(eventTester.isExpectedOldValue()).isTrue();
        assertThat(explorerController.getRuleGroups()).doesNotContain(ruleGroupToRemove);
    }
}
