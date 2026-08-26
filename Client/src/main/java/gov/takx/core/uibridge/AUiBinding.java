package gov.takx.core.uibridge;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Abstract base for UI Provider bindings.  A binding provides a mechanism to construct the UI provider by calling
 * its constructor with appropriate arguments as needed per concrete binding type.
 *
 * @param <P> Inferred UI Provider interface
 * @since 5.1
 */
public abstract class AUiBinding<P>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected final Class<? extends P> uiProviderClass;

    /**
     * @param uiProviderClass The type of the UI provider
     */
    protected AUiBinding(Class<? extends P> uiProviderClass)
    {
        this.uiProviderClass = uiProviderClass;
    }

    /**
     * @return The type of the UI provider
     */
    public Class<? extends P> getUiProviderClass()
    {
        return uiProviderClass;
    }

    /**
     * Given the provider class and argument map, we need to construct an instance of the
     * UI provider.  We offer a poor man's injection, allowing several parameter types to be used in the constructor.
     *
     * @param providerClass The UI provider class
     * @param argMap        Map of Class to Object, each constructor argument will be tested against entry keys in this map.
     *                      If a key is assignable to the given constructor arg type, then the corresponding map value is
     *                      used as the argument value.
     * @return The constructed UI provider, or {@code null} on failure
     */
    @Nullable
    protected P constructUi(Class<? extends P> providerClass, Map<Class<?>, Object> argMap)
    {
        final Constructor<P> constructor = getConstructor(providerClass, providerClass.getSimpleName());

        if (constructor != null)
        {
            final Object[] args = collectArguments(constructor, argMap);

            try
            {
                return constructor.newInstance(args);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e)
            {
                logger.error("Failed to instantiate UI provider class [{}]", providerClass.getName(), e);
            }
        }
        return null;
    }

    /**
     * Get the single constructor for the given provider class.  Report errors if the given class doesn't have exactly
     * one constructor.
     *
     * @param providerClass Class from which to obtain the constructor
     * @param type          Type name to be used in error message
     * @return singular constructor, or {@code null} if exactly one constructor is not found
     */
    @Nullable
    private <T> Constructor<T> getConstructor(Class<?> providerClass, String type)
    {
        final String msg = "In order to be used as a " + type + " UI provider, it must declare exactly one constructor";
        final Constructor<?>[] constructors = providerClass.getConstructors();
        final String providerClassName = providerClass.getName();

        switch (constructors.length)
        {
            case 0 -> logger.error("Class [{}] does not define any public constructors\n{}", providerClassName, msg);
            case 1 ->
            {
                //noinspection unchecked
                return (Constructor<T>) constructors[0];
            }
            default -> logger.error("Class [{}] defines more than one constructor, actually {} of them\n{}",
                    providerClassName, constructors.length, msg);
        }

        return null;
    }

    /**
     * Collect the arguments to be passed to the given constructor based on values from the argMap.  Class assignability
     * is used to match argument types to values.
     *
     * @param constructor The constructor to process
     * @param argMap      Map of Class to Object, each constructor argument will be tested against entry keys in this map.
     *                    If a key is assignable to the given constructor arg type, then the corresponding map value is
     *                    used as the argument value.
     * @return Array of arguments to pass to the invocation of the constructor
     */
    @Nonnull
    private Object[] collectArguments(Constructor<?> constructor, Map<Class<?>, Object> argMap)
    {
        final Class<?>[] parameterTypes = constructor.getParameterTypes();
        final Object[] args = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++)
        {
            final Class<?> parameterType = parameterTypes[i];
            boolean found = false;

            // Scan the arg map to find an assignable entry
            for (Map.Entry<Class<?>, Object> entry : argMap.entrySet())
            {
                if (entry.getKey().isAssignableFrom(parameterType))
                {
                    args[i] = entry.getValue();
                    found = true;
                    break;
                }
            }

            if (!found) logger.warn("Unable to determine parameter value of type [{}] on {}", parameterType, constructor);
        }

        return args;
    }
}
