package gov.takx.api.query;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.messages.IRaptorDataMessage;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This interface defines a Query Service available to developers of plugins, apps, and connections.
 * The query service offers a simple mechanism to query for Data Messages (as are produced by plugin
 * parsers) using a variety of constraints (like time and lat/long boundaries).
 * <p/>
 * The specifics of a query are controlled by an {@link IQueryConstraints} object.  This object holds the details
 * of the query such as the specific lat/long boundaries within which to query for messages.
 * <p/>
 * When the service processes an {@link IQueryConstraints}, it can:
 * <ol>
 * <li>query for the count of matching messages in the DB that meet the criteria</li>
 * <li>query for the actual list of matching messages (or selected fields from them)</li>
 * <li>determine if an RDM in memory matches (would be selected by) the constraints</li>
 * </ol>
 * Another common use case is to retrieve the last N messages according to some sort criteria.  To
 * achieve this end, use the sort ordering on the constraints
 * (see {@link IQueryConstraints#setSortingOptions(IQueryConstraints.SortOn,
 * IQueryConstraints.SortOrder)}) and set a limit on the number of results returned using
 * {@link IQueryConstraints#setMaxMessageCount(int)}.  For example, to
 * retrieve the last 5 messages received you would do something like this:
 * <pre>
 * IQueryService qs = delegate.getQueryService();
 * IQueryConstraints qc = qs.createEmptyConstraints();
 * qc.setSortingOptions(IQueryConstraints.SortOn.RECEIVE_DATE, IQueryConstraints.SortOrder.DESCENDING);
 * qs.setMaxMessageCount(5);
 * List<IRaptorDataMessage> rdms = qs.query(qc);
 * </pre>
 * The only trick is to set the sort order to DESCENDING so that the first N messages are the most recent.
 *
 */
@NoPluginImplementations
public interface IQueryService
{
    /**
     * Create a new, empty {@link IQueryConstraints} object.  Note that an empty constraints object
     * matches all messages in the query domain for caller.  For example, a plugin's query domain
     * contains only those messages created by that particular instance of the plugin.  Other consumers
     * of this service may have different query domains that are broader or narrower.
     *
     * @return Newly created object
     */
    IQueryConstraints createEmptyConstraints();

    /**
     * Determine if a given message is matched by the query constraints.  If the message was in persistence,
     * the query constraints would select that message if used in a call to {@link #query(IQueryConstraints)}.
     *
     * @param rdm         Message to test
     * @param constraints Constraints to use
     * @return {@code true} if the message matches the constraints
     */
    boolean matches(IRaptorDataMessage rdm, IQueryConstraints constraints);

    /**
     * Return the number of messages that match the specified constraints.
     *
     * @param constraints Query constraints to apply
     * @return number of matching messages
     * @throws QueryExecutionException If any errors occur during execution of the query
     */
    long count(IQueryConstraints constraints) throws QueryExecutionException;

    /**
     * Return the messages that match the specified constraints.  By default, the results of a query
     * are limited to returning no more than {@link IQueryConstraints#DEFAULT_MAX_MESSAGES}
     * messages.  This can be overridden by calling {@link IQueryConstraints#setMaxMessageCount(int)}.
     *
     * @param constraints Query constraints to apply
     * @return List of matching messages, may be empty
     * @throws QueryExecutionException If any errors occur during execution of the query
     */
    List<IRaptorDataMessage> query(IQueryConstraints constraints) throws QueryExecutionException;

    /**
     * Return the messages that match the specified constraints.  By default, the results of a query
     * are limited to returning no more than {@link IQueryConstraints#DEFAULT_MAX_MESSAGES}
     * messages.  This can be overridden by calling {@link IQueryConstraints#setMaxMessageCount(int)}.
     *
     * @param constraints Query constraints to apply
     * @param prefetch    Pass {@code true} to cause the extended data to be pre-fetched as part of the query operation
     * @return List of matching messages, may be empty
     * @throws QueryExecutionException If any errors occur during execution of the query
     */
    List<IRaptorDataMessage> query(IQueryConstraints constraints, boolean prefetch) throws QueryExecutionException;

    /**
     * Execute the query and return an Iterator over the messages that match the specified constraints.
     * <p/>
     * It is important to close iterators if you don't traverse them to exhaustion.
     * Use {@link #closeIterator(java.util.Iterator)} to properly close all iterators obtained from this method.
     *
     * @param constraints Query constraints to apply
     * @return Iterator over matching messages, may be empty
     * @throws QueryExecutionException If any errors occur during execution of the query
     */
    Iterator<IRaptorDataMessage> iterator(IQueryConstraints constraints) throws QueryExecutionException;

    /**
     * Return the field values specified in the given field selectors for the RDMs that match the query constraints.
     * This query form is dramatically faster than selecting entire RDMs just so you can pull out a couple of fields.
     * Each matching RDMs values is returned as an object array.
     *
     * @param constraints   Query constraints to apply
     * @param fieldSelector The selector indicating the fields to retrieve
     * @return List of Object[] containing the fields for each matching RDM in the same order as you specified them
     * in the field selector, core fields first, followed by extended fields.
     * @throws QueryExecutionException        If any errors occur during execution of the query
     * @throws InvalidFieldSelectorException  If the field selector is invalid
     * @throws ConflictingConstraintException If the field selector and field constraints are on different structures
     * @see FieldSelector
     */
    List<Object[]> queryForFields(IQueryConstraints constraints, FieldSelector fieldSelector)
            throws QueryExecutionException, InvalidFieldSelectorException, ConflictingConstraintException;

    /**
     * Return the field values specified in the given field selectors for the RDMs that match the query constraints.
     * This query form is dramatically faster than selecting entire RDMs just so you can pull out a couple of fields.
     * Each matching RDMs values is returned as a Map of field alias name to field value.
     * <p/>
     * As an example:
     * <pre>
     * FieldSelector fs = new FieldSelector()
     *     .addCoreFields(CoreField.heading, CoreField.id)
     *     .setStructure("BH_STATUS_MESSAGE").addExtendedFields("key", "Voltage");
     *
     * List&lt;Map&lt;String, Object&gt;&gt; results = qs.queryForFieldsAsMap(qc, fs);
     *
     * for (Map&lt;String, Object&gt; valueMap : results)
     * {
     *     Integer id = (Integer) valueMap.get("id");
     *     Double heading = (Double) valueMap.get("heading");
     *     String key = (String) valueMap.get("key");
     *     Double voltage = (Double) valueMap.get("Voltage");
     * }
     * </pre>
     *
     * @param constraints   Query constraints to apply
     * @param fieldSelector The selector indicating the fields to retrieve
     * @return List of results containing the fields for each matching RDM.  Each entry in the list corresponds
     * to one result row (values from a single matching RDM) in the form of a Map.  The keys of the map are
     * the field names selected via the field selectors.  For core fields, the key is the same as the core
     * field constant.  For extended fields, the key is the name of the field as defined in the structure.
     * @throws QueryExecutionException        If any errors occur during execution of the query
     * @throws InvalidFieldSelectorException  If the field selector is invalid
     * @throws ConflictingConstraintException If the field selector and field constraints are on different structures
     * @see FieldSelector
     * @since 1.25
     */
    List<Map<String, Object>> queryForFieldsAsMap(IQueryConstraints constraints, FieldSelector fieldSelector)
            throws QueryExecutionException, InvalidFieldSelectorException, ConflictingConstraintException;

    /**
     * Return the field values specified in the given field selectors for the RDMs that match the query constraints.
     * This query form is dramatically faster than selecting entire RDMs just so you can pull out a couple of fields.
     * <p/>
     * Each matching RDMs values is returned as an Object instance whose class is specified in {@code dtoType}.
     * The class must have fields or setter methods that match the selected query fields.  For example, if you include
     * {@link CoreField#heading} in the {@link FieldSelector}, then the receiving object must have a field named
     * {@code heading} or a setter method {@code setHeading(Double)} to accept the result value.
     * <p/>
     * As an example:
     * <pre>
     * public static class DtoResult
     * {
     *     Integer id;
     *     Double heading;
     *     String key;
     *     Double Voltage; // Note capitalization, it has to match the query field name exactly
     * }
     *
     * FieldSelector fs = new FieldSelector()
     *     .addCoreFields(CoreField.heading, CoreField.id)
     *     .setStructure("BH_STATUS_MESSAGE").addExtendedFields("key", "Voltage");
     *
     * //noinspection unchecked
     * List&lt;DtoResult&gt; results = qs.queryForFieldsAsBean(qc, fs, DtoResult.class);
     *
     * for (DtoResult dto : results)
     * {
     *     // Do something with DTO
     * }
     * </pre>
     * <p/>
     * The DTO object can provide setter methods instead of fields, the names of the setter methods must follow the
     * java bean standards.  For example:
     * <pre>
     * public static class DtoResult
     * {
     *     void setId(Integer id) { ... }
     *     void setHeading(Double heading) { ... };
     *     void setKey(String key) { ... };
     *     void setVoltage(Double voltage) { ... }
     * }
     *
     * FieldSelector fs = new FieldSelector()
     *     .addCoreFields(CoreField.heading, CoreField.id)
     *     .setStructure("BH_STATUS_MESSAGE").addExtendedFields("key", "Voltage");
     *
     * //noinspection unchecked
     * List&lt;DtoResult&gt; results = qs.queryForFieldsAsBean(qc, fs, DtoResult.class);
     *
     * for (DtoResult dto : results)
     * {
     *     // Do something with DTO
     * }
     * </pre>
     *
     * @param constraints   Query constraints to apply
     * @param fieldSelector The selector indicating the fields to retrieve
     * @param dtoType       The class object for the type of DTO to instantiate to hold the results for a single
     *                      matching RDM. The fields or setters in the DTO type must match the field names selected
     *                      via the field selectors.  For core fields, the key is the same as the core field constant.
     *                      For extended fields, the key is the name of the field as defined in the structure.
     * @return List of results containing the fields for each matching RDM.  Each entry in the list corresponds
     * to one result row (values from a single matching RDM) in the form of an instance of the {@code dtoType}
     * specified populated with data from that result row.
     * @throws QueryExecutionException        If any errors occur during execution of the query
     * @throws InvalidFieldSelectorException  If the field selector is invalid
     * @throws ConflictingConstraintException If the field selector and field constraints are on different structures
     * @see FieldSelector
     * @since 1.25
     */
    <T> List<T> queryForFieldsAsBean(IQueryConstraints constraints, FieldSelector fieldSelector, Class<T> dtoType)
            throws QueryExecutionException, InvalidFieldSelectorException, ConflictingConstraintException;

    /**
     * Close an iterator previously obtained from {@link #iterator(IQueryConstraints)}.
     *
     * @param iterator The iterator to close
     */
    void closeIterator(Iterator<IRaptorDataMessage> iterator);
}
