package gov.takx.api.plugin.annotations;

import gov.takx.api.mapobject.MapObjectOption;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.plugin.device.ADataStructureFactory;
import gov.takx.api.plugin.device.IDevicePluginController;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;
import jakarta.inject.Named;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifying annotation to mark a Device plugin class. Placing this annotation on a class declaration defines
 * it as a device plugin, and it will be initialized as the TAKX core is started.
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
public @interface DevicePlugin
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
     * Determines whether the user is allowed to manually create an instance of this device.
     *
     * @return {@code true} to allow the user to manually create an instance of this device.  Defaults to {@code true}.
     */
    boolean manualCreationAllowed() default true;

    /**
     * Determines whether or not the device map object can be dragged into a container and be contained.
     *
     * @return {@code true} to allow containment inside of a container.  Defaults to {@code true}.
     * @since 2.7
     */
    boolean canBeContained() default true;

    /**
     * For a device plugin that allows manual creation, see {@link #manualCreationAllowed()}, if this is set to
     * {@code true}, then the user will not be prompted for a unit ID when the device is dropped onto the globe.
     * A unique Id (UUID) will be generated automatically.
     *
     * @return {@code true} to automatically generate a unique unit Id when an instance is manually created by the user.  Defaults to false.
     */
    boolean autoGenerateUnitId() default false;

    /**
     * Specify the default preferences tab to display when the Map Object preferences dialog is displayed.
     *
     * @return The identifier of the default preferences tab. Defaults to the empty string.
     */
    String defaultPreferencesTab() default "";

    /**
     * Specify the resource path to the XML file to use to construct the data structures (for RDMs). Optional.
     *
     * @return XML resource path
     */
    String dataStructureXml() default "";

    /**
     * Specify by message type, which data structures should be excluded from the device log.  This is equivalent
     * to a {@link ADataStructureFactory#defineDataStructure(String, IRaptorDataStructure, boolean, boolean, boolean)}
     * call with includeInDeviceLogList set to {@code false}.
     *
     * @return Array of message type strings to exclude from the device log.
     * @since 2.21
     */
    @SuppressWarnings("JavadocReference")
    String[] excludeFromDeviceLog() default {};

    /**
     * Specify the default device log message type.  This is equivalent to overriding the method
     * {@link ADataStructureFactory#getDefaultDeviceLogDataStructure()} specifying the message type string instead
     * of the structure itself.
     *
     * @return message type string to use as a device log default.
     * @since 2.21
     */
    String defaultDeviceLogMessage() default "";

    /**
     * Specify the resource path to the XML file to use to construct the command structures (for RCMs). Optional.
     *
     * @return XML resource path
     */
    String commandStructureXml() default "";

    /**
     * Specify the Map Object Options to apply to all Map Objects created from this device.  These options will
     * be applied to any Map Object created for this plugin, unless an overriding set of options are supplied in
     * a call to create a Map Object.
     *
     * @return Map Object Options array
     * @see MapObjectSpec.Builder#mapObjectOptions()
     * @since 2.29
     */
    MapObjectOption[] mapObjectOptions() default {};

    /**
     * Specify the key of the importer that should be used if a file is dropped onto this device
     * via the drag and drop system. The importer will be provided with the map object's unique key during the
     * pre-configuration process and should know how to import the data into the map object via the messaging system,
     * events or by accessing the {@link IDevicePluginController} directly.
     *
     * @since 3.0
     * TODO Convert this to a use of the new ForPlugin annotation, when it is merged in
     */
    String fileImporterKey() default "";
}
