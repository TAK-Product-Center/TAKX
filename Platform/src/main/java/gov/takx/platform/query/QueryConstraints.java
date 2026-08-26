package gov.takx.platform.query;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IDataField;
import gov.takx.api.messages.IDataType;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.IStructure;
import gov.takx.api.messages.NoSuchStructureException;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.query.AFieldSelector;
import gov.takx.api.query.ASimpleFieldSelector;
import gov.takx.api.query.ComparisonConstraint;
import gov.takx.api.query.ComparisonType;
import gov.takx.api.query.ConflictingConstraintException;
import gov.takx.api.query.CoreField;
import gov.takx.api.query.CoreFieldSelector;
import gov.takx.api.query.FieldConstraint;
import gov.takx.api.query.FieldSelector;
import gov.takx.api.query.IConstraint;
import gov.takx.api.query.IExtendedQueryConstraints;
import gov.takx.api.query.IQueryConstraints;
import gov.takx.api.query.InvalidFieldTypeException;
import gov.takx.api.query.LogicConstraint;
import gov.takx.api.query.LogicType;
import gov.takx.api.query.NoSuchFieldException;
import gov.takx.api.query.ShapeConstraint;
import gov.takx.api.query.StructureFieldSelector;
import gov.takx.api.query.param.HQLParameter;
import gov.takx.api.registry.IDataStructureRegistry;
import gov.takx.api.shapes.IShape;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link IQueryConstraints}.  This class combines all the constraints as specified in the
 * various {@code add*} methods and then generates the appropriate HQL query and bindings required to
 * execute the query defined by those constraints.
 * <p>
 * As of version 1.49, a new selector and constraint object model were introduced.  All things that can be "selected"
 * are represented by some subclass of {@link AFieldSelector}.  Those selectors are fully qualified in that they
 * contain all the data needed to produce the SQL for referencing them.  The older API allowed the use of arbitrary
 * structure field names without specifying the structure name at the same time.  It was assumed that the structure
 * name would be set via some other call into the API.
 * <p>
 * The new constraints hierarchy, rooted at {@link ComparisonConstraint}, use a selector to specify the DB content
 * to be constrained and a comparison type/value.  Again, since a constraint is created on a selector, the entire
 * construct is fully specified at creation time.
 * <p>
 * Previously, only simple field-level constraints (one field compared against one value) were supported.  Now, a
 * full set of field-level constraints, aggregate constraints (for group by/having) and logic constraints (with
 * and/or/not support for an arbitrary list of sub-constraints) are provided.
 * <p>
 * Here's an example of the new expressive power of the selector/constraint model:
 * <pre>
 *   final IQueryConstraints qc = ...
 *
 *   final String struct = "STATUS_MESSAGE";
 *   final StructureFieldSelector panicField = new StructureFieldSelector(struct, "Panic");
 *   final StructureFieldSelector voltageField = new StructureFieldSelector(struct, "Voltage");
 *
 *   qc.addLogicConstraint(LogicType.or,
 *       new LogicConstraint(LogicType.and,
 *           new LogicConstraint(LogicType.not,
 *               new FieldConstraint(panicField, ComparisonType.NEQ, true)),
 *           new FieldConstraint(voltageField, ComparisonType.GT, 7.55),
 *           new FieldConstraint(voltageField, ComparisonType.LT, 7.85)),
 *
 *       new LogicConstraint(LogicType.and,
 *           new LogicConstraint(LogicType.not,
 *               new FieldConstraint(panicField, ComparisonType.NEQ, false)),
 *           new FieldConstraint(voltageField, ComparisonType.GT, 7.65),
 *           new FieldConstraint(voltageField, ComparisonType.LT, 7.95)));
 *
 *   qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 35.0);
 * </pre>
 *
 */
@Internal
@PseudoBean
public class QueryConstraints implements IExtendedQueryConstraints, IDataType
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // As JavaDoc'd in IQueryConstraints, searching against any of the new array types introduced in 1.7
    // is explicitly unsupported as determined by the architects.  Once we receive a formal request to provide such
    // a capability, we can revisit this logic.
    private static final List<Integer> legalFieldTypes = Arrays.asList(BOOLEAN, SHORT, INT, LONG, FLOAT, DOUBLE, STRING, DATE, BYTE_ARRAY);

    /**
     * We don't support sorting by these fields
     */
    public static final CoreField[] illegalSortFields = {
            CoreField.uuid,
            // sourceType is an int in the database, but an enum in the SourceType object, so it wouldn't be trivial to
            // create consistent sort behavior between the QueryService and MessageStore.
            CoreField.sourceType
    };

    @Inject
    private IMapObjectPersist mapObjectPersist;

    @Inject
    private IDataStructureRegistry dataStructureRegistry;

    protected Set<IMapObject> mapObjects = new HashSet<>();

    protected BitSet moIds = new BitSet();

    /**
     * A map from each structure name to its structure definition(s). There may be more than one structure definition
     * per name if the structure definition has been modified in a way that has changed its hash value. When the user
     * queries the structure by name, each of these alternate versions of the structure must be queried.
     * <p>
     * This map has been modified to use a Set to hold the {@link IStructure}s instead of a list to prevent
     * duplicates.
     *
     * @since 1.18
     */
    protected final Map<String, Set<IStructure>> messageStructureMap = new HashMap<>();

    protected boolean extendedClear;

    protected boolean hasDeviceDateRange;
    protected long deviceMinDate;
    protected long deviceMaxDate;

    protected boolean hasReceiveDateRange;
    protected long receiveMinDate;
    protected long receiveMaxDate;

    protected List<Integer> weekDays;

    protected boolean hasTimeBucketRange;
    protected Time minBucketTime;
    protected Time maxBucketTime;

    protected boolean hasLocationRange;
    protected ILocation minLocation;
    protected ILocation maxLocation;
    protected boolean strictCoordinates;

    protected final HashSet<Integer> messageIdSet = new HashSet<>(); // Message ids
    protected final HashSet<String> structureSet = new HashSet<>();   // Message structure names
    protected final Map<String, String> structureHashMap = new HashMap<>(); // Message structure hashes

    protected String fieldStructureName;
    protected final Map<CoreField, List<IConstraint>> coreFieldConstraints = new EnumMap<>(CoreField.class);
    protected final HashMap<String, List<IConstraint>> structureFieldConstraints = new HashMap<>();
    protected final List<IConstraint> logicConstraints = new ArrayList<>();
    protected final List<IConstraint> shapeConstraints = new ArrayList<>();

    protected boolean grouping;
    protected final List<IConstraint> groupConstraints = new ArrayList<>();

    protected SortOn sortOn;

    protected ASimpleFieldSelector sortOnFieldSelector;

    protected SortOrder sortOrder;

    private int maxMessages = DEFAULT_MAX_MESSAGES;
    private int startMessage;

    private final SqlGenerator generator = new SqlGenerator(this);

    public QueryConstraints()
    {
        /* empty */
    }

    /**
     * Copy constructor.  A shallow copy is performed.  Containers (like lists and maps) are newly created, but
     * the content of those collections is simply added to the new collection.
     *
     * @param source Constraints to copy
     */
    public QueryConstraints(IQueryConstraints source)
    {
        if (!(source instanceof final QueryConstraints qc))
        {
            throw new IllegalArgumentException("Source is not an instance of 'QueryConstraints'");
        }

        mapObjects.addAll(qc.mapObjects);
        setMoIdsFromMos();

        messageStructureMap.putAll(qc.messageStructureMap);
        extendedClear = qc.extendedClear;

        hasDeviceDateRange = qc.hasDeviceDateRange;
        deviceMinDate = qc.deviceMinDate;
        deviceMaxDate = qc.deviceMaxDate;

        hasReceiveDateRange = qc.hasReceiveDateRange;
        receiveMinDate = qc.receiveMinDate;
        receiveMaxDate = qc.receiveMaxDate;

        weekDays = qc.weekDays != null ? new ArrayList<>(qc.weekDays) : null;

        hasTimeBucketRange = qc.hasTimeBucketRange;
        minBucketTime = qc.minBucketTime;
        maxBucketTime = qc.maxBucketTime;

        hasLocationRange = qc.hasLocationRange;
        minLocation = qc.minLocation;
        maxLocation = qc.maxLocation;
        strictCoordinates = qc.strictCoordinates;

        messageIdSet.addAll(qc.messageIdSet);
        structureSet.addAll(qc.structureSet);
        structureHashMap.putAll(qc.structureHashMap);

        fieldStructureName = qc.fieldStructureName;
        coreFieldConstraints.putAll(qc.coreFieldConstraints);
        structureFieldConstraints.putAll(qc.structureFieldConstraints);
        logicConstraints.addAll(qc.logicConstraints);
        shapeConstraints.addAll(qc.shapeConstraints);

        grouping = qc.grouping;
        groupConstraints.addAll(qc.groupConstraints);
        sortOn = qc.sortOn;
        sortOrder = qc.sortOrder;

        maxMessages = qc.maxMessages;
        startMessage = qc.startMessage;
    }

    /**
     * Set the permanent constraint on the map object.  This limits the query domain to only
     * those messages that come from the given mapObject.
     *
     * @param mapObject map object
     */
    @Override
    public void setMapObject(IMapObject mapObject)
    {
        mapObjects.clear();
        mapObjects.add(mapObject);
        setMoIdsFromMos();
    }

    /**
     * Set the permanent constraint on the set of map object proxies.  This limits the query domain to only
     * those messages that come from one of the given mapObjects. Essentially only messages from some version of a
     * specific device instance.
     *
     * @param mapObjects Set of map object
     */
    @Override
    public void setMapObjects(Set<IMapObject> mapObjects)
    {
        this.mapObjects.clear();
        this.mapObjects.addAll(mapObjects);
        setMoIdsFromMos();
    }

    /**
     * Set the moIds to match the current map objects.
     */
    private void setMoIdsFromMos()
    {
        moIds.clear();

        mapObjects.stream()
                .filter(Objects::nonNull)
                .map(IMapObject::getId)
                .filter(id -> id >= 0) // Skip transient MOs
                .forEach(moIds::set);
    }

    @Override
    public Set<IMapObject> getMapObjects()
    {
        return new HashSet<>(mapObjects);
    }

    @Override
    public void setStructuresFromMapObject(String family, String type)
    {
        // Add in all structures from the MapEntity
        addKnownMessageStructures(dataStructureRegistry.getDataStructuresForMapObject(family, type));
    }

    @Override
    public void addKnownMessageStructures(@Nonnull Set<IRaptorDataStructure> messageStructures)
    {
        for (IRaptorDataStructure rds : messageStructures)
        {
            messageStructureMap.computeIfAbsent(rds.getMessageType(), k -> new HashSet<>())
                    .add(rds.getStructure());
        }
    }

    @Override
    public IQueryConstraints addMessageIds(@Nonnull Collection<Integer> ids)
    {
        messageIdSet.addAll(ids);
        return this;
    }

    @Override
    public IQueryConstraints addMessageIds(Integer... ids)
    {
        messageIdSet.addAll(Arrays.asList(ids));
        return this;
    }

    @Override
    public IQueryConstraints addDeviceDateRange(long minDate, long maxDate) throws ConflictingConstraintException
    {

        if (hasReceiveDateRange)
        {
            throw new ConflictingConstraintException("You can't specify both a Device and a Receive date range constraint.");
        }
        if (hasDeviceDateRange)
        {
            throw new ConflictingConstraintException("You can't specify a Device date range constraint multiple times.");
        }

        logger.debug("Add Device date range: min={}, max={}", minDate, maxDate);

        hasDeviceDateRange = true;
        deviceMinDate = minDate;
        deviceMaxDate = maxDate;
        return this;
    }

    @Override
    public IQueryConstraints addReceiveDateRange(long minDate, long maxDate) throws ConflictingConstraintException
    {

        if (hasReceiveDateRange)
        {
            throw new ConflictingConstraintException("You can't specify a Receive date range constraint multiple times.");
        }
        if (hasDeviceDateRange)
        {
            throw new ConflictingConstraintException("You can't specify both a Receive and a Device date range constraint.");
        }

        logger.debug("Add Receive date range: min={}, max={}", minDate, maxDate);

        hasReceiveDateRange = true;
        receiveMinDate = minDate;
        receiveMaxDate = maxDate;
        return this;
    }

    @Override
    public IQueryConstraints addDayOfWeekConstraint(int[] days) throws ConflictingConstraintException
    {
        if (weekDays != null)
        {
            throw new ConflictingConstraintException("You can't specify two day of week constraints.");
        }

        weekDays = new ArrayList<>(days.length);

        for (int day : days)
        {
            if (day < 0 || day > 6) throw new IllegalArgumentException("Day numbers must be from 0 to 6; got " + day);
            weekDays.add(day);
        }

        if (logger.isDebugEnabled()) logger.debug("Add DOW: {}", Arrays.toString(days));

        return this;
    }

    @Override
    public IQueryConstraints addTimeBucketRange(Time minTime, Time maxTime) throws ConflictingConstraintException
    {
        if (hasTimeBucketRange)
        {
            throw new ConflictingConstraintException("You can't specify two time bucket constraints.");
        }

        logger.debug("Add time bucket range: min={}, max={}", minTime, maxTime);

        hasTimeBucketRange = true;
        minBucketTime = new Time(minTime.getTime());
        maxBucketTime = new Time(maxTime.getTime());
        return this;
    }

    @Override
    public IQueryConstraints addLocationRange(@Nonnull ILocation minLocation, @Nonnull ILocation maxLocation)
            throws ConflictingConstraintException
    {
        return addLocationRange(minLocation, maxLocation, false);
    }

    @Override
    public IQueryConstraints addLocationRange(@Nonnull ILocation minLocation, @Nonnull ILocation maxLocation, boolean strictCoordinates)
            throws ConflictingConstraintException, IllegalArgumentException
    {
        if (hasLocationRange)
        {
            throw new ConflictingConstraintException("You can't specify two location range constraints.");
        }

        logger.debug("Add Location range: min={}, max={}, strict={}", minLocation, maxLocation, strictCoordinates);

        // A little paranoia never hurt anyone
        validateRange(minLocation.getLatitude(), -90, 90, "minLocation.latitude");
        validateRange(maxLocation.getLatitude(), -90, 90, "maxLocation.latitude");
        validateRange(minLocation.getLongitude(), -180, 180, "minLocation.longitude");
        validateRange(maxLocation.getLongitude(), -180, 180, "maxLocation.longitude");

        hasLocationRange = true;
        this.minLocation = minLocation;
        this.maxLocation = maxLocation;
        this.strictCoordinates = strictCoordinates;
        return this;
    }

    /**
     * Validate that the given {@code value} is within the range {@code [lower, upper]}.
     * @throws IllegalArgumentException If the value is not within the given range
     * @since 3.4
     */
    private void validateRange(double value, double lower, double upper, String name)
    {
        if (value < lower || value > upper) throw new IllegalArgumentException(name + " is out of bounds [" + lower + "," + upper + "]");
    }

    @Override
    public IQueryConstraints addMessageStructureConstraint(String... messageStructures) throws NoSuchStructureException
    {

        for (String structureName : messageStructures)
        {
            // Ensure that the structure name is known to us
            if (!messageStructureMap.containsKey(structureName))
            {
                throw new NoSuchStructureException(structureName);
            }

            logger.debug("Add Message Structure: {}", structureName);

            structureSet.add(structureName);    // Add it in
        }
        return this;
    }

    @Override
    public IQueryConstraints addMessageStructureConstraint(IRaptorDataStructure structure) throws NoSuchStructureException
    {
        String structureName = structure.getMessageType();
        addMessageStructureConstraint(structureName);
        structureHashMap.put(structureName, structure.getStructure().getHashValue());
        if (logger.isDebugEnabled())
        {
            logger.debug("Added structure hash {}", structure.getStructure().getHashValue());
        }

        return this;
    }

    @Override
    public Set<String> getMessageStructureConstraint()
    {
        return Collections.unmodifiableSet(structureSet);
    }

    @Override
    public Set<String> getMessageStructureHashConstraints()
    {
        return Set.copyOf(structureHashMap.values());
    }

    @Override
    public Set<Integer> getMessageStructureIds()
    {
        // Start with the structureSet and skip any message types specified in structureHashMap
        final Stream<Integer> structureSetStream = structureSet.stream()
                .filter(messageType -> !structureHashMap.containsKey(messageType))
                .flatMap(messageType -> mapObjectPersist.findStructuresByMessageType(messageType).stream())
                .map(IStructure::getId);

        // Add the ids of the structures with the specific hashes in structureHashMap
        final Stream<Integer> structureHashMapStream = structureHashMap.values().stream()
                .map(hash -> mapObjectPersist.findStructureByHash(hash))
                .filter(Objects::nonNull)
                .map(IStructure::getId);

        return Stream.concat(structureSetStream, structureHashMapStream).collect(Collectors.toSet());
    }

    @Override
    public void clearMessageStructureConstraints()
    {
        structureHashMap.clear();
        structureSet.clear();
    }

    @Override
    public IQueryConstraints addFieldConstraint(CoreField field, ComparisonType comparator, Object value)
    {
        final FieldConstraint fc = new FieldConstraint(field.selector, comparator, value);

        internalAddCoreFieldConstraint(field, fc);

        return this;
    }

    private void internalAddCoreFieldConstraint(CoreField field, FieldConstraint constraint)
    {
        // If we don't already have a list for this field, then create one
        List<IConstraint> constraintList = coreFieldConstraints.computeIfAbsent(field, k -> new ArrayList<>());
        constraintList.add(constraint); // Add this constraint to the list for the field
    }

    @Override
    public IQueryConstraints addFieldConstraint(String structureName, String fieldName, ComparisonType comparator, Object value)
            throws NoSuchFieldException, NoSuchStructureException, ConflictingConstraintException
    {
        final StructureFieldSelector fieldSpec = new StructureFieldSelector(structureName, fieldName);
        final FieldConstraint fc = new FieldConstraint(fieldSpec, comparator, value);

        internalAddStructureFieldConstraint(fc);

        return this;
    }

    private void internalAddStructureFieldConstraint(FieldConstraint constraint)
            throws NoSuchFieldException, NoSuchStructureException, ConflictingConstraintException
    {
        final ASimpleFieldSelector fieldSelector = constraint.getField();
        final String structureName = fieldSelector.getStructureName();
        final String fieldName = fieldSelector.getFieldName();

        final Set<IStructure> structureList = messageStructureMap.get(structureName);
        if (structureList == null)
        {
            throw new NoSuchStructureException(structureName);
        }

        updateStructureUsage(structureName);

        final List<IDataField> matchingFields = new ArrayList<>();
        for (IStructure structure : structureList)
        {
            final IDataField field = structure.getField(fieldName);
            if (field != null)
            {
                matchingFields.add(field);
            }
        }

        if (matchingFields.isEmpty())
        {
            throw new NoSuchFieldException("Structure '" + structureName + "' has no field named '" + fieldName + "'");
        }

        for (IDataField field : matchingFields)
        {
            if (!legalFieldTypes.contains(field.getFieldType()))
            {
                throw new InvalidFieldTypeException("Unsupported type for field '" + fieldName + "'");
            }
        }

        // If we don't already have a list for this field, then create one
        List<IConstraint> constraintList = structureFieldConstraints.computeIfAbsent(fieldName, k -> new ArrayList<>());

        constraintList.add(constraint); // Add this constraint to the list for the field
    }

    @Override
    public IQueryConstraints addFieldConstraints(FieldConstraint... fieldConstraints)
            throws NoSuchFieldException, NoSuchStructureException, ConflictingConstraintException
    {
        for (FieldConstraint constraint : fieldConstraints)
        {
            final ASimpleFieldSelector field = constraint.getField();

            if (constraint.usesExtendedFields())
            {
                internalAddStructureFieldConstraint(constraint);
            } else
            {
                internalAddCoreFieldConstraint(((CoreFieldSelector) field).getCoreField(), constraint);
            }
        }

        return this;
    }

    @Override
    public boolean removeFieldConstraints(FieldConstraint... fieldConstraints)
    {
        boolean allRemoved = true;

        for (FieldConstraint fc : fieldConstraints)
        {
            // Scan both the Core and Structure constraint lists
            boolean removed = false;
            for (List<IConstraint> fcList : coreFieldConstraints.values())
            {
                if (fcList.remove(fc))
                {
                    removed = true;
                    break;
                }
            }

            // If we haven't found it yet, try the structure constraints
            if (!removed)
            {
                for (List<IConstraint> fcList : structureFieldConstraints.values())
                {
                    if (fcList.remove(fc))
                    {
                        removed = true;
                        break;
                    }
                }
            }

            allRemoved = allRemoved && removed;
        }

        return allRemoved;
    }

    @Override
    public IQueryConstraints addLogicConstraint(LogicType type, IConstraint... subConstraints)
            throws ConflictingConstraintException
    {
        if (LogicType.not == type && 1 != subConstraints.length)
        {
            throw new IllegalArgumentException("LogicType.NOT expects one and only one subconstraint");
        }

        return addLogicConstraints(new LogicConstraint(type, subConstraints));
    }

    @Override
    public IQueryConstraints addLogicConstraints(LogicConstraint... constraints) throws ConflictingConstraintException
    {
        // Validate all these constraints against any current structure reference
        final Set<String> structures = new HashSet<>();
        for (LogicConstraint constraint : constraints)
        {
            structures.addAll(constraint.getReferencedStructures());
        }

        updateStructureUsage(structures.toArray(new String[0]));

        logicConstraints.addAll(Arrays.asList(constraints));
        return this;
    }

    @Override
    public boolean removeLogicConstraints(LogicConstraint... logicConstraints)
    {
        boolean allRemoved = true;

        for (LogicConstraint lc : logicConstraints)
        {
            allRemoved = allRemoved && this.logicConstraints.remove(lc);
        }

        return allRemoved;
    }

    @Override
    public IQueryConstraints setGrouping(boolean grouping)
    {
        this.grouping = grouping;
        return this;
    }

    @Override
    public boolean isGrouping()
    {
        return grouping;
    }

    @Override
    public IQueryConstraints addGroupConstraint(IConstraint constraint) throws ConflictingConstraintException
    {
        if (!grouping)
        {
            throw new IllegalArgumentException("Can't add a 'GroupConstraint' until after setGrouping(true).");
        }

        // Ensure our structure references/checks are all handled
        final Set<String> structures = constraint.getReferencedStructures();
        updateStructureUsage(structures.toArray(new String[0]));

        groupConstraints.add(constraint);
        return this;
    }

    @Override
    public IQueryConstraints addGroupConstraints(IConstraint... constraints) throws ConflictingConstraintException
    {
        if (!grouping)
        {
            throw new IllegalArgumentException("Can't add a 'GroupConstraint' until after setGrouping(true).");
        }

        // Ensure our structure references/checks are all handled
        for (IConstraint constraint : constraints)
        {
            final Set<String> structures = constraint.getReferencedStructures();
            updateStructureUsage(structures.toArray(new String[0]));
        }

        Collections.addAll(groupConstraints, constraints);
        return this;
    }

    @Override
    public boolean removeGroupConstraints(IConstraint... constraints)
    {
        boolean allRemoved = true;

        for (IConstraint c : constraints)
        {
            allRemoved = allRemoved && groupConstraints.remove(c);
        }

        return allRemoved;
    }

    @Override
    public IQueryConstraints addShapeConstraint(IShape shape)
    {
        return addShapeConstraint(new ShapeConstraint(shape));
    }

    @Override
    public IQueryConstraints addShapeConstraint(ILocation minCorner, ILocation maxCorner)
    {
        return addShapeConstraint(new ShapeConstraint(minCorner, maxCorner));
    }

    @Override
    public IQueryConstraints addShapeConstraint(ILocation center, double radius)
    {
        return addShapeConstraint(new ShapeConstraint(center, radius));
    }

    @Override
    public IQueryConstraints addShapeConstraint(List<ILocation> vertices)
    {
        return addShapeConstraint(new ShapeConstraint(vertices));
    }

    @Override
    public IQueryConstraints addShapeConstraint(ShapeConstraint constraint)
    {
        if (constraint == null) throw new IllegalArgumentException("constraint must not be null");

        shapeConstraints.add(constraint);
        return this;
    }

    @Override
    public boolean removeShapeConstraints(ShapeConstraint... shapeConstraints)
    {
        boolean allRemoved = true;

        for (IConstraint c : shapeConstraints)
        {
            allRemoved = allRemoved && this.shapeConstraints.remove(c);
        }

        return allRemoved;
    }

    @Override
    public void setSortingOptions(SortOn sortOn, SortOrder sortOrder) throws IllegalArgumentException
    {
        if ((sortOn == null) == (sortOrder != null))
        {
            throw new IllegalArgumentException("sortOn and sortOrder must both be null or both have values");
        }

        if (sortOn == SortOn.FIELD)
        {
            throw new IllegalArgumentException("Cannot set sortOn to SortOn.FIELD; to sort on a field, use setSortingOptions(ASimpleFieldSelector sortOnFieldSelector, SortOrder sortOrder)");
        }

        this.sortOn = sortOn;
        sortOnFieldSelector = null;
        this.sortOrder = sortOrder;
    }

    @Override
    public void setSortingOptions(ASimpleFieldSelector sortOnFieldSelector, SortOrder sortOrder) throws IllegalArgumentException
    {
        if (sortOnFieldSelector.isCoreField() &&
                Arrays.stream(illegalSortFields).anyMatch(coreField -> ((CoreFieldSelector) sortOnFieldSelector).getCoreField() == coreField))
        {
            throw new IllegalArgumentException("Sorting by \"" + ((CoreFieldSelector) sortOnFieldSelector).getCoreField() + "\" is not supported.");
        }

        sortOn = SortOn.FIELD;
        this.sortOnFieldSelector = sortOnFieldSelector;
        this.sortOrder = sortOrder;
    }

    @Override
    public SortOn getSortOn()
    {
        return sortOn;
    }

    @Override
    public ASimpleFieldSelector getSortOnFieldSelector()
    {
        return sortOnFieldSelector;
    }

    @Override
    public SortOrder getSortOrder()
    {
        return sortOrder;
    }

    @Override
    public boolean isSorted()
    {
        return sortOn != null;
    }

    @Override
    public void setMaxMessageCount(int maxMessages)
    {
        this.maxMessages = maxMessages;
    }

    @Override
    public int getMaxMessageCount()
    {
        return maxMessages;
    }

    @Override
    public void setStartMessageCount(int startMessage)
    {
        this.startMessage = startMessage;
    }

    @Override
    public int getStartMessageCount()
    {
        return startMessage;
    }

    @Override
    public Collection<List<IConstraint>> getCoreFieldConstraints()
    {
        return Collections.unmodifiableCollection(coreFieldConstraints.values());
    }

    @Override
    public Collection<List<IConstraint>> getExtendedFieldConstraints()
    {
        return Collections.unmodifiableCollection(structureFieldConstraints.values());
    }

    @Override
    public List<IConstraint> getShapeConstraints()
    {
        return Collections.unmodifiableList(shapeConstraints);
    }

    @Override
    public List<IConstraint> getLogicConstraints()
    {
        return Collections.unmodifiableList(logicConstraints);
    }

    @Override
    public List<IConstraint> getGroupConstraints()
    {
        return Collections.unmodifiableList(groupConstraints);
    }

    @Override
    public boolean isExtendedClear()
    {
        return extendedClear;
    }

    @Override
    public void setExtendedClear(boolean extendedClear)
    {
        this.extendedClear = extendedClear;
    }

    @Override
    public void clear()
    {
        if (extendedClear)
        {
            // Clear everything, including the device type and unit ID domain restrictions.
            mapObjects.clear();
            messageStructureMap.clear();
        }

        hasDeviceDateRange = false;
        hasReceiveDateRange = false;
        hasLocationRange = false;
        hasTimeBucketRange = false;
        minBucketTime = null;
        maxBucketTime = null;
        weekDays = null;
        minLocation = null;
        maxLocation = null;
        messageIdSet.clear();
        structureSet.clear();
        structureHashMap.clear();
        fieldStructureName = null;
        coreFieldConstraints.clear();
        structureFieldConstraints.clear();
        logicConstraints.clear();
        shapeConstraints.clear();
        startMessage = 0;
        maxMessages = DEFAULT_MAX_MESSAGES;
        grouping = false;
        groupConstraints.clear();
        sortOn = null;
        sortOrder = null;
        generator.reset();
    }

    @Override
    public Map<String, Set<IStructure>> getMessageStructureMap()
    {
        return Collections.unmodifiableMap(messageStructureMap);
    }

    /**
     * @return The HQL query string to count the number of messages that match our constraints
     */
    public String getHQLForCount()
    {
        return generator.getHQLForCount();
    }

    /**
     * @return The HQL query string to retrieve the messages that match our constraints
     */
    public String getHQLForMessages()
    {
        return generator.getHQLForMessages();
    }

    /**
     * @param fieldSelector field selector indicating which fields are to be retrieved by the query
     * @return The HQL query string to retrieve the fields of the messages that match our constraints
     * @throws ConflictingConstraintException If the field selector and field constraints are on different structures
     */
    public String getHQLForFields(FieldSelector fieldSelector) throws ConflictingConstraintException
    {
        // Ensure that any current extended field constraints are on the same table as the field selector
        if (fieldSelector.usesExtendedFields())
        {
            updateStructureUsage(fieldSelector.getStructure());
        }

        return generator.getHQLForFields(fieldSelector);
    }

    /**
     * @return The generated query parameters; the query must have been finalized before you call this method.
     */
    public List<HQLParameter> getHQLParametersList()
    {
        return generator.getParams();
    }

    /**
     * @return The set of structures referenced in the query, may be empty
     * @since 2.37
     */
    public Set<IStructure> getStructuresUsed()
    {
        return messageStructureMap.getOrDefault(fieldStructureName, Collections.emptySet());
    }

    /**
     * Get the "fieldStructureName" finalized based on any selector or constraint currently specified.  Throw
     * appropriate exceptions if more than one structure is referenced anywhere.
     *
     * @param structures structure names to process (no duplicates are allowed)
     * @throws ConflictingConstraintException If more than a single structure is referenced in our selectors/constraints
     */
    private void updateStructureUsage(String... structures) throws ConflictingConstraintException
    {
        if (structures.length == 0) return; // no structure actually referenced

        // If we have more than a single structure in use, then we have a conflict
        if (structures.length > 1)
        {
            throw new ConflictingConstraintException("Multiple structures specified on different constraints/selectors: '"
                    + Arrays.toString(structures));
        }

        final String structure = structures[0];

        // Ensure that any extended field references are all on the same structure
        if (fieldStructureName != null)
        {
            if (!structure.equals(fieldStructureName))
            {
                throw new ConflictingConstraintException("Multiple structures specified on different constraints/selectors: '"
                        + fieldStructureName + "' and '" + structure + "'");
            }
        } else
        {
            fieldStructureName = structure; // Finalize the fieldStructureName with the extended structure being used
        }
    }
}
