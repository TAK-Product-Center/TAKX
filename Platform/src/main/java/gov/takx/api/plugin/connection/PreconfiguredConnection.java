package gov.takx.api.plugin.connection;

import gov.takx.api.plugin.annotations.ConfigurationValue;
import gov.takx.api.plugin.annotations.DefinePreconfiguredConnection;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Configuration for a "pre-configured" connection.  This class holds the data needed to configure a connection
 * controller instance.
 *
 * @see DefinePreconfiguredConnection
 * @since 2.20
 */
public class PreconfiguredConnection
{
    private final String family;
    private final String type;
    private final String name;
    private final String description;
    private final String iconPath;
    private final String panelIconPath;
    private final boolean showConnectionDialog;
    private final String format;
    private final Map<String, Object> configuration;
    private final ClassLoader classLoader;

    /**
     * Construct a {@code PreconfiguredConnection} from the values in the given annotation.
     *
     * @param annotation  The annotation from which to create an instance
     * @param classLoader Class loader to use for accessing resources (like icon path)
     * @return New instance
     */
    public static PreconfiguredConnection fromAnnotation(DefinePreconfiguredConnection annotation, ClassLoader classLoader)
    {
        // Extract the map of name/value pairs
        final Map<String, Object> configuration = Stream.of(annotation.configuration())
                .collect(Collectors.toMap(ConfigurationValue::name, ConfigurationValue::value));

        return new PreconfiguredConnection(annotation.family(), annotation.type(), annotation.name(),
                annotation.description(), annotation.iconPath(), annotation.panelIconPath(),
                annotation.showConnectionDialog(), annotation.format(), configuration, classLoader);
    }

    /**
     * Construct a pre-configured connection configuration.
     *
     * @param family               The family of the connection to configure
     * @param type                 The type of the connection to configure
     * @param name                 The name to display for this configured connection in the various TAKX UI elements
     * @param description          The description to display for this configured connection in the various TAKX UI elements
     * @param iconPath             The resource path to the icon to display
     * @param showConnectionDialog {@code true} to show the configuration dialog, {@code false} otherwise
     * @param configuration        Configuration values map
     */
    public PreconfiguredConnection(String family, String type, String name, String description, String iconPath,
                                   String panelIconPath, boolean showConnectionDialog, String format,
                                   Map<String, Object> configuration, ClassLoader classLoader)
    {
        this.family = family;
        this.type = type;
        this.name = name;
        this.description = description;
        this.iconPath = iconPath;
        this.panelIconPath = panelIconPath;
        this.showConnectionDialog = showConnectionDialog;
        this.format = format;
        this.configuration = configuration;
        this.classLoader = classLoader;
    }

    /**
     * @return The family of the connection to configure
     */
    public String getFamily()
    {
        return family;
    }

    /**
     * @return The type of the connection to configure
     */
    public String getType()
    {
        return type;
    }

    /**
     * @return The name to display for this configured connection in the various TAKX UI elements
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return The description to display for this configured connection in the various TAKX UI elements
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @return The resource path to the icon to display
     */
    public String getIconPath()
    {
        return iconPath;
    }

    /**
     * @return The resource path to the icon to display in the connections panel
     */
    public String getPanelIconPath()
    {
        return panelIconPath;
    }

    /**
     * @return {@code true} to show the configuration dialog, {@code false} otherwise
     */
    public boolean isShowConnectionDialog()
    {
        return showConnectionDialog;
    }

    /**
     * @return The format to use for the connection's comm path
     */
    public String getFormat()
    {
        return format;
    }

    /**
     * @return Configuration values map
     */
    public Map<String, Object> getConfiguration()
    {
        return configuration;
    }

    public ClassLoader getClassLoader()
    {
        return classLoader;
    }

    @Override
    public String toString()
    {
        return "PreconfiguredConnection{" +
                "family='" + family + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", iconPath='" + iconPath + '\'' +
                ", format='" + format + '\'' +
                ", showConnectionDialog=" + showConnectionDialog +
                ", configuration=" + configuration +
                ", classLoader=" + classLoader +
                '}';
    }
}
