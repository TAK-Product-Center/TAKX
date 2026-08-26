package gov.takx.api.persist;

import gov.takx.dataservices.persist.Connection;

import java.util.List;

/**
 * Persistence management for the active connections established by the user within a TAKX Project.
 *
 */
public interface IConnectionPersist
{
    /**
     * Save a connection.
     *
     * @param connection connection to save
     */
    void persistConnection(Connection connection);

    /**
     * Query for all the Connection objects in the system.
     *
     * @return list of Connection objects
     */
    List<Connection> queryAll();

    /**
     * Delete the specified connection from the database.
     *
     * @param connection Connection to delete
     */
    void deleteConnection(Connection connection);
}
