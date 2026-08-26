package gov.takx.core.view.gui.maps.exporters.dataset;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.core.view.gui.components.RangedSlider;
import jakarta.annotation.Nonnull;
import org.jdesktop.swingx.JXBusyLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ForkJoinPool;

/**
 * Panel that allows a user to select which levels of a layer they'd like to include in the cache.
 *
 * @since 1.43
 */
public class LayerSizeManagementPanel extends JPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final CellConstraints SLIDER_CELL_CONSTRAINTS = new CellConstraints(1, 1, 5, 1);

    private final RangedSlider qualitySlider;
    private final PropertyChangeListener propertyChangeListener = evt -> updateDisplay((MapDataWrapper) evt.getSource());
    private final ChangeListener changeListener = e -> sliderStateChangePerformed();
    private final Icon busyIcon;

    private MapDataWrapper mapDataWrapper;

    public LayerSizeManagementPanel()
    {
        initComponents();

        busyIcon = previewLabel.getIcon();
        qualitySlider = buildQualitySlider();

        sliderPanel.add(qualitySlider, SLIDER_CELL_CONSTRAINTS);
        sliderPanel.setVisible(false);
    }

    /**
     * @param mapDataWrapper The map data to display in this panel
     */
    public void configure(@Nonnull MapDataWrapper mapDataWrapper)
    {
        if (this.mapDataWrapper != mapDataWrapper)
        {
            if (this.mapDataWrapper != null) this.mapDataWrapper.removePropertyChangeListener(propertyChangeListener);

            this.mapDataWrapper = mapDataWrapper;
            this.mapDataWrapper.addPropertyChangeListener(propertyChangeListener);
        }

        updateDisplay(mapDataWrapper);
    }

    @InvokeOnEDT
    private void updateDisplay(MapDataWrapper mapDataWrapper)
    {
        mapDataWrapper.removePropertyChangeListener(propertyChangeListener);

        selectedCheckbox.setSelected(mapDataWrapper.isSelected());
        selectedCheckbox.setText(mapDataWrapper.getCacheName());

        long estimatedSize = null != mapDataWrapper.getTotalAcceptableTileCount()
                ? mapDataWrapper.getTotalAcceptableTileCount()
                : mapDataWrapper.getTotalTileCount() != null
                        ? mapDataWrapper.getTotalTileCount()
                : 0;
        layerSizeLabel.setText(MapBuilderUtils.getByteString(estimatedSize * mapDataWrapper.getTileDataSize()));

        if (mapDataWrapper.isSizeManaged())
        {
            // Remove change listener prior to any updates.
            qualitySlider.removeChangeListener(changeListener);

            sliderPanel.setVisible(true);
            qualitySlider.setMinimum(mapDataWrapper.getMinAvailableResolutionLevel());
            qualitySlider.setMaximum(mapDataWrapper.getMaxAvailableResolutionLevel());

            qualitySlider.setLowerValue(null != mapDataWrapper.getMinAcceptableLevel()
                    ? mapDataWrapper.getMinAcceptableLevel()
                    : mapDataWrapper.getMinAvailableResolutionLevel());
            qualitySlider.setUpperValue(null != mapDataWrapper.getMaxAcceptableLevel()
                    ? mapDataWrapper.getMaxAcceptableLevel()
                    : mapDataWrapper.getMaxAvailableResolutionLevel());

            // Add back change listener
            qualitySlider.addChangeListener(changeListener);
        } else
        {
            sliderPanel.setVisible(false);
        }

        // Ignore elevation wrappers that don't have previews
        if (this.mapDataWrapper.getElevationSource() != null)
        {
            previewPanel.setVisible(false);
        } else
        {
            updatePreview(mapDataWrapper);
        }

        mapDataWrapper.addPropertyChangeListener(propertyChangeListener);
    }

    /**
     * Update the preview to show a new position.
     */
    private void updatePreview(@Nonnull MapDataWrapper mapData)
    {
        switch (mapData.getPreviewState())
        {
            case Available ->
            {
                previewLabel.setBusy(false);

                final BufferedImage preview = mapData.getPreview();
                previewLabel.setText(preview != null ? null : "Not Available");
                previewLabel.setIcon(preview != null ? new ImageIcon(preview) : null);
            }
            case NotAvailable ->
            {
                previewLabel.setBusy(false);
                previewLabel.setIcon(null);

                previewLabel.setText("Not Available");
            }
            case Retrieving ->
            {
                previewLabel.setText(null);

                previewLabel.setIcon(busyIcon);
                previewLabel.setBusy(true);
            }
            case Error ->
            {
                previewLabel.setBusy(false);
                previewLabel.setIcon(null);

                previewLabel.setText("Error retrieving preview");
            }
            default -> logger.warn("Unrecognized preview state: {}", mapData.getPreviewState());
        }
    }

    private RangedSlider buildQualitySlider()
    {
        final RangedSlider qualitySlider = new RangedSlider();

        qualitySlider.setMajorTickSpacing(1);

        qualitySlider.setPaintTicks(true);
        qualitySlider.setPaintLabels(false);
        qualitySlider.setPaintTrack(true);

        return qualitySlider;
    }

    private void sliderStateChangePerformed()
    {
        if (!qualitySlider.getValueIsAdjusting())
        {
            mapDataWrapper.removePropertyChangeListener(propertyChangeListener);

            mapDataWrapper.setMinAcceptableLevel(qualitySlider.getLowerValue());
            mapDataWrapper.setMaxAcceptableLevel(qualitySlider.getUpperValue());

            mapDataWrapper.addPropertyChangeListener(propertyChangeListener);

            ForkJoinPool.commonPool().submit(new MapDataQueries.UpdateRangeEstimate(mapDataWrapper));
        }
    }

    private void selectedCheckboxActionPerformed()
    {
        if (null != mapDataWrapper)
        {
            mapDataWrapper.removePropertyChangeListener(propertyChangeListener);
            mapDataWrapper.setSelected(selectedCheckbox.isSelected());
            mapDataWrapper.addPropertyChangeListener(propertyChangeListener);
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        previewPanel = new JPanel();
        previewLabel = new JXBusyLabel();
        var panel1 = new JPanel();
        selectedCheckbox = new JCheckBox();
        sliderPanel = new JPanel();
        var lowQualityLabel = new JLabel();
        var highQualityLabel = new JLabel();
        layerSizeLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(Borders.DLU4_BORDER);
        setName("this");
        setLayout(new FormLayout(
                "80dlu, $lcgap, default:grow, $lcgap, 40dlu",
                "fill:default"));

        //======== previewPanel ========
        {
            previewPanel.setName("previewPanel");
            previewPanel.setLayout(new FormLayout(
                    "default:grow",
                    "fill:50dlu"));

            //---- previewLabel ----
            previewLabel.setHorizontalAlignment(SwingConstants.CENTER);
            previewLabel.setToolTipText("High quality preview");
            previewLabel.setFont(previewLabel.getFont().deriveFont(previewLabel.getFont().getStyle() | Font.BOLD));
            previewLabel.setName("previewLabel");
            previewPanel.add(previewLabel, cc.xy(1, 1));
        }
        add(previewPanel, cc.xy(1, 1));

        //======== panel1 ========
        {
            panel1.setName("panel1");
            panel1.setLayout(new FormLayout(
                    "default:grow",
                    "default, $lgap, default"));

            //---- selectedCheckbox ----
            selectedCheckbox.setText("name");
            selectedCheckbox.setFont(selectedCheckbox.getFont().deriveFont(selectedCheckbox.getFont().getStyle() | Font.BOLD, selectedCheckbox.getFont().getSize() + 4f));
            selectedCheckbox.setIconTextGap(8);
            selectedCheckbox.setName("selectedCheckbox");
            selectedCheckbox.addActionListener(e -> selectedCheckboxActionPerformed());
            panel1.add(selectedCheckbox, cc.xy(1, 1));

            //======== sliderPanel ========
            {
                sliderPanel.setName("sliderPanel");
                sliderPanel.setLayout(new FormLayout(
                        "default, $lcgap, center:default:grow, $lcgap, default",
                        "default, $lgap, default"));

                //---- lowQualityLabel ----
                lowQualityLabel.setText("Low Quality");
                lowQualityLabel.setName("lowQualityLabel");
                sliderPanel.add(lowQualityLabel, cc.xy(1, 3));

                //---- highQualityLabel ----
                highQualityLabel.setText("High Quality");
                highQualityLabel.setHorizontalAlignment(SwingConstants.RIGHT);
                highQualityLabel.setName("highQualityLabel");
                sliderPanel.add(highQualityLabel, cc.xy(5, 3));
            }
            panel1.add(sliderPanel, cc.xy(1, 3));
        }
        add(panel1, cc.xy(3, 1, CellConstraints.DEFAULT, CellConstraints.CENTER));

        //---- layerSizeLabel ----
        layerSizeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        layerSizeLabel.setFont(layerSizeLabel.getFont().deriveFont(layerSizeLabel.getFont().getSize() + 4f));
        layerSizeLabel.setText("0 kB");
        layerSizeLabel.setName("layerSizeLabel");
        add(layerSizeLabel, cc.xy(5, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel previewPanel;
    private JXBusyLabel previewLabel;
    private JCheckBox selectedCheckbox;
    private JPanel sliderPanel;
    private JLabel layerSizeLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
