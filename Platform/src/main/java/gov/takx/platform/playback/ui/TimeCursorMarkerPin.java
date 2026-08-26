package gov.takx.platform.playback.ui;

import gov.takx.commons.swing.ImageUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;

/**
 * Marker cursor implementation that renders as a pin (simple ellipse) around the X position.
 *
 * @since 1.47
 */
public class TimeCursorMarkerPin extends TimeCursorLine
{
    public static final int RADIUS = 4;
    public static final int DIAMETER = RADIUS * 2;
    private GradientPaint gradientPaint;
    private final Stroke pointerStroke = new BasicStroke(3);
    private final Ellipse2D pinHead = new Ellipse2D.Double();

    public TimeCursorMarkerPin(Color color)
    {
        innerLineColor = Color.GRAY;
        setColor(color);
    }

    public void setColor(Color color)
    {
        gradientPaint = new GradientPaint(0, 0, color, 0, DIAMETER, ImageUtils.darker(color, 0.7), true);
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
            g2.draw(pinHead);
            g2.setPaint(gradientPaint);
            g2.fill(pinHead);
        }
    }

    @Override
    protected void refresh()
    {
        pinHead.setFrame(x - RADIUS, 0, DIAMETER, DIAMETER);
        setBounds(x - RADIUS, 0, DIAMETER, DIAMETER);
    }

    @Override
    public boolean contains(Point p)
    {
        return pinHead.contains(p);
    }
}
