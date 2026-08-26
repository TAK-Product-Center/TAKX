package gov.takx.commons.swing.entity;

import com.google.auto.value.AutoBuilder;
import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Comparator;
import java.util.function.Function;

/**
 * Record representing a column in an {@link EntityDefinition}.
 *
 * @param name the column name
 * @param editable {@code true} if the column cells are editable, otherwise {@code false}
 * @param type the column value class
 * @param value a function to convert the row object to the cell value
 * @param comparator the comparator to sort values in this column
 * @param sortValue a function to convert the row value to a sortable value
 * @param cached {@code true} if values for this column can be cached, otherwise {@code false}
 * @param sortable {@code true} to allow the column to be sortable (does not guarantee sortability) or {@code false} to force it to be unsortable
 * @param <E> the entity type
 * @param <C> the column type
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public record EntityColumn<E, C>(@Nonnull String name, boolean editable, @Nonnull Class<?> type, @Nonnull Function<E, C> value, @Nullable Comparator<C> comparator, @Nullable Function<E, Comparable<?>> sortValue, boolean cached, boolean sortable)
{

    /**
     * Construct a new uncached instance.
     *
     * @param name the column name
     * @param editable {@code true} if the column cells are editable, otherwise {@code false}
     * @param type the column value class
     * @param value a function to convert the row object to the cell value
     * @param comparator the comparator to sort values in this column
     * @param sortValue a function to convert the row value to a sortable value
     */
    public EntityColumn(@Nonnull String name, boolean editable, @Nonnull Class<?> type, @Nonnull Function<E, C> value, @Nullable Comparator<C> comparator, @Nullable Function<E, Comparable<?>> sortValue)
    {
        this(name, editable, type, value, comparator, sortValue, false, false);
    }

    /**
     * Returns a builder with default values:
     * <ul>
     *     <li>Name: {@code ""}</li>
     *     <li>Class: {@link Object}</li>
     *     <li>Editable: {@code false}</li>
     *     <li>Value: {@code v -> v}</li>
     *     <li>Comparator: {@code null}</li>
     * </ul>
     *
     * @return a builder with default properties
     * @param <E> the entity type
     * @param <C> the column type
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public static <E, C> Builder<E, C> builder()
    {
        return new AutoBuilder_EntityColumn_Builder<E, C>()
                .name("")
                .type(Object.class)
                .value(v -> (C) v)
                .editable(false)
                .sortable(true)
                .sortValue(null)
                .comparator(null)
                .cached(false);
    }

    /**
     * Builder for {@link EntityColumn} instances.
     * @param <E> the entity type
     * @param <C> the column type
     */
    @SuppressWarnings("NullableProblems")
    @AutoBuilder
    @IncubatingApi(since = "5.7")
    public abstract static class Builder<E, C>
    {
        /**
         * @param name the column name
         * @return {@code this} (for chaining)
         */
        @Nonnull
        public abstract Builder<E, C> name(@Nonnull String name);

        /**
         * @param editable {@code true} if the column is editable, otherwise {@code false}. Default is {@code false}.
         * @return {@code this} (for chaining)
         */
        @Nonnull
        public abstract Builder<E, C> editable(boolean editable);

        /**
         * @param type the type of values that this column contains. Default is {@link Object}.
         * @return {@code this} (for chaining)
         */
        @Nonnull
        public abstract Builder<E, C> type(@Nonnull Class<?> type);

        /**
         * @param value the function that maps entities to values for this column. Default returns the entity itself.
         * @return {@code this} (for chaining)
         */
        @Nonnull
        public abstract Builder<E, C> value(@Nonnull Function<E, C> value);

        /**
         * @param sortValue a function to convert to a sortable value
         * @return {@code this} (for chaining)
         */
        @Nonnull
        public abstract Builder<E, C> sortValue(Function<E, Comparable<?>> sortValue);

        /**
         * @param comparator the comparator to use when sorting on this column
         * @return {@code this} (for chaining)
         */
        @Nonnull
        public abstract Builder<E, C> comparator(@Nullable Comparator<C> comparator);

        /**
         * @param cached {@code true} if values of this column should be cached, otherwise {@code false}. Default is {@code false}.
         * @return {@code this} (for chaining)
         */
        @Nonnull
        public abstract Builder<E, C> cached(boolean cached);

        /**
         * @param sortable {@code true} to attempt to make the column sortable (does not guarantee sortability) or {@code false} to force it to be unsortable. Default is {@code true}.
         * @return {@code this} (for chaining)
         */
        @Nonnull
        public abstract Builder<E, C> sortable(boolean sortable);

        /**
         * @return the instance constructed using the properties set on this builder
         */
        @Nonnull
        public abstract EntityColumn<E, C> build();
    }
}
