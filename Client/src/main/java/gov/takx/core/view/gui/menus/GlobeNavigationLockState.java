package gov.takx.core.view.gui.menus;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Enumeration of valid navigation lock settings.
 *
 * @since 2.37 (extracted from ViewToolbarTaskWrapper)
 */
public enum GlobeNavigationLockState
{
    AllowAll(true, true, true),
    AllowAllExceptMouseClick(false, true, true),
    AllowMouse(true, true, false),
    AllowMouseExceptClick(false, true, false),
    AllowKeyboard(false, false, true),
    AllowNone(false, false, false);

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final GlobeNavigationLockState[] VALUES = values();

    public final boolean mouseClickEnabled;
    public final boolean mouseNavEnabled;
    public final boolean keyboardEnabled;
    public final boolean totalLock;

    GlobeNavigationLockState(boolean mouseClickEnabled, boolean mouseNavEnabled, boolean keyboardEnabled)
    {
        this.mouseClickEnabled = mouseClickEnabled;
        this.mouseNavEnabled = mouseNavEnabled;
        this.keyboardEnabled = keyboardEnabled;

        totalLock = !mouseClickEnabled && !mouseNavEnabled && !keyboardEnabled;
    }

    @Nonnull
    public GlobeNavigationLockState toggleMouseClick()
    {
        boolean newClick = !mouseClickEnabled;
        return findState(newClick, mouseNavEnabled || newClick, keyboardEnabled);
    }

    @Nonnull
    public GlobeNavigationLockState toggleMouseNav()
    {
        boolean newNav = !mouseNavEnabled;
        return findState(mouseClickEnabled && newNav, newNav, keyboardEnabled);
    }

    @Nonnull
    public GlobeNavigationLockState toggleKeyboard()
    {
        return findState(mouseClickEnabled, mouseNavEnabled, !keyboardEnabled);
    }

    @Nonnull
    public GlobeNavigationLockState toggleTotalLock()
    {
        return totalLock ? AllowAll : AllowNone;
    }

    /**
     * Find the state that has the given settings.
     *
     * @return Matching state, or {@link #AllowAll} if none match
     * @since 3.0
     */
    @Nonnull
    static GlobeNavigationLockState findState(boolean mouseClickEnabled, boolean mouseNavEnabled, boolean keyboardEnabled)
    {
        for (GlobeNavigationLockState state : VALUES)
        {
            if (state.mouseClickEnabled == mouseClickEnabled
                    && state.mouseNavEnabled == mouseNavEnabled
                    && state.keyboardEnabled == keyboardEnabled)
            {
                return state;
            }
        }

        logger.error("No matching state: click = {}, mouse = {}, keyboard = {}", mouseClickEnabled, mouseNavEnabled, keyboardEnabled);
        return AllowAll;
    }
}
