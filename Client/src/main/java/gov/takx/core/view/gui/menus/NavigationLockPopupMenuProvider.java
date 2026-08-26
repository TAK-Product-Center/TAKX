package gov.takx.core.view.gui.menus;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.commons.swing.IPopupMenuProvider;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;

/**
 * Producer of the popup menu for the Navigation lock button.
 *
 * @since 3.3 (significantly refactored and renamed from NavigationLockPopupCallback)
 */
@PseudoBean
public class NavigationLockPopupMenuProvider implements IPopupMenuProvider
{
    private final JCheckBoxMenuItem mouseClickLock = new JCheckBoxMenuItem("Center view on click");
    private final JCheckBoxMenuItem mouseNavLock = new JCheckBoxMenuItem("Allow mouse navigation");
    private final JCheckBoxMenuItem keyboardLock = new JCheckBoxMenuItem("Allow keyboard navigation");
    private final JCheckBoxMenuItem totalLock = new JCheckBoxMenuItem("Lock all navigation");

    private JPopupMenu popupMenu;
    private GlobeNavigationLockState state;

    @Inject
    protected IExtendedGlobeManager globeManager;

    /**
     * Determine the initial lock state so we properly initialize the menu buttons.
     *
     * @return nav lock state matching the values pulled from preferences
     * @since 3.0
     */
    @Nonnull
    private GlobeNavigationLockState findInitialState()
    {
        return GlobeNavigationLockState.findState(!PreferenceEntry.GLOBE_DISABLE_CENTER_GLOBE_ON_CLICK.getBooleanValue(),
                !PreferenceEntry.GLOBE_DISABLE_MOUSE_GLOBE_NAVIGATION.getBooleanValue(),
                !PreferenceEntry.GLOBE_DISABLE_KEYBOARD_GLOBE_NAVIGATION.getBooleanValue());
    }

    @InvokeOnEDT
    private void enterState(@Nonnull GlobeNavigationLockState newState)
    {
        state = newState;

        globeManager.doWhenGlobeReady(globeDelegate -> {
            final IApplicationGlobe globe = (IApplicationGlobe) globeDelegate;
            globe.setDisableCenterGlobeOnClick(!state.mouseClickEnabled);
            globe.setMouseNavLock(!state.mouseNavEnabled);
            globe.setKeyboardNavLock(!state.keyboardEnabled);
        });

        mouseClickLock.setSelected(state.mouseClickEnabled);
        mouseNavLock.setSelected(state.mouseNavEnabled);
        keyboardLock.setSelected(state.keyboardEnabled);
        totalLock.setSelected(state.totalLock);
    }

    void toggleTotalNavigationLock()
    {
        enterState(state.toggleTotalLock());
    }

    @Nonnull
    @Override
    public synchronized JPopupMenu getPopupMenu()
    {
        if (popupMenu == null)
        {
            popupMenu = new JPopupMenu();

            mouseClickLock.addActionListener(e -> enterState(state.toggleMouseClick()));
            mouseNavLock.addActionListener(e -> enterState(state.toggleMouseNav()));
            keyboardLock.addActionListener(e -> enterState(state.toggleKeyboard()));
            totalLock.addActionListener(e -> enterState(state.toggleTotalLock()));

            popupMenu.add(mouseClickLock);
            popupMenu.add(mouseNavLock);
            popupMenu.add(keyboardLock);
            popupMenu.add(totalLock);

            enterState(findInitialState());
        }
        return popupMenu;
    }
}
