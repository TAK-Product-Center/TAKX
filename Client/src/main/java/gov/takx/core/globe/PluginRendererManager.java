package gov.takx.core.globe;

import com.google.common.collect.ImmutableSet;
import gov.takx.api.annotation.Headless;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedPluginRendererManager;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IRenderingEngine;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.plugin.annotations.GlobeRenderer;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.module.DeploymentAttachments;
import gov.takx.module.IPluginModuleManager;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service responsible for managing all renderers defined within plugin deployments.
 *
 * @since 2.29
 */
@Singleton
public class PluginRendererManager implements IExtendedPluginRendererManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final DotName GLOBE_RENDERER_TYPE = DotName.createSimple(GlobeRenderer.class.getName());

    private static final String WILDCARD = "*";
    private static final String SEP = "/";

    @Inject
    @Headless
    private boolean headless;

    @Inject
    private IPluginModuleManager pluginModuleManager;

    private volatile boolean renderersLoaded = false;
    private final Map<Class<? extends IRenderingEngine>, Constructor<? extends IRenderingEngine>> rendererConstructors = new HashMap<>();

    // Map from rendering engine class to its exclusive message types
    private final Map<Class<? extends IRenderingEngine>, Set<String>> exclusiveMessageTypes = new HashMap<>();

    // Map from rendering engine class to its exclusive Map Object patterns
    private final Map<Class<? extends IRenderingEngine>, List<FamilyAndType>> exclusiveMapObjects = new HashMap<>();

    // Map from globe to all renderers created for it
    private final Map<IApplicationGlobe, List<IRenderingEngine>> renderersByGlobe = new HashMap<>();

    /**
     * Shutdown all plugin-supplied renderers just <em>before</em> the deployment containers are shut down.
     * This is required since the rendering engine can have injected references that become invalid once the
     * container is shutdown.  So, we need to clean up the renderers before the containers are destroyed.
     * <p>
     * I would have preferred to have the globe instance manage this, but there is a limitation on the handling of
     * event observers on {@link gov.takx.api.annotation.PseudoBean} objects (they don't support observer priority),
     * so sequencing can not be controlled.
     *
     * @since 2.29
     */
    private void shutdownPluginRenderers(@SuppressWarnings("unused") @Observes @Priority(ServiceOrdering.STOP_DeploymentContainerManager - 1) StopProjectServices event)
    {
        // Operate on a copy of the keys since this operation will lead to calls back to shutdownRenderers, which
        // modifies the renderersByGlobe map.

        new HashSet<>(renderersByGlobe.keySet()).forEach(IApplicationGlobe::deactivatePluginRenderers);
    }

    @Override
    public boolean isRenderingExcluded(Class<? extends IRenderingEngine> rendererClass, String messageType)
    {
        for (Map.Entry<Class<? extends IRenderingEngine>, Set<String>> entry : exclusiveMessageTypes.entrySet())
        {
            if (entry.getKey() != rendererClass && entry.getValue().contains(messageType))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isRenderingExcluded(Class<? extends IRenderingEngine> rendererClass, IMapObject mapObject)
    {
        final String family = mapObject.getFamily();
        final String type = mapObject.getType();

        // Go old school to avoid stream creation overhead
        for (Map.Entry<Class<? extends IRenderingEngine>, List<FamilyAndType>> entry : exclusiveMapObjects.entrySet())
        {
            if (entry.getKey() != rendererClass)
            {
                for (FamilyAndType familyAndType : entry.getValue())
                {
                    if (familyAndType.matches(family, type)) return true;
                }
            }
        }
        return false;
    }

    @Override
    public Set<String> getExcludedMessageTypes(Class<? extends IRenderingEngine> rendererClass)
    {
        return exclusiveMessageTypes.entrySet().stream()
                .filter(entry -> entry.getKey() != rendererClass)
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public List<IRenderingEngine> constructRenderers(IApplicationGlobe globe)
    {
        if (headless) return Collections.emptyList();

        // Find plugin provided renderers the first time we're asked to create them.
        if (!renderersLoaded)
        {
            findRenderers();
            renderersLoaded = true;
        }

        final List<IRenderingEngine> renderers = rendererConstructors.values().stream().map(ctor -> {
            try
            {
                return ctor.newInstance(globe);
            } catch (ReflectiveOperationException e)
            {
                logger.error("Failed to create an instance of renderer: {}", ctor.getDeclaringClass().getName(), e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        renderersByGlobe.put(globe, renderers);

        return renderers;
    }

    @Override
    public void shutdownRenderers(IApplicationGlobe globe)
    {
        final List<IRenderingEngine> renderers = renderersByGlobe.remove(globe);

        if (renderers != null) renderers.forEach(r -> r.shutdown(globe.isPrimaryGlobe()));
    }

    @Override
    public <T extends IRenderingEngine> T getRendererForGlobe(IGlobeDelegate globeDelegate, Class<T> rendererClass)
    {
        // Using this loop to avoid casting the given delegate to IApplicationGlobe
        final int globeId = globeDelegate.getGlobeId();

        for (Map.Entry<IApplicationGlobe, List<IRenderingEngine>> entry : renderersByGlobe.entrySet())
        {
            final IApplicationGlobe globe = entry.getKey();
            if (globe.getGlobeId() == globeId)
            {
                //noinspection unchecked
                return (T) entry.getValue().stream().filter(rendererClass::isInstance).findFirst().orElse(null);
            }
        }
        return null;
    }

    /**
     * Scan all deployments for renderers (marked with the {@link GlobeRenderer} annotation) and internalize their
     * configuration.
     */
    private void findRenderers()
    {
        pluginModuleManager.getAllDeployableDeployments().forEach(pd -> {
            final Index index = pd.getAttachment(DeploymentAttachments.INDEX);
            final ClassLoader classLoader = pd.getAttachment(DeploymentAttachments.CLASS_LOADER);

            index.getAnnotations(GLOBE_RENDERER_TYPE).forEach(ai -> {
                final String rendererClassName = ai.target().asClass().name().toString();

                final Class<? extends IRenderingEngine> rendererClass = internRendererClass(rendererClassName, classLoader);

                if (rendererClass != null)
                {
                    internExclusiveMapObjects(rendererClass, getStringsFromAnnotationValue(ai.value("exclusiveMapObjects")));
                    internExclusiveMessageTypes(rendererClass, getStringsFromAnnotationValue(ai.value("exclusiveMessageTypes")));
                }
            });
        });
    }

    /**
     * Given an annotation value, which may be {@code null}, return it as an array of strings.  Return an empty strings if the
     * value is {@code null}.
     *
     * @param annotationValue Annotation value to process
     * @return String[] from the value or an empty array if it was {@code null}
     * @since 2.30
     */
    private String[] getStringsFromAnnotationValue(AnnotationValue annotationValue)
    {
        return annotationValue != null ? annotationValue.asStringArray() : new String[0];
    }

    /**
     * Load the specified class from the plugin's class loader and ensure that it is of the proper type and has
     * the required constructor.
     *
     * @param rendererClassName FQN of renderer class name
     * @param classLoader       Class loader to use
     * @return {@code true} If the class loaded and is usable
     */
    private Class<? extends IRenderingEngine> internRendererClass(String rendererClassName, ClassLoader classLoader)
    {
        try
        {
            final Class<?> rendererClass = classLoader.loadClass(rendererClassName);

            // Now let's validate that it smells right

            final Class<? extends IRenderingEngine> asSubclass = rendererClass.asSubclass(IRenderingEngine.class);

            rendererConstructors.put(asSubclass, asSubclass.getConstructor(IGlobeDelegate.class));

            return asSubclass;
        } catch (ClassNotFoundException e)
        {
            logger.error("Renderer class [{}] does not implement {}\nIt will not be installed as a renderer.", rendererClassName, IRenderingEngine.class.getName());
        } catch (ClassCastException e)
        {
            // This should not happen unless the index is whacked somehow
            logger.error("Failed to load renderer class: {}", rendererClassName);
        } catch (NoSuchMethodException e)
        {
            logger.error("Renderer class [{}] does not have a proper constructor.\nIt must have a constructor of the form 'public SomeRenderer(IGlobeDelegate delegate)'\nIt will not be installed as a renderer.", rendererClassName);
        }

        return null;
    }

    /**
     * Internalize the exclusive message types for a renderer.
     *
     * @param rendererClass renderer class
     * @param exclusives    Array of exclusive message type strings
     */
    private void internExclusiveMessageTypes(Class<? extends IRenderingEngine> rendererClass, String[] exclusives)
    {
        exclusiveMessageTypes.put(rendererClass, ImmutableSet.copyOf(exclusives));
    }

    /**
     * Internalize the exclusive Map Object types for a renderer.
     *
     * @param rendererClass renderer class
     * @param exclusives    Array of exclusive Map Object pattern strings
     */
    private void internExclusiveMapObjects(Class<? extends IRenderingEngine> rendererClass, String[] exclusives)
    {
        final List<FamilyAndType> exclusiveList = Stream.of(exclusives)
                .filter(s -> {
                    final int count = StringUtils.countMatches(s, SEP); // Ensure exactly 1 '/'
                    if (count != 1)
                    {
                        logger.warn("Renderer {} specified an illegal exclusive Map Object pattern: '{}'", rendererClass, s);
                        return false;
                    } else
                    {
                        return true;
                    }
                })
                .map(FamilyAndType::new)
                .collect(Collectors.toList());

        exclusiveMapObjects.put(rendererClass, exclusiveList);
    }

    /**
     * Holder of a family and type pattern.
     */
    private static final class FamilyAndType
    {
        private final String family;
        private final String type;

        /**
         * Split the given string on SEP and store the family and type elements accordingly.  Properly internalize
         * the WILDCARD string so we can use {@code ==} instead of {@code .equals()} in our match tests.
         *
         * @param pattern Family/type to intern
         */
        private FamilyAndType(String pattern)
        {
            final String[] parts = pattern.split(SEP);

            family = parts[0].equals(WILDCARD) ? WILDCARD : parts[0];
            type = parts[1].equals(WILDCARD) ? WILDCARD : parts[1];
        }

        /**
         * Determine if the given family and type match the configured family and type, allowing for wildcards
         * in our configuration.  The matching rules are:
         * <ol>
         * <li>If family is wildcard ("*"), return true</li>
         * <li>If type is wildcard and family matches the MO family, return true</li>
         * <li>If neither is wildcard, return true if type and family match MO type and family</li>
         * </ol>
         *
         * @param family Family to test
         * @param type   type to test
         * @return {@code true} if the configuration matches the given family/type
         */
        @SuppressWarnings("StringEquality") // Since we always configure using WILDCARD
        private boolean matches(String family, String type)
        {
            if (this.family == WILDCARD) return true;
            if (this.type == WILDCARD) return this.family.equals(family);
            return this.family.equals(family) && this.type.equals(type);
        }
    }
}
