package gov.takx.core.view.gui.docking;

import bibliothek.gui.dock.common.intern.CDockable;
import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.UserPreferenceType;
import gov.takx.api.preferences.PreferenceEntryChange;
import gov.takx.api.ui.docking.DockState;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Component;
import java.lang.invoke.MethodHandles;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service to handle docking state notifications (flashing the dock title color).
 *
 * @since 2.25
 */
@ApplicationScoped
public class DockingStateNotifier
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final float MAX_RGB = 255f;
    private static final int NUM_COLOR_SAMPLES = 25;
    private static final float OPACITY_OFFSET = 30;

    @Inject
    private DockingManager dockingManager;

    @Inject
    @Headless
    private boolean headless;

    private volatile boolean notificationsEnabled = false;

    private final Map<CDockable, Color[]> minimizedDockFlashColorMap = new HashMap<>();
    private final Map<DockState, Color[]> stateColorsMap = new EnumMap<>(DockState.class);

    @PostConstruct
    private void postConstruct()
    {
        if (!headless)
        {
            stateColorsMap.put(DockState.Default, getColors(Color.BLACK));
            stateColorsMap.put(DockState.Active, getColors(Color.CYAN));
            stateColorsMap.put(DockState.Warning, getColors(Color.YELLOW));
            stateColorsMap.put(DockState.Critical, getColors(Color.RED));

            setupUI();

            notificationsEnabled = UserPreferenceEntry.DOCK_BUTTON_NOTIFICATIONS.getBooleanValue();

            startCriticalButtonFlasher();
        }
    }

    @InvokeOnEDT
    private void setupUI()
    {
        // TODO KMB: Fix for FlatLaf (TAKX-739)
//        SubstanceColorScheme substanceColorScheme = SubstanceColorSchemeUtilities.getColorScheme(new JButton(),
//                ColorSchemeAssociationKind.FILL, ComponentState.ENABLED);
//
//        stateColorsMap.put(DockState.Default, getColors(substanceColorScheme.getBackgroundFillColor()));
    }

    private void startCriticalButtonFlasher()
    {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try
            {
                if (notificationsEnabled && !minimizedDockFlashColorMap.isEmpty())
                {
                    for (int index = 0; index < NUM_COLOR_SAMPLES; index += 1)
                    {
                        //This is required because lambdas require effectively final variables
                        int finalIndex = index;
                        minimizedDockFlashColorMap.forEach((dockable, colors) -> dockable.getColors()
                                .setColor("minimized.background", colors[finalIndex]));

                        Thread.sleep(100);
                    }
                }
            } catch (Exception exception)
            {
                logger.warn("Failed to flash dockable button backgrounds: ", exception);
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * Set the state of the dockable with the given descriptor Id.
     * @param component The component to update
     * @param dockState New state
     */
    public void setDockableState(Component component, DockState dockState)
    {
        final DockableAndProps dockableAndProps = dockingManager.findDockableAndProps(component);
        if (dockableAndProps == null || !dockableAndProps.hasDockable()) return;

        final ComponentDockable dockable = dockableAndProps.requireDockable();
        setDockableState(dockable, dockState);
    }

    private void setDockableState(@Nonnull CDockable dockable, DockState dockState)
    {
        if (!notificationsEnabled) return;

        final Color[] colors = stateColorsMap.get(dockState);

        if (dockState == DockState.Critical)
        {
            minimizedDockFlashColorMap.put(dockable, colors);
        } else
        {
            minimizedDockFlashColorMap.remove(dockable);
            dockable.getColors().setColor("minimized.background", colors[colors.length / 2]);
        }
    }

    /**
     * Remove the given dockable.
     *
     * @param dockable the DockableAndProps object
     */
    public void removeDockable(ComponentDockable dockable)
    {
        minimizedDockFlashColorMap.remove(dockable);
    }

    /**
     * If the {@link UserPreferenceEntry#DOCK_BUTTON_NOTIFICATIONS} preference changes, then update accordingly.
     *
     * @param change Preference entry change event
     */
    private void handleNotificationChange(@Observes @UserPreferenceType(UserPreferenceEntry.DOCK_BUTTON_NOTIFICATIONS) PreferenceEntryChange change)
    {
        notificationsEnabled = change.getBooleanValue();

        if (!notificationsEnabled)
        {
            dockingManager.findDockableAndPropsWithDockable()
                    .forEach(dap -> setDockableState(dap.requireDockable(), DockState.Default));
        }
    }

    /**
     * Creates an opacity gradient arranged from low opacity, to high opacity, and then back to low opacity.
     *
     * @param color The color that will be decomposed into an opacity gradient.
     * @return An opacity gradient arranged from low opacity, to high opacity, and then back to low opacity.
     */
    private Color[] getColors(Color color)
    {
        final float red = color.getRed() / MAX_RGB;
        final float green = color.getGreen() / MAX_RGB;
        final float blue = color.getBlue() / MAX_RGB;

        final Color[] colors = new Color[NUM_COLOR_SAMPLES];

        for (int index = 0; index < NUM_COLOR_SAMPLES; index++)
        {
            final float alpha = index < NUM_COLOR_SAMPLES / 2
                    ? index / OPACITY_OFFSET // Increasing opacity
                    : (NUM_COLOR_SAMPLES - index) / OPACITY_OFFSET; // Decreasing opacity

            colors[index] = new Color(red, green, blue, alpha);
        }

        return colors;
    }
}
