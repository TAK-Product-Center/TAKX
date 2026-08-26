package gov.takx.core.view.gui.maptools.elevationtools.heatmap;

import com.atakmap.map.layer.elevation.ElevationHeatmapLayer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.layer.ITakLayerManager;
import gov.takx.core.globe.tak.WidgetLayerManager;
import gov.takx.core.globe.tak.layer.HeatMapLayer;
import gov.takx.core.globe.widgets.IsoKeyWidget;
import jakarta.inject.Inject;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

/**
 * An elevation tool panel specific to displaying and editing the {@link HeatMapLayer}.
 * @since 5.8
 */
@PseudoBean
public class HeatMapPanel extends JPanel
{
    @Inject
    private ITakLayerManager takLayerManager;

    @Inject
    private WidgetLayerManager widgetLayerManager;

    @Inject
    private IExtendedGlobeManager globeManager;

    // TODO: TAKX-4455, implement absolute and gradient features
    private ElevationHeatmapLayer heatMapLayer;

    public HeatMapPanel()
    {
        initComponents();
        String redrawReason = "heatmapUpdate";
        intensitySlider.addChangeListener(e -> {
            heatMapLayer.setAlpha((float) intensitySlider.getValue() / 100f);
            globeManager.requestRedraw(redrawReason, false);
        });
        saturationSlider.addChangeListener(e -> {
            heatMapLayer.setSaturation((float) saturationSlider.getValue() / 100f);
            globeManager.requestRedraw(redrawReason, false);
        });
        valueSlider.addChangeListener(e -> {
            heatMapLayer.setValue((float) valueSlider.getValue() / 100f);
            globeManager.requestRedraw(redrawReason, false);
        });
    }

    private void showHeatmapClicked()
    {
        if (showHeatMapCheckbox.isSelected())
        {
            heatMapLayer = heatMapLayer == null ? new ElevationHeatmapLayer("heatmap") : heatMapLayer;
            heatMapLayer.setVisible(true);
            widgetLayerManager.getWidgetsOfClass(IsoKeyWidget.class).forEach(e -> e.setVisible(true));
            heatMapLayer.setAlpha((float) intensitySlider.getValue() / 100f);
            heatMapLayer.setSaturation((float) saturationSlider.getValue() / 100f);
            heatMapLayer.setValue((float) valueSlider.getValue() / 100f);

            heatMapLayer.setDynamicRange();
            takLayerManager.addLayer(heatMapLayer);
            globeManager.getAllGlobeDelegates().forEach(globe -> globe.redraw(false));
        } else
        {
            if (heatMapLayer != null) takLayerManager.removeLayer(heatMapLayer);
            widgetLayerManager.getWidgetsOfClass(IsoKeyWidget.class).forEach(e -> e.setVisible(false));
            globeManager.getAllGlobeDelegates().forEach(globe -> globe.redraw(false));
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        showHeatMapCheckbox = new JCheckBox();
        var intensityLabel = new JLabel();
        intensitySlider = new JSlider();
        var saturationLabel = new JLabel();
        saturationSlider = new JSlider();
        var valueLabel = new JLabel();
        valueSlider = new JSlider();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "right:41dlu, $lcgap, center:134dlu",
                "3*(default, $lgap), default"));

        //---- showHeatMapCheckbox ----
        showHeatMapCheckbox.setText("Show HeatMap");
        showHeatMapCheckbox.setName("showHeatMapCheckbox");
        showHeatMapCheckbox.addActionListener(e -> showHeatmapClicked());
        add(showHeatMapCheckbox, cc.xy(3, 1));

        //---- intensityLabel ----
        intensityLabel.setText("Intensity");
        intensityLabel.setName("intensityLabel");
        add(intensityLabel, cc.xy(1, 3));

        //---- intensitySlider ----
        intensitySlider.setName("intensitySlider");
        add(intensitySlider, cc.xy(3, 3));

        //---- saturationLabel ----
        saturationLabel.setText("Saturation");
        saturationLabel.setName("saturationLabel");
        add(saturationLabel, cc.xy(1, 5));

        //---- saturationSlider ----
        saturationSlider.setName("saturationSlider");
        add(saturationSlider, cc.xy(3, 5));

        //---- valueLabel ----
        valueLabel.setText("Value");
        valueLabel.setName("valueLabel");
        add(valueLabel, cc.xy(1, 7));

        //---- valueSlider ----
        valueSlider.setName("valueSlider");
        add(valueSlider, cc.xy(3, 7));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JCheckBox showHeatMapCheckbox;
    private JSlider intensitySlider;
    private JSlider saturationSlider;
    private JSlider valueSlider;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
