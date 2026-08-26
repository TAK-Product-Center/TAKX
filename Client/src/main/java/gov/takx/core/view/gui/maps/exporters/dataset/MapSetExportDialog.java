package gov.takx.core.view.gui.maps.exporters.dataset;

import com.formdev.flatlaf.FlatClientProperties;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.tasks.ITaskManager;
import gov.takx.commons.swing.ComponentUtils;
import gov.takx.commons.wizard.IWizardModel;
import gov.takx.core.dataset.DataSetBuilderParameters;
import gov.takx.core.ioservice.ExportWizardTask;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.inject.Inject;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The MapBuilder dialog.
 *
 * @since 1.43
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MapSetExportDialog extends AStandardDialog implements IBoundaryDrawingListener
{
    private static final String HELP_HTML_TEXT = "<html>" +
            "To build a Map set, either select an existing shape from the 'Existing Shape' drop down or" +
            " select a shape type under 'Shape Creation', click <i>Create New Shape</i>, and then click" +
            " and drag on the map to select an area in which to download available maps." +
            " In order to use less space on disk," +
            " you can choose <i>Manage Map Size</i> at the bottom of this dialog." +
            " Choosing that option will provide sliders on each layer selected to allow" +
            " you to choose optimal resolution levels and reduce the size of your export" +
            " by not downloading every resolution in the map set." +
            "</html>";

    @Inject
    private ITaskManager taskManager;

    public MapSetExportDialog()
    {
        super("Map Set Builder");

        initComponents();

        helpButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_HELP);

        final var screenDimensions = ComponentUtils.getActiveMonitorScreenSize();
        setPreferredSize(new Dimension(600, (int) (screenDimensions.height * 0.66)));

        mapSetBuilderPanel.setBoundaryDrawingListener(this);
    }

    @Override
    public void onDrawingBoundary(boolean drawing)
    {
        setVisible(!drawing);
    }

    private void buildButtonActionPerformed()
    {
        final MapSetExportFileSelectionDialog dialog = new MapSetExportFileSelectionDialog(this, getSelectedSize());

        dialog.setModal(true);
        dialog.setVisible(true);

        if (dialog.getRetCode() == MapSetExportFileSelectionDialog.Action.SAVE)
        {
            final IWizardModel model = new MapSetExportWizardPropertiesBuilder()
                    .setName(dialog.getDestinationName())
                    .setDir(dialog.getDir())
                    .setSources(buildSources())
                    .setSector(mapSetBuilderPanel.getSector().orElse(null))
                    .setBoundary(mapSetBuilderPanel.getBoundary())
                    .setAutoConnect(mapSetBuilderPanel.isAutoConnect())
                    .createMapSetExportWizardModel();

            dispose();

            runOperation(model);
        }
    }

    private List<DataSetBuilderParameters> buildSources()
    {
        return mapSetBuilderPanel.getSelectedSources().stream()
                .map(this::buildDataSetBuilderParameters)
                .collect(Collectors.toList());
    }

    private DataSetBuilderParameters buildDataSetBuilderParameters(MapDataWrapper input)
    {
        return DataSetBuilderParameters.builder()
                .setHiResolution(mapSetBuilderPanel.isSizeManaged() ? input.getMaxAcceptableResolutionLevel() : null)
                .setLowResolution(mapSetBuilderPanel.isSizeManaged() ? input.getMinAcceptableResolutionLevel() : null)
                .setBoundary(mapSetBuilderPanel.getBoundary())
                .setDatasetDescriptors(input.getDatasetDescriptors())
                .setElevationSource(input.getElevationSource())
                .build();
    }

    private void runOperation(IWizardModel model)
    {
        taskManager.execute(new ExportWizardTask(getTitle(), new MapSetBuilderWizard(), model));
    }

    private long getSelectedSize()
    {
        return mapSetBuilderPanel.computeApproxSizeInBytes(mapSetBuilderPanel.getSelectedSizes());
    }

    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    @Override
    protected void onWindowClosing()
    {
        mapSetBuilderPanel.setBoundaryDrawingListener(null);
        mapSetBuilderPanel.cleanUp();
    }

    private void helpButtonActionPerformed()
    {
        helpPanel.setVisible(!helpPanel.isVisible());
        panel1.revalidate();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        panel1 = new JPanel();
        helpPanel = new JPanel();
        var helpTextLabel = new JLabel();
        var separator1 = new JSeparator();
        var description = new JLabel();
        helpButton = new JButton();
        mapSetBuilderPanel = new MapSetBuilderPanel();
        var buttonBar = new JPanel();
        var buildButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow"));

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow, $ugap, default",
                        "2*(default, $lgap), fill:default, $lgap, fill:default:grow"));

                //======== panel1 ========
                {
                    panel1.setName("panel1");
                    panel1.setLayout(new FormLayout(
                            "default:grow",
                            "2*(fill:default:grow)"));

                    //======== helpPanel ========
                    {
                        helpPanel.setVisible(false);
                        helpPanel.setName("helpPanel");
                        helpPanel.setLayout(new FormLayout(
                                "default:grow",
                                "default, $lgap, default"));

                        //---- helpTextLabel ----
                        helpTextLabel.setText("TEMP");
                        helpTextLabel.setName("helpTextLabel");
                        helpTextLabel.setText(HELP_HTML_TEXT);
                        helpPanel.add(helpTextLabel, cc.xy(1, 1));

                        //---- separator1 ----
                        separator1.setName("separator1");
                        helpPanel.add(separator1, cc.xy(1, 3));
                    }
                    panel1.add(helpPanel, cc.xy(1, 1));

                    //---- description ----
                    description.setText("Select an area on the map for download");
                    description.setName("description");
                    panel1.add(description, cc.xy(1, 2));
                }
                contentPanel.add(panel1, cc.xy(1, 1));

                //---- helpButton ----
                helpButton.setName("helpButton");
                helpButton.addActionListener(e -> helpButtonActionPerformed());
                contentPanel.add(helpButton, cc.xy(3, 1, CellConstraints.DEFAULT, CellConstraints.TOP));

                //---- mapSetBuilderPanel ----
                mapSetBuilderPanel.setName("mapSetBuilderPanel");
                contentPanel.add(mapSetBuilderPanel, cc.xywh(1, 7, 3, 1));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "pref"));

                //---- buildButton ----
                buildButton.setText("Build...");
                buildButton.setName("buildButton");
                buildButton.addActionListener(e -> buildButtonActionPerformed());
                buttonBar.add(buildButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(4, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, cc.xy(1, 1));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel panel1;
    private JPanel helpPanel;
    private JButton helpButton;
    private MapSetBuilderPanel mapSetBuilderPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
