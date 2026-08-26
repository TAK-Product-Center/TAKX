package gov.takx.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation use to provide a title and description for {@link gov.takx.api.preferences.IPreferenceEntry} values for
 * inclusion in the preferences UI.
 * @since 3.2
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PreferenceDescriptor
{
    /**
     * @return The user-friendly title of the preference.
     */
    String title();

    /**
     * @return The description of the preference to display to the user.
     */
    String description();
}
