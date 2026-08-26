package gov.takx.core.playback.ui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.playback.IMarker;
import gov.takx.api.playback.ITimeSpan;
import gov.takx.api.playback.ui.IPlaybackStage;
import gov.takx.platform.playback.TimelineUtils;
import gov.takx.platform.playback.ui.ATimeCursor;
import gov.takx.platform.playback.ui.TimeCursorKnob;
import gov.takx.platform.playback.ui.TimeCursorLine;
import gov.takx.platform.playback.ui.TimePanel;
import gov.takx.platform.ui.UIResources;

import javax.swing.JPanel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.TimeUnit;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * A timeline representation of a time span using a ruler visual metaphor.  Time is represented as large or small
 * tick marks along the ruler.  The time between marks is dynamic depending on the total time encompassed by
 * the {@link ITimeSpan}.
 * <p/>
 * This component supports user interaction to zoom to a subsection of the time span by holding down the
 * Shift key and dragging the mouse within the ruler to sweep off the desired subsection.
 *
 * @since 1.47
 */
public class RulerPanel extends JPanel
{
    private static final Color SELECTION_COLOR = new Color(136, 159, 232);
    private static final GradientPaint selectionPaint = new GradientPaint(
            0, 0, SELECTION_COLOR,
            0, 25, SELECTION_COLOR.darker(),
            true);

    private final RulerTickPanel rulerTickPanel;
    private final RulerLabelPanel rulerLabelPanel;

    private final IPlaybackStage playbackStage;
    private final ITimeSpan timeSpan;
    private final TickModel tickModel;

    protected Integer selectStart;
    protected Integer selectEnd;

    /**
     * Construct the ruler panel.
     *
     * @param playbackStage The playback stage we are part of
     * @param timeSpan      The time span defining the basis for the ruler
     */
    public RulerPanel(IPlaybackStage playbackStage, ITimeSpan timeSpan)
    {
        this.playbackStage = playbackStage;

        initComponents();

        rulerTickPanel = new RulerTickPanel(timeSpan);
        rulerLabelPanel = new RulerLabelPanel(timeSpan);
        tickPane.add(rulerTickPanel, new CellConstraints(1, 1));
        timeLabelPane.add(rulerLabelPanel, new CellConstraints(1, 1));
        this.timeSpan = timeSpan;
        this.timeSpan.setVisualComponent(this);
        tickModel = new TickModel(timeSpan);
        tickModel.setMajorTickUnit(TimeUnit.SECONDS);

        addMouseAdapter(new MouseDragHandler());
    }

    /**
     * Add a mouse adapter to the sub-panels (the ticks and labels).
     *
     * @param mouseAdapter Adapter to add
     */
    public void addMouseAdapter(MouseAdapter mouseAdapter)
    {
        rulerTickPanel.addMouseListener(mouseAdapter);
        rulerTickPanel.addMouseMotionListener(mouseAdapter);
        rulerLabelPanel.addMouseListener(mouseAdapter);
        rulerLabelPanel.addMouseMotionListener(mouseAdapter);
    }

    private int tickLength(double percent)
    {
        return (int) (percent * tickPane.getHeight());
    }

    private int boundToWidth(int v)
    {
        if (v < 0)
        {
            return 0;
        }
        if (v >= getWidth())
        {
            return getWidth() - 1;
        }
        return v;
    }

    /**
     * Panel to render the tick marks on the time ruler.
     */
    private class RulerTickPanel extends TimePanel
    {
        private static final double MAJOR_TICK_PERCENT_OF_HEIGHT = 0.90;
        private static final double MINOR_TICK_PERCENT_OF_HEIGHT = 0.5;
        private static final Stroke MAJOR_STROKE = new BasicStroke(2);
        private static final Stroke MINOR_STROKE = new BasicStroke(1);

        private int majorTickLength;
        private int minorTickLength;

        public RulerTickPanel(ITimeSpan timeSpan)
        {
            super(new TimeCursorKnob(), timeSpan);
            setLayout(null);
            setBackground(Color.WHITE);
        }

        @Override
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            // If the user is selecting a region, then render it
            if (selectStart != null && selectEnd != null)
            {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(selectionPaint);
                g2.fillRect(Math.min(selectStart, selectEnd), 0, Math.abs(selectEnd - selectStart), getHeight());
            }
        }

        @Override
        public void preLinePaint(Graphics2D g)
        {
            final Stroke originalStroke = g.getStroke();

            // Draw the major ticks

            g.setPaint(Color.BLACK);
            g.setStroke(MAJOR_STROKE);
            for (int x : tickModel.getMajorTicks())
            {
                g.drawLine(x, 0, x, majorTickLength);
            }

            // Draw the minor ticks

            g.setPaint(Color.DARK_GRAY);
            g.setStroke(MINOR_STROKE);
            for (int x : tickModel.getMinorTicks())
            {
                g.drawLine(x, 0, x, minorTickLength);
            }

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw timeline start triangle (upper left-hand corner of the ruler).
            int size = getHeight() / 2;
            int width = getWidth();
            g.setPaint(Color.LIGHT_GRAY);
            g.setStroke(MAJOR_STROKE);
            g.drawLine(1, 1, 1, size);
            g.drawLine(1, 1, size, 1);
            g.drawLine(1, size - 1, size, 1);

            // Draw timeline end triangle (upper right-hand corner of the ruler).
            g.drawLine(width - 2, 1, width - 2, size);
            g.drawLine(width - 2, 1, width - 2 - size, 1);
            g.drawLine(width - 2 - size, 1, width - 2, size);

            g.setStroke(originalStroke);

            for (IMarker marker : timeSpan.getMarkers())
            {
                if (marker.hasMoment() && marker.isVisible())
                {
                    renderCursor(marker.getRulerCursor(), g);
                }
            }
        }

        @Override
        public void timeSpanChanged()
        {
            if (isShowing())
            {
                tickModel.updateSpacings();
                majorTickLength = tickLength(MAJOR_TICK_PERCENT_OF_HEIGHT);
                minorTickLength = tickLength(MINOR_TICK_PERCENT_OF_HEIGHT);
                setToolTipText(TimelineUtils.getDateRangeString(timeSpan.getTimeBoundary()));
                repaint();
            }
        }
    }

    /**
     * Time panel to render the time labels along the ruler.
     */
    private class RulerLabelPanel extends TimePanel
    {
        private static final int TEXT_Y_OFFSET = 12;
        private static final int Y_HEIGHT = 15;
        private static final Color LIGHTER_GRAY = new Color(200, 215, 225);
        private final GradientPaint backgroundPaint = new GradientPaint(0, 0, Color.GRAY, 0, Y_HEIGHT, Color.DARK_GRAY, true);
        private final ATimeCursor markerCursor = new TimeCursorLine(Color.GRAY);

        public RulerLabelPanel(ITimeSpan timeSpan)
        {
            super(timeSpan);

            setLayout(null);
            setBackground(Color.DARK_GRAY);
            setForeground(Color.WHITE);
        }

        @Override
        public void paintComponent(Graphics g)
        {
            ((Graphics2D) g).setPaint(backgroundPaint);
            g.fillRect(0, 0, getWidth(), getHeight());
            if (selectStart != null && selectEnd != null)
            {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(selectionPaint);
                g2.fillRect(Math.min(selectStart, selectEnd), 0, Math.abs(selectEnd - selectStart), getHeight());
            }
        }

        @Override
        public void preLinePaint(Graphics2D g)
        {
            final Color originalColor = g.getColor();

            int count = 0;
            for (int x : tickModel.getMajorTicks())
            {
                long time = timeSpan.xToTime(x);
                String timeString;
                long roundedToSecond = TimelineUtils.roundSecond(time);
                long duration = timeSpan.getTotalTime();

                if (duration < TimelineUtils.DAY)
                {
                    timeString = TimelineUtils.getSecondString(roundedToSecond);
                } else if (duration < TimelineUtils.YEAR)
                {
                    timeString = TimelineUtils.getShortDateString(roundedToSecond);
                } else
                {
                    timeString = TimelineUtils.getShorterDateString(roundedToSecond);
                }

                Color currentTextColor = count % 2 == 0 ? LIGHTER_GRAY : originalColor;
                g.setColor(currentTextColor);
                g.drawString(timeString, x, TEXT_Y_OFFSET);
                count++;
            }

            g.setColor(originalColor); // Reset

            // Now render any markers that have a moment in time defined
            for (IMarker marker : timeSpan.getMarkers())
            {
                if (marker.hasMoment() && marker.isVisible())
                {
                    renderCursor(markerCursor, g, (int) timeSpan.timeToX(marker.getMoment()));
                }
            }
        }
    }

    /**
     * Mouse handler to invoke a dragging and "scale in" operations on the timespan.
     * <ul>
     *     <li>Using {@code Shift+drag} to sweep off a region, the system will scale the timeline to just that region</li>
     *     <li>Using {@code Ctrl+drag} will drag/move the timeline with the mouse</li>
     * </ul>
     */
    private final class MouseDragHandler extends MouseAdapter
    {
        private final Cursor defaultCursor = getCursor();
        private final Cursor scaleInCursor;
        private boolean active;
        private boolean scaling;
        private Integer lastXPos;

        MouseDragHandler()
        {
            final var cursorImage = UIResources.getImage(PLATFORM_ICONS, "playback/scale_in_cursor.png");
            scaleInCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImage, new Point(8, 8), "scaleInCursor");
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
            if (e.isConsumed()) return;

            if (e.isShiftDown())
            {
                setCursor(scaleInCursor);
                selectStart = selectEnd = e.getX();
                active = scaling = true;
                repaint();

                e.consume();
            } else if (e.isControlDown())
            {
                lastXPos = e.getX();
                active = true;
                e.consume();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            if (scaling)
            {
                selectEnd = e.getX();
                e.consume();
            } else if (lastXPos != null)
            {
                timeSpan.panTimeByXOffset(-(e.getX() - lastXPos));
                lastXPos = e.getX();
                e.consume();
            }
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            // If either select positions have been set, they need to be reset
            // (and possibly applied to the time span)
            if (scaling || selectStart != null || selectEnd != null)
            {
                setCursor(defaultCursor);

                // Update the time span duration only if both selection positions have been set
                if (selectStart != null && selectEnd != null)
                {
                    selectStart = boundToWidth(selectStart);
                    selectEnd = boundToWidth(selectEnd);
                    if (Math.abs(selectEnd - selectStart) > 0)
                    {
                        playbackStage.resetZoom();

                        final int minX = Math.min(selectStart, selectEnd);
                        final int maxX = Math.max(selectStart, selectEnd);

                        timeSpan.setTimes(timeSpan.xToTime(minX), timeSpan.xToTime(maxX));
                    }
                }
                selectStart = selectEnd = null;
                e.consume();
            }

            if (active) e.consume();

            active = scaling = false;
            lastXPos = null;
            repaint();
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        tickPane = new JPanel();
        timeLabelPane = new JPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setMaximumSize(new Dimension(2147483647, 35));
        setLayout(new FormLayout(
                "default:grow",
                "20px, 15px"));

        //======== tickPane ========
        {
            tickPane.setLayout(new FormLayout(
                    "default:grow",
                    "fill:20px"));
        }
        add(tickPane, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));

        //======== timeLabelPane ========
        {
            timeLabelPane.setLayout(new FormLayout(
                    "default:grow",
                    "fill:15px"));
        }
        add(timeLabelPane, cc.xy(1, 2, CellConstraints.FILL, CellConstraints.FILL));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel tickPane;
    private JPanel timeLabelPane;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
