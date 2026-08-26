package gov.takx.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation describing a plugin's documentation to be linked into the TAKX Help toolbar.
 *
 * @since 2.26 (reworked in 3.0)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PluginDocumentation
{
    /**
     * @return The display name to use to label the entry in the help toolbar.
     */
    String displayName();

    /**
     * @return The tool tip text for the entry in the help toolbar.
     */
    String tooltip();

    /**
     * Specifies the resource path, which must exist within the plugin's jar file, of the documentation file(s).
     * Or, this may be an external URL.
     *
     * @return local resource path or external URL
     * @see #external()
     */
    String filePath();

    /**
     * @return If {@code true} the {@link #filePath()}} will be interpreted as an external URL instead of a resource
     * path within the jar.  This allows externally hosted documents (via webserver) to be displayed.
     */
    boolean external() default false;

    /**
     * @return If {@code true} the {@link #filePath()} must refer to a resource within the jar that is either a
     * zip file or a directory.
     * <ul>
     * <li>
     * If the {@link #filePath()} refers to a zip file, it will be expanded into a temporary location. The zip file
     * content should NOT include a top-level directory.  A new, temporary, directory will be created, and the content
     * will be extracted to that new directory.
     * </li>
     * <li>
     * If the {@link #filePath()} refers to a directory, then that directory's content will be extracted
     * to a temporary location.
     * </li>
     * </ul>
     * In either case, an {@code index.html} file must be present at the top-level of the extracted content.
     * This {@code index.html} will then be opened using the system default web browser.
     * <p/>
     * Note: if this value is {@code true}, then {@code external} will be ignored.
     *
     * @since 5.1
     */
    boolean htmlDoc() default false;

    /**
     * @return The resource path to the icon to display in UI to represent this documentation.
     * If not specified, the icon specified on the containing plugin will be used.
     */
    String iconPath() default "";
}
