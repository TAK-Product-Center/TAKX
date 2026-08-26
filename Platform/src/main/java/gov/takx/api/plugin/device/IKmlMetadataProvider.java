package gov.takx.api.plugin.device;

/**
 * Interface for implementations of {@link IDevicePluginController} to implement so
 * that when the data on the globe are being exported, custom data can be reported from the controller.
 * Currently, only KML Description is implemented.  IDevicePluginController(s) that do not implement this
 * interface will only have default descriptions reported in the KML Export.
 *
 * @since 1.23
 */
public interface IKmlMetadataProvider
{
    /**
     * Return a description that will be used in the KML Description tag.  The description tag is used to populate
     * the KML balloon popup, and is a good place for custom descriptive items for a given IDevicePluginController.
     *
     * @return String KML Description.
     * @since 1.23
     */
    String getKmlDescription();
}
