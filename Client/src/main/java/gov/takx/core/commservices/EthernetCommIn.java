package gov.takx.core.commservices;

import gov.takx.api.commservices.ICommService;

/**
 * TCP/IP Comm In.  We override the default to provide re-start support.
 *
 * @since 2.0
 */
public class EthernetCommIn extends CommIn
{
    /**
     * Just pass the comm service to the base class and set the requested read buffer size
     *
     * @param commService ICommService which created this comm in
     * @param bufferSize  max buffer size of each TCP/IP read.  Value of -1 implies the default of 1024
     */
    EthernetCommIn(ICommService commService, int bufferSize)
    {
        super(commService);
        setInputBufferSize(bufferSize);
    }

    protected void restart()
    {
        if (commThread != null) commThread.start();
    }
}
