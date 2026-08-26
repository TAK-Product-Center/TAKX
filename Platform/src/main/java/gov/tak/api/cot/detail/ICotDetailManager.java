package gov.tak.api.cot.detail;

import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

/**
 * Contract for the service managing registration of handlers used to process specific CoT detail elements during
 * message parsing and generation.
 *
 * @since 3.0
 */
@InjectableService
@NoPluginImplementations
public interface ICotDetailManager
{
    /**
     * Register a detail handler for the detail element names it claims to handle.
     *
     * @param handler CoT detail handler
     * @see CotDetailHandler#getDetailNames()
     */
    void registerHandler(CotDetailHandler handler);

    /**
     * Unregister a detail handler.
     *
     * @param handler CoT detail handler to unregister
     */
    void unregisterHandler(CotDetailHandler handler);

    /**
     * Process all the detail sub-elements of the given CoT event.  Applying their configuration to the given
     * {@link MapItem}.
     *
     * @param item  Map Item to update
     * @param event CoT Event to process
     * @return Import result
     */
    ImportResult processDetails(MapItem item, CotEvent event);

    /**
     * Create and add CoT details to a given event.
     *
     * @param mapItem The map item to process
     * @param event   The CoT event to add to
     * @return {@code true} if one or more details were added, {@code false} if not
     */
    boolean addDetails(@Nonnull MapItem mapItem, @Nonnull CotEvent event);
}
