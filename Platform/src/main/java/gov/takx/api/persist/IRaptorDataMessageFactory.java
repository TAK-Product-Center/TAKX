package gov.takx.api.persist;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.IStructure;
import gov.takx.dataservices.persist.RaptorDataMessage;
import jakarta.annotation.Nonnull;

import java.util.function.BiFunction;

/**
 * Contract for the service that handles registration of custom RDM implementations.
 * <p>
 * Plugins can register a custom RDM implementation on a per-structure basis.  The factory method
 * will then be used to construct an RDM any time it is being pulled from persistence.  This occurs
 * when the query service results are being reified and when a project is re-opened and the RDMs are
 * being reloaded for rendering purposes.
 *
 * @since 2.29
 */
@InjectableService
@NoPluginImplementations
public interface IRaptorDataMessageFactory
{
    /**
     * Register a custom RDM factory for the given structure.  The factory function must accept an {@link IStructure}
     * and {@link IMapObject} and produce an implementation of {@link IPrePersistRaptorDataMessage}.
     *
     * @param structure           The Raptor Data Structure for which the custom factory should be used
     * @param factory             The factory (function) to use to construct the custom RDM implementation
     * @since 2.36
     */
    @SuppressWarnings("unused")
    // called from plugins
    void registerStructure(@Nonnull IRaptorDataStructure structure,
                           @Nonnull BiFunction<IStructure, IMapObject, IPrePersistRaptorDataMessage> factory);

    /**
     * Create an RDM instance according to the structure.  If any custom factory has been registered for the given
     * structure, then it will be used to create the RDM.  If not, then a standard {@link RaptorDataMessage} is returned.
     *
     * @param structure Structure for which an RDM is to be created
     * @param mapObject Map Object to assign to the RDM
     * @return RDM instance
     */
    IPrePersistRaptorDataMessage createRaptorDataMessage(@Nonnull IStructure structure, @Nonnull IMapObject mapObject);
}
