package gov.takx.core.view.gui.maps.terraindiscovery.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.shapes.IMutableRectangle;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.IShapeServiceListener;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.ITerrainDiscoveryController;
import gov.takx.dataservices.shapes.PublishedShapeTracker;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Component;
import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class GeofencePanel extends JPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private PublishedShapeTracker publishedShapeTracker;

    @Inject
    private IGlobeManager globeManager;

    private final ITerrainDiscoveryController controller;
    private final GeofenceModel geofenceModel = new GeofenceModel();
    private final Map<IRectangle, Color> geofenceColors = new ConcurrentHashMap<>();

    private IRectangle selectedRectangle;
    private boolean dialogVisible = false;
    private boolean listenerAdded;
    private String lastSelectedGeofenceKey = null;

    GeofencePanel(ITerrainDiscoveryController controller)
    {
        this.controller = controller;

        initComponents();

        geofenceComboBox.setRenderer(new GeofenceCellRenderer());
    }

    /**
     * Additional UI initialization that requires injection to have already been completed.  Retrieves the current
     * list of shapes that can be used for terrain discovery and populates the combo box.
     *
     * @since 2.33
     */
    @PostConstruct
    private void postConstruct()
    {
        reattachShapeListener();
        selectedRectangle = null;
        updateSelectedGeofence();
    }

    @InvokeOnEDT
    public void geofencesUpdate(IRectangle rectangle, boolean add)
    {
        if (add)
        {
            geofenceComboBox.addItem(rectangle);
            geofenceColors.put(rectangle, rectangle.getFillColor());
        }

        if (dialogVisible && !controller.isProjectClosing())
        {
            if (rectangle instanceof IMutableRectangle)
            {
                updateRectangleFillColor((IMutableRectangle) rectangle, add);
            }
        }

        if (!add)
        {
            geofenceComboBox.removeItem(rectangle);
            geofenceColors.remove(rectangle);
        }
        updateSelectedGeofence();
    }

    private IRectangle getSelectedGeofence()
    {
        return (IRectangle) geofenceComboBox.getSelectedItem();
    }

    /**
     * removes this panel from shape service listener and cleans up UI.
     * <p/>
     * This function should be called when Terrain Discovery tool is closed.
     */
    public void detachShapeListener()
    {
        IRectangle currentlySelectedRectangle = getSelectedGeofence();
        lastSelectedGeofenceKey = currentlySelectedRectangle != null ? currentlySelectedRectangle.getKey() : null;
        if (listenerAdded)
        {
            publishedShapeTracker.removeShapeServiceListener(geofenceModel);
            listenerAdded = false;
        }

        setGeofencesTransparent(false); // Revert geofences to their original transparency

        geofenceComboBox.removeAllItems();
        geofenceColors.clear();
        selectedRectangle = null;
    }

    /**
     * retrieves all valid geofences from shapeService upon creation of the panel. Also add this panel as a listener
     * to the ShapeService in order to add/remove geofences from the combo box when user adds/removes geofences from
     * the globe.
     */
    public void reattachShapeListener()
    {
        if (!listenerAdded)
        {
            listenerAdded = true;

            // add shape service listener to get notified of newly created geofence
            publishedShapeTracker.addShapeServiceListener(geofenceModel);
            List<IShape> shapes = publishedShapeTracker.getShapes();

            shapes.sort(Comparator.comparing(IShape::getName));

            IShape lastSelectedShape = null;
            for (IShape shape : shapes)
            {
                if (logger.isTraceEnabled()) logger.trace("adding {} to TerrainDiscovery tool", shape.getName());
                geofenceModel.addShape(shape);

                if (shape.getKey().equals(lastSelectedGeofenceKey)) lastSelectedShape = shape;
            }

            if (lastSelectedShape != null)
            {
                geofenceComboBox.setSelectedItem(lastSelectedShape);
                updateSelectedGeofence();
            }
        }
    }

    public void dialogVisible(boolean visible)
    {
        dialogVisible = visible;
        setGeofencesTransparent(visible);
    }

    public void refreshToModel()
    {
        updateSelectedGeofence();
    }

    private void setGeofencesTransparent(boolean transparent)
    {
        for (IRectangle rectangle : geofenceColors.keySet())
        {
            if (rectangle instanceof IMutableRectangle)
            {
                updateRectangleFillColor((IMutableRectangle) rectangle, transparent);
            }
        }
    }

    private void updateSelectedGeofence()
    {
        selectedRectangle = getSelectedGeofence();
        controller.setSelectedGeofence(selectedRectangle);
    }

    private void geofenceComboBoxActionPerformed()
    {
        updateSelectedGeofence();
    }

    private void goToButtonActionPerformed()
    {
        IRectangle item = (IRectangle) geofenceComboBox.getSelectedItem();
        if (item != null)
        {
            globeManager.getPrimaryGlobeDelegate().goTo(getCenter(item));
        }
    }

    private ILocation getCenter(IRectangle rectangle)
    {
        ILocation minCorner = rectangle.getMinCorner();
        ILocation maxCorner = rectangle.getMaxCorner();
        double midLat = (maxCorner.getLatitude() - minCorner.getLatitude()) / 2;
        double midLon = (maxCorner.getLongitude() - minCorner.getLongitude()) / 2;
        return new RaptorLocation(minCorner.getLatitude() + midLat, minCorner.getLongitude() + midLon);
    }

    @InvokeOnEDT
    private void updateRectangleFillColor(final IMutableRectangle rectangle, final boolean transparent)
    {
        Color c;
        Color fill;
        if (transparent)
        {
            fill = rectangle.getFillColor();
            c = new Color(fill.getRed(), fill.getBlue(), fill.getGreen(), 1);
        } else
        {
            c = geofenceColors.get(rectangle);
            if (c == null)
            {
                fill = rectangle.getFillColor();
                c = new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 178);
            }
        }
        rectangle.setFillColor(c);
    }

    /**
     * List cell renderer to show the name of the rectangle.
     *
     * @since 3.0
     */
    private static class GeofenceCellRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean selected, boolean focused)
        {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
            if (value instanceof IRectangle)
            {
                label.setText(((IRectangle) value).getName());
            }
            return label;
        }
    }

    private class GeofenceModel implements IShapeServiceListener
    {
        @Override
        public void onShapePublished(IShape shape)
        {
            addShape(shape);
        }

        @Override
        public void onShapeUnpublished(IShape shape)
        {
            removeShape(shape);
        }

        @Override
        public void onShapeDefined(IShape shape)
        {
            addShape(shape);
        }

        @Override
        public void onShapeDeletion(IShape shape)
        {
            removeShape(shape);
        }

        private void addShape(IShape shape)
        {
            if (shape instanceof IRectangle)
            {
                geofencesUpdate((IRectangle) shape, true);
            }
        }

        private void removeShape(IShape shape)
        {
            if (shape instanceof IRectangle)
            {
                geofencesUpdate((IRectangle) shape, false);
            }
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panel1 = new JPanel();
        var panel2 = new JPanel();
        var geofenceLabel = new JLabel();
        geofenceComboBox = new JComboBox<>();
        var goToButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("panelGeofence");
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        //======== panel1 ========
        {
            panel1.setName("panel1");
            panel1.setLayout(new FormLayout(
                    "default:grow",
                    "default"));

            //======== panel2 ========
            {
                panel2.setName("panel2");
                panel2.setLayout(new FormLayout(
                        "default, $lcgap, default:grow, $lcgap, default",
                        "default"));

                //---- geofenceLabel ----
                geofenceLabel.setText("Region: ");
                geofenceLabel.setName("geofenceLabel");
                panel2.add(geofenceLabel, cc.xy(1, 1));

                //---- geofenceComboBox ----
                geofenceComboBox.setName("geofenceComboBox");
                geofenceComboBox.addActionListener(e -> geofenceComboBoxActionPerformed());
                panel2.add(geofenceComboBox, cc.xy(3, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

                //---- goToButton ----
                goToButton.setText("Go To");
                goToButton.setToolTipText("Go to selected region on the globe");
                goToButton.setName("goToButton");
                goToButton.addActionListener(e -> goToButtonActionPerformed());
                panel2.add(goToButton, cc.xy(5, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
            }
            panel1.add(panel2, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));
        }
        add(panel1);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JComboBox<IRectangle> geofenceComboBox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
