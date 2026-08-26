package gov.takx.api.globe;

/**
 */
public interface IGlobeMapObjectChangeListener
{
    enum MapObjectChangeEvent
    {
        ADDED, REMOVED, CHANGED
    }

    void globeMapObjectChanged(IGlobeMapObject globeMapObject, MapObjectChangeEvent mapObjectChangeEvent);
}
