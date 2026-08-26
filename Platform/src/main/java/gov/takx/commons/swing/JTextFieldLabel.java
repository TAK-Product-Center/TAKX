package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;

import javax.swing.JTextField;
import javax.swing.UIManager;

/**
 * A text field styled to look like a label. Essentially allows the user to highlight and copy text from a label component. Useful for displaying
 * data that you suspect the user may want to copy and paste elsewhere.
 *
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
public class JTextFieldLabel extends JTextField
{
    /**
     * Create a new empty component.
     */
    public JTextFieldLabel()
    {
        setBorder(null);
        setEditable(false);
        setOpaque(false);
        setForeground(UIManager.getColor("Label.foreground"));
        setFont(UIManager.getFont("Label.font"));
    }
}
