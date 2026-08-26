package gov.takx.platform.playback.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;

/**
 * A simple cursor that renders a triangle pointing downward, centered at the X position and half as tall as the
 * parent component's height followed by a line painted from the tip of the triangle to the bottom of the panel.
 *
 * @since 1.47
 */
public class TimeCursorKnob extends TimeCursorLine
{
    private static final int HALF_SIZE = 10;
    private static final int SIZE = HALF_SIZE * 2;
    private Stroke pointerStroke = new BasicStroke(2);
    private Polygon pointer = new Polygon();

    public TimeCursorKnob()
    {
    }

    @Override
    public void paintComponents(Graphics g)
    {
        super.paintComponents(g);

        // Draw position knob (triangle)
        if (x >= 0)
        {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(pointerStroke);
            g2.setPaint(Color.YELLOW);
            g2.draw(pointer);
            g2.setPaint(Color.YELLOW);
            g2.fill(pointer);
        }
    }

    @Override
    protected void refresh()
    {
        pointer.reset();
        pointer.addPoint(x - HALF_SIZE, 0);
        pointer.addPoint(x + HALF_SIZE, 0);
        pointer.addPoint(x, height / 2);

        setBounds(x - HALF_SIZE, 0, SIZE, height / 2);
    }
}
