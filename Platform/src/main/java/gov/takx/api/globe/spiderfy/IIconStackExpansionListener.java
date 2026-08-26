package gov.takx.api.globe.spiderfy;

/**
 * Icon Stack expansion event listener that can be notified when an icon is expanded to reveal its nested icons
 * or collapsed to hide its nested icons.
 *
 * @since 2.26
 */
public interface IIconStackExpansionListener
{
    /**
     * A notification that {@code iconStack} has been expanded to reveal its nested icons.
     *
     * @param iconStack The icon stack that was expanded.
     */
    void iconStackExpanded(IIconStack iconStack);

    /**
     * A notification that {@code iconStack} has been collapsed to reveal its nested icons.
     *
     * @param iconStack The icon stack that was collapsed.
     */
    void iconStackCollapsed(IIconStack iconStack);
}
