package gov.takx.core.view.gui.maps.exporters.dataset;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.Color;
import java.io.File;

/**
 * Panel to allow the user to select a destination directory and map set name.
 *
 * @since 1.43
 */
public class DestinationPanel extends JPanel
{
    /**
     * Constructor.
     */
    public DestinationPanel()
    {
        initComponents();

        // Init the file chooser to the last selected path if one is available.
        JFileChooser fileChooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);

        if (fileChooser.getSelectedFile() != null)
        {
            textFieldDir.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }

        // Listen for changes so we can enable/disable the next button.
        DocumentListener listener = buildDocumentListener();
        textFieldDir.getDocument().addDocumentListener(listener);
        textFieldName.getDocument().addDocumentListener(listener);
    }

    public String getDir()
    {
        return textFieldDir.getText().trim();
    }

    public String getDestinationName()
    {
        return textFieldName.getText().trim();
    }

    private DocumentListener buildDocumentListener()
    {
        return new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                updateLabels();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                updateLabels();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                updateLabels();
            }
        };
    }

    public boolean isValidEntries()
    {
        if (getDestinationName().length() > 0 && getDir().length() > 0)
        {
            File parentDir = new File(getDir());

            if (parentDir.exists())
            {
                return Boolean.TRUE;
            }
        }

        return Boolean.FALSE;
    }

    /**
     * Update the "next" button, enabling it only if the user has chosen a valid directory and data set name.
     */
    private void updateLabels()
    {
        if (textFieldName.getDocument().getLength() > 0 && textFieldDir.getDocument().getLength() > 0)
        {
            File parentDir = new File(textFieldDir.getText());

            if (parentDir.exists())
            {
                String dataSetName = textFieldName.getText();
                labelFullPath.setForeground(label1.getForeground());
                File dataSetDir = new File(parentDir, dataSetName);
                labelFullPath.setText(dataSetDir.getAbsolutePath());

                if (dataSetDir.exists())
                {
                    labelExists.setText("<html>data will be added to the map set \"" + dataSetName + "\" that already exists at this location");
                } else
                {
                    labelExists.setText(null);
                }
            } else
            {
                labelFullPath.setForeground(Color.red);
                labelFullPath.setText("Invalid Directory: " + parentDir.getAbsolutePath());
                labelExists.setText(null);
            }
        } else
        {
            labelFullPath.setText(null);
            labelExists.setText(null);
        }
    }

    private void buttonDirSelectActionPerformed()
    {
        AlwaysOnTopFileChooser chooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.showDialog(this, "Select");

        File f = chooser.getSelectedFile();

        if (f != null)
        {
            textFieldDir.setText(f.getAbsolutePath());
        }
    }

    public JLabel getSizeLabel()
    {
        return sizeLabel;
    }

    public JTextField getTextFieldDir()
    {
        return textFieldDir;
    }

    public JTextField getTextFieldName()
    {
        return textFieldName;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        panelContent = new JPanel();
        label1 = new JLabel();
        textFieldDir = new JTextField();
        buttonDirSelect = new JButton();
        label2 = new JLabel();
        textFieldName = new JTextField();
        label3 = new JLabel();
        labelFullPath = new JLabel();
        label4 = new JLabel();
        sizeLabel = new JLabel();
        panel1 = new JPanel();
        labelExists = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default, 2*($lgap, default:grow)"));

        //======== panelContent ========
        {
            panelContent.setName("panelContent");
            panelContent.setLayout(new FormLayout(
                    "right:default, $lcgap, default:grow, $lcgap, default",
                    "4*(default, $lgap)"));

            //---- label1 ----
            label1.setText("Directory:");
            label1.setName("label1");
            panelContent.add(label1, cc.xy(1, 1));

            //---- textFieldDir ----
            textFieldDir.setColumns(40);
            textFieldDir.setName("textFieldDir");
            panelContent.add(textFieldDir, cc.xy(3, 1));

            //---- buttonDirSelect ----
            buttonDirSelect.setText("...");
            buttonDirSelect.setName("buttonDirSelect");
            buttonDirSelect.addActionListener(e -> buttonDirSelectActionPerformed());
            panelContent.add(buttonDirSelect, cc.xy(5, 1));

            //---- label2 ----
            label2.setText("Name:");
            label2.setName("label2");
            panelContent.add(label2, cc.xy(1, 3));

            //---- textFieldName ----
            textFieldName.setName("textFieldName");
            panelContent.add(textFieldName, cc.xywh(3, 3, 3, 1));

            //---- label3 ----
            label3.setText("Full Path:");
            label3.setName("label3");
            panelContent.add(label3, cc.xy(1, 5));

            //---- labelFullPath ----
            labelFullPath.setName("labelFullPath");
            panelContent.add(labelFullPath, cc.xywh(3, 5, 3, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

            //---- label4 ----
            label4.setText("Approx Size:");
            label4.setName("label4");
            panelContent.add(label4, cc.xy(1, 7));

            //---- sizeLabel ----
            sizeLabel.setText("text");
            sizeLabel.setName("sizeLabel");
            panelContent.add(sizeLabel, cc.xywh(3, 7, 3, 1));
        }
        add(panelContent, cc.xy(1, 3));

        //======== panel1 ========
        {
            panel1.setBorder(Borders.TABBED_DIALOG_BORDER);
            panel1.setName("panel1");
            panel1.setLayout(new FormLayout(
                    "center:default:grow",
                    "default"));

            //---- labelExists ----
            labelExists.setHorizontalAlignment(SwingConstants.CENTER);
            labelExists.setName("labelExists");
            panel1.add(labelExists, cc.xy(1, 1));
        }
        add(panel1, cc.xy(1, 5));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel panelContent;
    private JLabel label1;
    private JTextField textFieldDir;
    private JButton buttonDirSelect;
    private JLabel label2;
    private JTextField textFieldName;
    private JLabel label3;
    private JLabel labelFullPath;
    private JLabel label4;
    private JLabel sizeLabel;
    private JPanel panel1;
    private JLabel labelExists;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
