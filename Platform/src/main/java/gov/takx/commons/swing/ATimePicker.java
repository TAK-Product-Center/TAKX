package gov.takx.commons.swing;

import javax.swing.JPanel;

/**
 * Abstract class extended by panels that allow for selecting of time.
 *
 * @since 3.3
 */
public abstract class ATimePicker extends JPanel
{
    /**
     * @return The currently selected time of the picker represented in milliseconds since epoch.
     */
    public abstract long getTime();

    /**
     * Set the current time of the picker.
     *
     * @param time Milliseconds since epoch.
     */
    public abstract void setTime(long time);
}
