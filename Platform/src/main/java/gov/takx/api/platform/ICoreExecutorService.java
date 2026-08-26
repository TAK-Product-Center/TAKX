package gov.takx.api.platform;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Interface defining the core executor service.  This is just a tag interface to make injection targets
 * easier to distinguish.
 *
 * @since 2.0.0
 */
@InjectableService
@NoPluginImplementations
public interface ICoreExecutorService extends ScheduledExecutorService
{
}
