package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import org.jdesktop.swingx.sort.StringValueProvider;
import org.jdesktop.swingx.sort.TableSortController;

import javax.swing.table.TableModel;

import java.util.HashSet;
import java.util.Set;

/**
 * Extension of {@link TableSortController} that allows forcing columns to use the {@link org.jdesktop.swingx.sort.StringValueProvider}
 * set in {@link #setStringValueProvider(StringValueProvider)}. This can be necessary when you want to sort on a value other
 * than the actual column value.
 *
 * @since 5.6
 */
@SuppressWarnings("unused")
@IncubatingApi(since = "5.6")
public class TableSortControllerExt<M extends TableModel> extends TableSortController<M>
{
    private final Set<Integer> forceToString = new HashSet<>();

    /**
     * Construct a new instance.
     */
    public TableSortControllerExt()
    {
    }

    /**
     * Construct a new instance and use the provided model to configure default sorting.
     *
     * @param model the table model
     */
    public TableSortControllerExt(M model)
    {
        setModel(model);
    }

    /**
     * This override first checks whether the column is forced via {@link #forceToString(int)} then checks
     * {@link TableSortController#useToString(int)}.
     *
     * @param column the index of the column to test, in terms of the underlying model
     * @return {@code true} if the column should use the string value provider
     */
    @Override
    protected boolean useToString(int column)
    {
        return forceToString.contains(column) || super.useToString(column);
    }

    /**
     * @param column the model column to force to use the string value provider
     */
    public void forceToString(int column)
    {
        forceToString.add(column);
    }
}
