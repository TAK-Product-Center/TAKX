package gov.takx.dataservices.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IStructure;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.api.tracks.internal.ISmartTrackAssignmentManager;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.platform.utils.UnitsOfMeasureUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 */
@Internal
public class RaptorDataMessage extends ARaptorMessage implements IPrePersistRaptorDataMessage
{
    private static final int HAS_LOCATION_BIT = 0x2;
    private static final int IS_MANUAL_LOCATION_BIT = 0x4;
    private static final int HAS_ELEVATION_BIT = 0x8;
    private static final int HAS_HEADING_BIT = 0x10;
    private static final int HAS_3D_ORIENTATION_BIT = 0x20;
    private static final int HAS_SPEED_BIT = 0x40;
    private static final int HAS_TIME_BIT = 0x80;
    private static final int HAS_MOTION_SET_BIT = 0x100;
    private static final int HAS_MOTION_BIT = 0x200;
    private static final int IS_RENDERABLE_BIT = 0x400;
    private static final int IS_PERSISTABLE_BIT = 0x800;
    // IS_DISCARDED_BIT = 0x1000; // Can't reuse this since the flags are persisted
    private static final int IS_IMPORTED_BIT = 0x2000;

    private CommServiceType sourceType;
    private String sourceName;

    private double heading;
    private double speed;
    private ThreeDOrientation threeDOrientation = null;

    private long time;
    private long receiveTime = System.currentTimeMillis();

    private Integer trackStyleId = null;
    private Integer overrideTrackStyleId = null;

    private SourceMapObjectInfo sourceMapObjectInfo = null;

    private double latitude;
    private double longitude;
    private double elevation;

    /**
     * This latch is set when the message is added to a persistence batch.  It will be {@code null} in all other cases,
     * such as when a message is pulled out of persistence.
     *
     * @since 3.0 (moved here from ARaptorStructuredMessage)
     */
    private CountDownLatch batchLatch = null;

    /**
     * Construct an RDM based on the given structure and MO.
     *
     * @param structure Structure defining extended data
     * @param mapObject Map Object
     * @since 2.29
     */
    public RaptorDataMessage(IStructure structure, IMapObject mapObject)
    {
        super(structure, mapObject);
        init();
    }

    public RaptorDataMessage(IStructure structure, String family, String type, String unitId)
    {
        super(structure, family, type, unitId);
        init();
    }

    /**
     * Common construction settings.
     */
    private void init()
    {
        setPersistable(true);
        setRenderable(true);
    }

    @Override
    public String getSourceDeviceType()
    {
        return sourceMapObjectInfo != null ? sourceMapObjectInfo.sourceDeviceType : null;
    }

    @Override
    public String getSourceFamilyType()
    {
        return sourceMapObjectInfo != null ? sourceMapObjectInfo.sourceFamilyType : null;
    }

    @Override
    public String getSourceDeviceId()
    {
        return sourceMapObjectInfo != null ? sourceMapObjectInfo.sourceDeviceId : null;
    }

    @Override
    public void setSourceFamilyType(String sourceFamilyType)
    {
        if (sourceMapObjectInfo == null) sourceMapObjectInfo = new SourceMapObjectInfo();
        sourceMapObjectInfo.sourceFamilyType = sourceFamilyType.intern();
    }

    @Override
    public void setSourceDeviceType(String sourceDeviceType)
    {
        if (sourceMapObjectInfo == null) sourceMapObjectInfo = new SourceMapObjectInfo();
        sourceMapObjectInfo.sourceDeviceType = sourceDeviceType.intern();
    }

    @Override
    public void setSourceDeviceId(String sourceDeviceId)
    {
        if (sourceMapObjectInfo == null) sourceMapObjectInfo = new SourceMapObjectInfo();
        sourceMapObjectInfo.sourceDeviceId = sourceDeviceId.intern();
    }

    @Override
    public void setTrackStyleDefinition(@Nonnull ITrackStyleDefinition tsd)
    {
        if (!isPersistable())
        {
            throw new IllegalStateException("Non-persisted RDM instances cannot be explicitly assigned a track style definition");
        }

        Integer oldTsdId = trackStyleId;
        trackStyleId = tsd.getId();

        // Tell the assignment manager about this (which includes persisting the association),
        // but only if we already have an Id.
        if (hasId())
        {
            getBean(ISmartTrackAssignmentManager.class).assignTSDtoRDM(oldTsdId, tsd, this);
        }
    }

    @Override
    public void setTrackStyleDefinitionId(Integer tsdId)
    {
        trackStyleId = tsdId;

        // We intentionally don't notify the SmartTrackAssignmentManager here.  It is the responsibility of the caller
        // to ensure that the proper events are fired.  The reason for this is that this method is designed to be
        // used as part of a batch update and the batch handling will take care of those notifications.
    }

    @Override
    public ITrackStyleDefinition getTrackStyleDefinition()
    {
        return trackStyleId == null
                ? null
                : getBean(ISmartTrackAssignmentManager.class).getTrackStyleDefinitionById(trackStyleId);
    }

    @Override
    public Integer getTrackStyleDefinitionId()
    {
        return trackStyleId;
    }

    @Override
    public boolean hasTrackStyleDefinition()
    {
        // True if we have either the permanent or override TSD is non-null
        return trackStyleId != null || overrideTrackStyleId != null;
    }

    @Override
    public void resetTrackStyleDefinition()
    {
        Integer oldTsdId = trackStyleId;
        trackStyleId = null;

        // Tell the assignment manager about this (which includes persisting the association),
        // but only if we already have an Id.
        if (hasId())
        {
            getBean(ISmartTrackAssignmentManager.class).assignTSDtoRDM(oldTsdId, null, this);
        }
    }

    @Override
    public ITrackStyleDefinition getOverrideTrackStyleDefinition()
    {
        return overrideTrackStyleId == null
                ? null
                : getBean(ISmartTrackAssignmentManager.class).getTrackStyleDefinitionById(overrideTrackStyleId);
    }

    @Override
    public Integer getOverrideTrackStyleDefinitionId()
    {
        return overrideTrackStyleId;
    }

    @Override
    public void resetOverrideTrackStyleDefinition()
    {
        Integer oldTsdId = overrideTrackStyleId;
        overrideTrackStyleId = null;

        // Tell the assignment manager about this (which includes persisting the association)
        getBean(ISmartTrackAssignmentManager.class).assignOverrideTSDtoRDM(oldTsdId, null, this);
    }

    @Override
    public void setOverrideTrackStyleDefinition(@Nonnull ITrackStyleDefinition tsd)
    {
        if (!isPersistable())
        {
            throw new IllegalStateException("Non-persisted RDM instances cannot be explicitly assigned a track style definition");
        }

        Integer oldTsdId = overrideTrackStyleId;
        overrideTrackStyleId = tsd.getId();

        // Tell the assignment manager about this (which includes persisting the association)
        getBean(ISmartTrackAssignmentManager.class).assignOverrideTSDtoRDM(oldTsdId, tsd, this);
    }

    @Override
    public void setOverrideTrackStyleDefinitionId(Integer tsdId)
    {
        overrideTrackStyleId = tsdId;

        // We intentionally don't notify the SmartTrackAssignmentManager here.  It is the responsibility of the caller
        // to ensure that the proper events are fired.  The reason for this is that this method is designed to be
        // used as part of a batch update and the batch handling will take care of those notifications.
    }

    @Override
    public void setRenderable(boolean flag)
    {
        setFlag(IS_RENDERABLE_BIT, flag);
    }

    @Override
    public void resetLocation()
    {
        setFlag(HAS_LOCATION_BIT, false);
        setFlag(HAS_ELEVATION_BIT, false);
    }

    @Override
    public void setSourceType(@Nullable CommServiceType sourceType)
    {
        this.sourceType = sourceType;
    }

    @Override
    @Nullable
    public CommServiceType getSourceType()
    {
        return sourceType;
    }

    @Override
    public boolean hasSourceName()
    {
        return sourceName != null;
    }

    @Override
    public String getSourceName()
    {
        return sourceName;
    }

    @Override
    public void setSourceName(@Nonnull String sourceName)
    {
        this.sourceName = sourceName.intern();
    }

    @Override
    public ILocation getLocation()
    {
        return new RaptorLocation(latitude, longitude, hasElevation(), elevation);
    }

    @Override
    public void setLocation(double latitude, double longitude)
    {
        // Note, invalid Lat/Lon in the RDM can cause issues when restoring.  So, this is the same check
        // that is in RaptorLocation, which throws the IAE as well for invalid lat/lon.
        this.latitude = validateLatitude(latitude);
        this.longitude = validateLongitude(longitude);

        setFlag(HAS_LOCATION_BIT, true);
        setFlag(HAS_ELEVATION_BIT, false);
    }

    @Override
    public void setLocation(double latitude, double longitude, double elevation)
    {
        this.latitude = validateLatitude(latitude);
        this.longitude = validateLongitude(longitude);
        this.elevation = elevation;

        setFlag(HAS_LOCATION_BIT, true);
        setFlag(HAS_ELEVATION_BIT, true);
    }

    /**
     * Validate the given latitude.  Throw an IllegalArgumentException if it is out of range.
     *
     * @param latitude value to validate
     * @return given value if valid
     * @throws IllegalArgumentException If value is invalid
     * @since 2.29
     */
    private double validateLatitude(double latitude) throws IllegalArgumentException
    {
        if (latitude < -90.0 || latitude > 90.0)
        {
            throw new IllegalArgumentException("Latitude must be in the range -90.0 to +90.0");
        }
        return latitude;
    }

    /**
     * Validate the given longitude.  Throw an IllegalArgumentException if it is out of range.
     *
     * @param longitude value to validate
     * @return given value if valid
     * @throws IllegalArgumentException If value is invalid
     * @since 2.29
     */
    private double validateLongitude(double longitude) throws IllegalArgumentException
    {
        if (longitude < -180.0 || longitude > 180.0)
        {
            throw new IllegalArgumentException("Longitude must be in the range -180.0 to +180.0");
        }
        return longitude;
    }

    @Override
    public void setLocationWithElevationInFeet(double latitude, double longitude, double elevation)
    {
        setLocation(latitude, longitude, UnitsOfMeasureUtils.convertFeetToMeters(elevation));
    }

    @Override
    public void setManuallyGeneratedLocation(boolean manualLocation)
    {
        setFlag(IS_MANUAL_LOCATION_BIT, manualLocation);
    }

    @Override
    public void setHeading(double heading)
    {
        this.heading = heading;

        setFlag(HAS_HEADING_BIT, true);
    }

    @Override
    public void setOrientation(double heading, double pitch, double roll)
    {
        setHeading(heading);

        threeDOrientation = new ThreeDOrientation(pitch, roll);

        setFlag(HAS_3D_ORIENTATION_BIT, true);
    }

    @Override
    public void setSpeedInMps(double speed)
    {
        this.speed = speed;
        setFlag(HAS_SPEED_BIT, true);
    }

    @Override
    public void setSpeedInKph(double speedInKph)
    {
        setSpeedInMps(speedInKph * UnitsOfMeasureUtils.KPH_TO_MPS_MULTIPLIER);
    }

    @Override
    public void setSpeedInMph(double speedInMph)
    {
        setSpeedInMps(speedInMph * UnitsOfMeasureUtils.MPH_TO_MPS_MULTIPLIER);
    }

    @Override
    public void setSpeedInKnots(double speedInKnots)
    {
        setSpeedInMps(speedInKnots *
                UnitsOfMeasureUtils.KILOMETERS_PER_NAUTICAL_MILE * UnitsOfMeasureUtils.KPH_TO_MPS_MULTIPLIER);
    }

    @Override
    public void setTime(long time)
    {
        this.time = time;
        setFlag(HAS_TIME_BIT, true);
    }

    @Override
    public double getLatitude()
    {
        return latitude;
    }

    @Override
    public double getLongitude()
    {
        return longitude;
    }

    @Override
    public double getElevation()
    {
        return elevation;
    }

    @Override
    public double getPitch()
    {
        return threeDOrientation != null ? threeDOrientation.pitch : 0;
    }

    @Override
    public double getHeading()
    {
        return heading;
    }

    @Override
    public double getRoll()
    {
        return threeDOrientation != null ? threeDOrientation.roll : 0;
    }

    @Override
    public double getSpeedInMps()
    {
        return speed;
    }

    @Override
    public long getTime()
    {
        return time;
    }

    @Override
    public long getReceiveTime()
    {
        return receiveTime;
    }

    @Override
    public void setReceiveTime(long t)
    {
        receiveTime = t;
    }

    @Override
    public boolean hasMotion()
    {
        return getFlag(HAS_MOTION_SET_BIT) && getFlag(HAS_MOTION_BIT);
    }

    @Override
    public boolean isMotionSet()
    {
        return getFlag(HAS_MOTION_SET_BIT);
    }

    @Override
    public void setHasMotion(boolean hasMotion)
    {
        setFlag(HAS_MOTION_SET_BIT, true);
        setFlag(HAS_MOTION_BIT, hasMotion);
    }

    @Override
    public boolean hasLocation()
    {
        return getFlag(HAS_LOCATION_BIT);
    }

    @Override
    public boolean isManuallyGeneratedLocation()
    {
        return getFlag(IS_MANUAL_LOCATION_BIT);
    }

    @Override
    public boolean isRenderable()
    {
        return getFlag(IS_RENDERABLE_BIT);
    }

    @Override
    public boolean hasElevation()
    {
        return getFlag(HAS_ELEVATION_BIT);
    }

    @Override
    public boolean hasHeading()
    {
        return getFlag(HAS_HEADING_BIT);
    }

    @Override
    public boolean has3dOrientation()
    {
        return getFlag(HAS_3D_ORIENTATION_BIT);
    }

    @Override
    public boolean hasSpeed()
    {
        return getFlag(HAS_SPEED_BIT);
    }

    @Override
    public boolean hasTime()
    {
        return getFlag(HAS_TIME_BIT);
    }

    @Override
    public void setPersistable(boolean val)
    {
        setFlag(IS_PERSISTABLE_BIT, val);
    }

    @Override
    public boolean isPersistable()
    {
        return getFlag(IS_PERSISTABLE_BIT);
    }

    @Override
    public boolean isImported()
    {
        return getFlag(IS_IMPORTED_BIT);
    }

    @Override
    public void setImported(boolean imported)
    {
        setFlag(IS_IMPORTED_BIT, imported);
    }

    @Override
    public long getRelevantTime()
    {
        return hasTime() ? time : receiveTime;
    }

    @Override
    public void setBatchLatch(CountDownLatch batchLatch)
    {
        this.batchLatch = batchLatch;
    }

    @Override
    public void awaitBatchCompletion() throws InterruptedException
    {
        if (batchLatch != null) batchLatch.await();
    }

    @Override
    public boolean awaitBatchCompletion(long timeout, TimeUnit unit) throws InterruptedException
    {
        return batchLatch == null || batchLatch.await(timeout, unit);
    }

    @Override
    protected String coreFieldsToString()
    {
        @SuppressWarnings("StringBufferReplaceableByString")
        StringBuilder s = new StringBuilder();

        s.append("sourceName=").append(sourceName)
                .append(", sourceType=").append(sourceType)
                .append(", latitude=").append(getLatitude())
                .append(", longitude=").append(getLongitude())
                .append(", elevation=").append(getElevation())
                .append(", heading=").append(heading)
                .append(", speed=").append(speed)
                .append(", pitch=").append(getPitch())
                .append(", roll=").append(getRoll())
                .append(", time=").append(time)
                .append(", receiveTime=").append(receiveTime)
                .append(", isManualLocation=").append(isManuallyGeneratedLocation())
                .append(", hasLocation=").append(hasLocation())
                .append(", hasHeading=").append(hasHeading())
                .append(", hasSpeed=").append(hasSpeed())
                .append(", hasTime=").append(hasTime())
                .append(", hasMotion=").append(hasMotion());

        return s.toString();
    }

    /**
     * These fields are rarely used, and consume 12 bytes in every RDM.  So, I've moved them out to a separate
     * object to reduce the typical usage down to 4 bytes (for the {@code null} reference).  In the cases where they are
     * used, we'll have an additional object overhead, but AFAIK, those use cases are not the ones that have
     * millions of RDMs.  We can reevaluate, and possibly create a cache of tuples if needed.
     */
    private static class SourceMapObjectInfo
    {
        private String sourceDeviceType = null;
        private String sourceFamilyType = null;
        private String sourceDeviceId = null;
    }

    private record ThreeDOrientation(double pitch, double roll)
    {
    }
}
