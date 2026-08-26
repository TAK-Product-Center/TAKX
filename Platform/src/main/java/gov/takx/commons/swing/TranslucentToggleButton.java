package gov.takx.commons.swing;

import javax.swing.Icon;
import javax.swing.JToggleButton;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;

// TODO KMB: Need to check if any plugins are using this. If not, we can get rid of it and just use the client
//  property for borderless buttons
/**
 * A JToggleButton showing an icon or text with no background
 *
 */
public class TranslucentToggleButton extends JToggleButton
{
    /**
     * Create a toggle button with specified icons
     *
     * @param onIcon  Icon to display when enabled
     * @param offIcon Icon to display when disabled
     */
    public TranslucentToggleButton(Icon onIcon, Icon offIcon)
    {
        super(offIcon);
        setSelectedIcon(onIcon);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
    }

    @Override
    public void paint(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        super.paint(g2);
        g2.dispose();
    }
}
