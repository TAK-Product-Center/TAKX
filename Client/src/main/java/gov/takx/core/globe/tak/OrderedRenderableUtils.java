package gov.takx.core.globe.tak;

import gov.nasa.worldwind.render.Annotation;
import gov.nasa.worldwind.render.BasicAnnotationRenderer;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.markers.Marker;
import gov.nasa.worldwind.render.markers.MarkerRenderer;
import gov.takx.platform.globe.MarkerRenderable;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

/**
 * Utility class for working with {@link OrderedRenderable}.
 *
 * @since 3.5
 */
class OrderedRenderableUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static Field annotationField;
    private static Field markerField;

    @Nonnull
    public static Renderable extractRenderable(@Nonnull OrderedRenderable orderedRenderable)
    {
        if (orderedRenderable instanceof PointPlacemark.OrderedPlacemark orderedPlacemark)
        {
            PointPlacemark pointPlacemark = orderedPlacemark.getPlacemark();
            pointPlacemark.setValue(PointPlacemark.OrderedPlacemark.class.getName(), orderedPlacemark);
            return pointPlacemark;
        }
        if (orderedRenderable.getClass().getName().contains("OrderedAnnotation"))
        {
            Annotation annotation = extractAnnotation(orderedRenderable);
            if (annotation != null) return annotation;
        }
        if (orderedRenderable.getClass().getName().contains("OrderedMarker"))
        {
            Marker marker = extractMarker(orderedRenderable);
            if (marker != null) return new MarkerRenderable(marker);
        }
        return orderedRenderable;
    }

    private static Annotation extractAnnotation(OrderedRenderable orderedRenderable)
    {
        if (annotationField == null)
        {
            try
            {
                Class<?> clazz = BasicAnnotationRenderer.class.getClassLoader().loadClass(BasicAnnotationRenderer.class.getName() + "$OrderedAnnotation");
                annotationField = clazz.getDeclaredField("annotation");
            } catch (NoSuchFieldException | ClassNotFoundException e)
            {
                logger.error("Could not extract OrderedAnnotation class.", e);
                return null;
            }
            annotationField.setAccessible(true);
        }
        try
        {
            return (Annotation) annotationField.get(orderedRenderable);
        } catch (IllegalAccessException e)
        {
            logger.error("Could not extract OrderedAnnotation class.", e);
            return null;
        }
    }

    private static Marker extractMarker(OrderedRenderable orderedRenderable)
    {
        if (markerField == null)
        {
            try
            {
                Class<?> clazz = MarkerRenderer.class.getClassLoader().loadClass(MarkerRenderer.class.getName() + "$OrderedMarker");
                markerField = clazz.getDeclaredField("marker");
            } catch (NoSuchFieldException | ClassNotFoundException e)
            {
                logger.error("Could not extract OrderedMarker class.", e);
                return null;
            }
            markerField.setAccessible(true);
        }
        try
        {
            return (Marker) markerField.get(orderedRenderable);
        } catch (IllegalAccessException e)
        {
            logger.error("Could not extract OrderedMarker class.", e);
            return null;
        }
    }
}
