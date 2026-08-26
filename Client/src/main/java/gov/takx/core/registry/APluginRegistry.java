package gov.takx.core.registry;

import com.google.common.collect.ImmutableList;
import gov.takx.api.event.TestProjectDestroyed;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.registry.IPluginRegistry;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.event.Observes;
import org.jboss.weld.proxy.WeldClientProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Abstract base class for plugin registries, recording plugin definitions of a given plugin category.
 *
 * @see PluginCategory
 * @see IPluginDefinition
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
public abstract class APluginRegistry<T extends IRaptorPlugin, PD extends IPluginDefinition<?>> implements IPluginRegistry<T, PD>
{
    @SuppressWarnings("NonConstantLogger")
    private final Logger logger;

    private final PluginCategory pluginCategory;
    private final boolean requireIcon;

    final Map<String, IPluginDefinition<?>> pluginDefinitions = new ConcurrentHashMap<>();

    /**
     * Construct the connection plugin registry.  All plugins added will be tested to ensure they define an icon in
     * the metadata.
     *
     * @param pluginCategory The plugin category of this registry
     */
    protected APluginRegistry(PluginCategory pluginCategory)
    {
        this(pluginCategory, true);
    }

    /**
     * Construct the connection plugin registry.
     *
     * @param pluginCategory The plugin category of this registry
     * @param requireIcon    If {@code true} any plugin added will be tested to ensure it has defined a custom icon
     */
    protected APluginRegistry(PluginCategory pluginCategory, boolean requireIcon)
    {
        logger = LoggerFactory.getLogger(getClass()); // Use the logger of our subtype

        this.pluginCategory = pluginCategory;
        this.requireIcon = requireIcon;
    }

    @Override
    public PluginCategory getPluginCategory()
    {
        return pluginCategory;
    }

    @Override
    public Optional<PD> getPluginDefinition(@Nonnull String family, @Nonnull String type)
    {
        return Optional.ofNullable(cast(pluginDefinitions.get(KeyUtils.getUniqueKey(family, type))));
    }

    @Override
    public PD getPluginDefinition(@Nonnull T plugin)
    {
        return cast(pluginDefinitions.get(plugin.getUniqueKey()));
    }

    @Override
    public T getPlugin(@Nonnull String family, @Nonnull String type)
    {
        if (logger.isDebugEnabled()) logger.debug("Trying to find plugin for {}, {}", family, type);

        final Optional<PD> pluginDefinition = getPluginDefinition(family, type);

        return pluginDefinition.map(pd -> cast(pd.getInstance())).orElse(null);
    }

    @Override
    public Collection<T> getPlugins()
    {
        return pluginDefinitions.values().stream()
                .map(pd -> cast(pd.getInstance()))
                .filter(Objects::nonNull) // Avoid plugins that failed to deploy or produce no instance
                .collect(Collectors.toList());
    }

    @Override
    public Collection<PD> getPluginDefinitions()
    {
        //noinspection unchecked
        return (Collection<PD>) ImmutableList.copyOf(pluginDefinitions.values());
    }

    @Override
    public boolean exists(@Nonnull String family, @Nonnull String type, @Nonnull String version)
    {
        final Optional<PD> pluginDefinition = getPluginDefinition(family, type);

        return pluginDefinition.isPresent() && version.equals(pluginDefinition.get().getVersion());
    }

    @Override
    public boolean exists(@Nonnull String family, @Nonnull String type)
    {
        return getPluginDefinition(family, type).isPresent();
    }

    @Override
    public File getPluginSource(@Nonnull T plugin)
    {
        return getPluginDefinition(plugin).getPluginSource();
    }

    @Override
    public synchronized boolean addPlugin(@Nonnull IPluginDefinition<?> pluginDefinition)
    {
        final String uniqueKey = pluginDefinition.getUniqueKey();

        logger.debug("Add PD for {}", pluginDefinition);

        if (isNewerVersionRegistered(pluginDefinition))
        {
            logger.debug("Skip registration due to newer plugin");
            return false; // Skip registration if a newer one is already registered
        }

        if (requireIcon && pluginDefinition.getIconURI().isEmpty())
        {
            logger.warn("{} {} fails to define a valid icon path [{}]; a generic icon will be used on the toolbar", getPluginCategory(), uniqueKey, pluginDefinition.getIconPath());
        }

        pluginDefinitions.put(uniqueKey, pluginDefinition);
        return true;
    }

    /**
     * Determine if a newer version of the given plugin is already registered.
     *
     * @param pluginDefinition The plugin definition to test
     * @return {@code true} if a newer version is already registered
     */
    protected boolean isNewerVersionRegistered(IPluginDefinition<?> pluginDefinition)
    {
        boolean existingIsNewer = false;
        final String family = pluginDefinition.getFamily();
        final String type = pluginDefinition.getType();

        final Optional<PD> existingPlugin = getPluginDefinition(family, type);

        if (existingPlugin.isPresent())
        {
            // Collision, check version numbers
            final PD existingPluginDefinition = existingPlugin.get();
            final String existingVersion = existingPluginDefinition.getVersion();
            final String foundVersion = pluginDefinition.getVersion();

            existingIsNewer = foundVersion.compareTo(existingVersion) <= 0;

            logger.warn("""
                    Redefinition of plugin {}:
                        had   version {} from [{}]{},
                        found version {} from [{}]{}""",
                    pluginDefinition.getUniqueKey(),
                    existingVersion, existingPluginDefinition.getPluginSource(), existingIsNewer ? "   <== Using this one" : "",
                    foundVersion, pluginDefinition.getPluginSource(), !existingIsNewer ? "   <== Using this one" : "");
        }

        return existingIsNewer;
    }

    @Override
    public Collection<T> getPlugins(@Nonnull File jarFile)
    {
        return pluginDefinitions.values().stream()
                .filter(pd -> jarFile.equals(pd.getPluginSource()))
                .map(pd -> cast(pd.getInstance()))
                .collect(Collectors.toList());
    }

    @Override
    public ClassLoader getPluginClassLoader(@Nonnull String family, @Nonnull String type)
    {
        // First, just see if it is one of our plugins
        final Optional<PD> pluginDefinition = getPluginDefinition(family, type);

        return pluginDefinition
                .map(IPluginDefinition::getResourceClassLoader)
                .orElse(getClass().getClassLoader());
    }

    @Override
    public void validatePlugins()
    {
        pluginDefinitions.entrySet().removeIf(entry -> {
            final IPluginDefinition<?> pluginDefinition = entry.getValue();
            try
            {
                final IRaptorPlugin instance = pluginDefinition.getInstance();
                if (instance instanceof WeldClientProxy proxy) proxy.getMetadata().getContextualInstance();
            } catch (Exception e)
            {
                logger.error("Failed to instantiate {} {} version {}; plugin will be excluded!",
                        pluginDefinition.getPluginCategory(), pluginDefinition.getPluginClass(), pluginDefinition.getVersion(), e);
                return true;
            }
            return false;
        });
    }

    /**
     * Cast an arbitrary plugin to the required type of this registry.
     *
     * @param plugin plugin to cast
     * @return properly typed plugin
     */
    protected T cast(IRaptorPlugin plugin)
    {
        //noinspection unchecked
        return (T) plugin;
    }

    /**
     * Cast an arbitrary plugin definition to the required type of this registry.
     *
     * @param pluginDefinition plugin definition to cast
     * @return properly typed plugin
     */
    protected PD cast(IPluginDefinition<?> pluginDefinition)
    {
        //noinspection unchecked
        return (PD) pluginDefinition;
    }

    /**
     * Reset to clean state.
     */
    protected void reset(@SuppressWarnings("UnusedParameters") @Observes TestProjectDestroyed event)
    {
        pluginDefinitions.clear();
    }
}
