package gov.takx.core.filesystem;

import gov.takx.api.filesystem.IVFSFile;
import gov.takx.api.mapobject.IFileMapObject;
import gov.takx.commons.file.FilePath;

import javax.swing.Icon;

import java.io.File;

/**
 * @since 1.3
 */
public class TemplateFile extends File implements IVFSFile
{
    public TemplateFile(String pathname)
    {
        super(pathname);
    }

    @Override
    public FilePath getFilePath()
    {
        return new FilePath(getPath());
    }

    @Override
    public File getFile()
    {
        return this;
    }

    @Override
    public IFileMapObject getFileMapObject()
    {
        return null; // No map object associated with this file
    }

    @Override
    public String getFileSystemNamespace()
    {
        return null;
    }

    @Override
    public int compareTo(IVFSFile file)
    {
        return getFile().compareTo(file.getFile());
    }

    @Override
    public String getFileName()
    {
        return getName();
    }

    @Override
    public FilePath getParentFilePath()
    {
        return new FilePath(getParent());
    }

    @Override
    public long getCreationDate()
    {
        throw new UnsupportedOperationException("Creation date is not tracked for template files");
    }

    @Override
    public Icon getSystemIcon()
    {
        throw new UnsupportedOperationException("Icons are not supported for template files");
    }
}
