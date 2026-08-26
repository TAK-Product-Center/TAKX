package gov.takx.api.mapobject;

import com.google.auto.value.AutoValue;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.util.AttributeSet;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.plugin.IDataDelegate;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.plugin.device.IDevicePluginDefinition;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.utils.UnitIdHelper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.function.Consumer;

/**
 * A MapObjectSpec specifies the values to be applied to a MapObject when it is constructed.  Instances must be
 * created using a builder, see {@link #builder(IMapEntity, String)} and {@link #builder(String, String, String)}.
 * <p>
 * A MapObjectSpec instance is then used in a call to {@link IDataDelegate#getOrCreateMapObject(MapObjectSpec)}
 * or {@link IMapObjectManager#getOrCreateMapObject(MapObjectSpec)}.
 * <p>
 * For example, to create a Map Object with a given mapEntity and unitId, with all other values defaulted:
 * <pre>
 *     final MapObjectSpec spec = MapObjectSpec.builder(mapEntity, "myUnitId").build();
 *     delegate.getOrCreateMapObject(spec);
 * </pre>
 * Or, to create a Map Object with an initial name and description:
 * <pre>
 *     final MapObjectSpec spec = MapObjectSpec.builder(mapEntity, "myUnitId")
 *         .name("A clever name")
 *         .description("something even more clever")
 *         .build();
 *     delegate.getOrCreateMapObject(spec);
 * </pre>
 * Or, to create a MapObject and perform custom logic only in the case that it is actually created:
 * <pre>
 *     final MapObjectSpec spec = MapObjectSpec.builder(mapEntity, "myUnitId")
 *         .initializer(mo -&gt; doSomethingWithNewMapObject(mo))
 *         .build();
 *     delegate.getOrCreateMapObject(spec);
 * </pre>
 *
 * @since 2.29
 */
@SuppressWarnings("NewClassNamingConvention")
@AutoValue
public abstract class MapObjectSpec
{
    /**
     * Makes a new builder initialized from the values in this spec.  This is useful if you want to take a
     * starting configuration and only make a few changes to produce a new spec.
     *
     * @return A new builder initialized from the values in this spec
     */
    public abstract Builder toBuilder();

    /**
     * @return The MapEntity defining the Map Object to be constructed
     */
    public abstract IMapEntity mapEntity();

    /**
     * @return Unit Id for Map Object
     */
    @Nullable
    public abstract String unitId();

    /**
     * @return Map Object Options to be applied to the Map Object when it is created
     */
    @Nullable
    public abstract MapObjectOptions mapObjectOptions();

    /**
     * @return owning Map Object to be applied to the Map Object when it is created
     */
    @Nullable
    public abstract IMapObject owner();

    /**
     * @return name to be applied to the Map Object when it is created
     */
    @Nullable
    public abstract String name();

    /**
     * @return description to be applied to the Map Object when it is created
     */
    @Nullable
    public abstract String description();

    /**
     * @return The AttributeSet that will be added to the Map Object when it is created
     * @since 3.4
     */
    @Nullable
    public abstract AttributeSet attributeSet();

    /**
     * @return The UID to be applied to the Map Object when it is created
     * @since 3.0
     */
    @Nullable
    public abstract String uid();

    /**
     * @return The CoT Type to be applied to the Map Object when it is created
     * @since 3.0
     */
    @Nullable
    public abstract String cotType();

    /**
     *
     * @return The CoT event to be associated with the Map Object when it is created
     * @since 3.2
     */
    @Nullable
    public abstract CotEvent cotEvent();

    /**
     * @return initial location to be applied to the Map Object when it is created
     */
    @Nullable
    public abstract ILocation location();

    /**
     * @return initial geoPoint (location) to be applied to the Map Object when it is created
     * @since 3.0
     */
    @Nullable
    public abstract GeoPoint geoPoint();

    /**
     * @return Initializer to apply to the Map Object when it is created
     */
    @Nullable
    public abstract Consumer<IMapObject> initializer();

    /**
     * @return The unique key of the MapObject
     */
    public String getKey()
    {
        return KeyUtils.getUniqueKey(mapEntity().getFamily(), mapEntity().getType(), unitId());
    }

    /**
     * @return The Map Object family
     */
    public String family()
    {
        return mapEntity().getFamily();
    }

    /**
     * @return The Map Object type
     */
    public String type()
    {
        return mapEntity().getType();
    }

    /**
     * @return The plugin definition, if specified
     * @since 5.3
     */
    @Nullable
    public abstract IPluginDefinition<?> pluginDefinition();

    /**
     * @return The set MapObjectOptions, or the default options if none were set
     * @see MapObjectOptions#STANDARD_DEVICE_OPTIONS
     */
    public MapObjectOptions mapObjectOptionsOrDefault()
    {
        final MapObjectOptions mapObjectOptions = mapObjectOptions();
        return mapObjectOptions != null ? mapObjectOptions : MapObjectOptions.STANDARD_DEVICE_OPTIONS;
    }

    /**
     * Construct a builder.
     *
     * @param mapEntity Map Entity defining the Map Object
     * @param unitId    Unit Id for the Map Object
     */
    public static Builder builder(IMapEntity mapEntity, String unitId)
    {
        return new AutoValue_MapObjectSpec.Builder().mapEntity(mapEntity).unitId(unitId);
    }

    /**
     * Construct a builder.  The family and type are used to find or create a MapEntity.
     *
     * @param family Map Object family
     * @param type   Map Object type
     * @param unitId Unit Id for the Map Object
     */
    public static Builder builder(String family, String type, String unitId)
    {
        return new AutoValue_MapObjectSpec.Builder().family(family).type(type).unitId(unitId);
    }

    /**
     * Construct a builder.  The family and type are used to find or create a MapEntity.
     * This will auto-generate a unique unit Id and name.
     *
     * @param family Map Object family
     * @param type   Map Object type
     * @see Builder#autoGenerateUnitId()
     * @since 3.0
     */
    public static Builder builder(String family, String type)
    {
        return new AutoValue_MapObjectSpec.Builder().family(family).type(type).autoGenerateUnitId();
    }

    /**
     * Construct a builder.  The plugin definition provides the family and type used to find or create a MapEntity.
     * A unique unit Id and name will be generated automatically, except in the case that the given
     * {@code pluginDefinition} is a {@link IDevicePluginDefinition} and {@link IDevicePluginDefinition#isAutoGenerateUnitId()}
     * returns {code false}, in which case the user will be prompted for a unit ID.
     *
     * @param pluginDefinition Definition from which to pull the family and type
     */
    public static Builder builder(IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        final Builder builder = new AutoValue_MapObjectSpec.Builder().pluginDefinition(pluginDefinition);

        final boolean autoGenerate;

        if (pluginDefinition instanceof IDevicePluginDefinition devicePluginDefinition)
        {
            builder.mapObjectOptions(devicePluginDefinition.getMapObjectOptions());
            autoGenerate = devicePluginDefinition.isAutoGenerateUnitId();
        } else
        {
            autoGenerate = true;
        }

        if (autoGenerate) builder.autoGenerateUnitId();

        return builder;
    }

    /**
     * Construct a spec from an existing MapObject.
     *
     * @param mapObject Source Map Object
     */
    public static MapObjectSpec from(IMapObject mapObject)
    {
        return new AutoValue_MapObjectSpec.Builder().mapEntity(mapObject.getMapEntity()).unitId(mapObject.getUnitID()).build();
    }

    @SuppressWarnings("NewClassNamingConvention")
    @PseudoBean
    @AutoValue.Builder
    public abstract static class Builder
    {
        @Inject
        private IMapObjectManager mapObjectManager;

        @Inject
        private UnitIdHelper unitIdHelper;

        private String family;
        private String type;
        private boolean autoGenerateUnitId;

        /**
         * Specify the plugin definition for the constructed Map Object.
         *
         * @param pluginDefinition Plugin definition
         * @return this builder
         * @since 5.3
         */
        public abstract Builder pluginDefinition(@Nonnull IPluginDefinition<?> pluginDefinition);

        /**
         * Set the options to be applied to the Map Object when it is created.  If no options are specified on
         * this builder, then the default options as defined by the plugin will be used to construct the Map Object.
         *
         * @param mapObjectOptions Map object options to apply
         * @return this builder
         */
        public abstract Builder mapObjectOptions(MapObjectOptions mapObjectOptions);

        /**
         * Set the owning Map Object to be applied to the Map Object when it is created.
         *
         * @param owner Owning map object
         * @return this builder
         */
        public abstract Builder owner(IMapObject owner);

        /**
         * Generate a new unit ID using a random UUID and generating a name by using the type and appending
         * a number, starting with the number 1 as a string and incrementing until it finds an unused name.
         *
         * @since 3.0
         */
        public Builder autoGenerateUnitId()
        {
            autoGenerateUnitId = true;
            return this;
        }

        /**
         * Set the name to be applied to the Map Object when it is created.
         *
         * @param name Name for Map Object
         * @return this builder
         */
        public abstract Builder name(String name);

        /**
         * Set the description to be applied to the Map Object when it is created.
         *
         * @param description Description for Map Object
         * @return this builder
         */
        public abstract Builder description(String description);

        /**
         * Set the initial location to be applied to the Map Object when it is created.
         *
         * @param location Initial location for map object
         * @return this builder
         */
        public abstract Builder location(ILocation location);

        /**
         * Set the initial geoPoint (location) to be applied to the Map Object when it is created.
         *
         * @param geoPoint Initial location for map object
         * @return this builder
         * @since 3.0
         */
        public abstract Builder geoPoint(GeoPoint geoPoint);

        /**
         * Sets the AttributeSet for the Map Object, a copy of all entries in this set will be added to the MO's
         * initial attribute set when it is created.
         *
         * @param attributeSet The attribute set to associate with this Map Object when it is created.
         * @return this builder
         * @since 2.38
         */
        public abstract Builder attributeSet(AttributeSet attributeSet);

        /**
         * Specifies the UID of the Map Object.  It can only be set during construction.  This UID is a String,
         * as compared to other UUID identifiers on other API objects.  If this is not set, then a UID will be
         * generated as the string form of a random UUID.
         * <p>
         * The other typical case for this is when processing the import of a CoT event, which specifies a UID.
         * That UID will then be used for the Map Object constructed from the event.
         *
         * @param uid The UID to set when the Map Object is created
         * @return this builder
         * @since 3.0
         */
        public abstract Builder uid(String uid);

        /**
         * @param cotType The CoT Type that will be set when the Map Object is created
         * @return this builder
         * @since 3.0
         */
        public abstract Builder cotType(String cotType);

        /**
         * @param cotEvent The CoT event that will be associated with the Map Object
         * @return this builder
         * @since 3.2
         */
        public abstract Builder cotEvent(CotEvent cotEvent);

        /**
         * Specify arbitrary customizations or logic to be performed if/when the MapObject is created.  The initializer
         * will be passed the newly constructed Map Object as an argument.  For example,
         * <pre>{@code
         *     final MapObjectSpec.builder(mapEntity, "myUnitId")
         *         .name("A clever name")
         *         .initializer(mo -> mo.setRenderingMode(IconRenderingMode.HeadingOriented)
         *         .build();
         * }</pre>
         * <p>
         * The initializer is also a mechanism for determining if the MapObject was created or not since it will only
         * be called in the case that a new Map Object was constructed.
         *
         * @param initializer Initializer to execute upon creation
         * @return this builder
         */
        public abstract Builder initializer(Consumer<IMapObject> initializer);

        /**
         * @return New instance of MapObjectSpec built from this configuration
         */
        public MapObjectSpec build()
        {
            final var pluginDefinition = pluginDefinition();
            if (pluginDefinition != null)
            {
                family = pluginDefinition.getFamily();
                type = pluginDefinition.getType();
            }

            // If the family/type were specified, then we'll need to locate/create the required Map Entity.
            if (family != null) mapEntity(mapObjectManager.getOrCreateMapEntity(family, type));

            // Set a default name if none was provided
            final boolean customNameSet = name() != null;
            if (!customNameSet) name(mapEntity().getType() + " " + unitId());

            if (autoGenerateUnitId)
            {
                // Generate a UUID for unitId.
                unitId(UUIDUtils.generateType1UUID().toString());

                // Set a friendly name as the map object name, so the user doesn't actually see the UUID.
                if (!customNameSet) name(type + " " + unitIdHelper.getNextFriendlyUnitId(family, type));
            } else if (unitId() == null)
            {
                throw new IllegalStateException("You must either specify a unitId or set autoGenerateUnitId(true)");
            }

            return autoBuild();
        }

        abstract MapObjectSpec autoBuild();

        abstract Builder mapEntity(IMapEntity mapEntity);

        abstract IMapEntity mapEntity();

        abstract Builder unitId(String unitId);

        abstract String unitId();

        abstract String name();

        abstract IPluginDefinition<?> pluginDefinition();

        Builder family(@Nonnull String family)
        {
            this.family = family;
            return this;
        }

        Builder type(@Nonnull String type)
        {
            this.type = type;
            return this;
        }
    }
}
