package gov.takx.api.plugin;

import gov.takx.api.plugin.annotations.PluginDescriptor;
import gov.takx.api.plugin.annotations.PluginSpecification;
import gov.takx.platform.utils.InjectionUtils;

import java.lang.annotation.Annotation;

/**
 * Abstract base class for all TAKX plugin implementations.
 *
 * @since 3.0, Same name as the one that's been around since 1.3, but completely refactored
 */
public abstract class ARaptorPlugin<A extends Annotation> extends ARaptorMetadata implements IRaptorPlugin
{
    private Class<A> annotationClass;
    private PluginSpecification pluginSpecification;
    private PluginDescriptor pluginDescriptor;

    // These concrete member variables have been kept for backward compatibility.
    // When possible, we should use the getter methods instead.

    protected String family;
    protected String type;
    protected String version;

    /**
     * Default constructor using an annotation on the concrete sub-class to define the plugin.
     */
    protected ARaptorPlugin(Class<A> annotationClass)
    {
        this.annotationClass = annotationClass;

        final A pluginAnnotation = getPluginAnnotation();

        setPluginSpecification(PluginAnnotationUtils.extractPluginSpecification(pluginAnnotation));
        setPluginDescriptor(PluginAnnotationUtils.extractPluginDescriptor(pluginAnnotation));
    }

    /**
     * Special constructor for synthetic plugins, which don't have a defining annotation.  They are defined
     * dynamically, and the spec and descriptor are generated from that dynamic data.
     *
     * @param pluginSpecification Specification defining the plugin
     * @param pluginDescriptor    Descriptor defining the plugin
     */
    protected ARaptorPlugin(PluginSpecification pluginSpecification, PluginDescriptor pluginDescriptor)
    {
        setPluginSpecification(pluginSpecification);
        setPluginDescriptor(pluginDescriptor);
    }

    /**
     * @return PluginSpecification from our defining annotation.
     */
    public PluginSpecification getPluginSpecification()
    {
        return pluginSpecification;
    }

    private void setPluginSpecification(PluginSpecification pluginSpecification)
    {
        this.pluginSpecification = pluginSpecification;

        family = pluginSpecification.family();
        type = pluginSpecification.type();
        version = pluginSpecification.version();
    }

    /**
     * @return PluginDescriptor from our defining annotation.
     */
    public PluginDescriptor getPluginDescriptor()
    {
        return pluginDescriptor;
    }

    private void setPluginDescriptor(PluginDescriptor pluginDescriptor)
    {
        this.pluginDescriptor = pluginDescriptor;
    }

    /**
     * @return The annotation from our sub-class
     */
    public A getPluginAnnotation()
    {
        return InjectionUtils.getAnnotationFromOriginalType(getClass(), annotationClass);
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "{family=" + getFamily() + ", type=" + getType() + "}";
    }
}
