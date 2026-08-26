package gov.takx.core.view.gui.maps.exporters.dataset;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IShapePathMapObject;
import gov.takx.api.mapobject.IShapePolygonMapObject;
import gov.takx.api.mapobject.IShapeRectangleMapObject;
import gov.takx.api.mapobject.IShapeSquareMapObject;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.shapes.IIrregularShape;
import gov.takx.api.shapes.IMutablePolygon;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.api.shapes.IPolygon;
import gov.takx.api.shapes.IPolyline;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.IShapeDrawingService;
import gov.takx.api.shapes.ShapeAlreadyExistsException;
import gov.takx.api.shapes.ShapeDrawingConfiguration;
import gov.takx.api.shapes.ShapeType;
import gov.takx.core.shapes.ShapeService;
import gov.takx.core.shapes.ShapeServiceFactory;
import gov.takx.dataservices.shapes.Polygon;
import gov.takx.platform.utils.WWUtils;
import gov.takx.platform.utils.units.RangeUnits;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.MutableComboBoxModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static gov.takx.platform.preferences.UserPreferenceEntry.DISPLAY_RANGE_UNITS;

/**
 * Panel used to create new boundary selection shapes and allow selection of one to be used for map data cache creation
 */
@PseudoBean
public class MultishapeBoundarySelectionPanel extends JPanel implements IBoundarySelection
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String RECTANGLE_TYPE = ShapeType.RECTANGLE.getShapeType();
    private static final String POLYGON_TYPE = ShapeType.POLYGON.getShapeType();
    private static final String POLYLINE_TYPE = ShapeType.POLYLINE.getShapeType();
    private static final String SELECT_THICK_PATH = "Select Thick Path";

    private IPreferenceMonitor<RangeUnits> rangeUnitsMonitor;
    private RangeUnits rangeUnits;

    private boolean fromExistingPath = false;
    private final List<IPolyline> paths = new ArrayList<>();

    private ShapeType shapeType = ShapeType.RECTANGLE;
    private IBoundaryDrawingListener boundaryDrawingListener;
    private IShape createdShape;
    private IShape currentShape;
    private IBoundaryChangeListener boundaryChangeListener;
    private double defaultPathWidth = 1_000; // 1 KM
    private ShapeService shapeService;

    List<IShape> shapesList = new ArrayList<>();
    ShapeComboBoxModel<IShape> shapeModel = new ShapeComboBoxModel<>(shapesList);
    ShapeComboBoxModel<IPolyline> pathShapeModel = new ShapeComboBoxModel<>(paths);

    @Inject
    private IShapeDrawingService shapeDrawingService;

    @Inject
    private ShapeServiceFactory shapeServiceFactory;

    @Inject
    private IExtendedGlobeManager globeService;

    @Inject
    private IMapObjectPersist mapObjectPersist;

    @Inject
    private IMapObjectManager mapObjectManager;

    MultishapeBoundarySelectionPanel()
    {
        initComponents();
        shapeSelectionComboBox.setModel(shapeModel);
        pathAndThickPathSelectionComboBox.setModel(pathShapeModel);

        shapeCreationSelectionComboBox.addItem(RECTANGLE_TYPE);
        shapeCreationSelectionComboBox.addItem(POLYGON_TYPE);
        shapeCreationSelectionComboBox.addItem(POLYLINE_TYPE);
        shapeCreationSelectionComboBox.addItem(SELECT_THICK_PATH);
    }

    @PostConstruct
    private void postConstruct()
    {
        // Do this here to prevent JFD preview rendering errors
        rangeUnitsMonitor = DISPLAY_RANGE_UNITS.newMonitor(RangeUnits.class, this::updateRangeUnits);
        rangeUnits = rangeUnitsMonitor.getCurrentValue();

        setDefaultWidthForUnits();
        repopulateShapes();
    }

    private void setDefaultWidthForUnits()
    {
        double lengthUnitsMultiplier = rangeUnits.getMultiplier();
        double width = defaultPathWidth * lengthUnitsMultiplier;
        pathWidthSpinner.setValue(width);
    }

    private void updatePathWidget()
    {
        pathAndThickPathSelectionComboBox.setVisible(fromExistingPath);
        pathWidthLabel.setVisible(fromExistingPath);
        pathWidthSpinner.setVisible(fromExistingPath);
    }

    private void clearEditorPanel()
    {
        getGlobe().setMapTool(MapToolType.NONE);
    }

    private IApplicationGlobe getGlobe()
    {
        return globeService.getLastFocusedGlobe();
    }

    private void createShapeButtonActionPerformed()
    {
        if (null != shapeType)
        {
            clearEditorPanel();

            //noinspection EnumSwitchStatementWhichMissesCases,SwitchStatementWithoutDefaultBranch
            switch (shapeType)
            {
                case RECTANGLE -> invokeDraw(ShapeType.RECTANGLE);
                case POLYGON -> invokeDraw(ShapeType.POLYGON);
                case POLYLINE ->
                {
                    if (fromExistingPath)
                    {
                        IPolyline polyline = (IPolyline) pathAndThickPathSelectionComboBox.getSelectedItem();
                        if (polyline != null)
                        {
                            currentShape = polyline;
                            notifyBoundaryChanged();
                        }
                    } else
                    {
                        invokeDraw(ShapeType.POLYLINE);
                    }
                }
            }
        }
    }

    /**
     * Use the {@link IShapeDrawingService} to request the user draw a shape.
     *
     * @param shapeType Type of shape to draw
     * @since 3.5
     */
    private void invokeDraw(ShapeType shapeType)
    {
        removeCreatedShape();

        boundaryDrawingListener.onDrawingBoundary(true);

        final String instructions = switch (shapeType)
        {
            case RECTANGLE -> "Click and Drag to draw a rectangle";
            case POLYGON, POLYLINE -> "Click to place points, double click or hit ESC to complete the shape";
            default -> "";
        };

        final var configuration = ShapeDrawingConfiguration.builder(getGlobe(), shapeType)
                .instructions(instructions)
                .escapeAction(ShapeDrawingConfiguration.EscapeAction.complete)
                .build();
        final CompletableFuture<IMutableShape> future = shapeDrawingService.requestDraw(configuration);

        future.whenComplete((shape, t) -> {
            logger.debug("Got {}", shape, t);

            if (t == null)
            {
                currentShape = createdShape = shape;

                if (shapeType == ShapeType.POLYLINE)
                {
                    IPolygon polygon = handlePolylineSelection();
                    if (polygon != null) currentShape = polygon;
                }

                notifyBoundaryChanged();
            }

            boundaryDrawingListener.onDrawingBoundary(false);
        });
    }

    /**
     * Update the current units of measure selected by the user and update affected labels.
     *
     * @param rangeUnitsFormat The new units format
     * @since 5.5 (signature change)
     */
    @InvokeOnEDT
    private void updateRangeUnits(RangeUnits rangeUnitsFormat)
    {
        this.rangeUnits = rangeUnitsFormat;
        updatePathWidthLabel();
    }

    /**
     * Get the path width in meters
     */
    private double getPathWidthMeters()
    {
        return (Double) pathWidthSpinner.getValue() / rangeUnits.getMultiplier();
    }

    private void updatePathWidthLabel()
    {
        pathWidthLabel.setText("Width (" + rangeUnits.getAbbreviation() + "):");
    }

    private void repopulateShapes()
    {
        shapeSelectionComboBox.removeAllItems();

        shapesList.clear();
        shapesList.addAll(mapObjectPersist.findMapObjectsOfType(IShapeRectangleMapObject.class));
        shapesList.addAll(mapObjectPersist.findMapObjectsOfType(IShapeSquareMapObject.class));
        shapesList.addAll(mapObjectPersist.findMapObjectsOfType(IShapePolygonMapObject.class));
        shapesList.sort(Comparator.comparing(IShape::getName));
        shapeModel.fireUpdateList();
        if (currentShape != null) shapeSelectionComboBox.setSelectedItem(currentShape);

        paths.clear();
        paths.addAll(mapObjectPersist.findMapObjectsOfType(IShapePathMapObject.class));

        paths.sort(Comparator.comparing(IShape::getName));
        IShape path = pathShapeModel.selectedItem;
        pathShapeModel.fireUpdateList();
        if (path != null) pathAndThickPathSelectionComboBox.setSelectedItem(path);
    }

    private IPolygon handlePolylineSelection()
    {
        List<ILocation> vertices = createPolygonFromPolyline();
        return vertices != null ? createAndDrawPolygon(createBoundaryName(currentShape.getName()), vertices) : null;
    }

    private IMutablePolygon createAndDrawPolygon(String name, List<ILocation> vertices)
    {
        if (shapeService == null)
        {
            shapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        }

        IShapePolygonMapObject polygon = null;
        try
        {
            polygon = shapeService.createPolygonMapObject(new Polygon(name, vertices));
        } catch (IllegalArgumentException | ShapeAlreadyExistsException ex)
        {
            try
            {
                IShape poly = shapeModel.getShapePathByName(name);
                if (poly != null)
                {
                    mapObjectManager.deleteMapObject(poly.getShapeMapObject());
                }
                polygon = shapeService.createPolygonMapObject(new Polygon(name, vertices));
            } catch (Exception e)
            {
                logger.error("Error deleting shape named {}", name, e);
            }
        }
        if (polygon != null)
        {
            polygon.setEditable(true);
            polygon.setFillColor(Color.ORANGE);
            polygon.setOpacity(70);
        }

        return polygon;
    }

    private List<ILocation> createPolygonFromPolyline()
    {
        List<ILocation> polygonVertices = null;
        if (currentShape != null)
        {
            if (POLYLINE_TYPE.equals(currentShape.getType()))
            {
                List<ILocation> lineVertices = ((IIrregularShape) currentShape).getVertices();
                Sector s = getBoundingSector(lineVertices).get();
                double minLat = Math.abs(s.getMinLatitude().radians);
                double maxLat = Math.abs(s.getMaxLatitude().radians);
                double r = getGlobe().getWorldWindow().getModel().getGlobe().getRadiusAt(Angle.fromRadians(Math.max(maxLat, minLat)), s.getMinLongitude());
                double widthDegrees = getPathWidthMeters() / (Math.PI * 2 * r) * 360.0;
                polygonVertices = BufferedPolylineFactory.createPolygonFromBufferedPolyline(lineVertices, widthDegrees);
            }
        }
        return polygonVertices;
    }

    private void notifyBoundaryChanged()
    {
        Boundary boundary = getBoundary();
        if (boundary != null) boundaryChangeListener.onBoundaryChanged(boundary);
    }

    @Override
    public Boundary getBoundary()
    {
        Boundary boundary = null;
        if (currentShape != null)
        {
            if (currentShape instanceof IRectangle rectangle)
            {
                List<ILocation> vertices = rectangle.getVertices();
                if (vertices != null)
                {
                    boundary = new Boundary(vertices, true, 0);
                }
            } else if (currentShape instanceof IPolygon poly)
            {
                List<ILocation> vertices = poly.getVertices();
                boundary = new Boundary(vertices, true, 0);
            } else if (currentShape instanceof IPolyline)
            {
                var poly = handlePolylineSelection();
                currentShape = poly;
                repopulateShapes();
                List<ILocation> vertices = poly.getVertices();
                boundary = new Boundary(vertices, true, getPathWidthMeters());
            }
        }
        return boundary;
    }

    private String createBoundaryName(String name)
    {
        return name + "-boundary";
    }

    public void setBoundarySelectorListener(IBoundaryChangeListener boundaryChangeListener)
    {
        this.boundaryChangeListener = boundaryChangeListener;
    }

    @Override
    public void cleanUp()
    {
        rangeUnitsMonitor.dispose();
        boundaryChangeListener = null;

        removeCreatedShape();
    }

    /**
     * Remove any previously drawn shape.
     * @since 3.5
     */
    private void removeCreatedShape()
    {
        if (createdShape != null)
        {
            shapeDrawingService.removeDrawnShape(createdShape);
            createdShape = null;
        }
    }

    @Override
    public Optional<Sector> getSector()
    {
        return getBoundingSector();
    }

    private Optional<Sector> getBoundingSector()
    {
        if (currentShape != null)
        {
            if (currentShape instanceof IRectangle rectangle)
            {
                return getBoundingSector(rectangle.getVertices());
            } else if (currentShape instanceof IIrregularShape poly)
            {
                return getBoundingSector(poly.getVertices());
            }
        }
        return Optional.empty();
    }

    private Optional<Sector> getBoundingSector(List<ILocation> vertices)
    {
        return Optional.of(WWUtils.boundingSector(vertices));
    }

    @Override
    public void setBoundaryDrawingListener(IBoundaryDrawingListener listener)
    {
        boundaryDrawingListener = listener;
    }

    private void shapeSelectionComboBoxActionPerformed()
    {
        IMutableShape shape = (IMutableShape) shapeSelectionComboBox.getSelectedItem();
        if (shape != null)
        {
            removeCreatedShape();
            currentShape = shape;
            notifyBoundaryChanged();
        }
    }

    private void pathWidthSpinnerStateChanged()
    {
        defaultPathWidth = getPathWidthMeters();
    }

    private void shapeCreationSelectionComboBoxActionPerformed()
    {
        String shapeToCreateText = (String) shapeCreationSelectionComboBox.getSelectedItem();
        fromExistingPath = SELECT_THICK_PATH.equals(shapeToCreateText);

        if (fromExistingPath)
        {
            shapeType = ShapeType.POLYLINE;
        } else if (shapeToCreateText != null)
        {
            shapeType = ShapeType.valueForType(shapeToCreateText);
        } else
        {
            shapeType = null;
        }

        updatePathWidget();
    }

    private static class ShapeComboBoxModel<T extends IShape> extends AbstractListModel<T>
            implements MutableComboBoxModel<T>
    {
        List<T> shapes;
        IMutableShape selectedItem;

        public ShapeComboBoxModel(List<T> shapes)
        {
            this.shapes = shapes;
        }

        @Override
        public int getSize()
        {
            return shapes.size();
        }

        @Override
        public T getElementAt(int index)
        {
            return shapes.get(index);
        }

        @Override
        public void setSelectedItem(Object selectedItem)
        {
            if (!Objects.equals(this.selectedItem, selectedItem))
            {
                this.selectedItem = (IMutableShape) selectedItem;
                fireContentsChanged(this, -1, -1);
            }
        }

        public void fireUpdateList()
        {
            fireAllChanged();
        }

        @Override
        public IMutableShape getSelectedItem()
        {
            return selectedItem;
        }

        private void fireAllChanged()
        {
            fireContentsChanged(this, 0, getSize());
        }

        @Override
        public void addElement(T item)
        {
            shapes.add(item);
            fireAllChanged();
        }

        @SuppressWarnings("SuspiciousMethodCalls")
        @Override
        public void removeElement(Object obj)
        {
            shapes.remove(obj);
            fireAllChanged();
        }

        @Override
        public void insertElementAt(T item, int index)
        {
            shapes.add(index, item);
            fireAllChanged();
        }

        @Override
        public void removeElementAt(int index)
        {
            shapes.remove(index);
            fireAllChanged();
        }

        public IShape getShapePathByName(String name)
        {
            for (IShape shape : shapes)
            {
                if (shape instanceof IPolyline && shape.getName().equals(name)) return shape;
            }
            return null;
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var shapeCreationPanel = new JPanel();
        shapeCreationSelectionComboBox = new JComboBox<>();
        pathAndThickPathSelectionComboBox = new JComboBox<>();
        pathWidthLabel = new JLabel();
        pathWidthSpinner = new JSpinner();
        var createShapeButton = new JButton();
        var shapeSelectionPanel = new JPanel();
        var selectExistingShapeLabel = new JLabel();
        shapeSelectionComboBox = new JComboBox<>();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default, $lgap, fill:default"));

        //======== shapeCreationPanel ========
        {
            shapeCreationPanel.setBorder(new TitledBorder(null, "Shape Creation", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION));
            shapeCreationPanel.setName("shapeCreationPanel");
            shapeCreationPanel.setLayout(new FormLayout(
                    "default, $lcgap, default:grow, 2*($lcgap, default)",
                    "default, $lgap, fill:default"));

            //---- shapeCreationSelectionComboBox ----
            shapeCreationSelectionComboBox.setName("shapeCreationSelectionComboBox");
            shapeCreationSelectionComboBox.addActionListener(e -> shapeCreationSelectionComboBoxActionPerformed());
            shapeCreationPanel.add(shapeCreationSelectionComboBox, cc.xy(1, 1));

            //---- pathAndThickPathSelectionComboBox ----
            pathAndThickPathSelectionComboBox.setName("pathAndThickPathSelectionComboBox");
            shapeCreationPanel.add(pathAndThickPathSelectionComboBox, cc.xy(3, 1));

            //---- pathWidthLabel ----
            pathWidthLabel.setText("Width (m): ");
            pathWidthLabel.setName("pathWidthLabel");
            shapeCreationPanel.add(pathWidthLabel, cc.xy(5, 1));

            //---- pathWidthSpinner ----
            pathWidthSpinner.setModel(new SpinnerNumberModel(10.0, 1.0, null, 1.0));
            pathWidthSpinner.setName("pathWidthSpinner");
            pathWidthSpinner.addChangeListener(e -> pathWidthSpinnerStateChanged());
            shapeCreationPanel.add(pathWidthSpinner, cc.xy(7, 1));

            //---- createShapeButton ----
            createShapeButton.setText("Create New Shape");
            createShapeButton.setName("createShapeButton");
            createShapeButton.addActionListener(e -> createShapeButtonActionPerformed());
            shapeCreationPanel.add(createShapeButton, cc.xy(1, 3));
        }
        add(shapeCreationPanel, cc.xy(1, 1));

        //======== shapeSelectionPanel ========
        {
            shapeSelectionPanel.setBorder(new TitledBorder(null, "Shape Selection", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION));
            shapeSelectionPanel.setName("shapeSelectionPanel");
            shapeSelectionPanel.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "fill:default"));

            //---- selectExistingShapeLabel ----
            selectExistingShapeLabel.setText("Select Existing Shape:");
            selectExistingShapeLabel.setName("selectExistingShapeLabel");
            shapeSelectionPanel.add(selectExistingShapeLabel, cc.xy(1, 1));

            //---- shapeSelectionComboBox ----
            shapeSelectionComboBox.setName("shapeSelectionComboBox");
            shapeSelectionComboBox.addActionListener(e -> shapeSelectionComboBoxActionPerformed());
            shapeSelectionPanel.add(shapeSelectionComboBox, cc.xy(3, 1));
        }
        add(shapeSelectionPanel, cc.xy(1, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JComboBox<String> shapeCreationSelectionComboBox;
    private JComboBox<IPolyline> pathAndThickPathSelectionComboBox;
    private JLabel pathWidthLabel;
    private JSpinner pathWidthSpinner;
    private JComboBox<IShape> shapeSelectionComboBox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
