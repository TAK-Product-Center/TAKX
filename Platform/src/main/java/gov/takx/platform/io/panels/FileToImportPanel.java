package gov.takx.platform.io.panels;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.io.Format;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.SwingFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileSystemView;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static javax.swing.JFileChooser.FILES_AND_DIRECTORIES;

/**
 * Wizard panel for choosing a file to import. The user is prompted to
 * enter a file name, or browse to a location. The panel can be
 * created with a list of formats that will be used to enforce a file
 * chooser filter. Once the user has made a selection, the panel will
 * store the selected file path in the wizard properties under
 * {@link CommonWizardSettings#FILE}.
 *
 */
@SuppressWarnings("unused")     // used by plugins
public class FileToImportPanel extends AFileImportExportPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Whether or not multi-select is supported by this importer
     */
    private boolean supportsMultiselect = false;

    /**
     * Identifier for this wizard panel.
     */
    public static final String IDENTIFIER = "platform.io.panels.FileToImportPanel";

    /**
     * Create the panel.
     *
     * @param format Format to use to create a file chooser filter.
     */
    public FileToImportPanel(Format format)
    {
        this(Collections.singletonList(format));
    }

    /**
     * Create the panel.
     *
     * @param formats Formats to use to create a file chooser filter.
     */
    public FileToImportPanel(List<? extends Format> formats)
    {
        init(formats, null, null);
    }

    /**
     * Create the panel.
     *
     * @param formats Formats to use to create a file chooser filter.
     */
    @SuppressWarnings("UnusedDeclaration") // Called from subclasses in plugins
    public FileToImportPanel(List<? extends Format> formats, Map<String, Object> preconfigMap, FileSystemView view)
    {
        File defaultFile = null;
        if (preconfigMap != null)
        {
            Object prop = preconfigMap.get(CommonWizardSettings.FILE);
            if (prop != null)
            {
                if (prop instanceof File)
                {
                    defaultFile = (File) prop;
                } else if (prop instanceof String)
                {
                    defaultFile = new File((String) prop);
                } else
                {
                    logger.error("Unknown type for \"" + CommonWizardSettings.FILE + "\": {}", prop.getClass().getCanonicalName());
                }
            } else
            {
                logger.debug("Could not find " + CommonWizardSettings.FILE);
            }
        }

        setPanelIsBeingSkipped(defaultFile != null);

        init(formats, defaultFile, view);
    }

    /**
     * Create the panel.
     *
     * @param formats Formats to use to create a file chooser filter.
     */
    public FileToImportPanel(List<? extends Format> formats, File currentDirectory, FileSystemView view)
    {
        init(formats, currentDirectory, view);
    }

    private void init(List<? extends Format> formats, File currentDirectory, FileSystemView view)
    {
        List<String> filters = new ArrayList<>();

        for (Format format : formats)
        {
            Collections.addAll(filters, format.getFileTypes());
        }

        filter = new SwingFileFilter(filters);

        initComponents();
        if (currentDirectory != null && view != null)
        {
            chooser = AlwaysOnTopFileChooserFactory.getFileChooser(this, currentDirectory, view);
        } else if (currentDirectory != null)
        {
            chooser = AlwaysOnTopFileChooserFactory.getFileChooser(this, currentDirectory);
        } else
        {
            chooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        }
        // This is so that if the user selects the same file twice in a row,
        // a property change event will actually be fired the second time
        if (currentDirectory != null && currentDirectory.isFile())
        {
            chooser.setSelectedFile(currentDirectory);
        } else
        {
            chooser.setSelectedFile(null);
        }
        chooser.setControlButtonsAreShown(false);
        chooser.resetChoosableFileFilters();
        chooser.setFileFilter(filter);
        chooser.addPropertyChangeListener(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, evt -> validateContents());

        textField = (JTextField) findJTextField(chooser);
        textField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                validateContents();
            }
        });
        textField.setText("");

        add(chooser, new CellConstraints(1, 3));
    }

    @Override
    public String getIdentifier()
    {
        return IDENTIFIER;
    }

    /**
     * Allow multi select in the file chooser.  The IE Dialog will take care of setting this value based on
     * whether or not a plugin declares that is supports multiple files at once.
     *
     * @param allow boolean allow multi-select.
     */
    public void allowMultiSelect(boolean allow)
    {
        supportsMultiselect = allow;
        chooser.setMultiSelectionEnabled(allow);
        chooser.setFileSelectionMode(allow ? FILES_AND_DIRECTORIES : JFileChooser.FILES_ONLY);
    }

    protected void validateContents()
    {
        // Wizard will still be null when this method is called during instantiation.
        if (wizard != null)
        {
            if (supportsMultiselect)
            {
                // Not sure if this is enough of a validation, but since we are only getting the files from
                // the chooser in this case (as opposed to possibly the text box), maybe it is.
                wizard.getModel().setNextButtonEnabled(chooser.getSelectedFiles().length > 0);
            } else
            {
                File selectedFile = getSelectedFile();
                wizard.getModel().setNextButtonEnabled(selectedFile.exists() && selectedFile.isFile() && filter.accept(selectedFile));
            }
        }
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
        if (supportsMultiselect)
        {
            wizard.getModel().setProperty(CommonWizardSettings.FILES, getSelectedFiles());
        } else
        {
            wizard.getModel().setProperty(CommonWizardSettings.FILE, getSelectedFile().getPath());
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var label1 = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default, $lgap, fill:default:grow"));

        //---- label1 ----
        label1.setText("Select the file to import:");
        label1.setName("label1");
        add(label1, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
