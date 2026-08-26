package gov.takx.commons.swing;

import gov.takx.commons.file.FileUtils;
import jakarta.annotation.Nonnull;

import javax.swing.filechooser.FileFilter;

import java.io.File;

/**
 * Accepts files of a single extension.
 *
 * @since 5.0
 */
public class SingleTypeFileFilter extends FileFilter
{
    private final String extension;

    /**
     * @param extension Extension type to allow.
     */
    public SingleTypeFileFilter(@Nonnull String extension)
    {
        this.extension = extension;
    }

    @Override
    public boolean accept(@Nonnull File f)
    {
        if (f.isDirectory()) return false;

        String ext = FileUtils.getExtension(f);
        return extension.equalsIgnoreCase(ext);
    }

    @Override
    @Nonnull
    public String getDescription()
    {
        return extension;
    }
}
