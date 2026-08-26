package gov.takx.core.persist;

import com.google.common.annotations.VisibleForTesting;
import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.mapobject.IDeletedMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IExtendedDataStructure;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorMessage;
import gov.takx.api.messages.IStructure;
import gov.takx.api.persist.IDatabaseConnectionManager;
import gov.takx.api.persist.IDatabaseProvider;
import gov.takx.api.persist.IDatabaseServer;
import gov.takx.api.persist.IForceUpdatable;
import gov.takx.api.persist.IPersistService;
import gov.takx.api.persist.IPersistable;
import gov.takx.api.persist.IRdmBatchQueue;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.preferences.IProjectPreferenceManager;
import gov.takx.api.project.IProject;
import gov.takx.api.project.IProjectManager;
import gov.takx.api.query.param.HQLParameter;
import gov.takx.api.timer.ITimerService;
import gov.takx.api.ui.IUserProxy;
import gov.takx.commons.utils.PlatformVersion;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.database.DatabaseManager;
import gov.takx.core.database.exception.FatalDatabaseException;
import gov.takx.core.database.exception.LoadCancelledException;
import gov.takx.dataservices.persist.DatabaseUpgradeManager;
import gov.takx.dataservices.persist.DatabaseVersionManager;
import gov.takx.dataservices.persist.DatabaseVersionManager.DatabaseState;
import gov.takx.dataservices.persist.DeletedMapObject;
import gov.takx.dataservices.persist.MapEntity;
import gov.takx.dataservices.persist.Structure;
import gov.takx.dataservices.persist.StructureAssociation;
import gov.takx.dataservices.persist.upgraders.UpgraderException;
import gov.takx.dataservices.upgrade.UpgradeStatement;
import gov.takx.dataservices.utils.DynamicStructureDBUtils;
import gov.takx.platform.cdi.InjectableContainerSupport;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.ProjectPreferenceEntry;
import gov.takx.platform.utils.DatabaseUtils;
import gov.takx.platform.utils.TimeBoundedTask;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Transaction;
import org.hibernate.dialect.Dialect;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.query.CommonQueryContract;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.ref.Cleaner;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimerTask;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Persistence services.  The basic theory of operation is that every thread will have a unique
 * instance of this class.  That allows them to, as much as possible, avoid blocking each other
 * on persistence operations.
 */
public class PersistService implements IPersistService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Cleaner CLEANER = Cleaner.create();
    private static final InjectableContainerSupport BEAN_ACCESSOR = new InjectableContainerSupport();

    private static final Map<Thread, PersistService> thread2serviceMap = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Session, Thread> openSessions = new ConcurrentHashMap<>();

    private static IDatabaseProvider databaseProvider;

    // The connection manager can be shared, so just track it statically and clear it in shutdown().
    private static IDatabaseConnectionManager connectionMgr;

    // How long to wait before cleaning up stale sessions
    private static final long STALE_CLOSE_DELAY = TimeUnit.MINUTES.toMillis(2);

    // How long to wait between stale session checks
    private static final long SESSION_CLEANER_FREQUENCY = TimeUnit.SECONDS.toMillis(30);

    private static TimerTask staleSessionCleaner;

    // In order to minimize the overhead of creating PS instances, we avoid injecting some values that never change
    // while we're executing.  We initialize these values just once.
    private static boolean staticsCached;
    private static boolean doRDMBatch;
    private static boolean platformUnderTest;
    private static IRdmBatchQueue rdmBatchQueue;
    private static RDMQueryProcessor rdmQueryProcessor;
    private static IProjectManager projectManager;
    private static MessagePersistHelper messagePersistHelper;
    private static MapObjectPersist mapObjectPersist;
    private static IProjectPreferenceManager projectPreferenceManager;
    private static DynamicStructureDBUtils dynamicStructureDBUtils;
    private static DatabaseVersionManager databaseVersionManager;
    private static DatabaseUpgradeManager databaseUpgradeManager;

    // Task to clean up dead thread entries in our service map.
    private static final TimeBoundedTask CLEANUP_DEAD_THREADS = new TimeBoundedTask("Cleanup Dead Threads",
            TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(3),
            PersistService::cleanupDeadThreads);

    private final CleanableState cleanableState;
    private final Cleaner.Cleanable cleanable;

    private final Thread myThread;

    // Track last usage so we can clean up stale sessions.  This is an attempt to prevent "Idle In Transaction"
    // problems in Postgres when an arbitrary thread (usually coming from a thread pool, so it never dies) uses
    // a PersistService to perform a query, but never calls cleanupAndClose.  Instead of trying to find all such
    // places (which might even be in plugin code), we'll just reap any sessions that haven't been used for a
    // given period of time.
    private long lastUse = System.currentTimeMillis();

    private volatile boolean inTransaction;
    private volatile boolean inQuery; // To detect long-running queries

    /**
     * Status of upgrade function.
     */
    private enum UpgradeStatus
    {
        CannotBeUpgraded,
        NoNeedToUpgrade,
        Upgraded,
        UpgradeRejected
    }

    /**
     * Get or construct a PersistService instance for the current thread.
     *
     * @return PersistService for the current thread
     * @since 2.25
     */
    public static PersistService forCurrentThread()
    {
        return thread2serviceMap.computeIfAbsent(Thread.currentThread(), k -> new PersistService());
    }

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    private PersistService()
    {
        // Track all our incarnations so we can validate that no one is doing any illegal cross-thread usage
        myThread = Thread.currentThread();
        cleanableState = new CleanableState(toString());

        logger.debug("Creating PersistService: {}", this);

        final PersistService oldPS = thread2serviceMap.get(myThread);
        if (oldPS != null) logger.warn("got old PS: {} open={}", oldPS, oldPS.isSessionOpen(), new RuntimeException());

        if (!staticsCached)
        {
            doRDMBatch = PreferenceEntry.RDM_BATCHING.getBooleanValue();
            platformUnderTest = BEAN_ACCESSOR.getBean(IProgramEnvironment.class).isPlatformUnderTest();
            rdmBatchQueue = BEAN_ACCESSOR.getBean(IRdmBatchQueue.class);
            rdmQueryProcessor = BEAN_ACCESSOR.getBean(RDMQueryProcessor.class);
            projectManager = BEAN_ACCESSOR.getBean(IProjectManager.class);
            messagePersistHelper = BEAN_ACCESSOR.getBean(MessagePersistHelper.class);
            mapObjectPersist = BEAN_ACCESSOR.getBean(MapObjectPersist.class);
            projectPreferenceManager = BEAN_ACCESSOR.getBean(IProjectPreferenceManager.class);
            dynamicStructureDBUtils = BEAN_ACCESSOR.getBean(DynamicStructureDBUtils.class);
            databaseVersionManager = BEAN_ACCESSOR.getBean(DatabaseVersionManager.class);
            databaseUpgradeManager = BEAN_ACCESSOR.getBean(DatabaseUpgradeManager.class);

            staticsCached = true;
        }

        // Register for GC cleanup
        cleanable = CLEANER.register(this, cleanableState::cleanupAndClose);

        CLEANUP_DEAD_THREADS.request(); // kick off a cleanup of any services hanging around for dead threads
    }

    @Override
    public String toString()
    {
        return "PersistService@" + hexId(this) + " for thread tid=" + myThread.getId() + "/" + myThread;
    }

    /**
     * Shutdown the persistence subsystem.
     *
     * @since 3.0
     */
    static void shutdown()
    {
        logger.info("clearing database session state");

        CLEANUP_DEAD_THREADS.shutdown(); // Avoid a task running after we've shutdown

        if (staleSessionCleaner != null) staleSessionCleaner.cancel();

        // Close and clean out all PersistService instances

        synchronized (thread2serviceMap)
        {
            thread2serviceMap.values().forEach(PersistService::closeSession);
            thread2serviceMap.clear();
        }

        openSessions.clear();

        if (dynamicStructureDBUtils != null) dynamicStructureDBUtils.clear();
        if (messagePersistHelper != null) messagePersistHelper.clearIdGenerators();

        connectionMgr = null;
    }

    /**
     * Scan the service map and cleanup any instances associated with dead threads.
     */
    static void cleanupDeadThreads()
    {
        logger.trace("Cleaning up dead threads.");

        synchronized (thread2serviceMap)
        {
            new ArrayList<>(thread2serviceMap.values()).stream()
                    .filter(ps -> !ps.myThread.isAlive())
                    .forEach(PersistService::cleanupAndClose);
        }
    }

    /**
     * Scan our instances and close any that have not been used within the configured limit.
     */
    private static void cleanupStaleSessions()
    {
        if (connectionMgr == null) return; // Don't do this on external connections

        final long now = System.currentTimeMillis();

        // For a service instance to be reaped, it must:
        //    1. have an open session
        //    2. have no open iterators
        //    3. not be in a transaction
        //    4. not be executing a query (which is possibly long-running)
        //    5. have not been used within STALE_CLOSE_DELAY

        // Use a copy of the values to avoid ConcurrentModificationExceptions
        new ArrayList<>(thread2serviceMap.values()).stream()
                .filter(ps -> ps.isSessionOpen()
                        && ps.cleanableState.openIterators.isEmpty()
                        && !ps.inTransaction
                        && !ps.inQuery
                        && (now - ps.lastUse) > STALE_CLOSE_DELAY)
                .forEach(ps -> {
                    logger.debug("closing stale session: {}", ps);
                    ps.cleanupAndClose();
                });
    }

    /**
     * Cleanup and close the session for the given thread, if there is a PS recorded for it.
     *
     * @param t The thread to clean up
     */
    public static void cleanupForThread(Thread t)
    {
        final PersistService ps = thread2serviceMap.remove(t);
        if (ps != null) ps.cleanupAndClose();
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType") // Only used for tests
    protected static Map<Thread, PersistService> getThread2ServiceMap()
    {
        return thread2serviceMap;
    }

    @Override
    public boolean databaseSupportsNativeArrays()
    {
        return getConnectionMgr().supportsNativeArrays();
    }

    @Override
    public boolean databaseSupportsBulkInsertion()
    {
        return getConnectionMgr().supportsBulkInsertRDMs();
    }

    @Override
    public long bulkInsertRDMs(@Nonnull Iterable<IPrePersistRaptorDataMessage> rdms)
    {
        return getConnectionMgr().bulkInsertRDMs(rdms);
    }

    @Override
    public long bulkInsertRDMsExtendedData(@Nonnull IStructure structure, @Nonnull Iterable<IPrePersistRaptorDataMessage> rdms)
    {
        return getConnectionMgr().bulkInsertRDMsExtendedData(structure, rdms);
    }

    @Override
    public IExtendedDataStructure loadExtendedData(@Nonnull final IRaptorMessage rxm)
    {
        return dynamicStructureDBUtils.loadExtendedData(getCurrentSession(), rxm);
    }

    @Override
    public void bulkLoadExtendedData(@Nonnull List<IRaptorDataMessage> rdms)
    {
        try
        {
            dynamicStructureDBUtils.bulkLoadExtendedData(getCurrentSession(), rdms);
        } catch (Exception e)
        {
            logger.error("Failed to load extended data", e);
        }
    }

    /**
     * Should be private, but is also called from tests.
     *
     * @return open session
     */
    @VisibleForTesting
    Session getCurrentSession()
    {
        if (!isSessionOpen())
        {
            try
            {
                final Thread thread = Thread.currentThread();

                testValidThreadUsage(thread);

                final Session currentSession = getConnectionMgr().openSession();
                currentSession.setHibernateFlushMode(FlushMode.MANUAL);
                cleanableState.currentSession = currentSession;

                if (logger.isDebugEnabled()) dumpOpenSessions("Opening", currentSession, toString());

                openSessions.put(currentSession, thread);
            } catch (Throwable e)
            {
                // rethrow this so upper layers can properly handle/report it
                logger.error("Failed to open a new session", e);
                throw e;
            }
        }

        lastUse = System.currentTimeMillis(); // And bump our last use time

        return cleanableState.currentSession;
    }

    /**
     * Validate that the thread/service mapping is correct.
     *
     * @since 3.0
     */
    private void testValidThreadUsage(Thread thread)
    {
        final PersistService psForThread = thread2serviceMap.get(thread);

        if (psForThread == null)
        {
            failThreadCheck("Illegal PersistService instance not in Map", thread);
        } else if (psForThread != this)
        {
            failThreadCheck("Illegal PersistService shared instance", thread);
        }
    }

    private void failThreadCheck(String cause, Thread thread)
    {
        final String message = cause + " on thread: " + thread.getId() + "/" + thread + " on " + this;
        logger.error(message, new RuntimeException());
        throw new AssertionError(message);
    }

    /**
     * Dump the open sessions to the output log.
     *
     * @param operation The operation being performed on "currentSession" (Opened or Closed)
     */
    private static void dumpOpenSessions(String operation, Session currentSession, String serviceName)
    {
        final StringBuilder builder = new StringBuilder(1024);

        builder.append(operation)
                .append(" session (").append(hexId(currentSession)).append(")")
                .append(" in ").append(serviceName);

        if (!openSessions.isEmpty())
        {
            builder.append("\n   Other open sessions:");
            openSessions.forEach((s, t) -> builder.append("\n      ").append(hexId(s))
                    .append(": tid=").append(t.getId()).append("/").append(t));
        } else
        {
            builder.append("\n   No other open sessions");
        }

        logger.debug(builder.toString());
    }

    /**
     * Should be private, but called from tests.
     */
    @VisibleForTesting
    boolean isSessionOpen()
    {
        return cleanableState.currentSession != null && cleanableState.currentSession.isOpen();
    }

    @Override
    public void cleanupAndClose()
    {
        if (thread2serviceMap.get(myThread) == this) thread2serviceMap.remove(myThread);

        cleanable.clean();

        inTransaction = false;
        inQuery = false;
    }

    /**
     * Close the session associated with this PersistService.
     */
    private void closeSession()
    {
        cleanableState.closeSession();

        inTransaction = false;
        inQuery = false;
    }

    @Override
    public void deleteEntity(@Nonnull IPersistable entity)
    {
        if (entity instanceof IMapObject)
        {
            throw new IllegalArgumentException("Can not delete MapObjects here; use batchDeleteMapObjects");
        }

        if (logger.isDebugEnabled()) logger.debug("Deleting Entity: {}", makeNameAndId(entity));

        // If the object is currently in another session then we need to evict it. Otherwise, we
        // might get an exception on trying to re-persist an object with the same identifier.
        evictFromSessions(entity, false);

        doInTransaction(false, sess -> {
            try
            {
                // The object we've been asked to delete may have come from a different session, so we
                // need to see if we have this same object in our session.  If we do, then we delete that
                // object in order to avoid a potential NonUniqueObjectException.
                // Avoid get on transient objects.
                if (entity.getId() != null)
                {
                    final Object currentObject = sess.getReference(entity.getClass(), entity.getId());
                    if (currentObject != null)
                    {
                        sess.remove(currentObject);
                    }
                } else
                {
                    sess.remove(entity); // remove transient object
                }
            } catch (StaleObjectStateException e)
            {
                // eat these exceptions, someone else deleted this object
            }
        });
    }

    @Override
    public List<IDeletedMapObject> batchDeleteMapObjects(@Nonnull List<? extends IMapObject> mapObjects)
    {
        final List<IDeletedMapObject> dmoList = new ArrayList<>(mapObjects.size());

        doInTransaction(false, session -> {
            // MOs can get pulled into the session by various operations, and the instance being passed in to be
            // deleted may not be the one in the session.  So, clear the session to avoid exceptions.
            session.clear();

            for (IMapObject mapObject : mapObjects)
            {
                if (logger.isDebugEnabled()) logger.debug("Deleting Map Object: {}", makeNameAndId(mapObject));

                try
                {
                    evictFromSessions(mapObject, true);

                    // Construct the DeletedMapObject record
                    final DeletedMapObject dmo = new DeletedMapObject(mapObject);
                    session.persist(dmo);
                    dmoList.add(dmo);

                    // Let hibernate handle the deletion, so it invokes the lifecycle event method
                    session.remove(mapObject);

                    mapObjectPersist.removeFromCache(mapObject);
                } catch (StaleObjectStateException e)
                {
                    logger.error("Failed to delete: {}", mapObject, e);
                }
            }
        });

        return dmoList;
    }

    @Override
    public void batchPersistEntityList(@Nonnull List<? extends IPersistable> entities)
    {
        final long start = System.currentTimeMillis();

        doInTransaction(false, session -> entities.forEach(session::persist));

        if (logger.isDebugEnabled())
        {
            logger.debug("Total {}: {} ms", entities.size(), System.currentTimeMillis() - start);
        }
    }

    @Override
    public void persistEntity(@Nonnull IPersistable entity)
    {
        if (entity instanceof IPrePersistRaptorDataMessage)
        {
            throw new IllegalArgumentException("Don't use persistEntity() on RDMs, use persistRDM()");
        } else if (entity instanceof IMapObject mapObject && mapObject.isTransient())
        {
            throw new IllegalArgumentException("Attempt to persist transient MapObject: [" + makeNameAndId(mapObject) + "]: " + entity);
        } else
        {
            if (logger.isDebugEnabled()) logger.debug("persisting  [{}]: {}", makeNameAndId(entity), entity);

            final boolean idWasNull = entity.getId() == null;

            doInTransaction(false, session -> {
                //noinspection deprecation
                session.saveOrUpdate(entity);

                if (logger.isDebugEnabled()) logger.debug("   assigned ID = {}", entity.getId());

                updateCache(entity, idWasNull);
            });
        }
    }

    /**
     * Update our entity cache, if this is one of our cached types: Structure, MapEntity, and MapObject.
     *
     * @param entity    Entity to add to cache
     * @param idWasNull Pass {@code true} if the Id was {@code null} previously (i.e., a new entity)
     * @since 3.0
     */
    private void updateCache(@Nonnull IPersistable entity, boolean idWasNull)
    {
        if (entity instanceof Structure structure)
        {
            if (idWasNull)
            {
                mapObjectPersist.addToCache(structure);
            } else
            {
                logger.error("Re-persist Structure: {}", structure, new RuntimeException());
            }
        } else if (entity instanceof MapEntity mapEntity)
        {
            if (idWasNull)
            {
                mapObjectPersist.addToCache(mapEntity);
            } else
            {
                logger.error("Re-persist ME: {}", mapEntity, new RuntimeException());
            }
        } else if (entity instanceof IMapObject mo)
        {
            mapObjectPersist.addToCache(mo);
        }
    }

    @Override
    public void persistRDM(@Nonnull IPrePersistRaptorDataMessage rdm)
    {
        final boolean unsavedMessage = !rdm.hasId();

        // Regardless of batching or message type, get an Id assigned if this is a new message
        if (unsavedMessage)
        {
            // In the case that the message being persisted has never had the data object created, which can happen
            // for messages that have no structure fields (like manual location messages), we need to ensure that the
            // data object is created prior to assigning the message an Id.  If we don't do this, then later, when the
            // data object is created on demand, since we have an Id at that time, it will try to load the data from
            // persistence, which is a waste of time since this is a new message.

            rdm.getDataObject();

            // Now generate the Id
            rdm.setId(messagePersistHelper.generateRdmId());
        }

        if (doRDMBatch)
        {
            // In batch mode, add it to the batch and return
            // Note the batch operation will handle the extended data as well
            rdmBatchQueue.addToQueue(rdm, unsavedMessage);
        } else
        {
            try
            {
                doWork(connection -> messagePersistHelper.persistRDM(connection, rdm, unsavedMessage));
            } catch (Exception e)
            {
                closeSession();
                logger.error("Failed to persist message: {}", rdm, e);
            }
        }
    }

    @Override
    public void forceUpdate(@Nonnull IForceUpdatable entity)
    {
        Objects.requireNonNull(entity.getId(), "'entity' must have a non-null Id");

        if (DatabaseUtils.isProxy(entity)) throw new IllegalArgumentException("'entity' must not be a DB proxy");

        if (logger.isDebugEnabled()) logger.debug("force update: {} : {}", makeNameAndId(entity), entity);

        evictFromSessions(entity, true);

        // Use a stateless session to force update the entity to avoid having it loaded back in from persistence, which
        // would fire the lifecycle events, and we don't want to have those happen.
        getConnectionMgr().withNewStatelessSession(statelessSession -> {
            entity.preForceUpdate();
            statelessSession.update(entity);
        }, e -> {
            throw new RuntimeException("Failed StatelessSession operation", e);
        });
    }

    /**
     * Perform the given operation within a transaction.
     * Unless {@code leaveSessionAlone} is {@code true} then, at the end of the operation, the session will be cleared.
     *
     * @param leaveSessionAlone Pass {@code true} to prevent any flush/clear operations on the session
     * @param operation         Operation to perform within the transaction
     * @since 3.0
     */
    private void doInTransaction(boolean leaveSessionAlone, Consumer<Session> operation)
    {
        doReturningInTransaction(leaveSessionAlone, s -> {
            operation.accept(s);
            return null;
        });
    }

    /**
     * Perform the given operation within a transaction and return the operation's result.
     * Unless {@code leaveSessionAlone} is {@code true} then, at the end of the operation, the session will be cleared.
     *
     * @param leaveSessionAlone Pass {@code true} to prevent any flush/clear operations on the session
     * @param operation         Operation to perform within the transaction
     * @param <T>               Inferred return type
     * @return result of the {@code operation} function
     * @since 3.0
     */
    private <T> T doReturningInTransaction(boolean leaveSessionAlone, Function<Session, T> operation)
    {
        final Session session = getCurrentSession();
        final boolean nested = inTransaction;

        Transaction tx = null;

        try
        {
            if (!nested)
            {
                tx = session.beginTransaction();
                inTransaction = true;
            }

            final T result = operation.apply(session);

            if (!nested)
            {
                try
                {
                    if (!leaveSessionAlone) session.flush();
                    tx.commit();

                    // In analyzing the prior code paths, they all led to the clearing of the session,
                    // except if an iterator was open.  That handling was a hold-over to the days when RDM iterators
                    // (and RDMs themselves) where handled by hibernate.  So, I've reduced all that logic to simply
                    // clearing the session.  If that proves to be a problem, we can investigate a more correct
                    // solution at that time.

                    if (!leaveSessionAlone) session.clear();
                } catch (Exception e)
                {
                    logger.error("Flush/Commit failed.", e);
                    throw e;
                } finally
                {
                    inTransaction = false;
                }
            }

            return result;
        } catch (Exception e)
        {
            logger.error("Rolling back transaction", e);
            try
            {
                if (tx != null) tx.rollback();
            } finally
            {
                closeSession();
            }
            throw e;
        }
    }

    /**
     * Evict the given object from existing sessions.
     *
     * @param entity Entity to evict
     * @param all    Pass {@code true} to evict from ALL session, {@code false} for just "other" sessions
     * @since 3.0
     */
    private void evictFromSessions(IPersistable entity, boolean all)
    {
        synchronized (thread2serviceMap)
        {
            thread2serviceMap.values().stream()
                    .filter(ps -> (all || ps != this) && ps.isSessionOpen())
                    .forEach(ps -> {
                        // It is still possible for this session to have been closed between the test above and here
                        try
                        {
                            ps.evict(entity);
                        } catch (IllegalStateException e)
                        {
                            logger.debug("Ignoring IllegalStateException during evict of {}", entity);
                        }
                    });
        }
    }

    /**
     * Evict a given object from our session.
     *
     * @param entity Entity to evict
     * @since 2.28
     */
    private void evict(IPersistable entity)
    {
        if (isSessionOpen())
        {
            cleanableState.currentSession.evict(entity);

            if (logger.isTraceEnabled()) logger.trace("evict {} from {}", makeNameAndId(entity), hexId(cleanableState.currentSession));
        }
    }

    @Override
    public <T> List<T> query(@Nonnull String hql, HQLParameter... params)
    {
        return queryInternal(hql, false, null, null, params);
    }

    @Override
    public <T> List<T> query(@Nonnull String hql, Integer maxRows, Integer firstRow, HQLParameter... params)
    {
        return queryInternal(hql, false, maxRows, firstRow, params);
    }

    @Override
    public <T> List<T> querySQL(@Nonnull String hql, HQLParameter... params)
    {
        return queryInternal(hql, true, null, null, params);
    }

    /**
     * Internal query handler to process either HQL or SQL queries.
     *
     * @param <T>      Inferred list element type
     * @param hqlOrSql Query string to process, may be either HQL or SQL
     * @param isSql    {@code true} if the query string is SQL, {@code false} if it is HQL
     * @param maxRows  The max number of result rows to return
     * @param firstRow The first row in the results to return
     * @param params   HQL parameters
     * @return A list of the {@code T} returned by the query.
     * @since 3.0
     */
    private <T> List<T> queryInternal(String hqlOrSql, boolean isSql, Integer maxRows, Integer firstRow, HQLParameter... params)
    {
        final boolean traceEnabled = logger.isTraceEnabled();
        if (traceEnabled) logger.trace("PS.query >>>:instance={}\n   {}", this, hqlOrSql);

        maxRows = maxRows == null || maxRows <= 0 ? -1 : maxRows;

        try
        {
            inQuery = true;

            final Session sess = getCurrentSession();
            final Query<?> query = prepareQuery(sess, hqlOrSql, isSql, maxRows, firstRow, params);
            final List<?> result = safeDoReturningWork(sess, query::list);

            if (traceEnabled) logger.trace("  Size: {}", result.size());

            //noinspection unchecked
            return (List<T>) result;
        } catch (RuntimeException e)
        {
            logger.error("Error doing query", e);
            closeSession();
            throw e;
        } finally
        {
            inQuery = false;
            if (traceEnabled) logger.trace("PS.query <<<:instance={}", this);
        }
    }

    @Override
    public <T> T queryUnique(@Nonnull String hql, HQLParameter... params)
    {
        return queryUniqueInternal(hql, false, params);
    }

    @Override
    public <T> T queryUniqueSQL(@Nonnull String sql, HQLParameter... params)
    {
        return queryUniqueInternal(sql, true, params);
    }

    /**
     * Common handling for querying for a unique result.
     *
     * @param hqlOrSql Query string to process, may be either HQL or SQL
     * @param isSql    {@code true} if the query string is SQL, {@code false} if it is HQL
     * @param params   HQL parameters
     * @return Object, result of query
     * @since 3.0
     */
    private <T> T queryUniqueInternal(String hqlOrSql, boolean isSql, HQLParameter... params)
    {
        inQuery = true;

        try
        {
            final Session session = getCurrentSession();

            final Query<?> query = prepareQuery(session, hqlOrSql, isSql, null, null, params);
            final Object result = safeDoReturningWork(session, query::uniqueResult);

            if (logger.isTraceEnabled()) logger.trace("  Result: {}", result);

            //noinspection unchecked
            return (T) result;
        } catch (ObjectNotFoundException e)
        {
            // Quietly catch and rethrow this exception since it is a valid result
            logger.trace("Object not found in queryUnique");
            throw e;
        } catch (RuntimeException e)
        {
            logger.error("Error during queryUnique", e);
            throw e;
        } finally
        {
            inQuery = false;
        }
    }

    @Override
    public int executeNativeUpdateSQL(@Nonnull String sql, HQLParameter... params)
    {
        logger.trace("SQL: {}", sql);

        //noinspection SqlSourceToSinkFlow
        final MutationQuery query = getCurrentSession().createNativeMutationQuery(sql);
        bindQueryParameters(query, params);

        return doReturningInTransaction(true, s -> query.executeUpdate());
    }

    @Override
    public <T> T doReturningWork(@Nonnull ReturningWork<T> work) throws HibernateException
    {
        return getCurrentSession().doReturningWork(work);
    }

    @Override
    public void doWork(@Nonnull Work work)
    {
        getCurrentSession().doWork(work);
    }

    /**
     * Internal method to perform a {@link Session#doReturningWork(ReturningWork)} operation and ensure that the
     * connection is properly committed on success or rolled back on any exception.
     *
     * @param sess      Session to use
     * @param operation Operation to perform (supplies the result)
     * @param <T>       Inferred result type
     * @return Result from {@code operation}
     * @since 2.37
     */
    @SuppressWarnings("unchecked")
    private <T> T safeDoReturningWork(Session sess, Supplier<T> operation)
    {
        return (T) sess.doReturningWork(connection -> {
            try
            {
                final Object result = operation.get();
                connection.commit();
                return result;
            } catch (Exception e)
            {
                connection.rollback();
                throw e;
            }
        });
    }

    @Override
    public long count(@Nonnull String tableName, @Nullable String where, HQLParameter... params)
    {
        return countInternal(tableName, where, false, params);
    }

    @Override
    public long countSQL(@Nonnull String tableName, @Nullable String where, HQLParameter... params)
    {
        return countInternal(tableName, where, true, params);
    }

    /**
     * Common handling for count operations on either HQL or SQL.
     *
     * @param isSql Pass {@code true} to perform a SQL query, {@code false} for HQL
     * @see #count(String, String, HQLParameter...)
     * @see #countSQL(String, String, HQLParameter...)
     * @since 3.0
     */
    private long countInternal(String tableName, String where, boolean isSql, HQLParameter... params)
    {
        try
        {
            where = where == null ? "" : where.trim();

            if (!where.isEmpty() && !StringUtils.startsWithIgnoreCase(where, "where"))
            {
                where = "where " + where; // The caller didn't include the "where" keyword, so add it
            }

            final String query = String.format("select count(*) from %s %s", tableName, where);

            return ((Number) queryUniqueInternal(query, isSql, params)).longValue();
        } catch (HibernateException e)
        {
            logger.error("Error during count", e);
            throw e;
        }
    }

    @Override
    public <T> List<T> queryFromRdm(@Nonnull String hql, HQLParameter... params)
    {
        return queryFromRdm(hql, null, params);
    }

    @Override
    public <T> List<T> queryFromRdm(@Nonnull String hql, Integer maxRows, HQLParameter... params)
    {
        return rdmQueryProcessor.query(getCurrentSession(), hql, maxRows, params);
    }

    @Override
    public <T> List<T> sqlQueryFromRdm(@Nonnull String sql, Integer maxRows, boolean asMessages, HQLParameter... params)
    {
        return rdmQueryProcessor.querySQL(getCurrentSession(), sql, maxRows, asMessages, params);
    }

    @Override
    public <T> T queryUniqueFromRdm(@Nonnull String hql, HQLParameter... params)
    {
        return rdmQueryProcessor.queryUnique(getCurrentSession(), hql, params);
    }

    @Override
    public Iterator<IRaptorDataMessage> rdmIterator(@Nonnull String sql, Integer maxRows, HQLParameter... params)
    {
        logger.trace("rdmIterator: {}", sql);

        maxRows = maxRows == null || maxRows <= 0 ? null : maxRows;

        try
        {
            final Iterator<IRaptorDataMessage> iterator = rdmQueryProcessor.iteratorSQL(getConnectionMgr().getConnection(), sql, maxRows, params);
            cleanableState.openIterators.add(iterator);
            return iterator;
        } catch (SQLException e)
        {
            logger.error("Failed to get iterator", e);
            throw new HibernateException("Failed to get iterator", e);
        }
    }

    @Override
    public void closeRdmIterator(@Nonnull Iterator<IRaptorDataMessage> iterator)
    {
        closeIterator(iterator);

        cleanableState.openIterators.remove(iterator);

        // The session will still be open if there were open iterators when the last transaction completed.
        // Check to see if it's now safe to shut this session down.
        if (cleanableState.openIterators.isEmpty())
        {
            logger.debug("Closing session now that all iterators are closed.");
            cleanupAndClose();
        }
    }

    /**
     * Close an iterator, ensuring that it is AutoCloseable.
     *
     * @param iterator Iterator to close
     * @since 3.0
     */
    private static void closeIterator(Iterator<IRaptorDataMessage> iterator)
    {
        if (iterator instanceof AutoCloseable autoCloseable)
        {
            try
            {
                autoCloseable.close();
            } catch (Exception e)
            {
                logger.error("Failed to close iterator: {}", iterator, e);
            }
        } else
        {
            logger.error("Don't know how to close iterable of type: {}", iterator.getClass());
        }
    }

    /**
     * Given the query parameters, construct a properly initialized Query object.
     *
     * @param sess     Session from which to construct the query
     * @param hqlOrSql query string
     * @param maxRows  If not {@code null}, max number of rows to return
     * @param firstRow If not {@code null}, first row of results to return
     * @param params   query parameters
     * @return properly initialized query object
     */
    private Query<?> prepareQuery(Session sess, String hqlOrSql, boolean isSql, Integer maxRows, Integer firstRow, HQLParameter... params)
    {
        logger.trace("prepareQuery: {}", hqlOrSql);

        // Create the appropriate type of Query
        final Query<?> query = isSql
                ? sess.createNativeQuery(hqlOrSql, Object.class)
                : sess.createQuery(hqlOrSql, Object.class);

        if (maxRows != null && maxRows > 0) query.setMaxResults(maxRows);
        if (firstRow != null) query.setFirstResult(firstRow);

        bindQueryParameters(query, params);

        return query;
    }

    /**
     * Bind the given query parameters into the query, handling both scalar and collection types.
     *
     * @param query  Query to update
     * @param params Parameters to bind
     * @since 3.0
     */
    private void bindQueryParameters(CommonQueryContract query, HQLParameter[] params)
    {
        if (logger.isTraceEnabled()) logger.trace("Bind:\n   {}", StringUtils.join(params, "\n   ", null));

        for (HQLParameter p : params)
        {
            if (p != null && p.value != null)
            {
                if (p.value instanceof final Collection<?> collection)
                {
                    if (!collection.isEmpty()) query.setParameterList(p.name, collection);
                } else
                {
                    query.setParameter(p.name, p.value);
                }
            }
        }
    }

    /**
     * Initialize the overall persistence subsystem.
     *
     * @param progressMonitor Progress monitor to update
     * @since 3.0
     */
    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    void initialize(IProgressMonitor progressMonitor)
    {
        long ts = System.currentTimeMillis();

        try
        {
            final IProject proj = projectManager.getCurrentOpenProject();

            if (proj.isFromPackage())
            {
                handlePackageRestore(progressMonitor, proj);
            }

            if (proj.isNew())
            {
                handleNewProject(progressMonitor);
            } else
            {
                handleExistingProject(progressMonitor);
            }
        } catch (LoadCancelledException e)
        {
            throw e;
        } catch (Exception e)
        {
            logger.error("Error loading initial state", e);
            throw new FatalDatabaseException("Could not open project DB", e);
        }

        messagePersistHelper.initializeIdGenerators(getCurrentSession()); // Initialize the RDM Id generator

        // Start the stale session cleaner task
        staleSessionCleaner = new StaleSessionCleanerTask();

        BEAN_ACCESSOR.getBean(ITimerService.class).scheduleAtFixedRate(staleSessionCleaner, SESSION_CLEANER_FREQUENCY, SESSION_CLEANER_FREQUENCY);

        if (logger.isDebugEnabled())
        {
            logger.debug("Total time to load PersistService: {} ms", System.currentTimeMillis() - ts);

            final long rmCount = countSQL("MapPoint", null);
            final long moCount = countSQL("MapObject", null);

            logger.debug("Total MapObject count: {}, Total RDM count: {}", moCount, rmCount);
        }
    }

    /**
     * Handle a project with an existing database.  Check for and perform an upgrade if needed.
     * Then load our initial data: Structure associations, Structures and Map Entities.
     */
    private void handleExistingProject(IProgressMonitor progressMonitor)
    {
        handleProjectUpgrade(progressMonitor);

        progressMonitor.setText("Loading project data...");

        // Now load in our initial state to get it cached and configured
        mapObjectPersist.doInitialLoad();
    }

    /**
     * Handle the initialization of a new project's database.
     *
     * @since 3.0
     */
    private void handleNewProject(IProgressMonitor progressMonitor) throws SQLException
    {
        logger.info("Initializing a new project.");
        progressMonitor.setText("Initializing a new project.");

        // Write the current DB Plugin settings into the projx file
        ProjectPreferenceEntry.DATABASE_TYPE.put(getDatabaseProvider().getType());

        // Create the DB version table and initialize to the current version
        try (Connection connection = getConnectionMgr().getConnection())
        {
            databaseVersionManager.insertCurrentVersion(connection);
        }

        // Create our non-hibernate managed tables
        createTableIfNeeded("MapPoint", messagePersistHelper::buildRDMCreateStatements);
        createTableIfNeeded("StructureAssociation", StructureAssociation::buildTableDefinition);
    }

    /**
     * Restore the contents of the DB tar file extracted from a package.
     */
    private void handlePackageRestore(IProgressMonitor progressMonitor, IProject proj)
    {
        progressMonitor.setText("Restoring DB contents from package");
        logger.info("Restoring DB contents from package");

        try
        {
            getDatabaseProvider().getCoreServer().restoreData();
        } catch (Exception e)
        {
            // If that failed, all we can do is create a new database
            progressMonitor.setText("Creating new database...");
            proj.setNew(true);
        }
    }

    /**
     * Perform a database upgrade, if one is required.  If the user rejects the upgrade, then throw an exception
     * to let the caller know that the database can't be opened (was canceled).
     * <p>
     * If an upgrade was performed, then update the project preferences with the current version values.
     *
     * @throws LoadCancelledException If the user rejected the upgrade
     */
    private void handleProjectUpgrade(IProgressMonitor progressMonitor)
    {
        final UpgradeStatus status = performUpgradeIfNeeded(progressMonitor);

        switch (status)
        {
            case NoNeedToUpgrade -> logger.debug("DB at current version");
            case Upgraded ->
            {
                progressMonitor.setText("Database upgrade completed...");
                closeSession();

                // The DB is now irreversibly the latest version, so update the preferences file to reflect that.
                ProjectPreferenceEntry.VERSION.put(PlatformVersion.CURRENT_VERSION_STRING);
                ProjectPreferenceEntry.DATABASE_VERSION.put(DatabaseVersionManager.CURRENT_VERSION_STRING);
                projectPreferenceManager.savePreferences();
            }
            case UpgradeRejected, CannotBeUpgraded ->
            {
                // DB can't be upgraded, or the user did not approve project upgrade. Stop the load by throwing a LoadCancelledException.
                if (status == UpgradeStatus.CannotBeUpgraded)
                {
                    BEAN_ACCESSOR.getBean(IUserProxy.class).notify("Database Version Unsupported",
                            "This project's database version is too old and cannot be opened.");
                }

                shutdown();
                getDatabaseProvider().shutdown();
                throw new LoadCancelledException();
            }
        }
    }

    /**
     * Create a table if it does not exist.
     *
     * @param tableName       Name of table to test/create
     * @param tableDefinition Supplier of the statements to use to create the table
     * @since 2.29
     */
    private synchronized void createTableIfNeeded(String tableName, Supplier<List<UpgradeStatement>> tableDefinition)
    {
        if (!tableExists(tableName))
        {
            try
            {
                final List<UpgradeStatement> createStatements = tableDefinition.get();

                logger.debug("Create {}", tableName);

                if (logger.isTraceEnabled())
                {
                    logger.trace(createStatements.stream().map(Object::toString).collect(Collectors.joining(",")));
                }

                getConnectionMgr().performUpgradeStatements(createStatements);
            } catch (Exception e)
            {
                logger.error("Failed to create {} table exists", tableName, e);
            }
        }
    }

    @Override
    public boolean tableExists(@Nonnull String tableName)
    {
        try (final Connection connection = getConnectionMgr().getConnection())
        {
            // Only create the table if it doesn't exist
            return DatabaseUtils.tableExists(connection, tableName);
        } catch (SQLException e)
        {
            logger.error("Failed to test if table {} exists", tableName, e);
        }
        return false;
    }

    /**
     * Determine if the current project DB needs to be upgraded, and if so, perform the upgrade.
     *
     * @param progressMonitor Progress monitor
     * @return Status of the upgrade.
     */
    private UpgradeStatus performUpgradeIfNeeded(IProgressMonitor progressMonitor)
    {
        UpgradeStatus status = UpgradeStatus.NoNeedToUpgrade;

        try
        {
            IDatabaseConnectionManager connectionMgr = getConnectionMgr();

            try (Connection conn = connectionMgr.getConnection())
            {
                final DatabaseState state = databaseVersionManager.getState(conn);

                if (state.needsUpgrade())
                {
                    if (state.canUpgrade())
                    {
                        logger.info("Project database (version {}) needs to be upgraded to {}", state.version(), DatabaseVersionManager.CURRENT_VERSION);

                        final File workingDir = new File(projectManager.getProjectDirectory());
                        final Dialect dialect = connectionMgr.getDialect();

                        if (databaseUpgradeManager.doUpgrade(conn, dialect, state, workingDir, progressMonitor))
                        {
                            logger.debug("Database was upgraded.");
                            status = UpgradeStatus.Upgraded;
                        } else
                        {
                            logger.debug("Database needs upgrade, but user did not approve.");
                            status = UpgradeStatus.UpgradeRejected;
                        }
                    } else
                    {
                        // No upgrade support for a DB this old, so bail
                        logger.warn("Database version {} is too old to upgrade.", state.version());
                        status = UpgradeStatus.CannotBeUpgraded;
                    }
                }
            } catch (SQLException e)
            {
                throw new UpgraderException(e);
            }
        } catch (RuntimeException e)
        {
            closeSession();
            throw e;
        }

        return status;
    }

    @VisibleForTesting
    public static void setDatabaseProvider(IDatabaseProvider provider)
    {
        // We need to allow changing the DB provide during tests.
        if (databaseProvider != null && databaseProvider != provider && !platformUnderTest)
        {
            throw new IllegalArgumentException("Changing the database provider is not allowed");
        } else
        {
            databaseProvider = provider;
        }
    }

    @Override
    public IDatabaseProvider getDatabaseProvider()
    {
        if (databaseProvider == null)
        {
            // If we have a project preference (from an existing project) specifying the DB type, then use that.
            // Otherwise, use the default in the program properties.
            final String projectProviderType = ProjectPreferenceEntry.DATABASE_TYPE.getStringValue();
            final String providerType = !StringUtils.isEmpty(projectProviderType)
                    ? projectProviderType
                    : PreferenceEntry.DATABASE_TYPE.getStringValue();

            setDatabaseProvider(BEAN_ACCESSOR.getBean(DatabaseManager.class).getDatabaseProvider(providerType));
        }
        return databaseProvider;
    }

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    @Override
    public IDatabaseConnectionManager getConnectionMgr()
    {
        if (connectionMgr != null) return connectionMgr;

        final IDatabaseServer server = getDatabaseProvider().getCoreServer();

        connectionMgr = server.getConnectionManager();

        return connectionMgr;
    }

    @Override
    public void mapStructure(@Nonnull IStructure struct)
    {
        createTableIfNeeded(struct.getExtendedDataClassName(), () -> dynamicStructureDBUtils.buildCreateStatement(struct));
    }

    /**
     * For debugging purposes, compose a string with the simple name of the object's class and its persistable ID.
     *
     * @param o Object to dump
     * @return string for debugging
     */
    private String makeNameAndId(IPersistable o)
    {
        return o.getClass().getSimpleName() + ", id=" + o.getId();
    }

    /**
     * @return Hex encoded value of the given objects identity hash code
     * @since 3.0
     */
    private static String hexId(Object o)
    {
        return "0x" + Integer.toHexString(System.identityHashCode(o));
    }

    /**
     * Task to periodically cleanup stale sessions.
     *
     * @since 2.37
     */
    private static class StaleSessionCleanerTask extends TimerTask
    {
        @Override
        public void run()
        {
            cleanupStaleSessions();
        }
    }

    /**
     * Holder for state that needs to be cleaned up when a PersistService instance is no longer reachable.
     * @since 5.0
     */
    private static final class CleanableState
    {
        private final Set<Iterator<IRaptorDataMessage>> openIterators = new HashSet<>();
        private final String serviceName;

        private volatile Session currentSession;

        CleanableState(String serviceName)
        {
            this.serviceName = serviceName;
        }

        /**
         * Close the session associated with our PersistService.
         */
        private void closeSession()
        {
            if (currentSession != null)
            {
                if (currentSession.isOpen() && currentSession.isConnected()) currentSession.close();

                connectionMgr.closeSession(currentSession);

                openSessions.remove(currentSession);

                if (logger.isDebugEnabled()) dumpOpenSessions("Closed", currentSession, serviceName);

                currentSession = null;
            }
        }

        /**
         * Close our session and any open iterators.
         */
        void cleanupAndClose()
        {
            try
            {
                // Forcibly close all open iterators on this instance.
                if (!openIterators.isEmpty()) logger.warn("Force closing iterators on {}", serviceName);

                openIterators.forEach(PersistService::closeIterator);
                openIterators.clear();

                if (currentSession != null && currentSession.isOpen())
                {
                    currentSession.clear();
                    closeSession();
                }
            } catch (Exception e)
            {
                logger.error("Error closing {}", serviceName, e);
            }
        }
    }
}
