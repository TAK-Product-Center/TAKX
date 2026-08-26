package gov.takx.api.mapobject;

/**
 */
public abstract class AMapObjectDataMonitor extends ADataMonitor
{
    protected AMapObjectDataMonitor(String family, String type)
    {
        super(family, type);
        setAffectsAudioAndIcons(true);
    }
}
