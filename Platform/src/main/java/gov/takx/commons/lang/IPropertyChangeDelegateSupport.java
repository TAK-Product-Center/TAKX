package gov.takx.commons.lang;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;

/**
 * Interface that allows classes to cleanly get all the functionality of {@link IPropertyChangeListenerSupport} by
 * implementing only one method: {@link #getPropertyChangeListenerSupport()}. All calls are forwarded to the delegate. This
 * allows encapsulating listener support logic into a dedicated class (e.g.
 * {@link gov.takx.platform.utils.PrioritizedPropertyChangeSupport}, though any implementation will work) and returning an
 * instance as the delegate.
 * <p>Public APIs can expose interfaces that extend only {@link IPropertyChangeListenerSupport} and use this interface only
 * on their implementation classes to avoid exposing their internal {@link IPropertyChangeListenerSupport} instance.
 *
 * @since 5.7
 */
@FunctionalInterface
@IncubatingApi(since = "5.7")
public interface IPropertyChangeDelegateSupport extends IPropertyChangeListenerSupport
{
    /**
     * @return the listener support instance to delegate calls to
     */
    @Nonnull
    IPropertyChangeListenerSupport getPropertyChangeListenerSupport();

    @Override
    default void addPropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        getPropertyChangeListenerSupport().addPropertyChangeListener(listener);
    }

    @Override
    default void addPropertyChangeListener(@Nonnull String property, @Nonnull PropertyChangeListener listener)
    {
        getPropertyChangeListenerSupport().addPropertyChangeListener(property, listener);
    }

    @Override
    default void removePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        getPropertyChangeListenerSupport().removePropertyChangeListener(listener);
    }

    @Override
    default void removePropertyChangeListener(@Nonnull String property, @Nonnull PropertyChangeListener listener)
    {
        getPropertyChangeListenerSupport().removePropertyChangeListener(property, listener);
    }
}
