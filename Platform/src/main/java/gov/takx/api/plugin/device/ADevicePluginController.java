package gov.takx.api.plugin.device;

import gov.takx.api.messages.IPrePersistRaptorCommandMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.NoSuchStructureException;
import gov.takx.api.plugin.BasicPluginStateManager;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Abstract device plugin controller for plugin implementations. Developers are strongly encouraged to extend this
 * class and override methods as necessary.
 *
 * @since 2.27
 */
public abstract class ADevicePluginController extends BasicPluginStateManager implements IDevicePluginController
{
    protected final String family;
    protected final String type;
    protected final String unitId;
    protected final String uniqueKey;
    private volatile Map<String, String> metadata = null;

    protected IDevicePluginDelegate devicePluginDelegate;
    protected IDataStructureFactory factory;

    /**
     * Stores the plugin controller's unitId.
     *
     * @param family family namespace
     * @param type   type
     * @param unitId device unit id
     */
    protected ADevicePluginController(String family, String type, String unitId)
    {
        this.family = family;
        this.type = type;
        this.unitId = unitId;
        uniqueKey = KeyUtils.getUniqueKey(family, type, unitId);
    }

    @Override
    public String getFamily()
    {
        return family;
    }

    @Override
    public String getType()
    {
        return type;
    }

    @Override
    public String getUniqueKey()
    {
        return uniqueKey;
    }

    @Override
    public String getUnitId()
    {
        return unitId;
    }

    @Nonnull
    @Override
    public Set<String> getMetadataKeys()
    {
        return metadata != null ? metadata.keySet() : Collections.emptySet();
    }

    @Override
    public String getMetadataValue(String key)
    {
        return metadata != null ? metadata.get(key) : null;
    }

    /**
     * Set a value in the metadata map.  The map is allocated the first time a value is set.
     *
     * @param key   Metadata key
     * @param value value
     */
    protected synchronized void setMetadataValue(String key, String value)
    {
        if (metadata == null) metadata = new HashMap<>();
        metadata.put(key, value);
    }

    @Override
    public boolean isShareable(String sourceDeviceFamily, String sourceDeviceType, String sourceDeviceId, String passkey)
    {
        return false;
    }

    @Override
    public void receiveOtherDeviceMessage(IRaptorDataMessage rdm)
    {
        // Default to empty
    }

    @Override
    public void setDevicePluginDelegate(IDevicePluginDelegate mapObjectDelegate)
    {
        devicePluginDelegate = mapObjectDelegate;
        onDelegateInjected();
    }

    /**
     * Event triggered immediately after the IDevicePluginDelegate has been injected into the controller.  This event
     * can be used by classes to perform any setup once the delegate has been injected.
     */
    protected void onDelegateInjected()
    {
        // Default to empty
    }

    @Override
    public void shutdown()
    {
        devicePluginDelegate = null;
    }

    @Override
    public void setDataStructureFactory(IDataStructureFactory factory)
    {
        this.factory = factory;
    }

    /**
     * Convenience method for creating an {@link IPrePersistRaptorCommandMessage}. Inject the plugin controller into UI
     * panels and let them invoke this method to create command messages.
     *
     * @param structureType Name of the data structure to use as the basis for the command message
     * @return IPrePersistRaptorCommandMessage of the specified type
     * @throws IllegalStateException    When the IDataStructureFactory is {@code null}, or unit ID is {@code null}
     * @throws NoSuchStructureException When the specified structure cannot be found
     */
    protected IPrePersistRaptorCommandMessage createCommandMessage(String structureType) throws NoSuchStructureException
    {
        if (null == factory)
        {
            throw new IllegalStateException("IDataStructureFactory does not exist");
        }

        if (null == unitId)
        {
            throw new IllegalStateException("No Unit ID exists; command messages are not supported");
        }

        return factory.getDataStructure(structureType).createRaptorCommandMessage(unitId);
    }

    @Override
    public void delete()
    {
        // Nothing to do by default
    }
}
