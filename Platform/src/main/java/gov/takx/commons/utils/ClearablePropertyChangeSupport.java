package gov.takx.commons.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.beans.PropertyChangeSupport;
import java.lang.invoke.MethodHandles;

/**
 * Extension of the standard {@link PropertyChangeSupport} to add a mechanism to remove all listeners.
 *
 * @since 1.26
 */
public class ClearablePropertyChangeSupport extends PropertyChangeSupport
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Constructs a {@code ClearablePropertyChangeSupport} object.
     *
     * @param sourceBean The bean to be given as the source for any events.
     */
    public ClearablePropertyChangeSupport(Object sourceBean)
    {
        super(sourceBean);
    }

    /**
     * Remove all currently registered properly change listeners.
     */
    public void removeAllListeners()
    {
        for (PropertyChangeListener listener : getPropertyChangeListeners())
        {
            if (listener instanceof PropertyChangeListenerProxy)
            {
                PropertyChangeListenerProxy proxy = (PropertyChangeListenerProxy) listener;

                if (logger.isDebugEnabled())
                {
                    logger.debug("removePCL({}, {})", proxy.getPropertyName(), proxy.getListener());
                }
                removePropertyChangeListener(proxy.getPropertyName(), listener);
            } else
            {
                logger.debug("removePCL({})", listener);
                removePropertyChangeListener(listener);
            }
        }
    }
}
