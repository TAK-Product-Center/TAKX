package gov.takx.commons.swing.entity;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.lang.ObjectUtils;
import gov.takx.commons.swing.CheckBoxTableCellRendererEditor;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.swing.JXTableExt;
import gov.takx.commons.swing.RowBorderHighlighter;
import gov.takx.commons.swing.TriStateJCheckBox;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.util.PaintUtils;

import javax.swing.Icon;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Swing table to be used with {@link EntityDefinition} and {@link EntityTableModel}. This class applies a common set of
 * styles to allow tables throughout the application to have a common UI/UX.
 *
 * @param <E> the entity type
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class EntityTable<E> extends JXTableExt implements JXTableExt.ToolTipProvider
{
    public static final int ICON_COLUMN_SIZE = (int) (ImageUtils.MEDIUM_ICON_WIDTH * 1.5);
    public static final Color HIGHLIGHT_COLOR = UIResources.BORDER_HIGHLIGHT_COLOR;
    public static final Color HIGHLIGHT_COLOR_TRANSPARENT = new Color(HIGHLIGHT_COLOR.getRed(), HIGHLIGHT_COLOR.getGreen(), HIGHLIGHT_COLOR.getBlue(), 16);

    protected static final EntityTableModel<?> EMPTY_TABLE_MODEL = new EntityTableModel<>(new EntityDefinition<>());

    protected final Map<EntityColumn<E, ?>, TableColumnExt> columnMap = new HashMap<>();
    private final SelectColumnSelectionModel<E> selectColumnSelectionModel = new SelectColumnSelectionModel<>(); // Model for the checkbox column

    protected EntityColumn<E, TriStateJCheckBox.State> selectEntityColumn;
    protected TableColumnExt selectColumn;

    /**
     * Construct a new instance with an empty model.
     */
    public EntityTable()
    {
        setShowHorizontalLines(true);
        setIntercellSpacing(new Dimension(0, 1));
        setEditorHighlightsEnabled(true);
        setSelectionBackground(null); // We use a custom highlighter
        setBackground(null);
        selectColumnSelectionModel.addSelectionListener(this::selectColumnSelectionChanged);
        setToolTipProvider(this);
        getTableHeader().setResizingAllowed(false);
        getTableHeader().setReorderingAllowed(false);
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

    /**
     * @param visible {@code true} to make the standard header visible, otherwise {@code false}
     * @since 5.8
     */
    public void setStandardHeaderVisible(boolean visible)
    {
        getTableHeader().setStandardHeaderVisible(visible);
    }

    /**
     * @return {@code true} if the standard header is visible, otherwise {@code false}
     * @since 5.8
     */
    public boolean isStandardHeaderVisible()
    {
        return getTableHeader().isStandardHeaderVisible();
    }

    /**
     * @param visible {@code true} to make the compact header visible, otherwise {@code false}
     * @since 5.8
     */
    public void setCompactHeaderVisible(boolean visible)
    {
        getTableHeader().setCompactHeaderVisible(visible);
    }

    /**
     * @return {@code true} if the compact header is visible, otherwise {@code false}
     * @since 5.8
     */
    public boolean isCompactHeaderVisible()
    {
        return getTableHeader().isCompactHeaderVisible();
    }

    /**
     * @param visible {@code true} to make the action button panel visible, otherwise {@code false}
     * @since 5.8
     */
    public void setActionButtonPanelVisible(boolean visible)
    {
        getTableHeader().setActionButtonPanelVisible(visible);
    }

    /**
     * @return {@code true} if the action button panel is visible, otherwise {@code false}
     * @since 5.8
     */
    public boolean isActionButtonPanelVisible()
    {
        return getTableHeader().isActionButtonPanelVisible();
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
            selectEntityColumn = new EntityColumn<>("", true, TriStateJCheckBox.State.class, selectColumnSelectionModel::getSelectionState, null, null);
            ((EntityTableModel<E>) tableModel).addColumn(selectEntityColumn);
            super.setModel(tableModel);
            initModel();
            initRenderers();
            initSorters();
            initHighlighters();
            initListeners();
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
     * @since 5.7 (Renamed from getEntitySelectionModel in 5.8)
     */
    @Nonnull
    public SelectColumnSelectionModel<E> getSelectColumnSelectionModel()
    {
        return selectColumnSelectionModel;
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
     * @return the List of currently selected entities
     * @since 5.8
     */
    @Nonnull
    public List<E> getSelectedEntities()
    {
        return IntStream.of(getSelectionModel().getSelectedIndices()).map(this::convertRowIndexToModel).mapToObj(i -> getModel().getRow(i)).toList();
    }

    /**
     * @return the entity for the first selected row, or {@code null} if no rows are selected
     * @since 5.8
     */
    @Nullable
    public E getSelectedEntity()
    {
        var row = getSelectedRow();
        return row == -1 ? null : getModel().getRow(convertRowIndexToModel(row));
    }

    /**
     * @param entity the entity whose row should be selected
     * @since 5.8
     */
    public void setSelectedEntity(@Nonnull E entity)
    {
        var row = convertRowIndexToView(getModel().indexOf(entity));
        setRowSelectionInterval(row, row);
    }

    @Override
    public String getToolTipText(int row, int column)
    {
        var entityColumn = getModel().getColumn(convertColumnIndexToModel(column));
        var tooltip = entityColumn.tooltip();
        if (tooltip != null)
        {
            var entity = getModel().getRow(convertRowIndexToModel(row));
            return tooltip.apply(entity);
        }
        return null;
    }

    @Override
    public void tableChanged(@Nonnull TableModelEvent event)
    {
        super.tableChanged(event);

        /*
        TODO TAKX-4239 This is infuriating (mostly because I don't understand why) but for some reason we have to forcibly fire a second event
            to refresh the entire row when an update happens for a column that is both sorted and invisible. When updates happen for sorted columns
            the table must re-sort (obviously) but for some reason when this column is ALSO invisible, the table does not correctly redraw other
            rows that have now been sorted to other locations. Triggering a full refresh on the entire row seems to resolve the issue.
         */
        var forceSecondRefresh = false;
        if (event.getType() == TableModelEvent.UPDATE)
        {
            var sortedColumn = (TableColumnExt) getSortedColumn();
            if (sortedColumn != null && !sortedColumn.isVisible())
            {
                var eventColumn = event.getColumn();
                if (eventColumn == TableModelEvent.ALL_COLUMNS || requireColumn(getModel().getColumn(eventColumn)) == sortedColumn)
                {
                    forceSecondRefresh = true;
                }
            }
        }
        if (forceSecondRefresh)
        {
            super.tableChanged(new TableModelEvent(getModel(), event.getFirstRow(), event.getLastRow(), TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE));
        }
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
     * @since 5.7 (Renamed from selectionChanged in 5.8)
     */
    protected void selectColumnSelectionChanged(@Nonnull SelectColumnSelectionEvent<E> event)
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
            tableColumn.setHeaderValue(modelColumn.header());
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
        addHighlighter(new ColorHighlighter(HighlightPredicate.IS_SELECTED, null, null, PaintUtils.blend(getBackground(), HIGHLIGHT_COLOR_TRANSPARENT), null));
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
                        case SELECTED -> selectColumnSelectionModel.addSelection(entityTableModel.getRow(row));
                        case NOT_SELECTED -> selectColumnSelectionModel.removeSelection(entityTableModel.getRow(row));
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

    /**
     * Initializes table listeners.
     *
     * @since 5.8
     */
    protected void initListeners()
    {
        // Currently a no-op
    }
}
