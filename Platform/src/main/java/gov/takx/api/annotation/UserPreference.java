package gov.takx.api.annotation;

import gov.takx.api.preferences.PreferenceEntryChange;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This qualifier allows for injection of {@link UserPreferenceEntry} values.
 * <p>
 * Do not confuse this annotation, which is non-binding, with {@link UserPreferenceType}, which is binding and
 * supports restrictions on observed {@link PreferenceEntryChange} events.
 *
 * @since 2.35
 */
@Target({PARAMETER, FIELD, METHOD, CONSTRUCTOR, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface UserPreference
{
    @Nonbinding
    UserPreferenceEntry value() default UserPreferenceEntry.NONE;
}
