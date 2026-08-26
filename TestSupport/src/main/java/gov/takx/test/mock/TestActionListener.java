package gov.takx.test.mock;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Action listener to test that action is performed as expected.
 * @since 3.4 (formerly an inner class on ToolbarTest)
 */
public class TestActionListener implements ActionListener
{
    private int callCount = 0;

    @Override
    public void actionPerformed(ActionEvent e)
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
