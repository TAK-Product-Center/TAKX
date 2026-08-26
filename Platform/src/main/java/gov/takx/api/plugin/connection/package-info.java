/**
 * Classes for defining connection plugins.
 * <p>
 * Connection plugins are designed to either bring data into or send data out from the TAKX platform in near real-time.
 * <p>
 * When defining a connection plugin, developers should create a connection plugin class that extends
 * {@link gov.takx.api.plugin.connection.AConnectionPlugin} and annotate the class with
 * {@link gov.takx.api.plugin.annotations.ConnectionPlugin}. A connection plugin controller class extending
 * {@link gov.takx.api.plugin.connection.AConnectionPluginController} should also be defined and will be
 * instantiated by the connection plugin class. The controller class will be responsible for actually establishing an
 * instance of the connection.
 *
 * @since 2.0
 */
package gov.takx.api.plugin.connection;
