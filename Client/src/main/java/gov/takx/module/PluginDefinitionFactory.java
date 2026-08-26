package gov.takx.module;

import gov.takx.api.annotation.PluginDocumentation;
import gov.takx.api.mapobject.MapObjectGroupNames;
import gov.takx.api.mapobject.MapObjectOptions;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IPluginDocumentation;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.plugin.PluginAnnotationUtils;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.annotations.AppPlugin;
import gov.takx.api.plugin.annotations.ConnectionPlugin;
import gov.takx.api.plugin.annotations.DevicePlugin;
import gov.takx.api.plugin.annotations.PluginDescriptor;
import gov.takx.api.plugin.annotations.PluginSpecification;
import gov.takx.api.plugin.app.IAppPlugin;
import gov.takx.api.plugin.app.IAppPluginDefinition;
import gov.takx.api.plugin.app.IAppPluginManager;
import gov.takx.api.plugin.connection.IConnectionPlugin;
import gov.takx.api.plugin.connection.IConnectionPluginDefinition;
import gov.takx.api.plugin.device.IDevicePlugin;
import gov.takx.api.plugin.device.IDevicePluginController;
import gov.takx.api.plugin.device.IDevicePluginDefinition;
import gov.takx.api.plugin.io.IImportExportPlugin;
import gov.takx.api.plugin.io.IImportExportPluginDefinition;
import gov.takx.api.plugin.synthetic.ISyntheticPluginDefinition;
import gov.takx.bootstrap.Verification;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.utils.PlatformVersion;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.plugin.DefaultDevicePluginController;
import gov.takx.core.plugin.synthetic.SyntheticPluginDefinition;
import gov.takx.core.registry.PluginInitializer;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;
import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static gov.takx.module.DeploymentAttachments.CONTAINER;

/**
 * Factory to create concrete instances of {@link IPluginDefinition} from data extracted
 * from discovered plugins.
 *
 * @since 2.0.0
 */
@ApplicationScoped
public class PluginDefinitionFactory
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // The default icon image if a plugin doesn't define one or the one defined can't be found/read
    private static final BufferedImage DEFAULT_ICON_IMAGE = ImageUtils.getDefaultImage();

    // Map from plugin category to the annotation that defines it.
    private static final Map<PluginCategory, IDefinitionCreator> CONSTRUCTOR_FOR_CATEGORY = new EnumMap<>(PluginCategory.class);

    static
    {
        CONSTRUCTOR_FOR_CATEGORY.put(PluginCategory.AppPlugin, AppPluginDefinition::new);
        CONSTRUCTOR_FOR_CATEGORY.put(PluginCategory.ConnectionPlugin, ConnectionPluginDefinition::new);
        CONSTRUCTOR_FOR_CATEGORY.put(PluginCategory.DevicePlugin, DevicePluginDefinition::new);
        CONSTRUCTOR_FOR_CATEGORY.put(PluginCategory.ImportExportPlugin, ImportExportPluginDefinition::new);
    }

    @Inject
    private IPluginModuleManager pluginModuleManager;

    @Inject
    private PluginInitializer pluginInitializer;

    /**
     * Functional interface used for plugin definition constructors.
     */
    @FunctionalInterface
    private interface IDefinitionCreator
    {
        IPluginDefinition<?> create(Class<? extends IRaptorPlugin> pluginClass, File pluginSource);
    }

    @PostConstruct
    private void postConstruct()
    {
        PluginDefinition.setPluginModuleManager(pluginModuleManager);
        PluginDefinition.setPluginInitializer(pluginInitializer);
    }

    /**
     * Construct a plugin definition from the given data.
     *
     * @param pluginCategory Plugin category
     * @param pluginClass    The class defining the plugin
     * @param pluginSource   File in which plugin was discovered
     * @return Plugin definition
     */
    public IPluginDefinition<?> definitionFor(PluginCategory pluginCategory, Class<? extends IRaptorPlugin> pluginClass, File pluginSource)
    {
        return makeDefinitionFor(pluginCategory, pluginClass, pluginSource);
    }

    /**
     * Support for internal testing.  Create a plugin definition from a given plugin class.
     *
     * @param pluginClass Plugin class to process
     * @return Plugin definition
     * @since 2.19
     */
    public IPluginDefinition<?> definitionFor(Class<? extends IRaptorPlugin> pluginClass)
    {
        return makeDefinitionFor(PluginCategory.getCategoryForPlugin(pluginClass), pluginClass, null);
    }

    /**
     * Construct a synthetic (device) plugin definition using the supplied values.  It will be given a group of
     * DEVICES and a default icon.
     *
     * @param family Family
     * @param type   Type
     * @return Newly minted synthetic plugin definition
     * @since 2.11
     */
    @Nonnull
    public ISyntheticPluginDefinition syntheticDeviceDefinitionFor(@Nonnull String family, @Nonnull String type)
    {
        return syntheticDefinitionFor(family, type, MapObjectGroupNames.DEVICES_GROUP,
                ImageUtils.DEFAULT_IMAGE_URI, MapObjectOptions.STANDARD_DEVICE_OPTIONS,
                unitId -> new DefaultDevicePluginController(family, type, unitId));
    }

    /**
     * Construct a synthetic plugin definition using the supplied values.
     *
     * @param family              Family
     * @param type                Type
     * @param group               Group
     * @param imageURI            URI of icon
     * @param mapObjectOptions    Map object options to apply
     * @param controllerGenerator Function to generate the device plugin controller from a unit Id, or {@code null} to use
     *                            default controller
     * @return Newly minted synthetic plugin definition
     * @since 3.0
     */
    @Nonnull
    public ISyntheticPluginDefinition syntheticDefinitionFor(@Nonnull String family, @Nonnull String type,
                                                             @Nonnull String group, @Nullable URI imageURI,
                                                             @Nullable MapObjectOptions mapObjectOptions,
                                                             @Nullable Function<String, IDevicePluginController> controllerGenerator)
    {
        return new SyntheticPluginDefinition(family, type, group, imageURI, null, mapObjectOptions, controllerGenerator);
    }

    /**
     * Internal method to construct a plugin definition.
     *
     * @param pluginCategory Plugin category
     * @param pluginClass    The class defining the plugin
     * @param pluginSource   File in which plugin was discovered
     * @return Plugin definition
     */
    private IPluginDefinition<?> makeDefinitionFor(PluginCategory pluginCategory, Class<? extends IRaptorPlugin> pluginClass, File pluginSource)
    {
        return CONSTRUCTOR_FOR_CATEGORY.get(pluginCategory).create(pluginClass, pluginSource);
    }

    /**
     * Read in the defined plugin icon.  Return a default if it isn't defined or can't be read.
     *
     * @param pluginDefinition Plugin definition to process
     * @return icon image defined by plugin, or a default
     */
    private static <T extends IRaptorPlugin> BufferedImage getIconForPath(IPluginDefinition<T> pluginDefinition, String path)
    {
        if (StringUtils.isEmpty(path))
        {
            return DEFAULT_ICON_IMAGE;
        }

        final URL url = pluginDefinition.getResourceClassLoader().getResource(path);
        if (url == null)
        {
            logger.warn("Failed to load icon for plugin [{}] at path '{}'", pluginDefinition.getUniqueKey(), path);
            return DEFAULT_ICON_IMAGE;
        }

        return UIResources.getImage(url);
    }

    private static final class AppPluginDefinition extends PluginDefinition<IAppPlugin> implements IAppPluginDefinition
    {
        private final AppPlugin appPluginAnnotation;

        @SuppressWarnings("unchecked")
        AppPluginDefinition(Class<? extends IRaptorPlugin> pluginClass, File pluginSource)
        {
            super(PluginCategory.AppPlugin, (Class<IAppPlugin>) pluginClass, pluginSource);

            appPluginAnnotation = pluginClass.getAnnotation(AppPlugin.class);
        }

        @Override
        @Nonnull
        public IAppPluginManager.AutoStartMode getAutoStartMode()
        {
            return appPluginAnnotation.autoStartMode();
        }
    }

    private static final class ConnectionPluginDefinition extends PluginDefinition<IConnectionPlugin> implements IConnectionPluginDefinition
    {
        private final ConnectionPlugin connectionPluginAnnotation;

        @SuppressWarnings("unchecked")
        ConnectionPluginDefinition(Class<? extends IRaptorPlugin> pluginClass, File pluginSource)
        {
            super(PluginCategory.ConnectionPlugin, (Class<IConnectionPlugin>) pluginClass, pluginSource);

            connectionPluginAnnotation = pluginClass.getAnnotation(ConnectionPlugin.class);

            if (connectionPluginAnnotation.maxSupportedConnections() < 1)
            {
                throw new DeploymentException("Connection plugin [" + getUniqueKey() + "] declares 0 max connections");
            }
        }

        @Override
        public boolean isAutoReconnectSupported()
        {
            return connectionPluginAnnotation.autoReconnectSupported();
        }

        @Override
        public boolean isManualCreationAllowed()
        {
            return connectionPluginAnnotation.manualCreationAllowed();
        }

        @Override
        public String getPanelIconPath()
        {
            // If the panelIconPath is not specified in the plugin annotation, default to the icon path
            String panelIconPath = connectionPluginAnnotation.panelIconPath();
            if (panelIconPath.isEmpty())
            {
                panelIconPath = connectionPluginAnnotation.spec().iconPath();
            }
            return panelIconPath;
        }

        @Override
        public BufferedImage getPanelIcon()
        {
            return getIconForPath(this, getPanelIconPath());
        }

        @Override
        public boolean isEditSupported()
        {
            return connectionPluginAnnotation.editSupported();
        }

        @Override
        public int getMaxSupportedConnections()
        {
            return connectionPluginAnnotation.maxSupportedConnections();
        }
    }

    private static final class DevicePluginDefinition extends PluginDefinition<IDevicePlugin> implements IDevicePluginDefinition
    {
        private final DevicePlugin devicePluginAnnotation;
        private final MapObjectOptions mapObjectOptions;

        @SuppressWarnings("unchecked")
        DevicePluginDefinition(Class<? extends IRaptorPlugin> pluginClass, File pluginSource)
        {
            super(PluginCategory.DevicePlugin, (Class<IDevicePlugin>) pluginClass, pluginSource);

            devicePluginAnnotation = pluginClass.getAnnotation(DevicePlugin.class);
            mapObjectOptions = new MapObjectOptions(devicePluginAnnotation.mapObjectOptions());
        }

        @Override
        public boolean isAutoGenerateUnitId()
        {
            return devicePluginAnnotation.autoGenerateUnitId();
        }

        @Override
        public boolean isManualCreationAllowed()
        {
            return devicePluginAnnotation.manualCreationAllowed();
        }

        @Override
        public boolean canBeContained()
        {
            return devicePluginAnnotation.canBeContained();
        }

        @Override
        public String getDefaultPreferencesTab()
        {
            return devicePluginAnnotation.defaultPreferencesTab();
        }

        @Override
        public String getCommandStructureXml()
        {
            return devicePluginAnnotation.commandStructureXml();
        }

        @Override
        public String getDataStructureXml()
        {
            return devicePluginAnnotation.dataStructureXml();
        }

        @Override
        public String[] getExcludeFromDeviceLog()
        {
            return devicePluginAnnotation.excludeFromDeviceLog();
        }

        @Override
        public String getDefaultDeviceLogMessage()
        {
            return devicePluginAnnotation.defaultDeviceLogMessage();
        }

        @Override
        public MapObjectOptions getMapObjectOptions()
        {
            return mapObjectOptions;
        }

        @Override
        public String getFileImporterKey()
        {
            return devicePluginAnnotation.fileImporterKey();
        }
    }

    private static final class ImportExportPluginDefinition extends PluginDefinition<IImportExportPlugin> implements IImportExportPluginDefinition
    {
        @SuppressWarnings("unchecked")
        ImportExportPluginDefinition(Class<? extends IRaptorPlugin> pluginClass, File pluginSource)
        {
            super(PluginCategory.ImportExportPlugin, (Class<IImportExportPlugin>) pluginClass, pluginSource);
        }
    }

    /**
     * Abstract plugin definition.
     */
    private static abstract class PluginDefinition<T extends IRaptorPlugin> implements IPluginDefinition<T>
    {
        private static IPluginModuleManager pluginModuleManager;
        protected static PluginInitializer pluginInitializer;

        protected final PluginCategory pluginCategory;
        protected Class<T> pluginClass;
        protected final PluginDescriptor descriptor;
        protected final PluginSpecification spec;
        protected final File pluginSource;
        protected final String uniqueKey;
        protected final String group;
        protected BufferedImage image;
        protected PluginDocumentationImpl[] pluginDoc;

        PluginDefinition(PluginCategory pluginCategory, Class<T> pluginClass, File pluginSource)
        {
            this.pluginCategory = pluginCategory;
            this.pluginClass = pluginClass;
            this.pluginSource = pluginSource;

            final Class<? extends Annotation> annotationClass = pluginCategory.getAnnotationClass();

            descriptor = PluginAnnotationUtils.extractPluginDescriptor(pluginClass.getAnnotation(annotationClass));
            spec = PluginAnnotationUtils.extractPluginSpecification(pluginClass.getAnnotation(annotationClass));

            uniqueKey = KeyUtils.getUniqueKey(spec.family(), spec.type());

            group = pluginCategory == PluginCategory.DevicePlugin
                    ? MapObjectGroupNames.DEVICES_GROUP
                    : pluginCategory.toString();

            pluginDoc = Arrays.stream(descriptor.documentation())
                    .map(PluginDocumentationImpl::new).toArray(PluginDocumentationImpl[]::new);
        }

        /**
         * Set the plugin module manager, making it accessible in a static context for these internal classes.
         */
        private static void setPluginModuleManager(IPluginModuleManager pluginModuleManager)
        {
            PluginDefinition.pluginModuleManager = pluginModuleManager;
        }

        private static void setPluginInitializer(PluginInitializer pluginInitializer)
        {
            PluginDefinition.pluginInitializer = pluginInitializer;
        }

        /**
         * Get the instance from the CDI container.  Just retrieve it from the container, allowing it to deal
         * with the instance as needed. Don't retain a reference to the plugin.
         */
        @SuppressWarnings({"OptionalGetWithoutIsPresent"})
        @Override
        public T getInstance()
        {
            final PluginDeployment deployment = pluginModuleManager.findDeploymentForPlugin(pluginClass).get();
            final WeldContainer container = deployment.getAttachment(CONTAINER);

            // In certain test scenarios, the container will be null
            if (container == null) return null;

            try
            {
                // Need to use an @Any qualifier in case the plugin class also has a qualifying annotation on it.
                return container.select(pluginClass, Any.Literal.INSTANCE).get();
            } catch (Throwable e)
            {
                logger.error("Failed to obtain instance of {} from {}", pluginClass, deployment.getModuleIdentifier(), e);
                return null;
            }
        }

        @Override
        public String getFamily()
        {
            return spec.family();
        }

        @Override
        public String getType()
        {
            return spec.type();
        }

        @Override
        public Verification getVerification()
        {
            return pluginModuleManager.findDeploymentForPlugin(pluginClass).get().getAttachment(DeploymentAttachments.VERIFICATION);
        }

        @Override
        public String getVersion()
        {
            return spec.version();
        }

        @Override
        public boolean isExportControlled()
        {
            return spec.exportControlled();
        }

        @Override
        @Nullable
        public PlatformVersion getMinimumSupportedVersion()
        {
            try
            {
                return spec.minimumSupportedVersion().getVersion();
            } catch (Exception e)
            {
                logger.error("Failed to get minimum required version for plugin {}", getUniqueKey(), e);
                return null;
            }
        }

        @Override
        public String getCopyrightNotice()
        {
            return descriptor.copyright();
        }

        @Override
        public String getDescription()
        {
            return descriptor.description();
        }

        @Override
        public String getLongDescription()
        {
            return descriptor.longDescription();
        }

        @Override
        public String getName()
        {
            return descriptor.name();
        }

        @Override
        public String getVendor()
        {
            return descriptor.vendor();
        }

        @Override
        public PluginCategory getPluginCategory()
        {
            return pluginCategory;
        }

        @Override
        public Class<? extends IRaptorPlugin> getPluginClass()
        {
            return pluginClass;
        }

        @Override
        public File getPluginSource()
        {
            return pluginSource;
        }

        @Override
        public String getUniqueKey()
        {
            return uniqueKey;
        }

        @Override
        public Optional<String> getIconPath()
        {
            return Optional.of(spec.iconPath());
        }

        @Override
        public Optional<URI> getIconURI()
        {
            final String path = getIconPath().orElse(null);
            if (path == null) return Optional.empty();

            try
            {
                final URL url = getResourceClassLoader().getResource(path);
                if (url != null)
                {
                    return Optional.of(url.toURI());
                } else
                {
                    logger.warn("Failed to locate icon for plugin [{}] at path '{}'", getUniqueKey(), path);
                }
            } catch (Exception e)
            {
                logger.error("Error getting URI for: {}", path, e);
            }
            return Optional.empty();
        }

        @Override
        public BufferedImage getIcon()
        {
            if (image == null)
            {
                final Optional<String> iconPath = getIconPath();
                image = iconPath.isPresent() ? getIconForPath(this, iconPath.get()) : DEFAULT_ICON_IMAGE;
            }
            return image;
        }

        @Override
        public String getGroup()
        {
            return group;
        }

        @Override
        public IPluginDocumentation[] getPluginDocumentation()
        {
            return pluginDoc.clone();
        }

        @Override
        public String toString()
        {
            return "PluginDefinition{" +
                    "pluginCategory=" + pluginCategory +
                    ", pluginClass=" + pluginClass.getSimpleName() +
                    ", descriptor=" + descriptor +
                    ", spec=" + spec +
                    '}';
        }
    }

    /**
     * A basic implementation of the {@link IPluginDocumentation} interface.
     *
     * @since 2.26 (renamed in 3.0)
     */
    private static class PluginDocumentationImpl implements IPluginDocumentation
    {
        PluginDocumentation pluginDoc;

        /**
         * Constructor that uses the {@link PluginDocumentation} annotation.
         */
        PluginDocumentationImpl(PluginDocumentation documentation)
        {
            pluginDoc = documentation;
        }

        @Override
        public String getDisplayName()
        {
            return pluginDoc.displayName();
        }

        @Override
        public String getTooltip()
        {
            return pluginDoc.tooltip();
        }

        @Override
        public String getFilePath()
        {
            return pluginDoc.filePath();
        }

        @Override
        public boolean isExternal()
        {
            return pluginDoc.external();
        }

        @Override
        public boolean isHtmlDoc()
        {
            return pluginDoc.htmlDoc();
        }

        @Override
        public String getIconPath()
        {
            return pluginDoc.iconPath();
        }

        @Override
        public String toString()
        {
            return "PluginDocumentationImpl{" +
                    "pluginDoc=" + pluginDoc +
                    '}';
        }
    }
}
