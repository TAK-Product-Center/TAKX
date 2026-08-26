package gov.takx.platform.tracks;

import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.tracks.IConnectingLineDefinition;
import gov.takx.api.tracks.ITrackStyleDefinition;

/**
 * Utility class that accepts an IMapObject instance and determines its castability to an IIconMapObject for extracting
 * out track style and connecting line definitions from the map object.
 *
 * @since 1.12
 */
public final class TrackUtils
{
    private TrackUtils()
    {
    }

    /**
     * Set the Track Style Definition on a map object, first ensuring the map object is of the proper type
     * (an IIconMapObject).
     *
     * @param mapObject            The map object to operate upon
     * @param trackStyleDefinition The TSD to set on the map object
     * @throws IllegalArgumentException when mapObject cannot be cast to an IIconMapObject
     */
    public static void setTrackStyleDefinition(IMapObject mapObject, ITrackStyleDefinition trackStyleDefinition)
    {
        if (mapObject instanceof IIconMapObject)
        {
            IIconMapObject imo = (IIconMapObject) mapObject;
            imo.setTrackStyleDefinition(trackStyleDefinition);
        } else
        {
            throw new IllegalArgumentException("IMapObject must be castable to an IIconMapObject to set the track style definition");
        }
    }

    /**
     * Get the TSD set on the given map object, first ensuring the map object is of the proper type
     * (an IIconMapObject).
     *
     * @param mapObject The map object to operate upon
     * @return the TSD set on the map object, or {@code null} if none or the map object is of the wrong type
     */
    public static ITrackStyleDefinition getTrackStyleDefinition(IMapObject mapObject)
    {
        ITrackStyleDefinition defn = null;

        if (mapObject instanceof IIconMapObject)
        {
            IIconMapObject imo = (IIconMapObject) mapObject;
            defn = imo.getTrackStyleDefinition();
        }

        return defn;
    }

    /**
     * Set the stop record Track Style Definition on a map object, first ensuring the map object is of the proper type
     * (an IIconMapObject).
     *
     * @param mapObject            The map object to operate upon
     * @param trackStyleDefinition The stop record TSD to set on the map object
     * @throws IllegalArgumentException when mapObject cannot be cast to an IIconMapObject
     */
    public static void setStopRecordsTrackStyleDefinition(IMapObject mapObject, ITrackStyleDefinition trackStyleDefinition)
    {
        if (mapObject instanceof IIconMapObject)
        {
            IIconMapObject imo = (IIconMapObject) mapObject;
            imo.setStopRecordsTrackStyleDefinition(trackStyleDefinition);
        } else
        {
            throw new IllegalArgumentException("IMapObject must be castable to an IIconMapObject to set the track style definition");
        }
    }

    /**
     * Get the stop record TSD set on the given map object, first ensuring the map object is of the proper type
     * (an IIconMapObject).
     *
     * @param mapObject The map object to operate upon
     * @return the stop record TSD set on the map object, or {@code null} if none or the map object is of the wrong type
     */
    public static ITrackStyleDefinition getStopRecordsTrackStyleDefinition(IMapObject mapObject)
    {
        ITrackStyleDefinition defn = null;

        if (mapObject instanceof IIconMapObject)
        {
            IIconMapObject imo = (IIconMapObject) mapObject;
            defn = imo.getStopRecordsTrackStyleDefinition();
        }

        return defn;
    }

    /**
     * Get the CLD set on the given map object, first ensuring the map object is of the proper type
     * (an IIconMapObject).
     *
     * @param mapObject The map object to operate upon
     * @return the CLD set on the map object, or {@code null} if none or the map object is of the wrong type
     */
    public static IConnectingLineDefinition getConnectingLineDefinition(IMapObject mapObject)
    {
        IConnectingLineDefinition defn = null;

        if (mapObject instanceof IIconMapObject)
        {
            IIconMapObject imo = (IIconMapObject) mapObject;
            defn = imo.getConnectingLineDefinition();
        }

        return defn;
    }

    /**
     * Set the Connecting Line Definition on a map object, first ensuring the map object is of the proper type
     * (an IIconMapObject).
     *
     * @param mapObject                The map object to operate upon
     * @param connectingLineDefinition The CLD to set on the map object
     * @throws IllegalArgumentException when mapObject cannot be cast to an IIconMapObject
     */
    public static void setConnectingLineDefinition(IMapObject mapObject, IConnectingLineDefinition connectingLineDefinition)
    {
        if (mapObject instanceof IIconMapObject)
        {
            IIconMapObject imo = (IIconMapObject) mapObject;
            imo.setConnectingLineDefinition(connectingLineDefinition);
        } else
        {
            throw new IllegalArgumentException("IMapObject must be castable to an IIconMapObject to set the connecting line definition");
        }
    }
}
