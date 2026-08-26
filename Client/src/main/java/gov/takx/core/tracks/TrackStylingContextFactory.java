package gov.takx.core.tracks;

import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.tracks.IExtendedSmartTrackManager;
import gov.takx.api.tracks.ISmartTrack;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.api.tracks.ITrackStylingContext;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Factory for creating TrackStylingContext instances.
 *
 * @since 2.28
 */
@ApplicationScoped
public class TrackStylingContextFactory
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IExtendedSmartTrackManager smartTrackManager;

    @Inject
    TrackStylingContextFactory(IExtendedSmartTrackManager smartTrackManager)
    {
        this.smartTrackManager = smartTrackManager;
    }

    /**
     * Factory method to construct a TSC for the given MapObject.
     *
     * @param mapObject The MapObject to process, it must be an instance of {@link IIconMapObject}
     * @return A ITrackStylingContext that handles the mapObject
     */
    public ITrackStylingContext makeTrackStylingContext(@Nonnull IMapObject mapObject)
    {
        ITrackStyleDefinition moTsd = null;

        if (!mapObject.canHaveStyledTrackData())
        {
            logger.debug("Map Object '{}' does not support styled tracks; creating empty default", mapObject);
        } else
        {
            moTsd = ((ISmartTrack) mapObject).getTrackStyleDefinition();
        }

        // If the MO has no TSD, then craft up a new one with full reach back
        if (moTsd == null) moTsd = smartTrackManager.createDeferringTSD(mapObject);

        final ITrackStyleDefinition globalTSD = smartTrackManager.getGlobalTrackStyleDefinition();
        TrackStylingContext tsc = new TrackStylingContext(moTsd, new TrackStylingContext(globalTSD));

        tsc.setMapObject(mapObject);  // So it knows it is handling a map object

        return tsc;
    }

    /**
     * Factory method to construct a TSC for the given {@link IRaptorDataMessage}'s TSD.
     *
     * @param rdmTsd    The RDM's TrackStyleDefinition
     * @param mapObject The map object that owns the RDM that has the given TSD assigned
     * @return A ITrackStylingContext that handles the RDM
     */
    public ITrackStylingContext makeTrackStylingContextForRDM(ITrackStyleDefinition rdmTsd, IMapObject mapObject)
    {
        // Use the manager to ensure that only one TSC is created for any given MO
        ITrackStylingContext moTsc = smartTrackManager.getTrackStylingContext(mapObject);

        // TODO: we really should be caching RDM-level TSCs as well
        return rdmTsd != null ? new TrackStylingContext(rdmTsd, moTsc) : moTsc;
    }
}
