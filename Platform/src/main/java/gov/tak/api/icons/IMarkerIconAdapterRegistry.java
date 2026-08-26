package gov.tak.api.icons;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Contract for registering Marker icon adapters.  Plugins can register as additional adapters, and they are given
 * priority over the default handling.
 *
 * @since 3.0
 */
@InjectableService
@NoPluginImplementations
public interface IMarkerIconAdapterRegistry
{
    /**
     * Register an adapter to apply to all Marker instances created, or when the CoT type of the marker is changed.
     *
     * @param iconAdapter Adapter to register
     */
    void addIconAdapter(IconAdapter iconAdapter);

    /**
     * Unregister a previously registered adapter.
     *
     * @param iconAdapter Adapter to unregister
     */
    void removeIconAdapter(IconAdapter iconAdapter);
}
