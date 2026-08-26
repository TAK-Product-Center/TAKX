package gov.takx.test.mock.playback;

import gov.takx.api.playback.IPlaybackLane;
import gov.takx.platform.playback.TimeBoundary;

import javax.swing.JPanel;

import java.awt.Color;

/**
 * Stub playback lane implementation for testing.
 *
 * @since 1.48
 */
public class TestPlaybackLane implements IPlaybackLane
{
    public TimeBoundary timeBoundary = new TimeBoundary(0, 0);

    /**
     * Set the time boundary for this lane.
     *
     * @param timeBoundary New time boundary
     */
    public void setTimeBoundary(TimeBoundary timeBoundary)
    {
        this.timeBoundary = timeBoundary;
    }

    @Override
    public JPanel getInfoPanel()
    {
        return null;
    }

    @Override
    public JPanel getTimeDataPanel()
    {
        return null;
    }

    @Override
    public TimeBoundary getTimeBoundary()
    {
        return timeBoundary;
    }

    @Override
    public void setBackgroundColor(Color color)
    {
    }

    @Override
    public void displayConfigurationEditor()
    {
    }

    @Override
    public void disposeResources()
    {
    }
}
