package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import java.awt.Component;
import java.util.Map;
import java.util.function.Function;

/**
 * A {@link javax.swing.table.TableCellRenderer} that converts the model value into an {@link Icon} which can be specified
 * manually or dynamically via a provided function. Also allows specifying the alignment. The reason you
 * may want to use this over simply passing icons to a table is so that you can sort the icon column on some underlying
 * model value.
 *
 * @since 5.3
 * @param <T> the type of objects that this renderer converts into Icons
 */
@IncubatingApi(since = "5.3")
public class IconTableCellRenderer<T> extends DefaultTableCellRenderer
{
    private final Function<T, Icon> iconSupplier;

    /**
     * Construct a new renderer that renders the specified icon for all rows, with {@link javax.swing.SwingConstants#CENTER}
     * alignment.
     *
     * @param defaultIcon the Icon to render
     * @see IconTableCellRenderer#IconTableCellRenderer(Icon, int)
     */
    @SuppressWarnings("unused")
    public IconTableCellRenderer(@Nullable Icon defaultIcon)
    {
        this(defaultIcon, CENTER);
    }

    /**
     * Construct a new renderer that renders the specified icon with the specified horizontal alignment, for all rows.
     *
     * @param defaultIcon the icon
     * @param horizontalAlignment the alignment
     * @see IconTableCellRenderer#IconTableCellRenderer(Function, Icon, int)
     */
    public IconTableCellRenderer(@Nullable Icon defaultIcon, int horizontalAlignment)
    {
        this(t -> defaultIcon, null, horizontalAlignment);
    }

    /**
     * Construct a new renderer that uses the specified supplier to render icon for all rows, with no default icon and
     * {@link javax.swing.SwingConstants#CENTER} alignment.
     *
     * @param iconSupplier the icon supplier
     * @see IconTableCellRenderer#IconTableCellRenderer(Function, Icon, int)
     */
    public IconTableCellRenderer(@Nonnull Function<T, Icon> iconSupplier)
    {
        this(iconSupplier, null, CENTER);
    }

    /**
     * Construct a new renderer that uses the specified map to generate Icons for cell values.
     * 
     * @param iconMap The Map to use to convert values to Icons. Must be able to handle {@code null} key if present in the model.
     * @since 5.7
     */
    public IconTableCellRenderer(@Nonnull Map<T, Icon> iconMap)
    {
        this(iconMap::get, null, CENTER);
    }

    /**
     * Construct a new renderer that uses the specified supplier to render icon for all rows. When the supplier returns null
     * the defaultIcon will be rendered. This {@link javax.swing.JLabel} uses the specified horizontal alignment.
     *
     * @param iconSupplier the icon supplier function
     * @param defaultIcon the default icon to use when the supplier returns null
     * @param horizontalAlignment the alignment of this label
     */
    public IconTableCellRenderer(@Nonnull Function<T, Icon> iconSupplier, @Nullable Icon defaultIcon, int horizontalAlignment)
    {
        this.iconSupplier = defaultIcon == null ? iconSupplier : t -> {
            var icon = iconSupplier.apply(t);
            return icon != null ? icon : defaultIcon;
        };
        setHorizontalAlignment(horizontalAlignment);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        super.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
        setIcon(iconSupplier.apply((T) value));
        return this;
    }
}