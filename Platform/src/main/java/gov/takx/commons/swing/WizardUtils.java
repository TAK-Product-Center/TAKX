package gov.takx.commons.swing;

/**
 */
public final class WizardUtils
{
    /**
     * This class is not intended to be instantiated, it only provides static methods.  This constructor
     * enforces that (and quiets PMD).
     */
    private WizardUtils()
    {
    }

    public static String formatNotificationText(String text)
    {
        return "<html><font color=\"white\" size = \"4\">" + text + "</font><br>";
    }

    public static boolean isFormattedText(String text)
    {
        return text != null && text.contains("<html><font color=\"white\" size = \"4\">");
    }
}
