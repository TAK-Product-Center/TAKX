package gov.takx.api.plugin.annotations;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.plugin.connection.IConnectionPluginController;
import gov.takx.api.plugin.connection.IConnectionPluginDelegate;
import gov.takx.api.ui.IConnectionSwingUIProvider;
import jakarta.enterprise.inject.Stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark the class that provides the Swing UI for a given Connection plugin.  The class on
 * which this annotation is used must implement {@link IConnectionSwingUIProvider}.  Multiple
 * instances of the annotated class will be instantiated, one per connection UUID.
 * <p>
 * There must be exactly one constructor, and it can have a variety of arguments which will be supplied automatically
 * when the object is instantiated.  The arguments may be of the following types (in any order):
 * <ul>
 * <li>{@link IConnectionPluginController}, the controller for which this UI provider is being created</li>
 * <li>{@link IConnectionPluginDelegate}, the delegate for the plugin controller</li>
 * </ul>
 *
 * @since 2.0.0
 */
@Stereotype
@PseudoBean
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConnectionSwingUIProvider
{
    Class<? extends IConnectionPluginController> forPluginController();
}
