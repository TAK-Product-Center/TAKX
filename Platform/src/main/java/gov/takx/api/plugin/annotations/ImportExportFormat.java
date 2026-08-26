package gov.takx.api.plugin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for the Import / Export format supported by an Import / Export plugin.  Multiple formats can be
 * specified in brackets for {@link ImportExportPlugin#formats()}
 *
 * @since 2.13
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ImportExportFormat
{
    /**
     * @return Format identifier for the programmatic use (for example, "csv")
     */
    String identifier();

    /**
     * @return The title of the format, to display in the GUI (for example "Comma Separated Value (CSV)"
     */
    String title();

    /**
     * @return Any notes related to how this format is supported (limitations etc.) This will be displayed
     * in the GUI when the user selects the format.
     */
    String notes();

    /**
     * @return File extensions commonly assigned to files of this format. Panels that allow the user to
     * select files may use this list to create file filters, but it is not required.
     */
    String[] fileTypes() default "";
}
