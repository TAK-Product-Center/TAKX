package gov.takx.dataservices.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.AMapObjectDataMonitor;
import gov.takx.api.mapobject.FieldType;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.persist.IPersistable;
import gov.takx.api.plugin.datamonitor.DataMonitorEvent;
import gov.takx.api.plugin.datamonitor.DataMonitorEventType;
import gov.takx.api.query.ComparisonType;
import gov.takx.api.query.CoreField;
import gov.takx.api.shapes.ICircle;
import gov.takx.api.shapes.IPolygon;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.shapes.IShape;
import gov.takx.commons.constants.State;
import gov.takx.dataservices.shapes.PublishedShapeTracker;
import gov.takx.platform.query.FieldExtractor;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.OperationNotSupportedException;

import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Wrapper for DataMonitor used exclusively for now by the Custom Alarms Preferences panel.  MapObjectController
 * keeps a list so they can be displayed at any time in the preferences panel, edited, etc.
 *
 */
@Internal
@PseudoBean
@Entity
@Table(indexes = {@Index(name = "moKey", columnList = "moKey")})
public class CustomDataMonitor implements IPersistable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Indicates the type of alarm.  DataMonitor for field values and Geofence for shape containment.
     *
     * @since 3.0
     */
    public enum AlarmType
    {
        DataMonitor,
        Geofence
    }

    /**
     * Condition under which a geofence will trigger.
     */
    public enum GeofenceAlarmTrigger
    {
        /**
         * Triggers only upon entry, transitioning from a location outside the geofence to inside.
         */
        ENTERING,

        /**
         * Triggers only upon exit, transitioning from a location inside the geofence to outside.
         */
        EXITING,

        /**
         * Triggers upon entry or exit, transitioning in either direction.
         */
        BOTH;

        @Override
        public String toString()
        {
            return this == BOTH ? "ENTERING or EXITING" : name();
        }
    }

    // Predicate to determine if a given shape can be used for a geofence data monitor
    private static final Predicate<IShape> IS_VALID_SHAPE =
            shape -> shape instanceof IRectangle || shape instanceof IPolygon || shape instanceof ICircle;

    // Map from field type to function to parse it from a string
    private static final Map<FieldType, Function<String, Comparable<?>>> TYPE_PARSERS = Map.of(
            FieldType.BOOLEAN, Boolean::parseBoolean,
            FieldType.SHORT, Short::parseShort,
            FieldType.INTEGER, Integer::parseInt,
            FieldType.LONG, Long::parseLong,
            FieldType.FLOAT, Float::parseFloat,
            FieldType.DOUBLE, Double::parseDouble,
            FieldType.STRING, s -> s);

    // The core fields we support
    private static final Set<CoreField> SUPPORTED_CORE_FIELDS = Set.of(CoreField.latitude, CoreField.longitude,
            CoreField.elevation, CoreField.speed, CoreField.heading, CoreField.pitch, CoreField.roll,
            CoreField.hasMotion);

    @Transient
    @Inject
    private PublishedShapeTracker publishedShapeTracker;

    @SuppressWarnings("unused")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Transient
    private ACustomDataMonitor dm = null;

    // Common instance variables for all alarm types
    @Enumerated(EnumType.STRING)
    private AlarmType alarmType;

    private String displayName = "";
    private State alarmState;
    private State defaultState = State.Unknown;
    private boolean clearable = true;
    private boolean ackReq = true;
    private boolean holdState = true;
    private boolean enabled = true; // Indicates if the alarm is enabled or disabled (tied to the checkbox in the preferences panel)

    private String moKey;
    private String family;
    private String type;

    @Transient
    private IMapObject mapObject;

    // Instance variables only applicable to Geofence Alarms
    private GeofenceAlarmTrigger geofenceAlarmTrigger = GeofenceAlarmTrigger.ENTERING;
    private String geofenceShapeKey; // For restoring our reference to the geofence shape

    @Transient
    private IShape geofenceShape;

    // Instance variables only applicable to Field Data Monitor Alarms
    private String structureName;
    private String fieldName;
    private ComparisonType comparisonType;
    private String threshold;
    private FieldType fieldType;
    private boolean showValue = false;

    @Transient
    private CoreField coreField; // Non-null if the fieldName refers to a core field

    @SuppressWarnings("UnusedDeclaration") // Used by Hibernate
    protected CustomDataMonitor()
    {
    }

    /**
     * Constructor for creating a CustomDataMonitor before all the alarm values are known.
     * If this constructor is employed, the {@link #setDataMonitorAlarmValues} method or
     * {@link #setGeofenceAlarmValues} method must be called to set the appropriate values before utilizing
     * this data monitor.
     *
     * @param alarmType Alarm type
     * @param mapObject Map Object this data monitor is processing
     * @see #setDataMonitorAlarmValues
     * @see #setGeofenceAlarmValues
     */
    public CustomDataMonitor(AlarmType alarmType, IMapObject mapObject)
    {
        this.alarmType = alarmType;
        this.mapObject = mapObject;

        family = mapObject.getFamily();
        type = mapObject.getType();
        moKey = mapObject.getKey();
    }

    /**
     * Constructor for creating a FieldValueDataMonitor with all the values set.
     */
    public CustomDataMonitor(IMapObject mapObject, String displayName, String structureName, String fieldName,
                             ComparisonType comparisonType, String threshold,
                             State alarmState, FieldType fieldType, State defaultState, boolean clearable,
                             boolean ackReq, boolean showValue, boolean holdState)
    {
        this(AlarmType.DataMonitor, mapObject);

        setDataMonitorAlarmValues(displayName, structureName, fieldName, comparisonType, threshold, alarmState, fieldType, defaultState, clearable, ackReq, showValue, holdState);
    }

    /**
     * Constructor for creating a GeofenceDataMonitor with all the values set.
     *
     * @param mapObject            The map object owning this data monitor
     * @param displayName          The data monitor display name.
     * @param alarmState           The alarm state for the data monitor.
     * @param defaultState         The default state for the data monitor.
     * @param clearable            Whether the monitor is clearable.
     * @param ackReq               Whether acknowledgement is required for the monitor.
     * @param holdState            Whether the hold state is enabled for the monitor.
     * @param geofenceShape        The shape to use for the monitor.
     * @param geofenceAlarmTrigger The type of trigger to use for the monitor.
     */
    public CustomDataMonitor(IMapObject mapObject, String displayName,
                             State alarmState, State defaultState, boolean clearable, boolean ackReq,
                             boolean holdState, IShape geofenceShape, GeofenceAlarmTrigger geofenceAlarmTrigger)
    {
        this(AlarmType.Geofence, mapObject);

        setGeofenceAlarmValues(displayName, alarmState, defaultState, clearable, ackReq, holdState, geofenceShape, geofenceAlarmTrigger);
    }

    public void setDataMonitorAlarmValues(String displayName, String structureName, String fieldName,
                                          ComparisonType comparisonType, String threshold,
                                          State alarmState, FieldType fieldType,
                                          State defaultState, boolean clearable,
                                          boolean ackReq, boolean showValue, boolean holdState)
    {
        if (AlarmType.DataMonitor != alarmType)
            throw new IllegalStateException("Can not set VALUE properties on data monitor of type: " + alarmType);

        this.displayName = displayName;
        this.structureName = structureName;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.comparisonType = comparisonType;
        this.threshold = threshold;
        this.alarmState = alarmState;
        this.defaultState = defaultState;
        this.clearable = clearable;
        this.ackReq = ackReq;
        this.showValue = showValue;
        this.holdState = holdState;

        coreField = CoreField.forName(fieldName);

        if (coreField != null && !SUPPORTED_CORE_FIELDS.contains(coreField))
            throw new IllegalArgumentException("Unsupported core field type [" + coreField + "]");

        if (dm == null)
        {
            dm = new FieldValueDataMonitor(this);
        } else
        {
            dm.updateState();
        }
    }

    public void setGeofenceAlarmValues(String displayName, State alarmState, State defaultState,
                                       boolean clearable, boolean ackReq, boolean holdState,
                                       IShape geofenceShape, GeofenceAlarmTrigger geofenceAlarmTrigger)
    {
        if (AlarmType.Geofence != alarmType)
            throw new IllegalStateException("Can not set GEOFENCE properties on data monitor of type: " + alarmType);

        if (!IS_VALID_SHAPE.test(geofenceShape))
            throw new IllegalStateException("Can not use a " + geofenceShape.getClass() + " as a geofence");

        this.displayName = displayName;
        this.alarmState = alarmState;
        this.defaultState = defaultState;
        this.clearable = clearable;
        this.ackReq = ackReq;
        this.holdState = holdState;
        this.geofenceShape = geofenceShape;
        geofenceShapeKey = geofenceShape.getKey();
        this.geofenceAlarmTrigger = geofenceAlarmTrigger;

        if (dm == null)
        {
            dm = new GeofenceDataMonitor(this);
        } else
        {
            dm.updateState();
        }
    }

    /**
     * Reinitialize our state, relinking to our Map Object.  This is only called when this entity is being restored
     * from persistence (i.e., a project reopen).
     *
     * @param mapObject Our owning Map Object
     * @since 3.2
     */
    public void reinitialize(IMapObject mapObject)
    {
        this.mapObject = mapObject;

        if (alarmType == AlarmType.Geofence)
        {
            geofenceShape = publishedShapeTracker.getShape(geofenceShapeKey);

            if (geofenceShape == null) logger.error("Missing geofence: {}", geofenceShapeKey);
        } else
        {
            coreField = CoreField.forName(fieldName);
        }
    }

    public void shutdown()
    {
        if (dm != null) dm.onDataMonitorEvent(new DataMonitorEvent(dm, DataMonitorEventType.Deactivate));
    }

    /**
     * Return string in the form of "Battery: Voltage &lt;= 10.0" for Value and String Alarms,
     * and "Location: [trigger] Shape1" for a Geofence Alarm.
     *
     * @return string description of custom data monitor
     */
    public String toString()
    {
        final String description = AlarmType.Geofence == alarmType
                ? geofenceAlarmTrigger + " " + (geofenceShape != null ? geofenceShape.getName() : "<Unknown>")
                : fieldName + " " + comparisonType + " " + threshold;

        return displayName + ": " + description;
    }

    public IDataMonitor getDataMonitor()
    {
        if (dm == null)
        {
            dm = AlarmType.Geofence == alarmType
                    ? new GeofenceDataMonitor(this)
                    : new FieldValueDataMonitor(this);
        }

        return dm;
    }

    public String getStructureName()
    {
        return structureName;
    }

    public boolean isAckReq()
    {
        return ackReq;
    }

    public State getAlarmState()
    {
        return alarmState;
    }

    public boolean isClearable()
    {
        return clearable;
    }

    public ComparisonType getComparisonType()
    {
        return comparisonType;
    }

    public State getDefaultState()
    {
        return defaultState;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getFieldName()
    {
        return fieldName;
    }

    public FieldType getFieldType()
    {
        return fieldType;
    }

    public boolean isShowValue()
    {
        return showValue;
    }

    public String getThreshold()
    {
        return threshold;
    }

    public boolean isHoldState()
    {
        return holdState;
    }

    public String getKey()
    {
        return moKey;
    }

    @Override
    public Integer getId()
    {
        return id;
    }

    public AlarmType getAlarmType()
    {
        return alarmType;
    }

    public GeofenceAlarmTrigger getGeofenceAlarmTrigger()
    {
        return geofenceAlarmTrigger;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public IShape getGeofenceShape()
    {
        return geofenceShape;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public List<IShape> getSelectableValues()
    {
        return publishedShapeTracker.getShapes().stream()
                .filter(IS_VALID_SHAPE)
                .collect(Collectors.toList());
    }

    /**
     * Data monitor for field values.  Not all that different from the core DataMonitor class except that
     * we only have a single comparison, not a set of thresholds.
     *
     * @since 3.0
     */
    @SuppressWarnings("rawtypes")
    private static class FieldValueDataMonitor extends ACustomDataMonitor
    {
        private Comparable parsedThreshold = null;

        private FieldValueDataMonitor(CustomDataMonitor cdm)
        {
            super(cdm, rdm -> rdm.getStructure().getMessageType().equals(cdm.structureName));

            updateState();
        }

        @Override
        protected void updateState()
        {
            super.updateState();

            if (fieldType == null) return; // Is null on newly created instances
            try
            {
                parsedThreshold = TYPE_PARSERS.get(fieldType).apply(cdm.threshold);
            } catch (NumberFormatException e)
            {
                logger.error("Failed to parse threshold value: {}: {}", cdm.threshold, e.getMessage());
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        protected boolean performAnalysis(@Nonnull IRaptorDataMessage rdm)
        {
            final Comparable value = cdm.coreField != null
                    ? (Comparable) FieldExtractor.getCoreField(rdm, cdm.coreField)
                    : rdm.hasField(fieldName) ? (Comparable) rdm.getField(fieldName) : null;

            if (value == null || parsedThreshold == null) return false;

            final Comparator comparator = fieldType == FieldType.STRING
                    ? String.CASE_INSENSITIVE_ORDER
                    : Comparator.naturalOrder();

            final int comparison = comparator.compare(value, parsedThreshold);
            final boolean matched = cdm.comparisonType.interpretComparison(comparison);

            logger.debug("compare {}: {} {} {} => {}", fieldName, value, cdm.comparisonType, cdm.threshold, matched);

            setState(matched ? cdm.alarmState : cdm.defaultState);
            setValue(value);

            return false;
        }
    }

    /**
     * Geofence data monitor, which is used to monitor if a map object has crossed into or out of a geofence shape.
     */
    private static class GeofenceDataMonitor extends ACustomDataMonitor
    {
        private boolean wasInsideGeofence;

        protected GeofenceDataMonitor(CustomDataMonitor cdm)
        {
            super(cdm, IRaptorDataMessage::hasLocation);

            updateState();
        }

        @Override
        protected void updateState()
        {
            super.updateState();

            wasInsideGeofence = cdm.mapObject.hasLocation() && isInsideGeofence(cdm.mapObject.getLocation());
        }

        /**
         * @return {@code true} if the given location is within the boundaries of the configured geofence.
         * @since 3.2
         */
        private boolean isInsideGeofence(ILocation location)
        {
            if (cdm.geofenceShape == null) return false;

            try
            {
                return cdm.geofenceShape.contains(location);
            } catch (OperationNotSupportedException e)
            {
                logger.error("Shape {} does not support contains checks.", cdm.geofenceShape.getClass());
            }
            return false;
        }

        @Override
        protected boolean performAnalysis(@Nonnull IRaptorDataMessage rdm)
        {
            final boolean triggerOnExit = cdm.geofenceAlarmTrigger != GeofenceAlarmTrigger.ENTERING;
            final boolean triggerOnEnter = cdm.geofenceAlarmTrigger != GeofenceAlarmTrigger.EXITING;

            // Are we currently in or out of the geofence, and is that different from our last state
            final boolean insideGeofence = isInsideGeofence(rdm.getLocation());
            final boolean transitioned = insideGeofence != wasInsideGeofence;

            // Should we alarm?  Requires a transition and "direction" matching the trigger.
            final boolean shouldAlarm = transitioned &&
                    ((triggerOnEnter && insideGeofence) || (triggerOnExit && !insideGeofence));

            logger.debug("in={} wasIn={}, onExit={}, onEnter={}, trans={} alarm={}",
                    insideGeofence, wasInsideGeofence, triggerOnExit, triggerOnEnter,
                    transitioned, shouldAlarm);

            if (shouldAlarm) setState(cdm.alarmState);

            wasInsideGeofence = insideGeofence;

            return true;
        }
    }

    /**
     * Common base for our custom data monitor processing.
     * <p>
     * Note: I am really not happy with holding a reference to the containing CDM, but if these inner classes are not
     * static, even though they compile cleanly, it results in a ClassFormatError at runtime.  I couldn't find any
     * way around it other than this idiom.
     * <p>
     * In the long term, this whole class should be refactored so that we have one CDM class per alarm type instead
     * of having a type-specific inner class.
     */
    private static abstract class ACustomDataMonitor extends AMapObjectDataMonitor
    {
        protected final CustomDataMonitor cdm;
        private final Predicate<IRaptorDataMessage> messageAcceptor;

        /**
         * @param messageAcceptor Predicate to determine if an RDM is applicable to this data monitor
         */
        protected ACustomDataMonitor(CustomDataMonitor cdm, Predicate<IRaptorDataMessage> messageAcceptor)
        {
            super(cdm.family, cdm.type);

            this.cdm = cdm;
            this.messageAcceptor = messageAcceptor;

            updateCustomAlarmFields();
        }

        /**
         * Only process new, non-imported messages.
         */
        @Override
        protected boolean shouldProcessMessage(@Nonnull IRaptorDataMessage rdm)
        {
            return cdm.enabled && messageAcceptor.test(rdm) && super.shouldProcessMessage(rdm);
        }

        /**
         * Update any state/configuration coming from our containing CDM.
         *
         * @since 3.0
         */
        protected void updateState()
        {
            updateCustomAlarmFields();
        }

        /**
         * Applies the values stored in the CustomDataMonitor object to the ADataMonitor
         */
        private void updateCustomAlarmFields()
        {
            final String fieldName = cdm.getFieldName();
            if (fieldName != null) setFieldName(fieldName);

            final FieldType fieldType = cdm.getFieldType();
            if (fieldType != null) setFieldType(fieldType);

            setDisplayName(cdm.getDisplayName());
            setDefaultState(cdm.getDefaultState());
            setClearable(cdm.isClearable());
            setShowValueOnDisplay(cdm.isShowValue());
            setTrackHighestAlarmState(cdm.isAckReq());
            setHoldHighestAlarmState(cdm.isHoldState());
        }
    }
}
