package gov.takx.dataservices.persist;

import gov.takx.api.messages.IRaptorDataMessage;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is responsible for managing database primary key ids for the {@link IRaptorDataMessage}s. Note that this is
 * accomplished by storing a static variable for the id. This means that the id needs to be reset between projects
 * or anything else that would rely upon getting a unique or independent set of ids.
 *
 * @since 1.46
 */
public final class RdmIdGenerator
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int INITIAL_VALUE = 0;
    private static final String TABLE_NAME = "MapPoint";

    private final AtomicInteger nextID = new AtomicInteger(INITIAL_VALUE);

    /**
     * Construct an Id generator.  This is merely a simple wrapper on top of the 'nextId' counter, which must be
     * initialized on a per-project basis.
     *
     * @see #initialize(Session)
     */
    public RdmIdGenerator()
    {
    }

    /**
     * Increment the Id and return it.
     *
     * @return next Id in sequence
     * @since 2.18
     */
    public int generateId()
    {
        return nextID.incrementAndGet();
    }

    /**
     * Initialize the nextID sequence from the max ID value in the mappoint table.
     *
     * @param session The session to use to perform the SQL operations
     * @since 2.18
     */
    public void initialize(Session session)
    {
        final int maxId = session.doReturningWork(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT max(id) FROM " + TABLE_NAME);
                 ResultSet rs = ps.executeQuery())
            {
                return rs.next() ? rs.getInt(1) : INITIAL_VALUE;
            } catch (SQLException e)
            {
                logger.error("Error initializing nextId", e);
                throw new HibernateException("Unable to initialize nextId from " + TABLE_NAME + " table");
            }
        });

        logger.debug("Initializing {} nextID to: {}", TABLE_NAME, maxId);

        nextID.set(maxId);
    }

    /**
     * Reset to INITIAL_VALUE.  This should be called between projects (especially in tests).
     */
    public void clear()
    {
        nextID.set(INITIAL_VALUE);
    }
}
