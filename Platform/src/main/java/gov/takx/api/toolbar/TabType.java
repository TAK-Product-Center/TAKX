package gov.takx.api.toolbar;

import gov.takx.commons.utils.UUIDUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Enum used to identify the tab type, as well as whether it was created by TAKX, a plugin, or the user.
 *
 * @since 2.0 (Renamed and moved into API in 3.4)
 */
public enum TabType
{
    // Note: order matters here, as this is the order that TAKX-created tabs will be added to the toolbar
    HOME("Home"),
    CONNECT("Connect", ButtonType.CONNECTION_PLUGIN),
    DEVICES("Devices", ButtonType.DEVICE_PLUGIN),
    MAPS("Maps", ButtonType.PROJECT_FAVORITE, ButtonType.USER_FAVORITE),
    ATTACHMENTS("Attachments"),
    DATA("Data", ButtonType.IMPORT_PLUGIN, ButtonType.EXPORT_PLUGIN),
    DRAW("Draw", ButtonType.PLUGIN_DRAW_TOOL),
    VIEW("View"),
    PLUGINS("Plugins", ButtonType.PLUGIN_PANEL, ButtonType.PLUGIN_SCRIPT),

    /** @since 5.0.0 */
    RANGE_AND_BEARING("Range & Bearing"),
    HELP("Help", ButtonType.PLUGIN_DOCUMENTATION),

    PLUGIN_CREATED(),
    USER_CREATED();

    private static final Map<ButtonType, TabType> DYNAMIC_BUTTON_TYPE_MAP = new EnumMap<>(ButtonType.class);

    static
    {
        for (TabType tabType : values())
        {
            tabType.getDynamicButtonTypes().forEach(buttonType -> DYNAMIC_BUTTON_TYPE_MAP.put(buttonType, tabType));
        }
    }

    private static final String DEFAULT_DISPLAY_TEXT = "<New Tab>";

    /**
     * The default display text from tabs with this type (may be {@code null}).
     */
    private final String displayText;

    /**
     * Indicates if there will only ever be one unique tab with this type.
     */
    private final boolean singular;

    /**
     * If this is a dynamically updating tab (meaning the tab is updated dynamically by TAKX), this specifies the
     * type of buttons that will be updated dynamically.
     * @since 3.4
     */
    private final List<ButtonType> dynamicButtonTypes;

    /**
     * The id for all tabs of this type (if the type is singular).
     */
    private final UUID singularId;

    /**
     * Constructor for tabs <b>not</b> created by TAKX.
     * @see #TabType(String, boolean, ButtonType...) for parameter descriptions
     * @since 3.4
     */
    TabType()
    {
        this(DEFAULT_DISPLAY_TEXT, false);
    }

    /**
     * Constructor for tabs created by TAKX, which are all singular.
     * @see #TabType(String, boolean, ButtonType...) for parameter descriptions
     * @since 3.4
     */
    TabType(@Nonnull String displayText, @Nonnull ButtonType... dynamicButtonTypes)
    {
        this(displayText, true, dynamicButtonTypes);
    }

    /**
     * Constructor.
     *
     * @param displayText The text displayed for the tab
     * @param singular Indicates whether there will only ever be one unique tab with this type
     * @param dynamicButtonTypes The button types to be added to the tab dynamically by TAKX (or empty list if none)
     * @since 2.37 (signature changed in 3.4)
     */
    TabType(@Nonnull String displayText, boolean singular, @Nonnull ButtonType... dynamicButtonTypes)
    {
        this.displayText = displayText;
        this.singular = singular;
        this.dynamicButtonTypes = List.of(dynamicButtonTypes);
        singularId = singular ? UUIDUtils.generateType3UUID(name()) : null;
    }

    /**
     * @return The text displayed for the tab.
     * @since 2.0 (renamed in 3.4)
     */
    @Nonnull
    public String getDisplayText()
    {
        return displayText;
    }

    /**
     * Indicates if this is a singular tab type, meaning there will only ever be one unique tab with this type.
     * <p/>
     * For example, {@link #CONNECT} is a singular type so the tab with that type will have the singular id and
     * the behavior of updating automatically with {@link ButtonType#CONNECTION_PLUGIN} buttons as they are created.
     * <p/>
     * Conversely, {@link #USER_CREATED} is a non-singular type, meaning there may be multiple tabs with that type.
     * They will have different ids and different behavior.
     *
     * @return True if there is only ever one tab with this type.
     * @since 3.4
     */
    public boolean isSingular()
    {
        return singular;
    }

    /**
     * @return True if this tab may be dynamically updated by TAKX.
     * @since 3.4
     */
    public boolean isUpdatesDynamically()
    {
        return !dynamicButtonTypes.isEmpty();
    }

    /**
     * @return The list of {@link ButtonType}s which are dynamically added to the tab by TAKX (or empty if this tab is
     * not updated dynamically).
     * @since 3.4
     */
    @Nonnull
    public List<ButtonType> getDynamicButtonTypes()
    {
        return dynamicButtonTypes;
    }

    /**
     * @return The list of tabs that are created by TAKX by default.
     * @since 3.4
     */
    @Nonnull
    public static List<TabType> getDefaultTabTypes()
    {
        return Arrays.stream(values()).filter(tabType -> tabType.singular).toList();
    }

    /**
     * @return The id for all tabs of this type (if the type is singular), or {@code null} if the tab is not singular.
     * @since 3.4
     * @see #isSingular()
     */
    @Nullable
    public UUID getSingularId()
    {
        return singularId;
    }

    /**
     * @return The id for all tabs of this type.
     * @throws NullPointerException If the singular id is {@code null}
     * @since 3.4
     */
    @Nonnull
    public UUID requireSingularId()
    {
        return Objects.requireNonNull(singularId, "singularId is null");
    }

    /**
     * @param buttonType The button type to lookup
     * @return The tab type that has the provided button type dynamically added (or {@code null} if the button type does not
     * dynamically update any tabs).
     * @since 3.4
     */
    @Nullable
    public static TabType getTabWithDynamicButtonType(@Nonnull ButtonType buttonType)
    {
        return DYNAMIC_BUTTON_TYPE_MAP.get(buttonType);
    }
}
