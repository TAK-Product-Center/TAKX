package gov.takx.core.explorer.model;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import gov.takx.api.explorer.AExplorerEventSupport;
import gov.takx.api.explorer.IExplorableRule;
import gov.takx.api.state.IRestorableMap;
import gov.takx.core.explorer.rules.GeofenceRule;
import gov.takx.core.explorer.rules.NameRule;
import gov.takx.core.explorer.rules.ProximityRule;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of {@link IRuleGroupModel}.
 *
 * @since 1.48
 */
public class RuleGroupModel extends AExplorerEventSupport implements IRuleGroupModel, PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<IExplorableRule> rules = new CopyOnWriteArrayList<>();
    private final List<IExplorableRule> enabledRules = new CopyOnWriteArrayList<>();
    private final AtomicBoolean matchAll = new AtomicBoolean(true);
    private final ListMultimap<String, IRestorableMap> restoredRuleStateMap = ArrayListMultimap.create();
    private boolean hasLocationSensitiveRules;
    private boolean hasNameSensitiveRules;

    @Nonnull
    @Override
    public List<IExplorableRule> getRules()
    {
        return Collections.unmodifiableList(rules);
    }

    @Nonnull
    @Override
    public List<IExplorableRule> getEnabledRules()
    {
        return Collections.unmodifiableList(enabledRules);
    }

    @Override
    public boolean hasLocationSensitiveRules()
    {
        return hasLocationSensitiveRules;
    }

    @Override
    public boolean hasNameSensitiveRules()
    {
        return hasNameSensitiveRules;
    }

    @Override
    public boolean isMatchAll()
    {
        return matchAll.get();
    }

    @Override
    public void setMatchAll(boolean matchAll)
    {
        boolean old = this.matchAll.getAndSet(matchAll);
        if (matchAll != old) firePropertyChange(MATCH_ALL, old, matchAll);
    }

    @Override
    public boolean add(@Nonnull IExplorableRule rule)
    {
        boolean success = false;
        if (!rules.contains(rule))
        {
            success = rules.add(rule);
            if (rule.isEnabled()) enabledRules.add(rule);

            rule.addPropertyChangeListener(IExplorableRule.ENABLED, this);
            firePropertyChange(RULE_ADDED, null, rule);

            final String type = rule.getType();

            if (type.equals(NameRule.TYPE)) hasNameSensitiveRules = true;
            if (type.equals(GeofenceRule.TYPE) || type.equals(ProximityRule.TYPE)) hasLocationSensitiveRules = true;
        }
        return success;
    }

    /**
     * Determines if any of the current rules in the group contains a location-sensitive rule.
     *
     * @since 2.31
     */
    private void updateLocationSensitivity()
    {
        hasLocationSensitiveRules = rules.stream()
                .map(IExplorableRule::getType)
                .anyMatch(type -> type.equals(GeofenceRule.TYPE) || type.equals(ProximityRule.TYPE));
    }

    /**
     * Determines if any of the current rules in the group contains a name-sensitive rule.
     *
     * @since 2.36
     */
    private void updateNameSensitivity()
    {
        hasNameSensitiveRules = rules.stream()
                .anyMatch(rule -> NameRule.TYPE.equals(rule.getType()));
    }

    @Override
    public boolean remove(@Nonnull IExplorableRule rule)
    {
        if (rule == null)
        {
            logger.warn("Unable to remove null explorable rule");
            return false;
        }

        return remove(rules.indexOf(rule)) != null;
    }

    @Nonnull
    @Override
    public IExplorableRule remove(int index)
    {
        IExplorableRule removed = rules.remove(index);
        if (removed.isEnabled()) enabledRules.remove(removed);
        removed.removePropertyChangeListener(IExplorableRule.ENABLED, this);
        firePropertyChange(RULE_REMOVED, removed, null);
        updateLocationSensitivity();
        updateNameSensitivity();
        return removed;
    }

    @Override
    public int getRuleCount()
    {
        return rules.size();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getPropertyName().equals(IExplorableRule.ENABLED))
        {
            final IExplorableRule rule = (IExplorableRule) evt.getSource();
            final boolean enabled = (Boolean) evt.getNewValue();

            if (enabled)
            {
                enabledRules.add(rule);
            } else
            {
                enabledRules.remove(rule);
            }
        } else
        {
            logger.warn("Unexpected PCE: {}", evt);
        }
        firePropertyChange(evt);
    }

    @Override
    public void addRestoredRuleState(@Nonnull String type, @Nonnull IRestorableMap ruleState)
    {
        restoredRuleStateMap.put(type, ruleState);
        pcs.firePropertyChange(RESTORED_RULE_STATE_ADDED, null, ruleState);
    }

    @Nonnull
    @Override
    public List<IRestorableMap> getRestoredRuleState(@Nonnull String type, boolean remove)
    {
        if (remove)
        {
            boolean fireEvent = restoredRuleStateMap.containsKey(type);
            List<IRestorableMap> ruleStateList = restoredRuleStateMap.removeAll(type);
            if (fireEvent) pcs.firePropertyChange(RESTORED_RULE_STATE_REMOVED, type, null);
            return ruleStateList;
        }
        return restoredRuleStateMap.get(type);
    }
}
