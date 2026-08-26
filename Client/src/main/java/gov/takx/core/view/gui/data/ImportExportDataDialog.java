package gov.takx.core.view.gui.data;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.io.Format;
import gov.takx.api.io.ImportExportResult;
import gov.takx.api.plugin.PluginConstants;
import gov.takx.api.plugin.io.AWizardPanel;
import gov.takx.api.plugin.io.IExportWizard;
import gov.takx.api.plugin.io.IExtendedImportExportWizard;
import gov.takx.api.plugin.io.IImportExportWizard;
import gov.takx.api.plugin.io.IImportWizard;
import gov.takx.api.tasks.ITaskManager;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.wizard.IWizard;
import gov.takx.commons.wizard.IWizardModel;
import gov.takx.commons.wizard.IWizardPanelDescriptor;
import gov.takx.core.ioservice.AImportExportTask;
import gov.takx.core.ioservice.ExportWizardTask;
import gov.takx.core.ioservice.ImportWizardTask;
import gov.takx.core.uibridge.SwingUIBinder;
import gov.takx.core.view.gui.DialogHeaderPanel;
import gov.takx.platform.cdi.InjectableContainerSupport;
import gov.takx.platform.io.ImportExportMode;
import gov.takx.platform.io.panels.CommonWizardSettings;
import gov.takx.platform.io.panels.FileToImportPanel;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Dialog responsible for all data import and export.
 *
 * @since 1.14
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class ImportExportDataDialog extends AStandardDialog implements IWizard
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final InjectableContainerSupport BEAN_ACCESSOR = new InjectableContainerSupport();

    /**
     * Task manager to run export/import task
     */
    @Inject
    private ITaskManager taskManager;

    /**
     * Swing UI Binder to use for getting wizard panels
     */
    @Inject
    private SwingUIBinder swingUIBinder;

    /**
     * All of the panels provided by the import/export plugin.
     */
    private Map<String, IWizardPanelDescriptor> panels;

    /**
     * The panel currently being displayed.
     */
    private IWizardPanelDescriptor currentPanel;

    /**
     * The wizard provided by the import/export plugin.
     */
    private final IImportExportWizard wizard;

    /**
     * The model provided by this class to the import/export plugin's
     * panels used to track the current state of configuration.
     */
    private IWizardModel model;

    /**
     * The data format title.
     */
    private final String title;

    /**
     * The image associated with the import/export plugin.
     */
    private final BufferedImage iconImage;

    /**
     * Import/export mode.
     */
    private final ImportExportMode mode;

    /**
     * Boolean placeholder denoting whether or not the IE plugin supports multi-select.  Only valid for
     * Import plugins when the operator selects multiple files in the chooser.
     */
    private boolean supportsMultiImport = false;

    /**
     * If true then run the operation at the end of the wizard chain. If false then the call to run will happen at some
     * point in the future.
     */
    private final boolean doRun;

    /**
     * Wizard return code, default to cancel.
     */
    private int return_code = JOptionPane.CANCEL_OPTION;

    /**
     * Wizard description
     */
    private final String description;

    /**
     * Format supported by wizard
     */
    private final Format format;

    public static ImportExportDataDialog dialogFor(IImportExportWizard wizard, ImportExportMode mode, Format format, BufferedImage iconImage, boolean doRun)
    {
        final SwingUIBinder swingUIBinder = BEAN_ACCESSOR.getBean(SwingUIBinder.class);

        if (!swingUIBinder.isUIProviderPresent(wizard))
        {
            // If the wizard says its first panel is FINISH, then it doesn't need to interact with the user
            if (!wizard.getFirstWizardPanel().equals(IWizardPanelDescriptor.FINISH))
            {
                final String message = mode.displayName() + (format != null ? " " + format.getTitle() : "") + " Data";
                swingUIBinder.showNoSwingUIDialog(message);
            }
            return null;
        }

        return new ImportExportDataDialog(wizard, mode, format, iconImage, doRun);
    }

    /**
     * Constructor.
     *
     * @param wizard    The wizard provided by the plugin
     * @param mode      Import or export mode
     * @param format    The format of the data being processed
     * @param iconImage The icon image associated with the import/export plugin
     */
    public ImportExportDataDialog(IImportExportWizard wizard, ImportExportMode mode, Format format, BufferedImage iconImage, boolean doRun)
    {
        this.iconImage = iconImage;
        initComponents();

        this.wizard = wizard;
        this.mode = mode;
        title = getTitle();
        description = format != null ? format.getNotes() : "";
        this.doRun = doRun;
        this.format = format;
        setModal(true);
        setTitle(mode.displayName() + (format != null ? " " + format.getTitle() : "") + " Data");

        // If this is an import, ask the plugin if it supports multi files so we can set the chooser
        if (wizard instanceof IExtendedImportExportWizard && mode == ImportExportMode.IMPORT)
        {
            supportsMultiImport = ((IExtendedImportExportWizard) wizard).supportsMultipleFiles();
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @PostConstruct
    private void postConstruct()
    {
        // Get the set of custom panels.
        if (wizard == null)
        {
            panels = getFileChooserOnly();
        } else
        {
            wizard.setFormat(format);
            try
            {
                panels = swingUIBinder.forWizard(wizard).createWizardPanels();
            } catch (Exception e)
            {
                logger.warn(PluginConstants.PLUGIN_EXCEPTION + ": Exception in ImportExportDataDialog getting wizard panels", e);
            }
        }

        // TODO:  It is an error for a plugin not to provide any I/O panels.  Exit more gracefully rather than NPEs

        panels.values().stream()
                .filter(IResourceCleaner.class::isInstance)
                .map(IResourceCleaner.class::cast)
                .forEach(this::addResourceCleaner);

        // Prepare a model in which state data will be kept.
        model = new WizardModel();

        // Set the icon if one is available.
        if (iconImage != null)
        {
            dialogHeaderPanel.setIcon(iconImage);
        }

        dialogHeaderPanel.setDescription(description);

        // Determine the min/preferred width and height based on the largest
        // dimensions required to display the panels.
        int width = 0;
        int height = 0;

        // Add all panels.
        for (Map.Entry<String, IWizardPanelDescriptor> entry : panels.entrySet())
        {
            entry.getValue().registerPanel(this);

            // Temporarily make each panel the current panel and capture the total dimensions of the dialog.
            setCustomPanel(entry.getKey());
            panelCustom.revalidate();
            pack();
            width = Math.max(width, getWidth());
            height = Math.max(height, getHeight());
        }

        // Set this dialogs min and preferred size. Users can make it larger if they desire.
        setMinimumSize(new Dimension(width, height));
        setPreferredSize(new Dimension(width, height));

        // Keep track of the wizard being used.
        model.setProperty(FormatChooserPanelDescriptor.SELECTED_WIZARD, wizard);

        // Keep track of the format being used.
        model.setProperty(CommonWizardSettings.FORMAT, format);

        // Get the first panel. If no wizard was provided, use the default (file chooser).
        final String firstPanelId = wizard != null
                ? wizard.getFirstWizardPanel()
                : FormatChooserPanelDescriptor.IDENTIFIER;
        currentPanel = new FirstWizardPanelDescriptor(firstPanelId);
        Object firstPanelDescriptor = getNextPanelDescriptor();

        if (firstPanelDescriptor.equals(IWizardPanelDescriptor.FINISH))
        {
            setVisible(false);
            buttonNextActionPerformed();
            currentPanel = null;

            // If we're never going to run, let's copy the FILE from preconfig to model as a convenience
            final Map<String, Object> preconfigurationSettings = wizard.getPreconfigurationSettings();
            final Object f = preconfigurationSettings != null ? preconfigurationSettings.get(CommonWizardSettings.FILE) : null;
            if (f instanceof File)
            {
                // Copy in the FILE, but it is expected to be a String instead of a File, ugh.
                model.setProperty(CommonWizardSettings.FILE, ((File) f).getPath());
            }
            return;
        }

        // If we have a file name already then let's put it into the window title
        Object file = model.getProperty(CommonWizardSettings.FILE);
        String filename = "";
        if (file != null)
        {
            if (file instanceof File)
            {
                filename = ((File) file).getName();
            } else if (file instanceof String)
            {
                filename = new File((String) file).getName();
            }
            setTitle(getTitle() + " For " + filename);
        }

        IWizardPanelDescriptor firstPanel = panels.get(firstPanelDescriptor);
        if (FileToImportPanel.IDENTIFIER.equals(firstPanelDescriptor.toString()))
        {
            ((FileToImportPanel) firstPanel).allowMultiSelect(supportsMultiImport);
        }

        // Display the first panel.
        firstPanel.aboutToDisplayPanel();
        setCustomPanel(firstPanelDescriptor.toString());
        firstPanel.displayingPanel();

        currentPanel = panels.get(firstPanelDescriptor);

        // Check to see if the first panel is the only panel.
        // If so, update the "Next" button's text to read "Import" or "Export".
        if (currentPanel.getNextPanelDescriptor().equals(IWizardPanelDescriptor.FINISH))
        {
            model.setNextButtonText(mode.displayName());
        }

        SwingUtilities.invokeLater(this::pack);
    }

    @Override
    public IWizardPanelDescriptor getCurrentPanel()
    {
        return currentPanel;
    }

    @Override
    public IWizardPanelDescriptor getWizardPanel(Object id)
    {
        return panels.get(id.toString());
    }

    @Override
    public void registerWizardPanel(Object id, IWizardPanelDescriptor panel)
    {
        panels.put(id.toString(), panel);
        panelCustom.add((Component) panel, id);
    }

    @Override
    public void setCurrentPanelDescriptor(Object id)
    {
        setCustomPanel(id.toString());
    }

    @Override
    public IWizardModel getModel()
    {
        return model;
    }

    @Override
    public int getReturnCode()
    {
        return return_code;
    }

    @Override
    public JDialog getDialog()
    {
        return this;
    }

    @Override
    public int showModalDialog()
    {
        return 0;
    }

    @Override
    public boolean isBackButtonEnabled()
    {
        return buttonBack.isEnabled();
    }

    @Override
    public void setBackButtonEnabled(boolean b)
    {
        buttonBack.setEnabled(b);
    }

    @Override
    public boolean isNextButtonEnabled()
    {
        return buttonNext.isEnabled();
    }

    @Override
    public void setNextButtonEnabled(boolean b)
    {
        buttonNext.setEnabled(b);
    }

    @Override
    public boolean isCancelButtonEnabled()
    {
        return buttonCancel.isEnabled();
    }

    @Override
    public void setCancelButtonEnabled(boolean b)
    {
        buttonCancel.setEnabled(b);
    }

    @Override
    public void giveFocusToBackButton()
    {
        buttonBack.requestFocusInWindow();
    }

    @Override
    public void giveFocusToNextButton()
    {
        buttonNext.requestFocusInWindow();
    }

    @Override
    public void giveFocusToCancelButton()
    {
        buttonCancel.requestFocusInWindow();
    }

    @InvokeOnEDT
    private void setCustomPanel(final String id)
    {
        panelCustom.removeAll();
        panelCustom.revalidate();
        panelCustom.repaint();
        panelCustom.add((Component) panels.get(id), new CellConstraints(1, 1));
        panelCustom.revalidate();
    }

    /**
     * Instantiates a file chooser panel for plugins that do not provide a wizard.
     *
     * @return A file chooser panel.
     */
    private Map<String, IWizardPanelDescriptor> getFileChooserOnly()
    {
        Map<String, IWizardPanelDescriptor> result = new HashMap<>();
        result.put(FormatChooserPanelDescriptor.IDENTIFIER, FormatChooserPanelDescriptor.createForImport());
        return result;
    }

    /**
     * Responds to "Next" button clicks.
     */
    private void buttonNextActionPerformed()
    {
        // Ensure the current panel is prepared to move forward
        if (!currentPanel.forwardValidate()) return;

        Object nextDescriptor = getNextPanelDescriptor();

        if (nextDescriptor == null || nextDescriptor.equals(IWizardPanelDescriptor.FINISH))
        {
            setVisible(false);
            return_code = JOptionPane.YES_OPTION;
            // If this is the last panel, do the import/export and dispose of this dialog.
            if (doRun) runOperation();
        }
    }

    /**
     * Run the import/export operation.
     *
     * @return CompletableFuture holding the result, which will be completed when this task has completed execution
     */
    public CompletableFuture<ImportExportResult> runOperation()
    {
        dispose();

        final AImportExportTask<? extends IImportExportWizard> task = mode == ImportExportMode.IMPORT
                ? new ImportWizardTask(title, (IImportWizard) wizard, model)
                : new ExportWizardTask(title, (IExportWizard) wizard, model);

        taskManager.execute(task);

        return task.getResultFuture();
    }

    private Object getNextPanelDescriptor()
    {
        // Notify the current panel that it is about to be transitioned
        // away from.
        Object nextDescriptor = null;
        if (currentPanel != null)
        {
            currentPanel.aboutToHidePanel();
            nextDescriptor = currentPanel.getNextPanelDescriptor();
        }

        // If this is not the last panel, update the back/next buttons and transition
        // to the next panel.
        if (nextDescriptor != null && !nextDescriptor.equals(IWizardPanelDescriptor.FINISH))
        {
            String nextPanelId = nextDescriptor.toString();
            currentPanel = panels.get(nextPanelId);

            Object backDescriptor = currentPanel.getBackPanelDescriptor();
            if (!IWizardPanelDescriptor.BEGIN.equals(backDescriptor))
            {
                setBackButtonEnabled(true);
            }

            currentPanel.aboutToDisplayPanel();
            setCustomPanel(nextPanelId);
            if (currentPanel instanceof AWizardPanel)
            {
                if (((AWizardPanel) currentPanel).isPanelBeingSkipped())
                {
                    return getNextPanelDescriptor();
                }
            }

            currentPanel.displayingPanel();

            if (currentPanel.getNextPanelDescriptor().equals(IWizardPanelDescriptor.FINISH))
            {
                model.setNextButtonText(mode.displayName());
            }

            // It's unfortunate, but since the plugin creates the FileImport panel we have to get in the
            // way and tell it after the fact that it supports multi-select.
            if (FileToImportPanel.IDENTIFIER.equals(nextPanelId))
            {
                ((FileToImportPanel) currentPanel).allowMultiSelect(supportsMultiImport);
            }
        }

        return nextDescriptor;
    }

    /**
     * Process "Back" button clicks.
     */
    private void buttonBackActionPerformed()
    {
        // If moving back, the next button should always read "Next"
        // and not "Import"/"Export".
        model.setNextButtonText("Next");

        // Notify the current panel that it is about to be hidden.
        currentPanel.aboutToHidePanel();

        // Update the current panel to the previous panel.
        String previousPanelId = currentPanel.getBackPanelDescriptor().toString();
        currentPanel = panels.get(previousPanelId);

        currentPanel.aboutToDisplayPanel();
        setCustomPanel(previousPanelId);
        currentPanel.displayingPanel();

        // If the user has clicked the back button then we are no longer skipping the panel
        if (currentPanel instanceof AWizardPanel) ((AWizardPanel) currentPanel).setPanelIsBeingSkipped(false);

        // Update the back button.
        Object descriptor = currentPanel.getBackPanelDescriptor();
        setBackButtonEnabled(descriptor != null && !descriptor.equals(IWizardPanelDescriptor.BEGIN));
    }

    /**
     * Respond to cancel button clicks.
     */
    private void buttonCancelActionPerformed()
    {
        return_code = JOptionPane.CANCEL_OPTION;
        dispose();
    }

    @Nonnull
    public Map<String, Object> getPreconfigureProperties()
    {
        Map<String, Object> map = new HashMap<>();
        for (IWizardPanelDescriptor wiz : panels.values())
        {
            Map<String, Object> props = null;
            if (wiz != null)
            {
                props = wiz.getPreconfigureProperties();
            }
            if (props != null) map.putAll(props);
        }
        return map;
    }

    /**
     * Default first panel descriptor.
     *
     * @since 2.37
     */
    private static class FirstWizardPanelDescriptor implements IWizardPanelDescriptor
    {
        private final String firstPanelId;

        public FirstWizardPanelDescriptor(String firstPanelId)
        {
            this.firstPanelId = firstPanelId;
        }

        @Override
        public Component getPanelComponent()
        {
            return null;
        }

        @Override
        public Object getBackPanelDescriptor()
        {
            return BEGIN;
        }

        @Override
        public Object getNextPanelDescriptor()
        {
            return firstPanelId;
        }

        @Override
        public void registerPanel(IWizard wizard)
        {
        }

        @Override
        public void aboutToDisplayPanel()
        {
        }

        @Override
        public void displayingPanel()
        {
        }

        @Override
        public void aboutToHidePanel()
        {
        }

        @Override
        public boolean forwardValidate()
        {
            return true;
        }

        @Override
        @Nullable
        public Map<String, Object> getPreconfigureProperties()
        {
            return null;
        }
    }

    /**
     * The model used to track the current state of the import/export setup. Also provides wizards with
     * some callback functionality.
     */
    private class WizardModel implements IWizardModel
    {
        private final Map<String, Object> properties = new HashMap<>();
        private final List<PropertyChangeListener> listeners = new ArrayList<>();

        @Override
        public Icon getBackButtonIcon()
        {
            return buttonBack.getIcon();
        }

        @Override
        public IWizardPanelDescriptor getWizardPanel(Object id)
        {
            return panels.get(id.toString());
        }

        @Override
        public void registerWizardPanel(Object id, IWizardPanelDescriptor panel)
        {
            panels.put(id.toString(), panel);
            panelCustom.add((Component) panel, id);
        }

        @Override
        public IWizardPanelDescriptor getCurrentPanel()
        {
            return currentPanel;
        }

        @Override
        public boolean setCurrentPanel(Object id)
        {
            if (panels.containsKey(id.toString()))
            {
                setCurrentPanel(id.toString());
                return true;
            } else
            {
                return false;
            }
        }

        @Override
        public String getBackButtonText()
        {
            return buttonBack.getText();
        }

        @Override
        public void setBackButtonText(String newText)
        {
            buttonBack.setText(newText);
        }

        @Override
        public String getNextButtonText()
        {
            return buttonNext.getText();
        }

        @Override
        public void setNextButtonText(String newText)
        {
            buttonNext.setText(newText);
        }

        @Override
        public String getCancelButtonText()
        {
            return buttonCancel.getText();
        }

        @Override
        public void setCancelButtonText(String newText)
        {
            buttonCancel.setText(newText);
        }

        @Override
        public Boolean isBackButtonEnabled()
        {
            return buttonBack.isEnabled();
        }

        @Override
        public void setBackButtonEnabled(Boolean newValue)
        {
            buttonBack.setEnabled(newValue);
        }

        @Override
        public Boolean isNextButtonEnabled()
        {
            return buttonNext.isEnabled();
        }

        @Override
        public void setNextButtonEnabled(Boolean newValue)
        {
            buttonNext.setEnabled(newValue);
        }

        @Override
        public Boolean isCancelButtonEnabled()
        {
            return buttonCancel.isEnabled();
        }

        @Override
        public void setCancelButtonEnabled(Boolean newValue)
        {
            buttonCancel.setEnabled(newValue);
        }

        @Override
        public void setBackButtonIcon(Icon newIcon)
        {
            buttonBack.setIcon(newIcon);
        }

        @Override
        public Icon getNextButtonIcon()
        {
            return buttonNext.getIcon();
        }

        @Override
        public void setNextButtonIcon(Icon newIcon)
        {
            buttonNext.setIcon(newIcon);
        }

        @Override
        public Icon getCancelButtonIcon()
        {
            return buttonCancel.getIcon();
        }

        @Override
        public void setCancelButtonIcon(Icon newIcon)
        {
            buttonCancel.setIcon(newIcon);
        }

        @Override
        public Icon getIconProperty(String propertyName)
        {
            return null;
        }

        @Override
        public Object getProperty(String propertyName)
        {
            return properties.get(propertyName);
        }

        @Override
        public String getStringProperty(String propertyName)
        {
            return (String) properties.get(propertyName);
        }

        @Override
        public Boolean getBooleanProperty(String propertyName)
        {
            return (Boolean) properties.get(propertyName);
        }

        @Override
        public Integer getIntegerProperty(String propertyName)
        {
            return (Integer) properties.get(propertyName);
        }

        @Override
        public void setProperty(String propertyName, Object newValue)
        {
            Object oldValue = properties.get(propertyName);
            properties.put(propertyName, newValue);

            for (PropertyChangeListener listener : listeners)
            {
                listener.propertyChange(new PropertyChangeEvent(this, propertyName, oldValue, newValue));
            }
        }

        @Override
        public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener)
        {
            listeners.add(listener);
        }

        @Override
        public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener)
        {
            listeners.remove(listener);
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        dialogHeaderPanel = new DialogHeaderPanel();
        var contentPanel = new JPanel();
        panelCustom = new JPanel();
        var buttonBar = new JPanel();
        buttonBack = new JButton();
        buttonNext = new JButton();
        buttonCancel = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //---- dialogHeaderPanel ----
            dialogHeaderPanel.setName("dialogHeaderPanel");
            dialogPane.add(dialogHeaderPanel, BorderLayout.NORTH);

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default:grow"));

                //======== panelCustom ========
                {
                    panelCustom.setName("panelCustom");
                    panelCustom.setLayout(new FormLayout(
                            "default:grow",
                            "fill:default:grow"));
                }
                contentPanel.add(panelCustom, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "2*($button, $lcgap), $glue, $lcgap, $button",
                        "fill:default"));

                //---- buttonBack ----
                buttonBack.setText("Back");
                buttonBack.setEnabled(false);
                buttonBack.setName("buttonBack");
                buttonBack.addActionListener(e -> buttonBackActionPerformed());
                buttonBar.add(buttonBack, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

                //---- buttonNext ----
                buttonNext.setText("Next");
                buttonNext.setEnabled(false);
                buttonNext.setName("buttonNext");
                buttonNext.addActionListener(e -> buttonNextActionPerformed());
                buttonBar.add(buttonNext, cc.xy(3, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

                //---- buttonCancel ----
                buttonCancel.setText("Cancel");
                buttonCancel.setName("buttonCancel");
                buttonCancel.addActionListener(e -> buttonCancelActionPerformed());
                buttonBar.add(buttonCancel, cc.xy(7, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private DialogHeaderPanel dialogHeaderPanel;
    private JPanel panelCustom;
    private JButton buttonBack;
    private JButton buttonNext;
    private JButton buttonCancel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
