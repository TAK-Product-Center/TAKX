package gov.tak.api.cot.detail;

import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.maps.MapItem;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.AttributeSet;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.commons.utils.ParseUtils;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.Set;

/**
 * Abstract base for all CoT detail element handlers.
 * <p>
 * Bidirectional conversion from CoT XML detail elements and Map Objects/Items.
 *
 * @since 3.0
 */
@SuppressWarnings("deprecation")
@PseudoBean
public abstract class CotDetailHandler implements ICotDetailHandler2
{
    @Inject
    protected IMapObjectFinder mapObjectPersist; // Don't change the name of this field, or you'll get a ton of BCP errors

    // Set of detail names this handler supports
    private final Set<String> detailNames;

    protected CotDetailHandler(Set<String> detailNames)
    {
        this.detailNames = Collections.unmodifiableSet(detailNames);
    }

    protected CotDetailHandler(String detailName)
    {
        this(Collections.singleton(detailName));
    }

    /**
     * Get the set of detail names this handler supports
     *
     * @return Set of detail names
     */
    @Override
    public final Set<String> getDetailNames()
    {
        return detailNames;
    }

    /** @since 5.5.1 */
    @Override
    public ICotDetailHandler.ImportResult toItemMetadata(Object o, AttributeSet attributeSet, CotEvent cotEvent, CotDetail cotDetail)
    {
        return ImportResult.toKernelResult(toItemMetadata((MapItem) o, attributeSet, cotEvent, cotDetail));
    }

    /**
     * Convert {@link CotEvent} {@link CotDetail} to {@link AttributeSet} metadata
     *
     * @param item The map item being processed; owner of {@code attrs}
     * @param attributeSet {@link AttributeSet}
     * @param cotEvent {@link CotEvent} being processed
     * @param cotDetail The detail associated with this {@link ICotDetailHandler} (read from this)
     *
     * @return {@link ICotDetailHandler.ImportResult#SUCCESS} if handled successfully,
     * {@link ICotDetailHandler.ImportResult#FAILURE} if handled but failed,
     * {@link ICotDetailHandler.ImportResult#IGNORE} if not handled or N/A,
     * {@link ICotDetailHandler.ImportResult#DEFERRED} if we should try again later
     *
     * @since 5.5.1
     */
    public ImportResult toItemMetadata(MapItem item, AttributeSet attributeSet, CotEvent cotEvent, CotDetail cotDetail)
    {
        return toItemMetadata(item, cotEvent, cotDetail);
    }

    /** @since 5.5.1 */
    @Override
    public boolean toCotDetail(Object o, AttributeSet attributeSet, CotEvent cotEvent, CotDetail cotDetail)
    {
        return toCotDetail((MapItem) o, cotEvent, cotDetail);
    }

    /** @since 5.5.1 */
    @Override
    public boolean isSupported(Object o, AttributeSet attributeSet, CotEvent cotEvent, CotDetail cotDetail)
    {
        return isSupported((MapItem) o, cotEvent, cotDetail);
    }

    /**
     * Convert CoT event detail to map item metadata
     *
     * @param item   Map item
     * @param event  Item's associated CoT event
     * @param detail The detail associated with this handler (read from this)
     * @return {@link ImportResult#SUCCESS} if handled successfully<br>
     * {@link ImportResult#FAILURE} if handled but failed<br>
     * {@link ImportResult#IGNORE} if not handled or N/A<br>
     * {@link ImportResult#DEFERRED} if we should try again later
     */
    public abstract ImportResult toItemMetadata(MapItem item, CotEvent event, CotDetail detail);

    /**
     * Convert map item metadata to a CoT detail
     *
     * @param item  Map item to read
     * @param event Item's associated CoT event
     * @param root  The CoT event root detail (add to this)
     * @return True if handled, false if not
     */
    public abstract boolean toCotDetail(MapItem item, CotEvent event, CotDetail root);

    /**
     * Check if this handler supports this item
     * Used to filter out certain types from being processed by a handler
     *
     * @param item   Map item
     * @param event  CoT Event
     * @param detail Associated CoT detail
     * @return True if supported
     */
    public boolean isSupported(MapItem item, CotEvent event, CotDetail detail)
    {
        return true;
    }

    /**
     * Helper method for obtaining a point given a map item
     *
     * @param item Map item
     * @return A single point representing this map item or {@code null} if N/A
     */
    protected GeoPointMetaData getPoint(MapItem item)
    {
        return item instanceof IMapObject mapObject ? mapObject.getGeoPointMetaData() : null;
    }

    /**
     * Convert a string to a double (exceptions caught with fallback)
     *
     * @param value      String value
     * @param defaultVal Default value if conversion fails
     * @return Converted value or default value if conversion failed
     */
    protected double parseDouble(String value, double defaultVal)
    {
        return ParseUtils.parseDouble(value, defaultVal);
    }

    /**
     * Convert a string to an int (exceptions caught with fallback)
     *
     * @param value      String value
     * @param defaultVal Default value if conversion fails
     * @return Converted value or default value if conversion failed
     */
    protected int parseInt(String value, int defaultVal)
    {
        return ParseUtils.parseInt(value, defaultVal);
    }

    /**
     * Find a map item by its UID with a given class
     *
     * @param uid Map item UID
     * @param <T> Class type
     * @return Map item of type T or {@code null} if not found or not convertible
     */
    @SuppressWarnings("unchecked")
    protected <T extends MapItem> T getMapItem(String uid)
    {
        return uid == null
                ? null
                : (T) mapObjectPersist.findMapObjectByUid(uid);
    }
}
