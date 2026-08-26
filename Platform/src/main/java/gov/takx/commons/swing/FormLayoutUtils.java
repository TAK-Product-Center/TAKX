package gov.takx.commons.swing;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.ConstantSize;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpec;
import com.jgoodies.forms.layout.RowSpec;
import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;

import java.awt.Component;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

/**
 * A utils class for programmatically modifying {@link FormLayout} layouts. Because FormLayouts are often created using
 * JFormDesigner, modifying them in custom code can be cumbersome but can allow for far more user-friendly components. This
 * class is designed to allow applying and un-applying specific changes to the layouts. This can be useful when creating a
 * dynamic form where fields need to be shown/hidden based on prior component actions. For example, hiding or showing
 * specific rows in the layout after the user selects an option from a {@link javax.swing.JComboBox} or
 * {@link javax.swing.JRadioButton}.
 * <p>
 * Most of these changes are designed to be reversible, by persisting the initial layout specifications, such that the
 * initial state is restored when the change is undone. This is an important feature since row and column
 * specifications are complex and tedious to re-build from scratch.
 * <p>
 * Note: Some methods use {@link JComponent#putClientProperty(Object, Object)} to cache values. Using this rather an internal
 * cache is important so that memory leaks are not caused by outside references to swing components.
 *
 * @since 5.6
 */
@IncubatingApi(since = "5.6")
public class FormLayoutUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String ROW_SPEC_CACHE_PROPERTY = "FormLayoutUtils.RowSpecCache";
    private static final String COLUMN_SPEC_CACHE_PROPERTY = "FormLayoutUtils.ColumnSpecCache";

    /**
     * Spec for a hidden row.
     */
    private static final RowSpec HIDDEN_ROW = new RowSpec(new ConstantSize(0, ConstantSize.DIALOG_UNITS_Y));
    /**
     * Spec for a hidden column.
     * @since 5.7
     */
    public static final ColumnSpec HIDDEN_COLUMN = new ColumnSpec(new ConstantSize(0, ConstantSize.DIALOG_UNITS_X));

    /**
     * Hide a row in the {@link FormLayout} of a {@link JComponent}. This method assumes the
     * direct parent of the given component is a {@link JComponent} using a {@link FormLayout} and uses the component to
     * derive the row index, and assumes that the layout has row gaps.
     *
     * @param rowComponent a component on the row to hide
     * @since 5.7
     */
    public static void hideRow(@Nonnull Component rowComponent)
    {
        hideOrRestoreRow(rowComponent, true);
    }

    /**
     * Restore a row in the {@link FormLayout} of a {@link JComponent}. This method assumes the
     * direct parent of the given component is a {@link JComponent} using a {@link FormLayout} and uses the component to
     * derive the row index, and assumes that the layout has row gaps.
     *
     * @param rowComponent a component on the row to restore
     * @since 5.7
     */
    public static void restoreRow(@Nonnull Component rowComponent)
    {
        hideOrRestoreRow(rowComponent, false);
    }

    /**
     * Hide or restore a row in the {@link FormLayout} of a {@link JComponent}. This method assumes the
     * direct parent of the given component is a {@link JComponent} using a {@link FormLayout} and uses the component to
     * derive the row index, and assumes that the layout has row gaps.
     *
     * @param rowComponent a component on the row to hide
     * @param hide {@code true} to hide, or {@code false} to restore
     * @since 5.7
     */
    public static void hideOrRestoreRow(@Nonnull Component rowComponent, boolean hide)
    {
        hideOrRestoreRow((JComponent) rowComponent.getParent(), rowComponent, hide);
    }

    /**
     * Hide or restore a row in the {@link FormLayout} of a {@link JComponent}. This method uses the second component
     * parameter to derive the row index.
     *
     * @param container the component using a FormLayout
     * @param component a component on the row to hide
     * @param hide {@code true} to hide, or {@code false} to restore
     * @since 5.7
     */
    public static void hideOrRestoreRow(@Nonnull JComponent container, @Nonnull Component component, boolean hide)
    {
        hideOrRestoreRow(container, ((FormLayout) container.getLayout()).getConstraints(component).gridY, true, hide);
    }

    /**
     * Hide or restore a row in the {@link FormLayout} of a {@link JComponent} container. This method caches the existing
     * {@link RowSpec} in a client property so that it can be restored at a later time.
     *
     * @param container the component using a FormLayout
     * @param row the row to hide or restore
     * @param gaps {@code true} if the layout has row gaps, otherwise {@code false}
     * @param hide {@code true} to hide, or {@code false} to restore
     * @since 5.6 (Removed a flag in 5.7)
     */
    @SuppressWarnings("unchecked")
    public static void hideOrRestoreRow(@Nonnull JComponent container, int row, boolean gaps, boolean hide)
    {
        var layout = (FormLayout) container.getLayout();
        var rows = layout.getRowCount();
        if (hide)
        {
            if (container.getClientProperty(ROW_SPEC_CACHE_PROPERTY) == null)
            {
                container.putClientProperty(ROW_SPEC_CACHE_PROPERTY, IntStream.range(1, layout.getRowCount() + 1).mapToObj(layout::getRowSpec).toList());
            }
            if (!layout.getRowSpec(row).equals(HIDDEN_ROW)) // Dont proceed if its already hidden
            {
                layout.setRowSpec(row, HIDDEN_ROW);
                // Hide adjacent gap
                if (gaps) findAdjacentSpec(rows, row, layout::getRowSpec, FormFactory.LINE_GAP_ROWSPEC).ifPresent(i -> layout.setRowSpec(i, HIDDEN_ROW));
            }
        } else
        {
            var specs = (List<RowSpec>) container.getClientProperty(ROW_SPEC_CACHE_PROPERTY);
            if (specs != null)
            {
                var cachedSpec = specs.get(row - 1);
                if (!layout.getRowSpec(row).equals(cachedSpec))
                {
                    layout.setRowSpec(row, cachedSpec);
                    // Restore adjacent gap
                    if (gaps) findAdjacentSpec(rows, row, r -> specs.get(r - 1), FormFactory.LINE_GAP_ROWSPEC).ifPresent(i -> layout.setRowSpec(i, specs.get(i - 1)));
                }
            } else
            {
                logger.debug("Cache does not exist. Row is probably already visible");
            }
        }
        container.revalidate();
    }

    /**
     * Hide a column in the {@link FormLayout} of a {@link JComponent}. This method uses the second component parameter
     * to derive the column index and assumes the layout has column gaps.
     *
     * @param colComponent a component on the column to hide
     * @since 5.7
     */
    public static void hideColumn(@Nonnull Component colComponent)
    {
        hideOrRestoreColumn(colComponent, true);
    }

    /**
     * Restore a column in the {@link FormLayout} of a {@link JComponent}. This method uses the second component parameter
     * to derive the column index and assumes the layout has column gaps.
     *
     * @param colComponent a component on the column to hide
     * @since 5.7
     */
    public static void restoreColumn(@Nonnull Component colComponent)
    {
        hideOrRestoreColumn(colComponent, false);
    }

    /**
     * Hide or restore a column in the {@link FormLayout} of a {@link JComponent}. This method uses the second component parameter
     * to derive the column index and assumes the layout has column gaps.
     *
     * @param colComponent a component on the column to hide
     * @param hide {@code true} to hide, or {@code false} to restore
     * @since 5.7
     */
    public static void hideOrRestoreColumn(@Nonnull Component colComponent, boolean hide)
    {
        hideOrRestoreColumn((JComponent) colComponent.getParent(), colComponent, hide);
    }

    /**
     * Hide or restore a column in the {@link FormLayout} of a {@link JComponent}. This method uses the second component parameter
     * to derive the column index and assumes the layout has column gaps.
     *
     * @param container the component using a FormLayout
     * @param component a component on the column to hide
     * @param hide {@code true} to hide, or {@code false} to restore
     * @since 5.7
     */
    public static void hideOrRestoreColumn(@Nonnull JComponent container, @Nonnull Component component, boolean hide)
    {
        hideOrRestoreColumn(container, ((FormLayout) container.getLayout()).getConstraints(component).gridX, true, hide);
    }

    /**
     * Hide or restore a column in the {@link FormLayout} of a {@link JComponent}. This method caches the existing
     * {@link ColumnSpec} in a client property so that it can be restored at a later time.
     *
     * @param container the component using a FormLayout
     * @param column the column to hide or restore
     * @param gaps {@code true} if the layout has column gaps, otherwise {@code false}
     * @param hide {@code true} to hide, or {@code false} to restore
     * @since 5.6 (Removed a flag in 5.7)
     */
    @SuppressWarnings("unchecked")
    public static void hideOrRestoreColumn(@Nonnull JComponent container, int column, boolean gaps, boolean hide)
    {
        var layout = (FormLayout) container.getLayout();
        var columns = layout.getColumnCount();
        if (hide)
        {
            if (container.getClientProperty(COLUMN_SPEC_CACHE_PROPERTY) == null)
            {
                container.putClientProperty(COLUMN_SPEC_CACHE_PROPERTY, IntStream.range(1, layout.getColumnCount() + 1).mapToObj(layout::getColumnSpec).toList());
            }

            if (!layout.getColumnSpec(column).equals(HIDDEN_COLUMN)) // Dont proceed if its already hidden
            {
                layout.setColumnSpec(column, HIDDEN_COLUMN);
                // Hide adjacent gap
                if (gaps) findAdjacentSpec(columns, column, layout::getColumnSpec, FormFactory.LABEL_COMPONENT_GAP_COLSPEC).ifPresent(i -> layout.setColumnSpec(i, HIDDEN_COLUMN));
            }

        } else
        {
            List<ColumnSpec> specs = (List<ColumnSpec>) container.getClientProperty(COLUMN_SPEC_CACHE_PROPERTY);
            if (specs != null)
            {
                var cachedSpec = specs.get(column - 1);
                if (!layout.getColumnSpec(column).equals(cachedSpec))
                {
                    layout.setColumnSpec(column, cachedSpec);
                    // Restore adjacent gap
                    if (gaps) findAdjacentSpec(columns, column, r -> specs.get(r - 1), FormFactory.LABEL_COMPONENT_GAP_COLSPEC).ifPresent(i -> layout.setColumnSpec(i, specs.get(i - 1)));
                }
            } else
            {
                logger.debug("Cache does not exist. Column is probably already visible");
            }
        }
        container.revalidate();
    }

    /**
     * This is a helper method to determine if a collection of {@link FormSpec} instances contains a gap on either side of
     * a given index. This is necessary so that we can effectively hide and restore "gaps" in {@link FormLayout}
     * instances. These gaps can be either {@link RowSpec} or {@link ColumnSpec} instances, and we could be searching either
     * the layout instance directly, or the List of cached specs. Hence, why this needs a "provider" rather than just a 
     * collection. It returns an {@link OptionalInt} for conciseness in the caller logic.
     * <p>
     * Confusing? Yes. But this just ended up being the nicest way to do it. It was either this or four nearly duplicate 
     * chunks.
     * @since 5.8
     */
    @Nonnull
    private static OptionalInt findAdjacentSpec(int size, int index, @Nonnull IntFunction<FormSpec> specProvider, @Nonnull FormSpec find)
    {
        var findEncoded = find.encode();
        // check for an adjacent spec
        for (var r : new int[]{index - 1, index + 1})
        {
            if (r > 1 && r <= size)
            {
                var check = specProvider.apply(r);
                if (check.encode().equals(findEncoded))
                {
                    return OptionalInt.of(r);
                }
            }
        }
        return OptionalInt.empty();
    }
}
