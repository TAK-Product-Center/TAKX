package gov.takx.commons.swing;

import gov.takx.commons.file.FileUtils;

import javax.swing.filechooser.FileFilter;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 */
public class SwingFileFilter extends FileFilter
{
    private final List<String> filters;

    /**
     * Construct a file filter accepting the specified list of suffixes.
     *
     * @param filters Variable argument list of suffixes
     */
    public SwingFileFilter(String... filters)
    {
        this(Arrays.asList(filters));
    }

    /**
     * Construct a file filter accepting the specified list of suffixes.
     *
     * @param filters List of suffixes
     */
    public SwingFileFilter(List<String> filters)
    {
        this.filters = filters;
    }

    /**
     * Determine if a given file passes this filter.  It must have one of the suffixes in our configured list to pass.
     *
     * @param f File to test
     * @return {@code true} if the file passes
     */
    @Override
    public boolean accept(File f)
    {
        if (f.isDirectory())
        {
            return true;
        }

        String extension = FileUtils.getExtension(f);
        if (extension != null)
        {
            for (String filter : filters)
            {
                if (extension.equalsIgnoreCase(filter))
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Get the description to be used in the filter combo-box of a file chooser dialog.
     *
     * @return Description
     */
    public String getDescription()
    {
        StringBuilder description = new StringBuilder();
        for (String filter : filters)
        {
            description.append("*.").append(filter);
        }
        return description.toString();
    }
}
