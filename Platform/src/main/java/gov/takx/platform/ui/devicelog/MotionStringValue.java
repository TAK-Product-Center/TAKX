package gov.takx.platform.ui.devicelog;

import org.jdesktop.swingx.renderer.StringValue;

/**
 */
public class MotionStringValue implements StringValue
{
    @Override
    public String getString(Object value)
    {
        return value != null ? value.toString() : "";
    }
}
