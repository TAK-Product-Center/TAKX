package gov.takx.api.annotation;

import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.test.AContainerTest;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 2.0.0
 */
@PseudoBean
public class PseudoBeanTest extends AContainerTest
{
    @Inject
    private IProgramEnvironment environment;

    @Inject
    @VFSManagerScope(VFSScope.Global)
    private IVirtualFileSystemManager globalVFSManager;

    @Inject
    Event<MyEvent> myEvent;

    @Test
    public void testInjectionsOnTestClass()
    {
        assertThat(environment).isNotNull();
        assertThat(environment.isHeadless()).isTrue();
        assertThat(globalVFSManager).isNotNull();
    }

    /**
     * In this test, an open or close event generator fires an
     * event with different event type. Each correctly resolves to
     * the correct observer method
     */
    @Test
    public void testObserversWithoutQualifiers()
    {
        // register these with the bean manager
        final CloseObserver closeObserver = new CloseObserver();
        final OpenObserver openObserver = new OpenObserver();

        DoorEventGenerator generator = new DoorEventGenerator();

        generator.openGenerator.fire(new OpenDoorEvent());
        assertThat(openObserver.invokedCount).isEqualTo(1);

        generator.closeGenerator.fire(new CloseDoorEvent());
        assertThat(closeObserver.invokedCount).isEqualTo(1);
    }

    /**
     * Fire open and close door events and ensure the qualified observers are notified.
     */
    @Test
    public void testObserversWithQualifiers()
    {
        // register these with the bean manager
        final CloseObserverWithQualifier closeObserver = new CloseObserverWithQualifier();
        final OpenObserverWithQualifier openObserver = new OpenObserverWithQualifier();

        DoorEventGeneratorWithQualifier generator = new DoorEventGeneratorWithQualifier();

        generator.openGenerator.fire(new OpenDoorEvent());
        assertThat(openObserver.invokedCount).isEqualTo(1);

        generator.closeGenerator.fire(new CloseDoorEvent());
        assertThat(closeObserver.invokedCount).isEqualTo(1);
    }

    @Test
    public void testSingletonObserver()
    {
        DoorEventGenerator generator = new DoorEventGenerator();

        final SingletonObserver so1 = new SingletonObserver();

        generator.openGenerator.fire(new OpenDoorEvent());
        assertThat(so1.invokedCount).isEqualTo(1);

        final SingletonObserver so2 = new SingletonObserver();

        generator.openGenerator.fire(new OpenDoorEvent());
        assertThat(so1.invokedCount).isEqualTo(1);
        assertThat(so2.invokedCount).isEqualTo(1);

        generator.openGenerator.fire(new OpenDoorEvent());
        assertThat(so1.invokedCount).isEqualTo(1);
        assertThat(so2.invokedCount).isEqualTo(2);
    }

    @Test
    public void testMultipleObserverInstances()
    {
        final DoorEventGenerator generator = new DoorEventGenerator();
        final CloseDoorEvent closeDoorEvent = new CloseDoorEvent();

        final RemovableCloseObserver observer1 = new RemovableCloseObserver();
        final RemovableCloseObserver observer2 = new RemovableCloseObserver();
        final RemovableCloseObserver observer3 = new RemovableCloseObserver();

        generator.closeGenerator.fire(closeDoorEvent);
        assertThat(observer1.invokedCount).isEqualTo(1);
        assertThat(observer2.invokedCount).isEqualTo(1);
        assertThat(observer3.invokedCount).isEqualTo(1);

        observer2.cleanup();

        generator.closeGenerator.fire(closeDoorEvent);
        assertThat(observer1.invokedCount).isEqualTo(2);
        assertThat(observer2.invokedCount).isEqualTo(1);
        assertThat(observer3.invokedCount).isEqualTo(2);
    }

    @Test
    public void testDeserialization() throws IOException, ClassNotFoundException
    {
        final SerializablePseudoBean thing = new SerializablePseudoBean();
        thing.foo = 42;

        assertThat(SerializablePseudoBean.pcCount).isEqualTo(1);
        assertThat(thing.programEnvironment).isNotNull();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(thing);
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));

        final SerializablePseudoBean reifiedThing = (SerializablePseudoBean) ois.readObject();
        ois.close();

        assertThat(SerializablePseudoBean.pcCount).isEqualTo(2);
        assertThat(reifiedThing.foo).isEqualTo(42);
        assertThat(reifiedThing.programEnvironment).isNotNull();
    }

    @Test
    public void testDeserializationWithReadResolve() throws IOException, ClassNotFoundException
    {
        final SerializablePseudoBeanReadResolve thing = new SerializablePseudoBeanReadResolve();
        thing.foo = 42;

        assertThat(SerializablePseudoBeanReadResolve.pcCount).isEqualTo(1);
        assertThat(thing.programEnvironment).isNotNull();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(thing);
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));

        final SerializablePseudoBeanReadResolve reifiedThing = (SerializablePseudoBeanReadResolve) ois.readObject();
        ois.close();

        assertThat(SerializablePseudoBeanReadResolve.pcCount).isEqualTo(2);
        assertThat(reifiedThing.foo).isEqualTo(42);
        assertThat(reifiedThing.programEnvironment).isNotNull();
    }

    /**
     * Verify that when a method annotated with @RemoveObserver, that observation callbacks no longer happen.
     *
     * @since 2.37
     */
    @Test
    public void testRemoveObserverAnnotation()
    {
        final RemovableCloseObserver observer = new RemovableCloseObserver();
        assertThat(observer.invokedCount).isEqualTo(0);

        final DoorEventGenerator generator = new DoorEventGenerator();

        generator.closeGenerator.fire(new CloseDoorEvent());
        assertThat(observer.invokedCount).isEqualTo(1);

        // Call cleanup to remove the observer, and ensure invocation count no longer updates
        observer.cleanup();
        generator.closeGenerator.fire(new CloseDoorEvent());
        assertThat(observer.invokedCount).isEqualTo(1);
    }

    /**
     * Verify that newly created instances are properly notified of events.
     * @since 5.2
     */
    @Test
    void newInstances_observe()
    {
        final var observer1 = new OrderedObserver1();

        myEvent.fire(new MyEvent());

        assertThat(observer1.count).isEqualTo(1);

        final var observer2 = new OrderedObserver1();

        myEvent.fire(new MyEvent());

        assertThat(observer1.count).isEqualTo(2);
        assertThat(observer2.count).isEqualTo(1);

        final var observer3 = new OrderedObserver1();

        myEvent.fire(new MyEvent());

        assertThat(observer1.count).isEqualTo(3);
        assertThat(observer2.count).isEqualTo(2);
        assertThat(observer3.count).isEqualTo(1);

        observer1.cleanup();
        observer2.cleanup();
        observer3.cleanup();
    }

    /**
     * Verify that PseudoBean observer methods using @Priority are notified in the proper order.
     * @since 5.2
     */
    @Test
    void observersWithPriority_notifiedInOrder()
    {
        AOrderedObserver.orderCounter = 1;

        // Construct in opposite order of priority
        final var observer3 = new OrderedObserver3();
        final var observer2 = new OrderedObserver2();
        final var observer1 = new OrderedObserver1();

        myEvent.fire(new MyEvent());

        assertThat(observer1.count).isEqualTo(1);
        assertThat(observer2.count).isEqualTo(1);
        assertThat(observer3.count).isEqualTo(1);

        assertThat(observer1.order).isEqualTo(1);
        assertThat(observer2.order).isEqualTo(2);
        assertThat(observer3.order).isEqualTo(3);

        observer1.order = observer2.order = observer3.order = 0;
        AOrderedObserver.orderCounter = 1;

        // And a marginal check that ordering is consistent across events
        myEvent.fire(new MyEvent());

        assertThat(observer1.count).isEqualTo(2);
        assertThat(observer2.count).isEqualTo(2);
        assertThat(observer3.count).isEqualTo(2);

        assertThat(observer1.order).isEqualTo(1);
        assertThat(observer2.order).isEqualTo(2);
        assertThat(observer3.order).isEqualTo(3);

        observer1.cleanup();
        observer2.cleanup();
        observer3.cleanup();
    }

    static class DoorEvent
    {
        String state;

        DoorEvent(String state)
        {
            this.state = state;
        }
    }

    static class OpenDoorEvent extends DoorEvent
    {
        OpenDoorEvent()
        {
            super("OPENED");
        }
    }

    static class CloseDoorEvent extends DoorEvent
    {
        CloseDoorEvent()
        {
            super("CLOSED");
        }
    }

    @PseudoBean
    static class DoorEventGenerator
    {
        @Inject
        Event<OpenDoorEvent> openGenerator;

        @Inject
        Event<CloseDoorEvent> closeGenerator;
    }

    @PseudoBean
    static class DoorEventGeneratorWithQualifier
    {
        @Inject
        @OpenQualifier
        Event<DoorEvent> openGenerator;

        @Inject
        @CloseQualifier
        Event<DoorEvent> closeGenerator;
    }

    static class CountingObserver
    {
        int invokedCount = 0;

        void invoked()
        {
            invokedCount += 1;
        }
    }

    /**
     * Class used to verify that singletonObserver is working properly.
     */
    @PseudoBean(singletonObserver = true)
    static class SingletonObserver extends CountingObserver
    {
        @SuppressWarnings("unused")
        private void doorOpened(@Observes OpenDoorEvent ignored)
        {
            invoked();
        }
    }

    @PseudoBean
    static class OpenObserver extends CountingObserver
    {
        @SuppressWarnings("unused")
        private void doorOpened(@Observes OpenDoorEvent event)
        {
            assertThat("OPENED").isEqualTo(event.state);
            if ("OPENED".equals(event.state)) invoked();
        }
    }

    @PseudoBean
    static class OpenObserverWithQualifier extends CountingObserver
    {
        @SuppressWarnings("unused")
        private void doorOpened(@Observes @OpenQualifier DoorEvent event)
        {
            assertThat("OPENED").isEqualTo(event.state);

            if ("OPENED".equals(event.state))
            {
                invoked();
            }
        }
    }

    @PseudoBean
    static class CloseObserver extends CountingObserver
    {
        @SuppressWarnings("unused")
        private void doorClosed(@Observes CloseDoorEvent event)
        {
            assertThat("CLOSED").isEqualTo(event.state);

            if ("CLOSED".equals(event.state))
            {
                invoked();
            }
        }
    }

    @PseudoBean
    static class CloseObserverWithQualifier extends CountingObserver
    {
        @SuppressWarnings("unused")
        private void doorClosed(@Observes @CloseQualifier DoorEvent event)
        {
            assertThat("CLOSED").isEqualTo(event.state);

            if ("CLOSED".equals(event.state))
            {
                invoked();
            }
        }
    }

    @PseudoBean
    static class RemovableCloseObserver extends CountingObserver
    {
        @SuppressWarnings("unused")
        private void doorClosed(@Observes CloseDoorEvent event)
        {
            assertThat("CLOSED").isEqualTo(event.state);

            if ("CLOSED".equals(event.state))
            {
                invoked();
            }
        }

        @SuppressWarnings("unused")
        @RemoveObserver
        void cleanup()
        {
            // Nothing to do
        }
    }

    @PseudoBean
    private static class OrderedObserver1 extends AOrderedObserver
    {
        @SuppressWarnings("unused")
        private void observe(@Observes @Priority(1) MyEvent event)
        {
            observed();
        }
    }

    @PseudoBean
    private static class OrderedObserver2 extends AOrderedObserver
    {
        @SuppressWarnings("unused")
        private void observe(@Observes @Priority(2) MyEvent event)
        {
            observed();
        }
    }

    @PseudoBean
    private static class OrderedObserver3 extends AOrderedObserver
    {
        @SuppressWarnings("unused")
        private void observe(@Observes @Priority(3) MyEvent event)
        {
            observed();
        }
    }

    private abstract static class AOrderedObserver
    {
        static int instanceCounter = 1;
        static int orderCounter = 1;

        final int instance;

        int count = 0;
        int order = 0;

        AOrderedObserver()
        {
            instance = instanceCounter++;
        }

        void observed()
        {
            count++;
            order = orderCounter++;
        }

        @RemoveObserver
        void cleanup()
        {
        }

        @Override
        public String toString()
        {
            return getClass().getSimpleName() + "{" +
                    "instance=" + instance +
                    ", count=" + count +
                    ", order=" + order +
                    '}';
        }
    }

    private static class MyEvent
    {
    }
}
