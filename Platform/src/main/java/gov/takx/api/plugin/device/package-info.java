/**
 * Classes for defining device plugins.
 * <p>
 * Device plugins are  used to command and control (C2) sensors accessible over one or more communication pathways
 * defined by one or more connection plugin implementations.
 * <p>
 * When defining a device plugin, developers should create a device plugin class that extends
 * {@link gov.takx.api.plugin.device.ADevicePlugin} and annotate the class with
 * {@link gov.takx.api.plugin.annotations.DevicePlugin}. A device plugin controller class extending
 * {@link gov.takx.api.plugin.device.ADevicePluginController} should also be defined and will be
 * instantiated by the device plugin class. The controller class will be responsible for managing a single instance of
 * the device.
 *
 * @since 2.0
 */
package gov.takx.api.plugin.device;
