package gov.takx.platform.logging;

import gov.takx.api.annotation.InjectableService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.slf4j.Logger;

/**
 * Producer of Logger instances.  Just @inject a Logger and this class will produce the correct logger instance
 * for the consuming class.
 *
 * @since 2.0.0
 */
@SuppressWarnings("unused")
@InjectableService
@ApplicationScoped
public class LoggerFactory
{
    /**
     * Produce a logger for the class into which the value is being injected.
     *
     * @param injectionPoint To determine the declaring class of the injection point
     * @return Logger for the class being injected
     * @since 3.4 (parameter changed to Jakarta InjectionPoint)
     */
    @Produces
    public Logger createLogger(InjectionPoint injectionPoint)
    {
        return org.slf4j.LoggerFactory.getLogger(injectionPoint.getMember().getDeclaringClass());
    }
}
