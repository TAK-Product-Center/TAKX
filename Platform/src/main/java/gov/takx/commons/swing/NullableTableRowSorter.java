package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.DefaultRowSorter;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import java.util.Comparator;

/**
 * This class allows users to more easily sort a Swing {@link JTable} where the model contains null values.<br/>
 * <b>Why this is needed:</b>
 * <br/>
 * The Swing {@link JTable} uses a {@link RowSorter}
 * to control sorting. The most-common, and simplest way to implement this is via the {@link TableRowSorter} which extends
 * {@link DefaultRowSorter}. However, the DefaultRowSorter automatically forces null values to be lower than
 * non-null, and provides no way to override this behavior. This means that nulls in a JTable using that sorter
 * will always be sorted first. Writing a custom comparator will not solve this, because DefaultRowSorter won't even
 * pass the null values to the comparator. The method that does this {@link DefaultRowSorter#compare(int, int)}, cannot be
 * overridden, so we can't fix the issue directly (without copying the entire class). Most users end up polluting their
 * model by returning a null placeholder, which can then be passed to the registered comparator. This class prevents having
 * to do that.
 * <br/>
 * <b>How it works:</b>
 * <br/>
 * The way it works is a bit wonky, but it's the only way Swing allows. We override the {@link ModelWrapper}
 * field of the DefaultRowSorter with one that replaces null values with a placeholder. Then we also wrap
 * all registered Comparators with one that converts that placeholder back to null before passing to the real Comparator. This
 * allows the external comparators to see null values, while letting the {@link RowSorter} see a non-null value.

 * @param <M> the {@link TableModel} type
 * @since 5.3
 */
@SuppressWarnings("JavadocReference")
@IncubatingApi(since = "5.3")
public class NullableTableRowSorter<M extends TableModel> extends TableRowSorter<M>
{

    private static final Object NULL_VALUE = "NULL_PLACEHOLDER";

    /**
     * Construct a new instance with no model.
     */
    public NullableTableRowSorter()
    {
        super();
    }

    /**
     * Construct a new instance with the given model.
     *
     * @param model the table model to use
     */
    public NullableTableRowSorter(@Nullable M model)
    {
        super(model);
    }

    /**
     * {@inheritDoc}
     * Wraps the model in a {@link NullCompatibleTableRowSorterModelWrapper}.
     *
     * @param model the table model
     */
    @Override
    public void setModel(@Nullable M model)
    {
        super.setModel(model);
        setModelWrapper(new NullCompatibleTableRowSorterModelWrapper(getModelWrapper()));
    }

    /**
     * {@inheritDoc}
     * Wraps the comparator in a {@link NullReplacementComparator}.
     *
     * @param column the index of the colum
     * @param comparator the Comparator to use
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setComparator(int column, @Nonnull Comparator<?> comparator)
    {
        super.setComparator(column, new NullReplacementComparator((Comparator<Object>) comparator));
    }

    /**
     * Wraps a real Comparator with one that replaces the null placeholder with a true {@code null}.
     */
    private class NullReplacementComparator implements Comparator<Object>
    {
        private final Comparator<Object> real;

        protected NullReplacementComparator(Comparator<Object> real)
        {
            this.real = real;
        }

        @Override
        public int compare(Object o1, Object o2)
        {
            if (o1 == NULL_VALUE) o1 = null;
            if (o2 == NULL_VALUE) o2 = null;
            return real.compare(o1, o2);
        }
    }

    /**
     * Returns a null placeholder when the real model value is {@code null}.
     */
    private class NullCompatibleTableRowSorterModelWrapper extends ModelWrapper<M, Integer>
    {

        private final ModelWrapper<M, Integer> delegateModelWrapper;

        protected NullCompatibleTableRowSorterModelWrapper(ModelWrapper<M, Integer> delegate)
        {
            this.delegateModelWrapper = delegate;
        }

        @Override
        public M getModel()
        {
            return delegateModelWrapper.getModel();
        }

        @Override
        public int getColumnCount()
        {
            return delegateModelWrapper.getColumnCount();
        }

        @Override
        public int getRowCount()
        {
            return delegateModelWrapper.getRowCount();
        }

        @Override
        public Object getValueAt(int row, int column)
        {
            Object value = delegateModelWrapper.getValueAt(row, column);
            if (value == null) value = NULL_VALUE;
            return value;
        }

        @Override
        public Integer getIdentifier(int row)
        {
            return delegateModelWrapper.getIdentifier(row);
        }
    }
}