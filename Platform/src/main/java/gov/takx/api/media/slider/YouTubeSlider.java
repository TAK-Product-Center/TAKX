package gov.takx.api.media.slider;

import gov.takx.api.annotation.Experimental;

import javax.swing.JComponent;
import javax.swing.event.MouseInputAdapter;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slider class that mimics the look and feel of YouTube.
 *
 * @since 2.37
 */
@Experimental
public class YouTubeSlider extends JComponent implements Observer
{
    private final YouTubeSliderModel model = new YouTubeSliderModel();
    private int thumbRadiusPixels = 5;
    private boolean drawCompact = true;

    private final Set<ISliderListener> listeners = ConcurrentHashMap.newKeySet();

    public YouTubeSlider()
    {
        this(true);
    }

    /**
     * Constructs a YouTubeSlider with default settings.
     *
     * @param allowUserInput True to allow the user to drag the slider
     */
    public YouTubeSlider(boolean allowUserInput)
    {
        model.addObserver(this);

        if (allowUserInput)
        {
            MouseInputAdapter mia = new YouTubeSliderMouseInputAdapter();
            addMouseListener(mia);
            addMouseMotionListener(mia);
        }
    }

    /**
     * Registers an object with this slider that will receive events when the slider thumb is moved.
     *
     * @param listener the object that will receive events
     */
    public void addSliderListener(ISliderListener listener)
    {
        listeners.add(listener);
    }

    /**
     * Unregisters an object from this slider so that it will no longer receive the slider's events.
     *
     * @param listener the object to unregister
     */
    public void removeSliderListener(ISliderListener listener)
    {
        listeners.remove(listener);
    }

    /**
     * Updates the slider thumb position.
     * This method will NOT cause a slider event to be fired. If you want one, use {@link #updateThumbPosition(double, boolean)} instead.
     *
     * @param pct where to set the thumb, as a percentage of its full range (0.0 for full left, 1.0 for full right)
     */
    public void updateThumbPosition(double pct)
    {
        updateThumbPosition(pct, false);
    }

    /**
     * Updates the slider thumb position.
     *
     * @param pct              where to set the thumb, as a percentage of its full range (0.0 for full left, 1.0 for full right)
     * @param fireChangedEvent true if a {@link SliderEvent} is to be fired, false otherwise
     */
    public void updateThumbPosition(double pct, boolean fireChangedEvent)
    {
        model.setThumbPosition(pct);
        if (fireChangedEvent)
        {
            SliderEvent event = new SliderEvent(SliderEvent.SliderEventType.SLIDER_MOVED, pct);
            for (ISliderListener listener : listeners)
            {
                listener.sliderMoved(event);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        thumbRadiusPixels = getHeight() / 2;

        if (isOpaque())
        {
            drawBackground(g);
        }

        drawTrack(g);

        if (!drawCompact)
        {
            drawThumb(g);
        }

        g.dispose();
    }

    private void drawBackground(Graphics g)
    {
        g.setColor(Color.darkGray);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawTrack(Graphics g)
    {
        int width = getWidth();
        int height = getHeight();

        int trackHeight = drawCompact ? (height / 4) : (3 * height / 8);
        int trackYOffset = (height - trackHeight) / 2;

        for (SliderColorSegment segment : model.getSliderColorSegments())
        {
            int startPixel = (int) Math.floor(width * segment.start);
            int endPixel = (int) Math.ceil(width * segment.end);
            int widthPixels = endPixel - startPixel;
            g.setColor(segment.color);
            g.fillRect(startPixel, trackYOffset, widthPixels, trackHeight);
        }

        // Draw color behind thumb, if enabled
        if (model.isDrawColorBehindThumbEnabled())
        {
            Graphics2D g2 = (Graphics2D) g;
            int thumbCenterPixel = (int) Math.round(model.getThumbPosition() * width);
            Color backfillColor = model.getColorBehindThumb();

            // Translucent backfill color. Leave commented out for now, doesn't look as good as I thought it would.
            //
//            Color transparentBackfillColor = new Color(backfillColor.getRed(), backfillColor.getGreen(), backfillColor.getBlue(), 64);
//            backfillColor = transparentBackfillColor;

            g2.setColor(backfillColor);
            g2.fillRect(0, trackYOffset, thumbCenterPixel, trackHeight);
        }
    }

    private void drawThumb(Graphics g)
    {
        int width = getWidth();
        int thumbXPixels = (int) Math.round(model.getThumbPosition() * width);

        g.setColor(model.getThumbColor());
        g.fillOval(thumbXPixels - thumbRadiusPixels, 0, thumbRadiusPixels * 2, thumbRadiusPixels * 2);
    }

    public void addColorSegment(double start, double end, Color color)
    {
        System.err.println("addColorSegment " + start + " " + end + " " + color.toString());
        model.addColorSegment(start, end, color);
    }

    @Override
    public void update(Observable o, Object arg)
    {
        repaint();
    }

    private class YouTubeSliderMouseInputAdapter extends MouseInputAdapter
    {
        // Keep track of when we're dragging so we know when (and when not) to respond to mouseExited.
        boolean isDragging = false;

        @Override
        public void mousePressed(MouseEvent e)
        {
            super.mousePressed(e);
            if (e.getButton() == MouseEvent.BUTTON1)
            {
                updateThumbPosition(e.getPoint().x / (double) getWidth(), true);
                SliderEvent event = new SliderEvent(SliderEvent.SliderEventType.SLIDER_DRAG_BEGIN, model.getThumbPosition());
                for (ISliderListener listener : listeners)
                {
                    listener.sliderDragBegin(event);
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            super.mouseReleased(e);
            isDragging = false;
            drawCompact = !e.getComponent().contains(e.getPoint());
            repaint();
            SliderEvent event = new SliderEvent(SliderEvent.SliderEventType.SLIDER_DRAG_END, model.getThumbPosition());
            for (ISliderListener listener : listeners)
            {
                listener.sliderDragEnd(event);
            }
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            super.mouseDragged(e);
            updateThumbPosition(e.getPoint().x / (double) getWidth(), true);
            isDragging = true;
        }

        @Override
        public void mouseEntered(MouseEvent e)
        {
            drawCompact = false;
            repaint();
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
            if (!isDragging)
            {
                drawCompact = true;
            }
            repaint();
        }
    }
}
