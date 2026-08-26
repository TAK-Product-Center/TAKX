package gov.takx.api.mapobject;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.messages.IRaptorDataMessage;

import java.util.List;

/**
 * Interface to allow {@link IMapObjectController}s to override the default track dialog.
 *
 * @since 1.17
 */
public interface ITrackDisplayHandler
{
    /**
     * Provides the {@link IRaptorDataMessage}s associated with the tracks near a user's double-click.
     *
     * @param rdms        The {@link IRaptorDataMessage}s in the vicinity of the user's double-click.
     * @param minLocation The top-left corner of the geographic region determined to be "near" the user's double-click.
     * @param maxLocation The bottom-right corner of the geographic region determined to be "near" the user's double-click.
     */
    void onDisplay(List<IRaptorDataMessage> rdms, ILocation minLocation, ILocation maxLocation);
}
