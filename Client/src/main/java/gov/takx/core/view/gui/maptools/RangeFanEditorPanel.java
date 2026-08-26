package gov.takx.core.view.gui.maptools;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.nasa.worldwindx.examples.util.ShapeUtils;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IGlobeRangeFanMapObject;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.platform.IProgressCallback;
import gov.takx.api.shapes.IMutableRangeFan;
import gov.takx.api.shapes.IRangeFan;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.api.shapes.ShapeType;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.swing.BoundedDoubleTextField;
import gov.takx.commons.swing.IPanelStateListener;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import java.awt.CardLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Editor panel for the Range Fan shape.  Controls are provided to create range fans with specified orientations,
 * sweep angles, min/max distances and intervals.
 * <p/>
 * There are three modes/uses for which this panel will be instantiated.
 * <ol>
 * <li>The initial creation properties when placing a range fan on the globe (drawing tool)</li>
 * <li>The properties of a selected range fan, showing in the right side properties tab (direct action mode)</li>
 * <li>The preferences tab of the map object properties dialog (uses an apply action)</li>
 * </ol>
 *
 * @since 1.16
 */
@PseudoBean
public class RangeFanEditorPanel extends AShapePropertiesEditorPanel<IMutableRangeFan>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @SuppressWarnings("DataFlowIssue")
    private static final URL ICON_URL = UIResources.getUrl(PLATFORM_ICONS, ShapeType.RANGE_FAN.getIconName());

    // RangeFan does not accept 0 min or 0 interval, so we use 1cm as the min distance for the min ring and min distance
    // between rings
    private static final double MIN_METERS = 0.01;

    private static final List<String> SHAPE_PROPERTIES_TO_WATCH = List.of(
            ShapeProperties.INTERVAL, ShapeProperties.MAX_DISTANCE,
            ShapeProperties.MIN_DISTANCE, ShapeProperties.NAME, ShapeProperties.RANGE_DISTANCES,
            ShapeProperties.RANGE_LABELS, ShapeProperties.RANGE_TYPE, ShapeProperties.SAMPLE_DISTANCE,
            ShapeProperties.SHOW_EDGES, ShapeProperties.SHOW_ORIENTATION, ShapeProperties.SHOW_RANGE_VALUES,
            ShapeProperties.SWEEP);

    private final CardLayout distancesCardLayout;

    private TerrainProgressListener terrainProgressListener;
    private CustomDistancesTable distancesTable;

    private boolean changedFromDefaults = false; // Did the user change the default values we installed?

    /**
     * Constructor used when this panel is in the right-side properties panel for initial creation of a shape.
     */
    public RangeFanEditorPanel()
    {
        this(null, true, null);
    }

    /**
     * Constructor used when this panel is in the right-side properties panel to edit an existing shape.
     */
    public RangeFanEditorPanel(@Nonnull IMutableRangeFan rangeFan)
    {
        this(rangeFan, true, null);
    }

    /**
     * Constructor used when this panel is being used in the Preferences dialog.
     *
     * @param rangeFan Map Object to edit
     * @param panelListener Listener for "panel dirty" events
     */
    public RangeFanEditorPanel(@Nonnull IMutableRangeFan rangeFan, @Nonnull IPanelStateListener panelListener)
    {
        this(rangeFan, false, panelListener);
    }

    @Override
    public URL getIcon()
    {
        return ICON_URL;
    }

    /**
     * Private constructor.
     *
     * @param rangeFan Map Object to edit
     * @param directActionMode Pass {@code true} to immediately apply changes to UI controls
     * @param panelListener Listener for "panel dirty" events, {@code null} if not being used
     */
    private RangeFanEditorPanel(@Nullable IMutableRangeFan rangeFan, boolean directActionMode,
                                @Nullable IPanelStateListener panelListener)
    {
        super("Range Rings", rangeFan, directActionMode, panelListener);

        setupControls();

        distancesCardLayout = (CardLayout) distanceCardLayoutPanel.getLayout();

        if (rangeFan != null) setShape(rangeFan);
    }

    @PostConstruct
    private void postConstruct()
    {
        registerForEvents();
        updateGeometryControls();

        if (initialCreationOnly)
        {
            txtName.setText(nextFriendlyName(MapToolType.DRAWING_RANGE_FAN)); // Install the default name
            setDefaultSizes();
        } else
        {
            terrainProgressListener = new TerrainProgressListener();
        }
    }

    /**
     * Since this panel forces its values onto a new shape as it is created, it was always creating a range fan
     * of the default values, regardless of the current globe view.  This method uses the viewport of the last
     * focused globe to guess at a reasonable size (min/max distances) for the range fan.
     *
     * @since 3.3
     */
    private void setDefaultSizes()
    {
        final double rawMaxSize = ShapeUtils.getViewportScaleFactor(getWwd()) * rangeUnits.getMultiplier();
        final double maxSize = rawMaxSize < 1 ? rawMaxSize : Math.ceil(rawMaxSize);
        final double rawIntervalSize = maxSize / 5; // 5 rings
        final double intervalSize = rawIntervalSize < 1 ? rawIntervalSize : Math.floor(rawIntervalSize);

        minTextField.setValue(intervalSize);
        maxTextField.setValue(maxSize);
        intervalTextField.setValue(intervalSize);

        final double sampleDistance = calculateDefaultSampleDistance(maxSize);
        sampleDistanceSpinner.setValue(sampleDistance);

        logger.debug("Defaults set: max: {} interval and min: {} sample: {}", maxSize, intervalSize, sampleDistance);

        changedFromDefaults = false;
    }

    private double calculateDefaultSampleDistance(double size)
    {
        if (size < 1) return size; // prevent NaN

        int digitCount = (int) Math.log10(size);
        int multiplier = size > 0 ? (int) Math.pow(10, digitCount) : 1;
        double rounded = (Math.floor(size / multiplier)) * multiplier;
        return Math.max(rounded / 100, 10);
    }

    /**
     * Common control setup regardless of the way we are created.
     */
    private void setupControls()
    {
        updatingPanel = true;

        initComponents();

        initializeTextField(minTextField, minUnitsComboBox, d -> minValueChanged());
        initializeTextField(maxTextField, maxUnitsComboBox, d -> maxValueChanged());
        initializeTextField(intervalTextField, intervalUnitsComboBox, d -> intervalValueChanged());

        samplingUnitsComboBox.setSpinner(sampleDistanceSpinner);
        samplingUnitsComboBox.setSelectedItem(rangeUnits);

        updateDistanceRelations();

        pnlTerrainFollowing.setVisible(false);

        distancesTable = new CustomDistancesTable(null, null);
        distancesTable.setEditable(false); // No direct editing, must use "edit" button
        distancesTable.setVisibleRowCount(5);
        scrollDistancesTable.setViewportView(distancesTable);
        distancesTable.setUnitsLabel(rangeUnits.getAbbreviation());

        updatingPanel = false;
    }

    /**
     * @param textField The value text field
     * @param comboBox The units of measure combo box associated with the text field
     * @param consumer The value changed consumer
     * @since 5.5.1
     */
    private void initializeTextField(@Nonnull BoundedDoubleTextField textField, @Nonnull UnitOfMeasureComboBox comboBox,
                                     @Nonnull Consumer<Double> consumer)
    {
        comboBox.setComponentAccessors(textField::getDoubleValue, textField::setValue);
        comboBox.setSelectedItem(rangeUnits);
        textField.setMaxDecimalDigits(4); // This is enough to show nautical mile values with max zoom
        textField.setChangeListener(consumer);
    }

    @Override
    public void releaseResources()
    {
        if (target != null) target.removePropertyChangeListener(SHAPE_PROPERTIES_TO_WATCH, this);

        if (terrainProgressListener != null)
        {
            terrainProgressListener.releaseResources();
            terrainProgressListener = null;
        }
        super.releaseResources();
    }

    @Override
    protected void updateGeometryControls()
    {
        updatingPanel = true;

        minUnitsComboBox.setSelectedItem(rangeUnits);
        maxUnitsComboBox.setSelectedItem(rangeUnits);
        intervalUnitsComboBox.setSelectedItem(rangeUnits);
        samplingUnitsComboBox.setSelectedItem(rangeUnits);

        // Rescale table values to new units
        final List<Double> distances = convertArrayUnits(distancesTable.getDistances(), getLengthRescaleMultiplier());
        distancesTable.setDistancesAndLabels(distances, distancesTable.getLabels());

        updateDistanceRelations();

        updatingPanel = false;
    }

    private List<Double> convertArrayUnits(List<Double> distances, double multiplier)
    {
        return distances == null
                ? null
                : distances.stream()
                        .mapToDouble(d -> (d * multiplier * 1_000_000.0) / 1_000_000.0)
                        .boxed().toList();
    }

    @Override
    protected void initializeNewInstance()
    {
        // If the user hasn't changed the initial values, then re-calculate them now.  This handles the case where
        // the user changes the viewport after enabling this tool.
        if (!changedFromDefaults) setDefaultSizes();
    }

    /**
     * Set the UI control values from the given Range Fan.
     */
    private void setShape(final IMutableRangeFan rangeFan)
    {
        logger.debug("RangeFanPanel.setShape {}", rangeFan);

        updatingPanel = true; // Avoid cascading updates

        target = rangeFan;

        txtName.setText(rangeFan.getName());
        spnOrientation.setValue(rangeFan.getOrientation());
        spnSweep.setValue(rangeFan.getSweep());

        final double multiplier = rangeUnits.getMultiplier();

        // Load the distances table
        final List<Double> rangeDistances = convertArrayUnits(rangeFan.getRangeDistances(), multiplier);
        distancesTable.setDistancesAndLabels(rangeDistances, rangeFan.getRangeLabels());

        minUnitsComboBox.fromMeters(rangeFan.getMinDistance());
        intervalUnitsComboBox.fromMeters(rangeFan.getInterval());
        maxUnitsComboBox.fromMeters(rangeFan.getMaxDistance());
        samplingUnitsComboBox.fromMeters(rangeFan.getSampleDistance());

        updateDistanceControls(rangeDistances);

        chkEditable.setSelected(rangeFan.isEditable());
        chkShowAnnotations.setSelected(rangeFan.isShowRangeValues());
        chkShowOrientation.setSelected(rangeFan.isShowOrientation());
        chkShowEdgeLines.setSelected(rangeFan.isShowEdges());

        final boolean followTerrain = rangeFan.getRangeType() == IRangeFan.FOLLOW_TERRAIN;
        chkFollowTerrain.setSelected(followTerrain);
        pnlTerrainFollowing.setVisible(followTerrain);

        updatingPanel = false;

        rangeFan.addPropertyChangeListener(SHAPE_PROPERTIES_TO_WATCH, this);
    }

    @Override
    public String validateContents()
    {
        if (rbDiscrete.isSelected() && distancesTable.getDistances().isEmpty())
        {
            return "Discrete distances selected, but none are defined";
        }

        return null;
    }

    /**
     * Sets the shape properties but ignore the events.
     */
    @Override
    protected void updateTargetProperties()
    {
        final String name = txtName.getText();
        if (!name.isEmpty()) target.setName(name);

        target.setOrientation((Double) spnOrientation.getValue());
        target.setSweep((Double) spnSweep.getValue());
        target.setEditable(chkEditable.isSelected());
        target.setShowEdges(chkShowEdgeLines.isSelected());
        target.setShowOrientation(chkShowOrientation.isSelected());
        target.setShowRangeValues(chkShowAnnotations.isSelected());
        target.setRangeType(chkFollowTerrain.isSelected() ? IRangeFan.FOLLOW_TERRAIN : IRangeFan.GREAT_CIRCLE);

        target.setSampleDistance(samplingUnitsComboBox.toMeters());

        final double minDistance = minTextField.getDoubleValue();
        final double maxDistance = maxTextField.getDoubleValue();

        // Set interval first so we create an appropriate number of rings for the renderer
        target.setInterval(intervalUnitsComboBox.toMeters());

        if (minDistance < maxDistance)
        {
            target.setMinDistance(minUnitsComboBox.toMeters());
            target.setMaxDistance(maxUnitsComboBox.toMeters());
        } else
        {
            logger.error("Min distance must not be greater than Max distance");
        }

        if (rbDiscrete.isSelected())
        {
            setDistancesFromTable();
        } else
        {
            target.clearRangeDistances(); // Clear out any discrete distances
        }
    }

    /**
     * Handle a property change on the shape we are working on.  This happens when a change is made programmatically,
     * by another configuration panel instance, or the user dragging/resizing the shape.
     *
     * @param evt change event
     */
    @Override
    @InvokeOnEDT
    public void propertyChange(final PropertyChangeEvent evt)
    {
        final String propertyName = evt.getPropertyName();
        final Object value = evt.getNewValue();

        logger.debug("property change: ignore {} : {}={}", ignoreEvent, propertyName, value);

        if (ignoreEvent) return;

        updatingPanel = true;  // Avoid cascading events

        // Update our controls to match the updated property
        switch (propertyName)
        {
            case ShapeProperties.NAME -> txtName.setText((String) value);
            case ShapeProperties.ORIENTATION -> spnOrientation.setValue(value);
            case ShapeProperties.MAX_DISTANCE ->
            {
                maxUnitsComboBox.fromMeters((double) value);
                updateDistanceRelations();
            }
            case ShapeProperties.MIN_DISTANCE ->
            {
                minUnitsComboBox.fromMeters((double) value);
                updateDistanceRelations();
            }
            case ShapeProperties.INTERVAL -> intervalUnitsComboBox.fromMeters((double) value);
            case ShapeProperties.SWEEP -> spnSweep.setValue(value);
            case ShapeProperties.SHOW_RANGE_VALUES -> chkShowAnnotations.setSelected((boolean) value);
            case ShapeProperties.SHOW_ORIENTATION -> chkShowOrientation.setSelected((boolean) value);
            case ShapeProperties.SHOW_EDGES -> chkShowEdgeLines.setSelected((boolean) value);
            case ShapeProperties.EDITABLE -> chkEditable.setSelected((boolean) value);
            case ShapeProperties.RANGE_TYPE ->
            {
                boolean followTerrain = (int) value == IRangeFan.FOLLOW_TERRAIN;
                chkFollowTerrain.setSelected(followTerrain);
                pnlTerrainFollowing.setVisible(followTerrain);
            }
            case ShapeProperties.SAMPLE_DISTANCE -> samplingUnitsComboBox.fromMeters((double) value);
            case ShapeProperties.RANGE_DISTANCES, ShapeProperties.RANGE_LABELS ->
            {
                final List<Double> distances = target.getRangeDistances();
                final List<String> rangeLabels = target.getRangeLabels();

                // Separate events are fired for distances and range labels. If both distances and range labels are
                // being used but their counts aren't equal yet, skip the update and wait for the second event.
                if (distances != null && rangeLabels != null && distances.size() != rangeLabels.size()) break;

                updateDistanceControls(distances);
                final List<Double> rangeDistances = convertArrayUnits(distances, rangeUnits.getMultiplier());
                distancesTable.setDistancesAndLabels(rangeDistances, rangeLabels);
            }
            default -> logger.debug("Ignored {}", evt);
        }

        updatingPanel = false;
    }

    /**
     * @return Multiplier to switch from prior range units to current range units
     * @since 5.5 moved from {@link AShapePropertiesEditorPanel}
     */
    private double getLengthRescaleMultiplier()
    {
        return rangeUnits.getMultiplier() / priorRangeUnits.getMultiplier();
    }

    private void updateDistanceControls(List<Double> rangeDistances)
    {
        if (rangeDistances != null)
        {
            rbDiscrete.doClick();
        } else
        {
            rbInterval.doClick();
        }
    }

    private void chkFollowTerrainActionPerformed()
    {
        final boolean followTerrain = chkFollowTerrain.isSelected();

        updateProperty(() -> {
            pnlTerrainFollowing.setVisible(followTerrain);
            target.setRangeType(followTerrain ? IRangeFan.FOLLOW_TERRAIN : IRangeFan.GREAT_CIRCLE);
        });

        // Special handling when we're in the Preferences dialog.  We need to show/hide the terrain following sub-panel
        // even though the property has not been changed on the shape.
        if (!directActionMode)
        {
            pnlTerrainFollowing.setVisible(followTerrain);
            lblStatus.setText("Pending");
        }
    }

    /**
     * Display the custom distances editor.
     */
    private void btnEditDistancesActionPerformed()
    {
        final Window ancestor = SwingUtilities.getWindowAncestor(this);
        final var editor = new RangeFanDistancesEditor(ancestor, distancesTable.getDistances(), distancesTable.getLabels());

        editor.setVisible(true);

        // If the user didn't cancel, then publish the results
        if (!editor.isCanceled())
        {
            // Update our local table model
            distancesTable.setDistancesAndLabels(editor.getDistances(), editor.getLabels());
            updateProperty(this::setDistancesFromTable);
        }
    }

    /**
     * If we have a valid distance table model, then install it.
     * Otherwise, whatever was previously installed will remain in force.
     */
    private void setDistancesFromTable()
    {
        final List<Double> distances = convertArrayUnits(distancesTable.getDistances(),
                1.0 / rangeUnits.getMultiplier());

        if (distances != null && !distances.isEmpty())
        {
            target.setRangeDistances(distances, distancesTable.getLabels());
        }
    }

    /**
     * Make the panel associated with the distance mode visible.
     *
     * @param e Event holding card name (as action command)
     */
    private void distanceModeActionPerformed(ActionEvent e)
    {
        distancesCardLayout.show(distanceCardLayoutPanel, e.getActionCommand());

        updateProperty(() -> {
            if (rbDiscrete.isSelected())
            {
                setDistancesFromTable();
            } else
            {
                target.clearRangeDistances(); // Clear out any discrete distances
            }
        });
    }

    private void minValueChanged()
    {
        updateProperty(() -> target.setMinDistance(minUnitsComboBox.toMeters()));
        updateDistanceRelations();
        changedFromDefaults = true;
    }

    private void maxValueChanged()
    {
        updateProperty(() -> target.setMaxDistance(maxUnitsComboBox.toMeters()));
        updateDistanceRelations();
        changedFromDefaults = true;
    }

    /**
     * Update the min/max values on the min/max distance text fields to keep them from ever "passing" each other.
     * @since 3.3
     */
    private void updateDistanceRelations()
    {
        final double minMeters = minUnitsComboBox.toMeters();
        final double maxMeters = maxUnitsComboBox.toMeters();
        final double intervalMeters = intervalUnitsComboBox.toMeters();
        final double rangeMeters = maxMeters - minMeters;

        minTextField.setBounds(minUnitsComboBox.convertToUnit(MIN_METERS), minUnitsComboBox.convertToUnit(maxMeters - MIN_METERS));
        maxTextField.setBounds(maxUnitsComboBox.convertToUnit(minMeters + MIN_METERS), null);
        intervalTextField.setBounds(intervalUnitsComboBox.convertToUnit(MIN_METERS), rangeMeters);

        // Don't let the interval exceed the range
        if (intervalMeters > rangeMeters) intervalUnitsComboBox.fromMeters(rangeMeters);
    }

    private void txtNameChanged()
    {
        updateProperty(() -> target.setName(txtName.getText()));
    }

    private void intervalValueChanged()
    {
        updateProperty(() -> target.setInterval(intervalUnitsComboBox.toMeters()));
        changedFromDefaults = true;
    }

    private void spnOrientationStateChanged()
    {
        updateProperty(() -> target.setOrientation((double) spnOrientation.getValue()));
    }

    private void chkShowAnnotations()
    {
        updateProperty(() -> target.setShowRangeValues(chkShowAnnotations.isSelected()));
    }

    private void chkShowEdgeLines()
    {
        updateProperty(() -> target.setShowEdges(chkShowEdgeLines.isSelected()));
    }

    private void chkShowOrientation()
    {
        updateProperty(() -> target.setShowOrientation(chkShowOrientation.isSelected()));
    }

    private void chkEditable()
    {
        updateProperty(() -> target.setEditable(chkEditable.isSelected()));
    }

    private void sampleDistanceSpinnerStateChanged()
    {
        updateProperty(() -> target.setSampleDistance(samplingUnitsComboBox.toMeters()));
    }

    private void spnSweepStateChanged()
    {
        updateProperty(() -> target.setSweep((double) spnSweep.getValue()));
    }

    /**
     * Listener for terrain following calculation progress.
     * @since 3.3
     */
    private final class TerrainProgressListener implements IProgressCallback, IResourceCleaner
    {
        private final IGlobeRangeFanMapObject globeRangeFan;

        private TerrainProgressListener()
        {
            globeRangeFan = (IGlobeRangeFanMapObject) globeManager.getPrimaryGlobeDelegate()
                    .getGlobeMapObject(target.getKey());

            globeRangeFan.addProgressCallback(this);
        }

        @Override
        public void releaseResources()
        {
            globeRangeFan.removeProgressCallback(this);
        }

        @Override
        @InvokeOnEDT
        public void onProgress(double progress)
        {
            final int intProgress = (int) (progress * 100);

            lblTerrainCalcError.setVisible(false);
            lblStatus.setText("Calculating");
            progressBar.setValue(intProgress);
            progressBar.setVisible(true);

            if (intProgress == 100)
            {
                progressBar.setVisible(false);
                lblTerrainCalcError.setVisible(false);
                lblStatus.setText("Up to date");
            }
        }

        @Override
        @InvokeOnEDT
        public void onError(Throwable t)
        {
            boolean canceled = t instanceof CancellationException;
            lblStatus.setText(canceled ? "Canceled" : "Error");
            lblTerrainCalcError.setVisible(!canceled);
            progressBar.setVisible(false);
        }

        @Override
        public void onStageStarted(String stage)
        {
        }

        @Override
        public boolean isCancel()
        {
            return false;
        }
    }

    @SuppressWarnings("Convert2MethodRef")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var pnlName = new JPanel();
        var label6 = new JLabel();
        txtName = new JTextField();
        var panel1 = new JPanel();
        var distanceModePanel = new JPanel();
        var radioButtonPanel = new JPanel();
        rbInterval = new JRadioButton();
        rbDiscrete = new JRadioButton();
        distanceCardLayoutPanel = new JPanel();
        var useIntervalPanel = new JPanel();
        var lblMinDistance = new JLabel();
        minTextField = new BoundedDoubleTextField();
        minUnitsComboBox = new UnitOfMeasureComboBox();
        var lblMaxDistance = new JLabel();
        maxTextField = new BoundedDoubleTextField();
        maxUnitsComboBox = new UnitOfMeasureComboBox();
        var lblInterval = new JLabel();
        intervalTextField = new BoundedDoubleTextField();
        intervalUnitsComboBox = new UnitOfMeasureComboBox();
        var useDiscretePanel = new JPanel();
        scrollDistancesTable = new JScrollPane();
        var btnEditDistances = new JButton();
        var fieldOfViewPanel = new JPanel();
        var label1 = new JLabel();
        spnOrientation = new JSpinner();
        var label2 = new JLabel();
        spnSweep = new JSpinner();
        chkShowEdgeLines = new JCheckBox();
        chkShowOrientation = new JCheckBox();
        var terrainOptionsPanel = new JPanel();
        var basicPanel = new JPanel();
        chkShowAnnotations = new JCheckBox();
        chkEditable = new JCheckBox();
        chkFollowTerrain = new JCheckBox();
        pnlTerrainFollowing = new JPanel();
        var pnlTerrainInputs = new JPanel();
        var lblSampleDistance = new JLabel();
        var panel2 = new JPanel();
        sampleDistanceSpinner = new JSpinner();
        samplingUnitsComboBox = new UnitOfMeasureComboBox();
        var statusLabel = new JLabel();
        var statusPanel = new JPanel();
        lblStatus = new JLabel();
        progressBar = new JProgressBar();
        lblTerrainCalcError = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default, $ugap, $lgap, top:default, $ugap, top:default, fill:default:grow"));

        //======== pnlName ========
        {
            pnlName.setName("pnlName");
            pnlName.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "default"));

            //---- label6 ----
            label6.setText("Name: ");
            label6.setHorizontalAlignment(SwingConstants.RIGHT);
            label6.setName("label6");
            pnlName.add(label6, cc.xy(1, 1));

            //---- txtName ----
            txtName.setToolTipText("A name that describes the range rings");
            txtName.setName("txtName");
            txtName.addActionListener(e -> txtNameChanged());
            txtName.addFocusListener(new FocusAdapter()
            {
                @Override
                public void focusLost(FocusEvent e)
                {
                    txtNameChanged();
                }
            });
            pnlName.add(txtName, cc.xy(3, 1));
        }
        add(pnlName, cc.xy(1, 1));

        //======== panel1 ========
        {
            panel1.setName("panel1");
            panel1.setLayout(new FormLayout(
                    "default:grow, $lcgap, default",
                    "fill:default"));

            //======== distanceModePanel ========
            {
                distanceModePanel.setBorder(new CompoundBorder(
                        new TitledBorder("Distance Mode"),
                        new EmptyBorder(5, 5, 5, 5)));
                distanceModePanel.setName("distanceModePanel");
                distanceModePanel.setLayout(new FormLayout(
                        "default:grow",
                        "default, $lgap, default"));

                //======== radioButtonPanel ========
                {
                    radioButtonPanel.setName("radioButtonPanel");
                    radioButtonPanel.setLayout(new FormLayout(
                            "default:grow, $lcgap, default:grow",
                            "default"));

                    //---- rbInterval ----
                    rbInterval.setText("Use Interval");
                    rbInterval.setToolTipText("Use min/max distance and interval to define range ring distances");
                    rbInterval.setSelected(true);
                    rbInterval.setActionCommand("interval");
                    rbInterval.setName("rbInterval");
                    rbInterval.addActionListener(e -> distanceModeActionPerformed(e));
                    radioButtonPanel.add(rbInterval, cc.xy(1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));

                    //---- rbDiscrete ----
                    rbDiscrete.setText("Use Discrete");
                    rbDiscrete.setToolTipText("Use discrete distance settings with optional labels");
                    rbDiscrete.setActionCommand("discrete");
                    rbDiscrete.setName("rbDiscrete");
                    rbDiscrete.addActionListener(e -> distanceModeActionPerformed(e));
                    radioButtonPanel.add(rbDiscrete, cc.xy(3, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));
                }
                distanceModePanel.add(radioButtonPanel, cc.xy(1, 1));

                //======== distanceCardLayoutPanel ========
                {
                    distanceCardLayoutPanel.setName("distanceCardLayoutPanel");
                    distanceCardLayoutPanel.setLayout(new CardLayout());

                    //======== useIntervalPanel ========
                    {
                        useIntervalPanel.setName("useIntervalPanel");
                        useIntervalPanel.setLayout(new FormLayout(
                                "right:default:grow, $lcgap, default, $lcgap, 32dlu",
                                "2*(default, $lgap), default"));

                        //---- lblMinDistance ----
                        lblMinDistance.setText("Min");
                        lblMinDistance.setHorizontalAlignment(SwingConstants.RIGHT);
                        lblMinDistance.setName("lblMinDistance");
                        useIntervalPanel.add(lblMinDistance, cc.xy(1, 1));

                        //---- minTextField ----
                        minTextField.setText("1");
                        minTextField.setName("minTextField");
                        useIntervalPanel.add(minTextField, cc.xy(3, 1));

                        //---- minUnitsComboBox ----
                        minUnitsComboBox.setName("minUnitsComboBox");
                        useIntervalPanel.add(minUnitsComboBox, cc.xy(5, 1));

                        //---- lblMaxDistance ----
                        lblMaxDistance.setText("Max");
                        lblMaxDistance.setHorizontalAlignment(SwingConstants.RIGHT);
                        lblMaxDistance.setName("lblMaxDistance");
                        useIntervalPanel.add(lblMaxDistance, cc.xy(1, 3));

                        //---- maxTextField ----
                        maxTextField.setText("100");
                        maxTextField.setName("maxTextField");
                        useIntervalPanel.add(maxTextField, cc.xy(3, 3));

                        //---- maxUnitsComboBox ----
                        maxUnitsComboBox.setName("maxUnitsComboBox");
                        useIntervalPanel.add(maxUnitsComboBox, cc.xy(5, 3));

                        //---- lblInterval ----
                        lblInterval.setText("Interval");
                        lblInterval.setHorizontalAlignment(SwingConstants.RIGHT);
                        lblInterval.setName("lblInterval");
                        useIntervalPanel.add(lblInterval, cc.xy(1, 5));

                        //---- intervalTextField ----
                        intervalTextField.setText("1");
                        intervalTextField.setName("intervalTextField");
                        useIntervalPanel.add(intervalTextField, cc.xy(3, 5));

                        //---- intervalUnitsComboBox ----
                        intervalUnitsComboBox.setName("intervalUnitsComboBox");
                        useIntervalPanel.add(intervalUnitsComboBox, cc.xy(5, 5));
                    }
                    distanceCardLayoutPanel.add(useIntervalPanel, "interval");

                    //======== useDiscretePanel ========
                    {
                        useDiscretePanel.setName("useDiscretePanel");
                        useDiscretePanel.setLayout(new FormLayout(
                                "default:grow",
                                "2*(default, $lgap), default"));

                        //======== scrollDistancesTable ========
                        {
                            scrollDistancesTable.setName("scrollDistancesTable");
                        }
                        useDiscretePanel.add(scrollDistancesTable, cc.xy(1, 3));

                        //---- btnEditDistances ----
                        btnEditDistances.setText("Edit Custom Distances");
                        btnEditDistances.setToolTipText("Click to edit the table of custom distances and labels");
                        btnEditDistances.setName("btnEditDistances");
                        btnEditDistances.addActionListener(e -> btnEditDistancesActionPerformed());
                        useDiscretePanel.add(btnEditDistances, cc.xy(1, 5));
                    }
                    distanceCardLayoutPanel.add(useDiscretePanel, "discrete");
                }
                distanceModePanel.add(distanceCardLayoutPanel, cc.xy(1, 3));
            }
            panel1.add(distanceModePanel, cc.xy(1, 1));

            //======== fieldOfViewPanel ========
            {
                fieldOfViewPanel.setBorder(new CompoundBorder(
                        new TitledBorder("Field of View"),
                        new EmptyBorder(5, 5, 5, 5)));
                fieldOfViewPanel.setName("fieldOfViewPanel");
                fieldOfViewPanel.setLayout(new FormLayout(
                        "2*(default:grow)",
                        "3*(default, $lgap), default"));

                //---- label1 ----
                label1.setText("Orientation: ");
                label1.setLabelFor(spnOrientation);
                label1.setHorizontalAlignment(SwingConstants.RIGHT);
                label1.setName("label1");
                fieldOfViewPanel.add(label1, cc.xy(1, 1));

                //---- spnOrientation ----
                spnOrientation.setToolTipText("Orientation Line of Bearing in degrees (0 is North)");
                spnOrientation.setModel(new SpinnerNumberModel(0.0, 0.0, 360.0, 10.0));
                spnOrientation.setName("spnOrientation");
                spnOrientation.addChangeListener(e -> spnOrientationStateChanged());
                fieldOfViewPanel.add(spnOrientation, cc.xy(2, 1));

                //---- label2 ----
                label2.setText("Spread: ");
                label2.setLabelFor(spnSweep);
                label2.setHorizontalAlignment(SwingConstants.RIGHT);
                label2.setName("label2");
                fieldOfViewPanel.add(label2, cc.xy(1, 3));

                //---- spnSweep ----
                spnSweep.setToolTipText("The sweep angle in degrees");
                spnSweep.setModel(new SpinnerNumberModel(360.0, 0.0, 360.0, 10.0));
                spnSweep.setName("spnSweep");
                spnSweep.addChangeListener(e -> spnSweepStateChanged());
                fieldOfViewPanel.add(spnSweep, cc.xy(2, 3));

                //---- chkShowEdgeLines ----
                chkShowEdgeLines.setText("Show Edge Lines");
                chkShowEdgeLines.setSelected(true);
                chkShowEdgeLines.setName("chkShowEdgeLines");
                chkShowEdgeLines.addActionListener(e -> chkShowEdgeLines());
                fieldOfViewPanel.add(chkShowEdgeLines, cc.xywh(1, 5, 2, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));

                //---- chkShowOrientation ----
                chkShowOrientation.setText("Show Orientation Line");
                chkShowOrientation.setName("chkShowOrientation");
                chkShowOrientation.addActionListener(e -> chkShowOrientation());
                fieldOfViewPanel.add(chkShowOrientation, cc.xywh(1, 7, 2, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));
            }
            panel1.add(fieldOfViewPanel, cc.xy(3, 1));
        }
        add(panel1, cc.xy(1, 4));

        //======== terrainOptionsPanel ========
        {
            terrainOptionsPanel.setName("terrainOptionsPanel");
            terrainOptionsPanel.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "top:default"));

            //======== basicPanel ========
            {
                basicPanel.setBorder(new EmptyBorder(10, 5, 0, 5));
                basicPanel.setName("basicPanel");
                basicPanel.setLayout(new FormLayout(
                        "default:grow",
                        "2*(default, $lgap), default"));

                //---- chkShowAnnotations ----
                chkShowAnnotations.setText("Show Annotations");
                chkShowAnnotations.setSelected(true);
                chkShowAnnotations.setName("chkShowAnnotations");
                chkShowAnnotations.addActionListener(e -> chkShowAnnotations());
                basicPanel.add(chkShowAnnotations, cc.xy(1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));

                //---- chkEditable ----
                chkEditable.setText("Editable");
                chkEditable.setSelected(true);
                chkEditable.setName("chkEditable");
                chkEditable.addActionListener(e -> chkEditable());
                basicPanel.add(chkEditable, cc.xy(1, 3, CellConstraints.LEFT, CellConstraints.DEFAULT));

                //---- chkFollowTerrain ----
                chkFollowTerrain.setText("Follow Terrain");
                chkFollowTerrain.setName("chkFollowTerrain");
                chkFollowTerrain.addActionListener(e -> chkFollowTerrainActionPerformed());
                basicPanel.add(chkFollowTerrain, cc.xy(1, 5, CellConstraints.LEFT, CellConstraints.DEFAULT));
            }
            terrainOptionsPanel.add(basicPanel, cc.xy(1, 1));

            //======== pnlTerrainFollowing ========
            {
                pnlTerrainFollowing.setBorder(new CompoundBorder(
                        new TitledBorder("Terrain Calculations"),
                        new EmptyBorder(5, 5, 5, 5)));
                pnlTerrainFollowing.setName("pnlTerrainFollowing");
                pnlTerrainFollowing.setLayout(new FormLayout(
                        "default:grow",
                        "default, $lgap, default"));

                //======== pnlTerrainInputs ========
                {
                    pnlTerrainInputs.setName("pnlTerrainInputs");
                    pnlTerrainInputs.setLayout(new FormLayout(
                            "default:grow, $lcgap, default:grow",
                            "default, $lgap, default"));

                    //---- lblSampleDistance ----
                    lblSampleDistance.setText("Sampling distance");
                    lblSampleDistance.setToolTipText("<html>Enter the length of the sampling steps.<html>");
                    lblSampleDistance.setName("lblSampleDistance");
                    pnlTerrainInputs.add(lblSampleDistance, cc.xy(1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));

                    //======== panel2 ========
                    {
                        panel2.setName("panel2");
                        panel2.setLayout(new FormLayout(
                                "default, $lcgap, 32dlu",
                                "default"));

                        //---- sampleDistanceSpinner ----
                        sampleDistanceSpinner.setModel(new SpinnerNumberModel(10.0, 0.001, null, 1.0));
                        sampleDistanceSpinner.setToolTipText("<html>Enter the length in meters of the sampling steps.<html>");
                        sampleDistanceSpinner.setName("sampleDistanceSpinner");
                        sampleDistanceSpinner.addChangeListener(e -> sampleDistanceSpinnerStateChanged());
                        panel2.add(sampleDistanceSpinner, cc.xy(1, 1));

                        //---- samplingUnitsComboBox ----
                        samplingUnitsComboBox.setName("samplingUnitsComboBox");
                        panel2.add(samplingUnitsComboBox, cc.xy(3, 1));
                    }
                    pnlTerrainInputs.add(panel2, cc.xy(3, 1));

                    //---- statusLabel ----
                    statusLabel.setText("Status:");
                    statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
                    statusLabel.setName("statusLabel");
                    pnlTerrainInputs.add(statusLabel, cc.xy(1, 3, CellConstraints.RIGHT, CellConstraints.DEFAULT));

                    //======== statusPanel ========
                    {
                        statusPanel.setName("statusPanel");
                        statusPanel.setLayout(new FormLayout(
                                "default:grow",
                                "14dlu"));

                        //---- lblStatus ----
                        lblStatus.setText("Up to date");
                        lblStatus.setName("lblStatus");
                        statusPanel.add(lblStatus, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

                        //---- progressBar ----
                        progressBar.setVisible(false);
                        progressBar.setName("progressBar");
                        statusPanel.add(progressBar, cc.xy(1, 1));
                    }
                    pnlTerrainInputs.add(statusPanel, cc.xy(3, 3));
                }
                pnlTerrainFollowing.add(pnlTerrainInputs, cc.xy(1, 1));

                //---- lblTerrainCalcError ----
                lblTerrainCalcError.setText("Error, unable to load high resolution terrain");
                lblTerrainCalcError.setVisible(false);
                lblTerrainCalcError.setName("lblTerrainCalcError");
                pnlTerrainFollowing.add(lblTerrainCalcError, cc.xy(1, 3));
            }
            terrainOptionsPanel.add(pnlTerrainFollowing, cc.xy(3, 1));
        }
        add(terrainOptionsPanel, cc.xy(1, 6));

        //---- buttonGroup1 ----
        var buttonGroup1 = new ButtonGroup();
        buttonGroup1.add(rbInterval);
        buttonGroup1.add(rbDiscrete);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTextField txtName;
    private JRadioButton rbInterval;
    private JRadioButton rbDiscrete;
    private JPanel distanceCardLayoutPanel;
    private BoundedDoubleTextField minTextField;
    private UnitOfMeasureComboBox minUnitsComboBox;
    private BoundedDoubleTextField maxTextField;
    private UnitOfMeasureComboBox maxUnitsComboBox;
    private BoundedDoubleTextField intervalTextField;
    private UnitOfMeasureComboBox intervalUnitsComboBox;
    private JScrollPane scrollDistancesTable;
    private JSpinner spnOrientation;
    private JSpinner spnSweep;
    private JCheckBox chkShowEdgeLines;
    private JCheckBox chkShowOrientation;
    private JCheckBox chkShowAnnotations;
    private JCheckBox chkEditable;
    private JCheckBox chkFollowTerrain;
    private JPanel pnlTerrainFollowing;
    private JSpinner sampleDistanceSpinner;
    private UnitOfMeasureComboBox samplingUnitsComboBox;
    private JLabel lblStatus;
    private JProgressBar progressBar;
    private JLabel lblTerrainCalcError;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
