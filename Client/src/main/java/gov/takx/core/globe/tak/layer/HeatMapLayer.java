
package gov.takx.core.globe.tak.layer;

import com.atakmap.map.elevation.ElevationData;
import com.atakmap.map.layer.AbstractLayer;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages the heatmap layer state.
 *
 * @since 5.6 (from ATAK HeatMapOverlay)
 */
@SuppressWarnings("deprecation")
public class HeatMapLayer extends AbstractLayer
{
    private final List<OnHeatMapResolutionChangedListener> resolutionChangedListeners = new CopyOnWriteArrayList<>();

    private static final float ALPHA = 0.1f;
    private static final float SATURATION = 0.1f;
    private static final float VALUE = 0.1f;
    private int sampleResolutionX = 14 * 7;
    private int sampleResolutionY = 10 * 7;
    private int elevationModel = ElevationData.MODEL_TERRAIN;

    public HeatMapLayer(@Nonnull String name)
    {
        super(name);
    }

    /**
     * @param listener The resolution change listener to add
     */
    public void addOnHeatMapResolutionChangedListener(@Nonnull OnHeatMapResolutionChangedListener listener)
    {
        resolutionChangedListeners.add(listener);
    }

    /**
     * @param listener The resolution change listener to remove
     */
    public void removeOnHeatMapResolutionChangedListener(@Nonnull OnHeatMapResolutionChangedListener listener)
    {
        resolutionChangedListeners.remove(listener);
    }

    /**
     * @return The heatmap color alpha value
     */
    public float getAlpha()
    {
        return ALPHA;
    }

    /**
     * @return The heatmap color saturation value
     */
    public float getSaturation()
    {
        return SATURATION;
    }

    /**
     * @return The heatmap color value
     */
    public float getValue()
    {
        return VALUE;
    }

    /**
     * @return The x sample resolution
     */
    public int getSampleResolutionX()
    {
        return sampleResolutionX;
    }

    /**
     * @return The y sample resolution
     */
    public int getSampleResolutionY()
    {
        return sampleResolutionY;
    }

    /**
     * @param x The number of x samples
     * @param y The number of y samples
     */
    public void setSampleResolution(int x, int y)
    {
        sampleResolutionX = x;
        sampleResolutionY = y;

        notifyResolutionChangedListeners();
    }

    @Override
    public boolean isVisible()
    {
        return visible;
    }

    /**
     * @return either {@link ElevationData#MODEL_TERRAIN} or {@link ElevationData#MODEL_SURFACE}
     */
    public int getElevationModel()
    {
        return elevationModel;
    }

    /**
     * @param elevationModel The model to use, either {@link ElevationData#MODEL_TERRAIN} or {@link ElevationData#MODEL_SURFACE}
     */
    public void setElevationModel(int elevationModel)
    {
        this.elevationModel = elevationModel;
        notifyResolutionChangedListeners(); // resolution change listeners will use current elevation model
    }

    private void notifyResolutionChangedListeners()
    {
        resolutionChangedListeners.forEach(listener -> listener.onHeatMapResolutionChanged(this));
    }

    /**
     * Contract for resolution change listeners.
     */
    public interface OnHeatMapResolutionChangedListener
    {
        void onHeatMapResolutionChanged(@Nonnull HeatMapLayer layer);
    }
}
