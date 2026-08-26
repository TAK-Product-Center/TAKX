package gov.takx.core.view.gui.components;

import com.formdev.flatlaf.ui.FlatSliderUI;
import com.formdev.flatlaf.ui.FlatUIUtils;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

/**
 * UI tweaks for a dual (min/max) slider
 *
 * @since 1.43
 */
public class RangedSliderUI extends FlatSliderUI
{
    private Rectangle upperThumbRect;
    private boolean upperThumbSelected;
    private boolean lowerDragging;
    private boolean upperDragging;
    private Color defaultForeground;

    @Override
    public void installUI(JComponent c)
    {
        upperThumbRect = new Rectangle();
        defaultForeground = UIManager.getColor("Slider.foreground");
        super.installUI(c);
    }

    @Override
    protected TrackListener createTrackListener(JSlider slider)
    {
        return new RangeTrackListener();
    }

    @Override
    protected ChangeListener createChangeListener(JSlider slider)
    {
        return new ChangeHandler();
    }

    @Override
    protected void calculateThumbSize()
    {
        // Call superclass method for lower thumb size.
        super.calculateThumbSize();

        // Set upper thumb size.
        upperThumbRect.setSize(thumbRect.width, thumbRect.height);
    }

    @Override
    protected void calculateThumbLocation()
    {
        // Call superclass method for lower thumb location.
        super.calculateThumbLocation();

        // Adjust upper value to snap to ticks if necessary.
        if (slider.getSnapToTicks())
        {
            int upperValue = slider.getValue() + slider.getExtent();
            int snappedValue = upperValue;
            int majorTickSpacing = slider.getMajorTickSpacing();
            int minorTickSpacing = slider.getMinorTickSpacing();
            int tickSpacing = 0;

            if (minorTickSpacing > 0)
            {
                tickSpacing = minorTickSpacing;
            } else if (majorTickSpacing > 0)
            {
                tickSpacing = majorTickSpacing;
            }

            if (tickSpacing != 0)
            {
                // If it's not on a tick, change the value
                if ((upperValue - slider.getMinimum()) % tickSpacing != 0)
                {
                    float temp = (upperValue - slider.getMinimum()) / (float) tickSpacing;
                    int whichTick = Math.round(temp);
                    snappedValue = slider.getMinimum() + (whichTick * tickSpacing);
                }

                if (snappedValue != upperValue)
                {
                    slider.setExtent(snappedValue - slider.getValue());
                }
            }
        }

        // Calculate upper thumb location.  The thumb is centered over its value on the track.
        if (slider.getOrientation() == JSlider.HORIZONTAL)
        {
            int upperPosition = xPositionForValue(slider.getValue() + slider.getExtent());
            upperThumbRect.x = upperPosition - (upperThumbRect.width / 2) + 1;
            upperThumbRect.y = thumbRect.y;
        } else
        {
            int upperPosition = yPositionForValue(slider.getValue() + slider.getExtent());
            upperThumbRect.x = thumbRect.x;
            upperThumbRect.y = upperPosition - (upperThumbRect.height / 2) + 1;
        }
    }

    @Override
    public void paint(Graphics g, JComponent c)
    {
        super.paint(g, c);

        Rectangle clipRect = g.getClipBounds();
        if (upperThumbSelected)
        {
            // Paint lower thumb first, then upper thumb.
            if (clipRect.intersects(thumbRect))
            {
                paintLowerThumb(g);
            }
            if (clipRect.intersects(upperThumbRect))
            {
                paintUpperThumb(g);
            }
        } else
        {
            // Paint upper thumb first, then lower thumb.
            if (clipRect.intersects(upperThumbRect))
            {
                paintUpperThumb(g);
            }
            if (clipRect.intersects(thumbRect))
            {
                paintLowerThumb(g);
            }
        }
    }

    @Override
    public void paintTrack(Graphics g)
    {
        // Draw track.
        super.paintTrack(g);

        paintTrackRange(g);
    }

    /**
     * Paint the selected range portion of the track.
     */
    protected void paintTrackRange(Graphics g)
    {
        Rectangle trackBounds = trackRect;

        if (slider.getOrientation() == JSlider.HORIZONTAL)
        {
            // Determine position of selected range by moving from the middle
            // of one thumb to the other.
            int lowerX = thumbRect.x + (thumbRect.width / 2);
            int upperX = upperThumbRect.x + (upperThumbRect.width / 2);

            // Determine track position.
            int cy = (trackBounds.height / 2) - 2;

            // Save color and shift position.
            Color oldColor = g.getColor();
            g.translate(trackBounds.x, trackBounds.y + cy);

            // Draw selected range.
            g.setColor(trackValueColor);
            for (int y = 0; y <= 3; y++)
            {
                g.drawLine(lowerX - trackBounds.x, y, upperX - trackBounds.x, y);
            }

            // Restore position and color.
            g.translate(-trackBounds.x, -(trackBounds.y + cy));
            g.setColor(oldColor);
        } else
        {
            // Determine position of selected range by moving from the middle
            // of one thumb to the other.
            int lowerY = thumbRect.x + (thumbRect.width / 2);
            int upperY = upperThumbRect.x + (upperThumbRect.width / 2);

            // Determine track position.
            int cx = (trackBounds.width / 2) - 2;

            // Save color and shift position.
            Color oldColor = g.getColor();
            g.translate(trackBounds.x + cx, trackBounds.y);

            // Draw selected range.
            g.setColor(trackValueColor);
            for (int x = 0; x <= 3; x++)
            {
                g.drawLine(x, lowerY - trackBounds.y, x, upperY - trackBounds.y);
            }

            // Restore position and color.
            g.translate(-(trackBounds.x + cx), -trackBounds.y);
            g.setColor(oldColor);
        }
    }

    /**
     * Overrides superclass method to do nothing.  Thumb painting is handled
     * within the {@code paint()} method.
     */
    @Override
    public void paintThumb(Graphics g)
    {
        // Do nothing.
    }

    /**
     * Paints the thumb for the lower value using the specified graphics object.
     */
    private void paintLowerThumb(Graphics g)
    {
        paintInternalThumb(g, thumbRect);
    }

    /**
     * Paints the thumb for the upper value using the specified graphics object.
     */
    private void paintUpperThumb(Graphics g)
    {
        paintInternalThumb(g, upperThumbRect);
    }

    /**
     * Command paint method on a given bounds for the thumb.
     *
     * Note: this is a direct copy of {@link FlatSliderUI#paintThumb(Graphics)} with the exception of using the passed
     * in rectangle bounds.
     */
    private void paintInternalThumb(Graphics g, Rectangle knobBounds)
    {
        Color thumbColor = getThumbColor();
        Color color = stateColor(slider, thumbHover, thumbPressed,
                thumbColor, disabledThumbColor, null, hoverThumbColor, pressedThumbColor);
        color = FlatUIUtils.deriveColor(color, thumbColor);

        Color foreground = slider.getForeground();
        Color borderColor = (thumbBorderColor != null && foreground == defaultForeground)
                ? stateColor(slider, false, false, thumbBorderColor, disabledThumbBorderColor, focusedThumbBorderColor, null, null)
                : null;

        Color focusedColor = FlatUIUtils.deriveColor(this.focusedColor,
                (foreground != defaultForeground) ? foreground : focusBaseColor);

        paintThumb(g, slider, knobBounds, isRoundThumb(), color, borderColor, focusedColor, thumbBorderWidth, focusWidth);
    }

    /**
     * Sets the location of the upper thumb, and repaints the slider.  This is
     * called when the upper thumb is dragged to repaint the slider.  The
     * {@code setThumbLocation()} method performs the same task for the
     * lower thumb.
     */
    private void setUpperThumbLocation(int x, int y)
    {
        Rectangle upperUnionRect = new Rectangle();
        upperUnionRect.setBounds(upperThumbRect);

        upperThumbRect.setLocation(x, y);

        SwingUtilities.computeUnion(upperThumbRect.x, upperThumbRect.y, upperThumbRect.width, upperThumbRect.height, upperUnionRect);
        slider.repaint(upperUnionRect.x, upperUnionRect.y, upperUnionRect.width, upperUnionRect.height);
    }

    @SuppressWarnings("SynchronizeOnNonFinalField")
    @Override
    public void scrollByBlock(int direction)
    {
        // Called when user presses page up/down.
        synchronized (slider)
        {
            int blockIncrement = (slider.getMaximum() - slider.getMinimum()) / 10;
            if (blockIncrement <= 0 && slider.getMaximum() > slider.getMinimum())
            {
                blockIncrement = 1;
            }
            int delta = blockIncrement * ((direction > 0) ? POSITIVE_SCROLL : NEGATIVE_SCROLL);

            if (upperThumbSelected)
            {
                int oldValue = ((RangedSlider) slider).getUpperValue();
                ((RangedSlider) slider).setUpperValue(oldValue + delta);
            } else
            {
                super.scrollByBlock(direction);
            }
        }
    }

    @SuppressWarnings("SynchronizeOnNonFinalField")
    @Override
    public void scrollByUnit(int direction)
    {
        // Called when used presses arrow keys.
        synchronized (slider)
        {
            int delta = (direction > 0) ? POSITIVE_SCROLL : NEGATIVE_SCROLL;

            if (upperThumbSelected)
            {
                int oldValue = ((RangedSlider) slider).getUpperValue();
                ((RangedSlider) slider).setUpperValue(oldValue + delta);
            } else
            {
                super.scrollByUnit(direction);
            }
        }
    }

    /**
     * Listener to handle model change events.  This calculates the thumb
     * locations and repaints the slider if the value change is not caused by
     * dragging a thumb.
     */
    public class ChangeHandler implements ChangeListener
    {
        @Override
        public void stateChanged(ChangeEvent arg0)
        {
            if (!lowerDragging && !upperDragging)
            {
                calculateThumbLocation();
                slider.repaint();
            }
        }
    }

    /**
     * Listener to handle mouse movements in the slider track.
     */
    public class RangeTrackListener extends TrackListener
    {

        @Override
        public void mousePressed(MouseEvent e)
        {
            if (!slider.isEnabled()) return;

            currentMouseX = e.getX();
            currentMouseY = e.getY();

            if (slider.isRequestFocusEnabled())
            {
                slider.requestFocus();
            }

            // Determine which thumb is pressed.  If the upper thumb is
            // selected (last one dragged), then check its position first;
            // otherwise check the position of the lower thumb first.
            boolean lowerPressed = false;
            boolean upperPressed = false;
            if (upperThumbSelected || slider.getMinimum() == slider.getValue())
            {
                if (upperThumbRect.contains(currentMouseX, currentMouseY))
                {
                    upperPressed = true;
                } else if (thumbRect.contains(currentMouseX, currentMouseY))
                {
                    lowerPressed = true;
                }
            } else
            {
                if (thumbRect.contains(currentMouseX, currentMouseY))
                {
                    lowerPressed = true;
                } else if (upperThumbRect.contains(currentMouseX, currentMouseY))
                {
                    upperPressed = true;
                }
            }

            lowerDragging = false;
            upperDragging = false;

            // Handle lower thumb pressed.
            if (lowerPressed)
            {
                handleLowerThumbPressed();
            } else if (upperPressed)
            {
                handleUpperThumbPressed();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            lowerDragging = false;
            upperDragging = false;
            slider.setValueIsAdjusting(false);
            super.mouseReleased(e);
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            if (!slider.isEnabled()) return;

            currentMouseX = e.getX();
            currentMouseY = e.getY();

            if (lowerDragging)
            {
                slider.setValueIsAdjusting(true);
                moveLowerThumb();
            } else if (upperDragging)
            {
                slider.setValueIsAdjusting(true);
                moveUpperThumb();
            }
        }

        @Override
        public boolean shouldScroll(int direction)
        {
            return false;
        }

        private void handleLowerThumbPressed()
        {
            offset = slider.getOrientation() == JSlider.VERTICAL ? currentMouseY - thumbRect.y : currentMouseX - thumbRect.x;
            upperThumbSelected = false;
            lowerDragging = true;
        }

        private void handleUpperThumbPressed()
        {
            offset = slider.getOrientation() == JSlider.VERTICAL ? currentMouseY - upperThumbRect.y : currentMouseX - upperThumbRect.x;
            upperThumbSelected = true;
            upperDragging = true;
        }

        /**
         * Moves the location of the lower thumb, and sets its corresponding
         * value in the slider.
         */
        private void moveLowerThumb()
        {
            int thumbMiddle;

            if (slider.getOrientation() == JSlider.VERTICAL)
            {
                int halfThumbHeight = thumbRect.height / 2;
                int thumbTop = currentMouseY - offset;
                int trackTop = trackRect.y;
                int trackBottom = trackRect.y + (trackRect.height - 1);
                int vMax = yPositionForValue(slider.getValue() + slider.getExtent());

                // Apply bounds to thumb position.
                if (drawInverted())
                {
                    trackBottom = vMax;
                } else
                {
                    trackTop = vMax;
                }
                thumbTop = Math.max(thumbTop, trackTop - halfThumbHeight);
                thumbTop = Math.min(thumbTop, trackBottom - halfThumbHeight);
                setThumbLocation(thumbRect.x, thumbTop);

                // Update slider value.
                thumbMiddle = thumbTop + halfThumbHeight;
                slider.setValue(valueForYPosition(thumbMiddle));
            } else
            {
                int halfThumbWidth = thumbRect.width / 2;
                int thumbLeft = currentMouseX - offset;
                int trackLeft = trackRect.x;
                int trackRight = trackRect.x + (trackRect.width - 1);
                int hMax = xPositionForValue(slider.getValue() + slider.getExtent());

                // Apply bounds to thumb position.
                if (drawInverted())
                {
                    trackLeft = hMax;
                } else
                {
                    trackRight = hMax;
                }
                thumbLeft = Math.max(thumbLeft, trackLeft - halfThumbWidth);
                thumbLeft = Math.min(thumbLeft, trackRight - halfThumbWidth);
                setThumbLocation(thumbLeft, thumbRect.y);

                // Update slider value.
                thumbMiddle = thumbLeft + halfThumbWidth;
                slider.setValue(valueForXPosition(thumbMiddle));
            }
        }

        /**
         * Moves the location of the upper thumb, and sets its corresponding
         * value in the slider.
         */
        private void moveUpperThumb()
        {
            int thumbMiddle;

            if (slider.getOrientation() == JSlider.VERTICAL)
            {
                int halfThumbHeight = thumbRect.height / 2;
                int thumbTop = currentMouseY - offset;
                int trackTop = trackRect.y;
                int trackBottom = trackRect.y + (trackRect.height - 1);
                int vMin = yPositionForValue(slider.getValue());

                // Apply bounds to thumb position.
                if (drawInverted())
                {
                    trackTop = vMin;
                } else
                {
                    trackBottom = vMin;
                }
                thumbTop = Math.max(thumbTop, trackTop - halfThumbHeight);
                thumbTop = Math.min(thumbTop, trackBottom - halfThumbHeight);
                setUpperThumbLocation(thumbRect.x, thumbTop);

                // Update slider extent.
                thumbMiddle = thumbTop + halfThumbHeight;
                slider.setExtent(valueForYPosition(thumbMiddle) - slider.getValue());
            } else
            {
                int halfThumbWidth = thumbRect.width / 2;
                int thumbLeft = currentMouseX - offset;
                int trackLeft = trackRect.x;
                int trackRight = trackRect.x + (trackRect.width - 1);
                int hMin = xPositionForValue(slider.getValue());

                // Apply bounds to thumb position.
                if (drawInverted())
                {
                    trackRight = hMin;
                } else
                {
                    trackLeft = hMin;
                }
                thumbLeft = Math.max(thumbLeft, trackLeft - halfThumbWidth);
                thumbLeft = Math.min(thumbLeft, trackRight - halfThumbWidth);
                setUpperThumbLocation(thumbLeft, thumbRect.y);

                // Update slider extent.
                thumbMiddle = thumbLeft + halfThumbWidth;
                slider.setExtent(valueForXPosition(thumbMiddle) - slider.getValue());
            }
        }
    }

    @Override
    protected void installListeners(JSlider slider)
    {
        slider.addMouseListener(trackListener);
        slider.addMouseMotionListener(trackListener);
        slider.addFocusListener(focusListener);
        slider.addComponentListener(componentListener);
        slider.addPropertyChangeListener(propertyChangeListener);
        slider.getModel().addChangeListener(changeListener);
    }

    @Override
    protected void uninstallListeners(JSlider slider)
    {
        slider.removeMouseListener(trackListener);
        slider.removeMouseMotionListener(trackListener);
        slider.removeFocusListener(focusListener);
        slider.removeComponentListener(componentListener);
        slider.removePropertyChangeListener(propertyChangeListener);
        slider.getModel().removeChangeListener(changeListener);
    }
}
