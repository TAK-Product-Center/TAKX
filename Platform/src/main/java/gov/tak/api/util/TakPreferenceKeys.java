package gov.tak.api.util;

import gov.tak.platform.datapackage.PreferencesReader;
import gov.takx.platform.preferences.PreferenceEntry;

/**
 * Constants defining TAK preferences that are supported in TAKX.
 *
 * @since 5.3
 */
public class TakPreferenceKeys
{
    /**
     * Name of the preference element containing streaming TAKServer connections.  Note, this is not a key within
     * a given {@link PreferencesReader.Preference}, it is the <em>name</em> of a preference.
     * @since 5.4
     */
    public static final String COT_STREAMS = "cot_streams";

    // The root node under which the TAK preference entries are stored
    private static final String ROOT = PreferenceEntry.TAK_PREFERENCES_NODE.getKey() + "/";

    /** Default CA password for TAKServer connections. */
    public static final String CA_PASSWORD = "caPassword";

    /** Default client password for TAKServer connections. */
    public static final String CLIENT_PASSWORD = "clientPassword";

    /** "access" value on CoT events for a Map Item. */
    public static final String MAPITEM_ACCESS_PREF_KEY = ROOT + "mapitem.access";

    /** "caveat" value on CoT events for a Map Item. */
    public static final String MAPITEM_CAVEAT_PREF_KEY = ROOT + "mapitem.caveat";
    /** "releasableTo" value on CoT events for a Map Item. */
    public static final String MAPITEM_RELTO_PREF_KEY = ROOT + "mapitem.releasableTo";

    /** "access" value on CoT events for a Chat message. */
    public static final String GEOCHAT_ACCESS_PREF_KEY = ROOT + "geochat.access";

    /** "caveat" value on CoT events for a Chat message. */
    public static final String GEOCHAT_CAVEAT_PREF_KEY = ROOT + "geochat.caveat";

    /** "releasableTo" value on CoT events for a Chat message. */
    public static final String GEOCHAT_RELTO_PREF_KEY = ROOT + "geochat.releasableTo";

}
