package gov.tak.api.cot.detail;

import gov.tak.api.annotation.IncubatingApi;
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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

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

    /**
     * Extracts an attribute double value from the provided CoT detail, applies the {@code valueModifier} function (if
     * provided) and sets the value on the attribute set using the attribute name as the metadata key.
     * Returns the extracted (and optionally modified) value, or {@link Double#NaN} if the value was not present.
     *
     * @param attributeSet The attribute set
     * @param detail The CoT detail
     * @param attributeName The attribute name
     * @param valueModifier An optional function that modifies the extracted value before setting the value on the attribute set
     * @return The value extracted from the CoT detail or {@link Double#NaN} if the value was not present
     * @since 5.8
     */
    @IncubatingApi(since = "5.8")
    public double setMetaFromDoubleIfValid(@Nonnull AttributeSet attributeSet, @Nonnull CotDetail detail, @Nonnull String attributeName,
                                           @Nullable Function<Double, Double> valueModifier)
    {
        var value = parseDouble(detail.getAttribute(attributeName), Double.NaN);
        if (!Double.isNaN(value))
        {
            if (valueModifier != null) value = valueModifier.apply(value);
            attributeSet.setAttribute(attributeName, (double) Math.round(value));
        }
        return value;
    }

    /**
     * Extracts an attribute double value from the provided CoT detail and sets the value on the attribute set. If the
     * attribute is not present, the default value will be used. The value will set on the attribute set using the attribute
     * name as the metadata key. It will be set as an int if the {@code round} parameter is true. Always returns the
     * extracted (unrounded) value.
     *
     * @param attributeSet The attribute set
     * @param detail The CoT detail
     * @param attributeName The attribute name
     * @param defaultValue The value to use if the specified attribute is not present
     * @param round Pass {@code true} if the extracted value should be rounded and set on the map item as an int
     * @return The extracted value
     * @since 5.8
     */
    @IncubatingApi(since = "5.8")
    public double setMetaDouble(@Nonnull AttributeSet attributeSet, @Nonnull CotDetail detail, @Nonnull String attributeName,
                                double defaultValue, boolean round)
    {
        final var value = parseDouble(detail.getAttribute(attributeName), defaultValue);
        if (round)
        {
            attributeSet.setAttribute(attributeName, (int) Math.round(value));
        } else
        {
            attributeSet.setAttribute(attributeName, value);
        }

        return value;
    }

    /**
     * Extracts and attribute int value from the provided CoT detail and sets the value on the attribute set, using the same
     * attribute name.
     *
     * @param attributeSet The attribute set
     * @param detail The CoT detail
     * @param attributeName The attribute name
     * @param defaultValue The value to use if the specified attribute is not present
     * @return The extracted value
     * @since 5.8
     */
    @IncubatingApi(since = "5.8")
    public int setMetaInt(@Nonnull AttributeSet attributeSet, @Nonnull CotDetail detail, @Nonnull String attributeName, int defaultValue)
    {
        final var value = parseInt(detail.getAttribute(attributeName), defaultValue);
        attributeSet.setAttribute(attributeName, value);
        return value;
    }

    /**
     * Extracts the {@code double} metadata value specified by {@code attributeName} from an {@link AttributeSet} and sets
     * the value on the {@link CotDetail} using the same {@code attributeName}.
     *
     * @param attributeSet The attribute set
     * @param detail The CoT detail to update
     * @param attributeName The metadata/attribute name
     * @param defaultValue The value to use if the map item does not contain a metadata entry for {@code attributeName}
     * @since 5.8
     */
    @IncubatingApi(since = "5.8")
    public void setCotDetailFromAttributeSet(@Nonnull AttributeSet attributeSet, @Nonnull CotDetail detail, @Nonnull String attributeName, double defaultValue)
    {
        detail.setAttribute(attributeName, Double.toString(attributeSet.getDoubleAttribute(attributeName, defaultValue)));
    }

    /**
     * Extracts the {@code int} metadata value specified by {@code attributeName} from an {@link AttributeSet} and sets
     * the value on the {@link CotDetail} using the same {@code attributeName}.
     *
     * @param attributeSet The attribute set
     * @param detail The CoT detail to update
     * @param attributeName The metadata/attribute name
     * @param defaultValue The value to use if the attribute set does not contain a metadata entry for {@code attributeName}
     * @since 5.8
     */
    @IncubatingApi(since = "5.8")
    public void setCotDetailFromAttributeSet(@Nonnull AttributeSet attributeSet, @Nonnull CotDetail detail, @Nonnull String attributeName, int defaultValue)
    {
        detail.setAttribute(attributeName, Integer.toString(attributeSet.getIntAttribute(attributeName, defaultValue)));
    }

    /**
     * Extracts the {@code boolean} metadata value specified by {@code attributeName} from ab {@link AttributeSet} and sets
     * the value on the {@link CotDetail} using the same {@code attributeName}.
     *
     * @param attributeSet The attribute set
     * @param detail The CoT detail to update
     * @param attributeName The metadata/attribute name
     * @param defaultValue The value to use if the attribute set does not contain a metadata entry for {@code attributeName}
     * @since 5.8
     */
    @IncubatingApi(since = "5.8")
    public void setCotDetailFromAttributeSet(@Nonnull AttributeSet attributeSet, @Nonnull CotDetail detail, @Nonnull String attributeName, boolean defaultValue)
    {
        if (!attributeSet.containsAttribute(attributeName)) return;
        detail.setAttribute(attributeName, Boolean.toString(attributeSet.getBooleanAttribute(attributeName, defaultValue)));
    }
}
