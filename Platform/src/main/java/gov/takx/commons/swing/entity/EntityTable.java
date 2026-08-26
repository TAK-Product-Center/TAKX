package gov.takx.commons.swing.entity;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.lang.ObjectUtils;
import gov.takx.commons.swing.CheckBoxTableCellRendererEditor;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.swing.JXTableExt;
import gov.takx.commons.swing.RowBorderHighlighter;
import gov.takx.commons.swing.TriStateJCheckBox;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.table.TableColumnExt;

import javax.swing.Icon;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Swing table to be used with {@link EntityDefinition} and {@link EntityTableModel}. This class applies a common set of
 * styles to allow tables throughout the application to have a common UI/UX.
 *
 * @param <E> the entity type
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class EntityTable<E> extends JXTableExt
{
    public static final int ICON_COLUMN_SIZE = (int) (ImageUtils.MEDIUM_ICON_WIDTH * 1.5);
    public static final Color HIGHLIGHT_COLOR = new Color(38, 118, 157);
    public static final Color HIGHLIGHT_COLOR_TRANSPARENT = new Color(38, 118, 157, 16);

    protected static final EntityTableModel<?> EMPTY_TABLE_MODEL = new EntityTableModel<>(new EntityDefinition<>());

    protected final Map<EntityColumn<E, ?>, TableColumnExt> columnMap = new HashMap<>();
    protected EntityColumn<E, TriStateJCheckBox.State> selectEntityColumn;
    protected TableColumnExt selectColumn;
    protected EntityTableSelectionModel<E> selectionModel = new EntityTableSelectionModel<>();

    /**
     * Construct a new instance with an empty model.
     */
    public EntityTable()
    {
        getTableHeader().setReorderingAllowed(false);
        setShowHorizontalLines(true);
        setIntercellSpacing(new Dimension(0, 1));
        setEditorHighlightsEnabled(true);
        selectionModel.addSelectionListener(this::selectionChanged);
    }

    /**
     * @return a new instance of {@link EntityTableHeader}
     */
    @Override
    protected JTableHeader createDefaultTableHeader()
    {
        return new EntityTableHeader<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public EntityTableHeader<E> getTableHeader()
    {
        return (EntityTableHeader<E>) super.getTableHeader();
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nonnull
    public EntityTableModel<E> getModel()
    {
        return (EntityTableModel<E>) super.getModel();
    }

    /**
     * @param tableModel the model that should be used with this table
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setModel(@Nonnull TableModel tableModel)
    {
        if (!(tableModel instanceof EntityTableModel<?>)) throw new IllegalArgumentException("Model should be an instance of " + EntityTableModel.class.getName());
        if (tableModel != EMPTY_TABLE_MODEL)
        {
            if (super.getModel() != EMPTY_TABLE_MODEL) throw new UnsupportedOperationException("Cannot update model after it has been set");
            selectEntityColumn = new EntityColumn<>("", true, TriStateJCheckBox.State.class, o -> selectionModel.getSelectionState(o), null, null);
            ((EntityTableModel<E>) tableModel).addColumn(selectEntityColumn);
            super.setModel(tableModel);
            initModel();
            initRenderers();
            initSorters();
            initHighlighters();
            setAutoCreateColumnsFromModel(false); // This is to prevent structure events from mucking with the model setup
            getTableHeader().setTable(this);
        } else
        {
            super.setModel(tableModel);
        }
    }

    /**
     * @return the entity column instance for the select checkbox column
     */
    @Nonnull
    public EntityColumn<E, TriStateJCheckBox.State> getSelectEntityColumn()
    {
        return selectEntityColumn;
    }

    /**
     * @return the table column instance for the select checkbox column
     */
    @Nonnull
    public TableColumnExt getSelectColumn()
    {
        return selectColumn;
    }

    /**
     * @return the entity definition used by this table
     */
    @Nonnull
    public EntityDefinition<E> getEntityDefinition()
    {
        return getModel().getEntityDefinition();
    }

    /**
     * @return the entity selection model used to track selections made in the select column
     */
    @Nonnull
    public EntityTableSelectionModel<E> getEntitySelectionModel()
    {
        return selectionModel;
    }

    /**
     * @param point A screen point
     * @return The entity in the table underneath the given screen point
     */
    @Nullable
    public E getEntityAtPoint(@Nonnull Point point)
    {
        var r = rowAtPoint(point);
        return r >= 0 && r < getRowCount()
                ? getModel().getRow(convertRowIndexToModel(r))
                : null;
    }

    /**
     * @param point A screen point
     * @return The column in the table underneath the given screen point
     */
    @Nullable
    public EntityColumn<E, ?> getColumnAtPoint(@Nonnull Point point)
    {
        var modelColumn = convertColumnIndexToModel(columnAtPoint(point));
        return modelColumn > -1 ? getModel().getColumn(modelColumn) : null;
    }

    /**
     * @param column the entity column
     * @return the table column that corresponds to the given entity column
     */
    @Nullable
    public TableColumnExt getColumn(@Nonnull EntityColumn<E, ?> column)
    {
        return columnMap.get(column);
    }

    /**
     * @param column the entity column
     * @return the table column that corresponds to the given entity column
     * @throws IllegalArgumentException if the column does not exist in this table
     */
    @Nonnull
    public TableColumnExt requireColumn(@Nonnull EntityColumn<E, ?> column)
    {
        return ObjectUtils.requireNonNull(columnMap.get(column), () -> new IllegalArgumentException("Column map does not contain a table column for " + column));
    }

    /**
     * @return the {@link #EMPTY_TABLE_MODEL} so that the TableModel for this class is always an instance of {@link EntityTableModel}.
     */
    @Override
    @Nonnull
    protected TableModel createDefaultDataModel()
    {
        return EMPTY_TABLE_MODEL;
    }

    /**
     * Updates the select column in the table when the selection model changes.
     *
     * @param event the selection change event
     */
    protected void selectionChanged(@Nonnull EntitySelectionEvent<E> event)
    {
        var entityTableModel = getModel();
        var selectColumn = getSelectEntityColumn();
        event.getEntities().forEach(entity -> entityTableModel.fireTableCellUpdated(entity, selectColumn));
    }

    /**
     * Initializes the model columns.
     */
    protected void initModel()
    {
        var columns = getColumns(true);
        var entityTableModel = getModel();
        var modelColumns = entityTableModel.getColumns();
        for (var i = 0; i < modelColumns.size(); i++)
        {
            var modelColumn = modelColumns.get(i);
            var tableColumn = (TableColumnExt) columns.get(i);
            tableColumn.setIdentifier(modelColumn);
            columnMap.put(modelColumn, tableColumn);
            // Setup any icon columns
            if (modelColumn.type() == Icon.class)
            {
                tableColumn.setMinWidth(ICON_COLUMN_SIZE);
                tableColumn.setMaxWidth(ICON_COLUMN_SIZE);
                tableColumn.setSortable(false);
            }
        }

        selectColumn = requireColumn(selectEntityColumn);
        selectColumn.setVisible(false);
    }

    /**
     * Initializes table highlighters.
     */
    protected void initHighlighters()
    {
        addHighlighter(new RowBorderHighlighter(HighlightPredicate.ALWAYS, new EmptyBorder(1, 1, 1, 1)));
        addHighlighter(new RowBorderHighlighter(HighlightPredicate.ROLLOVER_ROW, new MatteBorder(1, 1, 1, 1, HIGHLIGHT_COLOR)));
        addHighlighter(new RowBorderHighlighter(HighlightPredicate.IS_SELECTED, new MatteBorder(1, 1, 1, 1, HIGHLIGHT_COLOR)));
    }

    /**
     * Initializes table cell renderers.
     */
    protected void initRenderers()
    {
        var entityTableModel = getModel();
        CheckBoxTableCellRendererEditor.setRendererAndEditor(
                this,
                selectColumn,
                event -> {
                    var cb = (TriStateJCheckBox) event.getSource();
                    var state = cb.getState();
                    var row = Integer.parseInt(event.getActionCommand());
                    switch (state)
                    {
                        case SELECTED -> selectionModel.addSelection(entityTableModel.getRow(row));
                        case NOT_SELECTED -> selectionModel.removeSelection(entityTableModel.getRow(row));
                        case INDETERMINATE ->
                        {
                            // No-op since renderer should not allow user to trigger a partial selection
                        }
                    }
                });
    }

    /**
     * Initializes table column sorters.
     */
    @SuppressWarnings({"DataFlowIssue", "unchecked"})
    protected void initSorters()
    {
        for (var column : getEntityDefinition().getColumns())
        {
            var tce = getColumn(column);
            if (column.sortable())
            {
                var clazz = column.type();
                if (column.sortValue() != null)
                {
                    tce.setSortable(true);
                    Comparator<?> comparator = Comparator.<E, Comparable<? super Comparable<?>>>comparing(v -> (Comparable<? super Comparable<?>>) column.sortValue().apply(v));
                    tce.setComparator(comparator);
                } else if (column.comparator() != null)
                {
                    tce.setComparator(column.comparator());
                    tce.setSortable(true);
                } else if (clazz == String.class)
                {
                    tce.setSortable(true);
                    tce.setComparator(Comparator.comparing(Object::toString, String.CASE_INSENSITIVE_ORDER));
                } else if (!(Comparable.class.isAssignableFrom(clazz)))
                {
                    tce.setSortable(false);
                }
            } else
            {
                tce.setSortable(false);
            }
        }
    }
}
