package gov.takx.core.filesystem.managers;

import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.core.filesystem.VirtualFileSystemManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.File;

/**
 * VFS Manager for {@link VFSScope#Global}.
 *
 * @since 2.0.0
 */
@ApplicationScoped
@VFSManagerScope(VFSScope.Global)
public class GlobalVirtualFileSystemManager extends VirtualFileSystemManager
{
    private final File rootDirectory;

    @Inject
    GlobalVirtualFileSystemManager(IProgramEnvironment programEnvironment)
    {
        super(VFSScope.Global);
        rootDirectory = new File(programEnvironment.getGlobalFilesDir());
    }

    @Override
    public File getRootDirectory()
    {
        return rootDirectory;
    }
}
