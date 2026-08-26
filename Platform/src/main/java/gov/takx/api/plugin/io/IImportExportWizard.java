package gov.takx.api.plugin.io;

import gov.takx.api.io.Format;
import gov.takx.api.io.ImportExportResult;
import gov.takx.api.plugin.annotations.ImportExportFormat;
import gov.takx.api.plugin.annotations.ImportExportPlugin;
import gov.takx.api.ui.IImportExportSwingUIProvider;

import java.util.List;
import java.util.Map;

/**
 * Contract for Import / Export Wizard implementations.
 * <p>
 * Each wizard may support multiple data formats. The user will select one format from a list.
 * <p/>
 * Life of an Import/Export wizard:
 * <ul>
 * <li>Plugin is created.</li>
 * <li>Framework gets formats from Plugin and injects formats into wizard {@link #setFormats(List)}</li>
 * <li>Framework calls {@link #setDelegate(IImportExportPluginDelegate)}.</li>
 * <li>Framework calls {@link #getFormats()}.</li>
 * <li>User selects a format.</li>
 * <li>Framework calls {@link #setFormat(Format)} with selected format.</li>
 * <li>Framework calls {@link IImportExportSwingUIProvider#createWizardPanels()} and {@link #getFirstWizardPanel()}.</li>
 * <li>User goes through wizard.</li>
 * <li>Wizard is destroyed.</li>
 * </ul>
 *
 * @since 2.13
 */
public interface IImportExportWizard
{
    /**
     * Inject the formats, as specified by the {@link ImportExportFormat} annotations in
     * {@link ImportExportPlugin#formats()}
     *
     * @param formats list of supported formats
     */
    void setFormats(List<Format> formats);

    /**
     * Gets the formats supported by this wizard. The wizard must support at least one format, and may support many.
     *
     * @return The formats supported by this wizard.
     */
    List<Format> getFormats();

    /**
     * Called by the wizard framework when the user selects a format.
     *
     * @param format The selected format. This will be one of the elements of the list returned by {@link #getFormats()}.
     */
    void setFormat(Format format);

    /**
     * Get the preconfiguration Map settings for the wizard.
     *
     * @return settings map
     * @since 2.13
     */
    Map<String, Object> getPreconfigurationSettings();

    /**
     * Get the identifier for the first panel in the wizard. This string must match one of the keys in the map
     * returned by {@link IImportExportSwingUIProvider}.
     *
     * @return Identifier of the first wizard panel.
     * @see IImportExportSwingUIProvider
     */
    String getFirstWizardPanel();

    /**
     * Set a delegate that gives the plugin access to platform services.
     *
     * @param delegate Delegate that provides access to the TAKX platform.
     */
    void setDelegate(IImportExportPluginDelegate delegate);

    /**
     * Does the wizard support canceling an Import/Export operation that has started but not finished? Return true if the user
     * should have the option to cancel the operation, or false if cancellation is not supported.
     *
     * @return True if the wizard supports cancellation, of false if not.
     * @see #cancel()
     */
    boolean allowCancel();

    /**
     * Called by the wizard framework if the user chooses to cancel the operation. This method will never
     * be called if {@link #allowCancel()} returns false.
     *
     * @see #allowCancel()
     */
    void cancel();

    /**
     * Add a listener that will be notified when this wizard has finished processing.
     * <br><br>
     * The app (or device or feature) that invokes the wizard will register itself
     * as a result listener and be passed back the {@link ImportExportResult}.
     *
     * @param listener The listener to add.
     * @since 1.14
     */
    void addResultListener(IWizardResultListener listener);

    /**
     * Add configuration information to this wizard. This information will allow
     * an importer/exporter to skip screens that gather information already defined
     * by the configuration object.
     * <p/>
     * IImportExportPlugins must Javadoc the expected key/value pairs that they will
     * look for in the {@code configuration} map.
     *
     * @param configuration The set of configuration options being provided to the wizard.
     * @since 1.14
     */
    void preconfigureWizard(Map<String, Object> configuration);
}
