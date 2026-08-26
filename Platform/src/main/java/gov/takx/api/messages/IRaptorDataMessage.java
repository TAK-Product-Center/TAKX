package gov.takx.api.messages;

import gov.takx.api.annotation.Internal;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.commons.constants.CommServiceType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * @since 1.3
 */
@SuppressWarnings("UnusedDeclaration")
public interface IRaptorDataMessage extends IRaptorMessage
{
    /**
     * Identifies a source map object family type, or {@code null} if this message was not sent from another map object.
     *
     * @return Source Device Type, or {@code null} if not from another map object
     */
    String getSourceFamilyType();

    /**
     * Identifies a source map object device type, or {@code null} if this message was not sent from another map object.
     *
     * @return Source Device Type, or {@code null} if not from another map object
     */
    String getSourceDeviceType();

    /**
     * Identifies a source map object device ID, or {@code null} if this message was not sent from another map object.
     *
     * @return Source Device ID, or {@code null} if not from another map object
     */
    String getSourceDeviceId();

    boolean hasSourceName();

    String getSourceName();

    @Nullable
    CommServiceType getSourceType();

    ILocation getLocation();

    /**
     * @return The latitude in decimal degrees.
     */
    double getLatitude();

    /**
     * @return The longitude in decimal degrees.
     */
    double getLongitude();

    /**
     * @return The elevation in meters.
     */
    double getElevation();

    /**
     * @return The pitch in degrees.
     */
    double getPitch();

    /**
     * @return The heading in degrees.
     */
    double getHeading();

    /**
     * @return The roll in degrees.
     */
    double getRoll();

    /**
     * @return The speed in meters/sec.
     */
    double getSpeedInMps();

    /**
     * @return The device time in milliseconds since epoch.
     */
    long getTime();

    /**
     * @return The time the message was received in TAKX in milliseconds since epoch.
     */
    long getReceiveTime();

    /**
     * Get the reported motion state. Will default to returning {@code false} if motion has not been explicitly set to
     * {@code true}.
     *
     * @return motion state
     */
    boolean hasMotion();

    /**
     * Returns whether or not the motion state was explicitly set for this message. Motion is basically a tri-state
     * variable in that it can be {@code false}, {@code true} or indeterminate (in which case this method will return
     * {@code false}).
     *
     * @return whether or not motion was set for this message
     * @since 1.49
     */
    boolean isMotionSet();

    boolean hasLocation();

    boolean isManuallyGeneratedLocation();

    boolean hasElevation();

    boolean hasHeading();

    boolean has3dOrientation();

    boolean hasSpeed();

    boolean hasTime();

    boolean isPersistable();

    /**
     * Returns true if the messages has been marked as imported.
     *
     * @return {@code true} if marked as imported, {@code false} otherwise.
     * @since 1.33
     */
    boolean isImported();

    /**
     * @return The <em>relevant</em> time for this message.  This will be the device time, if set, otherwise
     * it is the receive time.
     * @see #getTime()
     * @see #getReceiveTime()
     */
    long getRelevantTime();

    /**
     * Get the "permanent" TSD assigned to this RDM, set using {@link #setTrackStyleDefinition(ITrackStyleDefinition)}.
     * Note that this may be {@code null} even if the override TSD is not {@code null}.
     *
     * @return Current permanent TSD assigned, may be {@code null} if no TSD is assigned
     */
    ITrackStyleDefinition getTrackStyleDefinition();

    /**
     * Get the Id of the "permanent" TSD assigned to this RDM. Note that this may be {@code null} even if the override
     * TSD is not {@code null}. Note also that this operation is much faster than calling
     * {@link #getTrackStyleDefinition()} and calling getId() on the result since it avoids a database fetch of the TSD.
     *
     * @return Id of current permanent TSD assigned, may be {@code null} if no TSD is assigned
     * @since 1.25
     */
    Integer getTrackStyleDefinitionId();

    /**
     * Test if an RDM-level track style definition has been specified for this RDM.  This includes either a
     * permanent TSD or an override TSD.
     *
     * @return {@code True} if a TSD has been set (permanent or override), {@code false} otherwise
     * @see #setTrackStyleDefinition(ITrackStyleDefinition)
     * @see #setOverrideTrackStyleDefinition(ITrackStyleDefinition)
     * @see #resetTrackStyleDefinition()
     * @see #resetOverrideTrackStyleDefinition()
     * @since 1.12
     */
    boolean hasTrackStyleDefinition();

    /**
     * Set the permanent track style definition to use on this RDM.  This can be called at any time on the RDM (even after
     * the RDM has been persisted).  Any attributes specified on the new RDM-level TSD will override those set on the
     * MapObject's TSD.  Note that this TSD may be overridden by a call to
     * {@link #setOverrideTrackStyleDefinition(ITrackStyleDefinition)}.  As long as an override TSD is in place, the
     * attributes of the permanent TSD will be ignored for rendering.
     *
     * @param trackStyle The new track style definition to use on this RDM
     * @throws IllegalStateException If this RDM is not persistable
     * @since 1.12
     */
    void setTrackStyleDefinition(@Nonnull ITrackStyleDefinition trackStyle);

    /**
     * Set the permanent track style definition to use on this RDM.  This can be called at any time on the RDM (even after
     * the RDM has been persisted).  Any attributes specified on the new RDM-level TSD will override those set on the
     * MapObject's TSD.  Note that this TSD may be overridden by a call to
     * {@link #setOverrideTrackStyleDefinition(ITrackStyleDefinition)}.  As long as an override TSD is in place, the
     * attributes of the permanent TSD will be ignored for rendering.
     *
     * @param tsdId The ID of the new track style definition to use on this RDM
     * @since 1.48
     */
    void setTrackStyleDefinitionId(Integer tsdId);

    /**
     * Reset the track style definition on this RDM, essentially setting it to {@code null}. Once reset, all track
     * styling attributes will be drawn from the map object that owns this RDM. If an override TSD is in effect,
     * then the rendering of the track point for this RDM will not change until the override is cleared. At that
     * time, this new permanent TSDs attributes will take effect for rendering.
     *
     * @since 1.12
     */
    void resetTrackStyleDefinition();

    /**
     * Get the "override" TSD assigned to this RDM, set using {@link #setOverrideTrackStyleDefinition(ITrackStyleDefinition)}.
     *
     * @return Current override TSD assigned, may be {@code null} if no TSD is assigned
     * @since 1.14
     */
    ITrackStyleDefinition getOverrideTrackStyleDefinition();

    /**
     * Get the Id of the "override" TSD assigned to this RDM.
     * Note also that this operation is much faster than calling {@link #getOverrideTrackStyleDefinition()} and
     * calling getId() on the result since it avoids a database fetch of the TSD.
     *
     * @return Id of current override TSD assigned, may be {@code null} if no TSD is assigned
     * @since 1.25
     */
    Integer getOverrideTrackStyleDefinitionId();

    /**
     * Set the override track style definition to use on this RDM.  This can be called at any time on the RDM (even after
     * the RDM has been persisted).  Any permanent TSD has been assigned, set using
     * {@link #setTrackStyleDefinition(ITrackStyleDefinition)}, will be overridden with this new TSD until the
     * override is reset, using {@link #resetOverrideTrackStyleDefinition()}.
     *
     * @param trackStyle The new track style definition to use on this RDM
     * @throws IllegalStateException If this RDM is not persistable
     * @since 1.14
     */
    void setOverrideTrackStyleDefinition(@Nonnull ITrackStyleDefinition trackStyle);

    /**
     * Set the override track style definition to use on this RDM.  This can be called at any time on the RDM (even after
     * the RDM has been persisted).  Any permanent TSD has been assigned, set using
     * {@link #setTrackStyleDefinition(ITrackStyleDefinition)}, will be overridden with this new TSD until the
     * override is reset, using {@link #resetOverrideTrackStyleDefinition()}.
     *
     * @param tsdId The ID of the new track style definition to use on this RDM
     * @since 2.14
     */
    @Internal
    void setOverrideTrackStyleDefinitionId(Integer tsdId);

    /**
     * Reset the override track style definition on this RDM, essentially setting it to {@code null}. If a permanent TSD
     * had been previously assigned, using {@link #setTrackStyleDefinition(ITrackStyleDefinition)}, then that
     * TSD will be brought back into affect for rendering the track point for this RDM.
     *
     * @since 1.14
     */
    void resetOverrideTrackStyleDefinition();

    /**
     * Renderable flag is used to tell the core that even if this RDM's hasLocation() returns true, it should
     * not be automatically rendered as a track point on the globe. The RDM is left for the developer to provide
     * visual feedback through other, arbitrary means.
     *
     * @return {@code true}, if RDM should be rendered as a track point if hasLocation() is {@code true}, or
     * {@code false} if it should be ignored by the core's rendering cycle
     */
    boolean isRenderable();

    /**
     * Wait for the persistence batch (if any) containing this message to complete (be fully persisted).  Calling
     * this method will block the current thread until the persistence completes or the thread is interrupted.
     * If the batch persistence has already completed (or the message was never in a batch), then this method
     * returns immediately.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @see #awaitBatchCompletion(long, TimeUnit)
     * @since 1.49
     */
    void awaitBatchCompletion() throws InterruptedException;

    /**
     * Wait for the persistence batch (if any) containing this message to complete (be fully persisted) or the
     * specified waiting time elapses.
     * Calling this method will block the current thread until the persistence completes, the thread is interrupted,
     * or the timeout expires. If the batch persistence has already completed (or the message was never in a batch),
     * then this method returns immediately.
     *
     * @return {@code true} if the persistence was completed
     * or {@code false} if the waiting time elapsed before the persistence completed
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @see #awaitBatchCompletion()
     * @since 1.49
     */
    boolean awaitBatchCompletion(long timeout, TimeUnit unit) throws InterruptedException;
}
