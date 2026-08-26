package gov.takx.platform.ui.devicelog;

import gov.takx.platform.utils.StandardTimeFormatting;
import org.jdesktop.swingx.renderer.StringValue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * String value provider for use in rendering a motion duration in a JXTable.
 *
 * @since 1.49
 */
public class DateStringValue implements StringValue
{
    /**
     * The default date format.
     */
    public static final DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat(StandardTimeFormatting.DEFAULT_FORMAT);

    private final DateFormat format;

    /**
     * Construct using the default date format.
     */
    public DateStringValue()
    {
        this(DEFAULT_DATE_FORMAT);
    }

    /**
     * Construct using the specified date format.
     *
     * @param format Date format to use
     */
    public DateStringValue(DateFormat format)
    {
        this.format = format;
    }

    @Override
    public String getString(Object value)
    {
        // PLUGIN-1657: If the RDM has no device time set, render as "unknown" instead of epoch
        return value != null ? format.format(value) : "unknown";
    }
}
