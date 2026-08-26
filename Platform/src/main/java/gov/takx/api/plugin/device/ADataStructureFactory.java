package gov.takx.api.plugin.device;

import gov.takx.api.messages.IDataField;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.NoSuchStructureException;
import gov.takx.api.query.CoreField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract Data Structure Factory.  Plugin developers are encouraged to extend this base class
 * and rely on its protected methods to manage their data structure lists.
 *
 */
public abstract class ADataStructureFactory implements IDataStructureFactory
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Wrapper class that stores both the {@link IRaptorDataStructure} and the metadata about the structure. This class
     * is used internally as the value of the Map.
     */
    protected static class StructureWrapper
    {
        private final IRaptorDataStructure structure;
        private final boolean dataMessage;
        private final boolean persistable;
        private final boolean includeInDeviceLogList;

        StructureWrapper(IRaptorDataStructure structure, boolean isDataMessage, boolean isPersistable, boolean includeInDeviceLogList)
        {
            this.structure = structure;
            dataMessage = isDataMessage;
            persistable = isPersistable;
            this.includeInDeviceLogList = includeInDeviceLogList;
        }

        public IRaptorDataStructure getDataStructure()
        {
            return structure;
        }

        boolean isDataMessage()
        {
            return dataMessage;
        }

        public boolean isPersistable()
        {
            return persistable;
        }

        boolean isIncludedInDeviceLog()
        {
            return includeInDeviceLogList;
        }
    }

    protected Map<String, StructureWrapper> structureMap = new HashMap<>();

    /**
     * Defines a new {@link IRaptorDataMessage} data structure that is marked as persistable and included in the
     * Device Log structure listing.
     *
     * @param structureName Plugin developer defined arbitrary name of the data structure instance
     * @param structure     Data structure instance to associate with the given name
     */
    protected void defineDataStructure(String structureName, IRaptorDataStructure structure)
    {
        defineDataStructure(structureName, structure, true);
    }

    /**
     * Defines a new {@link IRaptorDataStructure} that is categorized as an RDM or RCM structure based on the
     * {@code isDataMessage} flag. If {@code isDataMessage} is {@code true}, then the structure is defined as
     * persistable and is included in the Device Log list. If {@code isDataMessage} is {@code false}, then the structure
     * is defined as not persistable and is not included in the Device Log list.
     *
     * @param structureName Plugin developer defined arbitrary name of the IRaptorDataStructure instance.
     * @param structure     Data structure instance to associate with the given name.
     * @param isDataMessage Flag that indicates if this is an {@link IRaptorDataMessage}. {@code true} indicates an RDM
     *                      structure, {@code false} an RCM structure
     */
    protected void defineDataStructure(String structureName, IRaptorDataStructure structure, boolean isDataMessage)
    {
        // If dataMessage is true, then we default to a persistable definition.

        // If dataMessage is false, then we default to a non-persistable definition.

        defineDataStructure(structureName, structure, isDataMessage, isDataMessage);
    }

    /**
     * Defines a new {@link IRaptorDataStructure} that is categorized as an RDM or RCM structure based on the
     * {@code isDataMessage} flag, and marked as persistable based on the {@code persistable} flag. The combination of
     * {@code isDataMessage = false} and {@code persistable = true} will result in an IllegalStateException. If
     * {@code persistable} is true, then the structure is included in the Device Log list.
     *
     * @param structureName Plugin developer defined arbitrary name of the IRaptorDataStructure instance.
     * @param structure     IRaptorDataStructure instance to associate with the given name.
     * @param isDataMessage Flag that indicates if this is an IRaptorDataMessage.  True indicates an RDM structure,
     *                      false an RCM structure
     * @param isPersistable Flag that indicates if the data structure is marked as persistable.
     */
    protected void defineDataStructure(String structureName, IRaptorDataStructure structure, boolean isDataMessage, boolean isPersistable)
    {
        // If persistable is true, then this must be an RDM definition.  By default, include it in the Device Log

        // If persistable is false, then this is either not persistable and won't be found in the DB, or it is an RCM
        // so exclude it from the device log list.

        defineDataStructure(structureName, structure, isDataMessage, isPersistable, isPersistable);
    }

    /**
     * Defines a new {@link IRaptorDataStructure} that is categorized as an RDM or RCM structure based on the
     * {@code isDataMessage} flag, and marked as persistable based on the {@code persistable} flag. The combination of
     * {@code isDataMessage = false} and {@code persistable = true} will result in an IllegalStateException.
     *
     * @param structureName          Plugin developer defined arbitrary name of the IRaptorDataStructure instance.
     * @param structure              IRaptorDataStructure instance to associate with the given name.
     * @param isDataMessage          Flag that indicates if this is an IRaptorDataMessage.  True indicates an RDM structure,
     *                               false an RCM structure
     * @param isPersistable          Flag that indicates if the data structure is marked as persistable.
     * @param includeInDeviceLogList Flag that indicates if the data structure should be included in the Device Log list
     */
    protected void defineDataStructure(String structureName, IRaptorDataStructure structure, boolean isDataMessage, boolean isPersistable, boolean includeInDeviceLogList)
    {
        if (!isDataMessage && isPersistable)
        {
            throw new IllegalStateException("IRaptorCommandMessage structures must not be marked as persistable");
        }

        // If the message is persistable, we need to ensure there are no user-defined fields with the same name as core fields
        if (isPersistable)
        {
            String[] fieldNames = structure.getFields().stream().map(IDataField::getFieldName).toArray(String[]::new);
            for (String fieldName : fieldNames)
            {
                if (CoreField.isCoreFieldName(fieldName))
                {
                    // Drop a log entry here since we can't seem to figure out how to get the exception out.
                    final String message = "Persistable structures must not contain user-defined fields with same name as core fields. " +
                            "Structure: " + structureName + ", field: " + fieldName;

                    logger.warn(message);
                    throw new IllegalArgumentException(message);
                }
            }
        }

        StructureWrapper wrapper = new StructureWrapper(structure, isDataMessage, isPersistable, includeInDeviceLogList);
        structureMap.put(structureName, wrapper);
    }

    @Override
    public List<IRaptorDataStructure> getPersistableDataStructures()
    {
        return structureMap.values().stream()
                .filter(StructureWrapper::isPersistable)
                .map(StructureWrapper::getDataStructure)
                .collect(Collectors.toList());
    }

    @Override
    public List<IRaptorDataStructure> getMessageDataStructures()
    {

        return structureMap.values().stream()
                .filter(StructureWrapper::isDataMessage)
                .map(StructureWrapper::getDataStructure)
                .collect(Collectors.toList());
    }

    @Override
    public List<IRaptorDataStructure> getCommandDataStructures()
    {

        return structureMap.values().stream()
                .filter(wrapper -> !wrapper.isDataMessage())
                .map(StructureWrapper::getDataStructure)
                .collect(Collectors.toList());
    }

    @Override
    public IRaptorDataStructure getDataStructure(String structureName)
    {
        final StructureWrapper wrapper = structureMap.get(structureName);
        if (wrapper == null)
        {
            throw new NoSuchStructureException(structureName);
        }
        return wrapper.getDataStructure();
    }

    /**
     * Clears out the entire structure map and re-invokes createStructureMap() so that new {@link IRaptorDataStructure}
     * instances are re-created.
     */
    @Override
    public void resetStructures()
    {
        structureMap.clear();
        createStructureMap();
    }

    /**
     * Developers must implement this method such that it <b>always creates</b> brand new {@link IRaptorDataStructure}
     * instances. Developers are encouraged to use one of the defineDataStructure() methods to populate the structure map.
     */
    protected abstract void createStructureMap();

    @Override
    public List<IRaptorDataStructure> getExcludedDeviceLogStructures()
    {
        return structureMap.values().stream()
                .filter(s -> !s.isIncludedInDeviceLog())
                .map(StructureWrapper::getDataStructure)
                .collect(Collectors.toList());
    }

    /**
     * Defines no default device log data structure; the Device Log dialog will automatically default to the first
     * IRaptorDataStructure instance in the combo box.
     *
     * @return {@code null}
     */
    @Override
    public IRaptorDataStructure getDefaultDeviceLogDataStructure()
    {
        return null;
    }
}
