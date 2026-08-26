package gov.takx.core.view.gui.preferences;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.preferences.IUserPreferenceManager;
import gov.takx.api.ui.AProgramPreferencesPanel;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.ioservice.ImportWizardManager;
import gov.takx.core.view.gui.maps.importers.FileImporterConfigurations;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@PseudoBean
public class FilesPreferencesPanel extends AProgramPreferencesPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private ImportWizardManager wizardManager;

    @Inject
    private IUserPreferenceManager userPreferenceManager;

    private FileImporterConfigurations importerConfigurations;
    private final List<String> typesToRemove = new ArrayList<>();
    private final DefaultTableModel model;

    public FilesPreferencesPanel()
    {
        initComponents();

        model = new DefaultTableModel(new Object[]{"File Ext", "Default Importer"}, 0)
        {
            @Override
            public boolean isCellEditable(int row, int col)
            {
                return false;
            }
        };

        configuredFilesTable.setModel(model);
        configuredFilesTable.setRowHeight(48);
        configuredFilesTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        configuredFilesTable.getColumnModel().getColumn(1).setCellRenderer(new ImporterCellRenderer());

        configuredFilesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
            {
                int selectedRow = configuredFilesTable.getSelectedRow();
                removeButton.setEnabled(selectedRow != -1);
            }
        });
    }

    @PostConstruct
    private void postConstruct()
    {
        importerConfigurations = UserPreferenceEntry.GLOBE_IMPORTERS_NODE.getNodeAs(FileImporterConfigurations.class, true);
        final List<String> childrenToRemove = new ArrayList<>();

        importerConfigurations.getImporterConfigurations().forEach((ext, config) -> {
            final String wizardName = wizardManager.getWizardName(config.getFamily(), config.getType());
            final Icon icon = getIcon(config.getIconUrl());

            if (wizardName == null || icon == null)
            {
                childrenToRemove.add(ext);
            } else
            {
                model.addRow(new Object[]{ext, new ImporterInfo(wizardName, icon)});
            }
        });

        // If there were any plugins that were determined to be missing that have preference entries, remove them
        // from the user preferences tree.
        childrenToRemove.forEach(importerConfigurations::removeChildNode);

        userPreferenceManager.savePreferences();
    }

    private void removeButtonActionPerformed()
    {
        int[] rows = configuredFilesTable.getSelectedRows();
        for (int i = rows.length - 1; i > -1; i--)
        {
            int idx = rows[i];
            typesToRemove.add(model.getValueAt(idx, 0).toString());
            model.removeRow(idx);
        }

        setPanelDirty();
    }

    @Override
    public boolean applyAction()
    {
        typesToRemove.forEach(importerConfigurations::removeChildNode);

        return userPreferenceManager.savePreferences();
    }

    @Override
    public void restoreDefaults()
    {
    }

    @Override
    public void releaseResources()
    {
    }

    private static Icon getIcon(String urlString)
    {
        ImageIcon icon = null;

        if (urlString != null)
        {
            try
            {
                URL url = new URL(urlString);
                BufferedImage bufferedImage = ImageIO.read(url);

                if (bufferedImage != null)
                {
                    icon = ImageUtils.getSquareIcon(bufferedImage, 40);
                }
            } catch (IOException ioe)
            {       // Not totally unheard of that a plugin was removed.  Not that necessary to show the whole
                // stack trace, but leaving it in there for debugging purposes.
                if (logger.isDebugEnabled())
                {
                    logger.error("Error reading file importer icon.", ioe);
                } else
                {
                    logger.error("Could not find icon at resource location: {}", urlString);
                }
            }
        }

        return icon;
    }

    private static class ImporterCellRenderer extends DefaultTableCellRenderer
    {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            ImporterInfo importerInfo = (ImporterInfo) value;
            setText(importerInfo.name);
            setIcon(importerInfo.icon);

            return this;
        }
    }

    private static class ImporterInfo
    {
        String name;
        Icon icon;

        public ImporterInfo(String name, Icon icon)
        {
            this.name = name;
            this.icon = icon;
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panel3 = new JPanel();
        var panel4 = new JPanel();
        var label1 = new JLabel();
        var scrollPane1 = new JScrollPane();
        configuredFilesTable = new JTable();
        removeButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default"));

        //======== panel3 ========
        {
            panel3.setName("panel3");
            panel3.setLayout(new FormLayout(
                    "default:grow, $lcgap, default",
                    "2*(default, $lgap), default"));

            //======== panel4 ========
            {
                panel4.setName("panel4");
                panel4.setLayout(new FormLayout(
                        "center:default:grow",
                        "default"));

                //---- label1 ----
                label1.setText("Default Importers");
                label1.putClientProperty("FlatLaf.styleClass", "large");
                label1.setName("label1");
                panel4.add(label1, cc.xy(1, 1));
            }
            panel3.add(panel4, cc.xywh(1, 1, 3, 1));

            //======== scrollPane1 ========
            {
                scrollPane1.setName("scrollPane1");

                //---- configuredFilesTable ----
                configuredFilesTable.setModel(new DefaultTableModel());
                configuredFilesTable.setAutoCreateRowSorter(true);
                configuredFilesTable.setFillsViewportHeight(true);
                configuredFilesTable.setPreferredScrollableViewportSize(new Dimension(100, 100));
                configuredFilesTable.setName("configuredFilesTable");
                scrollPane1.setViewportView(configuredFilesTable);
            }
            panel3.add(scrollPane1, cc.xywh(1, 3, 3, 1));

            //---- removeButton ----
            removeButton.setText("Remove");
            removeButton.setEnabled(false);
            removeButton.setName("removeButton");
            removeButton.addActionListener(e -> removeButtonActionPerformed());
            panel3.add(removeButton, cc.xy(3, 5));
        }
        add(panel3, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTable configuredFilesTable;
    private JButton removeButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
