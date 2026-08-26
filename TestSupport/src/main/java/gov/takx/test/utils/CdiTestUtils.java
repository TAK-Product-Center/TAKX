package gov.takx.test.utils;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.jboss.weld.proxy.WeldClientProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

import static java.lang.reflect.Modifier.isStatic;

/**
 * Utilities for tests that need to perform reflective tasks on objects that may be CDI client proxies.
 * These methods deal with determining the actual object instance in the case that the supplied object is a CDI proxy.
 * <p>
 * A lot of work was put in to remove the use of JMockit's Deencapsulation class (which has morphed over time and
 * become a source of problems).  Further, we don't want to pollute the transitive dependencies of this module
 * with additional libraries that might conflict with consuming projects.
 *
 * @since 2.6
 */
@SuppressWarnings("unused")
public class CdiTestUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Invokes a non-accessible (eg {@code private}) instance method from a given class with the given arguments.
     *
     * @param objectWithMethod the instance on which the invocation is to be done
     * @param methodName the name of the method to invoke
     * @param nonNullArgs zero or more non-null parameter values for the invocation; if a {@code null} value needs to be
     *                   passed, the {@code Class} object for the corresponding parameter type must be passed instead
     * @param <T> type to which the returned value should be assignable
     * @return the return value from the invoked method
     * @throws IllegalArgumentException if a {@code null} reference was provided for a parameter
     */
    public static <T> T invoke(Object objectWithMethod, String methodName, Object... nonNullArgs)
    {
        return invoke(objectWithMethod, methodName, ClassUtils.toClass(nonNullArgs), nonNullArgs);
    }

    /**
     * Invokes a non-accessible (eg {@code private}) instance method from a given class with the given arguments.
     *
     * @param objectWithMethod the instance on which the invocation is to be done
     * @param methodName       the name of the method to invoke
     * @param parameterTypes   the types of the parameters as declared in the desired method
     * @param methodArgs       zero or more parameter values for the invocation
     * @param <T>              type to which the returned value should be assignable
     * @return the return value from the invoked method
     */
    public static <T> T invoke(Object objectWithMethod, String methodName, Class<?>[] parameterTypes, Object... methodArgs)
    {
        try
        {
            final Object targetInstance = getInstance(objectWithMethod);

            //noinspection unchecked
            return (T) MethodUtils.invokeMethod(targetInstance, true, methodName, methodArgs, parameterTypes);
        } catch (NoSuchMethodException e)
        {
            final String argsDesc = Arrays.stream(parameterTypes).map(Class::getCanonicalName).collect(Collectors.joining());
            throw new IllegalArgumentException("No compatible method found: " + methodName + "(" + argsDesc + ")");
        } catch (InvocationTargetException e)
        {
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException)
            {
                throw (RuntimeException) cause;
            } else
            {
                throw new RuntimeException("Failed to invoke method: " + methodName, e);
            }
        } catch (IllegalAccessException e)
        {
            throw new RuntimeException("Failed to invoke method: " + methodName, e);
        }
    }

    /**
     * Invokes a non-accessible (eg {@code private}) {@code static} method with the given arguments.
     *
     * @param classWithStaticMethod the class on which the invocation is to be done
     * @param methodName            the name of the static method to invoke
     * @param parameterTypes        the types of the parameters as declared in the desired method
     * @param methodArgs            zero or more parameter values for the invocation
     * @param <T>                   type to which the returned value should be assignable
     * @return the return value from the invoked method
     */
    public static <T> T invoke(Class<?> classWithStaticMethod, String methodName, Class<?>[] parameterTypes, Object... methodArgs)
    {
        try
        {
            final Method method = MethodUtils.getMatchingMethod(classWithStaticMethod, methodName, parameterTypes);
            if (method == null)
            {
                throw new NoSuchMethodException("No such method: " + methodName + "() on class: " + classWithStaticMethod);
            }

            if (!method.canAccess(null)) method.setAccessible(true);

            //noinspection unchecked
            return (T) method.invoke(null, methodArgs);
        } catch (NoSuchMethodException e)
        {
            final String argsDesc = Arrays.stream(parameterTypes).map(Class::getCanonicalName).collect(Collectors.joining());
            throw new IllegalArgumentException("No compatible method found: " + methodName + "(" + argsDesc + ")");
        } catch (IllegalAccessException | InvocationTargetException e)
        {
            throw new RuntimeException("Failed to invoke method: " + methodName, e);
        }
    }

    /**
     * Invokes a non-accessible (eg {@code private}) {@code static} method with the given arguments.
     *
     * @param classWithStaticMethod the class on which the invocation is to be done
     * @param methodName the name of the static method to invoke
     * @param nonNullArgs zero or more non-null parameter values for the invocation; if a {@code null} value needs to be
     *                   passed, the {@code Class} object for the corresponding parameter type must be passed instead
     * @param <T> type to which the returned value should be assignable
     * @return the return value from the invoked method
     * @throws IllegalArgumentException if a {@code null} reference was provided for a parameter
     */
    public static <T> T invoke(Class<?> classWithStaticMethod, String methodName, Object... nonNullArgs)
    {
        return invoke(classWithStaticMethod, methodName, ClassUtils.toClass(nonNullArgs), nonNullArgs);
    }

    /**
     * Set the value of a field on the given object.
     *
     * @param objectWithField the instance on which to set the field value
     * @param fieldName       Name of the field to set
     * @param fieldValue      Value to set on the field
     * @throws IllegalArgumentException if field is not found
     * @throws RuntimeException         If {@link Field#set(Object, Object)} throws an exception
     * @since 2.35
     */
    public static void setField(Object objectWithField, String fieldName, Object fieldValue) throws IllegalArgumentException
    {
        final Object targetInstance = getInstance(objectWithField);

        try
        {
            findAndMakeAccessibleField(targetInstance.getClass(), fieldName, true).set(targetInstance, fieldValue);
        } catch (IllegalAccessException e)
        {
            throw new RuntimeException("Failed to set field: " + fieldName + " on " + targetInstance.getClass(), e);
        }
    }

    /**
     * Set the value of a field on the given object.
     *
     * @param theClass   The class from which to get the field value
     * @param fieldName  Name of the field to set
     * @param fieldValue Value to set on the field
     * @throws IllegalArgumentException if field is not found
     * @throws RuntimeException         If {@link Field#set(Object, Object)} throws an exception
     * @since 2.35
     */
    public static void setField(Class<?> theClass, String fieldName, Object fieldValue) throws IllegalArgumentException
    {
        try
        {
            findAndMakeAccessibleField(theClass, fieldName, false).set(null, fieldValue);
        } catch (IllegalAccessException e)
        {
            throw new RuntimeException("Failed to set field: " + fieldName + " on " + theClass, e);
        }
    }

    /**
     * Get the value of a field on the given object.
     *
     * @param objectWithField the instance from which to get the field value
     * @param fieldName       Name of the field to set
     * @throws IllegalArgumentException if field is not found
     * @throws RuntimeException         If {@link Field#get(Object)} throws an exception
     * @since 2.35
     */
    public static <T> T getField(Object objectWithField, String fieldName) throws IllegalArgumentException
    {
        final Object targetInstance = getInstance(objectWithField);

        try
        {
            //noinspection unchecked
            return (T) findAndMakeAccessibleField(targetInstance.getClass(), fieldName, true).get(targetInstance);
        } catch (IllegalAccessException e)
        {
            throw new RuntimeException("Failed to get field: " + fieldName + " on " + targetInstance.getClass(), e);
        }
    }

    /**
     * Get the value of a static field on the given class.
     *
     * @param theClass  The class from which to get the field value
     * @param fieldName Name of the field to set
     * @throws IllegalArgumentException if field is not found
     * @throws RuntimeException         If {@link Field#get(Object)} throws an exception
     * @since 2.35
     */
    public static <T> T getField(Class<?> theClass, String fieldName) throws IllegalArgumentException
    {
        try
        {
            //noinspection unchecked
            return (T) findAndMakeAccessibleField(theClass, fieldName, false).get(null);
        } catch (IllegalAccessException e)
        {
            throw new RuntimeException("Failed to get field: " + fieldName + " on " + theClass, e);
        }
    }

    /**
     * Gets the value of a non-accessible (eg {@code private}) field from a given object, <em>assuming</em> there is only
     * one field declared in the class of the given object whose type can receive values of the specified field type.
     *
     * @param objectWithField the instance from which to get the field value
     * @param fieldType       the declared type of the field, or a sub-type of the declared field type
     * @throws IllegalArgumentException if either the desired field is not found, or more than one is
     * @since 2.19
     */
    public static <T> T getField(Object objectWithField, Class<T> fieldType)
    {
        final Object targetInstance = getInstance(objectWithField);
        final Field field = getDeclaredFieldOfType(targetInstance.getClass(), fieldType, true);

        field.setAccessible(true);

        try
        {
            //noinspection unchecked
            return (T) field.get(targetInstance);
        } catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the value of a non-accessible (eg {@code private}) static field from a given class,
     * <em>assuming</em> there is only one field declared in the given class whose type can
     * receive values of the specified field type.
     *
     * @param theClass  the class from which to get the field value
     * @param fieldType the declared type of the field, or a sub-type of the declared field type
     * @throws IllegalArgumentException if either the desired field is not found, or more than one is
     * @since 2.35
     */
    public static <T> T getField(Class<?> theClass, Class<T> fieldType)
    {
        final Field field = getDeclaredFieldOfType(theClass, fieldType, false);

        field.setAccessible(true);

        try
        {
            //noinspection unchecked
            return (T) field.get(null);
        } catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Find a declared field on the given class and make it accessible.
     *
     * @param instanceClass Class to inspect
     * @param fieldName     Name of field to obtain
     * @param instanceField Pass {@code true} if accessing an instance field, {@code false} for a static field
     * @return Field instance, made accessible via {@link Field#setAccessible(boolean)}
     * @throws IllegalArgumentException if field is not found
     * @since 2.35
     */
    private static Field findAndMakeAccessibleField(Class<?> instanceClass, String fieldName, boolean instanceField)
    {
        try
        {
            final Field field = getDeclaredField(instanceClass, fieldName, instanceField);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e)
        {
            throw new IllegalArgumentException("Can't find field: " + fieldName + " on " + instanceClass, e);
        }
    }

    /**
     * Get a declared field on the given class, recurse up the super-class chain if not found directly on {@code theClass}.
     *
     * @param theClass      The class to inspect
     * @param fieldName     Name of field to locate
     * @param instanceField Pass {@code true} if accessing an instance field, {@code false} for a static field
     * @return Field
     * @throws NoSuchFieldException If there is no field of the given name in the class hierarchy
     * @since 2.35
     */
    private static Field getDeclaredField(Class<?> theClass, String fieldName, boolean instanceField) throws NoSuchFieldException
    {
        try
        {
            return theClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException ignore)
        {
            final Class<?> superClass = theClass.getSuperclass();

            if (superClass != null && superClass != Object.class)
            {
                return getDeclaredField(superClass, fieldName, instanceField);
            }

            final String kind = instanceField ? "instance" : "static";
            throw new NoSuchFieldException("No " + kind + " field of name \"" + fieldName + "\" found in " + theClass);
        }
    }

    /**
     * Get a field from the given class (or any of its super classes) that has the given type.  If no fields or
     * more than a single field matches the given type, then an error is thrown.
     *
     * @param theClass     The class to inspect
     * @param desiredClass The type of the field to retrieve
     * @return The located {@code Field}, may be {@code null} if no fields are type compatible with the given {@code desiredClass}
     * @throws IllegalArgumentException If a matching field can not be found or more than a single field matches the given type
     * @since 2.35
     */
    public static Field getDeclaredFieldOfType(Class<?> theClass, Class<?> desiredClass, boolean instanceField)
    {
        Field found = getDeclaredFieldInClass(theClass, desiredClass, instanceField);

        if (found == null)
        {
            Class<?> superClass = theClass.getSuperclass();

            if (superClass != null && superClass != Object.class)
            {
                return getDeclaredFieldOfType(superClass, desiredClass, instanceField);
            }

            String errorMsg = "Field of type " + desiredClass.getName() + " not found in " + theClass;
            throw new IllegalArgumentException(errorMsg);
        }

        return found;
    }

    /**
     * Inspect a specific class to locate a field matching the given type.
     *
     * @param theClass     The class to inspect
     * @param desiredClass The type of the field to retrieve
     * @return The located {@code Field}, may be {@code null} if no fields are type compatible with the given {@code desiredClass}
     * @throws IllegalArgumentException If a more than a single field matches the given type
     * @since 2.35
     */
    private static Field getDeclaredFieldInClass(Class<?> theClass, Class<?> desiredClass, boolean instanceField)
    {
        Field found = null;

        for (Field field : theClass.getDeclaredFields())
        {
            if (!field.isSynthetic())
            {
                final Class<?> fieldClass = field.getType();
                final boolean isCompatible = desiredClass.isAssignableFrom(fieldClass) || fieldClass.isAssignableFrom(desiredClass);

                if (instanceField != isStatic(field.getModifiers()) && isCompatible)
                {
                    if (found != null)
                    {
                        final String errorMessage = "More than one field from which a value of type "
                                + desiredClass + " can be read exists in "
                                + field.getDeclaringClass() + ": " + found.getName() + ", " + field.getName();
                        throw new IllegalArgumentException(errorMessage);
                    }

                    found = field;
                }
            }
        }

        return found;
    }

    /**
     * Given an object that may be a CDI proxy, return the actual object instance.
     *
     * @param possibleProxy Object that may be a proxy
     * @return The object if it is not a proxy, or the target object of the proxy otherwise
     */
    public static Object getInstance(Object possibleProxy)
    {
        return (possibleProxy instanceof WeldClientProxy)
                ? ((WeldClientProxy) possibleProxy).getMetadata().getContextualInstance()
                : possibleProxy;
    }

    /**
     * Return a string containing the class and identity hash code for a given object.
     *
     * @param o Object of interest
     * @return What would be returned by Object#toString()
     */
    public static String identityToString(Object o)
    {
        return ObjectUtils.identityToString(o);
    }

    /**
     * Find the no-arg constructor for the given class, ensure it is accessible, and then invoke it to return
     * a new instance.
     *
     * @param aClass The class to instantiate
     * @param <T>    Inferred type
     * @return New instance
     * @since 2.29
     */
    public static <T> T newInstance(Class<T> aClass)
    {
        try
        {
            final Constructor<T> constructor = aClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e)
        {
            final String message = "shape creation failed: " + aClass.getSimpleName();
            logger.warn(message, e);
            throw new RuntimeException(message, e);
        }
    }
}
