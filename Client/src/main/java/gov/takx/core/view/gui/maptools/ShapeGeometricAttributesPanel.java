package gov.takx.core.view.gui.maptools;

import com.atakmap.coremap.maps.coords.NorthReference;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.measure.AreaMeasurer;
import gov.nasa.worldwind.util.measure.LengthMeasurer;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.shapes.IArrow;
import gov.takx.api.shapes.ICircle;
import gov.takx.api.shapes.IEllipse;
import gov.takx.api.shapes.IIrregularShape;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.api.shapes.IPolygon;
import gov.takx.api.shapes.IPolyline;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.shapes.IRegularShape;
import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.ISquare;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.lang.StackWalkerUtils;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.WWUtils;
import gov.takx.platform.utils.units.AltitudeUnits;
import gov.takx.platform.utils.units.BearingUnits;
import gov.takx.platform.utils.units.RangeUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import java.awt.Color;
import java.awt.Insets;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

/**
 */
@PseudoBean
public class ShapeGeometricAttributesPanel extends JPanel implements IResourceCleaner
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final CellConstraints CC = new CellConstraints();

    @Inject
    private IExtendedGlobeManager globeManager;

    private final IPreferenceMonitor<AltitudeUnits> altitudeUnitsMonitor = UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS.newMonitor(AltitudeUnits.class, this::altitudeUnitsChanged);
    private final IPreferenceMonitor<RangeUnits> rangeUnitsMonitor = UserPreferenceEntry.DISPLAY_RANGE_UNITS.newMonitor(RangeUnits.class, this::rangeUnitsChanged);
    private final IPreferenceMonitor<NorthReference> northReferenceMonitor =
            UserPreferenceEntry.DISPLAY_NORTH_REFERENCE.newMonitor(NorthReference.class, nr -> refresh());
    private final IPreferenceMonitor<BearingUnits> bearingUnitsMonitor =
            UserPreferenceEntry.DISPLAY_BEARING_UNIT.newMonitor(BearingUnits.class, b -> refresh());

    private final TerrainFollowingPathPanel terrainFollowingPathPanel = new TerrainFollowingPathPanel();
    private AltitudeUnits altitudeUnits = altitudeUnitsMonitor.getCurrentValue();
    private RangeUnits rangeUnits = rangeUnitsMonitor.getCurrentValue();
    private IMutableShape shape = null;
    private Globe wwGlobe;
    private AShapePanel<? extends IShape> shapeInfoPanel;

    public ShapeGeometricAttributesPanel()
    {
        setName("ShapeGeometricAttributesPanel");
        setLayout(new FormLayout("default:grow", "default, $rgap, fill:default:grow"));

        terrainFollowingPathPanel.setUnits(rangeUnits);
    }

    @PostConstruct
    private void postConstruct()
    {
        wwGlobe = globeManager.getPrimaryGlobeDelegate().getWorldWindow().getModel().getGlobe();
    }

    @InvokeOnEDT
    private void altitudeUnitsChanged(AltitudeUnits altitudeUnits)
    {
        this.altitudeUnits = altitudeUnits;

        refresh();
    }

    @InvokeOnEDT
    private void rangeUnitsChanged(RangeUnits rangeUnits)
    {
        this.rangeUnits = rangeUnits;
        terrainFollowingPathPanel.setUnits(rangeUnits);

        refresh();
    }

    public void setShape(@Nonnull IMutableShape shape)
    {
        this.shape = shape;

        if (logger.isTraceEnabled()) logger.trace("setShape: {}\n{}", shape, StackWalkerUtils.getStackTraceAsString());

        // Add notification to the user if anyone is monitoring this shape
        final int listenerCount = shape.getShapeListenerCount();
        if (listenerCount > 0)
        {
            final String text = "Monitored by " + (listenerCount > 1 ? "one plugin." : listenerCount + " plugins.");
            final JLabel messageLabel = new JLabel(text);
            messageLabel.setIcon(null);
            messageLabel.setIconTextGap(0);

            add(messageLabel, CC.rc(1, 1));
        }

        setupShapeInfo();
    }

    @Override
    public void releaseResources()
    {
        altitudeUnitsMonitor.dispose();
        rangeUnitsMonitor.dispose();
        northReferenceMonitor.dispose();
        bearingUnitsMonitor.dispose();
        shapeInfoPanel = null;
    }

    private void setupShapeInfo()
    {
        if (shapeInfoPanel != null) remove(shapeInfoPanel);

        shapeInfoPanel = switch (MapToolType.valueOfShapeType(shape.getType()))
        {
            case DRAWING_ARROW -> new ArrowPanel();
            case DRAWING_QUAD -> new RectanglePanel();
            case DRAWING_SQUARE -> new SquarePanel();
            case DRAWING_CIRCLE -> new CirclePanel();
            case DRAWING_ELLIPSE -> new EllipsePanel();
            case DRAWING_PATH, DRAWING_FREEHAND_PATH -> new PathPanel();
            case DRAWING_POLYGON, DRAWING_FREEHAND_POLYGON -> new PolygonPanel();
            default -> new EmptyShapePanel();
        };

        shapeInfoPanel.createComponents();

        add(shapeInfoPanel, CC.rc(3, 1));
    }

    /**
     * Refresh all the property values displayed.
     * @since 5.0
     */
    @InvokeOnEDT
    public void refresh()
    {
        shapeInfoPanel.createComponents();
    }

    private final class ArrowPanel extends AShapePanel<IArrow>
    {
        @Override
        protected void createComponents()
        {
            addLocationAndOrientation();
            addPropertyPanel("Range", getShape().getRangeLabelText());
            addPropertyPanel("Bearing", getShape().getBearingLabelText());
        }
    }

    private final class RectanglePanel extends AShapePanel<IRectangle>
    {
        @Override
        protected void createComponents()
        {
            addLocationAndOrientation();
            addPropertyPanel("Height", formatLength(getShape().getHeight()));
            addPropertyPanel("Width", formatLength(getShape().getWidth()));
            addArea();
            addDepth(getVerticalHeightString());
        }
    }

    private final class SquarePanel extends AShapePanel<ISquare>
    {
        @Override
        protected void createComponents()
        {
            addLocationAndOrientation();
            addPropertyPanel("Size", formatLength(getShape().getWidth()));
            addArea();
            addDepth(getVerticalHeightString());
        }
    }

    private final class CirclePanel extends AShapePanel<ICircle>
    {
        @Override
        protected void createComponents()
        {
            addLocationAndOrientation();
            addPropertyPanel("Radius", formatLength(getShape().getRadius()));
            addArea();
            addDepth(getVerticalHeightString());
        }
    }

    private final class EllipsePanel extends AShapePanel<IEllipse>
    {
        @Override
        protected void createComponents()
        {
            addLocationAndOrientation();
            addPropertyPanel("Major Axis", formatLength(getShape().getSemiMajorAxis() * 2));
            addPropertyPanel("Minor Axis", formatLength(getShape().getSemiMinorAxis() * 2));
            addArea();
            addDepth(getVerticalHeightString());
        }
    }

    private final class PathPanel extends AShapePanel<IPolyline>
    {
        @Override
        protected void createComponents()
        {
            addLocationAndOrientation();

            final LengthMeasurer measurer = new LengthMeasurer();
            measurer.setFollowTerrain(true);
            measurer.setPathType(WorldWind.GREAT_CIRCLE);
            measurer.setPositions(getShape().getVertices().stream().map(WWUtils::locationToPosition).toList());
            addPropertyPanel("Length", formatLength(measurer.getLength(wwGlobe)));

            terrainFollowingPathPanel.setPath(getShape());
            add(terrainFollowingPathPanel);
        }
    }

    private final class PolygonPanel extends AShapePanel<IPolygon>
    {
        @Override
        protected void createComponents()
        {
            addLocationAndOrientation();
            addArea();
            addDepth(getVerticalHeightString());
        }
    }

    private final class EmptyShapePanel extends AShapePanel<IShape>
    {
        @Override
        protected void createComponents()
        {
        }
    }

    /**
     * Base class to support common operations for building the property panel for each shape type.
     * @param <T> The type of shape being handled
     */
    private abstract class AShapePanel<T extends IShape> extends JPanel
    {
        private final Map<String, PropertyValuePanel> panelsByName = new HashMap<>();

        protected AShapePanel()
        {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        @SuppressWarnings("unchecked")
        protected T getShape()
        {
            return (T) shape;
        }

        /**
         * Add/update all property panels for this shape type.
         */
        protected abstract void createComponents();

        /**
         * Create or update a {@link PropertyValuePanel} with the given name and value.
         */
        protected void addPropertyPanel(String name, String valueText)
        {
            if (panelsByName.containsKey(name))
            {
                panelsByName.get(name).setValue(valueText);
            } else
            {
                final PropertyValuePanel panel = new PropertyValuePanel(name, valueText);
                add(panel);
                panelsByName.put(name, panel);
            }
        }

        /**
         * Add property panels for the location of the shape and, if it is a regular shape, add a panel for the orientation.
         */
        protected void addLocationAndOrientation()
        {
            final ILocation location = shape.getLocation();
            final boolean unknownLoc = GeoPoint.UNKNOWN_POINT.equals(location);

            final String lat = unknownLoc ? "" : formatAngle(location.getLatitude());
            final String lon = unknownLoc ? "" : formatAngle(location.getLongitude());

            final boolean regular = shape instanceof IRegularShape;

            addPropertyPanel(regular ? "Center Lat" : "Start Lat", lat);
            addPropertyPanel(regular ? "Center Lon" : "Start Lon", lon);

            if (regular)
            {
                addPropertyPanel("Orientation", formatAngle(((IRegularShape) shape).getOrientation()));
            }
        }

        protected void addArea()
        {
            if (getShape() instanceof IIrregularShape irregularShape)
            {
                final AreaMeasurer measurer = new AreaMeasurer();
                measurer.setFollowTerrain(true);
                measurer.setPathType(WorldWind.GREAT_CIRCLE);
                measurer.setPositions(irregularShape.getVertices().stream().map(WWUtils::locationToPosition).toList());
                addPropertyPanel("Perimeter", formatLength(measurer.getPerimeter(wwGlobe)));
                addPropertyPanel("Area", formatArea(measurer.getArea(wwGlobe)));
            }
        }

        protected void addDepth(String depthString)
        {
            addPropertyPanel("Height", depthString);
        }

        protected String formatAngle(double angleDegrees)
        {
            return String.format("%9.4f°", angleDegrees).trim();
        }

        protected String formatLength(double meters)
        {
            return rangeUnits.format(meters, 3);
        }

        private String formatArea(double squareMeters)
        {
            return rangeUnits.formatArea(squareMeters, 3);
        }

        protected String getVerticalHeightString()
        {
            return shape == null || !shape.isVerticalHeightEnabled()
                    ? "N/A"
                    : formatVerticalHeight(shape.getVerticalHeight());
        }

        protected String formatVerticalHeight(double height)
        {
            return altitudeUnits.format(height, 1);
        }
    }

    /**
     * Panel containing components to show a name in an outlined box, and a property value to the right of that.
     */
    private static class PropertyValuePanel extends JPanel
    {
        private static final Border lineBorder = BorderFactory.createLineBorder(Color.darkGray);

        private final JLabel valueLabel;

        PropertyValuePanel(String name, String valueText)
        {
            setName(name);
            setLayout(new FormLayout("37dlu, $lcgap, default:grow", "default"));

            final JLabel nameLabel = new JLabel(name, SwingConstants.CENTER);
            nameLabel.setBorder(lineBorder);
            nameLabel.setIconTextGap(0);

            valueLabel = new JLabel(valueText, SwingConstants.LEFT);
            valueLabel.setIconTextGap(0);

            add(nameLabel, CC.xy(1, 1));
            add(valueLabel, CC.xy(3, 1));
        }

        void setValue(String value)
        {
            valueLabel.setText(value);
        }

        @Override
        public Insets getInsets()
        {
            return new Insets(3, 0, 0, 3);
        }
    }
}
