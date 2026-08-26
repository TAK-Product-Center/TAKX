package gov.takx.commons.lang;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.platform.utils.PrioritizedPropertyChangeSupport;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;

/**
 * Interface that allows classes to cleanly get all the functionality of {@link IPropertyChangeListenerSupport} by
 * implementing only the single method {@link #getPropertyChangeSupport()}. Most apis should expose interfaces that extend
 * only {@link IPropertyChangeListenerSupport} and use this interface only on their implementation classes to avoid exposing
 * {@link #getPropertyChangeSupport()} publicly.
 *
 * @since 5.6
 * @deprecated Use {@link IPropertyChangeDelegateSupport} which supports delegating to any instance of
 * {@link IPropertyChangeListenerSupport}
 */
@Deprecated(since = "5.7", forRemoval = true)
@DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
public interface IPropertyChangeSupport extends IPropertyChangeListenerSupport
{
    @Nonnull
    PrioritizedPropertyChangeSupport getPropertyChangeSupport();

    @Override
    default void addPropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        getPropertyChangeSupport().addPropertyChangeListener(listener);
    }

    @Override
    default void addPropertyChangeListener(@Nonnull String property, @Nonnull PropertyChangeListener listener)
    {
        getPropertyChangeSupport().addPropertyChangeListener(property, listener);
    }

    @Override
    default void removePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        getPropertyChangeSupport().removePropertyChangeListener(listener);
    }

    @Override
    default void removePropertyChangeListener(@Nonnull String property, @Nonnull PropertyChangeListener listener)
    {
        getPropertyChangeSupport().removePropertyChangeListener(property, listener);
    }
}
