package gov.takx.api.annotation;

import gov.takx.api.preferences.PreferenceEntryChange;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

import javax.swing.ImageIcon;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This qualifier allows for injection of {@link gov.takx.api.preferences.IPreferenceEntry} values for tool
 * preferences created by plugins.  Note that unlike other preference value injection, due to the transient nature
 * of tool preferences, the injection point must be for a {@link java.util.function.Supplier} of the data type desired.
 * For example:
 * <pre>{@code
 *     @Inject
 *     @ToolPreference(toolUid = TOOL_UID, preferenceId = BOOLEAN_PREF_ID)
 *     private Supplier<Boolean> booleanPreferenceValueSupplier;
 * }</pre>
 * <p>
 * Do not confuse this annotation, which is non-binding, with {@link ToolPreferenceType}, which is binding and
 * supports restrictions on observed {@link PreferenceEntryChange} events.
 *
 * @since 5.5.1
 * @see gov.takx.api.preferences.IToolPreferenceManager
 */
@Target({PARAMETER, FIELD, METHOD, CONSTRUCTOR, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface ToolPreference
{
    /**
     * @return The tool's UID, as used in {@link gov.takx.api.preferences.IToolPreferenceManager#addTool(String, String, String, ImageIcon, List)}
     */
    @Nonbinding
    String toolUid() default "";

    /**
     * @return The preference ID, as specified in one of the Tool Preference constructors.
     */
    @Nonbinding
    String preferenceId() default "";
}
