package gov.tak.api.cot.importer;

import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.maps.MapItem;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.platform.cot.util.AccessUtils;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.persist.IMapObjectPersist;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * CoT Event importer that creates items on the map.
 *
 * @since 3.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class MapItemImporter extends AbstractCotEventTypeImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // TODO put this constant somewhere centrally usable
    private static final String FROM_MISSION_PACKAGE = "MissionPackage";

    @Inject
    protected IMapObjectPersist mapObjectPersist;

    /**
     * Construct on a list of types.
     *
     * @param prefixOnly {@code true} if the type list is to be interpreted as prefixes of types, not discrete types.
     * @param types      List of type codes or type prefixes
     */
    protected MapItemImporter(boolean prefixOnly, String... types)
    {
        super(prefixOnly, types);
    }

    @Override
    public ImportResult importData(CotEvent event)
    {
        if (super.importData(event) != ImportResult.SUCCESS) return ImportResult.IGNORE;

        final MapItem existing = findItem(event);
        final ImportResult result = importMapItem(existing, event);

        if (result == ImportResult.SUCCESS)
        {
            final MapItem item = findItem(event);

            if (existing == null && !isLocalImport(event.getAttributeSet()))
            {
                postNotification(item);
            }

            if (item != null)
            {
                AccessUtils.setAccessFromCot(event, item);
            } else
            {
                logger.error("Import successful but item is null: {}", event.getType(), new Throwable());
            }
        }
        return result;
    }

    /**
     * Import a map item via CoT event.
     * <p>
     * Sub-class implementation is responsible for the following:
     * <ul>
     *     <li>Create the new map item (if it doesn't exist)</li>
     *     <li>Update item metadata based on event content</li>
     *     <li>Add the item to a map group (if not already added)</li>
     * </ul>
     *
     * @param existing Existing map item ({@code null} if new)
     * @param event    CoT event
     * @return {@link ImportResult#SUCCESS} if handled successfully
     * {@link ImportResult#FAILURE} if handled but failed
     * {@link ImportResult#IGNORE} if not handled or N/A
     * {@link ImportResult#DEFERRED} if we should try again later
     */
    protected abstract ImportResult importMapItem(MapItem existing, CotEvent event);

    /**
     * Find a map item given its CoT event UID.
     *
     * @return Map item or {@code null} if not found
     */
    @SuppressWarnings("unchecked")
    protected <T extends MapItem> T findItem(String uid)
    {

        return uid == null
                ? null
                : (T) mapObjectPersist.findMapObjectByUid(uid);
    }

    /**
     * Find a map item given a CoT event with its UID.  Equivalent to {@code findItem(event.getUID())}.
     *
     * @param event CoT event
     * @return Located MapItem or {@code null} if not found
     * @see #findItem(String)
     */
    protected MapItem findItem(CotEvent event)
    {
        return findItem(event.getUID());
    }

    /**
     * Helper method to check if import extras signify a local import depending on the "from" attribute.
     *
     * @param attributeSet event attribute set
     * @return True if local import
     */
    protected boolean isLocalImport(AttributeSet attributeSet)
    {
        final String from = attributeSet.getStringAttribute(StandardAttributeNames.FROM, "");

        return FROM_MISSION_PACKAGE.equals(from);
    }

    /**
     * Post notification for a newly received map item.
     *
     * @param item Map item to post the notification for
     */
    protected void postNotification(MapItem item)
    {
        if (item != null) return;

        // TODO - we don't have a general notification system
    }
}
