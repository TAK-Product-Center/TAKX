package gov.takx.api.messages;

import gov.takx.api.mapobject.IMapObject;

import java.io.Serializable;

/**
 */
public interface IMapObjectOfflineMessage extends Serializable
{
    String getMapObjectKey();

    String getFamily();

    String getType();

    String getUnitID();

    IMapObject getMapObject();

    /**
     * @return The time at which the Offline operation was initiated
     */
    long getTimeOfOperation();
}
