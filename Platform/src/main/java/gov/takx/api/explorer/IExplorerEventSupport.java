package gov.takx.api.explorer;

import gov.takx.commons.lang.IPropertyChangeListenerSupport;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;

/**
 * Interface for support of explorer-type event listeners.
 *
 * @since 1.48
 */
public interface IExplorerEventSupport extends IPropertyChangeListenerSupport
{
    /**
     * Add a listener to this object's property change events.
     *
     * @param listener listener to add.
     */
    void addPropertyChangeListener(@Nonnull PropertyChangeListener listener);

    /**
     * Add a listener for changes to a specific property of this object.
     *
     * @param property property of interest.
     * @param listener listener to add.
     */
    void addPropertyChangeListener(@Nonnull String property, @Nonnull PropertyChangeListener listener);

    /**
     * Remove a listener to this object's property change events.
     *
     * @param listener listener to remove.
     */
    void removePropertyChangeListener(@Nonnull PropertyChangeListener listener);

    /**
     * Remove a listener for changes to a specific property of this object.
     *
     * @param property property of interest.
     * @param listener listener to remove.
     */
    void removePropertyChangeListener(@Nonnull String property, @Nonnull PropertyChangeListener listener);
}
