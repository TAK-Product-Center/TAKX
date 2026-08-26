package gov.takx.platform.playback;

import gov.takx.api.playback.IMarker;
import gov.takx.api.playback.IPlaybackLane;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.platform.playback.ui.ATimeCursor;
import gov.takx.platform.playback.ui.TimeCursorLine;
import gov.takx.platform.playback.ui.TimeCursorMarkerDiamond;
import gov.takx.platform.playback.ui.TimeCursorMarkerPin;
import jakarta.annotation.Nonnull;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default {@link IMarker} implementation suitable for most marker purposes.
 * <p/>
 * When a double click is detected on the cursor for this marker in a given lane, a callback to {@link #handleDoubleClick()}
 * is made in response.  Subclasses should override this method to implement any required response to the user action.
 *
 * @see #handleDoubleClick()
 * @since 1.47
 */
@SuppressWarnings("UnusedDeclaration") // Used from plugins
public class DefaultMarker extends MarkerBoundary implements IMarker
{
    protected static int count;

    protected String label;
    protected Color markerColor;
    protected final TimeCursorMarkerPin rulerCursor;
    protected boolean visible = true;

    protected final List<IPlaybackLane> targetLanes = new ArrayList<>();
    protected Map<IPlaybackLane, ATimeCursor> cursorMap = new ConcurrentHashMap<>();

    /**
     * Create a default marker with a label of "Marker N" where N is the current count of markers and a color that
     * is randomly generated.
     */
    public DefaultMarker()
    {
        this("Marker " + (++count));
    }

    public DefaultMarker(String label)
    {
        this(label, ImageUtils.generateRandomColor(150));
    }

    /**
     * Create a default marker with the given label and color, which is used to render the pin-head marker on the ruler.
     *
     * @param label Label of marker
     * @param color Color to use for rendering marker
     */
    public DefaultMarker(String label, Color color)
    {
        markerColor = color;
        rulerCursor = new TimeCursorMarkerPin(markerColor);

        setLabel(label);
    }

    @Override
    public void handleDoubleClick(MouseEvent e)
    {
    }

    @Override
    public void setMoment(long moment)
    {
        super.setMoment(moment);

        rulerCursor.setToolTipText(String.format("<html>%s<br>%s</html>", getLabel(), TimelineUtils.getDateString(moment)));
    }

    @Override
    public String getLabel()
    {
        return label;
    }

    @Override
    public void setLabel(String label)
    {
        this.label = label;
        rulerCursor.setToolTipText(label);
    }

    @Override
    public Color getMarkerColor()
    {
        return markerColor;
    }

    @Override
    public void setMarkerColor(@Nonnull Color markerColor)
    {
        this.markerColor = markerColor;
        rulerCursor.setColor(markerColor);
    }

    @Override
    public ATimeCursor getRulerCursor()
    {
        return rulerCursor;
    }

    @Override
    public ATimeCursor getLaneCursor(@Nonnull IPlaybackLane lane)
    {
        ATimeCursor trackCursor = cursorMap.get(lane);
        if (trackCursor == null)
        {
            if (targetLanes.contains(lane))
            {
                trackCursor = new TimeCursorMarkerDiamond(getMarkerColor(), getLabel());
            } else
            {
                trackCursor = new TimeCursorLine(Color.GRAY);
            }
            cursorMap.put(lane, trackCursor);
        }
        return trackCursor;
    }

    @Override
    public void updateCursorPosition(int x)
    {
        rulerCursor.setXLocation(x);
    }

    @Override
    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    @Override
    public boolean isVisible()
    {
        return visible;
    }

    @Override
    public Collection<IPlaybackLane> getTargetLanes()
    {
        return Collections.unmodifiableList(targetLanes);
    }

    @Override
    public void addTargetLane(@Nonnull IPlaybackLane lane)
    {
        targetLanes.add(lane);
        cursorMap.remove(lane); // Remove any registered cursor
    }

    @Override
    public void removeTargetLane(@Nonnull IPlaybackLane lane)
    {
        targetLanes.remove(lane);
        cursorMap.remove(lane); // Remove any registered cursor
    }

    @Override
    public boolean isTargetLane(@Nonnull IPlaybackLane lane)
    {
        return targetLanes.contains(lane);
    }
}
