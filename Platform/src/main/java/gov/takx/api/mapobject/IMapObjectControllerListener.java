package gov.takx.api.mapobject;

import gov.takx.api.messages.IRaptorDataMessage;

/**
 */
public interface IMapObjectControllerListener
{
    void receiveMessage(IRaptorDataMessage rdm);
}
