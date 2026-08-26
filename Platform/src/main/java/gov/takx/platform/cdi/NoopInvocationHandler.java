package gov.takx.platform.cdi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * InvocationHandler that provides a no-op implementation for a dynamic proxy.  The handler will perform no
 * operations and will return {@code null} for all Object returning methods, and 0 or false for all primitive returning
 * methods.
 *
 * @since 2.37
 */
public class NoopInvocationHandler implements InvocationHandler
{
    /**
     * Factory to generate a noop handler for a given interface.
     *
     * @param cls Interface class for which to generate a proxy
     * @param <T> Inferred type
     * @return proxy
     */
    @SuppressWarnings("unchecked")
    public static <T> T forType(Class<T> cls)
    {
        return (T) Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{cls}, new NoopInvocationHandler(cls));
    }

    private final String toString;

    private NoopInvocationHandler(Class<?> proxiedClass)
    {
        toString = "Noop " + proxiedClass.getSimpleName();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
    {
        // If the method returns a primitive, return 0 or false accordingly.
        // Otherwise, if it is the toString method, return a descriptive string else return null

        final Class<?> returnType = method.getReturnType();

        return returnType.isPrimitive()
                ? returnType == boolean.class ? false : 0
                : "toString".equals(method.getName()) ? toString : null;
    }
}
