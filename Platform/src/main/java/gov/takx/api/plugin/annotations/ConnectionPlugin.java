package gov.takx.api.plugin.annotations;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;
import jakarta.inject.Named;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifying annotation to mark a Connection plugin class. Placing this annotation on a class declaration defines
 * it as a connection plugin, and it will be initialized as the TAKX core is started.
 * <p>
 * This annotation also declares the class as an {@link ApplicationScoped} bean within the plugin's CDI container.
 *
 * @since 2.0.0
 */
@SuppressWarnings("unused")
@Stereotype
@ApplicationScoped
@Named
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConnectionPlugin
{
    /**
     * @return Base plugin specification (required)
     */
    PluginSpecification spec();

    /**
     * @return Plugin descriptor
     */
    PluginDescriptor descriptor();

    /**
     * Property for a connection plugin to identify that it supports editing connections that are active.  Use case
     * would be something like a connection that polls for data on an interval.
     *
     * @return {@code true} if edit is supported.  Defaults to {@code false}
     */
    boolean editSupported() default false;

    /**
     * Property for a connection plugin to identify that it supports automatic reconnection.  Defaults to false.
     *
     * @return {@code true} to allow auto-reconnect support.  Defaults to {@code false}
     */
    boolean autoReconnectSupported() default false;

    /**
     * Property for a connection plugin to identify that it should go into the Connection Toolbar.  Prevents the
     * operator from opening a connection.  Can still do things like auto-connect from a template, or create the
     * connection from an App plugin.
     *
     * @return {@code true} to allow manual creation of the connection.
     * @since 2.5
     */
    boolean manualCreationAllowed() default true;

    /**
     * Property for a connection plugin to specify the maximum number of connections that can be opened in a project.
     * If there is no hard limit, just specify some arbitrarily large number that couldn't reasonably be exceeded.
     *
     * @return maximum number of connections allowed in a project.
     */
    int maxSupportedConnections() default 1;

    /**
     * String used to find the icon the controller wants to display in the Connection Panel in the TAKX UI. Will
     * display in the dropdown menu and super-imposed with the connection status LED. Unlike the iconPath metadata
     * this is not required (but recommended).
     *
     * @return String location icon path in the plugin's jar
     */
    // TODO:  Handle the default case where String.isEmpty() == true
    String panelIconPath() default "";
}
