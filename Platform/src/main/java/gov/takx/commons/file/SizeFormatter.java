package gov.takx.commons.file;

/**
 */
public final class SizeFormatter
{
    private static final long ONE_GIGABYTE = 1L << 30L;
    private static final long ONE_MEGABYTE = 1L << 20L;
    private static final long ONE_KILOBYTE = 1L << 10L;

    /**
     * This class is not intended to be instantiated, it only provides static methods.  This constructor
     * enforces that (and quiets PMD).
     */
    private SizeFormatter()
    {
    }

    /**
     * Format the given size into bytes, KB, MB, or GB depending on the scale of the size.
     *
     * @param bytes Size in bytes to format
     * @return Formatted size
     */
    public static String formatEstimate(long bytes)
    {
        boolean exact = false;
        String units = "B";
        double size = bytes;

        if (bytes < ONE_KILOBYTE)
        {
            exact = true;
        } else if (bytes < ONE_MEGABYTE)
        {
            size = size / ONE_KILOBYTE;
            units = "KB";
        } else if (bytes < ONE_GIGABYTE)
        {
            size = size / ONE_MEGABYTE;
            units = "MB";
        } else
        {
            size = size / ONE_GIGABYTE;
            units = "GB";
        }

        return exact
                ? String.format("%d %s", bytes, units)
                : String.format("%.1f %s", size, units);
    }
}
