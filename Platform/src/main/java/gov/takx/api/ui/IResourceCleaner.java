package gov.takx.api.ui;

/**
 * Contract for components that need to free up allocated resources.  This is particularly useful for panels that get
 * added to any of the Map Object dialogs (primary dialog, preferences dialog, etc.) that need to release
 * resources when the dialog is disposed.
 *
 * @since 2.37, moved here from Client
 */
public interface IResourceCleaner
{
    /**
     * Release allocated resources and unregister any listeners
     */
    void releaseResources();
}
