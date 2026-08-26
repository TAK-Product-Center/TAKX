package gov.tak.platform.cot.util;

import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.maps.MapItem;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.PreferencesNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static gov.tak.api.util.StandardAttributeNames.ACCESS;
import static gov.tak.api.util.StandardAttributeNames.CAVEAT;
import static gov.tak.api.util.StandardAttributeNames.RELEASABLE_TO;
import static gov.tak.api.util.TakPreferenceKeys.MAPITEM_ACCESS_PREF_KEY;
import static gov.tak.api.util.TakPreferenceKeys.MAPITEM_CAVEAT_PREF_KEY;
import static gov.tak.api.util.TakPreferenceKeys.MAPITEM_RELTO_PREF_KEY;

/**
 * Utility methods to deal with the "access" related fields in a CoT event and their associated attributes on a
 * map item: "access", "caveat", and "releasableTo".
 *
 * @since 5.3
 * @see CotEvent
 * @see MapItem
 */
public final class AccessUtils
{
    private final static PreferencesNode TAK_PREFERENCES_NODE = PreferenceEntry.TAK_PREFERENCES_NODE.node();

    private static final String UNDEFINED = "Undefined";

    /**
     * Sets the access defaults for a given map item if not already set.
     * @param item the map item to update
     */
    public static void setAccessDefault(@Nonnull MapItem item)
    {
        addAttributeIfNotPresent(item, ACCESS, MAPITEM_ACCESS_PREF_KEY, UNDEFINED);
        addAttributeIfNotPresent(item, CAVEAT, MAPITEM_CAVEAT_PREF_KEY, null);
        addAttributeIfNotPresent(item, RELEASABLE_TO, MAPITEM_RELTO_PREF_KEY, null);
    }

    /**
     * Set the access properties of the given CoT event to the current preference values.
     *
     * @param cotEvent the cot event to populate
     */
    public static void setAccessDefault(@Nonnull CotEvent cotEvent)
    {
        cotEvent.setAccess(TAK_PREFERENCES_NODE.get(ACCESS, UNDEFINED));
        cotEvent.setCaveat(TAK_PREFERENCES_NODE.get(CAVEAT, null));
        cotEvent.setReleasableTo(TAK_PREFERENCES_NODE.get(RELEASABLE_TO, null));
    }

    /**
     * Given a Cot Event and a map item, set the cot event properties to reflect the map item, defaulting to current
     * preferences if no value is set on the map item.
     *
     * @param cotEvent the cot event to populate
     * @param item the source map item
     */
    public static void setCotAccess(@Nonnull CotEvent cotEvent, @Nonnull MapItem item)
    {
        cotEvent.setAccess(getAttributeOrDefault(item, ACCESS, MAPITEM_ACCESS_PREF_KEY, UNDEFINED));
        cotEvent.setCaveat(getAttributeOrDefault(item, CAVEAT, MAPITEM_CAVEAT_PREF_KEY, null));
        cotEvent.setReleasableTo(getAttributeOrDefault(item, RELEASABLE_TO, MAPITEM_RELTO_PREF_KEY, null));
    }

    /**
     * Given a CoT Event, set the access values for the given map item.
     * 
     * @param event the cot event to use
     * @param item the map item to populate
     */
    public static void setAccessFromCot(@Nonnull CotEvent event, @Nonnull MapItem item)
    {
        item.setMetaString(ACCESS, event.getAccess());
        item.setMetaString(CAVEAT, event.getCaveat());
        item.setMetaString(RELEASABLE_TO, event.getReleasableTo());
    }

    /**
     * If the given map item doesn't have the attribute {@code attrKey}, add that attribute using the value of the
     * preference named {@code prefKey}, or the given {@code defaultValue} if not found.
     * 
     * @param item map item to update
     * @param attrKey attribute key
     * @param prefKey preference key
     * @param defaultValue fallback default value
     */
    private static void addAttributeIfNotPresent(@Nonnull MapItem item, @Nonnull String attrKey, @Nonnull String prefKey, @Nullable String defaultValue)
    {
        if (!item.hasMetaValue(attrKey)) item.setMetaString(attrKey, TAK_PREFERENCES_NODE.get(prefKey, defaultValue));
    }

    /**
     * @param item Map item to inspect
     * @param key attribute key
     * @param prefKey preference key
     * @param defaultValue fallback default value
     * @return the value of an attribute on the given map item, or the value of the given preference, falling back to
     *         the given {@code defaultValue}.
     */
    private static String getAttributeOrDefault(@Nonnull MapItem item, @Nonnull String key, @Nonnull String prefKey, @Nullable String defaultValue)
    {
        return item.hasMetaValue(key)
                ? item.getMetaString(key, "")
                : TAK_PREFERENCES_NODE.get(prefKey, defaultValue);
    }

    private AccessUtils()
    {
    }
}
