package gov.takx.test.utils;

import gov.takx.api.explorer.Explorable;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorableRule;
import gov.takx.api.explorer.IExplorableRuleFactory;
import gov.takx.api.explorer.IGeofenceRule;
import gov.takx.api.explorer.INameRule;
import gov.takx.api.explorer.IProximityRule;
import gov.takx.api.explorer.IRuleGroupController;
import gov.takx.api.explorer.IUserExplorableGroup;
import gov.takx.core.explorer.Explorer;
import gov.takx.core.explorer.common.ManualExplorableGroup;
import gov.takx.core.explorer.controller.IExplorerController;
import gov.takx.core.explorer.controller.RuleGroupController;
import gov.takx.core.explorer.model.IExplorerModel;
import gov.takx.core.explorer.rules.GeofenceRule;
import gov.takx.core.explorer.rules.NameRule;
import gov.takx.core.explorer.rules.ProximityRule;
import gov.takx.platform.KeyUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 */
public class ExplorerTestConstants
{
    public static final String NAME = "name";
    public static final String NAME_1 = "name1";
    public static final String NAME_2 = "name2";
    public static final String NAME_3 = "name3";
    public static final String NAME_4 = "name4";
    public static final String NAME_5 = "name5";
    public static final String NAME_6 = "name6";
    public static final String NAME_7 = "name7";
    public static final String NAME_8 = "name8";

    public static final String GROUP_1 = "group1";
    public static final String GROUP_2 = "group2";
    public static final String GROUP_3 = "group3";
    public static final String GROUP_4 = "group4";
    public static final String GROUP_5 = "group5";

    public static final String OBJECT_KEY = "objectKey";
    public static final String STRING_KEY = "StringKey";
    public static final String LONG_KEY = "longKey";
    public static final String INT_KEY = "intKey";
    public static final String SHORT_KEY = "shortKey";
    public static final String BYTE_KEY = "byteKey";
    public static final String DOUBLE_KEY = "doubleKey";
    public static final String FLOAT_KEY = "floatKey";
    public static final String BOOLEAN_KEY = "booleanKey";

    public static final Object OBJECT_VALUE = new Object();
    public static final String STRING_VALUE = "test";
    public static final Long LONG_VALUE = 5L;
    public static final Integer INT_VALUE = 3;
    public static final Short SHORT_VALUE = (short) 2;
    public static final Byte BYTE_VALUE = (byte) 127;
    public static final Double DOUBLE_VALUE = 373465D;
    public static final Float FLOAT_VALUE = 345F;
    public static final Boolean BOOLEAN_VALUE = true;

    public static final String RULE_GROUP_NAME_1 = "ruleGroup1";
    public static final String RULE_GROUP_NAME_2 = "ruleGroup2";
    public static final String RULE_GROUP_NAME_3 = "ruleGroup3";

    public final Explorer explorer = new Explorer(false);
    public final IExplorerModel explorerModel = explorer.getModel();
    public final IExplorerController controller = explorer.getController();

    public final IExplorable EXPLORABLE_1 = new Explorable(NAME_1, List.of(GROUP_2, GROUP_3));
    public final IExplorable EXPLORABLE_2 = new Explorable(NAME_2, List.of(GROUP_2, GROUP_3));
    public final IExplorable EXPLORABLE_3 = new Explorable(NAME_3, List.of(GROUP_2, GROUP_3, GROUP_4));
    public final IExplorable EXPLORABLE_4 = new Explorable(NAME_4, List.of(GROUP_4, GROUP_5));

    public final IUserExplorableGroup EXPLORABLE_GROUP_1 = new ManualExplorableGroup(controller, NAME_5,
            KeyUtils.getUniqueKey("group", "standard", UUID.randomUUID().toString()), null);
    public final IExplorable CHILD_1 = new Explorable(NAME_7, List.of(GROUP_1, NAME_5));
    public final IExplorable CHILD_2 = new Explorable(NAME_8, List.of(GROUP_2, GROUP_3, NAME_6));

    public final IRuleGroupController RULE_GROUP_1 =
            new RuleGroupController(controller, RULE_GROUP_NAME_1, UUID.randomUUID().toString(), Arrays.asList(GROUP_1, GROUP_2), null);
    public final IRuleGroupController RULE_GROUP_2 =
            new RuleGroupController(controller, RULE_GROUP_NAME_2, UUID.randomUUID().toString(), Arrays.asList(GROUP_3, GROUP_4), null);
    public final IRuleGroupController RULE_GROUP_3 =
            new RuleGroupController(controller, RULE_GROUP_NAME_3, UUID.randomUUID().toString(), Collections.singletonList(GROUP_5), null);

    public final IExplorableRuleFactory<INameRule> NAME_RULE_FACTORY = NameRule.getFactory();
    public final IExplorableRuleFactory<IGeofenceRule> GEOFENCE_RULE_FACTORY = GeofenceRule.getFactory();
    public final IExplorableRuleFactory<IProximityRule> PROXIMITY_RULE_FACTORY = ProximityRule.getFactory();

    public final IExplorableRule RULE_1 = NameRule.getFactory().createRule();
    public final IExplorableRule RULE_2 = GeofenceRule.getFactory().createRule();
    public final IExplorableRule RULE_3 = ProximityRule.getFactory().createRule();

}
