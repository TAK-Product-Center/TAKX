package gov.takx.core.view.gui.layers.viewmanager.properties;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.nasa.worldwind.layers.Layer;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.ui.AProgramPreferencesPanel;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.ErrorReportingSwingWorker;
import gov.takx.commons.swing.StatusDialog;
import gov.takx.core.globe.layers.IGlobeLayer;
import gov.takx.core.globe.layers.SurfaceImageLayer;
import gov.takx.core.view.gui.GUIUtils;
import gov.takx.core.view.gui.layers.GeoRectifyDialog;
import gov.takx.platform.utils.units.AltitudeUnits;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.NumberFormatter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Set;

import static gov.takx.platform.preferences.UserPreferenceEntry.DISPLAY_RANGE_UNITS;

/**
 * Panel allowing the user to view and edit the properties of a layer.
 */
@PseudoBean
public class LayerPropertiesPanel extends AProgramPreferencesPanel implements GeoRectifyDialog.IGeoRectifyListener
{
    // Collection of layer groups for which we show the opacity controls
    // TODO: This should probably be controlled by a flag on the layer definition and not by inference
    private static final Set<LayerGroup> GROUPS_WITH_OPACITY = Set.of(
            LayerGroup.SURFACE_STRUCTURES,
            LayerGroup.IMAGERY,
            LayerGroup.ANALYTIC_SURFACES,
            LayerGroup.SURFACE_IMAGERY,
            LayerGroup.PLACEMARK_OVERLAY);

    @Inject
    private IExtendedGlobeManager globeManager;

    private final IPreferenceMonitor<AltitudeUnits> altitudeUnitsMonitor =
            DISPLAY_RANGE_UNITS.newMonitor(AltitudeUnits.class, uf -> updateTextFieldUnits());

    protected KMFormattedTextField minVisibleAltitudeComponent;
    protected KMFormattedTextField maxVisibleAltitudeComponent;
    protected IRaptorLayer currentLayer;

    public LayerPropertiesPanel()
    {
        initComponents();

        GUIUtils.addSingleActionDocumentListener(nameTextField, e -> setPanelDirty());

        minVisibleAltitudeComponent = (KMFormattedTextField) minVisibleAltitudeTextField;
        maxVisibleAltitudeComponent = (KMFormattedTextField) maxVisibleAltitudeTextField;

        updateTextFieldUnits();
    }

    public void setRaptorLayer(@Nonnull IRaptorLayer layer)
    {
        currentLayer = layer;
        updateComponents();
    }

    @Override
    public boolean applyAction()
    {
        currentLayer.setDisplayName(nameTextField.getText());

        currentLayer.setMinActiveAltitude(minVisibleAltitudeComponent.getValueInMeters());

        final double newMax = maxVisibleAltitudeComponent.getValueInMeters();
        if (shouldApplyMaxAltitude(newMax))
        {
            currentLayer.setMaxActiveAltitude(newMax); // Set the value on the layer
        } else
        {
            maxVisibleAltitudeComponent.setValueInMeters(currentLayer.getMaxActiveAltitude()); // Reset to the original value
        }

        currentLayer.setOpacity(opacitySlider.getValue() / 100d);

        final LayerGroup group = currentLayer.getLayerGroup();
        if (group == LayerGroup.ELEVATION | group == LayerGroup.IMAGERY)
        {
            currentLayer.setSplitScale((Double) detailHintSpinner.getValue());
        }

        return true;
    }

    public void updateComponents()
    {
        LayerGroup group = currentLayer.getLayerGroup();

        boolean hasOpacityControls = GROUPS_WITH_OPACITY.contains(group);
        boolean isElevationLayer = group == LayerGroup.ELEVATION;
        boolean isLayerWithDetailHint = group == LayerGroup.ELEVATION | group == LayerGroup.IMAGERY;

        detailHintSlider.setVisible(isLayerWithDetailHint);
        detailHintSpinner.setVisible(isLayerWithDetailHint);
        detailHintLabel.setVisible(isLayerWithDetailHint);

        opacityLabel.setEnabled(hasOpacityControls);
        opacitySlider.setEnabled(hasOpacityControls);
        opacitySpinner.setEnabled(hasOpacityControls);

        minVisibleAltitudeComponent.setEnabled(!isElevationLayer);
        maxVisibleAltitudeComponent.setEnabled(!isElevationLayer);
        minVisibleAltitudeComponent.setValueInMeters(currentLayer.getMinActiveAltitude());
        maxVisibleAltitudeComponent.setValueInMeters(currentLayer.getMaxActiveAltitude());

        nameTextField.setText(currentLayer.getDisplayName());
        opacitySlider.setValue((int) (100d * currentLayer.getOpacity()));
        detailHintSpinner.setValue(currentLayer.getSplitScale());

        surfaceImagePropertiesPane.setVisible(isLayerSurfaceImage());

        final boolean editable = currentLayer.isEditable();
        editableCheckBox.setSelected(editable);
        geoRectifyButton.setEnabled(editable);
    }

    @Override
    public void restoreDefaults()
    {
        nameTextField.setText("");
        opacitySlider.setValue(100);
        detailHintSlider.setValue(0);
        minVisibleAltitudeComponent.setText("");
        maxVisibleAltitudeComponent.setText("");
    }

    @Override
    public void releaseResources()
    {
        altitudeUnitsMonitor.dispose();
    }

    @Override
    public void setSurfaceImageEditable(boolean editable)
    {
        editableCheckBox.setSelected(editable);
    }

    private double altitudeUnitsToMeters(double altitude)
    {
        return altitude / altitudeUnitsMonitor.getCurrentValue().getMultiplier();
    }

    private double metersToAltitudeUnits(double meters)
    {
        return meters * altitudeUnitsMonitor.getCurrentValue().getMultiplier();
    }

    protected Layer getWWLayerForSelectedLayer()
    {
        final IRaptorLayer layer = currentLayer;
        final IGlobeLayer globeLayer = globeManager.getPrimaryGlobeDelegate().getGlobeLayerManager().getGlobeLayer(layer);

        return globeLayer != null ? globeLayer.getLayer() : null;
    }

    protected boolean shouldApplyMaxAltitude(double max)
    {
        // If this new maximum is above the effective max for the layer, then warn the user
        final Layer wwLayer = getWWLayerForSelectedLayer();
        final Double effectiveMax = wwLayer.getMaxEffectiveAltitude(null);

        if (effectiveMax != null && max > effectiveMax)
        {
            final double effectiveMaxCurrentUnits = metersToAltitudeUnits(effectiveMax);
            final double maxCurrentUnits = metersToAltitudeUnits(max);
            final String unitsSymbol = altitudeUnitsMonitor.getCurrentValue().getAbbreviation();

            return JOptionPane.YES_OPTION == AlwaysOnTopJOptionPane.showConfirmDialog(
                    null,
                    String.format("""
                            The elevation you entered (%,.1f %s) is higher than the point
                            at which this layer has meaningful data (%,.1f %s).

                            Are you sure you want to use the new value?""", maxCurrentUnits,
                            unitsSymbol, effectiveMaxCurrentUnits, unitsSymbol),
                    "Confirm Altitude Value",
                    JOptionPane.YES_NO_OPTION);
        }

        return true;
    }

    protected void updateTextFieldUnits()
    {
        visibleAltitudeUnits.setText("(" + altitudeUnitsMonitor.getCurrentValue().getAbbreviation() + ")");
        minVisibleAltitudeComponent.refresh();
        maxVisibleAltitudeComponent.refresh();
    }

    protected boolean isLayerSurfaceImage()
    {
        return currentLayer.getLayerGroup() == LayerGroup.SURFACE_IMAGERY;
    }

    private void opacitySliderStateChanged()
    {
        opacitySpinner.setValue(opacitySlider.getValue());
        setPanelDirty();
    }

    private void opacitySpinnerStateChanged()
    {
        opacitySlider.setValue((Integer) opacitySpinner.getValue());
        setPanelDirty();
    }

    private void detailHintSliderStateChanged()
    {
        detailHintSpinner.setValue(detailHintSlider.getValue() / 100d);
        setPanelDirty();
    }

    private void detailHintSpinnerStateChanged()
    {
        detailHintSlider.setValue((int) ((Double) detailHintSpinner.getValue() * 100d));
        setPanelDirty();
    }

    private void editableCheckBoxItemStateChanged()
    {
        if (isLayerSurfaceImage() && currentLayer.isVisible())
        {
            boolean editable = editableCheckBox.isSelected();
            currentLayer.setEditable(editable);
            geoRectifyButton.setEnabled(editable);
            setPanelDirty();
        }
    }

    private void geoRectifyButtonActionPerformed()
    {
        GeoRectifyDialog.showGeoRectifyDialog(currentLayer, this);
    }

    private void exportButtonActionPerformed()
    {
        final SurfaceImageLayer layer = (SurfaceImageLayer) globeManager.getPrimaryGlobeDelegate()
                .getGlobeLayerManager().getGlobeLayer(currentLayer);

        AlwaysOnTopFileChooser chooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setDialogTitle("Export georectified image");
        chooser.setApproveButtonText("Export");
        chooser.setApproveButtonToolTipText("Export the image to this file");

        // Initialize the file chooser with the file name, in the default directory
        File directory = chooser.getCurrentDirectory();
        File imgFile = new File(layer.getImageFilePath());
        String ext = FileUtils.getExtension(imgFile);

        chooser.resetChoosableFileFilters();
        chooser.setSelectedFile(new File(directory, imgFile.getName()));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Images", ImageIO.getReaderFormatNames()));

        File imageFile = null;
        File geoFile = null;
        boolean readyToExport = false;

        while (!readyToExport)
        {
            if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;

            imageFile = chooser.getSelectedFile();
            // Make sure that the file has the appropriate extension.
            if (ext != null && !ext.equals(FileUtils.getExtension(imageFile)))
            {
                imageFile = new File(FileUtils.getNameWithoutExtension(imageFile.getPath()) + "." + ext);
            }

            geoFile = SurfaceImageLayer.getGeoInformationFileName(imageFile);

            if (imageFile.exists() || geoFile.exists())
            {
                String message = "Files " + imageFile.getName() + " and/or " + geoFile.getName() +
                        " already exist. Do you want to replace these files?";
                String title = "Replace existing files?";

                int answer = AlwaysOnTopJOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION);
                readyToExport = (answer == JOptionPane.YES_OPTION);
            } else
            {
                readyToExport = true;
            }
        }

        final StatusDialog statusDialog = new StatusDialog(false);
        statusDialog.setStatusText("Exporting...");
        statusDialog.setVisible(true);

        new ExportWorker(layer, imageFile, statusDialog, geoFile).execute();
    }

    /**
     * Worker to process the export of a surface image in the background.
     * @since 3.4
     */
    private static class ExportWorker extends ErrorReportingSwingWorker<Void, Object>
    {
        private final SurfaceImageLayer layer;
        private final File finalImageFile;
        private final StatusDialog statusDialog;
        private final File finalGeoFile;

        public ExportWorker(SurfaceImageLayer layer, File finalImageFile, StatusDialog statusDialog, File finalGeoFile)
        {
            super("Export failed", "Error while exporting image.");

            this.layer = layer;
            this.finalImageFile = finalImageFile;
            this.statusDialog = statusDialog;
            this.finalGeoFile = finalGeoFile;
        }

        @Override
        protected Void doInBackground() throws IOException
        {
            layer.export(finalImageFile, true, statusDialog);
            return null;
        }

        @Override
        protected void cleanup()
        {
            statusDialog.dispose();
        }

        @Override
        protected void onSuccess(Void result)
        {
            new ExportSurfaceImageDialog(finalImageFile.getAbsolutePath(), finalGeoFile.getAbsolutePath())
                    .setVisible(true);
        }
    }

    public class KMFormattedTextField extends JFormattedTextField implements PropertyChangeListener
    {
        /**
         * @return Configured formatter to use for instances of this text field
         * @since 3.4
         */
        private static NumberFormatter makeFormatter()
        {
            final NumberFormatter formatter = new NumberFormatter();
            formatter.setValueClass(Double.class);
            formatter.setAllowsInvalid(true);
            formatter.setCommitsOnValidEdit(true);

            return formatter;
        }

        private Double valueInMeters = 0.0;

        public KMFormattedTextField()
        {
            super(makeFormatter());

            addPropertyChangeListener("value", this);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            setPanelDirty();
            valueInMeters = altitudeUnitsToMeters(((Number) evt.getNewValue()).doubleValue());
        }

        private void refresh()
        {
            setValueInMeters(valueInMeters);
        }

        private Double getValueInMeters()
        {
            return valueInMeters;
        }

        @Override
        public void setValue(Object value)
        {
            super.setValue(value);
            if (value != null) valueInMeters = altitudeUnitsToMeters(((Number) value).doubleValue());
        }

        private void setValueInMeters(Double meters)
        {
            setValue(metersToAltitudeUnits(meters));
            valueInMeters = meters;
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panel5 = new JPanel();
        var panel7 = new JPanel();
        nameLabel_ = new JLabel();
        nameTextField = new JTextField();
        opacityLabel = new JLabel();
        opacitySpinner = new JSpinner();
        opacitySlider = new JSlider();
        detailHintLabel = new JLabel();
        detailHintSpinner = new JSpinner();
        detailHintSlider = new JSlider();
        panel2 = new JPanel();
        var visibleAltitudeLabel = new JLabel();
        visibleAltitudeUnits = new JLabel();
        panel1 = new JPanel();
        var minVisibleAltitudeLabel = new JLabel();
        var maxVisibleAltitudeLabel = new JLabel();
        minVisibleAltitudeTextField = new KMFormattedTextField();
        maxVisibleAltitudeTextField = new KMFormattedTextField();
        surfaceImagePropertiesPane = new JPanel();
        separator1 = new JSeparator();
        panel4 = new JPanel();
        surfaceImageLabel = new JLabel();
        editableCheckBox = new JCheckBox();
        geoRectifyButton = new JButton();
        exportButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default"));

        //======== panel5 ========
        {
            panel5.setName("panel5");
            panel5.setLayout(new FormLayout(
                    "default:grow",
                    "2*(default)"));

            //======== panel7 ========
            {
                panel7.setName("panel7");
                panel7.setLayout(new FormLayout(
                        "2*(default, $lcgap), default:grow",
                        "3*(default, $lgap), default"));

                //---- nameLabel_ ----
                nameLabel_.setText("Name:");
                nameLabel_.setName("nameLabel_");
                panel7.add(nameLabel_, cc.xy(1, 1));

                //---- nameTextField ----
                nameTextField.setName("nameTextField");
                panel7.add(nameTextField, cc.xywh(3, 1, 3, 1));

                //---- opacityLabel ----
                opacityLabel.setText("Opacity:");
                opacityLabel.setName("opacityLabel");
                panel7.add(opacityLabel, cc.xy(1, 3));

                //---- opacitySpinner ----
                opacitySpinner.setModel(new SpinnerNumberModel(100, 0, 100, 1));
                opacitySpinner.setToolTipText("Sets the opacity of the layer.  100% opacity means the layer is fully solid. 0% opacity means the layer is fully transparent.");
                opacitySpinner.setName("opacitySpinner");
                opacitySpinner.addChangeListener(e -> opacitySpinnerStateChanged());
                panel7.add(opacitySpinner, cc.xy(3, 3));

                //---- opacitySlider ----
                opacitySlider.setValue(100);
                opacitySlider.setToolTipText("Sets the opacity of the layer.  100% opacity means the layer is fully solid. 0% opacity means the layer is fully transparent.");
                opacitySlider.setName("opacitySlider");
                opacitySlider.addChangeListener(e -> opacitySliderStateChanged());
                panel7.add(opacitySlider, cc.xy(5, 3));

                //---- detailHintLabel ----
                detailHintLabel.setText("Detail Hint:  ");
                detailHintLabel.setName("detailHintLabel");
                panel7.add(detailHintLabel, cc.xy(1, 5));

                //---- detailHintSpinner ----
                detailHintSpinner.setModel(new SpinnerNumberModel(0.0, -1.0, 1.0, 0.1));
                detailHintSpinner.setName("detailHintSpinner");
                detailHintSpinner.addChangeListener(e -> detailHintSpinnerStateChanged());
                panel7.add(detailHintSpinner, cc.xy(3, 5));

                //---- detailHintSlider ----
                detailHintSlider.setMinimum(-100);
                detailHintSlider.setValue(0);
                detailHintSlider.setName("detailHintSlider");
                detailHintSlider.addChangeListener(e -> detailHintSliderStateChanged());
                panel7.add(detailHintSlider, cc.xy(5, 5));

                //======== panel2 ========
                {
                    panel2.setName("panel2");
                    panel2.setLayout(new FormLayout(
                            "default:grow",
                            "2*(default)"));

                    //---- visibleAltitudeLabel ----
                    visibleAltitudeLabel.setText("Visible alt.");
                    visibleAltitudeLabel.setName("visibleAltitudeLabel");
                    panel2.add(visibleAltitudeLabel, cc.xy(1, 1));

                    //---- visibleAltitudeUnits ----
                    visibleAltitudeUnits.setText("(km):");
                    visibleAltitudeUnits.setName("visibleAltitudeUnits");
                    panel2.add(visibleAltitudeUnits, cc.xy(1, 2));
                }
                panel7.add(panel2, cc.xy(1, 7));

                //======== panel1 ========
                {
                    panel1.setName("panel1");
                    panel1.setLayout(new FormLayout(
                            "default:grow, $lcgap, default:grow",
                            "default, fill:default:grow"));

                    //---- minVisibleAltitudeLabel ----
                    minVisibleAltitudeLabel.setText("min");
                    minVisibleAltitudeLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    minVisibleAltitudeLabel.setName("minVisibleAltitudeLabel");
                    panel1.add(minVisibleAltitudeLabel, cc.xy(1, 1));

                    //---- maxVisibleAltitudeLabel ----
                    maxVisibleAltitudeLabel.setText("max");
                    maxVisibleAltitudeLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    maxVisibleAltitudeLabel.setName("maxVisibleAltitudeLabel");
                    panel1.add(maxVisibleAltitudeLabel, cc.xy(3, 1));

                    //---- minVisibleAltitudeTextField ----
                    minVisibleAltitudeTextField.setColumns(8);
                    minVisibleAltitudeTextField.setName("minVisibleAltitudeTextField");
                    panel1.add(minVisibleAltitudeTextField, cc.xy(1, 2));

                    //---- maxVisibleAltitudeTextField ----
                    maxVisibleAltitudeTextField.setColumns(8);
                    maxVisibleAltitudeTextField.setName("maxVisibleAltitudeTextField");
                    panel1.add(maxVisibleAltitudeTextField, cc.xy(3, 2));
                }
                panel7.add(panel1, cc.xywh(3, 7, 3, 1));
            }
            panel5.add(panel7, cc.xy(1, 1));

            //======== surfaceImagePropertiesPane ========
            {
                surfaceImagePropertiesPane.setName("surfaceImagePropertiesPane");
                surfaceImagePropertiesPane.setLayout(new FormLayout(
                        "default:grow",
                        "default, $lgap, default"));

                //---- separator1 ----
                separator1.setName("separator1");
                surfaceImagePropertiesPane.add(separator1, cc.xy(1, 1));

                //======== panel4 ========
                {
                    panel4.setName("panel4");
                    panel4.setLayout(new FormLayout(
                            "default, $lcgap, default",
                            "default, $lgap, default"));

                    //---- surfaceImageLabel ----
                    surfaceImageLabel.setText("Surface Image:");
                    surfaceImageLabel.setName("surfaceImageLabel");
                    panel4.add(surfaceImageLabel, cc.xy(1, 1));

                    //---- editableCheckBox ----
                    editableCheckBox.setText("Editable");
                    editableCheckBox.setName("editableCheckBox");
                    editableCheckBox.addItemListener(e -> editableCheckBoxItemStateChanged());
                    panel4.add(editableCheckBox, cc.xy(3, 1));

                    //---- geoRectifyButton ----
                    geoRectifyButton.setText("GeoRectify...");
                    geoRectifyButton.setName("geoRectifyButton");
                    geoRectifyButton.addActionListener(e -> geoRectifyButtonActionPerformed());
                    panel4.add(geoRectifyButton, cc.xy(1, 3));

                    //---- exportButton ----
                    exportButton.setText("Export...");
                    exportButton.setName("exportButton");
                    exportButton.addActionListener(e -> exportButtonActionPerformed());
                    panel4.add(exportButton, cc.xy(3, 3));
                }
                surfaceImagePropertiesPane.add(panel4, cc.xy(1, 3));
            }
            panel5.add(surfaceImagePropertiesPane, cc.xy(1, 2));
        }
        add(panel5, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    protected JLabel nameLabel_;
    protected JTextField nameTextField;
    protected JLabel opacityLabel;
    private JSpinner opacitySpinner;
    private JSlider opacitySlider;
    private JLabel detailHintLabel;
    private JSpinner detailHintSpinner;
    private JSlider detailHintSlider;
    protected JPanel panel2;
    private JLabel visibleAltitudeUnits;
    protected JPanel panel1;
    private JFormattedTextField minVisibleAltitudeTextField;
    private JFormattedTextField maxVisibleAltitudeTextField;
    private JPanel surfaceImagePropertiesPane;
    protected JSeparator separator1;
    protected JPanel panel4;
    protected JLabel surfaceImageLabel;
    protected JCheckBox editableCheckBox;
    protected JButton geoRectifyButton;
    protected JButton exportButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
