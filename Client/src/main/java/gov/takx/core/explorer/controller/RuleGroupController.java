package gov.takx.core.explorer.controller;

import com.google.common.collect.Lists;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorableRule;
import gov.takx.api.explorer.IExplorableRuleFactory;
import gov.takx.api.explorer.IRuleGroupController;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.state.IRestorableMap;
import gov.takx.core.explorer.common.ControllerExplorableGroup;
import gov.takx.core.explorer.common.ExplorerStateKeys;
import gov.takx.core.explorer.model.IRuleGroupModel;
import gov.takx.core.explorer.model.RuleGroupModel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Rule group controller class.  Rule groups keep a list of explorable rules for logical matching against other
 * explorables.  Explorables are dynamically added or removed from rule groups based upon the group's rules and match
 * criteria.
 *
 * @since 1.48
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class RuleGroupController extends ControllerExplorableGroup implements IRuleGroupController, PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IRuleGroupModel model = new RuleGroupModel();

    @Inject
    private IExtendedGlobeManager globeManager;

    /**
     * Construct  a new instance.
     *
     * @param controller the explorer controller
     * @param displayName the display name
     * @param uniqueKey the unique key
     * @param parentPath the parent path
     * @param iconImage the icon image
     */
    public RuleGroupController(@Nonnull IExplorerController controller,
                               @Nonnull String displayName,
                               @Nonnull String uniqueKey,
                               @Nonnull List<String> parentPath,
                               @Nullable BufferedImage iconImage)
    {
        super(controller, displayName, uniqueKey, parentPath, iconImage);
        model.addPropertyChangeListener(this);
    }

    @Override
    @Nonnull
    public List<IExplorableRule> getRules()
    {
        return model.getRules();
    }

    @Override
    public boolean hasLocationSensitiveRules()
    {
        return model.hasLocationSensitiveRules();
    }

    @Override
    public boolean hasNameSensitiveRules()
    {
        return model.hasNameSensitiveRules();
    }

    @Override
    public boolean isMatchAll()
    {
        return model.isMatchAll();
    }

    @Override
    public void setMatchAll(boolean matchAll)
    {
        model.setMatchAll(matchAll);
    }

    @Override
    @Nonnull
    public List<IExplorable> getMatchingExplorables(@Nonnull Iterable<IExplorable> explorables)
    {
        List<IExplorable> matching = Lists.newArrayList();
        for (IExplorable explorable : explorables)
        {
            if (isMatch(explorable)) matching.add(explorable);
        }
        return matching;
    }

    @Override
    public boolean isMatch(@Nonnull IExplorable explorable)
    {
        final List<IExplorableRule> rules = model.getEnabledRules();
        if (rules.isEmpty())
        {
            return false;
        }

        if (isMatchAll())
        {
            for (IExplorableRule rule : rules)
            {
                if (!rule.isMatch(explorable)) return false;
            }
            return true;
        } else
        {
            for (IExplorableRule rule : rules)
            {
                if (rule.isMatch(explorable)) return true;
            }
            return false;
        }
    }

    @Override
    public boolean add(@Nonnull IExplorableRule rule)
    {
        return model.add(rule);
    }

    @Override
    public boolean remove(@Nonnull IExplorableRule rule)
    {
        return model.remove(rule);
    }

    @Override
    public void clearRules()
    {
        List<IExplorableRule> rules = new ArrayList<>(getRules());
        for (IExplorableRule rule : rules) remove(rule);
    }

    @Nonnull
    @Override
    public IExplorableRule remove(int index)
    {
        return model.remove(index);
    }

    @Override
    public int getRuleCount()
    {
        return model.getRuleCount();
    }

    @Override
    public boolean isRuleGroup()
    {
        return true;
    }

    @Override
    public void ruleFactoryAdded(@Nonnull IExplorableRuleFactory<? extends IExplorableRule> ruleFactory)
    {
        List<IRestorableMap> ruleStateList = model.getRestoredRuleState(ruleFactory.getType(), true);
        if (!ruleStateList.isEmpty())
        {
            for (IRestorableMap ruleState : ruleStateList)
            {
                IExplorableRule rule = ruleFactory.createRule();
                rule.setDisplayName(ruleState.getString(ExplorerStateKeys.DISPLAY_NAME_KEY, rule.getDisplayName()));
                rule.restore(ruleState);
                add(rule);
            }
            controller.ruleGroupUpdated(this);
        }
    }

    @Override
    public void store(@Nonnull IRestorableMap map)
    {
        map.setString(ExplorerStateKeys.DISPLAY_NAME_KEY, getDisplayName());
        map.setString(ExplorerStateKeys.UNIQUE_KEY_KEY, getUniqueKey());
        map.setStringList(ExplorerStateKeys.PATH_KEY, getParentPath());

        List<IRestorableMap> ruleStateList = new ArrayList<>();
        for (IExplorableRule rule : model.getRules())
        {
            IRestorableMap ruleState = map.createNewRestorableMap();
            rule.store(ruleState);
            ruleStateList.add(ruleState);
        }
        map.setRestorableMapList(ExplorerStateKeys.RULES_KEY, ruleStateList);
        map.setBoolean(ExplorerStateKeys.MATCH_ALL_KEY, isMatchAll());
    }

    @Override
    public void restore(@Nonnull IRestorableMap map)
    {
        setMatchAll(map.getBoolean(ExplorerStateKeys.MATCH_ALL_KEY, isMatchAll()));
        boolean restoreChanges = false;
        List<IRestorableMap> ruleStateList = map.getRestorableMapList(ExplorerStateKeys.RULES_KEY);
        for (IRestorableMap ruleState : ruleStateList)
        {
            String type = ruleState.getString(ExplorerStateKeys.RULE_TYPE_KEY, null);
            if (type != null)
            {
                // TODO: Rework the stored state to include the "rule class", and locate the factory
                // based on that.  Once the "untyped" factory registration is removed, we won't need to support
                // lookup by type/name.

                IExplorableRuleFactory<? extends IExplorableRule> factory = controller.getRuleFactory(type);
                if (factory != null)
                {
                    restoreChanges = true;
                    IExplorableRule rule = factory.createRule();
                    rule.restore(ruleState);
                    add(rule);
                } else
                {
                    // Since the correct factory could not be found, keep this rule's state in memory in the
                    // possibility that the factory will be added during the project session.
                    model.addRestoredRuleState(type, ruleState);
                }
            }
        }

        // Create a task (to be run once the globe is initialized) to update the rule groups.
        if (restoreChanges) globeManager.getPrimaryGlobeDelegate().doWhenReady(() -> controller.ruleGroupUpdated(this));
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        logger.trace(evt.toString());

        switch (evt.getPropertyName())
        {
            case IRuleGroupModel.RULE_ADDED -> initializeRule((IExplorableRule) evt.getNewValue());
            case IRuleGroupModel.RULE_REMOVED -> disposeRule((IExplorableRule) evt.getOldValue());
            default ->
                {
                }
        }
        firePropertyChange(evt);
    }

    @InvokeOnEDT
    private void initializeRule(IExplorableRule rule)
    {
        if (rule == null) return;
        rule.addPropertyChangeListener(this);
    }

    @InvokeOnEDT
    private void disposeRule(IExplorableRule rule)
    {
        if (rule == null) return;
        rule.removePropertyChangeListener(this);
    }

    @Override
    public String getPathName()
    {
        return uniqueKey;
    }
}
