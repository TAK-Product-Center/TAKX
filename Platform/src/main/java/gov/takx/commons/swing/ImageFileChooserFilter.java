package gov.takx.commons.swing;

import gov.takx.commons.file.FileUtils;
import jakarta.annotation.Nonnull;

import javax.swing.filechooser.FileFilter;

import java.io.File;

/**
 * Accepts all directories and all gif, jpg, tiff, png, or bmp files.
 */
public class ImageFileChooserFilter extends FileFilter
{
    @Override
    public boolean accept(@Nonnull File f)
    {
        if (f.isDirectory()) return true;

        String extension = FileUtils.getExtension(f);
        return extension != null && (extension.equals("tiff") ||
                extension.equals("tif") ||
                extension.equals("gif") ||
                extension.equals("jpeg") ||
                extension.equals("jpg") ||
                extension.equals("png") ||
                extension.equals("bmp"));
    }

    @Override
    @Nonnull
    public String getDescription()
    {
        return "tiff, tif, gif, jpeg, jpg, png, bmp";
    }
}
