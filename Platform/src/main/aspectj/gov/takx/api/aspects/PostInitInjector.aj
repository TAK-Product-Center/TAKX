package gov.takx.api.aspects;

import gov.takx.api.annotation.Internal;
import gov.takx.api.cdi.InstanceManager;
import gov.takx.platform.cdi.ContainerFinder;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Catch the initialization of any object marked with @PseudoBean and ensure that it is processed (injected)
 * by the proper CDI container after its main initializer, and during deserialization.
 *
 * @since 2.0.0
 */
public aspect PostInitInjector
{
    private static final Map<ClassLoader, InstanceManager> instanceManagers = new ConcurrentHashMap<>();

    /**
     * Clear out the cache of instance managers.  This should only be called when all the containers have been
     * destroyed as the project is closing.
     *
     * For internal use only.
     *
     * @since 2.37
     */
    @Internal
    public static void clear()
    {
        instanceManagers.clear();
    }

    // Match a class annotated wih @PseudoBean
    pointcut directPseudoBean(): within(@gov.takx.api.annotation.PseudoBean *);

    // Match any class annotated wih an annotation that is annotated with @PseudoBean
    pointcut inheritedPseudoBean(): within(@(@gov.takx.api.annotation.PseudoBean *) *);

    /**
     * Catch initialization on any types directly annotated with @PseudoBean.
     */
    after(Object instance): directPseudoBean() && initialization(*.new(..)) && this(instance)
            {
                handleInjection(instance, thisJoinPointStaticPart.getSignature().getDeclaringType(), false);
            }

    /**
     * Catch initialization on any types annotated with an annotation that is annotated with @PseudoBean.
     * This catches all annotation types, call it A, that are themselves marked with @PseudoBean and then
     * a class that needs injection is annotated with A.
     */
    after(Object instance): inheritedPseudoBean() && initialization(*.new(..)) && this(instance)
            {
                handleInjection(instance, thisJoinPointStaticPart.getSignature().getDeclaringType(), true);
            }

    // The next collection of pointcuts, ITDs, and advice are all to support detecting the deserialization of
    // a Type that is a PseudoBean.  Since the JVM does not invoke any constructor as it reifies an object, the
    // advice above will never get applied since the 'initialization' pointcut has to match against a constructor.
    //
    // We will catch deserialization by trapping a method invocation.  Based on reading I did in the Spring code,
    // readResolve() is a good choice.  If the object already declares that method, then we add a new tag
    // interface, InjectableDeserializationSupportReadResolve, to the type so we can apply a pointcut to the execution
    // of that method.  If it doesn't have a readResolve() method, then we add a tag interface,
    // InjectableDeserializationSupport, to which we add a readResolve() method, which then allows us to
    // define a pointcut on its execution.
    //
    // The test for the presence of a method is an "experimental" feature of AspectJ (I don't know why since it has
    // been available for years), so the build script has to specifically enable that feature.

    /**
     * Declare any class implementing Serializable and @PseudoBean and has a readResolve() method as also implementing
     * ConfigurableDeserializationSupportReadResolve. This allows us to select it with the
     * beanDeserializationReadResolve() pointcut.
     */
    // @formatter:off - The formatter completely screws up these type references (by removing spaces)
    declare parents : (@gov.takx.api.annotation.PseudoBean *)
            && Serializable+
            && hasmethod(Object readResolve()throws ObjectStreamException)
            implements InjectableDeserializationSupportReadResolve;

    declare parents : (@(@gov.takx.api.annotation.PseudoBean *) *)
            && Serializable+
            && hasmethod(Object readResolve()throws ObjectStreamException)
            implements InjectableDeserializationSupportReadResolve;
    // @formatter:on

    /**
     * A marker interface if the class already has a{@code readResolve()} method.
     */
    interface InjectableDeserializationSupportReadResolve extends Serializable
    {
    }

    /**
     * Select deserialization join point made available through ITD for InjectableDeserializationSupportReadResolve
     */
    pointcut beanDeserializationReadResolve(Object bean):
            execution(java.lang.Object InjectableDeserializationSupportReadResolve+.readResolve()) && this(bean);

    /**
     * Declare any class implementing Serializable and @PseudoBean as also implementing
     * ConfigurableDeserializationSupport. This allows us to introduce the {@code readResolve()}
     * method and select it with the beanDeserialization() pointcut.
     */
    // @formatter:off - The formatter completely screws up these type references (by removing spaces)
    declare parents : (@gov.takx.api.annotation.PseudoBean *)
            && Serializable+
            && !hasmethod(Object readResolve()throws ObjectStreamException)
            implements InjectableDeserializationSupport;

    declare parents : (@(@gov.takx.api.annotation.PseudoBean *) *)
            && Serializable+
            && !hasmethod(Object readResolve()throws ObjectStreamException)
            implements InjectableDeserializationSupport;
    // @formatter:on

    /**
     * A marker interface to which the {@code readResolve()} will be introduced.
     */
    interface InjectableDeserializationSupport extends Serializable
    {
    }

    /**
     * Introduce the {@code readResolve()} method so that we can advise its execution to inject the object.
     */
    public Object InjectableDeserializationSupport.readResolve() throws ObjectStreamException
            {
                return this;
            }

    /**
     * Select deserialization join point made available through ITD for ConfigurableDeserializationSupport
     */
    pointcut beanDeserialization(Object bean):
            execution(java.lang.Object InjectableDeserializationSupport+.readResolve()) && this(bean);

    /**
     * Post-deserialization configuration.
     */
    after(Object instance) returning : beanDeserialization(instance) || beanDeserializationReadResolve(instance) {
        handleInjection(instance, thisJoinPoint.getThis().getClass(), false);
    }

    /**
     * Common handling of injection after initialization.
     *
     * @param instance   The instance that is being initialized
     * @param directType The direct type from the join point (might be a super-interface)
     * @param inherited  Pass {@code true} if the PseudoBean annotation is inherited
     */
    private void handleInjection(Object instance, Class<?> directType, boolean inherited)
    {
        final var instanceManager = instanceManagers.computeIfAbsent(instance.getClass().getClassLoader(),
                        classLoader -> {
                            final var container = ContainerFinder.getContainer(classLoader);
                            return container != null ? container.select(InstanceManager.class).get() : null;
                        });

        if (instanceManager != null) instanceManager.inject(instance, directType, inherited);
    }
}
