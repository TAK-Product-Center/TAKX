package gov.takx.core.view.gui.toolbar;

import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.platform.cdi.NoopInvocationHandler;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * Responsible for instantiating the appropriate {@link IToolbar} implementation (depending on whether the application
 * is running headless or with toolbar disabled) and providing it to other classes as needed. Note: the only reason for
 * having this class is because the Toolbar itself cannot be an application-scoped bean as a Swing component.
 *
 * @since 3.3
 */
@ApplicationScoped
public class ToolbarProducer
{
    private IToolbar toolbar;

    @Inject
    public ToolbarProducer(@Headless boolean headless, @ProgramPreference(PreferenceEntry.SIMPLIFIED_USER_INTERFACE) boolean simplifiedUI)
    {
        if (!headless && !simplifiedUI)
        {
            initToolbar();
        } else
        {
            toolbar = NoopInvocationHandler.forType(IToolbar.class);
        }
    }

    @InvokeAndWaitOnEDT // Using invoke and wait to ensure toolbar gets initialized before it can be provided via produce method
    private void initToolbar()
    {
        toolbar = new Toolbar();
    }

    @Nonnull
    @Produces
    public IToolbar produceToolbar()
    {
        return toolbar;
    }
}
