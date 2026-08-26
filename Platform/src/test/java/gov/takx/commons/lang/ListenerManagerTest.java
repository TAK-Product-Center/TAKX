package gov.takx.commons.lang;

import gov.takx.platform.utils.PrioritizedPropertyChangeSupport;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import java.beans.PropertyChangeEvent;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ListenerManagerTest
{
    static String PROPERTY_A = "propa";
    static String PROPERTY_B = "propb";

    PropertyChangeEvent lastEvent;

    @Test
    public void test()
    {
        var listenerManager = new ListenerManager();
        assertThat(listenerManager.registrations).isEmpty();
        var entity = new PropertyChangeSupportTest();
        listenerManager.addPropertyChangeListener(entity, List.of(PROPERTY_A, PROPERTY_B), this::propertyChange);
        assertThat(listenerManager.registrations).hasSize(1);

        var oldValue = "old";
        var newValue = "new";
        entity.fire(PROPERTY_A, oldValue, newValue);
        assertThat(lastEvent).isNotNull();
        assertThat(lastEvent.getPropertyName()).isEqualTo(PROPERTY_A);
        assertThat(lastEvent.getOldValue()).isEqualTo(oldValue);
        assertThat(lastEvent.getNewValue()).isEqualTo(newValue);

        oldValue += "1";
        newValue += "1";
        entity.fire(PROPERTY_B, oldValue, newValue);
        assertThat(lastEvent.getOldValue()).isEqualTo(oldValue);
        assertThat(lastEvent.getNewValue()).isEqualTo(newValue);

        listenerManager.unregisterAll();
        assertThat(listenerManager.registrations).isEmpty();
        lastEvent = null;
        entity.fire(PROPERTY_B, oldValue, newValue);
        assertThat(lastEvent).isNull();
    }

    private void propertyChange(PropertyChangeEvent propertyChangeEvent)
    {
        lastEvent = propertyChangeEvent;
    }

    private static class PropertyChangeSupportTest implements IPropertyChangeDelegateSupport
    {
        PrioritizedPropertyChangeSupport propertyChangeSupport = new PrioritizedPropertyChangeSupport(this);

        @Nonnull
        @Override
        public IPropertyChangeListenerSupport getPropertyChangeListenerSupport()
        {
            return propertyChangeSupport;
        }

        public void fire(String property, String oldValue, String newValue)
        {
            propertyChangeSupport.firePropertyChange(property, oldValue, newValue);
        }
    }
}
