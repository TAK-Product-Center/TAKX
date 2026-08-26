package gov.takx.api.plugin.annotations;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.plugin.app.IAppPlugin;
import gov.takx.api.ui.IAppSwingUIProvider;
import jakarta.enterprise.inject.Stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark the class that provides the Swing UI for a given App plugin.  This class on
 * which this annotation is used must implement {@link IAppSwingUIProvider}.
 * <p/>
 * There must be exactly one constructor, and it can optionally have the following argument which will be supplied
 * automatically when the object is instantiated.
 * <ul>
 * <li>{@link IAppPlugin}, the App plugin for which this UI provider is being created</li>
 * </ul>
 *
 * @since 2.0.0
 */
@Stereotype
@PseudoBean
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AppSwingUIProvider
{
    /**
     * @return The class of the App plugin for which this class provides the Swing UI
     */
    Class<? extends IAppPlugin> forPlugin();
}
