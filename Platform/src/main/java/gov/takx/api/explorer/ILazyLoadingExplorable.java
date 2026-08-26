package gov.takx.api.explorer;

/**
 * Explorable that expands its children lazily. When an implementation of this class is added
 * to the explorer panel, it will have an expansion icon regardless of the status of its children.
 * Note: once expanded, if the explorable has no children the expansion icon disappears.
 *
 * @since 5.2
 */
public interface ILazyLoadingExplorable extends IExplorable
{
    /**
     *  Implementations will be invoked when the respective node is expanded.
     *  Note: It is incumbent upon the plugin to add children when this method is called.
     *  Failure to do so will cause the expansion icon to disappear.
     */
    void onExpand();
}
