package gov.takx.api.cdi;

import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Manages instances of non-managed objects that need injection services.
 *
 * @since 2.0.0
 */
@NoPluginImplementations
public interface InstanceManager
{
    /**
     * Inject the given object instance using the current CDI context.
     *
     * @param instance   Object to inject
     * @param directType The direct type from the join point (might be a super-interface)
     * @param inherited  Pass {@code true} if the PseudoBean annotation is inherited
     */
    void inject(Object instance, Class<?> directType, boolean inherited);

    /**
     * Get the object currently being injected on the current thread.  This is required since there is
     * no way to get hold of the object instance from an InjectionPoint, which is all a producer has access to.
     *
     * @return Current injection instance
     */
    Object getCurrentInjectionInstance();
}
