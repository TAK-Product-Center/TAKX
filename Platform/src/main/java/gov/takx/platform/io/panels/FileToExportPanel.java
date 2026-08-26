package gov.takx.platform.io.panels;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.io.Format;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.SwingFileFilter;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Wizard panel for choosing a target file for an export operation. The user is prompted to enter a file name,
 * or browse to a location. The panel can be created with a list of formats that will be used to enforce a file
 * chooser filter. Once the user has made a selection, the panel will store the selected file path in the wizard
 * properties under {@link CommonWizardSettings#FILE}.
 *
 */
public class FileToExportPanel extends AFileImportExportPanel
{
    /**
     * Identifier for this wizard panel.
     */
    public static final String IDENTIFIER = "platform.io.panels.FileToExportPanel";

    /**
     * A reference to the identifier of the wizard panel that originally followed this wizard
     * panel. This panel may temporarily set itself to be the next panel and needs some way to
     * revert back to the original sequence and does so by using this reference.
     *
     * @see #setNextPanel(String)
     * @see #aboutToDisplayPanel()
     * @see #aboutToHidePanel()
     * @since 1.14
     */
    private String realNextPanel;

    /**
     * Available export formats.
     *
     * @since 1.14
     */
    private final List<? extends Format> formats;

    /**
     * Listener for file selections.
     *
     * @since 1.14
     */
    private final PropertyChangeListener fileSelectionPCL;

    /**
     * Listener for text field key strokes.
     *
     * @since 1.14
     */
    private final KeyListener textFieldKL;

    /**
     * Create the panel.
     *
     * @param format Format to use to create a file chooser filter.
     */
    public FileToExportPanel(Format format)
    {
        this(Arrays.asList(format));
    }

    /**
     * Create the panel.
     *
     * @param formats Formats to use to create a file chooser filter.
     */
    public FileToExportPanel(List<? extends Format> formats)
    {
        this.formats = formats;

        List<String> filters = new ArrayList<String>();

        for (Format format : formats)
        {
            filters.addAll(Arrays.asList(format.getFileTypes()));
        }

        filter = new SwingFileFilter(filters);

        initComponents();

        chooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        chooser.cancelSelection();
        chooser.setControlButtonsAreShown(false);
        chooser.resetChoosableFileFilters();
        chooser.setFileFilter(filter);
        fileSelectionPCL = new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                validateContents();
            }
        };

        textField = (JTextField) findJTextField(chooser);
        textField.setText(null);
        textFieldKL = new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                validateContents();
            }
        };

        add(chooser, new CellConstraints(1, 3));
    }

    @Override
    public String getIdentifier()
    {
        return IDENTIFIER;
    }

    private void validateContents()
    {
        if (wizard != null)
        {
            File selectedFile = getSelectedFile();
            wizard.getModel().setNextButtonEnabled(!selectedFile.isDirectory() && (filter.accept(selectedFile) || formats.size() == 1));
        }
    }

    @Override
    public void setNextPanel(String identifier)
    {
        if (!IDENTIFIER.equals(identifier))
        {
            realNextPanel = identifier;
        }

        super.setNextPanel(identifier);
    }

    @Override
    public void aboutToDisplayPanel()
    {
        setNextPanel(realNextPanel);
        chooser.addPropertyChangeListener(fileSelectionPCL);
        textField.addKeyListener(textFieldKL);
        validateContents();
    }

    @Override
    public void displayingPanel()
    {
    }

    @Override
    public void aboutToHidePanel()
    {
        // Lame way to check to see if we are navigating back, in
        // which case we do not need to do an overwrite check. The
        // Next button will read "Export" if navigating forward
        // and "Next" if navigating back.
        if (wizard.getModel().getNextButtonText().equals("Export"))
        {
            File selectedFile = getSelectedFile();

            // Add a file extension if necessary.
            if (!filter.accept(selectedFile))
            {
                selectedFile = new File(selectedFile.getPath() + '.' + formats.get(0).getIdentifier());
            }

            if (selectedFile.exists())
            {
                int result = AlwaysOnTopJOptionPane.showConfirmDialog(
                        wizard.getDialog(),
                        "A file already exists with name " + selectedFile.getName() + ". Do you want to replace the existing file?",
                        "Replace existing file?",
                        JOptionPane.YES_NO_OPTION);

                if (result == JOptionPane.YES_OPTION)
                {
                    wizard.getModel().setProperty(CommonWizardSettings.FILE, selectedFile.getPath());
                } else
                {
                    setNextPanel(IDENTIFIER);
                }
            } else
            {
                wizard.getModel().setProperty(CommonWizardSettings.FILE, selectedFile.getPath());
            }
        } else
        {
            wizard.setNextButtonEnabled(true);
        }

        chooser.removePropertyChangeListener(fileSelectionPCL);
        textField.removeKeyListener(textFieldKL);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        label1 = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default, $lgap, fill:default:grow, $lgap, default"));

        //---- label1 ----
        label1.setText("Select a destination file:");
        label1.setName("label1");
        add(label1, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel label1;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
