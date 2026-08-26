package gov.takx.commons.swing.entity;

import com.google.common.annotations.VisibleForTesting;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.swing.ButtonTextHideResizeListener;
import gov.takx.commons.swing.FormLayoutUtils;
import jakarta.annotation.Nonnull;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import java.awt.BorderLayout;

/**
 * A custom replacement for the normal table header. Relies on {@link EntityTableSortMenu} for sorting capabilities.
 * Allows adding buttons for table specific actions. Button panel uses a {@link ButtonTextHideResizeListener} to hide
 * button labels when the width is reduced.
 *
 * @param <E> the entity type
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class EntityTableHeader<E> extends JTableHeader
{
    @VisibleForTesting
    final EntityTableSortMenu<E> sortMenu = new EntityTableSortMenu<>();

    /**
     * Construct an empty header.
     */
    public EntityTableHeader()
    {
        initComponents();
    }

    /**
     * Sets the table to derive an entity definition, and wire up the sort menu.
     *
     * @param table the entity table
     */
    public void setTable(@Nonnull EntityTable<E> table)
    {
        sortMenu.setTable(table);
    }

    /**
     * Add a button for the specified {@link Action} to this panel. The button is styled to be consistent with other buttons
     * in this header.
     *
     * @param button the button
     */
    public void addActionButton(@Nonnull JButton button)
    {
        actionButtonPanel.addActionButton(button);
    }

    /**
     * @param visible {@code true} if the sort button should be visible, otherwise {@code false}
     */
    public void setSortButtonVisible(boolean visible)
    {
        if (visible != sortButton.isVisible())
        {
            sortButton.setVisible(visible);
            FormLayoutUtils.hideOrRestoreColumn(sortButton, !visible);
        }
    }

    /**
     * @return {@code true} if the sort button is visible, otherwise {@code false}
     */
    public boolean isSortButtonVisible()
    {
        return sortButton.isVisible();
    }

    private void createUIComponents()
    {
        sortButton = sortMenu.getButton();
    }

    /**
     * No-op in this implementation.
     *
     * @param columnModel the new data source for this table
     */
    @Override
    public void setColumnModel(TableColumnModel columnModel)
    {
    }

    /**
     * No-op in this implementation.
     */
    @Override
    protected void initializeLocalVars()
    {
    }

    /**
     * No-op in this implementation.
     */
    @Override
    public void updateUI()
    {
    }

    @SuppressWarnings({"DataFlowIssue", "RedundantExplicitVariableType"})
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        createUIComponents();

        var panel = new JPanel();
        actionButtonPanel = new ActionButtonPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new BorderLayout());

        //======== panel ========
        {
            panel.setBackground(UIManager.getColor("Table.background"));
            panel.setBorder(new CompoundBorder(
                    new MatteBorder(1, 0, 1, 0, UIManager.getColor("TableHeader.separatorColor")),
                    new EmptyBorder(5, 5, 5, 5)));
            panel.setName("panel");
            panel.setLayout(new FormLayout(
                    "left:default:grow, $lcgap, default",
                    "default"));

            //---- actionButtonPanel ----
            actionButtonPanel.setName("actionButtonPanel");
            panel.add(actionButtonPanel, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

            //---- sortButton ----
            sortButton.setBorder(new EmptyBorder(3, 6, 3, 9));
            sortButton.setName("sortButton");
            panel.add(sortButton, cc.xy(3, 1));
        }
        add(panel, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private ActionButtonPanel actionButtonPanel;
    private JButton sortButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
