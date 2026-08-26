package gov.takx.api.plugin.annotations;

import gov.takx.api.plugin.device.IDataStructureFactory;
import gov.takx.api.plugin.device.IDevicePlugin;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

/**
 * Qualifying annotation for injecting the {@link IDataStructureFactory} for a specified Device plugin.  Use like this:
 * <pre>
 *    &#64;Inject
 *    &#64;DataStructureFactory(forPlugin = AcmeDevicePlugin.class)
 *    private IDataStructureFactory factory;
 * </pre>
 *
 * @since 2.0.0
 */
@Qualifier
@Target({PARAMETER, FIELD, METHOD, CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataStructureFactory
{
    /**
     * @return The device plugin class whose data structure factory is to be injected
     */
    @Nonbinding
    Class<? extends IDevicePlugin> forPlugin();
}
