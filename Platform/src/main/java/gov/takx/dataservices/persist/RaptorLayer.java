package gov.takx.dataservices.persist;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.GeographicExtent;
import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRestorable;
import gov.takx.api.layer.LayerGroup;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.utils.ClearablePropertyChangeSupport;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.globe.layers.IGlobeLayer;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;
import org.hibernate.Length;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.invoke.MethodHandles;

/**
 */
@Internal
@PseudoBean
@Entity(name = "ClientLayer")
public class RaptorLayer implements IRaptorLayer
{
    @Transient
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @SuppressWarnings("unused")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Transient
    private IRestorable restorableLayer;
    @Transient
    private String type;
    @Transient
    private ImageIcon smallIcon;

    @Transient
    private int specificGlobeId = ALL_GLOBES;

    @Transient
    private Layer constructionLayer; // Non-null if being constructed upon a pre-built WW layer

    @Transient
    private com.atakmap.map.layer.Layer takConstructionLayer; // Non-null if being constructed upon a pre-built TAK layer

    @Column(name = "key_")
    private final String key;
    private String name;
    private String displayName;
    private String className;
    private String configFileName;
    private boolean editable = true;
    private boolean visible = true;
    private boolean pickable;
    private double opacity = 1.00;
    private String iconPath;

    @Enumerated(EnumType.STRING)
    private LayerGroup layerGroup;

    private boolean networkRetrievalEnabled = true;

    private double maxActiveAltitude = DEFAULT_MAX_ACTIVE_ALTITUDE;
    private double minActiveAltitude = DEFAULT_MIN_ACTIVE_ALTITUDE;
    //TODO: not implemented yet
    private boolean maxActiveAltitudeEnabled = true;
    private boolean minActiveAltitudeEnabled = true;
    private double splitScale;

    @Column(length = Length.LONG32)
    private String restoreStateXML;

    private Integer orderkey;
    private boolean permanent;

    // Indicates that this layer was created by the core and can be removed if it cannot be restored
    private boolean systemManaged;

    @Column(nullable = false)
    private Boolean deleted = false;

    @Transient
    private final ClearablePropertyChangeSupport changeSupport = new ClearablePropertyChangeSupport(this);

    // Used to determine if the layer has become visible by the globe altitude moving within the layers thresholds
    @Transient
    private Boolean altitudeInView;

    @Inject
    @Transient
    private IGlobeManager globeManager;

    /**
     * Indicates if this layer is currently being rendered on the screen.
     *
     * @since 1.20
     */
    @Transient
    private Boolean isRendered = false;

    @SuppressWarnings("UnusedDeclaration")
    public RaptorLayer()
    {
        key = null;
    }

    /**
     * @since 5.0
     */
    public RaptorLayer(String name, String displayName, String className, String configFileName, boolean visible,
                       boolean pickable, String iconPath, LayerGroup layerGroup, String type, String restorableXMLString,
                       boolean permanent, double splitScale, double minActiveAltitude, double maxActiveAltitude,
                       boolean networkRetrievalEnabled)
    {
        final boolean configFileEmpty = configFileName == null || configFileName.isEmpty();
        final boolean classNameEmpty = className == null || className.isEmpty();
        if (classNameEmpty == configFileEmpty)
        {
            throw new IllegalArgumentException("Exactly one of className or configFileName must be specified.");
        }

        //todo: change to something unique
        // Note, don't be too cavalier about changing the key... there are quite a few places in TAKX that rely
        // on the key being the same name as the name when looking up a layer (IRaptorLayerManager.getLayer(String key))
        key = name;
        this.name = name;
        this.displayName = displayName == null ? name : displayName;
        this.className = className;
        this.configFileName = configFileName;
        this.pickable = pickable;
        // Fall back to our standard globe icon if the icon path is null/empty
        this.iconPath = StringUtils.isEmpty(iconPath) ? "resources/globe.png" : iconPath;
        this.layerGroup = layerGroup;
        this.visible = visible;
        this.type = type;
        restoreStateXML = restorableXMLString;
        this.permanent = permanent;
        this.splitScale = splitScale;
        this.networkRetrievalEnabled = networkRetrievalEnabled;
        this.minActiveAltitude = minActiveAltitude;
        this.maxActiveAltitude = maxActiveAltitude;

        if (logger.isTraceEnabled())
        {
            logger.trace("New Layer: {}, cn = '{}' config='{}' lg={}, pickable={}, visible={}, perm={}, icon={}, type={}, minActiveAltitude={}, maxActiveAltitude={}", name, className, configFileName, layerGroup, pickable, visible, permanent, iconPath, type, minActiveAltitude, maxActiveAltitude);
        }
    }

    /**
     * @since 5.2
     */
    public RaptorLayer(com.atakmap.map.layer.Layer layer, boolean visible, boolean pickable, String iconPath,
                       LayerGroup layerGroup, String type, String restorableXMLString,
                       boolean permanent, double splitScale, double minActiveAltitude, double maxActiveAltitude)
    {
        this(layer.getName(), layer.getName(), layer.getClass().getName(), null, visible, pickable, iconPath,
                layerGroup, type, restorableXMLString, permanent, splitScale, minActiveAltitude, maxActiveAltitude,
                true);

        takConstructionLayer = layer;
    }

    public RaptorLayer(Layer layer, boolean visible, boolean pickable, String iconPath,
                       LayerGroup layerGroup, String type, String restorableXMLString,
                       boolean permanent, double splitScale, double minActiveAltitude, double maxActiveAltitude)
    {
        this(layer.getName(), layer.getName(), layer.getClass().getName(), null, visible, pickable, iconPath,
                layerGroup, type, restorableXMLString, permanent, splitScale, minActiveAltitude, maxActiveAltitude,
                layer.isNetworkRetrievalEnabled());

        constructionLayer = layer;
    }

    /**
     * Unsupported constructor. Use of {@link ElevationModel} in RaptorLayer is no longer supported.
     *
     * @deprecated use {@link #RaptorLayer(String, String, String, String, boolean, boolean, String, LayerGroup, String, String, boolean, double, double, double, boolean)}
     */
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    @Deprecated(since = "5.6", forRemoval = true)
    public RaptorLayer(ElevationModel model, boolean visible, boolean pickable, String iconPath,
                       LayerGroup layerGroup, String type, String restorableXMLString,
                       boolean permanent, double splitScale, double minActiveAltitude, double maxActiveAltitude)
    {
        this(model.getName(), model.getName(), model.getClass().getName(), null, visible, pickable, iconPath,
                layerGroup, type, restorableXMLString, permanent, splitScale, minActiveAltitude, maxActiveAltitude,
                model.isNetworkRetrievalEnabled());
    }

    @Override
    public boolean isSharable()
    {
        return specificGlobeId == ALL_GLOBES;
    }

    @Override
    public void setSpecificGlobeId(int globeId)
    {
        specificGlobeId = globeId;
    }

    @Override
    public int getSpecificGlobeId()
    {
        return specificGlobeId;
    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        changeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void addPropertyChangeListener(@Nonnull String property, @Nonnull PropertyChangeListener listener)
    {
        changeSupport.addPropertyChangeListener(property, listener);
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        changeSupport.removePropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(@Nonnull String property, @Nonnull PropertyChangeListener listener)
    {
        changeSupport.removePropertyChangeListener(property, listener);
    }

    @Override
    public void removeAllRaptorLayerListeners()
    {
        changeSupport.removeAllListeners();
    }

    @Override
    public String getKey()
    {
        return key;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public boolean inViewAltitudeChanged(double altitude)
    {
        if (altitudeInView == null)
        {
            altitudeInView = altitudeWithinThreshold(altitude);
            return true;
        } else
        {
            boolean oldAltitudeInView = altitudeInView;
            altitudeInView = altitudeWithinThreshold(altitude);

            return oldAltitudeInView != altitudeInView;
        }
    }

    @Override
    public boolean layerIsRenderedChanged(boolean isRendered)
    {
        if (this.isRendered == null)
        {
            this.isRendered = isRendered;
            return true;
        } else
        {
            boolean oldRenderedValue = this.isRendered;
            this.isRendered = isRendered;

            return oldRenderedValue != isRendered;
        }
    }

    @Override
    public boolean altitudeWithinThreshold(double altitude)
    {
        return altitude <= maxActiveAltitude && altitude >= minActiveAltitude;
    }

    @Override
    public boolean isLayerRendered()
    {
        return isRendered;
    }

    @Override
    public void setDisplayName(String displayName)
    {
        String oldVal = this.displayName;
        this.displayName = displayName;
        changeSupport.firePropertyChange(DISPLAY_NAME, oldVal, displayName);
    }

    @Override
    public String getType()
    {
        return type;
    }

    @Override
    public String getClassName()
    {
        return className;
    }

    @Override
    public String getConfigFileName()
    {
        return configFileName;
    }

    @Override
    public Layer getConstructionlayer()
    {
        return constructionLayer;
    }

    @Override
    public com.atakmap.map.layer.Layer getTakConstructionLayer()
    {
        return takConstructionLayer;
    }

    @Override
    public ImageIcon getSmallIcon()
    {
        if (smallIcon == null)
        {
            smallIcon = getIcon(ImageUtils.SMALL_ICON_WIDTH, ImageUtils.SMALL_ICON_HEIGHT);
        }
        return smallIcon;
    }

    @Override
    public ImageIcon getIcon(int width, int height)
    {
        // TODO KMB: Update GlobeLayers.xml to use a URI (to allow specifying file, jar, etc.) and move current icons
        //  to the platform resources (TAKX-1217)
        return ImageUtils.getScaledIcon(new File(iconPath), width, height, false);
    }

    @Override
    public void setNetworkRetrievalEnabled(boolean networkRetrievalEnabled)
    {
        boolean oldVal = this.networkRetrievalEnabled;
        this.networkRetrievalEnabled = networkRetrievalEnabled;
        changeSupport.firePropertyChange(NETWORK_RETRIEVABLE, oldVal, networkRetrievalEnabled);
    }

    @Override
    public boolean isEditable()
    {
        return editable;
    }

    @Override
    public void setEditable(boolean editable)
    {
        boolean oldVal = this.editable;
        this.editable = editable;

        changeSupport.firePropertyChange(EDITABLE, oldVal, editable);
    }

    @Override
    public boolean isVisible()
    {
        return visible;
    }

    @Override
    public boolean isPickable()
    {
        return pickable;
    }

    @Override
    public double getOpacity()
    {
        return opacity;
    }

    @Override
    public double getMinActiveAltitude()
    {
        return minActiveAltitude;
    }

    @Override
    public double getMaxActiveAltitude()
    {
        return maxActiveAltitude;
    }

    @Override
    public boolean isNetworkRetrievalEnabled()
    {
        return networkRetrievalEnabled;
    }

    @Override
    public LayerGroup getLayerGroup()
    {
        return layerGroup;
    }

    @Override
    public String toString()
    {
        return displayName != null ? displayName : name;
    }

    @Override
    public boolean isMaxActiveAltitudeEnabled()
    {
        return maxActiveAltitudeEnabled;
    }

    @Override
    public boolean isMinActiveAltitudeEnabled()
    {
        return minActiveAltitudeEnabled;
    }

    @Override
    public void setName(@Nonnull String name)
    {
        String oldVal = this.name;
        this.name = name;
        changeSupport.firePropertyChange(NAME, oldVal, name);
    }

    @Override
    public void setVisible(boolean visible)
    {
        boolean oldVal = this.visible;
        this.visible = visible;
        changeSupport.firePropertyChange(VISIBLE, oldVal, visible);
    }

    @Override
    public void setPickable(boolean pickable)
    {
        boolean oldVal = this.pickable;
        this.pickable = pickable;
        changeSupport.firePropertyChange(PICKABLE, oldVal, pickable);
    }

    @Override
    public void setOpacity(double opacity)
    {
        double oldVal = this.opacity;
        this.opacity = opacity;
        changeSupport.firePropertyChange(OPACITY, oldVal, opacity);
    }

    @Override
    public void setMinActiveAltitude(double min)
    {
        double oldVal = minActiveAltitude;
        minActiveAltitude = min;
        changeSupport.firePropertyChange(MIN_ACTIVE_ALTITUDE, oldVal, min);
    }

    @Override
    public void setMaxActiveAltitude(double max)
    {
        if (logger.isTraceEnabled()) logger.trace("setting max active altitude for layer {} to: {}", getName(), max);
        double oldVal = maxActiveAltitude;
        maxActiveAltitude = max;
        changeSupport.firePropertyChange(MAX_ACTIVE_ALTITUDE, oldVal, max);
    }

    @Override
    public void setMaxActiveAltitudeEnabled(boolean b)
    {
        maxActiveAltitudeEnabled = b;
    }

    @Override
    public void setMinActiveAltitudeEnabled(boolean b)
    {
        minActiveAltitudeEnabled = b;
    }

    @Override
    public double getSplitScale()
    {
        return splitScale;
    }

    @Override
    public void setSplitScale(double splitScale)
    {
        double oldVal = this.splitScale;
        this.splitScale = splitScale;
        changeSupport.firePropertyChange(SPLIT_SCALE, oldVal, splitScale);
    }

    @Override
    public Integer getId()
    {
        return id;
    }

    @Override
    public void setRestorableState(IRestorable o)
    {
        restorableLayer = o;
    }

    @Override
    public String getRestorableState()
    {
        return restoreStateXML;
    }

    @PrePersist
    @PreUpdate
    private void onSave()
    {
        if (restorableLayer != null) restoreStateXML = restorableLayer.getRestorableState();

        logger.debug("onSave/Update: restoreState={}", restoreStateXML);
    }

    @PostRemove
    private void onDelete()
    {
        changeSupport.removeAllListeners();
    }

    @Override
    public Integer getOrderkey()
    {
        return orderkey;
    }

    @Override
    public void setOrderkey(Integer orderkey)
    {
        this.orderkey = orderkey;
    }

    @Override
    public boolean isPermanent()
    {
        return permanent;
    }

    @Override
    public void setPermanent(boolean permanent)
    {
        this.permanent = permanent;
    }

    @Override
    public boolean isDeleted()
    {
        return deleted;
    }

    @Override
    public void setDeleted(boolean deleted)
    {
        boolean oldValue = this.deleted;
        this.deleted = deleted;
        changeSupport.firePropertyChange(DELETED, oldValue, deleted);
    }

    @Override
    public Layer getLayer()
    {
        final IGlobeLayer layer = isSharable()
                // If it's shared, we can access it through the primary globe.
                ? globeManager.getPrimaryGlobeDelegate().getGlobeLayer(this)
                // Otherwise, get the globe layer manager specific to the globe associated with this layer.
                : globeManager.getGlobeDelegate(getSpecificGlobeId()).getGlobeLayer(this);

        return layer != null ? layer.getLayer() : null;
    }

    @Override
    public Sector getSector()
    {
        Sector sector = null;
        boolean validSector = false;

        // We need to get the construction parameters for the layer and then extract the sector from there.
        // Get parameters
        final Layer layer = getLayer();
        final LayerGroup layerGroup = getLayerGroup();
        AVList params = layer != null ? (AVList) layer.getValue(AVKey.CONSTRUCTION_PARAMETERS) : null;

        if (params != null)
        {
            final Object sectorValue = params.getValue(AVKey.SECTOR);

            if (sectorValue instanceof Sector)
            {
                sector = (Sector) sectorValue; // It's a valid sector
                Angle deltaLat = sector.getDeltaLat();
                Angle deltaLon = sector.getDeltaLon();

                validSector = deltaLat.getDegrees() < 90.0 && deltaLon.getDegrees() < 90.0;

                if (!validSector && layerGroup == LayerGroup.SURFACE_STRUCTURES && layer instanceof GeographicExtent)
                {
                    sector = ((GeographicExtent) layer).getSector();
                    deltaLat = sector.getDeltaLat();
                    deltaLon = sector.getDeltaLon();
                    validSector = deltaLat.getDegrees() < 90.0 && deltaLon.getDegrees() < 90.0;
                }
            }
        }

        return validSector ? sector : null;
    }

    /**
     * @return icon path
     */
    @Override
    public String getIconPath()
    {
        return iconPath;
    }

    /**
     * Marks this layer as one managed by the core. If a system-managed layer cannot be restored when a project is
     * reopened, it can be removed.
     * 
     * @since 5.0
     */
    public void setSystemManaged()
    {
        systemManaged = true;
    }

    /**
     * @return {@code true} if the layer is managed by the core and should be removed if stale
     * @since 5.0
     */
    public boolean isSystemManaged()
    {
        return systemManaged;
    }
}
