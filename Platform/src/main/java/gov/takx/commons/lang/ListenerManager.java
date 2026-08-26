package gov.takx.commons.lang;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class keeps track of property change listeners so that they can easily be removed without requiring the tedious
 * management of matching {@code removePropertyChangeListener(...)} calls.
 * <p> Rather than continue to propagate custom add/remove listener methods, classes that want to accept PCE
 * listeners should prefer using the {@link IPropertyChangeListenerSupport} interface, as it is already accommodated here.
 * <p>When cases are encountered that cannot use {@link IPropertyChangeListenerSupport}, the proper way to handle them here
 * is to either:
 * <ol>
 *     <li>Implement a new {@link ListenerRegistration}
 *  and add an additional {@code addPropertyChangeListener(...)} method that accepts the necessary type and instantiates the
 *  newly added registration implementation. Or ...</li>
 *  <li>Expose the {@link #register(ListenerRegistration)} method to allow direct additions of custom registration implementations.</li>
 *
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class ListenerManager
{
    @VisibleForTesting
    final List<ListenerRegistration> registrations = new CopyOnWriteArrayList<>();

    /**
     * Add and register a property change listener for all properties on an {@link IPropertyChangeListenerSupport} instance.
     *
     * @param object the object to listen on
     * @param listener the listener
     */
    public void addPropertyChangeListener(@Nonnull IPropertyChangeListenerSupport object, @Nonnull PropertyChangeListener listener)
    {
        register(new PropertyChangeSupportListenerRegistration(object, null, listener));
    }

    /**
     * Add and register a property change listener on an {@link IPropertyChangeListenerSupport} instance.
     * 
     * @param object the object to listen on
     * @param properties the List of properties, or {@code null} to listen on all
     * @param listener the listener
     */
    public void addPropertyChangeListener(@Nonnull IPropertyChangeListenerSupport object, @Nonnull List<String> properties, @Nonnull PropertyChangeListener listener)
    {
        register(new PropertyChangeSupportListenerRegistration(object, properties, listener));
    }

    /**
     * Unregister all {@link PropertyChangeListener} instances for a source object.
     *
     * @param object the source object to unregister listeners for
     */
    public void unregister(Object object)
    {
        var remove = new ArrayList<ListenerRegistration>();
        registrations.forEach(registration -> {
            if (registration.object() == object)
            {
                registration.unregister();
                remove.add(registration);
            }
        });
        registrations.removeAll(remove);
    }

    /**
     * Unregister all {@link PropertyChangeListener} instances.
     */
    public void unregisterAll()
    {
        List.copyOf(registrations).forEach(ListenerRegistration::unregister);
        registrations.clear();
    }

    /**
     * Add and register a {@link ListenerRegistration} instance.
     * 
     * @param registration the registration
     */
    public void register(@Nonnull ListenerRegistration registration)
    {
        registration.register();
        registrations.add(registration);
    }

    /**
     * Represents a listener registration for a specific object instance, with the ability to register and unregister. Exposed so that types that
     * do not implement {@link IPropertyChangeListenerSupport} can still take advantage of this manager.
     */
    public interface ListenerRegistration
    {
        /**
         * Remove property change listener from source object.
         */
        void unregister();

        /**
         * Add property change listener to source object.
         */
        void register();

        /**
         * @return the source object
         */
        @Nonnull
        Object object();
    }

    private record PropertyChangeSupportListenerRegistration(@Nonnull IPropertyChangeListenerSupport object, @Nullable List<String> properties, @Nonnull PropertyChangeListener listener) implements ListenerRegistration
    {
        @Override
        public void register()
        {
            if (properties == null)
            {
                object.addPropertyChangeListener(listener);
            } else
            {
                object.addPropertyChangeListener(properties, listener);
            }
        }

        @Override
        public void unregister()
        {
            if (properties == null)
            {
                object.addPropertyChangeListener(listener);
            } else
            {
                object.removePropertyChangeListener(properties, listener);
            }
        }
    }
}
