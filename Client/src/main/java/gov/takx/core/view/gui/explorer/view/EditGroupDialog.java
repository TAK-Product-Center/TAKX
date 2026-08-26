package gov.takx.core.view.gui.explorer.view;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.explorer.ExplorerUtils;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IManualExplorableGroup;
import gov.takx.api.explorer.IRuleGroupController;
import gov.takx.api.explorer.IUserExplorableGroup;
import gov.takx.commons.swing.DualList;
import gov.takx.commons.swing.FormButtonBar;
import gov.takx.commons.swing.FormLayoutUtils;
import gov.takx.core.explorer.common.CoreExplorerUtils;
import gov.takx.core.explorer.controller.IExplorerController;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

/**
 * A dialog for editing or creating {@link IUserExplorableGroup} instances. Group type can only be selected when creating
 * a new group and cannot be changed once the group is created.
 *
 * @since 5.6
 */
@PseudoBean
public class EditGroupDialog extends AStandardDialog
{
    private final IExplorerController controller;

    private IUserExplorableGroup group;

    /**
     * Create a new dialog using the given controller to source possible group members and rule factories.
     *
     * @param controller the controller
     */
    public EditGroupDialog(@Nonnull IExplorerController controller)
    {
        this(controller, null);
    }

    /**
     * Create a new dialog using the given controller to source possible group members and rule factories, and get all
     * fields to values from the given group.
     *
     * @param controller the controller
     * @param group the group
     */
    public EditGroupDialog(@Nonnull IExplorerController controller, @Nullable IUserExplorableGroup group)
    {
        this.controller = controller;
        initComponents();
        rulePanel.setController(controller);
        groupMembersField.setCellRenderer(new ExplorableRenderer());
        ((DefaultListModel<IExplorable>) groupMembersField.getModel()).addAll(controller.getGroupableExplorables());
        groupMembersField.setComparator((o1, o2) -> o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName()));
        buttonBar.attachField(nameField, event -> validateGroupName());
        buttonBar.attachFieldListSelectionListener(groupMembersField, groupMembersField::addListSelectionListener, null);
        buttonBar.attachField(typeStandardField, e -> validateType());
        buttonBar.attachField(typeRuleField, e -> validateType());
        buttonBar.attachPanelStateListener(rulePanel, rulePanel::setPanelStateListener, null);

        FormLayoutUtils.hideRow(groupMembersField);
        FormLayoutUtils.hideRow(rulePanel);

        if (group != null) setGroup(group);
        buttonBar.setPanelClean();
    }

    private String validateType()
    {
        return !typeStandardField.isSelected() && !typeRuleField.isSelected() ? "Must select a type" : null;
    }

    private void typeChanged()
    {
        FormLayoutUtils.hideOrRestoreRow(groupMembersField, !typeStandardField.isSelected());
        FormLayoutUtils.hideOrRestoreRow(rulePanel, typeStandardField.isSelected());
        pack();
    }

    private void buttonBarApply()
    {
        if (group == null)
        {
            typeStandardField.setEnabled(false);
            typeRuleField.setEnabled(false);
            group = controller.addGroup(nameField.getText().trim(), !typeStandardField.isSelected(), null);
            if (!typeStandardField.isSelected())
            {
                rulePanel.setGroup((IRuleGroupController) group, false);
            }
        }
        group.setDisplayName(nameField.getText().trim());
        if (group instanceof IManualExplorableGroup)
        {
            group.setChildren(groupMembersField.getSelectedItems());
        } else
        {
            rulePanel.applyActions();
        }
    }

    private void buttonBarDelete(ActionEvent e)
    {
        group.requestDelete();
        if (controller.getExplorable(group.getUniqueKey()) == null)
        {
            ((FormButtonBar.DeleteActionEvent) e).successful();
        }
    }

    /**
     * Validate a given explorable group name.
     *
     * @return the validation error text or {@code null} if the new name is valid
     */
    private String validateGroupName()
    {
        String errorMessage = null;
        String currentName = group != null ? group.getDisplayName() : null;
        Set<String> namesInUse = controller.getGroupNames();
        String newName = nameField.getText().trim();
        if (newName.isEmpty())
        {
            errorMessage = "Group name cannot be empty";
        } else if (namesInUse.contains(newName) && (!newName.equals(currentName)))
        {
            errorMessage = "Group name <b>" + newName + "</b> is already in use";
        }
        return errorMessage;
    }

    /**
     * @return the edited or created group
     */
    @Nullable
    public IUserExplorableGroup getGroup()
    {
        return group;
    }

    /**
     * @param userExplorableGroup the group to edit
     */
    public void setGroup(@Nonnull IUserExplorableGroup userExplorableGroup)
    {
        group = userExplorableGroup;
        buttonBar.setDeleteButtonVisible(true);
        setTitle(getTitle() + " - " + group.getDisplayName());
        nameField.setText(group.getDisplayName());

        if (userExplorableGroup instanceof IManualExplorableGroup manualExplorableGroup)
        {
            typeStandardField.setSelected(true);
            var groupExplorables = new ArrayList<>(manualExplorableGroup.getChildren());
            groupExplorables.sort(Comparator.comparing(IExplorable::getDisplayName, String::compareToIgnoreCase));
            groupMembersField.setSelectedItems(groupExplorables);
            FormLayoutUtils.restoreRow(groupMembersField);

        } else if (userExplorableGroup instanceof IRuleGroupController ruleGroup)
        {
            typeRuleField.setSelected(true);
            FormLayoutUtils.restoreRow(rulePanel);
            rulePanel.setGroup(ruleGroup);
        }
        FormLayoutUtils.hideRow(typeLabel);
        buttonBar.setPanelClean();
    }

    @Override
    public void dispose()
    {
        rulePanel.clear();
        super.dispose();
    }

    static class ExplorableRenderer extends DefaultTableCellRenderer
    {
        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean selected,
                                                       boolean focused,
                                                       int row,
                                                       int column)
        {
            row = table.convertRowIndexToModel(row);
            var label = (JLabel) super.getTableCellRendererComponent(table, table, selected, focused, row, column);
            var model = table.getModel();
            var explorable = (IExplorable) model.getValueAt(row, 0);
            label.setIcon(CoreExplorerUtils.iconForExplorable(explorable));
            label.setText(explorable.getDisplayName());
            label.setToolTipText(ExplorerUtils.createDefaultKey(
                    explorable.getDisplayName(),
                    explorable.getParentPath()));
            return label;
        }
    }

    @SuppressWarnings({"RedundantExplicitVariableType", "Convert2MethodRef"})
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var dialogPanel = new JPanel();
        var formPanel = new JPanel();
        var nameLabel = new JLabel();
        nameField = new JTextField();
        typeLabel = new JLabel();
        var typePanel = new JPanel();
        typeStandardField = new JRadioButton();
        typeRuleField = new JRadioButton();
        groupMembersField = new DualList<>();
        rulePanel = new RuleGroupPanel();
        buttonBar = new FormButtonBar();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Group");
        setMinimumSize(new Dimension(500, 50));
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPanel ========
        {
            dialogPanel.setBorder(Borders.DIALOG_BORDER);
            dialogPanel.setName("dialogPanel");
            dialogPanel.setLayout(new BorderLayout());

            //======== formPanel ========
            {
                formPanel.setName("formPanel");
                formPanel.setLayout(new FormLayout(
                        "default, $lcgap, default:grow",
                        "3*(default, $lgap), default"));

                //---- nameLabel ----
                nameLabel.setText("Name:");
                nameLabel.setName("nameLabel");
                formPanel.add(nameLabel, cc.xy(1, 1));

                //---- nameField ----
                nameField.setName("nameField");
                formPanel.add(nameField, cc.xy(3, 1));

                //---- typeLabel ----
                typeLabel.setText("Type:");
                typeLabel.setName("typeLabel");
                formPanel.add(typeLabel, cc.xy(1, 3));

                //======== typePanel ========
                {
                    typePanel.setName("typePanel");
                    typePanel.setLayout(new FormLayout(
                            "default, $lcgap, default",
                            "default"));

                    //---- typeStandardField ----
                    typeStandardField.setText("Standard");
                    typeStandardField.setName("typeStandardField");
                    typeStandardField.addActionListener(e -> typeChanged());
                    typePanel.add(typeStandardField, cc.xy(1, 1));

                    //---- typeRuleField ----
                    typeRuleField.setText("Rules-Based");
                    typeRuleField.setName("typeRuleField");
                    typeRuleField.addActionListener(e -> typeChanged());
                    typePanel.add(typeRuleField, cc.xy(3, 1));
                }
                formPanel.add(typePanel, cc.xy(3, 3));

                //---- groupMembersField ----
                groupMembersField.setRowHeight(30);
                groupMembersField.setSelectedText("Selected Map Objects:");
                groupMembersField.setUnselectedText("Available Map Objects:");
                groupMembersField.setName("groupMembersField");
                formPanel.add(groupMembersField, cc.xywh(1, 5, 3, 1));

                //---- rulePanel ----
                rulePanel.setName("rulePanel");
                formPanel.add(rulePanel, cc.xywh(1, 7, 3, 1));
            }
            dialogPanel.add(formPanel, BorderLayout.CENTER);

            //---- buttonBar ----
            buttonBar.setDeleteButtonVisible(false);
            buttonBar.setName("buttonBar");
            buttonBar.addApplyListener(e -> buttonBarApply());
            buttonBar.addDeleteListener(e -> buttonBarDelete(e));
            dialogPanel.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());

        //---- typeButtonGroup ----
        var typeButtonGroup = new ButtonGroup();
        typeButtonGroup.add(typeStandardField);
        typeButtonGroup.add(typeRuleField);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JTextField nameField;
    private JLabel typeLabel;
    private JRadioButton typeStandardField;
    private JRadioButton typeRuleField;
    private DualList<IExplorable> groupMembersField;
    private RuleGroupPanel rulePanel;
    private FormButtonBar buttonBar;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
