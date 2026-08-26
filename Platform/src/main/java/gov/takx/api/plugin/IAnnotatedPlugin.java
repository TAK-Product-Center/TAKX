package gov.takx.api.plugin;

import gov.takx.api.plugin.annotations.PluginDescriptor;
import gov.takx.api.plugin.annotations.PluginSpecification;
import gov.takx.platform.KeyUtils;

import java.lang.annotation.Annotation;

/**
 * Contract for declaring the annotation type for a plugin.  Default implementations for all the methods that pull
 * data from the {@link PluginSpecification} and {@link PluginDescriptor} are provided.
 *
 * @since 3.0
 */
public interface IAnnotatedPlugin<A extends Annotation> extends IRaptorPlugin
{
    /**
     * @return The annotation from the implementing class
     */
    A getPluginAnnotation();

    @Override
    default String getFamily()
    {
        return getPluginSpecification().family();
    }

    @Override
    default String getType()
    {
        return getPluginSpecification().type();
    }

    @Override
    default String getUniqueKey()
    {
        return KeyUtils.getUniqueKey(getFamily(), getType());
    }

    @Override
    default String getName()
    {
        return getPluginDescriptor().name();
    }

    @Override
    default String getVersion()
    {
        return getPluginSpecification().version();
    }

    @Override
    default String getIconPath()
    {
        return getPluginSpecification().iconPath();
    }

    @Override
    default String getDescription()
    {
        return getPluginDescriptor().description();
    }

    @Override
    default String getLongDescription()
    {
        return getPluginDescriptor().longDescription();
    }

    @Override
    default String getCopyrightNotice()
    {
        return getPluginDescriptor().copyright();
    }

    @Override
    default String getVendor()
    {
        return getPluginDescriptor().vendor();
    }

    /**
     * @return The {@link PluginDescriptor} annotation
     */
    PluginDescriptor getPluginDescriptor();

    /**
     * @return The {@link PluginSpecification} annotation
     */
    PluginSpecification getPluginSpecification();
}
