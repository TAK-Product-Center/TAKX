package gov.takx.commons.swing;

import jakarta.annotation.Nonnull;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import java.awt.Component;
import java.util.function.Function;

/**
 * Implementation of {@link javax.swing.ListCellRenderer} that allows mapping the cell value via a mapping
 * {@link Function}. Similar to {@link DelegatingTableCellRenderer} for table cells (Though that class currently has more features).
 *
 * @param <T> the type of expected values
 * @since 5.7
 */
public class DelegatingListCellRenderer<T> extends DefaultListCellRenderer
{
    private final Function<T, String> mappingFunction;

    /**
     * Create a new instance, using the given mapping function to map value objects to {@link String}. Note that the mapping
     * function will also receive {@code null} values if they are present in the underlying model.
     *
     * @param mappingFunction the mapping Function
     */
    public DelegatingListCellRenderer(@Nonnull Function<T, String> mappingFunction)
    {
        this.mappingFunction = mappingFunction;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
        return super.getListCellRendererComponent(list, mappingFunction.apply((T) value), index, isSelected, cellHasFocus);
    }
}
