package gov.takx.core.view.gui.maps.importers;

import gov.takx.api.annotation.Internal;
import gov.takx.platform.KeyParts;
import gov.takx.platform.preferences.PreferencesNode;

import java.util.Map;

/**
 * Intern the importer configurations from the preferences.
 *
 * @since 2.35
 */
@Internal
public final class FileImporterConfigurations extends PreferencesNode
{
    private FileImporterConfigurations(PreferencesNode rootNode)
    {
        super(rootNode);
    }

    /**
     * @return Map of all defined importers (by extension)
     */
    public Map<String, FileImporterConfiguration> getImporterConfigurations()
    {
        return childrenToMap(this::makeConfiguration);
    }

    /**
     * @return The {@link FileImporterConfiguration} for the given file extension
     */
    public FileImporterConfiguration getImporterConfiguration(String extension)
    {
        return makeConfiguration(nodeExists(extension) ? node(extension) : null);
    }

    private FileImporterConfiguration makeConfiguration(PreferencesNode extNode)
    {
        return extNode != null
                ? new FileImporterConfiguration(extNode, extNode.node(FileImporter.PREFS_PRECONFIG_PROPS_KEY).keyValuesAsMap())
                : null;
    }

    /**
     * Set/update an importer configuration.
     *
     * @param key       Importer's unique key
     * @param extension File extension handled by the importer
     * @param iconURL   URL for icon to show in the UI for this importer
     */
    void setImporterConfiguration(String key, String extension, String iconURL)
    {
        final PreferencesNode node = node(extension);
        final KeyParts keyParts = new KeyParts(key);

        node.put(FileImporter.PREFS_PLUGIN_FAMILY_KEY, keyParts.getFamily());
        node.put(FileImporter.PREFS_PLUGIN_TYPE_KEY, keyParts.getType());
        node.put(FileImporter.PREFS_PLUGIN_ICON_KEY, iconURL);
    }

    /**
     * Importer configuration for a file extension.
     *
     * @since 2.35
     */
    public static final class FileImporterConfiguration
    {
        private final String family; // Importer plugin family
        private final String type; // Importer plugin type
        private final String iconUrl; // Importer icon URL
        private final Map<String, String> properties; // Pre-configuration properties

        private FileImporterConfiguration(PreferencesNode extensionNode, Map<String, String> properties)
        {
            family = extensionNode.get(FileImporter.PREFS_PLUGIN_FAMILY_KEY, null);
            type = extensionNode.get(FileImporter.PREFS_PLUGIN_TYPE_KEY, null);
            iconUrl = extensionNode.get(FileImporter.PREFS_PLUGIN_ICON_KEY, null);
            this.properties = properties;
        }

        public String getFamily()
        {
            return family;
        }

        public String getType()
        {
            return type;
        }

        public String getIconUrl()
        {
            return iconUrl;
        }

        public Map<String, String> getProperties()
        {
            return properties;
        }
    }
}
