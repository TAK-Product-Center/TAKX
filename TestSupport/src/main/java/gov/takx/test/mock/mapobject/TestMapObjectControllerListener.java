package gov.takx.test.mock.mapobject;

import gov.takx.api.mapobject.IMapObjectControllerListener;
import gov.takx.api.messages.IRaptorDataMessage;

/**
 * Listener for messages received by a map object controller.
 *
 * @since 3.4
 */
@SuppressWarnings("unused") // full implementation provided for test support
public class TestMapObjectControllerListener implements IMapObjectControllerListener
{
    private int receivedMessagesCount = 0;
    private IRaptorDataMessage lastRdmReceived;

    @Override
    public void receiveMessage(IRaptorDataMessage rdm)
    {
        receivedMessagesCount++;
        lastRdmReceived = rdm;
    }

    /**
     * Resets state of listener.
     */
    public void reset()
    {
        receivedMessagesCount = 0;
        lastRdmReceived = null;
    }

    /**
     * @return The number of messages received by this listener since it was created or reset.
     */
    public int getReceivedMessagesCount()
    {
        return receivedMessagesCount;
    }

    /**
     * @return The last RDM received by this listener.
     */
    public IRaptorDataMessage getLastRdmReceived()
    {
        return lastRdmReceived;
    }
}
