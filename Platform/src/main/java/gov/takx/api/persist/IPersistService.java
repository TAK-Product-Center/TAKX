package gov.takx.api.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IDeletedMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IExtendedDataStructure;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorMessage;
import gov.takx.api.messages.IStructure;
import gov.takx.api.query.param.HQLParameter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * Contract for management of persistent entities.  Each thread is provided its own persist service instance
 * and concurrency issues are managed by database transactions.
 * <p>
 * All access to, and use of, a persist service instance is managed by the core, and no direct access
 * is provided to plugins.
 *
 */
@Internal
@NoPluginImplementations
public interface IPersistService
{
    /**
     * @return {@code true} if the current database supports insertion of arrays of native types
     * @since 5.4
     */
    boolean databaseSupportsNativeArrays();

    /**
     * @return {@code true} If the current database provider supports bulk RDM insertion operations
     * @since 2.14
     */
    boolean databaseSupportsBulkInsertion();

    /**
     * Perform a bulk insert of RDMs core data (as appropriate for the given database).
     *
     * @param rdms The collection of RDMs to insert
     * @return number of messages inserted, -1 on error
     * @since 2.14
     */
    long bulkInsertRDMs(@Nonnull Iterable<IPrePersistRaptorDataMessage> rdms);

    /**
     * Perform a bulk insert of RDMs extended data (as appropriate for the given database).
     *
     * @param structure The structure defining the data to be persisted
     * @param rdms      The collection of RDMs to insert
     * @return number of messages inserted, -1 on error
     * @since 2.14
     */
    long bulkInsertRDMsExtendedData(@Nonnull IStructure structure, @Nonnull Iterable<IPrePersistRaptorDataMessage> rdms);

    /**
     * Persist an RDM.  You must use this method, and not {@link #persistEntity(IPersistable)}, to persist RDMs
     * as they are not mapped entities handled by hibernate.
     *
     * @param msg Message to process
     * @since 2.14
     */
    void persistRDM(@Nonnull IPrePersistRaptorDataMessage msg);

    /**
     * Persist the given entity into the database. If not successful, an exception will be thrown.
     * Note that this operation will be performed inside a transaction.
     *
     * @param entity Entity to persist
     * @since 3.0 (renamed from persistMappedObject)
     */
    void persistEntity(@Nonnull IPersistable entity);

    /**
     * Delete the given entity from the database. If the object has cascading delete dependencies, then these objects
     * will also be deleted. Note that this operation will be performed inside a transaction.
     *
     * @param entity Entity to delete
     * @since 3.0 (renamed from deleteMappedObject)
     */
    void deleteEntity(@Nonnull IPersistable entity);

    /**
     * Delete a collection of Map Objects.  Use this method to delete Map Objects, and not
     * {@link #deleteEntity}.
     * <p>
     * This is not a full deletion, just the preparatory work to mark them as deleted, and create the
     * DeletedMapObject records.  The remainder of the deletion (of all data associated with the Map Object),
     * happens in the background.
     *
     * @param mapObjects List of map objects to delete
     * @return List of DeletedMapObjects created for the given Map Objects
     * @see IBackgroundDeleteManager
     * @see IDeletedMapObject
     */
    List<IDeletedMapObject> batchDeleteMapObjects(@Nonnull List<? extends IMapObject> mapObjects);

    /**
     * Persist a collection of entities within a single transaction.
     *
     * @param entities List of entities to persist
     * @since 3.0 (renamed from batchPersistMappedObjectList)
     */
    void batchPersistEntityList(@Nonnull List<? extends IPersistable> entities);

    /**
     * When a thread is done using the persistence services, it should call this method
     * to clean up and close the underlying resources.
     */
    void cleanupAndClose();

    /**
     * @param tableName Name of table to test for existence
     * @return {@code true} if the table exists, {@code false} if not
     * @since 2.37
     */
    boolean tableExists(@Nonnull String tableName);

    /**
     * Map a given structure, creating the DB table and dynamic class to back it.
     *
     * @param struct Structure to map
     * @since 2.37 (just added to interface, already was in concrete implementation)
     */
    void mapStructure(@Nonnull IStructure struct);

    /**
     * Force update the given entity into persistence.  Update the DB with the content of the given detached
     * entity without loading it or performing "changed value" checks.
     * <p>
     * The given entity must have a non-null Id, and must not be a Hibernate proxy.
     *
     * @param entity Entity to merge
     * @throws IllegalArgumentException if the entity is a Hibernate proxy
     * @since 3.0
     */
    void forceUpdate(@Nonnull IForceUpdatable entity);

    /**
     * Bulk load the extended data stored in the native tables into the data object for the given list of message.
     *
     * @param rdms The list of RDMs to process
     */
    void bulkLoadExtendedData(@Nonnull List<IRaptorDataMessage> rdms);

    /**
     * Load the extended data stored in the native tables into the data object for the given message
     *
     * @param rxm The Raptor Data Message
     * @return the extended data
     */
    IExtendedDataStructure loadExtendedData(@Nonnull IRaptorMessage rxm) throws SQLException;

    /**
     * @return The DB connection manager
     * @since 2.37 (just added to interface, already was in concrete implementation)
     */
    IDatabaseConnectionManager getConnectionMgr();

    /**
     * @return The DB provider
     * @since 3.0
     */
    IDatabaseProvider getDatabaseProvider();

    /**
     * Execute a Hibernate query with the supplied parameters.
     *
     * @param hql    The HQL query to execute.
     * @param params HQL parameters.
     * @return A list of the Objects returned by the query.
     */
    <T> List<T> query(@Nonnull String hql, HQLParameter... params);

    /**
     * Execute a Hibernate query with the supplied parameters, with a result set narrowed by first and max rows.
     *
     * @param hql      The HQL query to execute.
     * @param maxRows  The max number of result rows to return.
     * @param firstRow The first row in the results to return.
     * @param params   HQL parameters.
     * @return A list of the Objects returned by the query.
     */
    <T> List<T> query(@Nonnull String hql, Integer maxRows, Integer firstRow, HQLParameter... params);

    /**
     * Execute a SQL query with the supplied parameters.  This form allows the query to reference unmapped tables.
     *
     * @param hql    The SQL query to execute.
     * @param params HQL parameters.
     * @return A list of the Objects returned by the query.
     */
    <T> List<T> querySQL(@Nonnull String hql, HQLParameter... params);

    /**
     * Count the number of rows in the given table.
     *
     * @param tableName Table to query
     * @return Number of rows in the table, may be 0
     * @since 3.0
     */
    default long count(@Nonnull String tableName)
    {
        return count(tableName, null);
    }

    /**
     * Count the number of rows matching the given where clause and parameter bindings.
     *
     * @param tableName Table to query
     * @param where     Where clause with optional parameter bindings, may be {@code null} or empty
     * @param params    HQL parameters
     * @return Number of matching rows, may be 0
     * @since 3.0
     */
    long count(@Nonnull String tableName, @Nullable String where, HQLParameter... params);

    /**
     * Count the number of rows matching the given where clause and parameter bindings.
     * This form uses a SQL query allowing it to refer to unmapped tables.
     *
     * @param tableName Table to query
     * @param where     Where clause with optional parameter bindings, may be {@code null} or empty
     * @param params    HQL parameters
     * @return Number of matching rows, may be 0
     * @since 3.0
     */
    long countSQL(@Nonnull String tableName, @Nullable String where, HQLParameter... params);

    /**
     * Execute a Hibernate query with the supplied parameters.
     *
     * @param hql    The HQL query to execute.
     * @param params HQL parameters.
     * @return The Object returned by the query.
     */
    <T> T queryUnique(@Nonnull String hql, HQLParameter... params);

    /**
     * Execute a SQL query with the supplied named parameters that expects a unique result.
     *
     * @param sql    The SQL query to execute.
     * @param params HQL parameters.
     * @return The Object returned by the query.
     */
    <T> T queryUniqueSQL(@Nonnull String sql, HQLParameter... params);

    /**
     * Execute a query against RDMs, supporting a subset of the hibernate hql syntax.
     *
     * @param hql    query to execute
     * @param params Parameter bindings
     * @return A list of the Objects returned by the query.
     * @since 2.14
     */
    <T> List<T> queryFromRdm(@Nonnull String hql, HQLParameter... params);

    /**
     * Execute a query against RDMs, supporting a subset of the hibernate hql syntax.
     *
     * @param hql     query to execute
     * @param maxRows The max number of result rows to return.
     * @param params  Parameter bindings
     * @return A list of the Objects returned by the query.
     * @since 2.14
     */
    <T> List<T> queryFromRdm(@Nonnull String hql, Integer maxRows, HQLParameter... params);

    /**
     * Execute a SQL query against RDMs.  The query is not parsed or transformed, only parameter bindings are
     * processed (supporting the ":name" format).
     *
     * @param sql        SQL query to execute
     * @param maxRows    The max number of result rows to return
     * @param asMessages Pass {@code true} if this SQL results should be reified as RDMs, {@code false} for arbitrary selects
     * @param params     Parameter bindings
     * @return A list of the Objects returned by the query.
     * @since 2.14
     */
    <T> List<T> sqlQueryFromRdm(@Nonnull String sql, Integer maxRows, boolean asMessages, HQLParameter... params);

    /**
     * Execute a query for a unique result against RDMs, supporting a subset of the hibernate hql syntax.
     *
     * @param hql    query to execute
     * @param params Parameter bindings
     * @return The Object returned by the query.
     * @since 2.14
     */
    <T> T queryUniqueFromRdm(@Nonnull String hql, HQLParameter... params);

    /**
     * Run an iterator on a selection of RDMs.  This method is specific to the query service, and not for general use.
     *
     * @param sql     Query to execute
     * @param maxRows The maximum rows wanted in the result set
     * @param params  query parameters
     * @return iterator over query results
     * @since 3.0
     */
    Iterator<IRaptorDataMessage> rdmIterator(@Nonnull String sql, Integer maxRows, HQLParameter... params);

    /**
     * Close an iterator previously obtained with {@link #rdmIterator(String, Integer, HQLParameter...)}
     *
     * @param iterator Iterator to close
     * @since 3.0
     */
    void closeRdmIterator(@Nonnull Iterator<IRaptorDataMessage> iterator);

    /**
     * Execute a SQL update statement natively, bypassing the hibernate session.
     * Use with caution, you'd better be updating something you know doesn't affect the hibernate session objects.
     *
     * @param sql    SQL to execute, no parameters accepted
     * @param params query parameters
     * @return number of rows affected
     */
    int executeNativeUpdateSQL(@Nonnull final String sql, HQLParameter... params);

    /**
     * Allows direct access to the {@link Session#doWork(Work)} method, supporting low-level SQL operations.
     *
     * @param work The work to be performed.
     */
    void doWork(@Nonnull Work work);

    /**
     * Allows direct access to the {@link Session#doReturningWork(ReturningWork)} method,
     * supporting low-level SQL operations.
     *
     * @param work The work to be performed.
     * @param <T>  The type of the result returned from the work
     * @return the result from calling {@link ReturningWork#execute}.
     */
    <T> T doReturningWork(@Nonnull ReturningWork<T> work) throws HibernateException;
}
