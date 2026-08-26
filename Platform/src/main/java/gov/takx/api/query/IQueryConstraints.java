package gov.takx.api.query;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.IStructure;
import gov.takx.api.messages.NoSuchStructureException;
import gov.takx.api.shapes.IShape;
import jakarta.annotation.Nonnull;

import java.sql.Time;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface defining how to specify query constraints for use by an {@link IQueryService} instance.
 * A query can be constrained by the following parameters:
 * <p>
 * <ul>
 * <li><b>Date range</b> - a range of date/time values as compared to either the time the device generated
 * the message or the time that the message was received by the plugin.</li>
 * <li><b>Location range</b> - a lat/long/elevation cube bounded by two {@link ILocation}
 * instances.  If no elevation data is supplied in the location instances, then the query is unconstrained with
 * respect to elevation values on the messages.</li>
 * <li><b>Data Field value</b> - a comparison type (like equals or less than) and a value to be compared against.
 * For example, if a device message has a field named {@code SIGNAL_STRENGTH}, the constraint could be
 * {@code SIGNAL_STRENGTH &lt; 5.0}.  See {@link ComparisonType} for the legal comparison types.</li>
 * </ul>
 * <p>
 * Note that all constraints specified are conjunctive, meaning that they must <b>ALL</b> pass for a message
 * to be returned.  At present there is no support for disjunctive queries.
 *
 */
@NoPluginImplementations
public interface IQueryConstraints
{
    /**
     * The default maximum number of messages that {@link IQueryService#query(IQueryConstraints)} will return.
     */
    int DEFAULT_MAX_MESSAGES = 10000;

    /**
     * Enum specifying the property on which to sort the query results.
     */
    enum SortOn
    {
        /**
         * Sort on the device generated time in the message
         */
        DEVICE_DATE,

        /**
         * Sort on the receive time of the message
         */
        RECEIVE_DATE,

        /**
         * Sort on the relevant time of the message
         */
        RELEVANT_TIME,

        /**
         * Sort on the location (by latitude, then by longitude) of the message
         */
        LOCATION,

        /**
         * Sort on one of the SELECT fields, whose value will be set in sortOnExtended
         */
        FIELD
    }

    /**
     * Enum specifying the order of the sort
     */
    enum SortOrder
    {
        ASCENDING,
        DESCENDING
    }

    /**
     * Set the map object.  This limits the query domain to only those messages that come from the given mapObject.
     *
     * @param mapObject map object
     * @since 3.0
     */
    void setMapObject(IMapObject mapObject);

    /**
     * Set the set of map objects.  This limits the query domain to only those messages that come from one of the
     * given map objects. Essentially only messages from some version of a specific device instance.
     *
     * @param mapObjects Set of map objects
     * @since 3.0
     */
    void setMapObjects(Set<IMapObject> mapObjects);

    /**
     * Set the list of known message structures to be the list associated with the map object of the
     * given family and type.  The list of messages structures is required when using any field-based
     * constraints or structure name constraints.
     *
     * @param family Map object family
     * @param type   Map object type
     * @see #addMessageStructureConstraint(String[])
     * @see #addFieldConstraint(String, String, ComparisonType, Object)
     * @since 2.30
     */
    void setStructuresFromMapObject(String family, String type);

    /**
     * Set the extended clear flag.  When true, calling {@link #clear()}, in addition to clearing the standard
     * constraints, will also clear any restrictions added with the
     * {@link #setMapObjects(java.util.Set)} and the structure set put in place using
     * {@link #setStructuresFromMapObject(String, String)}.  Once clear() is called, if any constraints
     * that require a structure reference will be used, then another call to
     * {@link #setStructuresFromMapObject(String, String)} must be made.
     *
     * @since 2.0
     */
    void setExtendedClear(boolean extendedClear);

    /**
     * @return The extended clear setting
     * @see #setExtendedClear(boolean)
     * @since 2.0
     */
    boolean isExtendedClear();

    /**
     * Add a collection of message structures that define the fields that can be used in field constraints.
     *
     * @param messageStructures List of message structures to add
     * @see #addFieldConstraint(String, String, ComparisonType, Object)
     * @since 2.25
     */
    void addKnownMessageStructures(@Nonnull Set<IRaptorDataStructure> messageStructures);

    /**
     * Add a constraint against a set of message IDs.  Only the messages with the specified unique ID,
     * obtained by {@link IRaptorDataMessage#getId()}, will be included in
     * the results.  Other constraints may limit the results further.
     *
     * @param ids Collection of message ids
     * @return This object to allow chained calls to add methods
     */
    IQueryConstraints addMessageIds(@Nonnull Collection<Integer> ids);

    /**
     * Add a constraint against a set of message IDs.  Only the messages with the specified unique ID,
     * obtained by {@link IRaptorDataMessage#getId()}, will be included in
     * the results.  Other constraints may limit the results further.
     *
     * @param ids variable argument list of message ids
     * @return This object to allow chained calls to add methods.
     */
    IQueryConstraints addMessageIds(Integer... ids);

    /**
     * Add a constraint against the time at which the device generated the message.  The device generation
     * date/time of the message must be between the {@code minDate} and {@code maxDate}, inclusively.
     * That is, the following must hold true: {@code minDate &lt;= <i>deviceDate</i> &lt;= maxDate}
     * <p>
     * It is illegal to add multiple device date constraints or to add a constraint on both the device time
     * and the receive time.
     *
     * @param minDate Minimum date of the messages to be selected.
     * @param maxDate Maximum date of the messages to be selected.
     * @return This object to allow chained calls to add methods.
     * @throws ConflictingConstraintException If a receive date constraint has already been added using
     *                                        {@link #addReceiveDateRange(long, long)}
     */
    IQueryConstraints addDeviceDateRange(long minDate, long maxDate) throws ConflictingConstraintException;

    /**
     * Add a constraint against the time at which the message was received by the plugin handler. The receive
     * date/time of the message must be between the {@code minDate} and {@code maxDate}, inclusively.
     * That is, the following must hold true: {@code minDate &lt;= <i>receiveDate</i> &lt;= maxDate}
     * <p>
     * It is illegal to add multiple receive date constraints or to add a constraint on both the device time
     * and the receive time.
     *
     * @param minDate Minimum date of the messages to be selected.
     * @param maxDate Maximum date of the messages to be selected.
     * @return This object to allow chained calls to add methods.
     * @throws ConflictingConstraintException If a device date constraint has already been added using
     *                                        {@link #addDeviceDateRange(long, long)}
     */
    IQueryConstraints addReceiveDateRange(long minDate, long maxDate) throws ConflictingConstraintException;

    /**
     * Add a constraint against the day of the week as determined from the <em>relevant time</em> of the message.
     * The relevant time is the device time if the message has a device time, otherwise it is the receive time.
     * This constraint allows one to specify individual days of the week that must match.  For example, you could
     * specify that only messages with a relevant time occurring on Monday or Tuesday are to be matched.
     * <p>
     * The days are number from 0 (Sunday) to 6 (Saturday), as defined in {@link DayOfWeek}.
     *
     * @param days An array of days to match
     * @return This object to allow chained calls to add methods.
     * @throws ConflictingConstraintException If a call to this method has already been made
     * @see DayOfWeek
     */
    IQueryConstraints addDayOfWeekConstraint(int[] days) throws ConflictingConstraintException;

    /**
     * Add a constraint against <em>relevant time</em> of the message.
     * The relevant time is the device time if the message has a device time, otherwise it is the receiveTime.
     * This constraint allows one to select messages based on the time of the day, irrespective of the date.
     * Thus, one could search for messages that have times from 8am - 11am on any date.
     *
     * @param minTime The minimum time of the messages to be selected
     * @param maxTime The maximum time of the messages to be selected
     * @return This object to allow chained calls to add methods.
     * @throws ConflictingConstraintException If a call to this method has already been made
     */
    IQueryConstraints addTimeBucketRange(Time minTime, Time maxTime) throws ConflictingConstraintException;

    /**
     * Add a constraint against the recorded location in the message.  The min and max location parameters define
     * a cube in 3D space and the location specified in the message must lie within this cube to be selected.  If
     * the elevation is not specified in either {@code minLocation} or {@code maxLocation} then the
     * elevation of messages is not included in the selection criteria.  It is illegal to specify multiple
     * location constraints by calling this method more than once.
     * <p>
     * This method makes a "normalizing" assumption on the coordinates provided.  This means that "min" and "max"
     * are not strictly enforced.  Instead, the coordinates are inspected and the true bounding box is constructed
     * from the minimum (most westerly) longitude and minimum (most southerly) latitude of either coordinate as the
     * "minimum," and the maximum (most easterly) longitude and maximum (most northerly) latitude of either coordinate
     * as the "maximum" coordinate.
     * <p>
     * For example (ignoring elevations and assuming a notation of (lat,lon)), if {@code minLocation} is
     * {@code (30,100)} and {@code maxLocation} is {@code (20,80)}, then the actual coordinates used
     * will be {@code (20,80) - (30,100)}.
     * <p>
     * This can have "unexpected" results when the coordinates span the date line (where -180 becomes +180).  In this
     * case, the normalizing behavior will generate a bounding box that "avoids" the date line by always spanning
     * east from the minimum longitude to the maximum longitude.  If you are working on date-line spanning positions
     * and need to control this behavior, then see {@link #addLocationRange(ILocation, ILocation, boolean)}.
     *
     * @param minLocation The minimum lat/long/elevation to be selected.
     * @param maxLocation The maximum lat/long/elevation to be selected.
     * @return This object to allow chained calls to add methods.
     * @throws ConflictingConstraintException If a call to this method has already been made
     */
    IQueryConstraints addLocationRange(@Nonnull ILocation minLocation, @Nonnull ILocation maxLocation) throws ConflictingConstraintException;

    /**
     * Add a constraint against the recorded location in the message.  The min and max location parameters define
     * a cube in 3D space and the location specified in the message must lie within this cube to be selected.  If
     * the elevation is not specified in either {@code minLocation} or {@code maxLocation} then the
     * elevation of messages is not included in the selection criteria.  It is illegal to specify multiple
     * location constraints by calling this method more than once.
     * <p>
     * This method makes a "normalizing" assumption on the coordinates provided.  This means that "min" and "max"
     * are not strictly enforced.  Instead, the coordinates are inspected and the true bounding box is constructed
     * from the minimum (most westerly) longitude and minimum (most southerly) latitude of either coordinate as the
     * "minimum," and the maximum (most easterly) longitude and maximum (most northerly) latitude of either coordinate
     * as the "maximum" coordinate.
     * <p>
     * For example (ignoring elevations and assuming a notation of (lat,lon)), if {@code minLocation} is
     * {@code (30,100)} and {@code maxLocation} is {@code (20,80)}, then the actual coordinates used
     * will be {@code (20,80) - (30,100)}.
     * <p>
     * This can have "unexpected" results when the coordinates span the date line (where -180 becomes +180).  In this
     * case, the normalizing behavior will generate a bounding box that "avoids" the date line by always spanning
     * east from the minimum longitude to the maximum longitude.
     * <p>
     * If {@code strictCoordinates} is passed as true, then no normalization will be performed on the longitude
     * portions of the coordinates.  This would allow for a coordinate pair of {@code (170,10) , (-170,20)}
     * to encompass a 20 degree spanning arc that crosses the date line.  The bounding box spans from the minimum
     * longitude easterly to the maximum longitude.
     * <p>
     * Note, however, that normalization will still occur on the latitude component of the coordinates.  The
     * bounding box is not allowed to span either pole. If you require this behavior, you will need to perform
     * multiple queries using non-polar spanning coordinate pairs.
     *
     * @param minLocation       The minimum lat/long/elevation to be selected.
     * @param maxLocation       The maximum lat/long/elevation to be selected.
     * @param strictCoordinates If true, use a strict interpretation of the coordinates, no normalization is done
     * @return This object to allow chained calls to add methods.
     * @throws ConflictingConstraintException If a call to this method has already been made
     * @throws IllegalArgumentException If the latitude or longitude of the given minLocation or maxLocation are out of range
     */
    IQueryConstraints addLocationRange(@Nonnull ILocation minLocation, @Nonnull ILocation maxLocation, boolean strictCoordinates)
            throws ConflictingConstraintException, IllegalArgumentException;

    /**
     * Add one or more message structure constraints.  Only messages of the given structure(s) will be selected.  Note that
     * this method is slightly different from the others in that it may be called multiple times.  Each time it
     * is called, the list of message structures is augmented with the new values.  The final set of message structures
     * defines the constraint.  This allows the full constraint to be built up piece-meal.  For example:
     * <pre>
     * IQueryConstraints qc = queryService.createEmptyConstraints();
     * qc.addMessageStructureConstraint( "LOCATION" );
     * qc.addMessageStructureConstraint( "STATUS", "EXTENDED_STATUS" );
     * </pre>
     * The preceding code will match messages with a message structure equal to any of {@code LOCATION},
     * {@code STATUS}, or {@code EXTENDED_STATUS}.  Specifying message structure more than once is the
     * same as specifying it once.
     * <p>
     * Note: {@link #setStructuresFromMapObject(String, String)} <b>must</b> be called before calling this method.
     *
     * @param messageStructures One or more message structure names
     * @return This object to allow chained calls to add methods.
     * @throws NoSuchStructureException If a structure named is not known for this query domain
     * @see #setStructuresFromMapObject(String, String)
     */
    IQueryConstraints addMessageStructureConstraint(String... messageStructures) throws NoSuchStructureException;

    /**
     * Return the set of message structure names specified in previous calls to {@link #addMessageStructureConstraint(String...)}.
     * If no message structure constraints have been added, this will return an empty set.
     *
     * @return Set of structure names, may be empty
     */
    Set<String> getMessageStructureConstraint();

    /**
     * Add a single absolute structure constraint.  Only messages of the given structure matching exact hash value
     * will be selected (as opposed to the method that takes the string name of the structure / message type which
     * will select all versions).  Each time it is called, the list of message structures is augmented with the new
     * values.  The final set of message structures defines the constraint.  This allows the full constraint to be
     * built up piece-meal.  For example:
     * <pre>
     * IQueryConstraints qc = queryService.createEmptyConstraints();
     * qc.addMessageStructureConstraint(locationStructure);
     * qc.addMessageStructureConstraint(statusStructure);
     * </pre>
     * The preceding code will match messages with a message structure equal to the exact version of
     * {@code LOCATION} and {@code STATUS} structures. Specifying message structure more than once is the
     * same as specifying it once.
     * <p>
     * Note specifically, that if you have previously added a message structure constraint using the
     * {@link #addMessageStructureConstraint(String...)} method with a structure name matching the structure passed
     * to this method, then that prior constraint will be ignored.  Only the exact structure specified in this
     * method call will be included in the results.
     *
     * @param structure Message structure to match against.
     * @return This object to allow chained calls to add methods.
     * @throws NoSuchStructureException If a structure named is not known for this query domain
     * @since 2.0.0
     */
    IQueryConstraints addMessageStructureConstraint(IRaptorDataStructure structure);

    /**
     * Return the set of message structure hash constraints constructed in previous calls to
     * {@link #addMessageStructureConstraint(IRaptorDataStructure)}.  If no message structure hash constraints have been
     * added, this will return an empty set.
     *
     * @return Set of structure hash constraints, may be empty.
     */
    Set<String> getMessageStructureHashConstraints();

    /**
     * Return the set of Structure Ids configured with the various add structure constraint methods.
     * An RDM must have a structure id in this set to match these constraints.
     *
     * @return Set of structure ids to match against
     * @see #addMessageStructureConstraint(String...)
     * @see #addMessageStructureConstraint(IRaptorDataStructure)
     * @since 2.38
     */
    Set<Integer> getMessageStructureIds();

    /**
     * Clears any message structure constraints previously added (either by name or by hash).
     *
     * @see #addMessageStructureConstraint(String...)
     * @see #addMessageStructureConstraint(IRaptorDataStructure)
     * @since 2.31
     */
    void clearMessageStructureConstraints();

    /**
     * Add a constraint against a field associated with the message.  Fields are defined in the structure
     * that defines the message.  Multiple field constraints may be added for the same field.  This allows for
     * the specification of value ranges.  For example:
     * <pre>
     * IQueryConstraints qc = queryService.createEmptyConstraints();
     * qc.addFieldConstraint( "STATUS", "voltage", ComparisonType.GT, 11.0 );
     * qc.addFieldConstraint( "STATUS", "voltage", ComparisonType.LT, 13.0 );
     * </pre>
     * The preceding code creates a set of constraints that will limit messages returned to those where the
     * condition {@code 11.0 &lt; voltage &lt; 13.0} holds true.  The {@code voltage} field must be defined
     * in the {@code STATUS} message type.
     *
     * @param field      The {@link CoreField} to compare against
     * @param comparator The type of comparison to perform
     * @param value      The value to compare against
     * @return This object to allow chained calls to add methods.
     */
    IQueryConstraints addFieldConstraint(CoreField field, ComparisonType comparator, Object value);

    /**
     * Add a constraint against a field associated with the message.  Fields are defined in the structure
     * that defines the message.  Multiple field constraints may be added for the same field.  This allows for
     * the specification of value ranges.  For example:
     * <pre>
     * IQueryConstraints qc = queryService.createEmptyConstraints();
     * qc.addFieldConstraint( "STATUS", "voltage", ComparisonType.GT, 11.0 );
     * qc.addFieldConstraint( "STATUS", "voltage", ComparisonType.LT, 13.0 );
     * </pre>
     * The preceding code creates a set of constraints that will limit messages returned to those where the
     * condition {@code 11.0 &lt; voltage &lt; 13.0} holds true.  The {@code voltage} field must be defined
     * in the {@code STATUS} message type.
     * <p>
     * <b>Note:</b>The introduction of the various array data types in 1.7 provides a set of fields that presently
     * are unsearchable.  Searching for a match to an array using the IQueryConstraints interface is undefined.  If
     * this capability is required, please issue a formal request.
     * <p>
     * Note: {@link #setStructuresFromMapObject(String, String)} <b>must</b> be called before calling this method.
     *
     * @param structureName The name of the message structure in which {@code field} is defined
     * @param fieldName     The name of the field to compare against
     * @param comparator    The type of comparison to perform
     * @param value         The value to compare against
     * @return This object to allow chained calls to add methods.
     * @throws NoSuchFieldException           If the field specified is not defined in the specified message type
     * @throws NoSuchStructureException       If the message specified is not found in the domain of the query service
     * @throws ConflictingConstraintException If the structureName specified in this call is different than in any
     *                                        previous call.  Simply put, you can only query on fields defined by a
     *                                        single message structure at a time.
     * @see #setStructuresFromMapObject(String, String)
     */
    IQueryConstraints addFieldConstraint(String structureName, String fieldName, ComparisonType comparator, Object value)
            throws NoSuchFieldException, NoSuchStructureException, ConflictingConstraintException;

    /**
     * Add a set of field-level constraints.  The set may consist of both core and structure field constraints.
     *
     * @param fieldConstraints Field constraints to add
     * @return This object to allow chained calls
     * @throws NoSuchFieldException           If a field specified is not defined in the specified message type.
     *                                        This can only be thrown if the set includes extended field constraints.
     * @throws NoSuchStructureException       If the message specified is not found in the domain of the query service
     *                                        This can only be thrown if the set includes extended field constraints.
     * @throws ConflictingConstraintException If the structureName specified in this call is different than in any
     *                                        previous call.  Simply put, you can only query on fields defined by a
     *                                        single message structure at a time.
     *                                        This can only be thrown if the set includes extended field constraints.
     * @see #addFieldConstraint(CoreField, ComparisonType, Object)
     * @see #addFieldConstraint(String, String, ComparisonType, Object)
     */
    IQueryConstraints addFieldConstraints(FieldConstraint... fieldConstraints)
            throws NoSuchFieldException, NoSuchStructureException, ConflictingConstraintException;

    /**
     * Remove a previously added field constraint.  Constraints are matched using {@link Object#equals(Object)}, not identity.
     *
     * @param fieldConstraints Varargs list of field constraints to remove
     * @return {@code true} if all the constraints specified were found (and removed), {@code false} if any constraint
     * could not be found in the current field constraints.
     * @since 2.0
     */
    boolean removeFieldConstraints(FieldConstraint... fieldConstraints);

    /**
     * Add a constraint formed as the application of a LogicType, one of AND, OR or NOT, to subConstraints.  The NOT
     * LogicType can only be applied to one subConstraint; the AND or OR LogicTypes can be applied to an arbitrary
     * number of subConstraints.  The subConstraints themselves are typed as IConstraint and thus subConstraints can be
     * composed with LogicConstraints.  For example:
     * <pre>
     * IQueryConstraints qc = queryService.createEmptyConstraints();
     * qc.addLogicConstraint (LogicType.not,
     *       new LogicConstraint (LogicType.and,
     *             new FieldConstraint (CoreField.elevation, Comparison.GTE, 25.0),
     *             new FieldConstraint (CoreField.elevation, Comparison.LTE, 75.0)));
     * </pre>
     * The preceding code produces a query that will find messages whereby {@code elevation < 25.0 || elevation > 75.0}.
     * The SQL generated for the above is {@code not (rdm.elevation >= 25.0 and rdm.elevation <= 75.0)}
     * <p>
     * Note that subConstraints are typed as IConstraint and can thus include AggregateFieldConstraints; however,
     * AggregateFieldConstraints are not valid in the WHERE clause of a SELECT.  An error will be flagged if an
     * AggregateFieldConstraint shows up in a LogicConstraint used for SELECT.
     *
     * @param type           the logic type to apply
     * @param subConstraints the subConstraints
     * @return This object to allow chained calls to add methods
     * @throws ConflictingConstraintException If any of the constraints are in conflict
     * @since 1.49
     */
    IQueryConstraints addLogicConstraint(LogicType type, IConstraint... subConstraints) throws ConflictingConstraintException;

    /**
     * Add LogicConstraints to this query.
     *
     * @param constraints varargs constraints to add
     * @return This object to allow chained calls to add methods
     * @throws ConflictingConstraintException If any of the constraints are in conflict
     * @since 1.49
     */
    IQueryConstraints addLogicConstraints(LogicConstraint... constraints) throws ConflictingConstraintException;

    /**
     * Remove a previously added logic constraint.  Constraints are matched using {@link Object#equals(Object)}, not identity.
     *
     * @param logicConstraints Varargs list of logic constraints to remove
     * @return {@code true} if all the constraints specified were found (and removed), {@code false} if any constraint
     * could not be found in the current logic constraints.
     * @since 2.0
     */
    boolean removeLogicConstraints(LogicConstraint... logicConstraints);

    /**
     * Add a shape constraint limiting the results to those that have a location within the given shape.
     * The restrictions on shape type are the same as those defined in {@link ShapeConstraint}.
     *
     * @param shape The shape to use to construct the {@link ShapeConstraint}.
     * @return This object to allow chained calls
     */
    IQueryConstraints addShapeConstraint(IShape shape);

    /**
     * Add a shape constraint limiting results to those that have a location within the BOX (square/rectangle)
     * defined by the given min/max corners.
     *
     * @param minCorner Min corner
     * @param maxCorner Max corner
     * @return This object to allow chained calls
     */
    IQueryConstraints addShapeConstraint(ILocation minCorner, ILocation maxCorner);

    /**
     * Add a shape constraint limiting results to those that have a location within the CIRCLE defined by the
     * given center and radius.
     *
     * @param center Center location of circle
     * @param radius Radius of circle
     * @return This object to allow chained calls
     */
    IQueryConstraints addShapeConstraint(ILocation center, double radius);

    /**
     * Add a shape constraint limiting results to those that have a location within the POLYGON defined by the given
     * list of locations (vertices on polygon).  The polygon does not need to be closed, it will automatically
     * closed if the first and last points are not identical.
     *
     * @param vertices Polygon vertices
     * @return This object to allow chained calls
     */
    IQueryConstraints addShapeConstraint(List<ILocation> vertices);

    /**
     * Add a shape constraint.
     *
     * @param constraint The constraint to add
     * @return This object to allow chained calls to add methods
     */
    IQueryConstraints addShapeConstraint(ShapeConstraint constraint);

    /**
     * Remove a previously added shape constraint.  Constraints are matched using {@link Object#equals(Object)}, not identity.
     *
     * @param shapeConstraints Varargs list of shape constraints to remove
     * @return {@code true} if all the constraints specified were found (and removed), {@code false} if any constraint
     * could not be found in the current shape constraints.
     * @since 2.0
     */
    boolean removeShapeConstraints(ShapeConstraint... shapeConstraints);

    /**
     * Enable/disable grouping for this QueryConstraint.  When grouping is enabled, it leads to generated SQL with
     * a GROUP BY clause.  As per, SQL standards, the GROUP BY fields are composed of the SELECT fields w/ any
     * aggregate fields in SELECT removed.
     * <p>
     * One must call {@code setGrouping(true)} prior to adding grouping constraints.
     *
     * @return This object to allow changed calls to add methods
     * @see #addGroupConstraints(IConstraint...)
     * @since 1.49
     */
    IQueryConstraints setGrouping(boolean grouping);

    /**
     * @return current grouping setting
     * @since 1.49
     */
    boolean isGrouping();

    /**
     * Add a constraint to the HAVING clause of a GROUP BY statement.
     *
     * @param constraint the constraint
     * @return This object to allow changed calls to add methods
     * @throws ConflictingConstraintException If any of the constraints are in conflict
     * @since 1.49
     */
    IQueryConstraints addGroupConstraint(IConstraint constraint) throws ConflictingConstraintException;

    /**
     * Add multiple constraints to the grouping.  See {@link #addGroupConstraint(IConstraint)}
     * <p>
     * This is a convenience method
     *
     * @param constraints the vector of constraints to add
     * @return This object to allow changed calls to add methods
     * @throws ConflictingConstraintException If any of the constraints are in conflict
     * @since 1.49
     */
    IQueryConstraints addGroupConstraints(IConstraint... constraints) throws ConflictingConstraintException;

    /**
     * Remove a previously added group constraint.  Constraints are matched using {@link Object#equals(Object)}, not identity.
     *
     * @param constraints Varargs list of group constraints to remove
     * @return {@code true} if all the constraints specified were found (and removed), {@code false} if any constraint
     * could not be found in the current group constraints.
     * @since 2.0
     */
    boolean removeGroupConstraints(IConstraint... constraints);

    /**
     * Set the sorting options for the selection.  The resulting messages selected will be ordered according
     * to the criteria specified.  Note that only one ordering can be specified.  Calling this method a second
     * time overwrites any previously set ordering.
     * <p>
     * In order to clear a previously set sort ordering, you must call this method again with {@code null} values for
     * {@code sortOn} and {@code sortOrder}.
     *
     * @param sortOn    Which field to sort on, pass {@code null} for no sorting
     * @param sortOrder Direction of sort, pass {@code null} for no sorting
     * @throws IllegalArgumentException If one of sortOn or sortOrder is {@code null} and they are not both {@code null}
     */
    void setSortingOptions(SortOn sortOn, SortOrder sortOrder) throws IllegalArgumentException;

    /**
     * Sort the selection by an arbitrary field.  The resulting messages selected will be ordered according
     * to the criteria specified.  Note that only one ordering can be specified.  Calling this method a second
     * time overwrites any previously set ordering.
     * <p>
     * In order to clear a previously set sort ordering, you must call this method again with {@code null} values for
     * {@code sortOnFieldSelector} and {@code sortOrder}.
     * <p>
     * The {@code sortOnFieldSelector} value must be a column that is selected by the query. Otherwise a SQL
     * exception will be thrown
     *
     * @param sortOnFieldSelector Which field to sort on
     * @param sortOrder           Direction of sort, pass {@code null} for no sorting
     * @throws IllegalArgumentException If one of sortOn or sortOrder is {@code null} and they are not both {@code null}
     */
    void setSortingOptions(ASimpleFieldSelector sortOnFieldSelector, SortOrder sortOrder) throws IllegalArgumentException;

    /**
     * @return Field sort option, if any. May be {@code null}.
     */
    SortOn getSortOn();

    /**
     * @return FieldSelector for field to sort on if {@link #getSortOn()} returns {@code SortOn.FIELD}. May be {@code null}.
     */
    ASimpleFieldSelector getSortOnFieldSelector();

    /**
     * @return Field sort order, if any. May be {@code null}.
     */
    SortOrder getSortOrder();

    /**
     * @return {@code true} if a sorting option has been set using {@link #setSortingOptions(SortOn, SortOrder)}
     * @see #setSortingOptions(SortOn, SortOrder)
     */
    boolean isSorted();

    /**
     * Clear all the constraints previously set.  An existing grouping IS NOT cleared.
     */
    void clear();

    /**
     * Set the maximum number of messages that will be returned by {@link IQueryService#query(IQueryConstraints)}.
     *
     * @param maxMessages Maximum message count, pass a value &lt;= 0 for no limit
     */
    void setMaxMessageCount(int maxMessages);

    /**
     * Get the maximum number of messages that will be returned by {@link IQueryService#query(IQueryConstraints)}.
     *
     * @return Maximum message count
     */
    int getMaxMessageCount();

    /**
     * Set the start message count that will be returned by {@link IQueryService#query(IQueryConstraints)}.
     *
     * @param startMessage Starting message count, default = 0
     */
    void setStartMessageCount(int startMessage);

    /**
     * Get the starting message count that will be returned by {@link IQueryService#query(IQueryConstraints)}.
     *
     * @return Starting message count
     */
    int getStartMessageCount();

    /**
     * @return Core field constraints contained within this query constraints, empty list if none specified
     */
    Collection<List<IConstraint>> getCoreFieldConstraints();

    /**
     * @return Extended (structure) field constraints contained within this query constraints, empty list if none specified
     */
    Collection<List<IConstraint>> getExtendedFieldConstraints();

    /**
     * @return Shape constraints contained within this query constraints, empty list if none specified
     */
    List<IConstraint> getShapeConstraints();

    /**
     * @return Logic constraints contained within this query constraints, empty list if none specified
     */
    List<IConstraint> getLogicConstraints();

    /**
     * @return Group constraints contained within this query constraints, empty list if none specified
     */
    List<IConstraint> getGroupConstraints();

    /**
     * @return The map object proxies that have been added as constraints, empty list if none specified
     * @since 3.0
     */
    Set<IMapObject> getMapObjects();

    /**
     * @return An unmodifiable map of all names-&gt;structures used in the query
     * @since 2.20
     */
    Map<String, Set<IStructure>> getMessageStructureMap();
}
