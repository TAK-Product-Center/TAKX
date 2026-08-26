package gov.takx.core.view.gui.toolbar;

/**
 * Client properties for use with {@link JScrollableToolBar}. It is patterned after
 * {@link com.formdev.flatlaf.FlatClientProperties}.
 *
 * @since 3.3
 */
@SuppressWarnings("unused")
public class ScrollableToolbarProperties
{
    /**
     * Specifies the tooltip for the "more tools" button.
     * <p>
     * <strong>Component</strong> {@link JScrollableToolBar}<br>
     * <strong>Value type</strong> {@link String}<br>
     * <strong>Allowed Values</strong>
     *     Any string
     */
    public static final String TOOL_BAR_MORE_TOOLS_BUTTON_TOOL_TIP = "JScrollableToolBar.moreToolsButtonToolTipText";

    /**
     * Specifies the display policy for the "more tools" button, which shows a popup menu with the (partly) hidden tools.
     * <p>
     * <strong>Component</strong> {@link JScrollableToolBar}<br>
     * <strong>Value type</strong> {@link String}<br>
     * <strong>Allowed Values</strong>
     *     {@link #TOOL_BAR_POLICY_NEVER} or
     *     {@link #TOOL_BAR_POLICY_AS_NEEDED}
     */
    public static final String TOOL_BAR_TOOLS_POPUP_POLICY = "JScrollableToolBar.toolsPopupPolicy";

    /**
     * Specifies the display policy for the forward/backward scroll arrow buttons.
     * <p>
     * <strong>Component</strong> {@link JScrollableToolBar}<br>
     * <strong>Value type</strong> {@link String}<br>
     * <strong>Allowed Values</strong>
     *     {@link #TOOL_BAR_POLICY_NEVER},
     *     {@link #TOOL_BAR_POLICY_AS_NEEDED} or
     *     {@link #TOOL_BAR_POLICY_AS_NEEDED_SINGLE}
     */
    public static final String TOOL_BAR_SCROLL_BUTTONS_POLICY = "JScrollableToolBar.scrollButtonsPolicy";

    /**
     * Display never.
     *
     * @see #TOOL_BAR_TOOLS_POPUP_POLICY
     * @see #TOOL_BAR_SCROLL_BUTTONS_POLICY
     */
    public static final String TOOL_BAR_POLICY_NEVER = "never";

    /**
     * Display only when needed.
     * <p>
     * If used for {@link #TOOL_BAR_SCROLL_BUTTONS_POLICY}, both scroll arrow buttons are either shown or hidden.
     * Buttons are disabled if scrolling in that direction is not applicable.
     *
     * @see #TOOL_BAR_TOOLS_POPUP_POLICY
     * @see #TOOL_BAR_SCROLL_BUTTONS_POLICY
     */
    public static final String TOOL_BAR_POLICY_AS_NEEDED = "asNeeded";

    /**
     * Display single button only when needed. When used for {@link #TOOL_BAR_SCROLL_BUTTONS_POLICY}, if the toolbar
     * is scrolled all the way to one side and there are buttons hidden on the other side, only the scroll button on the
     * other side will be shown. No button will be shown on the side where no buttons are hidden. The difference
     * between this setting and {@link #TOOL_BAR_POLICY_AS_NEEDED} is buttons will be hidden instead of disabled.
     * <p>
     * If scroll button placement is trailing, then this option is ignored and both buttons are shown or hidden as needed.
     *
     * @see #TOOL_BAR_SCROLL_BUTTONS_POLICY
     */
    public static final String TOOL_BAR_POLICY_AS_NEEDED_SINGLE = "asNeededSingle";

    /**
     * Specifies the placement of the forward/backward scroll arrow buttons.
     * <p>
     * <strong>Component</strong> {@link JScrollableToolBar}<br>
     * <strong>Value type</strong> {@link String}<br>
     * <strong>Allowed Values</strong>
     *     {@link #TOOL_BAR_PLACEMENT_BOTH} or
     *     {@link #TOOL_BAR_PLACEMENT_TRAILING}
     */
    public static final String TOOL_BAR_SCROLL_BUTTONS_PLACEMENT = "JScrollableToolBar.scrollButtonsPlacement";

    /**
     * The forward/backward scroll arrow buttons are placed on both sides of the toolbar area.
     * The backward scroll button at the left/top side.
     * The forward scroll button at the right/bottom side.
     *
     * @see #TOOL_BAR_SCROLL_BUTTONS_PLACEMENT
     */
    public static final String TOOL_BAR_PLACEMENT_BOTH = "both";

    /**
     * The forward/backward scroll arrow buttons are placed on the trailing side of the toolbar area.
     *
     * @see #TOOL_BAR_SCROLL_BUTTONS_PLACEMENT
     */
    public static final String TOOL_BAR_PLACEMENT_TRAILING = "trailing";

    private ScrollableToolbarProperties()
    {
    }
}
