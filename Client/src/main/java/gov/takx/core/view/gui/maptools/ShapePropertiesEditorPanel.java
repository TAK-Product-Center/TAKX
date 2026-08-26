package gov.takx.core.view.gui.maptools;

import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.conversions.AngleUtilities;
import com.atakmap.coremap.maps.coords.NorthReference;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.FinalizeShapeEvent;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.mapobject.IShapeArrowMapObject;
import gov.takx.api.mapobject.IShapeBullseyeMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.mapobject.IShapeRangeFanMapObject;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.milsym.IMilStd2525ServiceProvider;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.shapes.IMutableArrow;
import gov.takx.api.shapes.IMutableCircle;
import gov.takx.api.shapes.IMutableRectangle;
import gov.takx.api.shapes.IMutableRegularShape;
import gov.takx.api.shapes.IMutableRingedShape;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.api.shapes.IMutableSquare;
import gov.takx.api.shapes.IRegularShape;
import gov.takx.api.shapes.IRingedShape;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.commons.swing.ComponentUtils;
import gov.takx.commons.swing.FormLayoutUtils;
import gov.takx.commons.swing.IPanelStateListener;
import gov.takx.core.structures.ICoreSurfaceStructureService;
import gov.takx.core.view.gui.maptools.tacticalgraphics.TacticalGraphicsEditorPanel;
import gov.takx.core.view.gui.metadata.MetadataTablePanel;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.units.BearingUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import net.java.dev.colorchooser.ColorChooser;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.lang.invoke.MethodHandles;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

/**
 * This class provides a UI panel for editing the properties of all shapes (except Range Fans, which are "special").
 * It is used in the docked Properties panel, and within the device Configuration dialog.
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class ShapePropertiesEditorPanel extends AShapePropertiesEditorPanel<IMutableShape>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<String> shapePropertyList = Arrays.asList(
            ShapeProperties.BEARING, ShapeProperties.EDITABLE, ShapeProperties.FILL_COLOR, ShapeProperties.HEIGHT, ShapeProperties.LINE_COLOR,
            ShapeProperties.LINE_WIDTH, ShapeProperties.LOCATION, ShapeProperties.OPACITY, ShapeProperties.ORIENTABLE,
            ShapeProperties.ORIENTATION, ShapeProperties.PUBLISHED, ShapeProperties.RADIUS, ShapeProperties.RANGE,
            ShapeProperties.SEMI_MAJOR_AXIS, ShapeProperties.SEMI_MINOR_AXIS, ShapeProperties.VERTICAL_HEIGHT,
            ShapeProperties.VERTICAL_HEIGHT_ENABLED, ShapeProperties.WIDTH,
            IMapObject.ATTRIBUTE_SET);

    private static final CellConstraints CC_1_1 = new CellConstraints(1, 1);

    @Inject
    private ICoreSurfaceStructureService surfaceStructureService;
    @Inject
    private IMilStd2525ServiceProvider milStd2525ServiceProvider;

    private final MapToolType toolType;
    private ShapeGeometricAttributesPanel attributesPanel;
    private MetadataTablePanel metadataPanel;
    private TacticalGraphicsEditorPanel tacticalGraphicsEditorPanel;
    private AttributeSet targetAttributeSet;

    private final IPreferenceMonitor<NorthReference> northReferenceMonitor =
            UserPreferenceEntry.DISPLAY_NORTH_REFERENCE.newMonitor(NorthReference.class, this::onNorthReferenceChange);
    private final IPreferenceMonitor<BearingUnits> bearingUnitsMonitor =
            UserPreferenceEntry.DISPLAY_BEARING_UNIT.newMonitor(BearingUnits.class, this::onBearingUnitsChange);

    protected NorthReference northReference = northReferenceMonitor.getCurrentValue();
    protected NorthReference priorNorthReference = northReference;
    protected BearingUnits bearingUnits = bearingUnitsMonitor.getCurrentValue();
    protected BearingUnits priorBearingUnits = bearingUnits;

    private boolean widthSpinnerDirty;
    private boolean heightSpinnerDirty;
    private boolean radiusSpinnerDirty;
    private boolean rangeSpinnerDirty;
    private boolean bearingSpinnerDirty;

    /**
     * Constructor used when this panel is in the right-side properties panel for initial creation of a shape.
     */
    public ShapePropertiesEditorPanel(@Nonnull MapToolType toolType)
    {
        this(toolType, null, true, null);
    }

    /**
     * Constructor used when this panel is in the right-side properties panel to edit an existing shape.
     */
    public ShapePropertiesEditorPanel(@Nonnull IMutableShape shape)
    {
        this(MapToolType.valueOfShapeType(shape.getType()), shape, true, null);
    }

    /**
     * Constructor used when this panel is being used in the Configuration dialog.
     *
     * @param shape Map Object to edit
     * @param panelListener Listener for "panel dirty" events
     */
    public ShapePropertiesEditorPanel(@Nonnull IMutableShape shape, @Nonnull IPanelStateListener panelListener)
    {
        this(MapToolType.valueOfShapeType(shape.getType()), shape, false, panelListener);
    }

    /**
     * Private constructor.
     *
     * @param toolType Tool type being constructed/edited
     * @param shape Shape to edit
     * @param directActionMode Pass {@code true} to immediately apply changes to UI controls
     * @param panelListener Listener for "panel dirty" events, {@code null} if not being used
     */
    private ShapePropertiesEditorPanel(@Nonnull MapToolType toolType, @Nullable IMutableShape shape,
                                       boolean directActionMode, @Nullable IPanelStateListener panelListener)
    {
        super(shape != null ? shape.getType() : "Shape", shape, directActionMode, panelListener);

        this.toolType = toolType;

        setupControls();

        if (shape != null) setShape(shape);
    }

    @PostConstruct
    private void postConstruct()
    {
        registerForEvents();

        if (initialCreationOnly)
        {
            txtName.setText(nextFriendlyName(toolType)); // Install the default name
        }

        // Only show the metadata panel if there is metadata of interest, and we have a target shape.
        final List<String> metadataOfInterest = surfaceStructureService.getMetaDataOfInterest();
        metadataPanel.setMetadataOfInterest(metadataOfInterest);
        metadataPanel.setVisible(!metadataOfInterest.isEmpty() && target != null);

        // Show the tactical graphics editor panel if there is a shape + map object + milsym code
        //  - unless it is a special shape type (primarily R&B)
        if (target != null)
        {
            final IShapeMapObject<?> mapObject = target.getShapeMapObject();

            if (mapObject != null
                    && !(mapObject instanceof IShapeArrowMapObject
                            || mapObject instanceof IShapeBullseyeMapObject
                            || mapObject instanceof IShapeRangeFanMapObject
                            || mapObject instanceof IRouteMapObject)
                    && !mapObject.getMetaBoolean(StandardAttributeNames.RANGE_BEARING, false))
            {
                tacticalGraphicsEditorPanel.setMapObject(mapObject);
            }
        }

        revalidate();
    }

    private void setupControls()
    {
        updatingPanel = true;

        initComponents();

        heightUnitsComboBox.setSpinner(heightSpinner);
        widthUnitsComboBox.setSpinner(widthSpinner);
        radiusUnitsComboBox.setSpinner(radiusSpinner);
        rangeUnitsComboBox.setSpinner(rangeSpinner);
        verticalHeightUnitsComboBox.setSpinner(verticalHeightSpinner);

        setupColorChooser(fillColorChooser, this::setFillColor);
        setupColorChooser(borderColorChooser, this::setLineColor);

        attributesPanel = new ShapeGeometricAttributesPanel();
        attributesPanelHolder.add(attributesPanel, CC_1_1);

        metadataPanel = new MetadataTablePanel(this::metadataValueChanged);
        metadataPanelHolder.add(metadataPanel, CC_1_1);

        tacticalGraphicsEditorPanel =
                new TacticalGraphicsEditorPanel(this::tacticalGraphicsValueChanged, () -> ignoreEvent, directActionMode);
        tacticalGraphicsPanelHolder.add(tacticalGraphicsEditorPanel, CC_1_1);

        if (!directActionMode || !initialCreationOnly) FormLayoutUtils.hideRow(txtName);

        adjustControlsForShape();
        updateGeometryControls();

        updatingPanel = false;
    }

    private void verticalHeightEnabled(boolean enabled)
    {
        verticalHeightSpinner.setEnabled(enabled);
        verticalHeightUnitsComboBox.setEnabled(enabled);
    }

    private void adjustControlsForShape()
    {
        final boolean supportsVerticalHeight = ShapePropertyUtils.supportsVerticalHeight(toolType);
        FormLayoutUtils.hideOrRestoreRow(verticalHeightSpinner, !supportsVerticalHeight);

        final boolean hasInterior = ShapePropertyUtils.shapeHasInterior(toolType);
        FormLayoutUtils.hideOrRestoreRow(fillColorLabel, !hasInterior);
        borderColorLabel.setText(hasInterior ? "Border Color:" : "Line Color:");
        borderWidthLabel.setText(hasInterior ? "Border Width:" : "Line Width:");

        publishedCheckBox.setVisible(hasInterior);

        // Routes can't have tactical graphics associated with them
        if (toolType == MapToolType.DRAWING_ROUTE) tacticalGraphicsPanelHolder.setVisible(false);

        // For those shapes that are drawn using a "rubber band" mode, there's no need to show the UI controls for
        // width, height, radius, orientation, range, and bearing during initial creation.

        final boolean showOrientation = ShapePropertyUtils.supportsOrientation(toolType)
                && (!initialCreationOnly || !ShapePropertyUtils.usesRubberBandDrawing(toolType));
        FormLayoutUtils.hideOrRestoreRow(orientationLabel, !showOrientation);
        orientableCheckbox.setVisible(showOrientation);

        final boolean isRectangle = toolType == MapToolType.DRAWING_QUAD;
        final boolean isRectangular = isRectangle || toolType == MapToolType.DRAWING_SQUARE;

        final boolean supportsRings = ShapePropertyUtils.supportsRings(toolType);
        FormLayoutUtils.hideOrRestoreRow(ringCountSpinner, !supportsRings);

        final boolean showHeight = !initialCreationOnly && isRectangle;
        FormLayoutUtils.hideOrRestoreRow(heightLabel, !showHeight);

        final boolean showWidth = !initialCreationOnly && isRectangular;
        FormLayoutUtils.hideOrRestoreRow(widthLabel, !showWidth);

        final boolean showRadius = !initialCreationOnly && supportsRadius();
        FormLayoutUtils.hideOrRestoreRow(radiusLabel, !showRadius);

        final boolean showRangeAndBearing = !initialCreationOnly && toolType == MapToolType.DRAWING_ARROW;
        FormLayoutUtils.hideOrRestoreRow(rangeLabel, !showRangeAndBearing);
        FormLayoutUtils.hideOrRestoreRow(bearingLabel, !showRangeAndBearing);
    }

    @Override
    protected void updateGeometryControls()
    {
        updatingPanel = true;

        widthUnitsComboBox.setSelectedItem(rangeUnits);
        heightUnitsComboBox.setSelectedItem(rangeUnits);
        rangeUnitsComboBox.setSelectedItem(rangeUnits);
        radiusUnitsComboBox.setSelectedItem(rangeUnits);
        verticalHeightUnitsComboBox.setSelectedItem(altitudeUnits);

        bearingUnitsLabel.setText(bearingUnits.getAbbreviation() + northReference.getAbbrev());

        updatingPanel = false;
    }

    /**
     * North reference changed, update UI controls.
     * @since 5.0
     */
    private void onNorthReferenceChange(NorthReference northReference)
    {
        priorNorthReference = this.northReference;
        this.northReference = northReference;
        final var angle = bearingUnits.getAngle();

        // Convert our current bearing value to degrees and get range in meters
        final double currentValue = (double) bearingSpinner.getValue();
        final double degrees = AngleUtilities.convert(currentValue, angle, Angle.DEGREE);
        final double rangeMeters = rangeUnitsComboBox.toMeters();

        // Calculate the new bearing value
        final double newDegrees = NorthReference.convert(degrees, target.getLocation(), rangeMeters, priorNorthReference, northReference);
        final double newBearing = AngleUtilities.convert(newDegrees, Angle.DEGREE, angle);

        bearingUnitsLabel.setText(angle.getAbbrev() + northReference.getAbbrev());

        updatingPanel = true;
        final boolean wasDirty = bearingSpinnerDirty;
        bearingSpinner.setValue(newBearing);
        bearingSpinnerDirty = wasDirty;
        updatingPanel = false;
    }

    /**
     * Bearing units changed, update UI controls.
     * @since 5.0
     */
    private void onBearingUnitsChange(BearingUnits bearingUnits)
    {
        priorBearingUnits = this.bearingUnits;
        this.bearingUnits = bearingUnits;

        bearingUnitsLabel.setText(this.bearingUnits.getAbbreviation() + northReference.getAbbrev());

        final double currentBearing = (double) bearingSpinner.getValue();
        final double newBearing = AngleUtilities.convert(currentBearing, priorBearingUnits.getAngle(), this.bearingUnits.getAngle());
        updateBearingSpinnerModel(newBearing);
    }

    /**
     * Update the model for the bearing spinner according to the current bearing units.
     *
     * @param bearing Bearing in current bearing units
     * @since 5.0
     */
    private void updateBearingSpinnerModel(double bearing)
    {
        final boolean wasUpdatingPanel = updatingPanel;
        final boolean wasDirty = bearingSpinnerDirty;

        updatingPanel = true;

        final var model = switch (bearingUnits)
        {
            case DEGREE -> new SpinnerNumberModel(bearing, 0d, 360, 10);
            case MIL -> new SpinnerNumberModel(bearing, 0d, 6400, 100);
            case RADIAN -> new SpinnerNumberModel(bearing, 0d, 2 * Math.PI, .1);
            default ->
            {
                logger.error("Unsupported bearing units: {}", bearingUnits);
                yield null;
            }
        };

        if (model != null) bearingSpinner.setModel(model);

        bearingSpinnerDirty = wasDirty;
        updatingPanel = wasUpdatingPanel;
    }

    @Override
    public void releaseResources()
    {
        if (target != null) target.removePropertyChangeListener(shapePropertyList, this);
        attributesPanel.releaseResources();
        tacticalGraphicsEditorPanel.releaseResources();
        northReferenceMonitor.dispose();
        bearingUnitsMonitor.dispose();

        super.releaseResources();
    }

    private void setShape(IMutableShape shape)
    {
        logger.debug("Set shape {}", shape);

        updatingPanel = true;

        target = shape;
        attributesPanel.setShape(shape);
        txtName.setText(shape.getName());
        fillColorChooser.setColor(new Color(shape.getFillColor().getRGB(), false));
        borderColorChooser.setColor(new Color(shape.getLineColor().getRGB(), false));
        opacitySpinner.setValue(shape.getOpacity());
        borderWidthSpinner.setValue(shape.getLineWidth());
        publishedCheckBox.setSelected(shape.isPublished());
        editableCheckBox.setSelected(shape.isEditable());
        orientableCheckbox.setSelected(shape.isOrientable());

        if (target instanceof IRegularShape regularShape)
        {
            orientationSpinner.setValue(regularShape.getOrientation());
        }

        if (target instanceof IShapeMapObject<?> shapeMapObject)
        {
            targetAttributeSet = shapeMapObject.getAttributeSet();
            metadataPanel.setMetadata(targetAttributeSet);
        }

        final boolean canUseVerticalHeight = shape.canUseVerticalHeight();

        if (canUseVerticalHeight)
        {
            verticalHeightSpinner.setValue(shape.getVerticalHeight() * altitudeUnits.getMultiplier());
            boolean verticalHeightEnabled = shape.isVerticalHeightEnabled();
            verticalHeightEnabled(verticalHeightEnabled);

            enableHeightCheckBox.setSelected(verticalHeightEnabled);
        }

        if (shape instanceof IMutableSquare square)
        {
            widthUnitsComboBox.fromMeters(square.getSideLength());
        } else if (shape instanceof IMutableRectangle rectangle)
        {
            widthUnitsComboBox.fromMeters(rectangle.getWidth());
            heightUnitsComboBox.fromMeters(rectangle.getHeight());
        } else if (supportsRadius())
        {
            radiusUnitsComboBox.fromMeters(getRadius());
        } else if (shape instanceof IMutableArrow arrow)
        {
            // Bearing from the shape is always in degrees true
            final double degrees = NorthReference.convert(arrow.getBearing(), arrow.getLocation(), arrow.getRange(), NorthReference.TRUE, northReference);
            final double bearing = AngleUtilities.convert(degrees, Angle.DEGREE, bearingUnits.getAngle());
            bearingSpinner.setValue(bearing);
            updateBearingSpinnerModel(bearing);

            rangeUnitsComboBox.fromMeters(arrow.getRange());
        }

        if (shape instanceof IRingedShape ringedShape) ringCountSpinner.setValue(ringedShape.getRingCount());

        widthSpinnerDirty = heightSpinnerDirty = radiusSpinnerDirty = bearingSpinnerDirty = rangeSpinnerDirty = false;

        enableEditableControls(shape.isEditable());

        revalidate();

        updatingPanel = false;

        shape.addPropertyChangeListener(shapePropertyList, this);
    }

    private boolean supportsRadius()
    {
        return toolType == MapToolType.DRAWING_CIRCLE;
    }

    private double getRadius()
    {
        return target instanceof IMutableCircle circle ? circle.getRadius() : 0;
    }

    private void setRadius(double radius)
    {
        if (target instanceof IMutableCircle circle) circle.setRadius(radius);
    }

    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void finalizeShape(FinalizeShapeEvent event)
    {
        if (initialCreationOnly)
        {
            ignoreEvent = true;

            target.setName(txtName.getText());

            // Initially, the shape had to be editable to allow it to actually be drawn.
            // Now that the user is done drawing it, if was supposed to be non-editable, make it so.
            if (!editableCheckBox.isSelected()) target.setEditable(false);

            ignoreEvent = false;
        }
    }

    @Override
    protected void updateTargetProperties()
    {
        metadataPanel.commit();

        // Exclude Height, width, and radius spinners, getValueFromSpinner will commit them (with accuracy retained)
        commitSpinners(orientationSpinner, borderWidthSpinner, opacitySpinner, verticalHeightSpinner);

        // If we're creating a new shape, we must leave the shape as editable until the user is done drawing it,
        // or they won't be able to actually draw the shape.
        if (!initialCreationOnly) target.setEditable(editableCheckBox.isSelected());

        // We ony show the name field when in direct action mode, so don't update the target in that case
        if (directActionMode) target.setName(txtName.getText().trim());

        target.setFillColor(makeColorWithOpacity(fillColorChooser.getColor())); // Opacity is derived from fill color
        target.setLineColor(makeColorWithOpacity(borderColorChooser.getColor()));
        target.setLineWidth((double) borderWidthSpinner.getValue());
        target.setPublished(publishedCheckBox.isSelected());
        target.setOrientable(orientableCheckbox.isSelected());

        updateShapeSpecificProperties();
        updateMetadata();
        updateTacticalGraphicsProperties();

        widthSpinnerDirty = heightSpinnerDirty = radiusSpinnerDirty = bearingSpinnerDirty = rangeSpinnerDirty = false;
    }

    /**
     * Apply the UI values to shape properties, according to the type of the shape.
     * @since 3.3
     */
    private void updateShapeSpecificProperties()
    {
        if (target instanceof IMutableRegularShape regularShape)
        {
            regularShape.setOrientation((double) orientationSpinner.getValue());
        }

        if (supportsRadius())
        {
            updateFromLengthSpinner(this::getRadius, this::setRadius, radiusSpinner, radiusUnitsComboBox, radiusSpinnerDirty);
        }

        if (target instanceof IMutableRingedShape ringedShape) ringedShape.setRingCount((int) ringCountSpinner.getValue());

        if (target instanceof IMutableSquare square)
        {
            updateFromLengthSpinner(square::getSideLength, square::setSideLength, widthSpinner, widthUnitsComboBox, widthSpinnerDirty);
        } else if (target instanceof IMutableRectangle rectangle)
        {
            updateFromLengthSpinner(rectangle::getHeight, rectangle::setHeight, heightSpinner, heightUnitsComboBox, heightSpinnerDirty);
            updateFromLengthSpinner(rectangle::getWidth, rectangle::setWidth, widthSpinner, widthUnitsComboBox, widthSpinnerDirty);
        } else if (target instanceof IMutableArrow arrow)
        {
            updateFromLengthSpinner(arrow::getRange, arrow::setRange, rangeSpinner, rangeUnitsComboBox, rangeSpinnerDirty);

            final double valueFromShape = arrow.getBearing();
            final double valueFromSpinner = getValueFromSpinner(bearingSpinner, bearingSpinnerDirty, valueFromShape,
                    spinner -> {
                        // The value in the spinner has been converted from degrees True to the current north reference.
                        // Here we need to convert it back to degrees True before storing into the shape.
                        final double degrees = getUnscaledBearingFromSpinner(spinner);
                        return NorthReference.convert(degrees, arrow.getLocation(), arrow.getRange(), northReference, NorthReference.TRUE);
                    });

            if (valueFromShape != valueFromSpinner) arrow.setBearing(valueFromSpinner);
        }

        if (target.canUseVerticalHeight())
        {
            final boolean enabled = enableHeightCheckBox.isSelected();
            target.setVerticalHeightEnabled(enabled);
            if (enabled)
            {
                target.setVerticalHeight(verticalHeightUnitsComboBox.toMeters());
            }
        }
    }

    private void updateMetadata()
    {
        if (target instanceof IShapeMapObject<?> shapeMapObject)
        {
            final Set<String> changedMetadataKeys = metadataPanel.getChangedMetadataKeys();

            if (!changedMetadataKeys.isEmpty())
            {
                final AttributeSet oldMetadata = metadataPanel.getOriginalMetadata();
                final AttributeSet newMetadata = metadataPanel.getMetadata();

                for (String key : changedMetadataKeys)
                {
                    final String oldValue = oldMetadata.getStringAttribute(key);
                    final String newValue = newMetadata.getStringAttribute(key);

                    targetAttributeSet.setAttribute(key, newValue);
                    surfaceStructureService.notifyListenersOfMetaDataChange(shapeMapObject, key, oldValue, newValue);
                }
            }
        }
    }

    /**
     * Updates the tactical graphics properties when the user applies changes made in the config dialog.
     *
     * @since 5.1
     */
    private void updateTacticalGraphicsProperties()
    {
        if (target != null && target.getShapeMapObject() != null)
        {
            if (tacticalGraphicsEditorPanel.isVisible()) tacticalGraphicsEditorPanel.commit();
        }
    }

    /**
     * Apply the value from the given spinner to the shape, if it has been modified.  This method specifically handles
     * spinners whose values are treated as "length" values (meaning they are scaled by the current units format length
     * multiplier).
     *
     * @param getter Supplier to retrieve the current value from the shape
     * @param updater Consumer to update the property on the shape
     * @param spinner Spinner to process
     * @param spinnerDirty Has the spinner value been modified by the user
     * @since 3.3 (renamed in 5.0)
     */
    private void updateFromLengthSpinner(DoubleSupplier getter, DoubleConsumer updater, JSpinner spinner, UnitOfMeasureComboBox unitOfMeasureComboBox, boolean spinnerDirty)
    {
        final double shapeValue = getter.getAsDouble();
        final double valueFromSpinner = getValueFromSpinner(spinner, spinnerDirty, shapeValue, s -> unitOfMeasureComboBox.toMeters());
        if (shapeValue != valueFromSpinner) updater.accept(valueFromSpinner);
    }

    /**
     * if the user hasn't modified the height or width values, we want to grab the height/width
     * values from the shape itself. Otherwise, the precision of the height/width will be truncated
     * from 14 decimal places down to 3 decimal places. This loss of precision affects the area and
     * perimeter values of the shape.
     *
     * @param spinner         Spinner to process
     * @param spinnerDirty    Has the spinner value been modified by the user
     * @param shapeValue      current value from the shape
     * @param unscaleFunction Function to "unscale" (convert to base units) the current spinner value
     * @return value from the spinner, if modified, or the given {@code shapeValue}
     * @since 3.3 (signature changed in 5.0)
     */
    private double getValueFromSpinner(JSpinner spinner, boolean spinnerDirty, double shapeValue, Function<JSpinner, Double> unscaleFunction)
    {
        final DecimalFormat df = new DecimalFormat("#.###");

        final double valueBeforeCommit = Double.parseDouble(df.format(spinner.getValue()));
        ComponentUtils.commitSpinner(spinner);
        final double valueAfterCommit = Double.parseDouble(df.format(spinner.getValue()));

        final boolean userModified = spinnerDirty || (valueBeforeCommit != valueAfterCommit);

        final double valueToUse = userModified
                ? unscaleFunction.apply(spinner)
                : shapeValue;

        logger.debug("Using {} value {}", userModified ? "user modified" : "original", valueToUse);

        return valueToUse;
    }

    @InvokeOnEDT
    @Override
    public void propertyChange(final PropertyChangeEvent evt)
    {
        final String propertyName = evt.getPropertyName();
        final Object value = evt.getNewValue();

        logger.debug("property change: ignore {} : {}={}", ignoreEvent, propertyName, value);

        if (ignoreEvent) return;

        updatingPanel = true;  // Avoid cascading events

        switch (propertyName)
        {
            // Bearing from the shape is always in Degrees
            case ShapeProperties.BEARING -> bearingSpinner.setValue(AngleUtilities.convert((double) value, Angle.DEGREE, bearingUnits.getAngle()));
            case ShapeProperties.EDITABLE ->
            {
                final boolean editable = (boolean) value;
                editableCheckBox.setSelected(editable);
                enableEditableControls(editable);
            }
            case ShapeProperties.FILL_COLOR ->
            {
                fillColorChooser.setColor(new Color((int) value, false));
                opacitySpinner.setValue((target.getOpacity()));
            }
            case ShapeProperties.HEIGHT -> heightUnitsComboBox.fromMeters((double) value);
            case ShapeProperties.LINE_COLOR -> borderColorChooser.setColor(new Color((int) value, false));
            case ShapeProperties.LINE_WIDTH -> borderWidthSpinner.setValue(value);
            case ShapeProperties.LOCATION -> logger.debug("LOCATION - will update attributesPanel");
            case ShapeProperties.NAME -> txtName.setText((String) value);
            case ShapeProperties.OPACITY -> opacitySpinner.setValue(value);
            case ShapeProperties.ORIENTABLE ->
            {
                final boolean orientable = (boolean) value;
                orientableCheckbox.setSelected(orientable);
                orientationSpinner.setEnabled(orientable);
            }
            case ShapeProperties.ORIENTATION -> orientationSpinner.setValue(value);
            case ShapeProperties.PUBLISHED -> publishedCheckBox.setSelected((boolean) value);
            case ShapeProperties.RADIUS -> radiusUnitsComboBox.fromMeters((double) value);
            case ShapeProperties.RANGE -> rangeUnitsComboBox.fromMeters((double) value);
            case ShapeProperties.RING_COUNT -> ringCountSpinner.setValue(value);
            case ShapeProperties.SEMI_MAJOR_AXIS -> logger.debug("ignore SEMI_MAJOR_AXIS");
            case ShapeProperties.SEMI_MINOR_AXIS -> logger.debug("ignore SEMI_MINOR_AXIS");
            case ShapeProperties.VERTICAL_HEIGHT -> verticalHeightUnitsComboBox.fromMeters((double) value);
            case ShapeProperties.VERTICAL_HEIGHT_ENABLED ->
            {
                var selected = (boolean) value;
                enableHeightCheckBox.setSelected(selected);
                verticalHeightEnabled(selected);
            }
            case ShapeProperties.WIDTH -> widthUnitsComboBox.fromMeters((double) value);
            case IMapObject.ATTRIBUTE_SET -> metadataPanel.setMetadata((AttributeSet) value);
            default -> logger.warn("Received unexpected property change event: {}", evt);
        }

        attributesPanel.refresh();

        updatingPanel = false;
    }

    private void setFillColor(Color color)
    {
        updateProperty(() -> target.setFillColor(makeColorWithOpacity(color)));
    }

    private void setLineColor(Color color)
    {
        updateProperty(() -> target.setLineColor(makeColorWithOpacity(color)));
    }

    /**
     * @return a Color object with the given base color and the current opacity spinner value as the alpha channel
     * @since 3.3
     */
    private Color makeColorWithOpacity(Color color)
    {
        final int alpha = (int) ((int) opacitySpinner.getValue() / 100d * 255);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    @InvokeOnEDT
    private void enableEditableControls(boolean enable)
    {
        metadataPanel.setEditable(enable);
        editorPanel.setEnabled(enable);
        tacticalGraphicsEditorPanel.setEnabled(enable);

        // Start by enabling/disabling all the components, as indicated
        for (Component component : editorPanel.getComponents()) component.setEnabled(enable);

        // Then adjust certain elements based on the features supported by the various shape types
        final boolean enableVerticalHeight = enable && enableHeightCheckBox.isSelected();
        verticalHeightEnabled(enableVerticalHeight);
        orientationSpinner.setEnabled(enable && target instanceof IRegularShape && orientableCheckbox.isSelected());
        orientableCheckbox.setEnabled(enable);
    }

    /**
     * @return Spinner value converted to degrees true
     * @since 5.0
     */
    private double getUnscaledBearingFromSpinner(JSpinner spinner)
    {
        return AngleUtilities.convert((double) spinner.getValue(), bearingUnits.getAngle(), Angle.DEGREE);
    }

    private void metadataValueChanged(PropertyChangeEvent pce)
    {
        if (targetAttributeSet != null)
        {
            updateProperty(() -> targetAttributeSet.setAttribute(pce.getPropertyName(), (String) pce.getNewValue()));
        }
    }

    /**
     * Respond to tactical graphics modifier changes by updating the map object or setting the panel dirty.
     *
     * @param pce The change event
     * @since 5.1
     */
    private void tacticalGraphicsValueChanged(PropertyChangeEvent pce)
    {
        if (!updatingPanel && target != null)
        {
            IShapeMapObject<?> shapeMapObject = target.getShapeMapObject();
            if (shapeMapObject != null)
            {
                if (directActionMode)
                {
                    final String propertyName = pce.getPropertyName();

                    if (!propertyName.equals(StandardAttributeNames.MIL_SYM)) // ignore symbol changes
                    {
                        final String modifierValue = (String) pce.getNewValue();

                        milStd2525ServiceProvider.get2525ServiceForMapObject(shapeMapObject)
                                .setModifier(shapeMapObject, propertyName, modifierValue);
                    }
                } else
                {
                    setPanelDirty();
                }
            }
        }
    }

    private void editableCheckBoxActionPerformed()
    {
        final boolean editable = editableCheckBox.isSelected();
        enableEditableControls(editable);
        updateProperty(() -> target.setEditable(editable));
    }

    private void enableHeightCheckBoxActionPerformed()
    {
        final boolean verticalHeightEnabled = enableHeightCheckBox.isSelected();
        verticalHeightEnabled(verticalHeightEnabled);
        updateProperty(() -> target.setVerticalHeightEnabled(enableHeightCheckBox.isSelected()));
        attributesPanel.refresh();
    }

    private void verticalHeightSpinnerStateChanged()
    {
        final double verticalHeight = verticalHeightUnitsComboBox.toMeters();
        updateProperty(() -> target.setVerticalHeight(verticalHeight));
        attributesPanel.refresh();
    }

    private void widthSpinnerStateChanged()
    {
        widthSpinnerDirty = true;

        updateProperty(() -> {
            final double width = widthUnitsComboBox.toMeters();

            if (toolType == MapToolType.DRAWING_QUAD)
            {
                ((IMutableRectangle) target).setWidth(width);
            } else
            {
                ((IMutableSquare) target).setSideLength(width);
            }
        });
        attributesPanel.refresh();
    }

    private void heightSpinnerStateChanged()
    {
        heightSpinnerDirty = true;

        final double height = heightUnitsComboBox.toMeters();
        updateProperty(() -> ((IMutableRectangle) target).setHeight(height));
        attributesPanel.refresh();
    }

    private void radiusSpinnerStateChanged()
    {
        radiusSpinnerDirty = true;

        final double radius = radiusUnitsComboBox.toMeters();
        updateProperty(() -> setRadius(radius));
        attributesPanel.refresh();
    }

    private void rangeSpinnerStateChanged()
    {
        rangeSpinnerDirty = true;

        final double range = rangeUnitsComboBox.toMeters();
        updateProperty(() -> ((IMutableArrow) target).setRange(range));
        attributesPanel.refresh();
    }

    private void bearingSpinnerStateChanged()
    {
        bearingSpinnerDirty = true;

        updateProperty(() -> ((IMutableArrow) target).setBearing(getUnscaledBearingFromSpinner(bearingSpinner)));
        attributesPanel.refresh();
    }

    private void orientableCheckboxActionPerformed()
    {
        final boolean orientable = orientableCheckbox.isSelected();
        orientationSpinner.setEnabled(orientable);
        updateProperty(() -> target.setOrientable(orientable));
    }

    private void orientationSpinnerStateChanged()
    {
        updateProperty(() -> ((IMutableRegularShape) target).setOrientation((double) orientationSpinner.getValue()));
        attributesPanel.refresh();
    }

    private void borderWidthSpinnerStateChanged()
    {
        updateProperty(() -> target.setLineWidth((double) borderWidthSpinner.getValue()));
    }

    private void opacitySpinnerStateChanged()
    {
        updateProperty(() -> target.setOpacity((int) opacitySpinner.getValue()));
    }

    private void geofenceActionPerformed()
    {
        updateProperty(() -> target.setPublished(publishedCheckBox.isSelected()));
    }

    private void txtNameChanged()
    {
        updateProperty(() -> target.setName(txtName.getText()));
    }

    private void ringCountSpinnerStateChanged()
    {
        updateProperty(() -> ((IMutableRingedShape) target).setRingCount((int) ringCountSpinner.getValue()));
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var masterPanel = new JPanel();
        editorPanel = new JPanel();
        nameLabel = new JLabel();
        txtName = new JTextField();
        fillColorLabel = new JLabel();
        fillColorChooser = new ColorChooser();
        borderColorLabel = new JLabel();
        borderColorChooser = new ColorChooser();
        borderWidthLabel = new JLabel();
        borderWidthSpinner = new JSpinner();
        var label13 = new JLabel();
        opacitySpinner = new JSpinner();
        orientationLabel = new JLabel();
        orientationSpinner = new JSpinner();
        heightLabel = new JLabel();
        heightSpinner = new JSpinner();
        heightUnitsComboBox = new UnitOfMeasureComboBox();
        widthLabel = new JLabel();
        widthSpinner = new JSpinner();
        widthUnitsComboBox = new UnitOfMeasureComboBox();
        radiusLabel = new JLabel();
        radiusSpinner = new JSpinner();
        radiusUnitsComboBox = new UnitOfMeasureComboBox();
        rangeLabel = new JLabel();
        rangeSpinner = new JSpinner();
        rangeUnitsComboBox = new UnitOfMeasureComboBox();
        bearingLabel = new JLabel();
        bearingSpinner = new JSpinner();
        bearingUnitsLabel = new JLabel();
        ringCountLabel = new JLabel();
        ringCountSpinner = new JSpinner();
        enableHeightCheckBox = new JCheckBox();
        verticalHeightSpinner = new JSpinner();
        verticalHeightUnitsComboBox = new UnitOfMeasureComboBox();
        var panel22 = new JPanel();
        var separator1 = new JSeparator();
        var panel23 = new JPanel();
        editableCheckBox = new JCheckBox();
        orientableCheckbox = new JCheckBox();
        publishedCheckBox = new JCheckBox();
        var separator3 = new JSeparator();
        attributesPanelHolder = new JPanel();
        metadataPanelHolder = new JPanel();
        tacticalGraphicsPanelHolder = new JPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "[130dlu,default]:grow",
                "4*(default, $lgap), default"));

        //======== masterPanel ========
        {
            masterPanel.setName("masterPanel");
            masterPanel.setLayout(new FormLayout(
                    "default",
                    "default, 9dlu, default"));

            //======== editorPanel ========
            {
                editorPanel.setName("editorPanel");
                editorPanel.setLayout(new FormLayout(
                        "default, $lcgap, 70dlu, $lcgap, left:32dlu",
                        "12*(default, $lgap), default"));

                //---- nameLabel ----
                nameLabel.setText("Name:");
                nameLabel.setName("nameLabel");
                editorPanel.add(nameLabel, cc.xy(1, 1));

                //---- txtName ----
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
                editorPanel.add(txtName, cc.xy(3, 1));

                //---- fillColorLabel ----
                fillColorLabel.setText("Fill Color:");
                fillColorLabel.setName("fillColorLabel");
                editorPanel.add(fillColorLabel, cc.xy(1, 3));

                //---- fillColorChooser ----
                fillColorChooser.setColor(Color.cyan);
                fillColorChooser.setName("fillColorChooser");
                editorPanel.add(fillColorChooser, cc.xy(3, 3));

                //---- borderColorLabel ----
                borderColorLabel.setText("Border Color:");
                borderColorLabel.setName("borderColorLabel");
                editorPanel.add(borderColorLabel, cc.xy(1, 5));

                //---- borderColorChooser ----
                borderColorChooser.setName("borderColorChooser");
                editorPanel.add(borderColorChooser, cc.xy(3, 5));

                //---- borderWidthLabel ----
                borderWidthLabel.setText("Border Width:");
                borderWidthLabel.setName("borderWidthLabel");
                editorPanel.add(borderWidthLabel, cc.xy(1, 7));

                //---- borderWidthSpinner ----
                borderWidthSpinner.setModel(new SpinnerNumberModel(3.0, 1.0, 7.0, 0.25));
                borderWidthSpinner.setName("borderWidthSpinner");
                borderWidthSpinner.addChangeListener(e -> borderWidthSpinnerStateChanged());
                editorPanel.add(borderWidthSpinner, cc.xy(3, 7));

                //---- label13 ----
                label13.setText("Opacity:");
                label13.setName("label13");
                editorPanel.add(label13, cc.xy(1, 9));

                //---- opacitySpinner ----
                opacitySpinner.setModel(new SpinnerNumberModel(70, 0, 100, 1));
                opacitySpinner.setName("opacitySpinner");
                opacitySpinner.addChangeListener(e -> opacitySpinnerStateChanged());
                editorPanel.add(opacitySpinner, cc.xy(3, 9));

                //---- orientationLabel ----
                orientationLabel.setText("Orientation:");
                orientationLabel.setName("orientationLabel");
                editorPanel.add(orientationLabel, cc.xy(1, 11));

                //---- orientationSpinner ----
                orientationSpinner.setModel(new SpinnerNumberModel(0.0, 0.0, 360.0, 0.5));
                orientationSpinner.setName("widthSpinner");
                orientationSpinner.addChangeListener(e -> orientationSpinnerStateChanged());
                editorPanel.add(orientationSpinner, cc.xy(3, 11));

                //---- heightLabel ----
                heightLabel.setText("Height:");
                heightLabel.setName("heightLabel");
                editorPanel.add(heightLabel, cc.xy(1, 13));

                //---- heightSpinner ----
                heightSpinner.setModel(new SpinnerNumberModel(1.0, 1.0E-7, null, 1.0));
                heightSpinner.setName("heightSpinner");
                heightSpinner.addChangeListener(e -> heightSpinnerStateChanged());
                editorPanel.add(heightSpinner, cc.xy(3, 13));

                //---- heightUnitsComboBox ----
                heightUnitsComboBox.setName("heightUnitsComboBox");
                editorPanel.add(heightUnitsComboBox, cc.xy(5, 13));

                //---- widthLabel ----
                widthLabel.setText("Width:");
                widthLabel.setName("widthLabel");
                editorPanel.add(widthLabel, cc.xy(1, 15));

                //---- widthSpinner ----
                widthSpinner.setModel(new SpinnerNumberModel(1.0, 0.0, null, 1.0));
                widthSpinner.setName("widthSpinner");
                widthSpinner.addChangeListener(e -> widthSpinnerStateChanged());
                editorPanel.add(widthSpinner, cc.xy(3, 15));

                //---- widthUnitsComboBox ----
                widthUnitsComboBox.setName("widthUnitsComboBox");
                editorPanel.add(widthUnitsComboBox, cc.xy(5, 15));

                //---- radiusLabel ----
                radiusLabel.setText("Radius:");
                radiusLabel.setName("radiusLabel");
                editorPanel.add(radiusLabel, cc.xy(1, 17));

                //---- radiusSpinner ----
                radiusSpinner.setModel(new SpinnerNumberModel(1.0, 1.0E-7, null, 1.0));
                radiusSpinner.setName("radiusSpinner");
                radiusSpinner.addChangeListener(e -> radiusSpinnerStateChanged());
                editorPanel.add(radiusSpinner, cc.xy(3, 17));

                //---- radiusUnitsComboBox ----
                radiusUnitsComboBox.setName("radiusUnitsComboBox");
                editorPanel.add(radiusUnitsComboBox, cc.xy(5, 17));

                //---- rangeLabel ----
                rangeLabel.setText("Range:");
                rangeLabel.setName("rangeLabel");
                editorPanel.add(rangeLabel, cc.xy(1, 19));

                //---- rangeSpinner ----
                rangeSpinner.setModel(new SpinnerNumberModel(1.0, 1.0E-7, null, 1.0));
                rangeSpinner.setName("rangeSpinner");
                rangeSpinner.addChangeListener(e -> rangeSpinnerStateChanged());
                editorPanel.add(rangeSpinner, cc.xy(3, 19));

                //---- rangeUnitsComboBox ----
                rangeUnitsComboBox.setName("rangeUnitsComboBox");
                editorPanel.add(rangeUnitsComboBox, cc.xy(5, 19));

                //---- bearingLabel ----
                bearingLabel.setText("Bearing:");
                bearingLabel.setName("bearingLabel");
                editorPanel.add(bearingLabel, cc.xy(1, 21));

                //---- bearingSpinner ----
                bearingSpinner.setModel(new SpinnerNumberModel(0.0, 0.0, 360.0, 0.5));
                bearingSpinner.setName("widthSpinner");
                bearingSpinner.addChangeListener(e -> bearingSpinnerStateChanged());
                editorPanel.add(bearingSpinner, cc.xy(3, 21));

                //---- bearingUnitsLabel ----
                bearingUnitsLabel.setText("T");
                bearingUnitsLabel.setName("bearingUnitsLabel");
                editorPanel.add(bearingUnitsLabel, cc.xy(5, 21));

                //---- ringCountLabel ----
                ringCountLabel.setText("Ring Count:");
                ringCountLabel.setName("ringCountLabel");
                editorPanel.add(ringCountLabel, cc.xy(1, 23));

                //---- ringCountSpinner ----
                ringCountSpinner.setModel(new SpinnerNumberModel(1, 1, 25, 1));
                ringCountSpinner.setName("ringCountSpinner");
                ringCountSpinner.addChangeListener(e -> ringCountSpinnerStateChanged());
                editorPanel.add(ringCountSpinner, cc.xy(3, 23));

                //---- enableHeightCheckBox ----
                enableHeightCheckBox.setText("Vertical Height");
                enableHeightCheckBox.setName("enableHeightCheckBox");
                enableHeightCheckBox.addActionListener(e -> enableHeightCheckBoxActionPerformed());
                editorPanel.add(enableHeightCheckBox, cc.xy(1, 25));

                //---- verticalHeightSpinner ----
                verticalHeightSpinner.setModel(new SpinnerNumberModel(10.0, 0.01, 1000000.0, 1.0));
                verticalHeightSpinner.setName("verticalHeightSpinner");
                verticalHeightSpinner.addChangeListener(e -> verticalHeightSpinnerStateChanged());
                editorPanel.add(verticalHeightSpinner, cc.xy(3, 25));

                //---- verticalHeightUnitsComboBox ----
                verticalHeightUnitsComboBox.setName("verticalHeightUnitsComboBox");
                editorPanel.add(verticalHeightUnitsComboBox, cc.xy(5, 25));
            }
            masterPanel.add(editorPanel, cc.xy(1, 1));

            //======== panel22 ========
            {
                panel22.setName("panel22");
                panel22.setLayout(new FormLayout(
                        "default:grow",
                        "8dlu"));

                //---- separator1 ----
                separator1.setName("separator1");
                panel22.add(separator1, cc.xy(1, 1));
            }
            masterPanel.add(panel22, cc.xy(1, 2));

            //======== panel23 ========
            {
                panel23.setName("panel23");
                panel23.setLayout(new FormLayout(
                        "default, $ugap, default",
                        "default, $nlgap, default"));

                //---- editableCheckBox ----
                editableCheckBox.setText("Editable");
                editableCheckBox.setSelected(true);
                editableCheckBox.setName("editableCheckBox");
                editableCheckBox.addActionListener(e -> editableCheckBoxActionPerformed());
                panel23.add(editableCheckBox, cc.xy(1, 1));

                //---- orientableCheckbox ----
                orientableCheckbox.setText("Orientable");
                orientableCheckbox.setSelected(true);
                orientableCheckbox.setName("orientableCheckbox");
                orientableCheckbox.addActionListener(e -> orientableCheckboxActionPerformed());
                panel23.add(orientableCheckbox, cc.xy(3, 1));

                //---- publishedCheckBox ----
                publishedCheckBox.setText("Published");
                publishedCheckBox.setName("publishedCheckBox");
                publishedCheckBox.addActionListener(e -> geofenceActionPerformed());
                panel23.add(publishedCheckBox, cc.xy(1, 3));
            }
            masterPanel.add(panel23, cc.xy(1, 3));
        }
        add(masterPanel, cc.xy(1, 1));

        //---- separator3 ----
        separator3.setName("separator3");
        add(separator3, cc.xy(1, 3));

        //======== attributesPanelHolder ========
        {
            attributesPanelHolder.setName("attributesPanelHolder");
            attributesPanelHolder.setLayout(new FormLayout(
                    "default",
                    "default"));
        }
        add(attributesPanelHolder, cc.xy(1, 7));

        //======== metadataPanelHolder ========
        {
            metadataPanelHolder.setName("metadataPanelHolder");
            metadataPanelHolder.setLayout(new FormLayout(
                    "default",
                    "default"));
        }
        add(metadataPanelHolder, cc.xy(1, 9));

        //======== tacticalGraphicsPanelHolder ========
        {
            tacticalGraphicsPanelHolder.setName("tacticalGraphicsPanelHolder");
            tacticalGraphicsPanelHolder.setLayout(new FormLayout(
                    "default:grow",
                    "default"));
        }
        add(tacticalGraphicsPanelHolder, cc.xy(1, 5));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel editorPanel;
    private JLabel nameLabel;
    private JTextField txtName;
    private JLabel fillColorLabel;
    private ColorChooser fillColorChooser;
    private JLabel borderColorLabel;
    private ColorChooser borderColorChooser;
    private JLabel borderWidthLabel;
    private JSpinner borderWidthSpinner;
    private JSpinner opacitySpinner;
    private JLabel orientationLabel;
    private JSpinner orientationSpinner;
    private JLabel heightLabel;
    private JSpinner heightSpinner;
    private UnitOfMeasureComboBox heightUnitsComboBox;
    private JLabel widthLabel;
    private JSpinner widthSpinner;
    private UnitOfMeasureComboBox widthUnitsComboBox;
    private JLabel radiusLabel;
    private JSpinner radiusSpinner;
    private UnitOfMeasureComboBox radiusUnitsComboBox;
    private JLabel rangeLabel;
    private JSpinner rangeSpinner;
    private UnitOfMeasureComboBox rangeUnitsComboBox;
    private JLabel bearingLabel;
    private JSpinner bearingSpinner;
    private JLabel bearingUnitsLabel;
    private JLabel ringCountLabel;
    private JSpinner ringCountSpinner;
    private JCheckBox enableHeightCheckBox;
    private JSpinner verticalHeightSpinner;
    private UnitOfMeasureComboBox verticalHeightUnitsComboBox;
    private JCheckBox editableCheckBox;
    private JCheckBox orientableCheckbox;
    private JCheckBox publishedCheckBox;
    private JPanel attributesPanelHolder;
    private JPanel metadataPanelHolder;
    private JPanel tacticalGraphicsPanelHolder;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
