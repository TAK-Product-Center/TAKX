package gov.takx.commons.lang;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Util methods relating to objects, similar to {@link Objects}, but throwing more meaningful exceptions and with more robust
 * ways to specify messages.
 *
 * @since 5.6
 */
public class ObjectUtils
{
    /**
     * Throw the exception from the given supplier when the object reference is {@code null}, otherwise return the object.
     * 
     * @param object the object to check for {@code null}
     * @param exceptionSupplier the supplier for the exception to be thrown when the object is {@code null}
     * @return the object
     * @param <T> the object type
     * @param <E> the exception type
     * @throws E the exception created by the supplier
     */
    public static <T, E extends Exception> T requireNonNull(@Nullable T object, @Nonnull Supplier<E> exceptionSupplier) throws E
    {
        if (object == null) throw exceptionSupplier.get();
        return object;
    }

    /**
     * Throw an {@link IllegalStateException} with the message from the given supplier when the object reference is {@code null}, 
     * otherwise return the object.
     * 
     * @param object the object to check for {@code null}
     * @param exceptionMessageSupplier the supplier for the exception message
     * @return the object
     * @param <T> the object type
     * @throws IllegalStateException when the object is {@code null}
     */
    public static <T> T requireNonNullState(@Nullable T object, @Nonnull Supplier<String> exceptionMessageSupplier) throws IllegalStateException
    {
        return requireNonNull(object, () -> new IllegalStateException(exceptionMessageSupplier.get()));
    }

    /**
     * Throw an {@link IllegalStateException} with the given message when the object reference is {@code null}, otherwise 
     * return the object.
     * 
     * @param object the object to check for {@code null}
     * @param exceptionMessage the exception message
     * @return the object
     * @param <T> the object type
     * @throws IllegalStateException when the object is {@code null}
     */
    @SuppressWarnings("unused")
    public static <T> T requireNonNullState(@Nullable T object, @Nonnull String exceptionMessage) throws IllegalStateException
    {
        return requireNonNullState(object, () -> exceptionMessage);
    }
}
