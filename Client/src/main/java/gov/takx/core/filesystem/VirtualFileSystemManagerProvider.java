package gov.takx.core.filesystem;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.annotations.ForPlugin;
import gov.takx.platform.utils.InjectionUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Simple bean to provide instances of VFS managers.
 *
 * @since 2.0.0
 */
@InjectableService
@ApplicationScoped
public class VirtualFileSystemManagerProvider
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    @VFSManagerScope(VFSScope.Global)
    private IVirtualFileSystemManager globalVFSManager;

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;

    /**
     * Produce the VFS in GLOBAL scope for the plugin identified in the {@link ForPlugin} annotation on the injection
     * point.  This is only supported for the following plugin categories:
     * <ul>
     *     <li>{@link PluginCategory#AppPlugin}</li>
     *     <li>{@link PluginCategory#ConnectionPlugin}</li>
     *     <li>{@link PluginCategory#ImportExportPlugin}</li>
     * </ul>
     * <p>
     * Note: the {@link ForPlugin} annotation values are non-binding, so the values used on this producer method
     * are just placeholder values and are not meant to imply that this method is only supported for apps.
     *
     * @return VFS for plugin
     * @since 3.0
     */
    @Produces
    @VFSManagerScope(VFSScope.Global)
    @ForPlugin(family = "", type = "", category = PluginCategory.AppPlugin)
    private IVirtualFileSystem produceGlobalVfsForPlugin(InjectionPoint injectionPoint)
    {
        return getVfsForPlugin(injectionPoint, globalVFSManager);
    }

    /**
     * Produce the VFS in PROJECT scope for the plugin identified in the {@link ForPlugin} annotation on the injection
     * point.  This is only supported for the following plugin categories:
     * <ul>
     *     <li>{@link PluginCategory#AppPlugin}</li>
     *     <li>{@link PluginCategory#ConnectionPlugin}</li>
     *     <li>{@link PluginCategory#ImportExportPlugin}</li>
     * </ul>
     * <p>
     * Note: the {@link ForPlugin} annotation values are non-binding, so the values used on this producer method
     * are just placeholder values and are not meant to imply that this method is only supported for apps.
     *
     * @return VFS for plugin
     * @since 3.0
     */
    @Produces
    @VFSManagerScope(VFSScope.Project)
    @ForPlugin(family = "", type = "", category = PluginCategory.AppPlugin)
    private IVirtualFileSystem produceProjectVfsForPlugin(InjectionPoint injectionPoint)
    {
        return getVfsForPlugin(injectionPoint, projectVFSManager);
    }

    /**
     * Obtain the VFS for the plugin specified in the {@link ForPlugin} annotation on the given injection point.
     *
     * @param injectionPoint Injection point to process
     * @param vfsManager     VFS Manager (global or project) to use to obtain the VFS
     * @return VFS for the given plugin category, family, and type
     * @since 3.0
     */
    private IVirtualFileSystem getVfsForPlugin(InjectionPoint injectionPoint, IVirtualFileSystemManager vfsManager)
    {
        final ForPlugin forPluginAnnotation = InjectionUtils.getAnnotation(injectionPoint, ForPlugin.class)
                .orElseThrow(() -> new IllegalStateException("No @ForPlugin on: " + injectionPoint));

        final PluginCategory pluginCategory = forPluginAnnotation.category();
        final String family = forPluginAnnotation.family();
        final String type = forPluginAnnotation.type();

        switch (pluginCategory)
        {
            case AppPlugin:
                return vfsManager.getVirtualFileSystemForApp(family, type);
            case ConnectionPlugin:
                return vfsManager.getVirtualFileSystemForConnection(family, type);
            case ImportExportPlugin:
                return vfsManager.getVirtualFileSystemForImportExport(family, type);
            default:
                logger.error("Cannot produce VFS for plugin category [{}] on {}", pluginCategory, injectionPoint);
                return null;
        }
    }

    /**
     * Obtain a VFS manager for the given scope.
     *
     * @param scope Scope of interest
     * @return VFS Manager for that scope
     */
    public IVirtualFileSystemManager getVirtualFileSystemManager(VFSScope scope)
    {
        switch (scope)
        {
            case Project:
                return projectVFSManager;
            case Global:
                return globalVFSManager;
            default:
                logger.error("Unknown VFSScope: {}", scope, new RuntimeException());
                return null;
        }
    }
}
