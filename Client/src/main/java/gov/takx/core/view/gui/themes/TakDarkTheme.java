package gov.takx.core.view.gui.themes;

import com.formdev.flatlaf.FlatDarkLaf;

/**
 * Customized version of the dark theme for FlatLAF. All of the details of the theme are managed by the properties file.
 *
 * @since 3.3
 * @see <a href="https://www.formdev.com/flatlaf/theme-editor/">FlatLAF theme editor</a> for instructions on modifying themes.
 * @see <a href="https://www.formdev.com/flatlaf/how-to-customize/#new_theme">FlatLAF instructions for using a new theme</a>
 */
public class TakDarkTheme extends FlatDarkLaf
{
    public static final String NAME = "TAK Dark";

    public static boolean setup()
    {
        return setup(new TakDarkTheme());
    }

    public static void installLafInfo()
    {
        installLafInfo(NAME, TakDarkTheme.class);
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getDescription()
    {
        return "TAK Dark Look and Feel";
    }
}
