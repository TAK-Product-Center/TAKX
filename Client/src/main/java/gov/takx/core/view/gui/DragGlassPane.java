package gov.takx.core.view.gui;

import javax.swing.JPanel;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 */
public class DragGlassPane extends JPanel
{
    private BufferedImage dragged = null;
    private Point location = new Point(0, 0);
    private Point oldLocation = new Point(0, 0);

    private int width;
    private int height;

    private final float zoom = 1.0f;
    private final float alpha = 0.6f;

    public DragGlassPane()
    {
        setOpaque(false);
    }

    public void setImage(BufferedImage dragged)
    {
        setImage(dragged, dragged == null ? 0 : dragged.getWidth());
    }

    public void setImage(BufferedImage dragged, int width)
    {
        if (dragged != null)
        {
            float ratio = (float) dragged.getWidth() / (float) dragged.getHeight();
            this.width = width;
            height = (int) (width / ratio);
        }

        this.dragged = dragged;
    }

    public BufferedImage getImage()
    {
        return this.dragged;
    }

    public void setPoint(Point location)
    {
        this.oldLocation = this.location;
        this.location = location;
    }

    public Rectangle getRepaintRect()
    {
        int x = (int) (location.getX() - (width * zoom / 2));
        int y = (int) (location.getY() - (height * zoom / 2));

        int x2 = (int) (oldLocation.getX() - (width * zoom / 2));
        int y2 = (int) (oldLocation.getY() - (height * zoom / 2));

        int width = (int) (this.width * zoom);
        int height = (int) (this.height * zoom);

        return new Rectangle(x, y, width, height).union(new Rectangle(x2, y2, width, height));
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        if (dragged == null || !isVisible()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int x = (int) (location.getX() - (width * zoom / 2));
        int y = (int) (location.getY() - (height * zoom / 2));

        g2.drawImage(dragged, x, y, (int) (width * zoom), (int) (height * zoom), null);
    }
}
