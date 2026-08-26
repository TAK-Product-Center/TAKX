package gov.takx.platform.utils;

import gov.takx.commons.lang.IPropertyChangeListenerSupport;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.interceptor.Interceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.beans.PropertyChangeSupport;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Specialized form of {@link PropertyChangeSupport} to support listeners that declare a priority using
 * the {@link Priority} annotation.
 * <p>
 * The implementation uses a navigable map to order a collection of {@link PropertyChangeSupport} instances,
 * each responsible for the listeners at a given priority value.  Ordering is applied according to
 * {@link Interceptor.Priority}, meaning listeners with lower priority values are called first.
 *
 * @see Interceptor.Priority
 * @since 2.38
 */
public class PrioritizedPropertyChangeSupport implements IPropertyChangeListenerSupport
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int DEFAULT_PRIORITY = Interceptor.Priority.APPLICATION;
    private final Object source;

    // Map from listener class to registered priority.
    // NOTE: This is static since the data is derived from the compiled code, and can never change at runtime
    private static final Map<Class<?>, Integer> listenerPriority = new ConcurrentHashMap<>();

    // Ordered map from priority to PCS
    private final Map<Integer, PropertyChangeSupport> orderedSupport = new ConcurrentSkipListMap<>();

    /**
     * Constructor.
     *
     * @param source The bean to be given as the source for any events.
     */
    public PrioritizedPropertyChangeSupport(Object source)
    {
        this.source = source;
    }

    /**
     * @see PropertyChangeSupport#addPropertyChangeListener(PropertyChangeListener)
     */
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        getPropertyChangeSupportForListener(listener).addPropertyChangeListener(listener);
    }

    /**
     * @see PropertyChangeSupport#removePropertyChangeListener(PropertyChangeListener)
     */
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        getPropertyChangeSupportForListener(listener).removePropertyChangeListener(listener);
    }

    /**
     * @see PropertyChangeSupport#getPropertyChangeListeners()
     */
    public PropertyChangeListener[] getPropertyChangeListeners()
    {
        return orderedSupport.values().stream()
                .flatMap(pcs -> Stream.of(pcs.getPropertyChangeListeners()))
                .toArray(PropertyChangeListener[]::new);
    }

    /**
     * @see PropertyChangeSupport#addPropertyChangeListener(String, PropertyChangeListener)
     */
    public void addPropertyChangeListener(@Nonnull String propertyName, @Nonnull PropertyChangeListener listener)
    {
        getPropertyChangeSupportForListener(listener).addPropertyChangeListener(propertyName, listener);
    }

    /**
     * @see PropertyChangeSupport#removePropertyChangeListener(String, PropertyChangeListener)
     */
    public void removePropertyChangeListener(@Nonnull String propertyName, @Nonnull PropertyChangeListener listener)
    {
        getPropertyChangeSupportForListener(listener).removePropertyChangeListener(propertyName, listener);
    }

    /**
     * @see PropertyChangeSupport#getPropertyChangeListeners(String)
     */
    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName)
    {
        return orderedSupport.values().stream()
                .flatMap(pcs -> Stream.of(pcs.getPropertyChangeListeners(propertyName)))
                .toArray(PropertyChangeListener[]::new);
    }

    /**
     * @see PropertyChangeSupport#firePropertyChange(String, Object, Object)
     */
    public void firePropertyChange(String propertyName, Object oldValue, Object newValue)
    {
        applyInOrder(pcs -> pcs.firePropertyChange(propertyName, oldValue, newValue));
    }

    /**
     * @see PropertyChangeSupport#firePropertyChange(String, int, int)
     */
    public void firePropertyChange(String propertyName, int oldValue, int newValue)
    {
        applyInOrder(pcs -> pcs.firePropertyChange(propertyName, oldValue, newValue));
    }

    /**
     * @see PropertyChangeSupport#firePropertyChange(String, boolean, boolean)
     */
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue)
    {
        applyInOrder(pcs -> pcs.firePropertyChange(propertyName, oldValue, newValue));
    }

    /**
     * @see PropertyChangeSupport#firePropertyChange(PropertyChangeEvent)
     */
    public void firePropertyChange(PropertyChangeEvent event)
    {
        applyInOrder(pcs -> pcs.firePropertyChange(event));
    }

    /**
     * Fires a new {@link PropertyChangeEvent} using the same property, old value, and value, as the given event, but using
     * this classes internal source value.
     * 
     * @param event the event to copy
     * @see PropertyChangeSupport#firePropertyChange(PropertyChangeEvent)
     * @since 5.7
     */
    public void firePropertyChangeCopy(@Nonnull PropertyChangeEvent event)
    {
        var newEvent = new PropertyChangeEvent(source, event.getPropertyName(), event.getOldValue(), event.getNewValue());
        applyInOrder(pcs -> pcs.firePropertyChange(newEvent));
    }

    /**
     * Compare the given values and, if different, apply the given updater and fire a property change event with the
     * given property name.
     *
     * @param property Name of property
     * @param oldValue Current value
     * @param newValue New value
     * @param updater Action to execute to update the property value on the caller
     * @since 3.3
     */
    public void updateAndFire(String property, Object oldValue, Object newValue, Runnable updater)
    {
        if (!Objects.equals(oldValue, newValue))
        {
            updater.run();
            applyInOrder(pcs -> pcs.firePropertyChange(property, oldValue, newValue));
        }
    }

    /**
     * Compare the given values and, if different, apply the given updater and fire a property change event with the
     * given property name.
     *
     * @param property Name of property
     * @param oldValue Current value
     * @param newValue New value
     * @param updater Action to execute to update the property value on the caller
     * @since 3.3
     */
    public void updateAndFire(String property, Object[] oldValue, Object[] newValue, Runnable updater)
    {
        if (!Arrays.equals(oldValue, newValue))
        {
            updater.run();
            applyInOrder(pcs -> pcs.firePropertyChange(property, oldValue, newValue));
        }
    }

    /**
     * Compare the given values and, if different, apply the given updater and fire a property change event with the
     * given property name.
     *
     * @param property Name of property
     * @param oldValue Current value
     * @param newValue New value
     * @param updater Action to execute to update the property value on the caller
     * @since 3.3
     */
    public void updateAndFire(String property, double[] oldValue, double[] newValue, Runnable updater)
    {
        if (!Arrays.equals(oldValue, newValue))
        {
            updater.run();
            applyInOrder(pcs -> pcs.firePropertyChange(property, oldValue, newValue));
        }
    }

    /**
     * Compare the given values and, if different, apply the given updater and fire a property change event with the
     * given property name.
     *
     * @param property Name of property
     * @param oldValue Current value
     * @param newValue New value
     * @param updater Action to execute to update the property value on the caller
     * @since 3.3
     */
    public void updateAndFire(String property, boolean oldValue, boolean newValue, Runnable updater)
    {
        if (oldValue != newValue)
        {
            updater.run();
            applyInOrder(pcs -> pcs.firePropertyChange(property, oldValue, newValue));
        }
    }

    /**
     * Compare the given values and, if different, apply the given updater and fire a property change event with the
     * given property name.
     *
     * @param property Name of property
     * @param oldValue Current value
     * @param newValue New value
     * @param updater Action to execute to update the property value on the caller
     * @since 3.3
     */
    public void updateAndFire(String property, int oldValue, int newValue, Runnable updater)
    {
        if (oldValue != newValue)
        {
            updater.run();
            applyInOrder(pcs -> pcs.firePropertyChange(property, oldValue, newValue));
        }
    }

    /**
     * Compare the given values and, if different, apply the given updater and fire a property change event with the
     * given property name.
     *
     * @param property Name of property
     * @param oldValue Current value
     * @param newValue New value
     * @param updater Action to execute to update the property value on the caller
     * @since 3.3
     */
    public void updateAndFire(String property, long oldValue, long newValue, Runnable updater)
    {
        if (oldValue != newValue)
        {
            updater.run();
            applyInOrder(pcs -> pcs.firePropertyChange(property, oldValue, newValue));
        }
    }

    /**
     * Compare the given values and, if different, apply the given updater and fire a property change event with the
     * given property name.
     *
     * @param property Name of property
     * @param oldValue Current value
     * @param newValue New value
     * @param updater Action to execute to update the property value on the caller
     * @since 3.3
     */
    public void updateAndFire(String property, double oldValue, double newValue, Runnable updater)
    {
        if (Double.compare(oldValue, newValue) != 0)
        {
            updater.run();
            applyInOrder(pcs -> pcs.firePropertyChange(property, oldValue, newValue));
        }
    }

    /**
     * Compare the given values and, if different, apply the given updater and fire a property change event with the
     * given property name.
     *
     * @param property Name of property
     * @param oldValue Current value
     * @param newValue New value
     * @param updater Action to execute to update the property value on the caller
     * @since 3.4.1
     */
    public void updateAndFire(String property, float oldValue, float newValue, Runnable updater)
    {
        if (Float.compare(oldValue, newValue) != 0)
        {
            updater.run();
            applyInOrder(pcs -> pcs.firePropertyChange(property, oldValue, newValue));
        }
    }

    /**
     * @see PropertyChangeSupport#fireIndexedPropertyChange(String, int, Object, Object)
     */
    public void fireIndexedPropertyChange(String propertyName, int index, Object oldValue, Object newValue)
    {
        applyInOrder(pcs -> pcs.fireIndexedPropertyChange(propertyName, index, oldValue, newValue));
    }

    /**
     * @see PropertyChangeSupport#fireIndexedPropertyChange(String, int, int, int)
     */
    public void fireIndexedPropertyChange(String propertyName, int index, int oldValue, int newValue)
    {
        applyInOrder(pcs -> pcs.fireIndexedPropertyChange(propertyName, index, oldValue, newValue));
    }

    /**
     * @see PropertyChangeSupport#fireIndexedPropertyChange(String, int, boolean, boolean)
     */
    public void fireIndexedPropertyChange(String propertyName, int index, boolean oldValue, boolean newValue)
    {
        applyInOrder(pcs -> pcs.fireIndexedPropertyChange(propertyName, index, oldValue, newValue));
    }

    /**
     * @see PropertyChangeSupport#hasListeners(String)
     */
    public boolean hasListeners(String propertyName)
    {
        return orderedSupport.values().stream().anyMatch(pcs -> pcs.hasListeners(propertyName));
    }

    /**
     * Invoke the given consumer on each PCS in ascending priority order.
     *
     * @param action Consumer to invoke
     */
    private void applyInOrder(Consumer<PropertyChangeSupport> action)
    {
        orderedSupport.values().forEach(action);
    }

    /**
     * Get or create the PCS responsible for the priority of the given listener.
     *
     * @param listener Listener from which to obtain the priority for which to get/create the PCS
     * @return PCS responsible for the priority of the given listener
     * @see #getPriorityFromListener
     */
    private PropertyChangeSupport getPropertyChangeSupportForListener(PropertyChangeListener listener)
    {
        final Integer priority = listenerPriority.computeIfAbsent(listener.getClass(), this::getPriorityFromListener);
        return orderedSupport.computeIfAbsent(priority, k -> new PropertyChangeSupport(source));
    }

    /**
     * Find the {@link Priority} annotation on the given listener class and return the priority value.
     * First, this looks for the annotation on the {@link PropertyChangeListener#propertyChange(PropertyChangeEvent)}
     * method parameter, and if not found there, on the class itself.
     * <p>
     * If the annotation is not present in either location, then return the default priority.
     *
     * @param listenerClass Class of listener to process
     * @return Priority from annotation, or default
     * @see Class#getAnnotation(java.lang.Class)
     * @see #DEFAULT_PRIORITY
     */
    private int getPriorityFromListener(Class<?> listenerClass)
    {
        return getPriorityFromMethod(listenerClass)
                .orElseGet(() -> Optional.ofNullable(listenerClass.getAnnotation(Priority.class))
                        .map(Priority::value).orElse(DEFAULT_PRIORITY));
    }

    /**
     * Find the {@link Priority} annotation on the 1st parameter of the {@code propertyChange} method,
     * and return its value if present, empty otherwise.
     * @since 3.3
     */
    private Optional<Integer> getPriorityFromMethod(Class<?> listenerClass)
    {
        try
        {
            final Method propertyChange = listenerClass.getMethod("propertyChange", PropertyChangeEvent.class);

            for (Annotation annotation : propertyChange.getParameterAnnotations()[0])
            {
                if (annotation instanceof Priority priority) return Optional.of(priority.value());
            }
        } catch (NoSuchMethodException e)
        {
            logger.error("No 'propertyChange' method found on {}", listenerClass);
        }

        return Optional.empty();
    }

    /**
     * Remove all currently registered properly change listeners.
     */
    public void removeAllListeners()
    {
        orderedSupport.values().forEach(this::removeAllListeners);
        orderedSupport.clear();
    }

    /**
     * Remove all listeners from the given PCS.
     *
     * @param pcs Property change support to process
     */
    private void removeAllListeners(PropertyChangeSupport pcs)
    {
        for (PropertyChangeListener listener : pcs.getPropertyChangeListeners())
        {
            if (listener instanceof PropertyChangeListenerProxy proxy)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("removePCL({}, {})", proxy.getPropertyName(), proxy.getListener());
                }
                pcs.removePropertyChangeListener(proxy.getPropertyName(), listener);
            } else
            {
                logger.debug("removePCL({})", listener);
                pcs.removePropertyChangeListener(listener);
            }
        }
    }
}
