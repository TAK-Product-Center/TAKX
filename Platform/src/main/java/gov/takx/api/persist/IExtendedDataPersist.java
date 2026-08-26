package gov.takx.api.persist;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.messages.IExtendedDataStructure;
import gov.takx.api.messages.IRaptorMessage;

import java.sql.SQLException;

/**
 * Contract for loading an extended data object (structure data).  This is in support of allowing plugins
 * to generate custom RDM implementations without exposing the entire persist service interface.
 *
 * @since 2.29
 */
@InjectableService
@NoPluginImplementations
public interface IExtendedDataPersist
{
    /**
     * Generate the next RDM Id to use.  This method must be used to properly allocate an RDM Id that does
     * not clash with other RDMs created by the system.  This is especially critical when custom RDM
     * implementations are not persisted, yet need to function like ordinary RDMs.
     *
     * @return Generated RDM Id
     */
    int generateRdmId();

    /**
     * Load the extended data stored in the native tables into the data object for the given message.
     *
     * @param rxm The Raptor Property/Data Message
     * @return the extended data
     */
    IExtendedDataStructure loadExtendedData(IRaptorMessage rxm) throws SQLException;
}
