package gov.takx.core.view.gui.docking;

import bibliothek.gui.dock.displayer.DisplayerDockBorder;
import bibliothek.gui.dock.themes.ThemeManager;
import bibliothek.gui.dock.themes.border.BorderModifier;
import bibliothek.gui.dock.util.Priority;
import bibliothek.gui.dock.util.UIBridge;
import bibliothek.gui.dock.util.UIValue;
import gov.takx.api.playback.IPlaybackManager;
import gov.takx.api.playback.PlaybackLiveEvent;
import gov.takx.platform.ui.ComponentDescriptorBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import javax.swing.border.MatteBorder;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

/**
 * A UI Bridge to dynamically install border modifiers to indicate that playback is active or not.  When playback
 * is active, we render a green border around all panels that are <em>playback aware</em>.  Panels are playback aware
 * if they declare it via the component descriptor using
 * {@link ComponentDescriptorBuilder#setPlaybackAware(boolean)}, or if it is a globe panel
 * (as all globes are implicitly playback aware).
 *
 * @since 2.25
 */
@ApplicationScoped
public class PlaybackBorderModifierBridge implements UIBridge<BorderModifier, UIValue<BorderModifier>>
{
    // This is the shared border to use for playback aware panels when playback is active
    private static final MatteBorder GREEN_BORDER = new MatteBorder(2, 2, 2, 2, Color.GREEN);

    @Inject
    protected IPlaybackManager playbackManager;

    private final BorderModifier playbackModifier = border -> GREEN_BORDER; // Substitute playback border
    private final BorderModifier noopModifier = border -> border; // No modification in live mode

    // Collection of targets (UI components that are playback aware)
    private final Set<UIValue<BorderModifier>> targets = new HashSet<>();

    /**
     * Register this bridge with the given theme manager.
     *
     * @param themeManager Theme manager on which to register
     */
    public void registerBridge(ThemeManager themeManager)
    {
        themeManager.publish(Priority.CLIENT, DisplayerDockBorder.KIND, ThemeManager.BORDER_MODIFIER_TYPE, this);
    }

    /**
     * Monitor playback live events to dynamically update our border handling.
     */
    private void updateForPlaybackState(@Observes PlaybackLiveEvent event)
    {
        final BorderModifier modifier = event.isLive() ? noopModifier : playbackModifier;

        for (UIValue<BorderModifier> border : targets)
        {
            border.set(modifier);
        }
    }

    /**
     * Add a new component, track it if it is playback aware.
     *
     * @param id      Id of resource, ignored
     * @param uiValue The component listening for border modifier updates
     */
    @Override
    public void add(String id, UIValue<BorderModifier> uiValue)
    {
        /*
         * RAPTORX-564 - The panels we show in TAKX's docking framework are wrapped in a "DisplayerContentPane". The
         * "DisplayerContentPane" is used exclusively by the "BasicDockableDisplayer". The "BasicDockableDisplayer"
         * has two border strategies that are responsible for coloring around the entire panel
         * (including outside the tab handle) and the content within the panel. The "content" key was used as a filter
         * since this key is unique suffix that distinguishes between the "base" and "content" borders. Removing this
         * filter would cause both borders to be colored.
         */
        if (id.contains("content"))
        {
            final boolean playbackAware = isPlaybackAware(uiValue);
            if (playbackAware)
            {
                targets.add(uiValue);

                // If we're already in playback mode the first time we get this panel, then immediately set the border
                if (!playbackManager.isLive()) uiValue.set(playbackModifier);
            }
        }
    }

    /**
     * Remove the component from our tracked set of targets.
     *
     * @param id      ignored
     * @param uiValue component to remove
     */
    @Override
    public void remove(String id, UIValue<BorderModifier> uiValue)
    {
        targets.remove(uiValue);
    }

    @Override
    public void set(String id, BorderModifier value, UIValue<BorderModifier> uiValue)
    {
        if (!isPlaybackAware(uiValue))
        {
            uiValue.set(value);
        }
    }

    /**
     * Determine if the given component is playback aware or not.
     *
     * @param uiValue Component to inspect
     * @return {@code true} if it is playback aware
     */
    private boolean isPlaybackAware(UIValue<BorderModifier> uiValue)
    {
        // This cast is safe since we registered for only 'DisplayerDockBorder' KIND of objects above
        return uiValue != null && ((DisplayerDockBorder) uiValue).getDisplayer().getDockable() instanceof PlaybackAwareDockable;
    }
}
