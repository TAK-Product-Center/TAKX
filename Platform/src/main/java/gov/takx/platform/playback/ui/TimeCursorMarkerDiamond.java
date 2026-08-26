package gov.takx.platform.playback.ui;

import gov.takx.commons.swing.ImageUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;

/**
 * A cursor that renders as a diamond.
 *
 * @since 1.47
 */
public class TimeCursorMarkerDiamond extends TimeCursorLine
{
    private static final int SIZE = 5;

    private Paint gradientPaint;
    private final Stroke pointerStroke = new BasicStroke(3);
    private final Polygon diamond = new Polygon();

    public TimeCursorMarkerDiamond(Color color, String toolTip)
    {
        innerLineColor = Color.GRAY;
        setColor(color);
        setToolTipText(toolTip);
    }

    public void setColor(Color color)
    {
        gradientPaint = new GradientPaint(0, -SIZE, color, 0, SIZE, ImageUtils.darker(color, 0.7), true);
    }

    @Override
    public void paintComponents(Graphics g)
    {
        super.paintComponents(g);

        // Draw position pin head (ellipse)
        if (x >= 0)
        {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(pointerStroke);
            g2.setPaint(Color.DARK_GRAY);
            g2.draw(diamond);
            g2.setPaint(gradientPaint);
            g2.fill(diamond);
        }
    }

    @Override
    protected void refresh()
    {
        int center = height / 2;
        diamond.reset();
        diamond.addPoint(x, center - SIZE);
        diamond.addPoint(x - SIZE, center);
        diamond.addPoint(x, center + SIZE);
        diamond.addPoint(x + SIZE, center);

        setBounds(diamond.getBounds());
    }

    /**
     * @param point the point to test
     * @return {@code true} if our diamond contains the given {@code point}
     * @since 5.7
     */
    @Override
    public boolean contains(Point point)
    {
        return diamond.contains(point);
    }
}
