package gov.takx.core.view.gui.maps.terraindiscovery.view;

import com.formdev.flatlaf.FlatClientProperties;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.ITerrainDiscoveryController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class TerrainDiscoveryPanel extends JPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String HELP_TEXT = "<html>Visualizes the highest-resolution terrain on disk for selected Elevation Layer(s).<br><br>" +
            "To use, select a Rectangle geofence region, a visualization type, one or more elevation layers, and click Calculate.<br><br>" +
            "Terrain Discovery tool provides three different terrain data visualizations, selected by one of the following radio buttons:<ul>" +
            "<li><b>Coverage</b> displays local disk terrain-coverage, color-coded by elevation layer.<b>*</b></li>" +
            "<li><b>Resolution</b> displays the best local disk terrain resolutions, color-coded by resolution.</li>" +
            "<li><b>Terrain Image</b> visualizes each selected elevation layer's terrain data with a colorized " +
            "heat-map.<b>*</b> Any invalid data in an elevation layer is colored black.</li>" +
            "</ul><b>*</b> Regions that are missing elevation data in any of the available elevation layers are " +
            "colored transparent gray. You may receive warnings regarding this missing data. For more fidelity, " +
            "select the <b>Resolution</b> visualization button.</html>";

    private static final Color[] OVERLAY_COLORS = {
            new Color(116, 205, 108),
            new Color(55, 75, 255),
            new Color(184, 200, 0),
            new Color(209, 150, 0),
            new Color(149, 215, 255),
            new Color(183, 65, 0),
            new Color(19, 159, 110),
    };
    private final ITerrainDiscoveryController controller;
    private final GeofencePanel geofencePanel;
    private final Map<IRaptorLayer, TerrainListItemPanel> panelMap;
    private JDialog owner;
    private boolean showTileBoundaries;

    public TerrainDiscoveryPanel(ITerrainDiscoveryController controller)
    {
        this.controller = controller;
        owner = null;
        showTileBoundaries = false;
        initComponents();

        // TODO KMB: This should show a popup, not add a label to the panel. The UX isn't good.
        helpButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_HELP);

        geofencePanel = new GeofencePanel(controller);
        geofencePane.add(geofencePanel, new CellConstraints(1, 1));
        panelMap = new HashMap<>();
        progressBarPanel.setVisible(false);
        updateOpacity();
    }

    public void setElevationLayers(List<IRaptorLayer> elevationLayers)
    {
        removeElevationModelPanels();
        int count = 0;
        for (int i = 0; i < elevationLayers.size(); i++)
        {
            IRaptorLayer elevationLayer = elevationLayers.get(i);
            TerrainListItemPanel panel = new TerrainListItemPanel(controller, elevationLayer);
            panel.setTopEnabled(i != 0);
            panel.setAlternateBackgroundEnabled((count % 2) == 0);  // Use alternate background if even index
            addModelPanel(panel, OVERLAY_COLORS[count++ % OVERLAY_COLORS.length]);
        }
    }

    public void updateElevationModelOrder(List<IRaptorLayer> elevationLayers)
    {
        listPanel.removeAll();
        for (int i = 0; i < elevationLayers.size(); i++)
        {
            IRaptorLayer elevationLayer = elevationLayers.get(i);
            TerrainListItemPanel panel = panelMap.get(elevationLayer);
            panel.setTopEnabled(i != 0);
            panel.setAlternateBackgroundEnabled((i % 2) == 0);  // Use alternate background if even index
            listPanel.add(panel);
        }
        revalidate();
    }

    public void setElevationAtMouseCursor(IRaptorLayer elevationLayer, String elevation)
    {
        panelMap.get(elevationLayer).setElevation(elevation);
    }

    public void setDialog(JDialog owner)
    {
        this.owner = owner;
    }

    public GeofencePanel getGeofencePanel()
    {
        return geofencePanel;
    }

    public void disposeVisualizations()
    {
        setComponentsEnabled(true);
        controller.cancelCalculation();
        controller.clearVisualizations();
        progressBarPanel.setVisible(false);
    }

    public void setElevationLayerCoverage(IRaptorLayer elevationLayer, String coverage)
    {
        panelMap.get(elevationLayer).setCoverageText(coverage);
    }

    public void setElevationLayerResolution(IRaptorLayer elevationLayer, String resolution)
    {
        panelMap.get(elevationLayer).setResolutionText(resolution);
    }

    public void setElevationLayerVisible(IRaptorLayer elevationLayer, boolean visible)
    {
        TerrainListItemPanel panel = panelMap.get(elevationLayer);
        if (panel != null)
        {
            panel.setEnableCheckBoxSelected(visible);
        }
    }

    public void setProgressStarted()
    {
        setComponentsEnabled(false);
        progressBar.setStringPainted(false);
    }

    public void setProgressUpdated(double progress)
    {
        progressBar.setValue((int) progress);
    }

    public void setProgressText(String text)
    {
        if (text != null && !text.isEmpty())
        {
            progressBar.setStringPainted(true);
            progressBar.setString(text);
        } else
        {
            progressBar.setStringPainted(false);
        }
    }

    public void setProgressStopped()
    {
        setComponentsEnabled(true);
        progressBar.setValue(0);
        progressBar.setStringPainted(false);
    }

    public void setCalculationPreparationEnabled(boolean enable)
    {
        progressBar.setIndeterminate(enable);
        progressBar.setStringPainted(enable);
        progressBar.setString(enable ? "Preparing for calculation..." : "");
    }

    public void reattachShapeListener()
    {
        geofencePanel.reattachShapeListener();
    }

    public void clearTemp()
    {
        for (TerrainListItemPanel panel : panelMap.values())
        {
            panel.resetView();
        }
    }

    public void clearAll()
    {
        clearTemp();
        removeElevationModelPanels();
        getGeofencePanel().detachShapeListener();
    }

    public void refreshToModel()
    {
        geofencePanel.refreshToModel();
    }

    private void addModelPanel(TerrainListItemPanel panel, Color surfaceColor)
    {
        panel.setSurfaceColor(surfaceColor);
        listPanel.add(panel);
        panelMap.put(panel.getElevationLayer(), panel);
        controller.setElevationLayerColor(panel.getElevationLayer(), surfaceColor);
    }

    private void removeElevationModelPanels()
    {
        for (TerrainListItemPanel panel : panelMap.values())
        {
            panel.dispose();
        }
        listPanel.removeAll();
        panelMap.clear();
        revalidate();
    }

    private void calculateButtonActionPerformed()
    {
        controller.calculate();
    }

    private void clearButtonActionPerformed()
    {
        clearTemp();
        disposeVisualizations();
        controller.cancelCalculation();
        controller.removeAnalyticsLayers();
    }

    private void closeButtonActionPerformed()
    {
        geofencePanel.detachShapeListener();
        clearTemp();
        if (owner != null)
        {
            owner.setVisible(false);
        }
    }

    private void setComponentsEnabled(boolean enabled)
    {
        setEnabled(listPanel, enabled);
        setEnabled(geofencePanel, enabled);
        calculateButton.setEnabled(enabled);
        progressBarPanel.setVisible(!enabled);
    }

    private void setEnabled(Component c, boolean enabled)
    {
        if (c instanceof Container)
        {
            for (Component d : ((Container) c).getComponents())
            {
                setEnabled(d, enabled);
            }
        }
        c.setEnabled(enabled);
    }

    private void opacitySliderStateChanged()
    {
        updateOpacity();
    }

    private void updateOpacity()
    {
        double opacity = ((double) opacitySlider.getValue()) / opacitySlider.getMaximum();
        opacityValueLabel.setText((int) (opacity * 100d) + " %");
        controller.setOpacity(opacity);
    }

    private void helpButtonActionPerformed()
    {
        AlwaysOnTopJOptionPane.showMessageDialog(HELP_TEXT, "Terrain Discovery Help", JOptionPane.INFORMATION_MESSAGE);
    }

    // TODO KMB: Is this still useful? If so, let's find somewhere to put it. I certainly didn't know about it. These
    //  kinds of dev features get lost over time.
    private void iconLabelMouseClicked(MouseEvent e)
    {
        // Debug visualization for developers; likely not useful for users, so it's a hidden feature.
        if (e.getButton() == MouseEvent.BUTTON3 && e.getClickCount() >= 5)
        {
            showTileBoundaries = !showTileBoundaries;
            controller.setTileBoundsVisible(showTileBoundaries);
            logger.warn("Showing tile boundaries: {}", showTileBoundaries);
        }
    }

    private void coverageRadioButtonActionPerformed()
    {
        controller.setUseCoverageVisualization();
    }

    private void resolutionRadioButtonActionPerformed()
    {
        controller.setUseResolutionVisualization();
    }

    private void terrainImageRadioButtonActionPerformed()
    {
        controller.setUseTerrainImageVisualization();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        helpButton = new JButton();
        geofencePane = new JPanel();
        var visualizationLabel = new JLabel();
        var radioButtonPanel = new JPanel();
        var coverageRadioButton = new JRadioButton();
        var resolutionRadioButton = new JRadioButton();
        var terrainImageRadioButton = new JRadioButton();
        var opacityLabel = new JLabel();
        opacitySlider = new JSlider();
        opacityValueLabel = new JLabel();
        var scrollPane1 = new JScrollPane();
        listPanel = new JPanel();
        progressBarPanel = new JPanel();
        var label1 = new JLabel();
        progressBar = new JProgressBar();
        var buttonPanel = new JPanel();
        calculateButton = new JButton();
        var clearButton = new JButton();
        var closeButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default:grow, $lcgap, default",
                "default, $rgap, 3*(default, $lgap), fill:default:grow, 2*(default)"));

        //---- helpButton ----
        helpButton.setFocusable(false);
        helpButton.setToolTipText("Display help");
        helpButton.setName("helpButton");
        helpButton.addActionListener(e -> helpButtonActionPerformed());
        add(helpButton, cc.xy(5, 1));

        //======== geofencePane ========
        {
            geofencePane.setName("geofencePane");
            geofencePane.setLayout(new FormLayout(
                    "default:grow",
                    "default:grow"));
        }
        add(geofencePane, cc.xywh(1, 3, 5, 1));

        //---- visualizationLabel ----
        visualizationLabel.setText("Visualization: ");
        visualizationLabel.setFocusable(false);
        visualizationLabel.setName("visualizationLabel");
        add(visualizationLabel, cc.xy(1, 5));

        //======== radioButtonPanel ========
        {
            radioButtonPanel.setName("radioButtonPanel");
            radioButtonPanel.setLayout(new FormLayout(
                    "2*(default, $lcgap), default",
                    "default"));

            //---- coverageRadioButton ----
            coverageRadioButton.setText("Coverage");
            coverageRadioButton.setSelected(true);
            coverageRadioButton.setToolTipText("Display coverage of each model's highest-resolution data");
            coverageRadioButton.setName("coverageRadioButton");
            coverageRadioButton.addActionListener(e -> coverageRadioButtonActionPerformed());
            radioButtonPanel.add(coverageRadioButton, cc.xy(1, 1));

            //---- resolutionRadioButton ----
            resolutionRadioButton.setText("Resolution");
            resolutionRadioButton.setToolTipText("Display actual resolution (in meters) of terrain data");
            resolutionRadioButton.setName("resolutionRadioButton");
            resolutionRadioButton.addActionListener(e -> resolutionRadioButtonActionPerformed());
            radioButtonPanel.add(resolutionRadioButton, cc.xy(3, 1));

            //---- terrainImageRadioButton ----
            terrainImageRadioButton.setText("Terrain Image");
            terrainImageRadioButton.setToolTipText("Display image representations of terrain data");
            terrainImageRadioButton.setName("terrainImageRadioButton");
            terrainImageRadioButton.addActionListener(e -> terrainImageRadioButtonActionPerformed());
            radioButtonPanel.add(terrainImageRadioButton, cc.xy(5, 1));
        }
        add(radioButtonPanel, cc.xywh(3, 5, 3, 1));

        //---- opacityLabel ----
        opacityLabel.setText("Opacity: ");
        opacityLabel.setName("opacityLabel");
        add(opacityLabel, cc.xy(1, 7));

        //---- opacitySlider ----
        opacitySlider.setValue(60);
        opacitySlider.setName("opacitySlider");
        opacitySlider.addChangeListener(e -> opacitySliderStateChanged());
        add(opacitySlider, cc.xy(3, 7));

        //---- opacityValueLabel ----
        opacityValueLabel.setText("N/A");
        opacityValueLabel.setName("opacityValueLabel");
        add(opacityValueLabel, cc.xy(5, 7));

        //======== scrollPane1 ========
        {
            scrollPane1.setName("scrollPane1");

            //======== listPanel ========
            {
                listPanel.setName("listPanel");
                listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
            }
            scrollPane1.setViewportView(listPanel);
        }
        add(scrollPane1, cc.xywh(1, 9, 5, 1));

        //======== progressBarPanel ========
        {
            progressBarPanel.setName("progressBarPanel");
            progressBarPanel.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "$rgap, fill:default:grow"));

            //---- label1 ----
            label1.setText("Calculation Progress: ");
            label1.setName("label1");
            progressBarPanel.add(label1, cc.xy(1, 2));

            //---- progressBar ----
            progressBar.setName("progressBar");
            progressBarPanel.add(progressBar, cc.xy(3, 2));
        }
        add(progressBarPanel, cc.xywh(1, 10, 5, 1));

        //======== buttonPanel ========
        {
            buttonPanel.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
            buttonPanel.setName("buttonPanel");
            buttonPanel.setLayout(new FormLayout(
                    "$glue, 2*($button, $lcgap), $button",
                    "default"));

            //---- calculateButton ----
            calculateButton.setText("Calculate");
            calculateButton.setToolTipText("Run a terrain discovery calculation");
            calculateButton.setName("calculateButton");
            calculateButton.addActionListener(e -> calculateButtonActionPerformed());
            buttonPanel.add(calculateButton, cc.xy(2, 1));

            //---- clearButton ----
            clearButton.setText("Clear");
            clearButton.setToolTipText("Clear calculation results");
            clearButton.setName("clearButton");
            clearButton.addActionListener(e -> clearButtonActionPerformed());
            buttonPanel.add(clearButton, cc.xy(4, 1));

            //---- closeButton ----
            closeButton.setText("Close");
            closeButton.setToolTipText("Close terrain discovery");
            closeButton.setName("closeButton");
            closeButton.addActionListener(e -> closeButtonActionPerformed());
            buttonPanel.add(closeButton, cc.xy(6, 1));
        }
        add(buttonPanel, cc.xywh(1, 11, 5, 1));

        //---- buttonGroup1 ----
        var buttonGroup1 = new ButtonGroup();
        buttonGroup1.add(coverageRadioButton);
        buttonGroup1.add(resolutionRadioButton);
        buttonGroup1.add(terrainImageRadioButton);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JButton helpButton;
    private JPanel geofencePane;
    private JSlider opacitySlider;
    private JLabel opacityValueLabel;
    private JPanel listPanel;
    private JPanel progressBarPanel;
    private JProgressBar progressBar;
    private JButton calculateButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
