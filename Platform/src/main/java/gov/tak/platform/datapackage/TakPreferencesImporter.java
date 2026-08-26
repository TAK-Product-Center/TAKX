package gov.tak.platform.datapackage;

import com.google.common.annotations.VisibleForTesting;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.PreferencesNode;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Objects;

/**
 * Importer to handle a "TAK" preferences file.  There are several preference names, legacy and current, that should
 * all be processed.  Each entry in any of the named preferences will be copied into our preferences under
 * {@link gov.takx.platform.preferences.PreferenceEntry#TAK_PREFERENCES_NODE}.
 * <p/>
 * Example:
 * <pre>{@code
 *   <preferences>
 *     <preference version="1" name="com.atakmap.app.civ_preferences">
 *         <entry key="mapitem.access" class="class java.lang.String">SECRET</entry>
 *         <entry key="mapitem.caveat" class="class java.lang.String">Rel To</entry>
 *         <entry key="mapitem.releasableTo" class="class java.lang.String">FVEY</entry>
 *     </preference>
 *   </preferences>
 * }</pre>
 *
 * @since 5.3
 */
@PseudoBean
public class TakPreferencesImporter
{
    // All the names under which ATAK preferences can be stored
    @VisibleForTesting
    static final List<String> PREFERENCE_NAMES =
            List.of("com.atakmap.app_preferences",
                    "com.atakmap.app.civ_preferences", "com.atakmap.civ_preferences",
                    "com.atakmap.app.fvey_preferences", "com.atakmap.fvey_preferences");

    private final PreferencesNode takPreferencesNode = PreferenceEntry.TAK_PREFERENCES_NODE.node();

    /**
     * Import all {@link PreferencesReader.Preference preference} {@link PreferencesReader.Entry entries} from our
     * supported preference names.  All key/value pairs will be placed into
     * {@link gov.takx.platform.preferences.PreferenceEntry#TAK_PREFERENCES_NODE}.
     *
     * @param preferences Preferences to import
     */
    public void importPreferences(@Nonnull PreferencesReader.Preferences preferences)
    {
        PREFERENCE_NAMES.stream()
                .map(preferences::findByName)
                .filter(Objects::nonNull)
                .flatMap(p -> p.entries.stream())
                .forEach(entry -> takPreferencesNode.put(entry.key, entry.value));
    }
}
