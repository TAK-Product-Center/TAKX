package gov.takx.core.mapobject.gui;

import javax.swing.Icon;
import javax.swing.JLabel;

import java.awt.Graphics;
import java.awt.Image;

/**
 */
public class LayeredLabel extends JLabel
{
    private Image backgroundIcon;

    public void setBackgroundImage(Image background)
    {
        backgroundIcon = background;
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (backgroundIcon != null)
        {
            g.drawImage(backgroundIcon, 0, 5, this);
            super.paintComponent(g);
        }
    }

    @Override
    public void setIcon(Icon icon)
    {
        super.setIcon(icon);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
