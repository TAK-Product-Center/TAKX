package gov.takx.api.annotation;

import gov.takx.api.preferences.PreferenceEntryChange;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import javax.swing.ImageIcon;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This qualifier supports limiting {@link PreferenceEntryChange} event observation.
 * <p>
 * Do not confuse this annotation, which IS binding, with {@link ToolPreference}, which is non-binding and
 * supports injection of {@link gov.takx.api.preferences.IPreferenceEntry tool preference} values.
 *
 * @since 5.5.1
 */
@Target({PARAMETER})
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface ToolPreferenceType
{
    /**
     * @return The tools UID, as used in {@link gov.takx.api.preferences.IToolPreferenceManager#addTool(String, String, String, ImageIcon, List)}
     */
    String toolUid() default "";

    /**
     * @return The preference ID, as specified in one of the Tool Preference constructors.
     */
    String preferenceId() default "";

    /**
     * Annotation literal for use in event selection.
     */
    @SuppressWarnings("ClassExplicitlyAnnotation")
    class Literal extends AnnotationLiteral<ToolPreferenceType> implements ToolPreferenceType
    {
        private final String toolUid;
        private final String preferenceId;

        public Literal(String toolUid, String preferenceId)
        {
            this.toolUid = toolUid;
            this.preferenceId = preferenceId;
        }

        @Override
        public String toolUid()
        {
            return toolUid;
        }

        @Override
        public String preferenceId()
        {
            return preferenceId;
        }
    }
}
