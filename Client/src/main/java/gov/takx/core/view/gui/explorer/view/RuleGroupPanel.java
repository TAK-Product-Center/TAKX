package gov.takx.core.view.gui.explorer.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.explorer.IExplorableRule;
import gov.takx.api.explorer.IExplorableRuleFactory;
import gov.takx.api.explorer.IExplorableRuleViewComponent;
import gov.takx.api.explorer.IRuleGroupController;
import gov.takx.commons.swing.AListAndDetailPanel;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.core.explorer.controller.IExplorerController;
import jakarta.annotation.Nonnull;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A swing panel for editing the rules of a {@link IRuleGroupController} explorable.
 *
 * @since 1.48
 */
public class RuleGroupPanel extends AListAndDetailPanel<IExplorableRule, IExplorableRuleViewComponent>
{
    private IExplorerController controller;
    private IRuleGroupController group;
    private JComboBox<String> matchAllComboBox;

    /**
     * Construct a new panel without a group value. The panel will not function fully until an explorer controller is
     * set via {@link #setController(IExplorerController)}.
     *
     * @since 5.6
     */
    public RuleGroupPanel()
    {
        setEntryTypeNames("Rule", "Rules");
        addTableFooter(createMatchTypePanel());
    }

    /**
     * @param controller the explorer controller to use
     * @since 5.6
     */
    public void setController(@Nonnull IExplorerController controller)
    {
        this.controller = controller;
    }

    /**
     * @param group the group to edit
     * @since 5.6
     */
    public void setGroup(@Nonnull IRuleGroupController group)
    {
        setGroup(group, true);
    }

    /**
     * @param group the group to edit
     * @param init {@code true} to initialize field values or {@code false} to leave fields in their current state
     * @since 5.6
     */
    public void setGroup(@Nonnull IRuleGroupController group, boolean init)
    {
        this.group = group;
        if (init)
        {
            initialize(group.getRules());
            matchAllComboBox.setSelectedIndex(group.isMatchAll() ? 0 : 1);
        }
    }

    public void addRule(@Nonnull IExplorableRule rule)
    {
        addEntry(rule);
    }

    @Override
    public void addEntry(@Nonnull IExplorableRule entry)
    {
        super.addEntry(entry);
        // Need this because some map rule config fields can be extremely wide
        if (SwingUtilities.getAncestorOfClass(Dialog.class, this) instanceof Dialog dialog) dialog.pack();
    }

    @Override
    public void applyActions()
    {
        if (group != null)
        {
            commit();

            List<IExplorableRule> rules = new ArrayList<>();
            Map<IExplorableRule, Boolean> enabledMap = new HashMap<>();
            Map<IExplorableRule, String> displayNameMap = new HashMap<>();
            for (TableRow row : rows)
            {
                IExplorableRule rule = row.entryProxy.getEntry();
                rules.add(rule);
                enabledMap.put(rule, row.enabled);
                displayNameMap.put(rule, row.displayName);
            }

            controller.updateGroup(group, rules, enabledMap, displayNameMap, matchAllComboBox.getSelectedIndex() == 0);
        }
    }

    @Override
    protected EntryProxy createEntryProxy(@Nonnull IExplorableRule entry)
    {
        return new RuleEntryProxy(entry);
    }

    @Override
    public String validateContents()
    {
        return null;
    }

    @Override
    protected void addButtonActionPerformed()
    {
        List<Action> addActions = new ArrayList<>();
        for (final IExplorableRuleFactory<? extends IExplorableRule> factory : controller.getRuleFactories())
        {
            addActions.add(new AbstractAction(factory.getDisplayName())
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    addRule(factory.createRule());
                }
            });
        }
        showAddActions(addActions);

    }

    private void matchAllComboBoxActionPerformed()
    {
        setPanelDirty();
    }

    /**
     * Proxy to stand in between our superclass' table entries and {@link IExplorableRule} instances.
     */
    private class RuleEntryProxy extends EntryProxy
    {
        private final IExplorableRule rule;
        private final IExplorableRuleViewComponent viewComponent;
        private final String key = UUIDUtils.generateType1UUID().toString();

        protected RuleEntryProxy(IExplorableRule rule)
        {
            this.rule = rule;
            viewComponent = rule.createViewComponent();
        }

        @Nonnull
        @Override
        public IExplorableRule getEntry()
        {
            return rule;
        }

        @Nonnull
        @Override
        protected String getKey()
        {
            return key;
        }

        @Override
        protected boolean isEnabled()
        {
            return rule.isEnabled();
        }

        @Override
        protected void setEnabled(boolean enabled)
        {
            // Do nothing, this will be done by our controller.  See applyActions() above.
        }

        @Nonnull
        @Override
        protected String getDisplayName()
        {
            return rule.getDisplayName();
        }

        @Nonnull
        @Override
        protected IExplorableRuleViewComponent getValidationHandler()
        {
            return viewComponent;
        }

        @Nonnull
        @Override
        protected JPanel getDetailPanel()
        {
            return viewComponent.getPanel();
        }

        @Override
        protected void applyAction()
        {
            viewComponent.applyAction();
        }
    }

    /**
     * @return a panel showing our match all/any combobox.
     * @since 3.4
     */
    private JComponent createMatchTypePanel()
    {
        final CellConstraints cc = new CellConstraints();
        final JPanel panel = new JPanel();
        panel.setName("extraComponentPanel");
        panel.setLayout(new FormLayout(
                "default, $lcgap, default",
                "default"));

        //---- filterTypeLabel ----
        final JLabel filterTypeLabel = new JLabel();
        filterTypeLabel.setText("Filter Type: ");
        filterTypeLabel.setFont(filterTypeLabel.getFont().deriveFont(filterTypeLabel.getFont().getSize() + 1f));
        filterTypeLabel.setName("filterTypeLabel");
        panel.add(filterTypeLabel, cc.xy(1, 1));

        //---- matchAllComboBox ----
        matchAllComboBox = new JComboBox<>();
        matchAllComboBox.setModel(new DefaultComboBoxModel<>(new String[]{
                "Match All Rules",
                "Match Any Rule"
        }));
        matchAllComboBox.setFont(matchAllComboBox.getFont().deriveFont(matchAllComboBox.getFont().getSize() + 1f));
        matchAllComboBox.setName("matchAllComboBox");
        matchAllComboBox.addActionListener(e -> matchAllComboBoxActionPerformed());
        panel.add(matchAllComboBox, cc.xy(3, 1));

        return panel;
    }
}
