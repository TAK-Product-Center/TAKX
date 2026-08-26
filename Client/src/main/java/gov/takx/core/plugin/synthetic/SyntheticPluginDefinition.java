package gov.takx.core.plugin.synthetic;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObjectImageUtils;
import gov.takx.api.mapobject.MapObjectGroupNames;
import gov.takx.api.mapobject.MapObjectOptions;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.device.IDevicePlugin;
import gov.takx.api.plugin.device.IDevicePluginController;
import gov.takx.api.plugin.synthetic.ISyntheticPlugin;
import gov.takx.api.plugin.synthetic.ISyntheticPluginDefinition;
import gov.takx.bootstrap.Verification;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.utils.PlatformVersion;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

/**
 * Synthetic plugin definitions are used for Map Object types that are not defined by plugins.  This includes
 * types like resource defined Map Objects (forces, vehicles, etc.).  Synthetic plugins are also manufactured
 * when a Map Object is created for which there is no defining plugin.
 *
 * @since 2.0.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@Internal
@PseudoBean
public class SyntheticPluginDefinition implements ISyntheticPluginDefinition
{
    private static final String DEFAULT_VERSION = "1.0.0";

    @Inject
    private IMapObjectImageUtils mapObjectImageUtils;

    private final String family;
    private final String type;
    private final String group;
    private final String version;
    private final URI imageURI;
    private final String key;
    private final File pluginSource;
    private final Class<? extends IRaptorPlugin> pluginClass;
    private final MapObjectOptions mapObjectOptions;
    private final Verification verification;

    private final Function<String, IDevicePluginController> controllerGenerator;
    private SyntheticPlugin instance;

    private BufferedImage image;

    /**
     * Create a synthetic plugin definition in the DEVICES group, with a default Icon URI.
     *
     * @param family  Plugin family
     * @param type    Plugin type
     * @param version Plugin (MapEntity) version
     * @return Synthetic plugin definition
     */
    @Nonnull
    public static ISyntheticPluginDefinition syntheticTestDevice(@Nonnull String family, @Nonnull String type, @Nonnull String version)
    {
        return syntheticTestDevice(family, type, version, ImageUtils.DEFAULT_IMAGE_URI);
    }

    /**
     * Create a synthetic plugin definition in the DEVICES group.
     *
     * @param family   Plugin family
     * @param type     Plugin type
     * @param version  Plugin (MapEntity) version
     * @param imageURI URI to image icon
     * @return Synthetic plugin definition
     * @since 2.11
     */
    @Nonnull
    public static ISyntheticPluginDefinition syntheticTestDevice(@Nonnull String family, @Nonnull String type,
                                                                 @Nonnull String version, @Nullable URI imageURI)
    {
        return new SyntheticPluginDefinition(family, type, MapObjectGroupNames.DEVICES_GROUP, version, imageURI,
                null, MapObjectOptions.STANDARD_DEVICE_OPTIONS, null);
    }

    public SyntheticPluginDefinition(@Nonnull String family, @Nonnull String type, @Nonnull String group, @Nullable URI imageURI,
                                     @Nullable IPluginDefinition<? extends IRaptorPlugin> sourceDefinition,
                                     @Nullable MapObjectOptions mapObjectOptions)
    {
        this(family, type, group, DEFAULT_VERSION, imageURI, sourceDefinition, mapObjectOptions, null);
    }

    public SyntheticPluginDefinition(@Nonnull String family, @Nonnull String type, @Nonnull String group, @Nullable URI imageURI,
                                     @Nullable IPluginDefinition<? extends IRaptorPlugin> sourceDefinition,
                                     @Nullable MapObjectOptions mapObjectOptions,
                                     @Nullable Function<String, IDevicePluginController> controllerGenerator)
    {
        this(family, type, group, DEFAULT_VERSION, imageURI, sourceDefinition, mapObjectOptions, controllerGenerator);
    }

    public SyntheticPluginDefinition(@Nonnull String family, @Nonnull String type, @Nonnull String group, @Nonnull String version,
                                     @Nullable URI imageURI, @Nullable IPluginDefinition<?> sourceDefinition,
                                     @Nullable MapObjectOptions mapObjectOptions,
                                     @Nullable Function<String, IDevicePluginController> controllerGenerator)
    {
        this.family = family;
        this.type = type;
        this.group = group;
        this.version = version;
        this.imageURI = imageURI;
        key = KeyUtils.getUniqueKey(family, type);
        this.mapObjectOptions = mapObjectOptions;
        this.controllerGenerator = controllerGenerator;

        if (sourceDefinition != null)
        {
            pluginSource = sourceDefinition.getPluginSource();
            pluginClass = sourceDefinition.getPluginClass();
            verification = sourceDefinition.getVerification();

        } else
        {
            pluginSource = null;
            pluginClass = ISyntheticPlugin.class;
            verification = null;
        }
    }

    @Override
    public String getFamily()
    {
        return family;
    }

    @Override
    public String getType()
    {
        return type;
    }

    @Override
    public String getGroup()
    {
        return group;
    }

    @Override
    public String getUniqueKey()
    {
        return key;
    }

    @Override
    public Verification getVerification()
    {
        return verification;
    }

    @Override
    public String getVersion()
    {
        return version;
    }

    @Override
    public PluginCategory getPluginCategory()
    {
        return PluginCategory.SyntheticPlugin;
    }

    @Override
    public Class<? extends IRaptorPlugin> getPluginClass()
    {
        return pluginClass;
    }

    @Override
    public PlatformVersion getMinimumSupportedVersion()
    {
        return PlatformVersion.Version_3_0;
    }

    @Override
    public boolean isExportControlled()
    {
        return false;
    }

    @Override
    public Optional<String> getIconPath()
    {
        return Optional.ofNullable(imageURI.getPath());
    }

    @Override
    public Optional<URI> getIconURI()
    {
        return Optional.ofNullable(imageURI);
    }

    @Override
    public BufferedImage getIcon()
    {
        return image == null ? loadIcon() : image;
    }

    private BufferedImage loadIcon()
    {
        image = ImageUtils.getDefaultImage(); // Use a default if we don't have one

        if (imageURI != null)
        {
            final var loadedImage = mapObjectImageUtils.loadImage(imageURI, getClass().getClassLoader());
            if (loadedImage != null) image = loadedImage;
        }

        return image;
    }

    @Override
    public File getPluginSource()
    {
        return pluginSource;
    }

    @Override
    public IDevicePlugin getInstance()
    {
        return instance != null
                ? instance
                : (instance = new SyntheticPlugin(family, type, version, controllerGenerator));
    }

    @Override
    public String getName()
    {
        return type;
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public String getLongDescription()
    {
        return null;
    }

    @Override
    public String getCopyrightNotice()
    {
        return null;
    }

    @Override
    public String getVendor()
    {
        return null;
    }

    @Override
    public String getCommandStructureXml()
    {
        return null;
    }

    @Override
    public String[] getExcludeFromDeviceLog()
    {
        return new String[]{};
    }

    @Override
    public String getDefaultDeviceLogMessage()
    {
        return "";
    }

    @Override
    public boolean isManualCreationAllowed()
    {
        return true;
    }

    @Override
    public boolean canBeContained()
    {
        return true;
    }

    @Override
    public boolean isAutoGenerateUnitId()
    {
        return true;
    }

    @Override
    public String getDefaultPreferencesTab()
    {
        return null;
    }

    @Override
    public String getDataStructureXml()
    {
        return null;
    }

    @Override
    public MapObjectOptions getMapObjectOptions()
    {
        return mapObjectOptions;
    }

    @Override
    public String getFileImporterKey()
    {
        return "";
    }

    @Override
    public String toString()
    {
        return "SyntheticPluginDefinition{" +
                "key='" + key + '\'' +
                ", group='" + group + '\'' +
                ", URI='" + imageURI + '\'' +
                ", " + mapObjectOptions +
                '}';
    }
}
