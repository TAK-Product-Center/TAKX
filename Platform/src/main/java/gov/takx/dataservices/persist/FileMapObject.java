package gov.takx.dataservices.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.filesystem.IVFSFile;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.mapobject.IFileMapObject;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.commons.file.FilePath;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.ImageUtils;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Map object that represents a file.
 *
 */
@SuppressWarnings("JpaEntityListenerInspection")
@Internal
@PseudoBean
@Entity
public class FileMapObject extends IconMapObject implements IFileMapObject
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String namespace;
    private String relativePath;

    @Transient
    private IVFSFile vfsFile;

    /**
     * No-arg constructor for Hibernate.
     */
    protected FileMapObject()
    {
        setAllowDeviceToBeContainer(false);
    }

    public FileMapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(mapObjectSpec, pluginDefinition);

        setAllowDeviceToBeContainer(false);
    }

    /**
     * Reconstruct our underlying VFS File once we're loaded.
     *
     * @since 3.0
     */
    @Override
    @PostLoad
    protected void onLoad()
    {
        super.onLoad();

        vfsFile = new VFSFile(projectVFSManager, getFileSystemNamespace(), relativePath);
    }

    @Override
    public IVFSFile getVfsFile()
    {
        return vfsFile;
    }

    @Override
    public FilePath getFilePath()
    {
        return new FilePath(relativePath);
    }

    @Override
    public IVirtualFileSystem getVfs()
    {
        return projectVFSManager.getVirtualFileSystem(namespace);
    }

    @Override
    protected BufferedImage loadMainIcon()
    {
        // Need to test for null here because a call to this method occurs during initial construction,
        // which is before the call to setFile happens.

        return vfsFile != null
                ? FileUtils.getSystemIconImage(vfsFile.getFile())
                : ImageUtils.getDefaultImage();
    }

    /**
     * "private" method to support construction of File Map Objects.  There is no way to pass specific arguments
     * to a MapObject constructor, so we have to support setting the VFS File after construction, but we don't want
     * this in the interface.
     *
     * @param vfsFile VFSFile to associate with this MO
     */
    public void setFile(@Nonnull VFSFile vfsFile)
    {
        this.vfsFile = vfsFile;

        relativePath = vfsFile.getPath();
        namespace = vfsFile.getFileSystemNamespace();

        vfsFile.fileMapObject = this;

        resetIconImage();
    }

    @Override
    public void resetIconImage()
    {
        final File file = vfsFile.getFile();
        try
        {
            final URI shellURI = new URI(IIconMapObject.PROTOCOL_SCHEME_SHELL, file.toURI().getPath(), null);
            super.setIconImage(shellURI, FileUtils.getSystemIconImage(file));
        } catch (URISyntaxException e)
        {
            logger.error("Unable to construct URL for file {}", file.getAbsolutePath(), e);
        }
    }

    @Override
    public void setLocation(ILocation location)
    {
        setLocation(location, false);
    }

    @Override
    public String toString()
    {
        return namespace + "/" + relativePath;
    }

    @Override
    public boolean isPreviewImageAvailable()
    {
        File externalFile = getExternalFile();
        return externalFile != null && ImageUtils.isImageTypeReadable(externalFile);
    }

    @Override
    public String getPreviewDescription()
    {
        String desc = getDescription();
        return desc == null ? vfsFile.getFileName() : desc;
    }

    @Override
    public File getExternalFile()
    {
        return vfsFile.getFile();
    }

    @Override
    public String getFileSystemNamespace()
    {
        return namespace;
    }

    @Override
    public void showDeviceLog()
    {
        // Do Nothing
        logger.info("Ignoring show device log request for FileMapObject");
    }

    @Override
    public void setIconImage(URI imageURI, BufferedImage image)
    {
        if (getIconImageURI() == null)
        {
            super.setIconImage(imageURI, image);
        } else
        {
            throw new UnsupportedOperationException("Icons cannot be set on FileMapObjects");
        }
    }

    @Override
    public void setIconImage(URI imageURI)
    {
        if (getIconImageURI() == null)
        {
            super.setIconImage(imageURI);
        } else
        {
            throw new UnsupportedOperationException("Icons cannot be set on FileMapObjects");
        }
    }
}
