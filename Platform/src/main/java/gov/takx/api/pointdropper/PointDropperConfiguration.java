package gov.takx.api.pointdropper;

import com.google.auto.value.AutoValue;
import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.icons.IIconSetManager;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.globe.IWorldWindowGL;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.bootstrap.FlavorConstants;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Defines a set of configuration properties for controlling the operation of the
 * {@link IPointDropperService point dropper service}.
 *
 * @since 5.1
 */
@SuppressWarnings("removal")
@AutoValue
public abstract class PointDropperConfiguration
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Contract for a mechanism to determine if the point dropper service should be deactivated due to a given
     * {@link MouseEvent mouse pressed event}.  This can be used to deactivate the point dropper if the user clicks
     * somewhere off of the globe. {@link Builder#deactivateOnExternalClick(boolean)} is a shorthand mechanism for that
     * simple case.  This could also be used to allow interaction with another UI component as well as the globe, but
     * deactivate if the user clicks anywhere else, {@link OffGlobeOrComponentDeactivator} does that.
     * 
     * @since 5.2
     * @see OffGlobeDeactivator
     * @see OffGlobeOrComponentDeactivator
     */
    @FunctionalInterface
    public interface IDeactivator
    {
        /**
         * Determine if the point dropper should be deactivated due to the given {@link MouseEvent mouse pressed event}.
         *
         * @param mouseEvent Event to process
         * @return {@code true} if the point dropper should be deactivated, {@code false} to remain active
         */
        boolean shouldDeactivate(MouseEvent mouseEvent);
    }

    /**
     * Deactivator that always returns {@code false}
     * @since 5.2.
     */
    public static class NoopDeactivator implements IDeactivator
    {
        @Override
        public boolean shouldDeactivate(MouseEvent mouseEvent)
        {
            return false;
        }
    }

    /**
     * Deactivator that triggers if a mouse pressed event happens anywhere but on a globe.
     * @since 5.2
     */
    public static class OffGlobeDeactivator implements IDeactivator
    {
        @Override
        public boolean shouldDeactivate(MouseEvent mouseEvent)
        {
            return !(mouseEvent.getComponent() instanceof IWorldWindowGL);
        }
    }

    /**
     * Deactivator that triggers if a mouse pressed event happens anywhere but on a globe or within a given {@link JComponent}.
     * @since 5.2
     */
    public static class OffGlobeOrComponentDeactivator extends OffGlobeDeactivator
    {
        private final JComponent allowedComponent;

        /**
         * @param allowedComponent Component in which to allow mouse presses in addition to the globe
         */
        public OffGlobeOrComponentDeactivator(@Nonnull JComponent allowedComponent)
        {
            this.allowedComponent = allowedComponent;
        }

        @Override
        public boolean shouldDeactivate(MouseEvent mouseEvent)
        {
            // Convert the mouse point on screen to a point relative to the allowed component.
            final Point mouseLocation = mouseEvent.getLocationOnScreen();
            SwingUtilities.convertPointFromScreen(mouseLocation, allowedComponent);

            // If the relative point is not within the allowed panel, deactivate.
            return super.shouldDeactivate(mouseEvent) && !allowedComponent.contains(mouseLocation);
        }
    }

    /**
     * Contract for a mechanism to generate names for Map Objects created by the Point Dropper Service.
     * The generator should not produce the same name twice, if possible, to avoid confusion by the user.
     */
    @FunctionalInterface
    public interface INameGenerator
    {
        /**
         * @return Generate the next name to use, {@code null} to cause the default name to be used instead
         */
        @Nullable
        String generateName();
    }

    /**
     * Generate names based on a prefix, starting number, and increment. E.g., a configuration of ('PFX', 2, 3)
     * would generate names {@code PFX 2}, {@code PFX 5}, {@code PFX 8}, etc.
     * <p/>
     * This generator will not produce names that are already in use.
     * @see IMapObjectManager#nameExists(String)
     */
    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    @PseudoBean
    public static class CountingNameGenerator implements INameGenerator
    {
        @Inject
        private IMapObjectManager mapObjectManager;

        protected final String prefix;
        protected final int start;
        protected final int increment;

        protected int currentIndex;

        /**
         * @param prefix The prefix to use for the name, may be {@code null} or blank to use no prefix
         * @param start The starting index to use for the name, if not &gt; 0, 1 will be used
         * @param increment The increment to add to the starting index for each additional point dropped
         */
        public CountingNameGenerator(@Nullable String prefix, int start, int increment)
        {
            this.prefix = StringUtils.isEmpty(prefix) ? "" : prefix + " ";
            this.start = start;
            this.increment = increment;

            reset(); // initialize currentIndex
        }

        /**
         * Restore original starting index.
         *
         * @since 5.5
         */
        public void reset()
        {
            currentIndex = start > 0 ? start : 1;
        }

        @Override
        public String generateName()
        {
            String name;
            do
            {
                name = prefix + currentIndex;
                currentIndex += increment;
            } while (mapObjectManager.nameExists(name));

            return name;
        }

        @Override
        public String toString()
        {
            return "CountingNameGenerator[" +
                    "prefix=" + prefix + ", " +
                    "startIndex=" + start + ", " +
                    "increment=" + increment + ']';
        }
    }

    /**
     * Generate marker names, based on a given CoT type and our CIV/MIL flavor, in the form
     * {@code <prefix>.<day>.<hhmmss>[.<dedupe>]}.
     * <p/>
     * {@code 'prefix'} is determined from the configured CoT type, as follows:
     * <ul>
     *     <li>{@code a-f-*} → F</li>
     *     <li>{@code a-N-*} → N</li>
     *     <li>{@code a-u-*} → U</li>
     *     <li>{@code a-h-*} → R (CIV) or TGT (MIL)</li>
     *     <li>{@code b-m-p-s-m} → S (sensor)</li>
     *     <li>{@code b-r-f-h-c-*} → MED</li>
     *     <li>If no match → device callsign</li>
     * </ul>
     * {@code 'day'} is the day in month (1-31) at time of creation in the <em>local</em> time zone, zero padding is
     *               not applied to this segment.
     * <p/>
     * {@code 'hhmmss'} Hours, Minutes, Seconds, each element is zero padded to two characters.
     * <p/>
     * {@code 'dedupe'} In the event that more than one item of the same CoT type with the derived name already exists,
     *                  an additional segment is added to deduplicate. The dedupe index begins at 1 increments by 1.
     * <p/>
     * Some examples:
     * <pre>{@code
     * N.22.135034
     * N.22.135034.1
     * N.22.135034.2
     * U.5.040506
     * SOME_CALLSIGN.10.060708
     * }</pre>
     * @see <a href="Point+Dropper+Naming+Convention">
     *     Point Dropper Naming Convention</a>
     * @since 5.2
     */
    @SuppressWarnings({"CdiManagedBeanInconsistencyInspection", "CdiInjectionPointsInspection"})
    @PseudoBean
    public static class CotMarkerNameGenerator implements INameGenerator
    {
        @Inject
        private IProgramEnvironment programEnvironment;

        @Inject
        private IMapObjectManager mapObjectManager;

        @Inject
        @ProgramPreference(PreferenceEntry.PRODUCT_FLAVOR)
        private String productFlavor;

        private final String cotType;

        private ZoneId localZoneId;
        private DateTimeFormatter localTimeFormatter;

        /**
         * @param cotType CoT type from which to generate names, if {@code null} the prefix will be the current callsign
         */
        public CotMarkerNameGenerator(@Nullable String cotType)
        {
            this.cotType = cotType != null ? cotType.toLowerCase() : null;
        }

        @PostConstruct
        private void postConstruct()
        {
            localZoneId = programEnvironment.getLocalTimeZone().toZoneId();
            localTimeFormatter = DateTimeFormatter.ofPattern("d.HHmmss").withZone(localZoneId);
        }

        @Override
        public String generateName()
        {
            final var now = Instant.ofEpochMilli(CoordinatedTime.currentTimeMillis()).atZone(localZoneId).toLocalDateTime();
            final String baseName = determinePrefix(cotType) + "." + now.format(localTimeFormatter);

            String name = baseName;
            int dedupe = 1;

            while (mapObjectManager.nameExists(name)) name = baseName + "." + dedupe++;

            return name;
        }

        /**
         * @return The prefix string to use based on the given {@code cotType}, defaulting to the current callsign
         */
        private String determinePrefix(String cotType)
        {
            if (cotType != null)
            {
                if (cotType.equals(CotTypes.MANUALLY_PLACED_SENSOR)) return "S";
                if (cotType.startsWith(CotTypes.EVAC_POINT)) return "MED";
                if (cotType.startsWith(CotTypes.PREFIX_FRIENDLY)) return "F";
                if (cotType.startsWith(CotTypes.PREFIX_NEUTRAL)) return "N";
                if (cotType.startsWith(CotTypes.PREFIX_UNKNOWN)) return "U";
                if (cotType.startsWith(CotTypes.PREFIX_HOSTILE)) return productFlavor.equals(FlavorConstants.MIL) ? "TGT" : "R";
            }

            return PreferenceEntry.CALL_SIGN.getStringValue(); // default to call sign
        }
    }

    private static final Consumer<IMapObject> NOOP_MO_CONSUMER = mo -> {
    };

    private static final Runnable NOOP_DEACTIVATION_HANDLER = () -> {
    };

    private static final INameGenerator DEFAULT_NAME_GENERATOR = () -> null; // Use default name

    /**
     * Makes a new builder initialized from the values in this configuration.  This is useful if you want to take a
     * starting configuration and only make a few changes to produce a new one.
     *
     * @return A new builder initialized from the current values in this configuration
     */
    public abstract PointDropperConfiguration.Builder toBuilder();

    /**
     * @return {@code true} if the point dropper will deactivate if the user clicks anywhere outside the globe.
     * The default is {@code false} if not set in the builder.
     */
    public boolean deactivateOnExternalClick()
    {
        return deactivator().getClass() == OffGlobeDeactivator.class;
    }

    /**
     * @return Deactivator to process AWT Mouse Pressed events and determine if the dropper should deactivate
     * @since 5.2
     */
    @Nonnull
    public abstract IDeactivator deactivator();

    /**
     * Note that this method is marked nullable, but actually never returns null.  This is a limitation with the
     * AutoValue/Builder system.
     *
     * @return Plugin definition for creating the Map Objects
     */
    @Nullable // Will never actually be null, see javadoc
    public abstract IPluginDefinition<?> pluginDefinition();

    /**
     * @return The {@link CotIcon} used to determine the actual icon and type to be applied to the dropped Map Objects.
     * May be {@code null}.
     */
    @Nullable
    public abstract CotIcon cotIcon();

    /**
     * @return The CoT type string used to determine the actual icon to be applied to the dropped Map Objects, may be {@code null}.
     */
    @Nullable
    public abstract String cotType();

    /**
     * @return The SIDC code used to provide additional detail for icon rendering not representable through cot-types.
     * @since 5.2
     */
    @Nullable
    public abstract String sidc();

    /**
     * @return The name generator to use for determining the name to apply to the dropped Map Objects.
     */
    @Nonnull
    public abstract INameGenerator nameGenerator();

    /**
     * @return Initializer to apply to any Map Object created by the point dropper service.
     * @see gov.takx.api.mapobject.MapObjectSpec.Builder#initializer(Consumer)
     */
    @Nonnull
    public abstract Consumer<IMapObject> initializer();

    /**
     * @return Handler to invoke on each Map Object created by the point dropper service.
     * @since 5.2
     */
    @Nonnull
    public abstract Consumer<IMapObject> dropHandler();

    /**
     * @return Handler to invoke when the point dropper service deactivates.
     * @since 5.2
     */
    @Nonnull
    public abstract Runnable deactivationHandler();

    /**
     * Construct a new builder, specifying the CoT icon to use for created Map Objects.  The {@link CotIcon} defines
     * the CoT type string which is used to determine the actual icon to be applied to the dropped Map Objects.
     *
     * @return A new builder
     */
    @Nonnull
    public static Builder builder(@Nonnull CotIcon cotIcon)
    {
        return builderWithDefaults().cotIcon(cotIcon).cotType(cotIcon.getCotType());
    }

    /**
     * Construct a new builder, specifying the CoT type string to use for created Map Objects.
     *
     * @return A new builder
     */
    @Nonnull
    public static Builder builder(@Nonnull String cotType)
    {
        return builderWithDefaults().cotType(cotType);
    }

    /**
     * Construct a new builder, specifying an icon from an icon set to use for created Map Objects.
     * <p/>
     * <b>Partial Deprecation Note:</b> As of 5.2, this method will interpret its parameters differently:
     * <ul>
     *     <li>
     *         The first parameter, renamed from {@code iconGroup} to {@code iconSetNameOrUid}, is now expected to
     *         be the name or UID of an {@link gov.tak.api.icons.IUserIconSet icon set}, as managed by the
     *         {@link gov.tak.api.icons.IIconSetManager Icon Set Manager}.
     *     </li>
     *     <li>
     *         The second parameter, renamed from {@code iconType} to {@code iconName}, is now expected to
     *         be the name of an icon within the identified {@code iconSet}.
     *     </li>
     * </ul>
     * <p/>
     *
     * @param iconSetNameOrUid The name or UID of an icon set known to the {@link IIconSetManager}
     * @param iconName The name of the icon within the icon set
     * @return A new builder
     * @throws IllegalArgumentException If the named icon set or icon can not be found
     */
    @Nonnull
    public static Builder builder(@Nonnull String iconSetNameOrUid, @Nonnull String iconName) throws IllegalArgumentException
    {
        return builderWithDefaults().pluginDefinition(iconSetNameOrUid, iconName);
    }

    /**
     * Construct a new builder, specifying the plugin definition to use for created Map Objects.
     *
     * @return A new builder
     */
    @Nonnull
    public static Builder builder(@Nonnull IPluginDefinition<?> pluginDefinition)
    {
        return builderWithDefaults().pluginDefinition(pluginDefinition);
    }

    /**
     * @return New builder with default properties configured
     */
    @Nonnull
    private static Builder builderWithDefaults()
    {
        return new AutoValue_PointDropperConfiguration.Builder()
                .deactivator(new NoopDeactivator())
                .nameGenerator(DEFAULT_NAME_GENERATOR)
                .initializer(NOOP_MO_CONSUMER)
                .dropHandler(NOOP_MO_CONSUMER)
                .deactivationHandler(NOOP_DEACTIVATION_HANDLER);
    }

    @PseudoBean
    @AutoValue.Builder
    public abstract static class Builder
    {
        private static final String CANNOT_DETERMINE_ICON =
                "Cannot determine icon to use; must specify one of CoT type, CotIcon, or icon set/icon name";

        @Inject
        private IIconSetManager iconSetManager;

        private boolean useCotNamer = false;

        /**
         * @param deactivateOnExternalClick Pass {@code true} if the point dropper should deactivate if the user clicks
         *                                  anywhere outside the globe, {@code false} to ignore such clicks
         */
        @Nonnull
        public Builder deactivateOnExternalClick(boolean deactivateOnExternalClick)
        {
            return deactivator(deactivateOnExternalClick ? new OffGlobeDeactivator() : new NoopDeactivator());
        }

        /**
         * @param deactivator Deactivator to process AWT Mouse Pressed events to determine when to deactivate the dropper
         * @return this builder
         * @see MouseEvent#MOUSE_PRESSED
         * @since 5.2
         */
        @Nonnull
        public abstract Builder deactivator(@Nonnull IDeactivator deactivator);

        /**
         * @param pluginDefinition the plugin definition to use for created Map Objects
         * @return this builder
         */
        @Nonnull
        public abstract Builder pluginDefinition(@Nonnull IPluginDefinition<?> pluginDefinition);

        /**
         * Private mechanism to specify the plugin definition from a given icon set name/UID and icon name.
         */
        @Nonnull
        private Builder pluginDefinition(@Nonnull String iconSetNameOrUid, @Nonnull String iconName)
        {
            var iconSet = iconSetManager.getIconSetByName(iconSetNameOrUid);
            if (iconSet == null) iconSet = iconSetManager.getIconSetByUid(iconSetNameOrUid);

            final var userIcon = iconSet != null ? iconSet.getIcon(iconName) : null;
            if (userIcon != null)
            {
                pluginDefinition(iconSetManager.getOrCreatePluginDefinition(userIcon));
            } else
            {
                throw new IllegalArgumentException("Unknown icon set reference: " + iconSetNameOrUid + "/" + iconName);
            }
            return this;
        }

        /**
         * Specify arbitrary customizations or logic to be performed if/when a MapObject is created via the point dropper.
         * The initializer will be passed the newly constructed Map Object as an argument.  For example,
         * <pre>{@code
         *     final PointDropperConfiguration.builder()
         *         .initializer(mo -> {
         *             mo.setName(someCustomName);
         *             mo.setMetaString(someKey, someValue);
         *         })
         *         .build();
         * }</pre>
         *
         * @param initializer Initializer to apply to any Map Object created by the point dropper service
         * @return this builder
         */
        @Nonnull
        public abstract Builder initializer(Consumer<IMapObject> initializer);

        /**
         * Specify a handler to be invoked on each point as it is dropped by the user.
         * The handler will be passed the newly constructed Map Object as an argument.  For example,
         * <pre>{@code
         *     final PointDropperConfiguration.builder()
         *         .dropHandler(mo -> {
         *             // Deactivate after the first point is dropped
         *             pointDropperService.deactivatePointDropper();
         *         })
         *         .build();
         * }</pre>
         *
         * @param handler Handler to invoke on each point dropped
         * @return this builder
         * @since 5.2
         */
        @Nonnull
        public abstract Builder dropHandler(Consumer<IMapObject> handler);

        /**
         * Handler to invoke when the point dropper service is deactivated, either by the deactivator, the user
         * pressing the {@code ESC} key, or programmatically if {@link IPointDropperService#deactivatePointDropper()}
         * is called.
         *
         * @param handler Handler to invoke
         * @return this builder
         * @since 5.2
         */
        @Nonnull
        public abstract Builder deactivationHandler(Runnable handler);

        /**
         * Set the CoT type for the dropped Map Objects.  The CoT type string is used to determine the actual icon
         * to be applied to the dropped Map Objects.
         *
         * @return this builder
         */
        @Nonnull
        public abstract Builder cotType(@Nullable String cotType);

        /**
         * @param sidc The SIDC code used to provide additional detail for icon rendering not representable through cot-types.
         * @return this builder.
         * @since 5.2
         */
        @Nonnull
        public abstract Builder sidc(@Nullable String sidc);

        /**
         * Specify a custom name generator to use for created Map Objects.
         *
         * @return this builder
         */
        @Nonnull
        public abstract Builder nameGenerator(@Nonnull INameGenerator nameGenerator);

        /**
         * Specify that the {@link CotMarkerNameGenerator} should be used to generate marker names based on the
         * configured {@code cotType}.
         *
         * @return this builder.
         * @since 5.2
         */
        public Builder useCotNamer()
        {
            // Can't create the namer instance until we have a CoT type, so just record for now
            useCotNamer = true;
            return this;
        }

        /**
         * Use a {@link CountingNameGenerator} configured with the given values to generate names for created Map Objects.
         *
         * @param prefix The prefix to use for the name, may be {@code null} to use no prefix
         * @param start The starting index to use for the name, if not &gt; 0, 1 will be used
         * @param increment The increment to add to the starting index for each additional point dropped
         * @return this builder
         */
        @Nonnull
        public Builder nameGenerator(@Nullable String prefix, int start, int increment)
        {
            return nameGenerator(new CountingNameGenerator(prefix, start, increment));
        }

        /**
         * @return New instance of PointDropperConfiguration built from this configuration
         * @throws IllegalStateException If no plugin definition can be located using the given inputs
         */
        @Nonnull
        public PointDropperConfiguration build() throws IllegalStateException
        {
            // If we don't have a plugin definition yet, find one
            IPluginDefinition<?> pluginDefinition = pluginDefinition();
            if (pluginDefinition == null)
            {
                final CotIcon cotIcon = cotIcon();
                if (cotIcon != null)
                {
                    pluginDefinition = iconSetManager.getOrCreateCotDefinition(cotIcon.getCotType());
                } else
                {
                    final String cotType = cotType();
                    if (cotType != null) pluginDefinition = iconSetManager.getOrCreateCotDefinition(cotType);
                }

                if (pluginDefinition == null) throw new IllegalStateException(CANNOT_DETERMINE_ICON);

                pluginDefinition(pluginDefinition);
            }

            if (useCotNamer) configureCotNamer();

            return autoBuild();
        }

        /**
         * Install the {@link CotMarkerNameGenerator} based on our configured {@link #cotType()}.
         * @since 5.2
         */
        private void configureCotNamer()
        {
            final String cotType = cotType();
            if (cotType != null)
            {
                nameGenerator(new CotMarkerNameGenerator(cotType));
            } else
            {
                throw new IllegalStateException("no 'cotType' defined; Cannot use CotNamer");
            }
        }

        @Nullable
        abstract String cotType();

        @Nullable
        abstract CotIcon cotIcon();

        @Nullable
        abstract IPluginDefinition<?> pluginDefinition();

        @Nonnull
        abstract Builder cotIcon(@Nonnull CotIcon cotIcon);

        @Nonnull
        abstract PointDropperConfiguration autoBuild();
    }
}
