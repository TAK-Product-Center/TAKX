package gov.takx.api.plugin.annotations;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.plugin.device.IDevicePluginController;
import gov.takx.api.ui.IDeviceSwingUIProvider;
import jakarta.enterprise.inject.Stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark the class that provides the Swing UI for a given Device plugin.  The class on
 * which this annotation is used must implement {@link IDeviceSwingUIProvider}.  Multiple
 * instances of the annotated class will be instantiated, one per unit Id encountered.
 * <p>
 * There must be exactly one constructor, and it can have a variety of arguments which will be supplied automatically
 * when the object is instantiated.  The arguments may be of the following types (in any order):
 * <ul>
 * <li>{@link String}, which will be the unit Id</li>
 * <li>{@link gov.takx.api.mapobject.IMapObject}, the Map Object for which this UI provider is being created</li>
 * <li>{@link gov.takx.api.mapobject.IMapObjectController}, the Map Object controller for which this UI provider is being created</li>
 * <li>{@link gov.takx.api.plugin.device.IDevicePluginController}, the plugin controller for which this UI provider is being created</li>
 * <li>{@link gov.takx.api.plugin.device.IDevicePluginDelegate}, the delegate for the plugin controller (same as the MapObjectController</li>
 * </ul>
 *
 * @since 2.0.0
 */
@Stereotype
@PseudoBean
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DeviceSwingUIProvider
{
    /**
     * This value specifies the Device plugin controller class for which this is the UI provider.
     *
     * @return The class of the plugin controller for which this class provides the Swing UI
     */
    Class<? extends IDevicePluginController> forPluginController();
}
