package gov.takx.platform.utils;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.util.RestorableSupport;

import java.util.Map;

/**
 */
public class PersistableMetadataUtils
{
    public static String AVLIST_STATE_OBJECT_NAME = "avlist";

    public static String getMetadataAsXML(AVListImpl metadata)
    {
        RestorableSupport rs = RestorableSupport.newRestorableSupport();
        RestorableSupport.StateObject so = rs.addStateObject(null, AVLIST_STATE_OBJECT_NAME);
        for (Map.Entry<String, Object> e : metadata.getEntries())
        {
            metadata.getRestorableStateForAVPair(e.getKey(), e.getValue() != null ? e.getValue() : "", rs, so);
        }
        return rs.getStateAsXml();
    }

    public static void restoreMetadataFromXML(String metadataXML, AVList metadata)
    {
        if (metadataXML != null)
        {
            RestorableSupport rs = RestorableSupport.parse(metadataXML);
            RestorableSupport.StateObject avlist = rs.getStateObject(AVLIST_STATE_OBJECT_NAME);
            if (avlist != null)
            {
                RestorableSupport.StateObject[] soList = rs.getAllStateObjects(avlist, "");
                if (soList != null)
                {
                    for (RestorableSupport.StateObject so : soList)
                    {
                        String name = so.getName();
                        String value = so.getValue();
                        metadata.setValue(name, value);
                    }
                }
            }
        }
    }
}
