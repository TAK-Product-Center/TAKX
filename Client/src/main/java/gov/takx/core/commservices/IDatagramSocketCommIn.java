package gov.takx.core.commservices;

import gov.takx.api.commservices.ICommIn;

import java.net.DatagramSocket;

/**
 * Package-access interface used strictly by ACommService and its descendants.
 *
 */
interface IDatagramSocketCommIn extends ICommIn
{
    void startCommIn(DatagramSocket socket);
}
