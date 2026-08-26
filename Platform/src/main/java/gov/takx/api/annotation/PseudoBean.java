package gov.takx.api.annotation;

import jakarta.enterprise.event.Observes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tag a type as participating in CDI operations (like injection and event observation), but is NOT a managed bean.
 * This means that the CDI container has no knowledge of this type, and it can not be injected into other managed
 * beans or other types tagged with {@code @PseudoBean}.
 * <p/>
 * The pseudo-CDI handling of types tagged with this annotation comes via aspect oriented programming (AOP) magic.
 * It wraps the constructor for a tagged type, and processes injection at the end of that constructor.  This means
 * a couple of things:
 * <ul>
 *     <li>Injected values can not be referenced within the constructor.  If you need to use them as part of the
 *     construction process, you will need to add an {@code @PostConstruct} annotated method.  The injected values will
 *     be available there.  To be clear, you don't need to use an {@code @PostConstruct} method if you don't need to
 *     reference any injected value in the constructor.  Using them in any other method after construction will
 *     work fine.</li>
 *     <li>You cannot use constructor injection.  In most respects, tagged types are just normal classes.  Their
 *     lifecycle is controlled by application logic, and not by the CDI container.</li>
 * </ul>
 * Here's an example usage:
 * <pre>
 * {@code
 * @PseudoBean
 * public class SomeClass
 * {
 *     @Inject
 *     private IGlobeManager globeManager;
 *
 *     private final int value;
 *
 *     public SomeClass(int value)
 *     {
 *         this.value = value;
 *     }
 *
 *     @PostConstruct
 *     private void postConstruct()
 *     {
 *         globeManager.doWhenGlobeReady(globe -> doSomethingWithGlobe());
 *     }
 * }
 * }
 * </pre>
 * PseudoBeans can participate in event observation using the standard CDI {@link Observes} annotation on a method
 * parameter.  However, since the CDI container is not managing the lifecycle, you must take care to unregister the
 * event listener when the lifecycle of a given object instance ends.  This is typically handled in some form of
 * "cleanup" method.  The simplest way to do this is to annotate the method with {@link RemoveObserver}.
 * When the method is called, the event observation will automatically be terminated. For example:
 * <pre>
 * {@code
 *     @RemoveObserver
 *     @Override
 *     public void dispose()
 *     {
 *         // Any cleanup needed
 *     }
 * }
 * </pre>
 * Since the pseudo-bean's lifecycle is not container managed, you cannot use the {@code PreDestroy} annotation.
 *
 *
 * @see gov.takx.api.aspects.PostInitInjector
 * @see gov.takx.platform.cdi.InjectableObserverManager
 * @see gov.takx.api.cdi.InstanceManager
 * @since 3.4 (renamed from Injectable)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PseudoBean
{
    /**
     * (Optional) Indicates whether there should only be one instance of the annotated type in existence
     * at a time.  This is only needed when the PseudoBean also uses {@link Observes}.
     * Event observation on dynamic objects must either be expected for the lifetime of the object (until it is
     * finalized) or is explicitly terminated by a call to a method annotated with {@link RemoveObserver}
     * <p>
     * Setting {@code singleton} to {@code true} will cause any prior instance of a type to be automatically
     * unregistered as an event observer when a new instance is created, freeing the developer from having to
     * call a {@link RemoveObserver} method.
     */
    boolean singletonObserver() default false;
}
