package gov.takx.core.navigation;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.lang.ListenerManager;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static gov.takx.core.navigation.NavigationConfiguration.CURRENT_TARGET;
import static gov.takx.core.navigation.NavigationConfiguration.ETA;

/**
 * Controls the color and rate at which navigation lines flash.
 *
 * @since 5.6 (Renamed from BloodhoundFlasher in 5.7)
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class NavigationFlasher implements IResourceCleaner
{
    private static final long FLASH_DURATION_MILLIS = 350;
    private static final String THREAD_NAME = NavigationFlasher.class.getSimpleName();

    @Inject
    private ICoreExecutorService executorService;

    @Inject
    private NavigationManager navigationManager;

    private final ListenerManager listenerManager = new ListenerManager();
    private final NavigationConfiguration navigation;

    private long flashDelaySec;
    private NavigationTarget flashedTarget;
    private volatile boolean running;

    /**
     * Construct a new flasher to flash line colors for a given navigation.
     *
     * @param navigation the navigation object
     */
    public NavigationFlasher(@Nonnull NavigationConfiguration navigation)
    {
        this.navigation = navigation;
        listenerManager.addPropertyChangeListener(navigation, List.of(ETA, CURRENT_TARGET), ignored -> update());
    }

    @Override
    public void releaseResources()
    {
        listenerManager.unregisterAll();
        running = false;
    }

    synchronized void update()
    {
        var wasRunning = running;
        running = navigationManager.isFlashEnabled() && navigation.isFlashEnabled();
        if (running)
        {
            flashDelaySec = navigationManager.getFlashDelay(navigation.getEtaSec());
            running = flashDelaySec > 0;
            if (!wasRunning && running) executorService.execute(this::flash);
        }
    }

    private void flash()
    {
        Thread.currentThread().setName(THREAD_NAME);
        if (running)
        {
            flashedTarget = navigation.getCurrentTarget();

            flashedTarget.setLineColor(navigationManager.getFlashColor());
            executorService.schedule(this::unflash, FLASH_DURATION_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    private void unflash()
    {
        Thread.currentThread().setName(THREAD_NAME);
        flashedTarget.setLineColor(null);
        if (running) executorService.schedule(this::flash, flashDelaySec, TimeUnit.SECONDS);
    }
}
