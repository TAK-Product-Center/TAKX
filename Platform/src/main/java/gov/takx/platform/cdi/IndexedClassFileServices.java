package gov.takx.platform.cdi;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.weld.environment.deployment.WeldResourceLoader;
import org.jboss.weld.environment.deployment.discovery.jandex.JandexClassFileInfo;
import org.jboss.weld.environment.logging.CommonLogger;
import org.jboss.weld.resources.spi.ClassFileInfo;
import org.jboss.weld.resources.spi.ClassFileServices;
import org.jboss.weld.util.cache.ComputingCache;
import org.jboss.weld.util.cache.ComputingCacheBuilder;
import org.jboss.weld.util.collections.ImmutableSet;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.function.Function;

/**
 * Jandex implementation of the {@link ClassFileServices}), which is a service that is a faster alternative to get info
 * about class without the need to load it with a ClassLoader.
 * <p>
 * Pretty much stolen from {@link org.jboss.weld.environment.deployment.discovery.jandex.JandexClassFileServices} because
 * it was written in a way to make it impossible to subclass.
 *
 * @since 2.0.0
 */
public class IndexedClassFileServices implements ClassFileServices
{
    private IndexView index;
    private ComputingCache<DotName, Set<String>> annotationClassAnnotationsCache;
    private final ClassLoader classLoader;

    public IndexedClassFileServices(IndexView index)
    {
        this.index = index;
        if (index == null)
        {
            throw CommonLogger.LOG.jandexIndexNotCreated(ClassFileServices.class.getSimpleName());
        }
        this.classLoader = WeldResourceLoader.getClassLoader();
        this.annotationClassAnnotationsCache = ComputingCacheBuilder.newBuilder().build(new AnnotationClassAnnotationLoader());
    }

    @Override
    public ClassFileInfo getClassFileInfo(String className)
    {
        return new JandexClassFileInfo(className, index, annotationClassAnnotationsCache, classLoader);
    }

    @Override
    public void cleanupAfterBoot()
    {
        if (annotationClassAnnotationsCache != null)
        {
            annotationClassAnnotationsCache.clear();
            annotationClassAnnotationsCache = null;
        }
        index = null;
    }

    @Override
    public void cleanup()
    {
        cleanupAfterBoot();
    }

    private class AnnotationClassAnnotationLoader implements Function<DotName, Set<String>>
    {
        @Override
        public Set<String> apply(DotName name)
        {
            ClassInfo annotationClassInfo = index.getClassByName(name);
            ImmutableSet.Builder<String> builder = ImmutableSet.builder();
            if (annotationClassInfo != null)
            {
                for (DotName annotationName : annotationClassInfo.annotations().keySet())
                {
                    builder.add(annotationName.toString());
                }
            } else
            {
                try
                {
                    Class<?> annotationClass = classLoader.loadClass(name.toString());
                    for (Annotation annotation : annotationClass.getDeclaredAnnotations())
                    {
                        builder.add(annotation.annotationType().getName());
                    }
                } catch (ClassNotFoundException e)
                {
                    throw CommonLogger.LOG.unableToLoadAnnotation(name.toString());
                }
            }
            return builder.build();
        }
    }
}
