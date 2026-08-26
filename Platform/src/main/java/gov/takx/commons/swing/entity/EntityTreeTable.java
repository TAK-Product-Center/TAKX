package gov.takx.commons.swing.entity;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.lang.ObjectUtils;
import gov.takx.commons.swing.CheckBoxTreeTableTreeCellRendererEditor;
import gov.takx.commons.swing.JXTreeTableExt;
import gov.takx.commons.swing.MouseClickedListener;
import gov.takx.commons.swing.RowBorderHighlighter;
import gov.takx.commons.swing.TableUtils;
import gov.takx.commons.swing.TriStateJCheckBox;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.treetable.TreeTableModel;
import org.jdesktop.swingx.util.PaintUtils;

import javax.swing.Icon;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.TreeSelectionEvent;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import static gov.takx.commons.swing.entity.EntityTable.HIGHLIGHT_COLOR;
import static gov.takx.commons.swing.entity.EntityTable.HIGHLIGHT_COLOR_TRANSPARENT;
import static gov.takx.commons.swing.entity.EntityTable.ICON_COLUMN_SIZE;

/**
 * TreeTable to be used with {@link EntityTreeTableModel} and {@link EntityDefinition}. Handles setup for columns created by that model. This class
 * includes a "selectColumn" feature which allows selecting tree nodes via checkboxes rather than the normal swing row click style selection.
 *
 * @param <E> the entity type that this table works with
 * @apiNote The "selectColumn" is not really a column, but it's close enough, so we are using that terminology to remain consistent with
 * {@link EntityTable}.
 * @implNote The goal is for this class to stay in sync with {@link EntityTable} so that both have the same or similar features and ui/ux.
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class EntityTreeTable<E> extends JXTreeTableExt
{
    protected final Map<EntityColumn<E, ?>, TableColumnExt> columnMap = new HashMap<>();

    protected EntityTreeTableModel<E> entityTreeTableModel; // The entity model
    protected TreeTableModel treeTableModel; // The model that actually drives the tree contents
    protected TableColumnExt treeColumn;
    private boolean selectColumnVisible;
    private SelectColumnTreeSelectionModel<E> selectColumnSelectionModel;
    private boolean initModel;

    /**
     * Construct a new tree table component with no data.
     */
    public EntityTreeTable()
    {
        // Use LAF icons
        setExpandedIcon(UIManager.getIcon("Tree.expandedIcon"));
        setCollapsedIcon(UIManager.getIcon("Tree.collapsedIcon"));

        // Don't want any node icons right now
        setLeafIcon(null);
        setOpenIcon(null);
        setClosedIcon(null);

        setBackground(null);
        setSelectionBackground(null);
        setFocusable(false); // If we leave this to true then expander cell is selectable for some reason
        setShowHorizontalLines(true);
        setIntercellSpacing(new Dimension(0, 1));
        addMouseListener((MouseClickedListener) this::mouseClicked);
    }

    /**
     * @param column the entity column
     * @return the table column that corresponds to the given entity column
     * @throws IllegalArgumentException if the column does not exist in this table
     */
    @Nonnull
    public TableColumnExt getColumn(@Nonnull EntityColumn<E, ?> column)
    {
        return ObjectUtils.requireNonNull(columnMap.get(column), () -> new IllegalArgumentException("Column map does not contain a table column for " + column));
    }

    /**
     * Set the tree table model and the entity model for use with this component.
     *
     * @param treeTableModel the model to use for sourcing data in this component
     * @param entityTreeTableModel the entity model to use for configuring columns in this component
     * @throws IllegalStateException if the model has already been set
     */
    public void setModels(@Nonnull TreeTableModel treeTableModel, @Nonnull EntityTreeTableModel<E> entityTreeTableModel)
    {
        if (this.entityTreeTableModel != null) throw new IllegalStateException("Cannot change model after it has been set");
        setTreeTableModel(treeTableModel);
        this.entityTreeTableModel = entityTreeTableModel;
        this.treeTableModel = treeTableModel;
        initModel();
        initRenderers();
        initHighlighters();
        setAutoCreateColumnsFromModel(false);
    }

    /**
     * @return the entity definition used in this component, derived from the {@link EntityTreeTableModel}
     */
    @Nullable
    public EntityDefinition<E> getEntityDefinition()
    {
        return entityTreeTableModel.getEntityDefinition();
    }

    /**
     * @param point A screen point
     * @return The entity in the table underneath the given screen point
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public E getEntityAtPoint(@Nonnull Point point)
    {
        var path = getPathForLocation(point.x, point.y);
        return path != null ? (E) path.getLastPathComponent() : null;
    }

    /**
     * @param point a screen point
     * @return The table column underneath the given screen point
     */
    @Nullable
    public EntityColumn<E, ?> getColumnAtPoint(@Nonnull Point point)
    {
        var modelColumn = convertColumnIndexToModel(columnAtPoint(point));
        return modelColumn > -1 ? entityTreeTableModel.getColumn(modelColumn) : null;
    }

    /**
     * @param visible {@code true} if the select checkbox should be visible, otherwise {@code false}
     * @since 5.8
     */
    public void setSelectColumnVisible(boolean visible)
    {
        selectColumnVisible = visible;
        if (initModel && selectColumnVisible && getHierarchicalEditor() == null) CheckBoxTreeTableTreeCellRendererEditor.setRendererAndEditor(this, selectColumnSelectionModel::getSelectionState, this::selectColumnSelection);
    }

    /**
     * @return {@code true} if the select checkbox is visible, otherwise {@code false}
     * @since 5.8
     */
    public boolean isSelectColumnVisible()
    {
        return selectColumnVisible;
    }

    /**
     * @return the entity selection model used to track selections made in the select column
     * @since 5.8
     */
    @Nonnull
    public SelectColumnTreeSelectionModel<E> getSelectColumnSelectionModel()
    {
        return selectColumnSelectionModel;
    }

    /**
     * Initialize the data model.
     */
    protected void initModel()
    {
        // Create column map so we can easily access columns
        var columns = getColumns(true);
        var modelColumns = entityTreeTableModel.getColumns();
        for (var i = 0; i < modelColumns.size(); i++)
        {
            var modelColumn = modelColumns.get(i);
            var tableColumn = (TableColumnExt) columns.get(i);
            tableColumn.setIdentifier(modelColumn);
            columnMap.put(modelColumn, tableColumn);
            // Setup icon columns
            if (modelColumn.type() == Icon.class)
            {
                TableUtils.setColumnWidths(ICON_COLUMN_SIZE, tableColumn);
                tableColumn.setSortable(false);
            }
        }
        treeColumn = getColumn(entityTreeTableModel.getHierarchicalEntityColumn());
        treeColumn.setVisible(true);
        selectColumnSelectionModel = new SelectColumnTreeSelectionModel<>(treeTableModel);
        selectColumnSelectionModel.addTreeSelectionListener(this::selectColumnSelectionChanged);
        initModel = true;
    }

    /**
     * Initialize cell renderers.
     */
    protected void initRenderers()
    {
        setSelectColumnVisible(selectColumnVisible);
    }

    /**
     * Initialize table highlighters.
     */
    protected void initHighlighters()
    {
        addHighlighter(new RowBorderHighlighter(HighlightPredicate.ALWAYS, new EmptyBorder(1, 1, 1, 1)));
        addHighlighter(new RowBorderHighlighter(HighlightPredicate.ROLLOVER_ROW, new MatteBorder(1, 1, 1, 1, HIGHLIGHT_COLOR)));
        addHighlighter(new RowBorderHighlighter(HighlightPredicate.IS_SELECTED, new MatteBorder(1, 1, 1, 1, HIGHLIGHT_COLOR)));
        addHighlighter(new ColorHighlighter(HighlightPredicate.IS_SELECTED, null, null, PaintUtils.blend(getBackground(), HIGHLIGHT_COLOR_TRANSPARENT), null));
    }

    private void mouseClicked(MouseEvent event)
    {
        if (!event.isConsumed() && event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() % 2 == 0)
        {
            var point = event.getPoint();
            var modelColumn = convertColumnIndexToModel(columnAtPoint(point));
            if (modelColumn == getTreeTableModel().getHierarchicalColumn())
            {
                toggleExpandRow(rowAtPoint(point));
                event.consume(); // Make sure we take no other action
            }
        }
    }

    private void selectColumnSelectionChanged(@Nonnull TreeSelectionEvent treeSelectionEvent)
    {
        for (var path : treeSelectionEvent.getPaths())
        {
            //noinspection unchecked all nodes are same type
            entityTreeTableModel.fireEntityChanged((E) path.getLastPathComponent());
        }
    }

    private void selectColumnSelection(@Nonnull ActionEvent event)
    {
        var state = ((TriStateJCheckBox) event.getSource()).getState();
        // Unlike the corresponding code in EntityTable, this event returns the view row, not the model row, since there are no model rows in a tree
        var treePath = getPathForRow(Integer.parseInt(event.getActionCommand()));
        switch (state)
        {
            case SELECTED -> selectColumnSelectionModel.addSelectionPath(treePath);
            case NOT_SELECTED -> selectColumnSelectionModel.removeSelectionPath(treePath);
            case INDETERMINATE ->
            {
                // No-op since renderer should not allow user to trigger a partial selection
            }
        }
    }
}
