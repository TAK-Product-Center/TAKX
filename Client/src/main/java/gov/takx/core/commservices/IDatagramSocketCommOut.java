package gov.takx.core.commservices;

import gov.takx.api.commservices.ICommOut;

import java.net.DatagramSocket;

/**
 * Package-access interface used strictly by ACommService and its descendants.
 *
 */
interface IDatagramSocketCommOut extends ICommOut
{
    void startCommOut(DatagramSocket socket);
}
