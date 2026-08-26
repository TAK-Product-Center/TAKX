package gov.takx.test.mock;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Mouse listener to test that mouse event notification is issued as expected.
 *
 * @since 5.0
 */
public class TestMouseListener implements MouseListener
{
    private int callCount = 0;

    @Override
    public void mouseClicked(MouseEvent e)
    {
        callCount++;
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        callCount++;
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        callCount++;
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
        callCount++;
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
        callCount++;
    }

    /**
     * @return The number of times the listener was called
     */
    public int getCallCount()
    {
        return callCount;
    }
}
