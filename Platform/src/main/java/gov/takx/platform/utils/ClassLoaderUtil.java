package gov.takx.platform.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Stolen from Ehcache.  Expanded to offer subclass handling.
 *
 * @since 1.36
 */
@SuppressWarnings("UnusedDeclaration")
public final class ClassLoaderUtil
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Thread local providing an empty stack as the initial value.
     */
    private static final ThreadLocal<Deque<ClassLoader>> savedClassLoader = ThreadLocal.withInitial(ArrayDeque::new);

    // Place holder for a null class loader
    private static final ClassLoader NULL_CLASSLOADER = new ClassLoader()
    {
    };

    /**
     * Utility class.
     */
    private ClassLoaderUtil()
    {
        //noop
    }

    /**
     * Set the thread context class loader to the given class loader, saving the current value away.
     *
     * @param classLoader Class loader to install as thread context class loader
     * @return Context class loader in use at the time of this call
     * @see #popContextClassLoader()
     */
    public static ClassLoader pushContextClassLoader(ClassLoader classLoader)
    {
        ClassLoader currentTCCL = getContextClassLoader();

        if (currentTCCL == null)
        {
            currentTCCL = NULL_CLASSLOADER; // We can't push a null onto the stack, so push a placeholder
        }

        savedClassLoader.get().push(currentTCCL);
        Thread.currentThread().setContextClassLoader(classLoader);
        return currentTCCL;
    }

    /**
     * Restore the previous context class loader saved by a call to {@link #pushContextClassLoader(ClassLoader)}.
     */
    public static void popContextClassLoader()
    {
        final Deque<ClassLoader> stack = savedClassLoader.get();

        if (stack.isEmpty())
        {
            logger.error("No prior call to pushContextClassLoader in pop", new RuntimeException("stack"));
        } else
        {
            ClassLoader previousTCCL = stack.pop();

            // Don't try to restore the null class loader placeholder
            if (previousTCCL == NULL_CLASSLOADER)
            {
                previousTCCL = null;
            }

            Thread.currentThread().setContextClassLoader(previousTCCL); // Take last pushed CL and restore it
        }
    }

    /**
     * Gets the current threads context class loader.
     *
     * @return the thread context class loader.
     */
    public static ClassLoader getContextClassLoader()
    {
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * Gets a fallback {@code ClassLoader} that all classes, and extensions,
     * should use for class loading. This is used if the context class loader does not work.
     *
     * @return the {@code ClassLoaderUtil.class.getClassLoader();}
     */
    public static ClassLoader getFallbackClassLoader()
    {
        return ClassLoaderUtil.class.getClassLoader();
    }

    /**
     * Creates a new class instance. Logs errors along the way. Classes are loaded using the
     * standard classloader.
     *
     * @param className a fully qualified class name
     * @return the newly created instance
     */
    public static Object createNewInstance(String className)
    {
        return createNewInstance(className, new Class[0], new Object[0]);
    }

    /**
     * Creates a new class instance of the given subclass type. Logs errors along the way.
     *
     * @param className a fully qualified class name
     * @return the newly created instance
     */
    public static <T> T createNewInstance(Class<T> subclass, String className)
    {
        return createNewInstance(subclass, className, new Class[0], new Object[0]);
    }

    /**
     * Creates a new class instance and passes args to the constructor call. Logs errors along the way.
     *
     * @param className a fully qualified class name
     * @param argTypes  Types for constructor argument parameters
     * @param args      Values for constructor argument parameters
     * @return the newly created instance
     * @throws RuntimeException if instance cannot be created due to a missing class or exception
     */
    public static Object createNewInstance(String className, Class<?>[] argTypes, Object[] args) throws RuntimeException
    {
        return createNewInstance(Object.class, className, argTypes, args);
    }

    /**
     * Load the given class by name
     *
     * @param className a fully qualified class name
     * @return Class the loaded class
     * @throws ClassNotFoundException if the class cannot be loaded
     * @since 1.7
     */
    public static Class<?> loadClass(String className) throws ClassNotFoundException
    {
        Class<?> clazz;
        try
        {
            clazz = Class.forName(className, true, getContextClassLoader());
        } catch (ClassNotFoundException e)
        {
            //try fallback
            clazz = Class.forName(className, true, getFallbackClassLoader());
        }

        return clazz;
    }

    /**
     * Creates a new class instance of the given subclass type and passes args to the constructor call.
     * Logs errors along the way.
     *
     * @param className a fully qualified class name
     * @param argTypes  Types for constructor argument parameters
     * @param args      Values for constructor argument parameters
     * @return the newly created instance
     * @throws RuntimeException if instance cannot be created due to a missing class or exception
     */
    public static <T> T createNewInstance(Class<T> subclass, String className, Class<?>[] argTypes, Object[] args)
            throws RuntimeException
    {
        Class<? extends T> clazz;
        T newInstance;
        try
        {
            clazz = loadClass(subclass, className);
        } catch (ClassNotFoundException e)
        {
            throw new RuntimeException("Unable to load class " + className +
                    ". Initial cause was " + e.getMessage(), e);
        }

        try
        {
            Constructor<? extends T> constructor = clazz.getConstructor(argTypes);
            newInstance = constructor.newInstance(args);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | SecurityException | IllegalArgumentException e)
        {
            throw new RuntimeException("Unable to load class " + className +
                    ". Initial cause was " + e.getMessage(), e);
        } catch (InvocationTargetException e)
        {
            throw new RuntimeException("Unable to load class " + className +
                    ". Initial cause was " + e.getCause().getMessage(), e.getCause());
        }
        return newInstance;
    }

    /**
     * Load the given class by name as a subclass of the specified type.
     *
     * @param subclass  Subclass, {@code className} must indicate a class assignable to this type or a ClassCastException will result
     * @param className Name of class to load
     * @param <T>       Subclass type
     * @return Class of the given subclass type
     * @throws ClassNotFoundException If it can't be loaded
     */
    public static <T> Class<? extends T> loadClass(Class<T> subclass, String className) throws ClassNotFoundException
    {
        Class<? extends T> clazz;
        try
        {
            clazz = Class.forName(className, true, getContextClassLoader()).asSubclass(subclass);
        } catch (ClassNotFoundException e)
        {
            //try fallback
            clazz = Class.forName(className, true, getFallbackClassLoader()).asSubclass(subclass);
        }

        return clazz;
    }
}
