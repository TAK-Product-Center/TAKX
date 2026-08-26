package gov.takx.api.plugin;

import gov.takx.api.plugin.annotations.PluginDescriptor;
import gov.takx.api.plugin.annotations.PluginSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Utility methods for accessing information from the various plugin annotations.
 *
 * @since 3.0
 */
public class PluginAnnotationUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Extract the PluginSpecification annotation from this plugin annotation on this class.
     * This method on the containing annotation is always called {@code spec}.
     *
     * @return extracted PluginSpecification
     */
    public static PluginSpecification extractPluginSpecification(Annotation annotation)
    {
        final Class<? extends Annotation> annotationClass = annotation.annotationType();

        try
        {
            final Method method = annotationClass.getMethod("spec");

            return (PluginSpecification) method.invoke(annotation);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e)
        {
            logger.error("[FATAL] Unable to find spec() method on {}", annotationClass, e);
            throw new IllegalStateException("Plugin annotations are mis-configured!", e);
        }
    }

    /**
     * Extract the PluginDescriptor annotation from this plugin annotation on this class.
     * This method on the containing annotation is always called {@code descriptor}.
     *
     * @return extracted PluginSpecification
     */
    public static PluginDescriptor extractPluginDescriptor(Annotation annotation)
    {
        final Class<? extends Annotation> annotationClass = annotation.annotationType();

        try
        {
            final Method method = annotationClass.getMethod("descriptor");

            return (PluginDescriptor) method.invoke(annotation);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e)
        {
            logger.error("[FATAL] Unable to find descriptor() method on {}", annotationClass, e);
            throw new IllegalStateException("Plugin annotations are mis-configured!", e);
        }
    }

    private PluginAnnotationUtils()
    {
    }
}
