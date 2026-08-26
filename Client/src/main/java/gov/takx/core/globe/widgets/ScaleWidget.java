package gov.takx.core.globe.widgets;

import android.graphics.Typeface;
import com.atakmap.android.maps.MapTextFormat;
import gov.tak.api.engine.map.IMapRendererEnums.DisplayOrigin;
import gov.tak.api.widgets.IScaleWidget2;
import gov.tak.platform.widgets.ShapeWidget;
import gov.takx.api.globe.CoreWidgetNames;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IWidget;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.units.RangeUnits;
import gov.takx.platform.utils.units.SpanConversionUtils;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Displays a visual representation of the map scale.
 *
 * @since 5.2
 */
public class ScaleWidget extends ShapeWidget implements IScaleWidget2, IWidget
{
    private final IPreferenceMonitor<RangeUnits> rangeUnitsMonitor =
            UserPreferenceEntry.DISPLAY_RANGE_UNITS.newMonitor(RangeUnits.class, this::setRangeUnits);
    private final List<OnTextChangedListener> textChangeListeners = new CopyOnWriteArrayList<>();
    private final List<OnDisplayChangedListener> displayChangeListeners = new CopyOnWriteArrayList<>();
    private final MapTextFormat mapTextFormat;

    private final float minWidth;
    private final float maxWidth;
    private String text = "";
    private int spanSystem = SpanConversionUtils.toSpanSystem(rangeUnitsMonitor.getCurrentValue());
    private double scale;

    public ScaleWidget(@Nonnull IGlobeDelegate globe)
    {
        float dpi = (float) globe.getPortableMapRenderer().getMapSceneModel(false, DisplayOrigin.UpperLeft).dpi;
        mapTextFormat = new MapTextFormat(Typeface.DEFAULT_BOLD, false, 12);
        minWidth = dpi * 2;
        maxWidth = dpi * 3;

        setPadding(4);
        setMargins(4, 8, 4, 8);
        setName(CoreWidgetNames.MAP_SCALE);
        setSize(1, 1); // Not to actually specify the widget size, but to trigger the initial size-update invalidation.
        setVisible(true);
    }

    @Override
    public void releaseResources()
    {
        rangeUnitsMonitor.dispose();
    }

    @Override
    public boolean testHit(float x, float y)
    {
        // False to disable mouse interactions
        return false;
    }

    @Override
    public boolean isRounded()
    {
        return true;
    }

    @Override
    public String getText()
    {
        return text;
    }

    @Override
    public double getScale()
    {
        return scale;
    }

    @Override
    public int getUnits()
    {
        return spanSystem;
    }

    @Override
    public float getMinWidth()
    {
        return minWidth;
    }

    @Override
    public float getMaxWidth()
    {
        return maxWidth;
    }

    @Override
    public void update(String text, double scale)
    {
        this.scale = scale;

        if (!Objects.equals(this.text, text))
        {
            this.text = text;
            textChangeListeners.forEach(l -> l.onScaleTextChanged(this));
        }
    }

    @Override
    public MapTextFormat getTextFormat()
    {
        return mapTextFormat;
    }

    @Override
    public void onPointChanged()
    {
        super.onPointChanged();
        onDisplayChanged();
    }

    @Override
    public void removeOnDisplayChangedListener(OnDisplayChangedListener listener)
    {
        displayChangeListeners.remove(listener);
    }

    @Override
    public void addOnDisplayChangedListener(OnDisplayChangedListener listener)
    {
        displayChangeListeners.add(listener);
    }

    @Override
    public final void addOnTextChangedListener(OnTextChangedListener l)
    {
        textChangeListeners.add(l);
    }

    @Override
    public final void removeOnTextChangedListener(OnTextChangedListener l)
    {
        textChangeListeners.remove(l);
    }

    private void onDisplayChanged()
    {
        displayChangeListeners.forEach(l -> l.onDisplayChanged(this));
    }

    /**
     * Updates the range units to display distance with.
     *
     * @param rangeUnits Units to display distance in.
     */
    private void setRangeUnits(@Nonnull RangeUnits rangeUnits)
    {
        // Adapt to TAK span int key
        int spanSystem = SpanConversionUtils.toSpanSystem(rangeUnits);
        if (spanSystem != this.spanSystem)
        {
            this.spanSystem = spanSystem;
            onDisplayChanged();
        }
    }
}
