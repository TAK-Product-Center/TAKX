package gov.takx.api.plugin.io;

import gov.takx.api.io.Format;
import gov.takx.api.io.ImportExportResult;
import gov.takx.api.plugin.annotations.ImportExportFormat;
import gov.takx.platform.KeyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Plugin Convenience class that provides default implementations of some methods for an Import/Export wizard.
 * Plugin Developers are encouraged to extend this class rather than the IExtendedImportExportWizard interface.
 *
 * @since 2.13
 */
public abstract class AImportExportWizard implements IExtendedImportExportWizard
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * List of formats supported by the wizard, injected from the plugin annotation
     * {@link ImportExportFormat}
     */
    private List<Format> formats;

    /**
     * A list of all result listeners.
     *
     * @since 1.14
     */
    protected List<IWizardResultListener> resultListeners = new CopyOnWriteArrayList<>();

    /**
     * Delegate that provides access to TAKX services. This delegate is injected by the framework through
     * a call to {@link #setDelegate(IImportExportPluginDelegate)}.
     */
    protected IImportExportPluginDelegate delegate;

    /**
     * Pre-configuration settings.
     *
     * @since 2.16
     */
    protected Map<String, Object> preconfSettings = new HashMap<>();

    @Override
    public final void setFormats(List<Format> formats)
    {
        this.formats = formats;
    }

    @Override
    public final List<Format> getFormats()
    {
        return formats;
    }

    @Override
    public void setDelegate(IImportExportPluginDelegate delegate)
    {
        this.delegate = delegate;
        onDelegateInjected();
    }

    /**
     * Event triggered immediately after the IImportExportPluginDelegate has been injected into the controller.  This event
     * can be used by classes to perform any setup once the delegate has been injected.
     */
    protected void onDelegateInjected()
    {
        // Default to empty
    }

    /**
     * Does the wizard support canceling an Import/Export operation that has started but not finished?
     * This implementation returns false. Subclasses may override to change cancelablity.
     *
     * @return false
     */
    @Override
    public boolean allowCancel()
    {
        return false;
    }

    /**
     * Called by the wizard framework if the user chooses to cancel the operation. This implementation does nothing.
     *
     * @see #allowCancel()
     */
    @Override
    public void cancel()
    {
        // Default to empty
    }

    @Override
    public void addResultListener(IWizardResultListener listener)
    {
        resultListeners.add(listener);
    }

    @Override
    public void preconfigureWizard(Map<String, Object> configuration)
    {
        preconfSettings = new HashMap<>(configuration);
    }

    @Override
    public Map<String, Object> getPreconfigurationSettings()
    {
        return preconfSettings;
    }

    @Override
    public String getDisplayName()
    {
        logger.debug("No display name is specified for this wizard.");
        return delegate == null ? getClass().getSimpleName() : KeyUtils.getTypeFromKey(delegate.getUniqueKey());
    }

    @Override
    public URL getIcon()
    {
        logger.debug("No icon is specified for this wizard. Attempting to use plugin icon.");
        return delegate == null ? null : delegate.getPluginIconURL();
    }

    @Override
    public String getHelpMessage()
    {
        logger.debug("No help message is specified for this importer.");
        return getDisplayName();
    }

    @Override
    public boolean offerToProcessFile(String fileExtension, File file)
    {
        for (Format format : getFormats())
        {
            for (String fileType : format.getFileTypes())
            {
                if (fileType.equals(fileExtension))
                {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean supportsMultipleFiles()
    {
        return false;
    }

    @Override
    public boolean overrideDefaultImporters()
    {
        return false;
    }

    /**
     * Helper method that notifies all registered listeners of the wizard's results.
     *
     * @param result The wizard's import/export results.
     * @since 1.14
     */
    protected void notifyResultListeners(ImportExportResult result)
    {
        for (IWizardResultListener listener : resultListeners)
        {
            listener.onResults(result);
        }
    }
}
