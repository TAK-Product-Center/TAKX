package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import java.awt.Component;
import java.util.function.Function;

/**
 * Implementation of {@link TableCellRenderer} that allows mapping the cell value to another value as well as delegating the
 * rendering to another instance. This reduces the code necessary for many renderers by allowing them to pass method
 * references. By default, the delegate is the super {@link #getTableCellRendererComponent(JTable, Object, boolean, boolean, int, int)}
 * method, and the mapping function is {@link Function#identity()}. The {@link ValueConverter} interface can be used when
 * more complex value conversions are necessary and all parameters of the renderer are needed.
 *
 * @param <T> the type of expected values
 * @since 5.3
 */
@IncubatingApi(since = "5.3")
public class DelegatingTableCellRenderer<T> extends DefaultTableCellRenderer
{
    private final TableCellRenderer delegate;
    private final ValueConverter<T> mapping;
    private Function<T, String> tooltipMapping;

    /**
     * Construct a new instance with no delegate rendered or mapping function.
     */
    @SuppressWarnings("unused")
    public DelegatingTableCellRenderer()
    {
        this(null, Function.identity());
    }

    /**
     * Construct a new instance with a delegate and no mapping function.
     *
     * @param delegate the renderer to delegate to
     */
    @SuppressWarnings("unused")
    public DelegatingTableCellRenderer(@Nonnull TableCellRenderer delegate)
    {
        this(delegate, Function.identity());
    }

    /**
     * Construct a new instance with no delegate and the specified value mapping function.
     *
     * @param mapping the mapping function
     */
    public DelegatingTableCellRenderer(@Nonnull Function<T, ?> mapping)
    {
        this(null, mapping);
    }

    /**
     * Construct a new instance with a value converter.
     *
     * @param mapping the value converter
     * @since 5.6
     */
    public DelegatingTableCellRenderer(@Nonnull ValueConverter<T> mapping)
    {
        this(null, mapping);
    }

    /**
     * Construct a new instance.
     *
     * @param delegate the renderer to delegate to, or {@code null} to use this instance
     * @param mapping the mapping function
     */
    @SuppressWarnings("unchecked")
    public DelegatingTableCellRenderer(@Nullable TableCellRenderer delegate, @Nonnull Function<T, ?> mapping)
    {
        this(delegate, (table, value, isSelected, hasFocus, row, column) -> mapping.apply((T) value));
    }

    /**
     * Construct a new instance.
     *
     * @param delegate the renderer to delegate to, or {@code null} to use this instance
     * @param mapping the mapping function
     * @since 5.6
     */
    public DelegatingTableCellRenderer(@Nullable TableCellRenderer delegate, @Nullable ValueConverter<T> mapping)
    {
        this.delegate = delegate == null ? super::getTableCellRendererComponent : delegate;
        this.mapping = mapping;
    }

    /**
     * @param mapping Mapping function to produce the tooltip text for a cell
     * @since 5.7
     */
    public void setTooltipMapping(@Nonnull Function<T, String> mapping)
    {
        tooltipMapping = mapping;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        final String tooltip = tooltipMapping != null ? tooltipMapping.apply((T) value) : null;

        final Component component = invokeDelegate(table, invokeMapping(table, (T) value, isSelected, hasFocus, row, column), isSelected, hasFocus, row, column);
        ((JComponent) component).setToolTipText(tooltip);
        return component;
    }

    /**
     * Invoke the delegate renderer with the standard renderer parameters.
     *
     * @param table the table
     * @param value the cell value
     * @param isSelected {@code true} if the cell is selected
     * @param hasFocus @{@code true} if the cell has focus
     * @param row the row index
     * @param column the column index
     * @return the component to render
     * @since 5.3 (Removed delegate parameter in 5.6)
     */
    protected Component invokeDelegate(@Nonnull JTable table, @Nullable Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        return delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

    /**
     * Invoke the mapping function.
     * 
     * @param value the cell value to map
     * @return the mapped value
     * @since 5.3 (Added additional parameters in 5.6)
     */
    protected Object invokeMapping(@Nonnull JTable table, @Nullable T value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        return mapping.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

    /**
     * A functional interface that allows consumers to convert the value passed to the renderer to a new value. This
     * interface exists for the scenario where a consumer needs access to all the standard table cell renderer parameters
     * to perform a value mapping.
     *
     * @since 5.6
     */
    @FunctionalInterface
    public interface ValueConverter<T>
    {
        /**
         * @param table the table component
         * @param value the cell value
         * @param isSelected {@code true} if the cell is selected
         * @param hasFocus @{@code true} if the cell has focus
         * @param row the row index
         * @param column the column index
         * @return the converted value
         */
        Object getTableCellRendererComponent(@Nonnull JTable table, @Nullable Object value,
                                             boolean isSelected, boolean hasFocus,
                                             int row, int column);
    }
}