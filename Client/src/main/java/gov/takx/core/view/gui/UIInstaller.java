package gov.takx.core.view.gui;

import com.formdev.flatlaf.FlatLaf;
import gov.takx.core.view.gui.themes.TakDarkTheme;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import java.awt.GraphicsEnvironment;

import static gov.takx.core.view.gui.toolbar.ScrollableToolbarProperties.TOOL_BAR_PLACEMENT_BOTH;
import static gov.takx.core.view.gui.toolbar.ScrollableToolbarProperties.TOOL_BAR_POLICY_AS_NEEDED;
import static gov.takx.core.view.gui.toolbar.ScrollableToolbarProperties.TOOL_BAR_POLICY_NEVER;
import static gov.takx.core.view.gui.toolbar.ScrollableToolbarProperties.TOOL_BAR_SCROLL_BUTTONS_PLACEMENT;
import static gov.takx.core.view.gui.toolbar.ScrollableToolbarProperties.TOOL_BAR_SCROLL_BUTTONS_POLICY;
import static gov.takx.core.view.gui.toolbar.ScrollableToolbarProperties.TOOL_BAR_TOOLS_POPUP_POLICY;

/**
 * This class is responsible for installing all of our UI configurations prior to instantiating any UI components.
 *
 * @since 2.25
 */
public class UIInstaller
{
    /**
     * Install our UI configuration.
     */
    public static void installUI()
    {
        if (GraphicsEnvironment.isHeadless()) return; // Do nothing if there's no actual head

        // use this to use our custom dialog bars
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);

        // We want popups to appear over other heavy-weight components like the globe
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        // For SwingX components, the UIDefaults in the flatlaf-extras jar take precedence over values set in a theme.
        // This means if we set any properties to adjust colors, etc. for SwingX components like JXTaskPane in our
        // theme properties file, they would be ignored. The easiest way to resolve this is to add yet another
        // properties file with the SwingX values and tell FlatLAF to use it as a custom default, which is higher
        // priority than the properties in flatlaf-extras. FlatLAF is smart enough to reference the appropriate
        // properties file in the provided custom defaults source for the specified theme, so we just need to add a
        // <ThemeName>.properties file to this resources directory for each theme we use. See the following method in
        // FlatLAF to understand this ordering: UIDefaultsLoader.loadDefaultsFromProperties()
        final ClassLoader classLoader = UIInstaller.class.getClassLoader();
        FlatLaf.registerCustomDefaultsSource("gov.takx.core.view.gui.customdefaults", classLoader);

        // The following properties add the necessary UI for using an overflow menu on the toolbar.
        final var uiDefaults = UIManager.getDefaults();
        uiDefaults.put("ScrollableToolBarUI", "gov.takx.core.view.gui.toolbar.FlatScrollableToolBarUI");
        uiDefaults.put(TOOL_BAR_SCROLL_BUTTONS_PLACEMENT, TOOL_BAR_PLACEMENT_BOTH);
        uiDefaults.put(TOOL_BAR_SCROLL_BUTTONS_POLICY, TOOL_BAR_POLICY_NEVER);
        uiDefaults.put(TOOL_BAR_TOOLS_POPUP_POLICY, TOOL_BAR_POLICY_AS_NEEDED);

        // Only allow the selection background to cover the tree node label, and not the open/close handle
        uiDefaults.put("Tree.wideSelection", Boolean.FALSE);

        // Install our custom L&F
        FlatLaf.setup(new TakDarkTheme());

        // Now that we have the L&F installed, we can set the class loader for the UI
        UIManager.getLookAndFeelDefaults().put("ClassLoader", classLoader);

        // Install the selected skin, or the default if none specified.
        // We have to load the class directly since Substance doesn't use the ContextClassLoader to load classes.
        // Sigh.
        // TODO KMB: Fix for FlatLAF (TAKX-739)
//        final String skinClassName = UserPreferenceEntry.SKIN.getStringValue().trim();
//        final SubstanceSkin skin = skinClassName.isEmpty()
//                ? new DefaultRaptorSkin()
//                : ClassLoaderUtil.createNewInstance(SubstanceSkin.class, skinClassName);
//
//        SubstanceLookAndFeel.setSkin(skin);
    }
}
