package gov.takx.core.view.gui.explorer.model;

import gov.takx.api.explorer.IExplorableRule;
import gov.takx.api.state.IRestorableMap;
import gov.takx.api.state.JSONMapUtils;
import gov.takx.core.explorer.model.IRuleGroupModel;
import gov.takx.core.explorer.model.RuleGroupModel;
import gov.takx.test.mock.explorer.TestExplorerPropertyChangeListener;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class RuleGroupModelTest
{
    private static final IRestorableMap restore1 = JSONMapUtils.createNewRestorableMap();
    private static final IRestorableMap restore2 = JSONMapUtils.createNewRestorableMap();
    private static final IRestorableMap restore3 = JSONMapUtils.createNewRestorableMap();

    static
    {
        restore1.setString("string", "test");
        restore2.setDouble("double", 2.5D);
        restore3.setLong("long", 1L);
    }

    @Test
    public void ruleGroupRuleAddRemoveTest()
    {
        /*    IRuleGroupModel groupModel = createRuleGroupModel();
        
        testRuleAdd(groupModel, RULE_1);
        testRuleAdd(groupModel, RULE_2);
        testRuleAdd(groupModel, RULE_3);
        
        testRuleRemove(groupModel, RULE_3);
        testRuleRemove(groupModel, RULE_2);
        testRuleRemove(groupModel, RULE_1);
        
        assertThat(groupModel.getRuleCount()).isEqualTo(0);
        testRuleAdd(groupModel, RULE_1);
        assertThat(groupModel.getRuleCount()).isEqualTo(1);
        testRuleAdd(groupModel, RULE_2);
        assertThat(groupModel.getRuleCount()).isEqualTo(2);
        testRuleAdd(groupModel, RULE_3);
        assertThat(groupModel.getRuleCount()).isEqualTo(3);
        
        TestExplorerPropertyChangeListener eventTester = new TestExplorerPropertyChangeListener(
                IRuleGroupModel.RULE_REMOVED,
                RULE_2,
                null,
                groupModel);
        groupModel.remove(1);
        assertThat(eventTester.hasOccurred()).isTrue();
        assertThat(eventTester.isExpectedOldValue()).isTrue();
        assertThat(groupModel.getRuleCount()).isEqualTo(2);
        assertThat(groupModel.getRules()).doesNotContain(RULE_2);*/
    }

    @Test
    public void ruleGroupMatchAllSetterTest()
    {
        IRuleGroupModel groupModel = createRuleGroupModel();

        boolean matchAll = groupModel.isMatchAll();
        TestExplorerPropertyChangeListener eventTester = new TestExplorerPropertyChangeListener(
                IRuleGroupModel.MATCH_ALL,
                null,
                !matchAll,
                groupModel);
        groupModel.setMatchAll(!matchAll);
        assertThat(eventTester.hasOccurred()).isTrue();
        assertThat(eventTester.isExpectedNewValue()).isTrue();
        assertThat(groupModel.isMatchAll()).isEqualTo(!matchAll);
    }

    @Test
    public void ruleGroupAddRemoveRestoredRuleStateTest()
    {
        IRuleGroupModel groupModel = createRuleGroupModel();

        testRestoredRuleStateAdd(groupModel, "1", restore1);
        testRestoredRuleStateAdd(groupModel, "2", restore2);
        testRestoredRuleStateAdd(groupModel, "3", restore3);

        testRestoredRuleStateRemove(groupModel, "1", restore1);
        testRestoredRuleStateRemove(groupModel, "2", restore2);
        testRestoredRuleStateRemove(groupModel, "3", restore3);
    }

    private void testRuleAdd(IRuleGroupModel model, IExplorableRule rule)
    {
        assertThat(model.getRules()).doesNotContain(rule);
        TestExplorerPropertyChangeListener eventTester = new TestExplorerPropertyChangeListener(IRuleGroupModel.RULE_ADDED, null, rule, model);
        int size = model.getRuleCount();
        model.add(rule);
        assertThat(eventTester.hasOccurred()).isTrue();
        assertThat(eventTester.isExpectedNewValue()).isTrue();
        assertThat(model.getRules()).contains(rule);
        assertThat(model.getRuleCount()).isEqualTo(size + 1);
    }

    private void testRuleRemove(IRuleGroupModel model, IExplorableRule rule)
    {
        assertThat(model.getRules()).contains(rule);
        TestExplorerPropertyChangeListener eventTester = new TestExplorerPropertyChangeListener(IRuleGroupModel.RULE_REMOVED, rule, null, model);
        int size = model.getRuleCount();
        model.remove(rule);
        assertThat(eventTester.hasOccurred()).isTrue();
        assertThat(eventTester.isExpectedOldValue()).isTrue();
        assertThat(model.getRules()).doesNotContain(rule);
        assertThat(model.getRuleCount()).isEqualTo(size - 1);
    }

    private void testRestoredRuleStateAdd(IRuleGroupModel model, String type, IRestorableMap ruleState)
    {
        assertThat(model.getRestoredRuleState(type, false)).doesNotContain(ruleState);
        TestExplorerPropertyChangeListener eventTester = new TestExplorerPropertyChangeListener(
                IRuleGroupModel.RESTORED_RULE_STATE_ADDED,
                null,
                ruleState,
                model);
        model.addRestoredRuleState(type, ruleState);
        assertThat(eventTester.hasOccurred()).isTrue();
        assertThat(eventTester.isExpectedNewValue()).isTrue();
        assertThat(model.getRestoredRuleState(type, false)).contains(ruleState);
    }

    private void testRestoredRuleStateRemove(IRuleGroupModel model, String type, IRestorableMap ruleState)
    {
        assertThat(model.getRestoredRuleState(type, false)).contains(ruleState);
        TestExplorerPropertyChangeListener eventTester = new TestExplorerPropertyChangeListener(
                IRuleGroupModel.RESTORED_RULE_STATE_REMOVED,
                type,
                null,
                model);
        model.getRestoredRuleState(type, true);
        assertThat(eventTester.hasOccurred()).isTrue();
        assertThat(eventTester.isExpectedOldValue()).isTrue();
        assertThat(model.getRestoredRuleState(type, false)).doesNotContain(ruleState);
    }

    private IRuleGroupModel createRuleGroupModel()
    {
        IRuleGroupModel ruleGroupModel = new RuleGroupModel();
        return ruleGroupModel;
    }
}
