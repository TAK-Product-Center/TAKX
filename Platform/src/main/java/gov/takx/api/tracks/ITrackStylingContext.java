package gov.takx.api.tracks;

import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * The Track Styling Context holds the inheritance hierarchy for applying Track Style Definitions to an RDM.
 * The inheritance hierarchy is RDM -&gt; MapObject -&gt; Global.  The methods here will "reach back" to the
 * parent to obtain an attribute value.
 * <p/>
 * Individual attributes are served up from the track style definition hierarchy. In other words, the track style
 * definition at the RDM level might only define Track Shape and Track Shape Color, deferring all the other
 * track style attributes to its immediate parent. Thus, when calling {@link #getTrackShape()} the caller really
 * has no notion of which track style definition explicitly assigned the Track Shape- it could have been the RDM, the
 * Map Object, or the global track style definition.
 * <p/>
 * <b>Note:</b>Plugin developers should never create instances of this interface or its associated implementations.
 * The TAKX core is responsible for properly creating instances with the appropriate parent settings.
 *
 * @since 1.12
 */
public interface ITrackStylingContext extends ITrackStyle
{
    String TRACK_STYLE_DEFINITION = "trackStyleDefinition";

    /**
     * Identifies the parent track style context deferred to for styling attribute values when this style definition
     * at this level explicit does not define a value for the attribute.
     *
     * @return parent, or {@code null} if no parent is defined
     */
    ITrackStylingContext getParent();

    /**
     * Get the direct (as opposed to parent) TSD being managed by the styling context.
     *
     * @return the TSD
     */
    ITrackStyleDefinition getTrackStyleDefinition();

    // Property Change Listener Management

    /**
     * Add property change listeners for the named list of properties.  The list of supported properties comes
     * from {@link ITrackStyleDefinition}.
     *
     * @param listener   Listener to add
     * @param properties List of properties to listen for
     * @throws IllegalArgumentException When the {@code properties} list is empty.
     */
    void addPropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull List<String> properties);

    /**
     * Add property change listeners for the named property.  The list of supported properties comes
     * from {@link ITrackStyleDefinition}.
     *
     * @param listener Listener to add
     * @param property Property to listen for
     */
    void addPropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull String property);

    /**
     * Add a property change listener on ALL properties of this object.  To unregister, use
     * {@link #removePropertyChangeListener(java.beans.PropertyChangeListener)}.
     *
     * @param listener Listener to add
     * @see java.beans.PropertyChangeSupport#addPropertyChangeListener(java.beans.PropertyChangeListener)
     */
    void addPropertyChangeListener(@Nonnull PropertyChangeListener listener);

    /**
     * Remove the listener from the specified properties.
     *
     * @param listener   Listener to remove
     * @param properties list of properties to unregister
     * @throws IllegalArgumentException When the {@code properties} list is empty.
     * @see java.beans.PropertyChangeSupport#removePropertyChangeListener(String, java.beans.PropertyChangeListener)
     */
    void removePropertyChangeListener(@Nonnull PropertyChangeListener listener, List<String> properties);

    /**
     * Remove the listener from the specified property.
     *
     * @param listener Listener to remove
     * @param property Property to unregister
     * @see java.beans.PropertyChangeSupport#removePropertyChangeListener(String, java.beans.PropertyChangeListener)
     */
    void removePropertyChangeListener(@Nonnull PropertyChangeListener listener, String property);

    /**
     * Remove the listener. This method can only be used if the listener was originally registered using
     * {@link #addPropertyChangeListener(java.beans.PropertyChangeListener)}
     *
     * @param listener Listener to remove
     * @see java.beans.PropertyChangeSupport#removePropertyChangeListener(java.beans.PropertyChangeListener)
     */
    void removePropertyChangeListener(@Nonnull PropertyChangeListener listener);
}
