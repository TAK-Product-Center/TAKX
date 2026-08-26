package gov.takx.api.layer;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.layers.Layer;
import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.persist.IPersistable;
import gov.takx.commons.swing.ImageUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.ImageIcon;

import java.beans.PropertyChangeListener;
import java.io.Serializable;

/**
 */
@NoPluginImplementations
public interface IRaptorLayer extends Serializable, IPersistable
{
    /**
     * Globe Id to use if the layer is not specific to a globe instance (i.e. sharable).
     *
     * @see #setSpecificGlobeId(int)
     */
    int ALL_GLOBES = -1;

    // Property keys

    String NAME = "NAME";
    String DISPLAY_NAME = "DISPLAY_NAME";
    String VISIBLE = "VISIBLE";
    String OPACITY = "OPACITY";
    String PICKABLE = "PICKABLE";
    String EDITABLE = "EDITABLE";
    String NETWORK_RETRIEVABLE = "NETWORK_RETRIEVABLE";
    String MIN_ACTIVE_ALTITUDE = "MIN_ACTIVE_ALTITUDE";
    String MAX_ACTIVE_ALTITUDE = "MAX_ACTIVE_ALTITUDE";
    String SPLIT_SCALE = "SPLIT_SCALE";
    String DELETED = "DELETED";

    /**
     * Default maximum eye altitude, in meters, at which the layer will be displayed.
     */
    double DEFAULT_MAX_ACTIVE_ALTITUDE = 200_000_000D;

    /**
     * Default minimum eye altitude, in meters, at which the layer will be displayed.
     * With this setting (of -1000) the layer is always visible no matter how zoomed in the view.
     */
    double DEFAULT_MIN_ACTIVE_ALTITUDE = -1_000D;

    String INSTALLED_TYPE = "INSTALLED_TYPE";

    String getKey();

    String getName();

    /**
     * @return The display name for this layer.
     */
    String getDisplayName();

    /**
     * Determine if this layer is sharable across multiple globe instances.  If {@link #setSpecificGlobeId(int)} has
     * been called, then this will return false.  If a layer is <em>sharable</em>, then the underlying Globe layer
     * will determine if the actual layer can be shared.
     *
     * @return {@code true} if this is a sharable layer
     * @see #setSpecificGlobeId(int)
     */
    boolean isSharable();

    /**
     * If this layer is specific to a globe instance (i.e. not sharable), then use this method to specify the id
     * of the globe to which the layer is targeted.
     *
     * @param globeId Id of globe
     */
    void setSpecificGlobeId(int globeId);

    /**
     * @return If this layer is targeted at a specific globe, then the Id of that globe.  Otherwise -1
     */
    int getSpecificGlobeId();

    /**
     * Returns true if the layer's "in view" state has changed.  The state changes if the altitude goes from being
     * within the layers' min and max altitudes to being outside of these thresholds. The state also returns true if the
     * altitude goes from outside the thresholds to within the thresholds. If the state hasn't changed false is
     * returned.
     *
     * @param altitude if between the layers min and max altitudes the layer should be rendered
     * @return whether the layer's "In View" state has changed due to the altitude
     */
    boolean inViewAltitudeChanged(double altitude);

    /**
     * Update the layer's rendered status and return a boolean indicating if it has transitioned to a new state
     * (not rendered to rendered or vice-versa).
     *
     * @param isRendered Boolean indicating if this layer is currently being rendered on the screen.
     * @return Boolean indicating if the rendered state of this layer has changed.
     * @since 1.20
     */
    boolean layerIsRenderedChanged(boolean isRendered);

    /**
     * Return a boolean indicating if this layer is currently being rendered.
     *
     * @return A boolean indicating if this layer is currently being rendered.
     * @since 1.20
     */
    boolean isLayerRendered();

    /**
     * @param altitude to compare to the layers min/max altitude thresholds
     * @return true if the given altitude is within the layers max and min thresholds
     */
    boolean altitudeWithinThreshold(double altitude);

    /**
     * Set the display name for this layer.
     *
     * @param displayName New display name
     */
    void setDisplayName(String displayName);

    /**
     * Each layer is either defined by a class name to instantiate or by a configuration file name.  This method
     * returns the class name if it is set, {@code null} otherwise.
     * <p/>
     * Either this method or {@link #getConfigFileName()} will return a non-null value.
     *
     * @return The name of the class that defines this layer, may be {@code null} if the layer is defined by an XML config file.
     * @see #getConfigFileName()
     */
    String getClassName();

    /**
     * Each layer is either defined by a class name to instantiate or by a configuration file name.  This method
     * returns the name of the XML configuration file if it is set, {@code null} otherwise.
     * <p/>
     * Either this method or {@link #getClassName()} will return a non-null value.
     *
     * @return The path to the XML config file that defines this layer, may be {@code null} if the layer is defined by a Class.
     * @since 1.11
     */
    String getConfigFileName();

    /**
     * If an {@link IRaptorLayer} is being constructed around an existing (pre-built) WW layer, then this method will
     * return that layer. Otherwise, it returns {@code null}.
     *
     * @return Pre-built layer upon which this layer was constructed, {@code null} if className/config file is used.
     */
    @Nullable
    Layer getConstructionlayer();

    /**
     * If an {@link IRaptorLayer} is being constructed around an existing (pre-built) TAK layer, then this method will
     * return that layer. Otherwise, it returns {@code null}.
     *
     * @return Pre-built layer upon which this layer was constructed, {@code null} if className/config file is used.
     * @since 5.2
     */
    @Nullable
    com.atakmap.map.layer.Layer getTakConstructionLayer();

    /**
     * Always returns {@code null}.
     *
     * @return {@code null}
     */
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    @Deprecated(since = "5.6", forRemoval = true)
    @Nullable
    default ElevationModel getConstructionModel()
    {
        return null;
    }

    boolean isEditable();

    void setEditable(boolean editable);

    boolean isVisible();

    boolean isPickable();

    double getOpacity();

    double getMinActiveAltitude();

    double getMaxActiveAltitude();

    boolean isNetworkRetrievalEnabled();

    LayerGroup getLayerGroup();

    String toString();

    boolean isMaxActiveAltitudeEnabled();

    boolean isMinActiveAltitudeEnabled();

    void setName(@Nonnull String name);

    void setVisible(boolean visible);

    void setPickable(boolean pickable);

    void setOpacity(double opacity);

    /**
     * @return The <em>small</em> icon for this layer.
     * @see ImageUtils#SMALL_ICON_WIDTH
     * @see ImageUtils#SMALL_ICON_HEIGHT
     */
    ImageIcon getSmallIcon();

    ImageIcon getIcon(int width, int height);

    /**
     * @return The path to the icon for this layer
     * @since 3.5 (added to interface, was already in implementation)
     */
    String getIconPath();

    void setNetworkRetrievalEnabled(boolean networkRetrievalEnabled);

    /**
     * Remove all previously registered listeners, regardless of how they registered.
     * <p/>
     * <em>NOTE: This method is not intended for general use, it is intended for use by the core during shutdown.</em>
     */
    @Internal
    void removeAllRaptorLayerListeners();

    /**
     * Add a property change listener on ALL properties of this object.  To unregister, use
     * {@link #removePropertyChangeListener(PropertyChangeListener)}.
     *
     * @param listener Listener to add
     * @since 3.4.1
     */
    void addPropertyChangeListener(@Nonnull PropertyChangeListener listener);

    /**
     * Add a property change listener on the given property of this object.  To unregister, use
     * {@link #removePropertyChangeListener(String, PropertyChangeListener)}.
     *
     * @param property Property of interest
     * @param listener Listener to register
     * @since 3.4.1
     */
    void addPropertyChangeListener(@Nonnull String property, @Nonnull PropertyChangeListener listener);

    /**
     * Remove the listener. This method can only be used if the listener was originally registered using
     * {@link #addPropertyChangeListener(PropertyChangeListener)}
     *
     * @param listener Listener to remove
     * @since 3.4.1
     */
    void removePropertyChangeListener(@Nonnull PropertyChangeListener listener);

    /**
     * Remove the listener from the specified property.
     *
     * @param property Property to unregister
     * @param listener Listener to remove
     * @since 3.4.1
     */
    void removePropertyChangeListener(@Nonnull String property, @Nonnull PropertyChangeListener listener);

    /**
     * @return {@code true} if this is a <em>permanent</em> layer, meaning that the user can not delete it.
     */
    boolean isPermanent();

    /**
     * Specify if this layer is a <em>permanent</em> layer, meaning that the user can not delete it.
     *
     * @param permanent Pass {@code true} to make the layer permanent
     */
    void setPermanent(boolean permanent);

    /**
     * Is this layer deleted.  We track deletions so that layers deleted by a user are not recreated when a project
     * is upgraded.
     *
     * @return {@code true} if this layer has been deleted.
     */
    boolean isDeleted();

    /**
     * Set the deleted data of a layer.
     *
     * @param deleted New deleted state.
     */
    void setDeleted(boolean deleted);

    void setMinActiveAltitude(double min);

    void setMaxActiveAltitude(double max);

    void setMaxActiveAltitudeEnabled(boolean b);

    void setMinActiveAltitudeEnabled(boolean b);

    void setRestorableState(IRestorable o);

    String getRestorableState();

    Integer getOrderkey();

    void setOrderkey(Integer orderkey);

    String getType();

    /**
     * Get the object that represents the visible layer. The runtime type of this object will match the classname
     * used when the layer was created.
     *
     * @return The graphical layer.
     * @see #getClassName()
     */
    Layer getLayer();

    /**
     * Always returns {@code null}.
     *
     * @return {@code null}
     */
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    @Deprecated(since = "5.6", forRemoval = true)
    @Nullable
    default ElevationModel getElevationModel()
    {
        return null;
    }

    /**
     * Get the sector for a layer if available.  Some layers explicitly provide sectors and some do not.  In the case
     * of Surface Structure layers, some extra logic may be used to determine the sector.
     * <p>
     * Note: This was originally part of WWUtils which moved from client to platform in version 2.38
     *
     * @return the layer's sector (if one exists)
     * @since 2.38
     */
    Sector getSector();

    double getSplitScale();

    void setSplitScale(double splitScale);

    /**
     * @deprecated as of 5.6
     */
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    @Deprecated(since = "5.6", forRemoval = true)
    default void setInstalled(boolean ignored)
    {
        // do nothing
    }

    /**
     * @return Always {@code true}
     * @deprecated as of 5.6
     */
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    @Deprecated(since = "5.6", forRemoval = true)
    default boolean isInstalled()
    {
        return true;
    }
}
