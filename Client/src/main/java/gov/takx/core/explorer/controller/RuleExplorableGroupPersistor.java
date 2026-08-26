package gov.takx.core.explorer.controller;

import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IRuleGroupController;
import gov.takx.api.state.IRestorableMap;
import gov.takx.api.state.JSONMapUtils;
import gov.takx.core.explorer.common.ExplorerStateKeys;
import gov.takx.core.explorer.model.IExplorablePersistor;
import jakarta.annotation.Nonnull;

import java.util.Comparator;
import java.util.List;

/**
 * Implementation of {@link IExplorablePersistor} that can store and restore {@link IRuleGroupController} instances.
 *
 * @since 5.3
 */
public class RuleExplorableGroupPersistor implements IExplorablePersistor
{

    private final IExplorerController controller;

    /**
     * Construct a new instance.
     *
     * @param controller the explorer controller;
     */
    public RuleExplorableGroupPersistor(@Nonnull IExplorerController controller)
    {
        this.controller = controller;
    }

    @Override
    public IRestorableMap storeExplorable(@Nonnull IExplorable explorable, @Nonnull IRestorableMap map)
    {
        return null;
    }

    @Override
    public IExplorable restoreExplorable(@Nonnull IRestorableMap map)
    {
        return null;
    }

    @Override
    public void store(@Nonnull IRestorableMap map)
    {
        final List<IRuleGroupController> ruleGroups = controller.getExplorables()
                .stream()
                .filter(IRuleGroupController.class::isInstance)
                .map(IRuleGroupController.class::cast)
                .filter(IRuleGroupController::isPersist)
                .sorted(Comparator.comparing(IRuleGroupController::getDepth))
                .toList();
        map.setRestorableMapList(ExplorerStateKeys.RULE_GROUPS_KEY, JSONMapUtils.createNewRestoreMapList(ruleGroups));
    }

    @Override
    public void restore(@Nonnull IRestorableMap map)
    {
        // Restore rule groups.
        for (IRestorableMap ruleGroupState : map.getRestorableMapList(ExplorerStateKeys.RULE_GROUPS_KEY))
        {
            // We need to know the name of the rule group before instantiating it.
            String displayName = ruleGroupState.getString(ExplorerStateKeys.DISPLAY_NAME_KEY, null);
            if (displayName != null)
            {
                // Instantiate the rule group, add it to the tree and restore its data.
                List<String> path = ruleGroupState.getStringList(ExplorerStateKeys.PATH_KEY, null);
                IRuleGroupController ruleGroup = controller.addRuleGroup(displayName, ruleGroupState.getString(ExplorerStateKeys.UNIQUE_KEY_KEY, null), path, null);
                if (ruleGroup != null) ruleGroup.restore(ruleGroupState);
            }
        }
    }
}