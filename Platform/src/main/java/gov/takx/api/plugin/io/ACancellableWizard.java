package gov.takx.api.plugin.io;

/**
 * Plugin Convenience class that implements common functionality in cancellable wizards.
 * This class implements logic to determine if the wizard has been cancelled. It
 * is up to the wizard implementation to periodically check the status of the cancel flag.
 * A typical implementation would look like this:
 * <p/>
 * <pre>
 *   public IOResult doImport(IWizardProperties props, IIOProgressMonitor progressMonitor) throws Exception {
 *       while (!isCancelled()) {
 *          // import some data
 *       }
 *
 *       if (isCancelled())
 *          // do cleanup in the case of cancelled operation
 *   }
 * </pre>
 *
 * @since 2.13
 */
public abstract class ACancellableWizard extends AImportExportWizard
{
    /**
     * Has the wizard been cancelled?
     */
    private volatile boolean cancelled;

    /**
     * Create the wizard.
     */
    protected ACancellableWizard()
    {
        // Default to empty
    }

    /**
     * Return true, the wizard is cancellable.
     *
     * @return true
     */
    @Override
    public boolean allowCancel()
    {
        return true;
    }

    /**
     * Called if the user clicks the cancel button. Can also be called programmatically if
     * the wizard wants to cancel itself.
     */
    @Override
    public void cancel()
    {
        cancelled = true;
    }

    /**
     * Has the wizard been cancelled?
     *
     * @return True if the wizard has been cancelled, otherwise false.
     */
    public boolean isCancelled()
    {
        return cancelled;
    }
}
