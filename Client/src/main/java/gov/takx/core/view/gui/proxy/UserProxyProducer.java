package gov.takx.core.view.gui.proxy;

import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.ui.IUserProxy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * This class is responsible for producing the correct user proxy based on our configuration.
 *
 * @since 2.0.0
 */
@SuppressWarnings("unused")
@InjectableService
@ApplicationScoped
public class UserProxyProducer
{
    @Inject
    @Headless
    private boolean headless;

    @Inject
    private SwingUserProxy swingUserProxy;

    @Inject
    private HeadlessUserProxy headlessUserProxy;

    /**
     * @return Swing or Headless proxy depending on {@link IProgramEnvironment#isHeadless()}
     */
    @Produces
    private IUserProxy produceUserProxy()
    {
        return headless
                ? headlessUserProxy
                : swingUserProxy;
    }
}
