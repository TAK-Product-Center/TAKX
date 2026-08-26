package gov.takx.core.view.gui.menus;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobeNavigationLockStateTest
{
    @Test
    public void transitionTests()
    {
        // test that every "toggle" transition for every state leads to a state with the
        // specified field negated.

        for (GlobeNavigationLockState state : GlobeNavigationLockState.values())
        {
            GlobeNavigationLockState toggledMouseClick = state.toggleMouseClick();
            assertThat(toggledMouseClick.mouseClickEnabled).isEqualTo(!state.mouseClickEnabled);

            GlobeNavigationLockState toggledMouseNav = state.toggleMouseNav();
            assertThat(toggledMouseNav.mouseNavEnabled).isEqualTo(!state.mouseNavEnabled);

            GlobeNavigationLockState toggledKeyboard = state.toggleKeyboard();
            assertThat(toggledKeyboard.keyboardEnabled).isEqualTo(!state.keyboardEnabled);

            GlobeNavigationLockState toggledAll = state.toggleTotalLock();
            assertThat(toggledAll.totalLock).isEqualTo(!state.totalLock);
        }
    }

    @Test
    public void consistencyTests()
    {
        // test that every state passes basic consistency tests

        for (GlobeNavigationLockState state : GlobeNavigationLockState.values())
        {
            if (state.mouseClickEnabled)
            {
                assertThat(state.mouseNavEnabled).isTrue();
            }

            if (state.totalLock)
            {
                assertThat(state.mouseClickEnabled).isFalse();
                assertThat(state.mouseNavEnabled).isFalse();
                assertThat(state.keyboardEnabled).isFalse();
            } else
            {
                assertThat(state.mouseClickEnabled || state.mouseNavEnabled || state.keyboardEnabled).isTrue();
            }
        }
    }
}
