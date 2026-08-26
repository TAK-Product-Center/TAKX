package gov.takx.platform.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import java.util.stream.Stream;

/**
 * Utility methods for working with Jandex created indices.
 *
 * @since 2.18
 */
@ApplicationScoped
public class IndexUtils
{
    /**
     * Find all the annotation instances in the given index for all the given dot names.  If any of the annotation
     * instance's values are nested arrays, then those are expanded and returned in the stream instead of the
     * containing annotation.  This simplifies the handling of repeatable annotations.
     *
     * @param index    Index to search
     * @param dotNames Dot names to locate
     * @return Flattened stream of all annotation instances
     */
    public Stream<AnnotationInstance> getAnnotationInstances(IndexView index, DotName... dotNames)
    {
        return Stream.of(dotNames)
                .flatMap(t -> index.getAnnotations(t).stream())
                .flatMap(ai -> ai.value() != null && ai.value().kind() == AnnotationValue.Kind.ARRAY
                        ? Stream.of(ai.value().asNestedArray()) // Expand nested array of annotations
                        : Stream.of(ai));
    }

    IndexUtils()
    {
    }
}
