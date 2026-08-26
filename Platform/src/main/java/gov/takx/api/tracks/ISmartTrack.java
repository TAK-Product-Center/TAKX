package gov.takx.api.tracks;

import gov.takx.api.messages.IRaptorDataMessage;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Implemented by Map Object instances that fully support SmartTrack capabilities. Each SmartTrack enabled Map Object
 * defines a collection of four attributes, including an {@link IConnectingLineDefinition}, and a
 * stop record TrackStyleDefinition.
 * <p/>
 * The Connecting Line is used to connect every visible track point using an inherent sequence defined by the temporal
 * attributes of the Map Object's RDMs. Each RDM with a valid location translates to at most a single track point, and
 * that single track point is associated with exactly one track point style. The track point style on the RDM may be
 * {@code null}, and the track point style on the Map Object may be {@code null}, but there will always be a global track style
 * definition. Rendering decisions are therefore based on the defined sequence: RDM first, Map Object second, Global
 * last.
 * <p/>
 * The track style definition defines precisely how the track points should be rendered. In cases where the RDM has a
 * {@code null} track style definition, then the definition of the Map Object, if defined, is used. In cases where neither the
 * RDM and the Map Object have assigned values, the Global track style definition is applied.
 *
 * @since 1.12
 */
public interface ISmartTrack
{
    /**
     * Defines the connecting line definition for the SmartTrack-enabled Map Object.
     *
     * @param definition connecting line definition
     */
    void setConnectingLineDefinition(IConnectingLineDefinition definition);

    /**
     * Optional connecting line definition associated with the SmartTrack-enabled Map Object.
     *
     * @return Connecting line definition, or {@code null} if none is defined
     */
    @Nullable
    IConnectingLineDefinition getConnectingLineDefinition();

    /**
     * Defines the track style definition for the SmartTrack-enabled Map Object.
     *
     * @param trackStyleDefinition track style definition
     */
    void setTrackStyleDefinition(ITrackStyleDefinition trackStyleDefinition);

    /**
     * Optional track style definition associated with the SmartTrack-enabled Map Object.
     *
     * @return Track style definition, or {@code null} if none is defined
     */
    @Nullable
    ITrackStyleDefinition getTrackStyleDefinition();

    /**
     * Defines the stop records track style definition for the SmartTrack-enabled Map Object.
     *
     * @param trackStyleDefinition track style definition
     */
    void setStopRecordsTrackStyleDefinition(ITrackStyleDefinition trackStyleDefinition);

    /**
     * Optional stop record track style definition associated with the SmartTrack-enabled Map Object.
     *
     * @return Track style definition, or {@code null} if none is defined
     */
    @Nullable
    ITrackStyleDefinition getStopRecordsTrackStyleDefinition();

    /**
     * Get a track styling context for the given RDM.  The context is used to retrieve styling attribute values
     * (like track color, shape, etc.) while taking into account the inheritance hierarchy of TrackStyleDefinitions:
     * from RDM -&gt; MapObject -&gt; Global.
     *
     * @param rdm The RDM of interest
     * @return A styling context for that RDM
     */
    @Nullable
    ITrackStylingContext getTrackStylingContext(@Nonnull IRaptorDataMessage rdm);
}
