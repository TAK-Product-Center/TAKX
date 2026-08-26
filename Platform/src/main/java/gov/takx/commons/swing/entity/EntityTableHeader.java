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
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

/**
 * A custom replacement for the normal table header. Relies on {@link EntityTableSortMenu} for sorting capabilities.
 * Allows adding buttons for table specific actions. Button panel uses a {@link ButtonTextHideResizeListener} to hide
 * button labels when the width is reduced. Allows showing / hiding both a compact header and the standard header.
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
        setStandardHeaderVisible(false);
    }

    /**
     * @param resizingAllowed {@code true} if columns should allow resizing
     * @since 5.8
     */
    @Override
    public void setResizingAllowed(boolean resizingAllowed)
    {
        standardHeader.setResizingAllowed(resizingAllowed);
    }

    /**
     * @return {@code true} if columns allow resizing
     * @since 5.8
     */
    @Override
    public boolean getResizingAllowed()
    {
        return standardHeader.getResizingAllowed();
    }

    /**
     * @param reorderingAllowed {@code true} if columns should allow reordering
     * @since 5.8
     */
    @Override
    public void setReorderingAllowed(boolean reorderingAllowed)
    {
        standardHeader.setReorderingAllowed(reorderingAllowed);
    }

    /**
     * @return {@code true} if columns allow reordering
     * @since 5.8
     */
    @Override
    public boolean getReorderingAllowed()
    {
        return standardHeader.getReorderingAllowed();
    }

    /**
     * @param visible {@code true} to make the standard header visible, otherwise {@code false}
     * @since 5.8
     */
    public void setStandardHeaderVisible(boolean visible)
    {
        FormLayoutUtils.hideOrRestoreRow(standardHeader, !visible);
        standardHeader.setVisible(visible);
    }

    /**
     * @return {@code true} if the standard header is visible, otherwise {@code false}
     * @since 5.8
     */
    public boolean isStandardHeaderVisible()
    {
        return standardHeader.isVisible();
    }

    /**
     * @param visible {@code true} to make the compact header visible, otherwise {@code false}
     * @since 5.8
     */
    public void setCompactHeaderVisible(boolean visible)
    {
        FormLayoutUtils.hideOrRestoreRow(compactHeader, !visible);
        compactHeader.setVisible(visible);
    }

    /**
     * @return {@code true} if the compact header is visible, otherwise {@code false}
     * @since 5.8
     */
    public boolean isCompactHeaderVisible()
    {
        return compactHeader.isVisible();
    }

    /**
     * @param visible {@code true} to make the action button panel visible, otherwise {@code false}
     * @since 5.8
     */
    public void setActionButtonPanelVisible(boolean visible)
    {
        FormLayoutUtils.hideOrRestoreRow(actionButtonPanel, !visible);
        actionButtonPanel.setVisible(visible);
    }

    /**
     * @return {@code true} if the action button panel is visible, otherwise {@code false}
     * @since 5.8
     */
    public boolean isActionButtonPanelVisible()
    {
        return actionButtonPanel.isVisible();
    }

    /**
     * Sets the table to derive an entity definition, and wire up the sort menu.
     *
     * @param table the entity table
     */
    public void setTable(@Nonnull EntityTable<E> table)
    {
        sortMenu.setTable(table);
        var sortableColumns = sortMenu.getSortableMenuItems();
        setSortButtonVisible(!sortableColumns.isEmpty());
        // automatically sort by the first available column
        if (!sortableColumns.isEmpty()) sortableColumns.get(0).doClick();
        standardHeader.setTable(table);
        standardHeader.setColumnModel(table.getColumnModel());
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

    /**
     * @return the sort menu component
     * @since 5.8
     */
    @Nonnull
    public EntityTableSortMenu<E> getSortMenu()
    {
        return sortMenu;
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

    private void createUIComponents()
    {
        sortButton = sortMenu.getButton();
    }

    @SuppressWarnings("RedundantExplicitVariableType")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        createUIComponents();

        actionButtonPanel = new ActionButtonPanel();
        compactHeader = new JPanel();
        standardHeader = new JTableHeader();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBackground(UIManager.getColor("TableHeader.background"));
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "3*(default)"));

        //---- actionButtonPanel ----
        actionButtonPanel.setBackground(UIManager.getColor("Panel.background"));
        actionButtonPanel.setName("actionButtonPanel");
        add(actionButtonPanel, cc.xy(1, 1));

        //======== compactHeader ========
        {
            compactHeader.setBorder(new MatteBorder(0, 0, 1, 0, UIManager.getColor("TableHeader.separatorColor")));
            compactHeader.setBackground(null);
            compactHeader.setName("compactHeader");
            compactHeader.setLayout(new FormLayout(
                    "right:default:grow",
                    "24px"));

            //---- sortButton ----
            sortButton.setBorder(new EmptyBorder(3, 6, 3, 9));
            sortButton.setName("sortButton");
            compactHeader.add(sortButton, cc.xy(1, 1));
        }
        add(compactHeader, cc.xy(1, 2, CellConstraints.FILL, CellConstraints.DEFAULT));

        //---- standardHeader ----
        standardHeader.setOpaque(false);
        standardHeader.setBackground(null);
        standardHeader.setName("standardHeader");
        add(standardHeader, cc.xy(1, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private ActionButtonPanel actionButtonPanel;
    private JPanel compactHeader;
    private JButton sortButton;
    private JTableHeader standardHeader;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
