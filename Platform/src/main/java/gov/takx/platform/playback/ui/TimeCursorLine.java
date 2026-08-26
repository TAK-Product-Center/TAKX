package gov.takx.platform.playback.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

/**
 * A time cursor that draws a simple vertical line at the current X position.
 *
 * @since 1.47
 */
public class TimeCursorLine extends ATimeCursor
{
    protected Stroke lineStroke = new BasicStroke(3);
    protected Stroke innerLineStroke = new BasicStroke(2);
    protected Color positionLineColor = Color.DARK_GRAY;
    protected Color innerLineColor = Color.YELLOW;

    public TimeCursorLine()
    {
    }

    public TimeCursorLine(Color color)
    {
        innerLineColor = color;
    }

    @Override
    public void paintComponents(Graphics g)
    {
        super.paintComponents(g);
        if (x > -1)
        {
            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(lineStroke);
            g2.setColor(positionLineColor);
            g2.drawLine(x, 0, x, height);
            g2.setStroke(innerLineStroke);
            g2.setColor(innerLineColor);
            g2.drawLine(x, 0, x, height);
        }
    }

    @Override
    protected void refresh()
    {
        setBounds(x, 0, 2, height);
    }
}
