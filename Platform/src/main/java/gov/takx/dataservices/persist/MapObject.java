package gov.takx.dataservices.persist;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.icons.IIconSetManager;
import gov.tak.api.maps.MapItem;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.symbology.Affiliation;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.platform.cot.util.AccessUtils;
import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.dataportal.IRaptorDataPortal;
import gov.takx.api.drifter.DrifterConfiguration;
import gov.takx.api.filesystem.IVFSFile;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.mapobject.ElevationMode;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectImageUtils;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.ITrackInfo;
import gov.takx.api.mapobject.MapObjectGroupNames;
import gov.takx.api.mapobject.MapObjectOptions;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.IStructure;
import gov.takx.api.milsym.CotSidcAdapter;
import gov.takx.api.milsym.IMilStd2525ServiceProvider;
import gov.takx.api.persist.IExtendedLifecycle;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.commons.constants.State;
import gov.takx.commons.file.FilePath;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.commons.lang.IPropertyChangeDelegateSupport;
import gov.takx.commons.lang.IPropertyChangeListenerSupport;
import gov.takx.commons.swing.CanonicalImageCache;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.dataservices.shapes.FieldOfView;
import gov.takx.dataservices.utils.AttributeSetConverter;
import gov.takx.platform.KeyParts;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.utils.PrioritizedPropertyChangeSupport;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostRemove;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.Length;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static gov.takx.api.milsym.MilStd2525Variant.CHARLIE;

/**
 */
@SuppressWarnings("NewClassNamingConvention")
@Internal
@PseudoBean
@Entity
@Table(indexes = @Index(name = "MapObject_uniqueKey", columnList = "uniqueKey", unique = true))
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class MapObject implements IMapObject, IExtendedLifecycle, IPropertyChangeDelegateSupport
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Transient
    @Inject
    protected IMapObjectManager mapObjectManager;

    @Transient
    @Inject
    protected IRaptorDataPortal dataPortal;

    /** @since 5.5 */
    @Transient
    @Inject
    protected IMilStd2525ServiceProvider milStd2525ServiceProvider;

    @Transient
    @Inject
    protected IMapObjectImageUtils mapObjectImageUtils;

    @Transient
    @Inject
    @VFSManagerScope(VFSScope.Project)
    protected IVirtualFileSystemManager projectVFSManager;

    @Transient
    @Inject
    protected IMapObjectPersist mapObjectPersist;

    @Transient
    @Inject
    private IIconSetManager iconSetManager;

    @Transient
    protected final PrioritizedPropertyChangeSupport propertyChangeSupport = new PrioritizedPropertyChangeSupport(this);

    @Transient
    private final PropertyChangeListener fovPropertyRefireListener = this::fieldOfViewPropertyChange;

    @Transient
    protected BufferedImage iconImage;

    private static final int DEFAULT_ICON_SELECTOR = 0;
    private static final int SMALL_ICON_SELECTOR = 1;
    private static final int MEDIUM_ICON_SELECTOR = 2;
    private static final int LARGE_ICON_SELECTOR = 3;
    private static final int[] ICON_SIZES = {ImageUtils.DEFAULT_ICON_WIDTH, ImageUtils.SMALL_ICON_WIDTH, ImageUtils.MEDIUM_ICON_WIDTH, ImageUtils.LARGE_ICON_WIDTH};

    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final File[] EMPTY_FILE_ARRAY = new File[0];

    // Cache of BufferedImage instances to reduce overall memory usage when the same image is used for
    // numerous map objects, or a set of images are cycled on a given map object.
    //
    // Enable cache statistics if DEBUG is enabled
    //
    private static final CanonicalImageCache IMAGE_CACHE = new CanonicalImageCache(logger.isDebugEnabled());

    @Transient
    private final ImageIcon[] sizedIcons = new ImageIcon[ICON_SIZES.length];

    // Handlers to be notified when this MO is deleted (see the shutdown() method)
    @Transient
    private final ConcurrentLinkedQueue<Consumer<MapItem>> deletionHandlers = new ConcurrentLinkedQueue<>();

    @Transient
    private State highestState = State.Unknown;
    @Transient
    private boolean inPlaybackMode;
    @Transient
    private volatile String family;
    @Transient
    private volatile String type;

    @Transient
    private CotEvent latestCotEvent; // The latest received CoT event

    @Transient
    protected GeoPoint geoPoint;

    @Transient
    private IMapEntity mapEntity;

    @Transient
    private MapObjectSpec mapObjectSpec;

    @Transient
    protected IPluginDefinition<? extends IRaptorPlugin> pluginDefinition;

    @Transient
    protected URI imageURI_t;

    @Transient
    private IMapObject owner; // MapObject that owns this MapObject

    @Transient
    private boolean isDevice;

    @Transient
    private boolean restoring;

    @JdbcTypeCode(SqlTypes.JSON)
    private FieldOfView fieldOfView;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Unique Id, can only be set during construction
    private String uid;
    private String uniqueKey; // No longer transient to improve performance on MO existence checks

    @Embedded
    private MapObjectOptions mapObjectOptions;

    private boolean allowDeviceContainment = true;

    private String name;
    private String unitID;
    private String cotType;

    // This is the actual persisted link between owner/owned, it is the id of the MO that owns this MO.
    private Integer ownerId;

    @Column(length = Length.LONG32)
    private String remarks;

    private String description;

    @Convert(converter = AttributeSetConverter.class)
    @Column(length = Length.LONG32)
    protected AttributeSet attributeSet;

    @Embedded
    protected GeoPointMetaData geoPointMetaData;

    @JdbcTypeCode(SqlTypes.JSON)
    private DrifterConfiguration drifterConfiguration;

    private String groupName;
    private String displayableType;
    protected String imageURI;
    private boolean visible = true;
    private boolean selectable = true;
    private boolean allowDrag = true;
    private long mostRecentCommunicationTime = -1;
    private double iconScale = 1.0;
    private double heading = Double.NaN;
    private double speed = Double.NaN;
    private int defaultTrackColor;
    private long oldestCommunicationTime = -1;
    private boolean snapIconToSurface = true;
    private String defaultPreferencesTab;
    private ElevationMode elevationMode = ElevationMode.MSL;

    /**
     * Contains the metadata describing the set of tracks owned by this map object.
     *
     * @since 1.13
     */
    @Embedded
    private TrackInfo trackInfo;

    /**
     * No-arg constructor for Hibernate.
     */
    protected MapObject()
    {
    }

    protected MapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        init(mapObjectSpec, pluginDefinition);
    }

    @PostConstruct
    private void postConstruct()
    {
        // Handle new MO creation vs. being reified from persistence
        if (family != null && type != null)
        {
            // Reconnect our MapEntity
            mapEntity = mapObjectPersist.findMapEntity(family, type);

            // If we are defined by an icon set plugin definition, then we need to set imageURI (since that is persisted)
            // and we need that to recreate our plugin definition when the project is reopened.
            if (pluginDefinition != null && family.startsWith(KeyUtils.RESOURCE_ICON_FAMILY) && iconSetManager.isIconSetUri(imageURI_t))
            {
                imageURI = imageURI_t.toString();
            }
        }

        // Set our owner from the value in the spec.
        if (mapObjectSpec != null) setOwner(mapObjectSpec.owner());
    }

    /**
     * Initialize the internal state of this MO.
     *
     * @param mapObjectSpec    The MO specification
     * @param pluginDefinition Our plugin configuration, may be {@code null}
     */
    private void init(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        this.mapObjectSpec = mapObjectSpec;

        family = mapObjectSpec.family();
        type = mapObjectSpec.type();
        unitID = mapObjectSpec.unitId();
        displayableType = type;
        uniqueKey = mapObjectSpec.getKey();
        name = mapObjectSpec.name();
        mapObjectOptions = mapObjectSpec.mapObjectOptionsOrDefault();

        final String description = mapObjectSpec.description();
        if (description != null) setDescription(description);

        // Initialize our location, giving precedence to geoPoint
        final GeoPoint specGeoPoint = mapObjectSpec.geoPoint();
        final ILocation specLocation = mapObjectSpec.location();

        if (specGeoPoint != null)
        {
            geoPoint = new GeoPoint(specGeoPoint, GeoPoint.Access.READ_WRITE);
        } else if (specLocation != null)
        {
            geoPoint = GeoPoint.createMutable().set(specLocation.getLatitude(), specLocation.getLongitude(), specLocation.getAltitude());
        } else
        {
            geoPoint = GeoPoint.createMutable();
        }
        geoPointMetaData = new GeoPointMetaData(geoPoint);

        if (pluginDefinition != null)
        {
            this.pluginDefinition = pluginDefinition;

            groupName = pluginDefinition.getGroup();
            iconImage = pluginDefinition.getIcon(); // No reason to run this through the IMAGE_CACHE
            imageURI_t = pluginDefinition.getIconURI().orElse(null);
        }

        if (MapObjectGroupNames.DEVICES_GROUP.equalsIgnoreCase(groupName))
        {
            isDevice = true;
            allowDeviceContainment = false;
        }

        defaultTrackColor = ImageUtils.generateRandomColor().getRGB(); // Set the default color for this map object's track points
        trackInfo = new TrackInfo();

        attributeSet = new AttributeSet();
        final AttributeSet specAttributeSet = mapObjectSpec.attributeSet();
        if (specAttributeSet != null) attributeSet.putAll(specAttributeSet);

        // Translate PCEs on the AttributeSet to PCEs on this object
        attributeSet.addPropertyChangeListener(pce -> propertyChangeSupport.firePropertyChange(ATTRIBUTE_SET, null, attributeSet));

        final String specUid = mapObjectSpec.uid();
        uid = specUid != null ? specUid : UUID.randomUUID().toString();
        attributeSet.setAttribute(StandardAttributeNames.UID, uid);

        final String specCotType = mapObjectSpec.cotType();
        setCotType(specCotType != null ? specCotType : CotTypes.EMPTY);

        final CotEvent cotEvent = mapObjectSpec.cotEvent();
        if (cotEvent != null)
        {
            setLatestCotEvent(cotEvent);
        } else
        {
            AccessUtils.setAccessDefault(this);
        }

        initializeIcons();
    }

    /**
     * Called by Hibernate after this MapObject is loaded
     */
    @PostLoad
    protected void onLoad()
    {
        restoring = true;

        final KeyParts keyParts = new KeyParts(uniqueKey);
        family = keyParts.getFamily();
        type = keyParts.getType();

        // Reconnect our MapEntity
        mapEntity = mapObjectPersist.findMapEntity(family, type);

        // Restore our PluginDefinition
        pluginDefinition = mapObjectManager.getPluginDefinition(family, type).orElse(null);

        // Try to recreate our synthetic plugin definition if we're defined by an icon set
        if (pluginDefinition == null && family.startsWith(KeyUtils.RESOURCE_ICON_FAMILY) && imageURI != null)
        {
            try
            {
                final URI uri = new URI(imageURI);
                if (iconSetManager.isIconSetUri(uri)) pluginDefinition = iconSetManager.getOrCreatePluginDefinition(uri);
                logger.debug("recreated SPD: {}", pluginDefinition);
            } catch (URISyntaxException | IllegalArgumentException e)
            {
                logger.warn("Invalid URI '{}' for {}", imageURI, uniqueKey, e);
            }
        }

        if (imageURI != null) imageURI_t = URI.create(imageURI);

        // Restore the isDevice status based on the group name
        isDevice = MapObjectGroupNames.DEVICES_GROUP.equals(getGroupName());

        // 'geoPoint' is not persisted directly. 'geoPointMetaData' maintains a read-only copy of it (and that
        // is what gets persisted).  So, we need to reconstitute our mutable geoPoint.
        geoPoint = new GeoPoint(geoPointMetaData.get(), GeoPoint.Access.READ_WRITE);

        final String cotEventXml = attributeSet.getStringAttribute(StandardAttributeNames.COT_EVENT_XML, "");
        if (!cotEventXml.isEmpty()) latestCotEvent = CotEvent.parse(cotEventXml);

        // Translate PCEs on the AttributeSet to PCEs on this object
        attributeSet.addPropertyChangeListener(pce -> propertyChangeSupport.firePropertyChange(ATTRIBUTE_SET, null, attributeSet));
        if (fieldOfView != null) fieldOfView.addPropertyChangeListener(fovPropertyRefireListener);
    }

    @Override
    public void onAllLoaded()
    {
        // Reconnect our owner, if any.
        relinkMapObject(ownerId, "owner", this::setOwner);
    }

    /**
     * Re-links a map object with the provided id to this one by performing the supplied operation if the MO is found.
     *
     * @param mapObjectId The id of the map object to relink (may be {@code null} if no object to relink)
     * @param nameForLog  The name to describe the map object (e.g. owner, center anchor, etc.); used in error log
     * @param handler     A handler operation to perform the necessary logic if the map object is found
     * @since 3.0
     */
    protected void relinkMapObject(@Nullable Integer mapObjectId, @Nonnull String nameForLog,
                                   @Nonnull Consumer<IMapObject> handler)
    {
        IMapObject mapObject = findMapObjectById(mapObjectId, nameForLog, IMapObject.class);

        if (mapObject != null) handler.accept(mapObject);
    }

    /**
     * Find a map object given its id and expected type.
     *
     * @param mapObjectId  The id of the map object to find (may be {@code null} if no object to find)
     * @param nameForLog   The name to describe the map object (e.g. owner, center anchor, etc.); used in error log
     * @param expectedType The expected type of the map object
     * @param <T>          The type of map object expected
     * @return The map object, or {@code null} if provided id was {@code null}, MO was not found, or MO was not the right type.
     * @since 3.0
     */
    @Nullable
    protected <T extends IMapObject> T findMapObjectById(@Nullable Integer mapObjectId, @Nonnull String nameForLog,
                                                         @Nonnull Class<T> expectedType)
    {
        if (mapObjectId == null) return null;
        return processFoundMapObject(mapObjectPersist.findMapObjectById(mapObjectId), nameForLog, "id", mapObjectId, expectedType);
    }

    /**
     * Processes a found map object to perform logging and convert it to the correct type.
     *
     * @param mapObject        The found map object (may be {@code null})
     * @param nameForLog       The name to describe the map object (e.g. owner, center anchor, etc.); used in error log
     * @param lookupTypeForLog The type of lookup performed (e.g. id or key); used in error log
     * @param identifier       The identifier used for the lookup; used in error log
     * @param <T>              The type of map object expected
     * @return The map object, or {@code null} if MO was not found or MO was not the right type.
     */
    private <T extends IMapObject> T processFoundMapObject(@Nullable IMapObject mapObject, @Nonnull String nameForLog,
                                                           @Nonnull String lookupTypeForLog, @Nonnull Object identifier,
                                                           @Nonnull Class<T> expectedType)
    {
        if (mapObject == null)
        {
            logger.error("Failed to locate {} map object for {} '{}'", nameForLog, lookupTypeForLog, identifier);
        } else
        {
            final Class<? extends IMapObject> mapObjectClass = mapObject.getClass();
            if (expectedType.isAssignableFrom(mapObjectClass))
            {
                return expectedType.cast(mapObject);
            } else
            {
                logger.error("Found {} map object for {} '{}', but not the right type. Expected type: {}; actual type: {}",
                        nameForLog, lookupTypeForLog, identifier, expectedType, mapObjectClass);
            }
        }

        return null;
    }

    /**
     * Find a map object given its key and expected type.
     *
     * @param <T>          The type of map object expected
     * @param key          Key of map object to find
     * @param nameForLog   The name to describe the map object (e.g. owner, center anchor, etc.); used in error log
     * @param expectedType The expected type of the map object
     * @return The map object, or {@code null} if provided key was {@code null}, MO was not found, or MO was not the right type.
     * @since 3.0
     */
    @Nullable
    protected <T extends IMapObject> T findMapObjectByKey(@Nullable String key, @Nonnull String nameForLog, Class<T> expectedType)
    {
        if (key == null) return null;
        return processFoundMapObject(mapObjectPersist.findMapObject(key), nameForLog, "key", key, expectedType);
    }

    /**
     * Find the map objects for the comma-separated string of ids.
     *
     * @param csvIds comma-separated string of ids
     * @param <T>    The type of map object expected
     * @return List of map objects (may be empty if none found)
     * @since 3.0
     */
    @Nonnull
    protected <T extends IMapObject> List<T> findMapObjectsByCsvIds(@Nonnull String csvIds)
    {
        //noinspection unchecked
        return IntStream.of(StringUtils.splitToInt(csvIds, -1))
                .filter(id -> id > 0)
                .mapToObj(moId -> (T) mapObjectPersist.findMapObjectById(moId))
                .filter(Objects::nonNull) // It shouldn't happen, but if the find fails, it returns null
                .collect(Collectors.toList());
    }

    /**
     * Encodes the provided map objects as a comma-separated string of ids.
     *
     * @param mapObjects The map object to encode
     * @return Comma-separated string of map object ids
     * @since 3.0
     */
    @Nonnull
    protected String encodeMapObjectsAsCsvIds(@Nonnull Collection<? extends IMapObject> mapObjects)
    {
        return mapObjects.stream()
                .map(mo -> Integer.toString(mo.getId()))
                .collect(Collectors.joining(","));
    }

    @Override
    public boolean isRestoring()
    {
        return restoring;
    }

    @Nonnull
    @Override
    public AttributeSet getAttributeSet()
    {
        return attributeSet;
    }

    @Nonnull
    @Override
    public String getUid()
    {
        return uid;
    }

    @Nonnull
    @Override
    public String getCotType()
    {
        return cotType;
    }

    @Override
    public void setCotType(@Nonnull String cotType)
    {
        propertyChangeSupport.updateAndFire(COT_TYPE, this.cotType, cotType, () -> {
            this.cotType = cotType;
            attributeSet.setAttribute(StandardAttributeNames.COT_TYPE, cotType);
        });
    }

    @Nullable
    @Override
    public CotEvent getLatestCotEvent()
    {
        return latestCotEvent;
    }

    @Override
    public void setLatestCotEvent(@Nonnull CotEvent cotEvent)
    {
        latestCotEvent = cotEvent;
        if (latestCotEvent.isValid()) attributeSet.setAttribute(StandardAttributeNames.COT_EVENT_XML, latestCotEvent.toString());
    }

    @Override
    public void setFieldOfView(@Nullable FieldOfView fieldOfView)
    {
        if (fieldOfView == this.fieldOfView) return;
        propertyChangeSupport.firePropertyChange(FIELD_OF_VIEW, this.fieldOfView, fieldOfView);
        // Has to be done separately or the globe rendered FOV won't be removed
        // Clean up old fov
        if (this.fieldOfView != null)
        {
            this.fieldOfView.removePropertyChangeListener(fovPropertyRefireListener);
            this.fieldOfView.releaseResources();
        }

        // Update
        this.fieldOfView = fieldOfView;
        if (this.fieldOfView != null) this.fieldOfView.addPropertyChangeListener(fovPropertyRefireListener);
    }

    @Nullable
    @Override
    public FieldOfView getFieldOfView()
    {
        return fieldOfView;
    }

    /**
     * Refire the event values as an event that is monitored.
     * @param evt the PCE of what was updated in the FOV
     * @since 5.8
     */
    private void fieldOfViewPropertyChange(PropertyChangeEvent evt)
    {
        propertyChangeSupport.firePropertyChange(FIELD_OF_VIEW_PROPERTIES, evt.getOldValue(), evt.getNewValue());
    }

    @Override
    public IMapObjectController getController()
    {
        return mapObjectManager.getMapObjectController(id);
    }

    @Override
    public IPluginDefinition<? extends IRaptorPlugin> getPluginDefinition()
    {
        return pluginDefinition;
    }

    @Override
    public boolean isDevice()
    {
        return isDevice;
    }

    /**
     * Reset (null out) the images cached for this Map Object.
     */
    protected void resetImageBuffers()
    {
        iconImage = null;

        Arrays.fill(sizedIcons, null); // Null out all scaled images
    }

    @PostRemove
    protected void onDelete()
    {
        logger.debug("onDelete: {}", uniqueKey);
        if (!getAttachments().isEmpty()) deleteAttachments(getAttachments().toArray(EMPTY_FILE_ARRAY));
        shutdown(); // Cleanup listeners and image buffers to help with GC
    }

    @Override
    public Integer getId()
    {
        return id;
    }

    @Override
    public void addDeletionHandler(Consumer<MapItem> handler)
    {
        deletionHandlers.add(handler);
    }

    @Override
    public void removeDeletionHandler(Consumer<MapItem> handler)
    {
        deletionHandlers.remove(handler);
    }

    @Override
    public void notifyDeletionHandlers()
    {
        ExceptionUtils.safeDoOperation(deletionHandlers, h -> h.accept(this));
    }

    @Override
    public void shutdown()
    {
        propertyChangeSupport.removeAllListeners();
        attributeSet.dispose();
        resetImageBuffers();

        if (fieldOfView != null)
        {
            fieldOfView.removePropertyChangeListener(fovPropertyRefireListener);
            fieldOfView.releaseResources();
        }
    }

    @Override
    public void setVisible(boolean visible)
    {
        propertyChangeSupport.updateAndFire(VISIBILITY, this.visible, visible, () -> this.visible = visible);
    }

    /**
     * Set the location for this map object by sending a location change message.
     *
     * @param location The new location
     * @since 3.0
     */
    protected void setLocation(ILocation location)
    {
        if (!getLocation().sameAs(location))
        {
            final IMapObjectController controller = getController();
            if (controller != null)
            {
                controller.sendLocationChangeMessage(location);
            } else
            {
                // This can happen in tests or if setLocation is called in the initializer of a MapObjectSpec, since
                // that occurs before the controller is set. Usually, we want to use the sendLocationChangeMessage
                // call to set the location, but it's ok not to in this case because the map object is still in the
                // process of being created.
                setLocation(location, false);
            }
        }
    }

    @Override
    public void setGeoPointMetaData(@Nonnull GeoPointMetaData geoPointMetaData)
    {
        final var oldGeoPointMetaData = new GeoPointMetaData(geoPointMetaData); // Save a copy

        // Remove old point metadata
        for (String name : this.geoPointMetaData.getMetaData().getAttributeNames()) attributeSet.removeAttribute(name);

        // Copy in new point metadata
        attributeSet.putAll(geoPointMetaData.getMetaData());

        // Install the new location
        setLocation(geoPointMetaData.get(), false);

        // Changing the GeoPoint clears the geoPointMetaData attributes, so copy them AFTER calling setLocation
        final var metaData = this.geoPointMetaData.getMetaData();
        metaData.putAll(geoPointMetaData.getMetaData());
        metaData.setAttribute(StandardAttributeNames.BEARING, heading);
        metaData.setAttribute(StandardAttributeNames.SPEED, speed);

        propertyChangeSupport.firePropertyChange(GEOPOINT, oldGeoPointMetaData, this.geoPointMetaData);
    }

    @Override
    public GeoPointMetaData getGeoPointMetaData()
    {
        return new GeoPointMetaData(geoPointMetaData);
    }

    @Override
    public boolean setLocation(ILocation location, long time, boolean manualLocation)
    {
        return internalSetLocation(location, time, manualLocation);
    }

    /**
     * Set the location of this map object.
     *
     * @param location       New location, ignored if {@code null}
     * @param time           time location was specified (usually from an RDMs relevant time)
     * @param manualLocation Pass {@code true} if this was a manually generated location (like a user manually
     *                       moved the icon on the globe).
     * @return {@code true} if the location was actually changed, {@code false} otherwise
     * @since 5.5 - signature changed
     */
    protected boolean internalSetLocation(ILocation location, long time, boolean manualLocation)
    {
        if (location == null) return false;

        // set oldest and most recent communication times, if not manually located
        if (!manualLocation) updateCommunicationTimes(time);

        // if it is a newer location than any other data, set location
        final boolean newMsg = time >= mostRecentCommunicationTime;

        if (manualLocation || newMsg)
        {
            final GeoPoint oldValue = geoPointMetaData.get();

            if (location instanceof final RaptorLocation raptorLocation)
            {
                if (geoPoint.sameAs(raptorLocation)) return false;

                geoPoint.set(raptorLocation);
            } else if (location instanceof GeoPoint)
            {
                if (geoPoint.equals(location)) return false;

                geoPoint.set((GeoPoint) location);
            }

            geoPointMetaData.set(geoPoint);
            propertyChangeSupport.firePropertyChange(LOCATION, oldValue, geoPoint);

            if (fieldOfView != null) fieldOfView.setPoint(geoPoint);

            return true;
        }
        return false;
    }

    /**
     * Update the oldest/newest communication times.  Fire property change events if the value is updated.
     *
     * @param time time of the communication
     * @since 3.0
     */
    private void updateCommunicationTimes(long time)
    {
        final long currentOldest = oldestCommunicationTime;
        if (time < currentOldest || currentOldest == -1)
        {
            oldestCommunicationTime = time;
            propertyChangeSupport.firePropertyChange(OLDEST_COMMUNICATION_TIME, currentOldest, time);
        }

        final long currentNewest = mostRecentCommunicationTime;
        if (time > currentNewest || currentNewest == -1)
        {
            mostRecentCommunicationTime = time;
            propertyChangeSupport.firePropertyChange(MOST_RECENT_COMMUNICATION_TIME, currentNewest, time);
        }
    }

    @Override
    public void setName(@Nonnull String name)
    {
        propertyChangeSupport.updateAndFire(NAME, this.name, name, () -> this.name = name);
    }

    @Override
    public String getTitle()
    {
        final String title = IMapObject.super.getTitle();
        return title != null ? title : getName();
    }

    /**
     * {@inheritDoc}
     * @since 3.5
     */
    @Override
    public void setRemarks(@Nonnull String remarks)
    {
        propertyChangeSupport.updateAndFire(REMARKS, this.remarks, remarks, () -> this.remarks = remarks);
    }

    @Override
    public void setDescription(@Nonnull String description)
    {
        final String descriptionToUse;

        if (description.length() > 255)
        {
            logger.warn("Description will be trimmed to 255 characters on {}", this);
            descriptionToUse = description.substring(0, 255);
        } else
        {
            descriptionToUse = description;
        }

        propertyChangeSupport.updateAndFire(DESCRIPTION, this.description, descriptionToUse,
                () -> this.description = descriptionToUse);
    }

    protected void setIconImage(URI newImageURI, BufferedImage image)
    {
        logger.debug("Received new Icon Image URI: {}", newImageURI);

        if (newImageURI == null)
        {
            // Reset to our original icon (from the plugin definition)

            if (imageURI == null) return; // Nothing to do since we are already using our default icon

            // Only change the icon if we can get one from the Plugin Definition
            if (pluginDefinition == null || !mapObjectImageUtils.isSyntheticDefaultIcon(pluginDefinition))
            {
                final BufferedImage oldValue = iconImage;

                resetImageBuffers();
                imageURI = null;

                if (pluginDefinition == null)
                {
                    // We don't have a plugin-definition to fall back to...
                    iconImage = ImageUtils.getDefaultImage();
                    imageURI_t = null;
                } else
                {
                    iconImage = IMAGE_CACHE.getCanonicalImage(pluginDefinition.getIcon());
                    imageURI_t = pluginDefinition.getIconURI().orElse(null);
                }

                logger.debug("Firing ICON property change event: RESET");

                propertyChangeSupport.firePropertyChange(ICON, oldValue, iconImage);
            }
        } else if (!newImageURI.equals(imageURI_t))
        {
            // Whenever an icon is set, we want to disable the automatic icon adapter.  If we don't
            // do this, then when a Map Object is created with a CoT type AND an icon is set in the initializer,
            // the icon is overwritten with the CoT icon by the MarkerIconAdapter.
            setMetaBoolean(StandardAttributeNames.ADAPT_MARKER_ICON, false);

            final BufferedImage oldValue = iconImage;

            resetImageBuffers();

            imageURI_t = newImageURI;
            imageURI = imageURI_t.toString();
            iconImage = IMAGE_CACHE.getCanonicalImage(image);
            initializeIcons();

            logger.debug("Firing ICON property change event: {}", imageURI);

            propertyChangeSupport.firePropertyChange(ICON, oldValue, iconImage);
        }
    }

    @Override
    public void setIconImage(URI imageURI)
    {
        setIconImage(imageURI, null);
    }

    @Override
    public void setExternalIconImage(@Nonnull BufferedImage image)
    {
        // Get the image persisted in VFS
        final IVirtualFileSystem vfs = projectVFSManager.getVirtualFileSystem(MAP_OBJECT_ICONS_DIR);
        final URI fileURI = ImageUtils.writeImageToCanonicalFile(vfs.getRootDir(), image);
        String customIcon = new File(fileURI.getSchemeSpecificPart()).getName();
        try
        {
            final URI imageURI = new URI(IIconMapObject.PROTOCOL_SCHEME_VFS, MAP_OBJECT_ICONS_DIR + "/" + customIcon, null);
            imageURI_t = null; // clear the current image URI so the new one will be loaded
            setIconImage(imageURI, image);
        } catch (URISyntaxException e)
        {
            logger.error("Error: ", e);
        }
    }

    @Override
    public BufferedImage getDefaultIcon()
    {
        return IMAGE_CACHE.getCanonicalImage(pluginDefinition.getIcon());
    }

    @Override
    public URI getDefaultIconURI()
    {
        return pluginDefinition.getIconURI().orElse(null);
    }

    @Override
    public void setIconScale(double iconScale)
    {
        if (iconScale < MIN_ICON_SCALE || iconScale > MAX_ICON_SCALE)
        {
            logger.warn("Illegal icon scale of {} for map object {}.  Legal range is between {} and {}.",
                    iconScale, getKey(), MIN_ICON_SCALE, MAX_ICON_SCALE);
        } else
        {
            propertyChangeSupport.updateAndFire(ICON_SCALE, this.iconScale, iconScale, () -> this.iconScale = iconScale);
        }
    }

    @Override
    public double getIconScale()
    {
        return iconScale;
    }

    @Override
    public void setSnapIconToSurface(boolean snapIconToSurface)
    {
        propertyChangeSupport.updateAndFire(SNAP_ICON_TO_SURFACE, this.snapIconToSurface, snapIconToSurface,
                () -> this.snapIconToSurface = snapIconToSurface);
    }

    @Override
    public boolean isSnapIconToSurface()
    {
        return snapIconToSurface;
    }

    /**
     * {@inheritDoc}
     * Default to false.
     */
    @Override
    public boolean supportsSnapIconToSurface()
    {
        return false;
    }

    @Override
    public void setElevationMode(ElevationMode elevationMode)
    {
        propertyChangeSupport.updateAndFire(ELEVATION_MODE, this.elevationMode, elevationMode,
                () -> this.elevationMode = elevationMode);
    }

    @Override
    public ElevationMode getElevationMode()
    {
        return elevationMode;
    }

    @Override
    public ITrackInfo getTrackInfo()
    {
        return trackInfo;
    }

    @Override
    public String getFamily()
    {
        return family;
    }

    @Override
    public String getType()
    {
        return type;
    }

    @Override
    public String getUnitID()
    {
        return unitID;
    }

    @Override
    public void setDisplayableType(String type)
    {
        displayableType = type;
    }

    @Override
    public String getDisplayableType()
    {
        return displayableType;
    }

    @Override
    public boolean isVisible()
    {
        return visible;
    }

    @Nonnull
    @Override
    public GeoPoint getLocation()
    {
        return geoPointMetaData.get();
    }

    @Override
    public void setMetaString(@Nonnull String key, @Nullable String value)
    {
        if (StandardAttributeNames.UID.equals(key))
        {
            logger.warn("### WARNING ###  changing an immutable UID --- BLOCKED {} to: {}", uid, value);
        } else if (StandardAttributeNames.COT_TYPE.equals(key))
        {
            setCotType(Objects.requireNonNullElse(value, CotTypes.EMPTY));
            return;
        }

        // Special support to handle null values (which AttributeSet doesn't allow) to indicate removing an attribute
        if (value == null)
        {
            attributeSet.removeAttribute(key);
        } else
        {
            attributeSet.setAttribute(key, value);
        }

        // do not modify shape Cot type
        if (this instanceof AShapeMapObject<? extends IMutableShape>) return;

        // When we update the mil-sym attribute we need to also update the cot-type to match.
        // This must be done AFTER updating the attribute value so that icon adaption has up-to-date values it can
        // pull off of the map object.
        if (StandardAttributeNames.MIL_SYM.equals(key) && value != null)
        {
            String symbolCodeC = milStd2525ServiceProvider.get2525Service(CHARLIE).convertFromOther2525Variant(value);
            String adaptedCotType = Objects.requireNonNullElseGet(CotSidcAdapter.cotTypeFromMil2525C(symbolCodeC), () -> {
                // If the initial conversion from 2525C failed, grab the affiliation and make it a ground unit.
                Affiliation affiliation = milStd2525ServiceProvider.get2525ServiceForSymbol(value).getAffiliation(value);
                return CotSidcAdapter.getCotAtomPrefix(affiliation) + "G";
            });

            // If the existing CoT type is something already more specific then we do not want to downgrade.
            String oldCotType = getCotType();
            if (!oldCotType.startsWith(adaptedCotType)) setCotType(adaptedCotType);
        }
    }

    @Nonnull
    @Override
    public List<File> getAttachments()
    {
        String[] attachmentNames = attributeSet.getStringArrayAttribute(StandardAttributeNames.ATTACHED_FILES, EMPTY_STRING_ARRAY);
        List<File> attachmentFiles = new ArrayList<>();
        for (String attachmentName : attachmentNames)
        {
            File attachment = getAttachment(attachmentName);
            if (attachment != null) attachmentFiles.add(attachment);
        }
        return attachmentFiles;
    }

    @Nullable
    @Override
    public File getAttachment(@Nonnull String attachmentName)
    {
        IVirtualFileSystem attachmentFileSystem = projectVFSManager.getVirtualFileSystemForAttachments();
        List<String> attachmentNames = new ArrayList<>(Arrays.asList(attributeSet.getStringArrayAttribute(StandardAttributeNames.ATTACHED_FILES, EMPTY_STRING_ARRAY)));

        final var vfsFile = attachmentFileSystem.getFile(new FilePath(uid, attachmentName));
        if (vfsFile == null)
        {
            logger.error("Attachment file is missing from VFS of Map Object {}|{}: {}", name, uid, attachmentName);
            attachmentNames.remove(attachmentName);
            setAttachedFilesAttribute(attachmentNames);
            return null;
        } else if (!attachmentNames.contains(attachmentName))
        {
            logger.debug("VFS contains untracked file: '{}', please use MapItem API to attach items", attachmentName);
            return null;
        } else
        {
            return vfsFile.getFile();
        }
    }

    @Override
    public File addAttachment(@Nonnull String attachmentName, @Nonnull File fileToAdd)
    {
        if (!fileToAdd.exists()) return null;
        Path path = fileToAdd.toPath();
        try (InputStream attachmentStream = Files.newInputStream(path))
        {
            return addAttachment(attachmentName, attachmentStream);
        } catch (IOException e)
        {
            logger.error("Failed to create filestream of attachment file for mapObject {}|{}: {}", name, uid, path, e);
            return null;
        }
    }

    @Override
    public File addAttachment(@Nonnull String attachmentName, @Nonnull InputStream source)
    {
        final String safeAttachmentName = FileUtils.sanitizeForFilename(attachmentName);

        if (!safeAttachmentName.equals(attachmentName))
        {
            logger.warn("Invalid attachment name '{}' changed to '{}'", attachmentName, safeAttachmentName);
        }

        try
        {
            List<String> attachmentNames = new ArrayList<>(Arrays.asList(attributeSet.getStringArrayAttribute(StandardAttributeNames.ATTACHED_FILES, EMPTY_STRING_ARRAY)));
            IVirtualFileSystem attachmentVfs = projectVFSManager.getVirtualFileSystemForAttachments();
            FilePath filePath = new FilePath(uid, safeAttachmentName);
            IVFSFile attachmentVfsFile = attachmentVfs.getFile(filePath);
            if (attachmentVfsFile == null) attachmentVfsFile = attachmentVfs.createFile(filePath);
            File attachmentFile = attachmentVfsFile.getFile();
            File attachmentDir = attachmentFile.getParentFile();
            if (!attachmentDir.exists() && !attachmentDir.mkdirs()) logger.warn("Failed to create attachment folder for mapObject {}|{}: {}", name, uid, safeAttachmentName);
            Files.copy(source, attachmentFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            if (!attachmentNames.contains(safeAttachmentName))
            {
                attachmentNames.add(safeAttachmentName);
                setAttachedFilesAttribute(attachmentNames);
            } else
            {
                logger.debug("Overwrote VFS attachment on map object '{}'|'{}': {}", name, uid, filePath);
            }
            return attachmentFile;
        } catch (IOException e)
        {
            logger.error("Failed to create attachment file for mapObject {}|{}: {}", name, uid, safeAttachmentName, e);
            return null;
        }
    }

    @Override
    public void deleteAttachments(@Nonnull File... filesToDelete)
    {
        final var attachmentVfs = projectVFSManager.getVirtualFileSystemForAttachments();
        final List<String> attachmentNames = new ArrayList<>(Arrays.asList(attributeSet.getStringArrayAttribute(StandardAttributeNames.ATTACHED_FILES, EMPTY_STRING_ARRAY)));

        for (File fileToDelete : filesToDelete)
        {
            final String fileName = fileToDelete.getName();
            if (attachmentNames.contains(fileName))
            {
                try
                {
                    final var vfsPath = new FilePath(uid, fileName);
                    final var attachmentVfsFile = attachmentVfs.getFile(vfsPath);
                    if (attachmentVfsFile == null)
                    {
                        logger.warn("Attachment file is missing from VFS of Map Object {}|{}: {}/{}", name, uid, attachmentVfs.getRootDir(), vfsPath);
                    } else if (Files.isSameFile(fileToDelete.toPath(), attachmentVfsFile.getFile().toPath()))
                    {
                        attachmentNames.remove(fileName);
                        attachmentVfs.deleteFile(attachmentVfsFile);
                    }
                } catch (IOException e)
                {
                    logger.error("Error removing attachment from Map Object {}|{}: {}", name, uid, fileName, e);
                }
            } else
            {
                logger.warn("Attachment not found on Map Object {}|{}: {}", name, uid, fileName);
            }
        }
        setAttachedFilesAttribute(attachmentNames);
        deleteAttachmentFolderIfEmpty();
    }

    @Override
    public void deleteAttachments(@Nonnull String... attachmentNamesToDelete)
    {
        final var attachmentVfs = projectVFSManager.getVirtualFileSystemForAttachments();
        final List<String> attachmentNames = new ArrayList<>(Arrays.asList(attributeSet.getStringArrayAttribute(StandardAttributeNames.ATTACHED_FILES, EMPTY_STRING_ARRAY)));

        for (String attachmentName : attachmentNamesToDelete)
        {
            if (attachmentNames.remove(attachmentName))
            {
                final var vfsPath = new FilePath(uid, attachmentName);
                final var attachmentVfsFile = attachmentVfs.getFile(vfsPath);

                if (attachmentVfsFile != null)
                {
                    attachmentNames.remove(attachmentName);
                    attachmentVfs.deleteFile(attachmentVfsFile);
                } else
                {
                    logger.warn("Attachment file is missing from VFS of Map Object {}|{}: {}/{}", name, uid, attachmentVfs.getRootDir(), vfsPath);
                }
            } else
            {
                logger.warn("Attachment not found on Map Object {}|{}: {}", name, uid, attachmentName);
            }
        }
        setAttachedFilesAttribute(attachmentNames);
        deleteAttachmentFolderIfEmpty();
    }

    /**
     * Determine if this MapObject's attachment list is empty, and attempt to delete the folder if it is. The folder will be
     * removed from the VFS, though on-disk contents may remain if the folder contents have been altered manually.
     * 
     * @since 5.3
     */
    private void deleteAttachmentFolderIfEmpty()
    {
        if (getAttachments().isEmpty())
        {
            final var attachmentVfs = projectVFSManager.getVirtualFileSystemForAttachments();
            final var vfsPath = new FilePath(uid);
            final var attachmentVfsFile = attachmentVfs.getFile(vfsPath);
            if (attachmentVfsFile != null) attachmentVfs.deleteFile(attachmentVfsFile);
        }
    }

    /**
     * Standardized setter for the {@link StandardAttributeNames#ATTACHED_FILES} attribute to ensure the attribute is removed
     * if there are no attachments
     * @param attachmentNames Names of attachments to set; can be empty but not {@code null}
     */
    private void setAttachedFilesAttribute(@Nonnull List<String> attachmentNames)
    {
        if (attachmentNames.isEmpty())
        {
            attributeSet.removeAttribute(StandardAttributeNames.ATTACHED_FILES);
        } else
        {
            attributeSet.setAttribute(StandardAttributeNames.ATTACHED_FILES, attachmentNames.toArray(EMPTY_STRING_ARRAY));
        }
    }

    @Override
    public String getName()
    {
        return name;
    }

    /**
     * {@inheritDoc}
     * @since 3.5
     */
    @Override
    public String getRemarks()
    {
        return remarks;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public BufferedImage getIconImage()
    {
        if (iconImage == null) initializeIcons();

        return iconImage;
    }

    @Override
    public URI getIconImageURI()
    {
        // If we have a URI, then return that.  If not, then try to extract it from the plugin definition.
        URI uri = imageURI_t != null
                ? imageURI_t
                : pluginDefinition != null ? pluginDefinition.getIconURI().orElse(null) : null;

        logger.debug("Returning image URI of {}", uri);

        return uri;
    }

    @Override
    public ImageIcon getSizedIcon(int size)
    {
        // generate and cache the small, medium, and large icons on demand
        final int selector = switch (size)
        {
            case ImageUtils.DEFAULT_ICON_WIDTH -> DEFAULT_ICON_SELECTOR;
            case ImageUtils.SMALL_ICON_WIDTH -> SMALL_ICON_SELECTOR;
            case ImageUtils.MEDIUM_ICON_WIDTH -> MEDIUM_ICON_SELECTOR;
            case ImageUtils.LARGE_ICON_WIDTH -> LARGE_ICON_SELECTOR;
            default -> -1;
        };

        if (selector != -1)
        {
            if (sizedIcons[selector] == null && iconImage != null)
            {
                final int iconSize = ICON_SIZES[selector];
                if (logger.isDebugEnabled()) logger.debug("Generated icon of size: {} for {}", iconSize, getKey());

                final BufferedImage image = IMAGE_CACHE.getCanonicalScaledImageWithPadding(iconImage, iconSize);
                sizedIcons[selector] = new ImageIcon(image);
            }
            return sizedIcons[selector];
        } else
        {
            return new ImageIcon(IMAGE_CACHE.getCanonicalScaledImageWithPadding(iconImage, size));
        }
    }

    @Override
    public String getGroupName()
    {
        return groupName != null ? groupName : ""; // avoid null group name
    }

    @Override
    public String getKey()
    {
        return uniqueKey;
    }

    @Override
    public Color getDefaultTrackColor()
    {
        return new Color(defaultTrackColor, true);
    }

    @Override
    public void setDefaultTrackColor(Color defaultTrackColor)
    {
        final int newColor = defaultTrackColor.getRGB();
        propertyChangeSupport.updateAndFire(DEFAULT_TRACK_COLOR, this.defaultTrackColor, newColor,
                () -> this.defaultTrackColor = newColor);
    }

    @Override
    public long getMostRecentCommunicationTime()
    {
        return mostRecentCommunicationTime;
    }

    @Override
    public long getOldestCommunicationTime()
    {
        return oldestCommunicationTime;
    }

    @Override
    public void setHighestState(State highestState)
    {
        propertyChangeSupport.updateAndFire(HIGHEST_STATE, this.highestState, highestState,
                () -> this.highestState = highestState);
    }

    @Override
    public State getHighestState()
    {
        return highestState;
    }

    @Override
    public boolean hasHeading()
    {
        return !Double.isNaN(heading);
    }

    @Override
    public double getHeading()
    {
        return heading;
    }

    @Override
    public void setHeading(double heading)
    {
        // Normalize to 0..359
        final double headingToUse = !Double.isNaN(heading)
                ? (heading < 0 ? -heading : heading) % 360
                : heading;

        geoPointMetaData.getMetaData().setAttribute(StandardAttributeNames.BEARING, heading);
        propertyChangeSupport.updateAndFire(HEADING, this.heading, headingToUse, () -> this.heading = headingToUse);
    }

    @Override
    public void setSpeedInMps(double speed)
    {
        geoPointMetaData.getMetaData().setAttribute(StandardAttributeNames.SPEED, speed);
        propertyChangeSupport.updateAndFire(SPEED, this.speed, speed, () -> this.speed = speed);
    }

    @Override
    public double getSpeedInMps()
    {
        return speed;
    }

    @Override
    public boolean hasSpeed()
    {
        return !Double.isNaN(speed);
    }

    @Override
    public void setTrack(double heading, double speed)
    {
        setHeading(heading);
        setSpeedInMps(speed);
    }

    @Override
    public boolean hasElevation()
    {
        return geoPoint.isAltitudeValid();
    }

    @Override
    public IRaptorDataStructure getRaptorDataStructure(String structureType)
    {
        return mapEntity.getRaptorDataStructure(structureType);
    }

    @Override
    public Collection<IRaptorDataStructure> getRaptorDataStructures()
    {
        return mapEntity.getRaptorDataStructures();
    }

    @Override
    public IPrePersistRaptorDataMessage createRaptorDataMessage(String structureType)
    {
        return mapEntity.getRaptorDataStructure(structureType).createRaptorDataMessage(this);
    }

    @Override
    public IPrePersistRaptorDataMessage createLocationDataMessage()
    {
        IPrePersistRaptorDataMessage rdm = createRaptorDataMessage(DataStructureBuilder.LOCATION_MESSAGE_TYPE);
        rdm.setManuallyGeneratedLocation(true);
        rdm.setTime(System.currentTimeMillis());
        return rdm;
    }

    @Override
    public String toString()
    {
        return String.format("%s{%s:%s}", getClass().getSimpleName(), id, uniqueKey);
    }

    @Override
    public boolean isSelectable()
    {
        return selectable;
    }

    @Override
    public void setSelectable(boolean selectable)
    {
        propertyChangeSupport.updateAndFire(SELECTABLE, this.selectable, selectable, () -> this.selectable = selectable);
    }

    @Override
    public boolean isAllowDrag()
    {
        return allowDrag;
    }

    @Override
    public void setAllowDrag(boolean allowDrag)
    {
        propertyChangeSupport.updateAndFire(ALLOW_DRAG, this.allowDrag, allowDrag, () -> this.allowDrag = allowDrag);
    }

    @Override
    public boolean isAllowDeviceToBeContainer()
    {
        return allowDeviceContainment;
    }

    @Override
    public void setAllowDeviceToBeContainer(boolean allowDeviceContainment)
    {
        propertyChangeSupport.updateAndFire(ALLOW_DEVICE_CONTAINMENT,
                this.allowDeviceContainment, allowDeviceContainment,
                () -> this.allowDeviceContainment = allowDeviceContainment);
    }

    /**
     * Load the primary icon for this map object. Subclasses may override this method to customize the image loading
     * process. The MapObject class will automatically generate large, medium, and small icons using the image returned
     * by this method as the base.
     *
     * @return Image to use as base for large, medium, and small map object icons. If the return value is {@code null} a
     * default icon will be used.
     */
    protected BufferedImage loadMainIcon()
    {
        // If we have no image URI, then we're probably in the "default" state where we should be using
        // the icon specified in the plugin definition.  To avoid issues with jar expansion, we don't record
        // the URI for a MOs icon unless it has been specifically set via a call to setIconImage().
        if (imageURI_t == null && pluginDefinition != null)
        {
            logger.debug("Using PD icon");
            return pluginDefinition.getIcon();
        }

        return getMainIconFromURI();
    }

    /**
     * Gets an icon from the URI
     *
     * @return a BufferedImage of the map object's icon
     * @since 2.33
     */
    protected BufferedImage getMainIconFromURI()
    {
        logger.debug("Loading Image URL: {}", imageURI_t);

        if (imageURI_t == null && imageURI != null)
        {
            try
            {
                imageURI_t = new URI(imageURI);
            } catch (URISyntaxException e)
            {
                logger.error("Error", e);
            }
        }

        // If we have an image to load and we failed to do so, then report that
        if (imageURI_t == null)
        {
            if (imageURI != null) logger.error("Failed to get URI for: {} on {}", imageURI, getKey());
            return null;
        }

        logger.debug("Group: {}, Image URL: {}", groupName, imageURI_t);

        final ClassLoader classLoader = pluginDefinition != null
                ? pluginDefinition.getResourceClassLoader()
                : getClass().getClassLoader();

        final BufferedImage mainIcon = mapObjectImageUtils.loadImage(imageURI_t, classLoader);

        return IMAGE_CACHE.getCanonicalImage(mainIcon);
    }

    @Override
    public void initializeIcons()
    {
        if (iconImage == null)
        {
            iconImage = loadMainIcon();
            if (iconImage == null)
            {
                iconImage = IMAGE_CACHE.getCanonicalImage(ImageUtils.getDefaultImage());
            }
        }

        // Standard icon sizes are no longer generated up front, they are now built on demand.  See getSizedIcon().

        if (logger.isDebugEnabled()) logger.debug("after initIcons, cache stats: {}", IMAGE_CACHE.stats());
    }

    @Override
    public MapObjectOptions getMapObjectOptions()
    {
        return mapObjectOptions;
    }

    @Override
    public Set<IStructure> getAllDataStructures()
    {
        return getMapEntity().getDataStructures();
    }

    @Override
    public IMapEntity getMapEntity()
    {
        return mapEntity;
    }

    /**
     * @return Use the hashcode of our unique key (since we might not have an Id yet)
     */
    @Override
    public int hashCode()
    {
        return uniqueKey == null ? 0 : uniqueKey.hashCode();
    }

    /**
     * Determine if the given object is equal to this one.  Equality for MapObjects is strictly based on the unique
     * key.
     *
     * @param obj Object to compare against
     * @return {@code true} if equal
     */
    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof MapObject && uniqueKey != null && uniqueKey.equals(((MapObject) obj).uniqueKey);
    }

    @Override
    public boolean inPlaybackMode()
    {
        return inPlaybackMode;
    }

    @Override
    public void setPlaybackMode(boolean playback)
    {
        propertyChangeSupport.updateAndFire(PLAYBACK_MODE, inPlaybackMode, playback, () -> inPlaybackMode = playback);
    }

    @Nullable
    @Override
    public IMapObject getOwner()
    {
        return owner;
    }

    @Override
    public void setOwner(@Nullable IMapObject owner)
    {
        this.owner = owner;
        ownerId = owner != null ? owner.getId() : null;
    }

    @Override
    public void setDefaultPreferencesTab(String defaultPreferencesTab)
    {
        propertyChangeSupport.updateAndFire(DEFAULT_PREFERENCES_TAB, this.defaultPreferencesTab, defaultPreferencesTab,
                () -> this.defaultPreferencesTab = defaultPreferencesTab);
    }

    @Override
    public String getDefaultPreferencesTab()
    {
        return defaultPreferencesTab;
    }

    @Override
    @Nullable
    public DrifterConfiguration getDrifterConfiguration()
    {
        return drifterConfiguration;
    }

    @Override
    public void setDrifterConfiguration(DrifterConfiguration drifterConfiguration)
    {
        var oldValue = this.drifterConfiguration;
        this.drifterConfiguration = drifterConfiguration;
        // Update comm times if we are unpausing or setting a new config
        if (oldValue == null || (drifterConfiguration != null && !drifterConfiguration.paused() && oldValue.paused())) updateCommunicationTimes(System.currentTimeMillis());
        propertyChangeSupport.firePropertyChange(DRIFTER, oldValue, drifterConfiguration);
    }

    @Override
    public void setTooltipVisibility(@Nonnull TooltipVisibility tooltipVisibility)
    {
        propertyChangeSupport.updateAndFire(TOOLTIP_VISIBILITY, getTooltipVisibility(), tooltipVisibility,
                () -> setMetaString(StandardAttributeNames.TOOLTIP_VISIBILITY, tooltipVisibility.name()));
    }

    @Nonnull
    @Override
    public IPropertyChangeListenerSupport getPropertyChangeListenerSupport()
    {
        return propertyChangeSupport;
    }

    /**
     * A PCL to monitor the LOCATION of a Map Object and call a handler when it changes.
     *
     * @since 3.0
     */
    protected static class LocationMonitor implements PropertyChangeListener
    {
        protected final IMapObject source;
        private final Consumer<GeoPoint> locationHandler;

        /**
         * @param source          Map Object to monitor
         * @param locationHandler Handler to invoke when the LOCATION changes
         */
        LocationMonitor(IMapObject source, Consumer<GeoPoint> locationHandler)
        {
            this.source = source;
            this.locationHandler = locationHandler;

            source.addPropertyChangeListener(LOCATION, this);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            locationHandler.accept(source.getLocation());
        }

        /**
         * Stop listening for changes.
         */
        void dispose()
        {
            source.removePropertyChangeListener(LOCATION, this);
        }
    }
}
