package gov.takx.api.annotation;

import gov.takx.api.preferences.PreferenceEntryChange;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This qualifier supports limiting {@link PreferenceEntryChange} event observation.
 * <p>
 * Do not confuse this annotation, which IS binding, with {@link UserPreference}, which is non-binding and
 * supports injection of {@link UserPreferenceEntry} values.
 *
 * @since 2.35
 */
@Target({PARAMETER})
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface UserPreferenceType
{
    UserPreferenceEntry value() default UserPreferenceEntry.NONE;

    /**
     * Annotation literals for use in event selection.
     */
    class Literal extends AnnotationLiteral<UserPreferenceType> implements UserPreferenceType
    {
        private final UserPreferenceEntry preferenceEntry;

        public Literal(UserPreferenceEntry preferenceEntry)
        {
            this.preferenceEntry = preferenceEntry;
        }

        @Override
        public UserPreferenceEntry value()
        {
            return preferenceEntry;
        }
    }
}
