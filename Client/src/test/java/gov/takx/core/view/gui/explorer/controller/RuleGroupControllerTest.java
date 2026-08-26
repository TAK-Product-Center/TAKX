package gov.takx.core.view.gui.explorer.controller;

import gov.takx.api.explorer.Explorable;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorableGroup;
import gov.takx.api.explorer.IExplorablePathProvider;
import gov.takx.api.explorer.IExplorableRule;
import gov.takx.api.explorer.IExplorableRuleFactory;
import gov.takx.api.explorer.IExplorerDelegateListener;
import gov.takx.api.explorer.IManualExplorableGroup;
import gov.takx.api.explorer.IRuleGroupController;
import gov.takx.api.explorer.IUserExplorableGroup;
import gov.takx.api.state.IRestorableMap;
import gov.takx.api.state.JSONMapUtils;
import gov.takx.core.explorer.common.IControllerExplorable;
import gov.takx.core.explorer.common.IPathExplorableGroup;
import gov.takx.core.explorer.controller.IExplorerAlarmsController;
import gov.takx.core.explorer.controller.IExplorerController;
import gov.takx.core.explorer.controller.RuleGroupController;
import gov.takx.core.explorer.model.IExplorablePersistor;
import gov.takx.core.explorer.model.IExplorerModel;
import gov.takx.core.explorer.rules.NameRule;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.ExplorerTestConstants;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class RuleGroupControllerTest extends AContainerTest
{
    private static final List<String> EMPTY_PATH = List.of();

    private static final NameRule nameRuleA = new NameRule("a", "a");
    private static final NameRule nameRuleB = new NameRule("b", "b");
    private static final NameRule nameRuleC = new NameRule("c", "c");
    private static final NameRule nameRuleD = new NameRule("d", "d");
    private static final NameRule nameRuleE = new NameRule("e", "e");
    private static final NameRule nameRuleF = new NameRule("f", "f");
    private static final NameRule nameRuleG = new NameRule("g", "g");

    private static final IExplorable exA = new Explorable("a", EMPTY_PATH);
    private static final IExplorable exB = new Explorable("b", EMPTY_PATH);
    private static final IExplorable exC = new Explorable("c", EMPTY_PATH);
    private static final IExplorable exD = new Explorable("d", EMPTY_PATH);
    private static final IExplorable exE = new Explorable("e", EMPTY_PATH);
    private static final IExplorable exF = new Explorable("f", EMPTY_PATH);
    private static final IExplorable exG = new Explorable("g", EMPTY_PATH);
    private static final IExplorable exABCDEFG = new Explorable("abcdefg", EMPTY_PATH);

    private static final IExplorable exNotMatchable = new Explorable("No", EMPTY_PATH);

    private static final List<IExplorable> explorables = List.of(exA, exB, exC, exD, exE, exF, exG, exABCDEFG);

    private final ExplorerTestConstants data = new ExplorerTestConstants();
    private final IExplorerController controller = data.controller;

    @Test
    public void matchAnyTest()
    {
        // Create a rule group with all name rules, set to Match-ANY
        IRuleGroupController ruleGroup = createDefaultRuleGroup();
        ruleGroup.setMatchAll(false);

        // Do a match against a list of explorables - the returned list should be identical (all explorables
        // match).
        List<IExplorable> matching = ruleGroup.getMatchingExplorables(explorables);
        assertThat(matching).isEqualTo(explorables);

        // Create a new list with all matching explorables PLUS an extra explorable that can't match.
        List<IExplorable> newList = new ArrayList<>(explorables);
        newList.add(exNotMatchable);

        // Run another match.  All the explorables should be returned, EXCEPT the non-matchable explorable.
        matching = ruleGroup.getMatchingExplorables(newList);
        for (IExplorable ex : explorables)
        {
            assertThat(matching).contains(ex);
        }
        assertThat(matching).doesNotContain(exNotMatchable);

        // Do individual matches (should have the same results as above).
        for (IExplorable ex : explorables)
        {
            assertThat(ruleGroup.isMatch(ex)).isTrue();
        }
        assertThat(ruleGroup.isMatch(exNotMatchable)).isFalse();
    }

    @Test
    public void matchAllTest()
    {
        // Create a rule group with all name rules, set to Match-ALL
        IRuleGroupController ruleGroup = createDefaultRuleGroup();
        ruleGroup.setMatchAll(true);

        // Do a match against the list of explorables.  The returned list should ONLY contain a single explorable.
        List<IExplorable> matching = ruleGroup.getMatchingExplorables(explorables);
        assertThat(matching).hasSize(1);

        // The original and returned lists should not be equal.
        assertThat(matching).isNotEqualTo(explorables);

        // Iterate through the explorables and determine whether they are correctly filtered into/out of the matching
        // list.
        for (IExplorable ex : explorables)
        {
            boolean contains = matching.contains(ex);
            if (ex != exABCDEFG)
            {
                assertThat(contains).isFalse();
            } else
            {
                assertThat(contains).isTrue();
            }
        }

        // Iterate through the explorables and individually match them against the rule.  The results should be the
        // same as above.
        for (IExplorable ex : explorables)
        {
            boolean match = ruleGroup.isMatch(ex);
            if (ex != exABCDEFG)
            {
                assertThat(match).isFalse();
            } else
            {
                assertThat(match).isTrue();
            }
        }
    }

    /**
     * The feature being tested here probably isn't obvious.  Rule group controllers are persistable (via
     * IRestorableMap), and are restored upon project restore.  The groups rely on having access to rule factories to
     * instantiate new rule instances according to their persisted state.  For default rules packaged with the core,
     * this isn't an issue - they will always be available.  However, plugins can also contribute rule factories
     * at any time.  Their factories are not necessarily accessible upon project startup.  Because of this, it may not
     * be possible for some rule groups to completely restore themselves on project restore.
     * <p/>
     * So, during project restore, if a rule group can't access a rule factory that it needs, it will store a reference
     * to the restore state for that specific rule.  If, at some future time, this rule group is notified that a new
     * factory has become available, the rule group will look at the factory's type, and determine whether it can
     * restore its outstanding rule state(s).  If it can, it will use the factory to instantiate the rule, and throw
     * away its reference to that rule's restore map.
     * <p/>
     * This method tests this functionality.
     */
    @Test
    public void delayedRestoreFactoryTest()
    {
        // Create a rule group with several NameRules attached.
        IRuleGroupController ruleGroupToStore = createDefaultRuleGroup();

        // Put this rule group's state (including name rules states) in a restorable map.
        IRestorableMap restorableMap = JSONMapUtils.createNewRestorableMap();
        ruleGroupToStore.store(restorableMap);

        // Create a new, empty rule group that does not yet have access to the NameRule factory.
        IRuleGroupController ruleGroup = new RuleGroupController(createStubDelegate(), "t", UUID.randomUUID().toString(), Collections.singletonList("1"), null);

        // Restore the empty rule group from the other rule group's state.
        ruleGroup.restore(restorableMap);

        // Even after restore, the new rule group should not have any rules (it couldn't instantiate NameRule).
        assertThat(ruleGroup.getRuleCount()).isEqualTo(0);

        // Notify the new rule group of the availability of NameRule's factory.  This should trigger a restore of the
        // outstanding rule states.
        ruleGroup.ruleFactoryAdded(NameRule.getFactory());

        // Now, the new rule group should have some rules.
        assertThat(ruleGroup.getRuleCount()).isNotEqualTo(0);

        // The new rule group should also now have the same number of rules as the old rule group.
        assertThat(ruleGroup.getRuleCount()).isEqualTo(ruleGroupToStore.getRuleCount());

        // For good measure, also ensure that the new rule group's rules are all of NameRule type.
        for (IExplorableRule rule : ruleGroup.getRules())
        {
            assertThat(rule.getType()).isEqualTo(NameRule.TYPE);
        }
    }

    private IRuleGroupController createDefaultRuleGroup()
    {
        IRuleGroupController ruleGroup = new RuleGroupController(controller, "test", UUID.randomUUID().toString(), Collections.singletonList("1"), null);
        ruleGroup.add(nameRuleA);
        ruleGroup.add(nameRuleB);
        ruleGroup.add(nameRuleC);
        ruleGroup.add(nameRuleD);
        ruleGroup.add(nameRuleE);
        ruleGroup.add(nameRuleF);
        ruleGroup.add(nameRuleG);
        return ruleGroup;
    }

    private IExplorerController createStubDelegate()
    {
        return new IExplorerController()
        {
            @Override
            public void addPathProvider(@Nonnull IExplorablePathProvider pathProvider)
            {
            }

            @Override
            public IManualExplorableGroup getOrCreateManualGroup(@Nonnull String displayName, @Nullable BufferedImage iconImage, boolean create)
            {
                return null;
            }

            @Nonnull
            @Override
            public IManualExplorableGroup createManualGroup(@Nonnull String displayName, @Nullable String uniqueKey, @Nullable BufferedImage iconImage)
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addExplorable(@Nonnull IExplorable explorable)
            {
                return false;
            }

            @Override
            public boolean removeExplorable(@Nonnull IExplorable explorable)
            {
                return false;
            }

            @Nonnull
            @Override
            public IPathExplorableGroup getRoot()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public void removeExplorable(@Nonnull IControllerExplorable explorable, boolean confirm)
            {
            }

            @Override
            public boolean removeExplorable(@Nonnull IExplorable explorable, boolean removeEmptyParents)
            {
                return false;
            }

            @Nonnull
            @Override
            public Collection<IExplorable> getExplorables()
            {
                return List.of();
            }

            @Override
            public IExplorable getExplorable(@Nonnull String uniqueKey)
            {
                return null;
            }

            @Nullable
            @Override
            public IExplorable getExplorableByPath(@Nonnull String path)
            {
                return null;
            }

            @Override
            public boolean hasExplorable(@Nonnull String uniqueKey)
            {
                return false;
            }

            @Override
            public IRuleGroupController addRuleGroup(@Nonnull String groupName, @Nullable String uniqueKey, @Nullable List<String> path, @Nullable BufferedImage iconImage)
            {
                return null;
            }

            @Nonnull
            @Override
            public IExplorerModel getModel()
            {
                throw new UnsupportedOperationException();
            }

            @SuppressWarnings("removal")
            @Nonnull
            @Override
            public IExplorableGroup getAppsRoot()
            {
                throw new UnsupportedOperationException();
            }

            @Nonnull
            @Override
            public IExplorerAlarmsController getAlarmsController()
            {
                throw new UnsupportedOperationException();
            }

            @Nonnull
            @Override
            public Predicate<IExplorable> getGroupablePredicate()
            {
                throw new UnsupportedOperationException();
            }

            @SuppressWarnings("removal")
            @Nonnull
            @Override
            public IExplorableGroup getGroupsRoot()
            {
                throw new UnsupportedOperationException();
            }

            @Nonnull
            @Override
            public Set<String> getGroupNames()
            {
                return Set.of();
            }

            @Override
            public <T extends IExplorableRule> void addRuleFactory(@Nonnull Class<T> ruleClass, @Nonnull IExplorableRuleFactory<T> factory)
            {
            }

            @Override
            public void removeRuleFactory(@Nonnull IExplorableRuleFactory<? extends IExplorableRule> factory)
            {
            }

            @Override
            public void setRulesEnabled(boolean enabled)
            {
            }

            @Override
            public boolean areRulesEnabled()
            {
                return false;
            }

            @Override
            public void updateGroup(@Nonnull IRuleGroupController delegate, @Nonnull List<IExplorableRule> rules, @Nonnull Map<IExplorableRule, Boolean> enabledMap, @Nonnull Map<IExplorableRule, String> displayNameMap, boolean matchAll)
            {
            }

            @Override
            public void requestGroupsRefresh()
            {
            }

            @Nonnull
            @Override
            public List<IRuleGroupController> getRuleGroups()
            {
                return List.of();
            }

            @Override
            public <T extends IExplorableRule> IExplorableRuleFactory<T> getRuleFactory(@Nonnull Class<T> ruleClass)
            {
                return null;
            }

            @Override
            public IExplorableRuleFactory<? extends IExplorableRule> getRuleFactory(@Nonnull String type)
            {
                return null;
            }

            @Nonnull
            @Override
            public List<IExplorableRuleFactory<? extends IExplorableRule>> getRuleFactories()
            {
                return List.of();
            }

            @Override
            public void ruleGroupUpdated(@Nonnull IRuleGroupController ruleGroupController)
            {
            }

            @Nonnull
            @Override
            public List<IExplorable> getGroupableExplorables()
            {
                return List.of();
            }

            @Override
            public void requestSetSelected(@Nonnull List<IExplorable> explorables, @Nonnull List<IExplorerDelegateListener.SelectionState> states)
            {
            }

            @Nonnull
            @Override
            public IUserExplorableGroup addGroup(@Nonnull String name, boolean isRulesGroup, BufferedImage icon)
            {
                throw new UnsupportedOperationException();
            }

            @SuppressWarnings("removal")
            @Override
            public void requestSetChildren(@Nonnull IExplorableGroup group, @Nonnull Collection<IExplorable> children)
            {
            }

            @Override
            public void requestDelete(@Nonnull IExplorable explorable)
            {
            }

            @Override
            public void addExplorablePersistor(@Nonnull IExplorablePersistor persistor)
            {
            }
        };
    }
}
