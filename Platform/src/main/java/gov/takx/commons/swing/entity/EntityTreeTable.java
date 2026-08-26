package gov.takx.commons.swing.entity;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.lang.ObjectUtils;
import gov.takx.commons.swing.JXTreeTableExt;
import gov.takx.commons.swing.MouseClickedListener;
import gov.takx.commons.swing.RowBorderHighlighter;
import gov.takx.commons.swing.TableUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.treetable.TreeTableModel;

import javax.swing.Icon;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import static gov.takx.commons.swing.entity.EntityTable.HIGHLIGHT_COLOR;
import static gov.takx.commons.swing.entity.EntityTable.HIGHLIGHT_COLOR_TRANSPARENT;
import static gov.takx.commons.swing.entity.EntityTable.ICON_COLUMN_SIZE;

/**
 * TreeTable to be used with {@link EntityTreeTableModel} and {@link EntityDefinition}. Handles setup for standard columns created by
 * that model.
 *
 * @implNote The goal is for this class to stay in sync with {@link EntityTable} so that both have the same or similar features and ui/ux.
 * @param <E> the entity type that this table works with
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class EntityTreeTable<E> extends JXTreeTableExt
{
    protected final Map<EntityColumn<E, ?>, TableColumnExt> columnMap = new HashMap<>();

    protected EntityTreeTableModel<E> entityTreeTableModel;
    protected TreeTableModel treeTableModel;
    protected TableColumnExt treeColumn;

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
    }

    /**
     * Initialize cell renderers.
     */
    protected void initRenderers()
    {
    }

    /**
     * Initialize table highlighters.
     */
    protected void initHighlighters()
    {
        addHighlighter(new RowBorderHighlighter(HighlightPredicate.ALWAYS, new EmptyBorder(1, 1, 1, 1)));
        addHighlighter(new RowBorderHighlighter(HighlightPredicate.ROLLOVER_ROW, new MatteBorder(1, 1, 1, 1, HIGHLIGHT_COLOR)));
        addHighlighter(new RowBorderHighlighter(HighlightPredicate.IS_SELECTED, new MatteBorder(1, 1, 1, 1, HIGHLIGHT_COLOR)));
        // TODO DCW TAKX-4224 There is some weirdness with the below highlighter but for the most part it works
        addHighlighter(new ColorHighlighter(HighlightPredicate.IS_SELECTED, null, null, HIGHLIGHT_COLOR_TRANSPARENT, null));
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
}
