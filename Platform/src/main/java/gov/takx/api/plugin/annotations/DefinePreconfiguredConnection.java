package gov.takx.api.plugin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify a "pre-configured" connection plugin to create and make available from within the
 * TAKX UI. This connection will appear in the connections panel and the connection toolbar.
 * <p>
 * A pre-configured connection allows a plugin to define the connection plugin (family/type) as well as
 * a custom name and icon, and s set of configuration values to be set on the connection controller.
 * <p>
 * Typically, this is used to create a connection with a specified {@code format} so that a device plugin
 * can attach a parser to it.  This supports reuse of common connection types, like Serial or Ethernet.
 *
 * @see ConfigurationValue
 * @see ConfigurationValues
 * @since 2.20
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(value = DefinePreconfiguredConnections.class)
public @interface DefinePreconfiguredConnection
{
    /**
     * @return The family of the connection to configure
     */
    String family();

    /**
     * @return The type of the connection to configure
     */
    String type();

    /**
     * @return The name to display for this configured connection in the various TAKX UI elements
     */
    String name();

    /**
     * @return The description to display for this configured connection in the various TAKX UI elements
     */
    String description();

    /**
     * Specify the path to the icon to display in UI to represent this connection.
     * This is used in various UI panels to represent this plugin.  It is common to use a different icon for a
     * pre-configured connection than the base connection is uses (so the user can visually distinguish them in the UI).
     *
     * @return The icon path in the plugin's jar.
     */
    String iconPath();

    /**
     * Specify the path to the icon to display specifically in the Connections panel.  Use this when you need
     * a different icon in the connections panel than in the Connect toolbar.  If not specified, the value in
     * {@link #iconPath()} will be used.
     *
     * @return The icon path in the plugin's jar.
     */
    String panelIconPath() default "";

    /**
     * Specify whether the connection plugins standard configuration UI panel should be shown.
     * <p>
     * If the configuration specified is <em>complete</em>, and no user input is required them this can be set
     * to {@code false}.  Otherwise, it should be left as {@code true} so the user can complete the required
     * configuration elements.
     *
     * @return {@code true} to show the configuration dialog, {@code false} otherwise
     */
    boolean showConnectionDialog() default true;

    /**
     * Specify the format string to assign to the comm path when opened.
     * <p>
     * By far, the most common case for using a preconfigured connection is to use a custom format so that a
     * device plugin can know to attach its parser.
     *
     * @return The format to use for the connection's comm path
     */
    String format();

    /**
     * @return Configuration values
     */
    ConfigurationValue[] configuration() default {};
}
