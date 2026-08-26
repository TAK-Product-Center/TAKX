package gov.takx.api.commservices;

import gov.takx.api.plugin.commservices.ICommPath;

/**
 */
public interface IServerSocketListener extends ICommPath
{
    String getName();

    void stopSocketListener();
}
