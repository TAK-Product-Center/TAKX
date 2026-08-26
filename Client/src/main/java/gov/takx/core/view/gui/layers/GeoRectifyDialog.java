package gov.takx.core.view.gui.layers;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.ZoomableImagePanel;
import gov.takx.core.globe.layers.SurfaceImageLayer;
import gov.takx.core.globe.layers.SurfaceImageLayerState;
import gov.takx.core.uimodel.ACoordinateSystemListModel;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.ResourceDirectory.RESOURCES;
import static java.awt.Cursor.CROSSHAIR_CURSOR;
import static java.awt.Cursor.MOVE_CURSOR;
import static java.awt.Cursor.getPredefinedCursor;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class GeoRectifyDialog extends AStandardDialog implements ListDataListener
{
    private final IRaptorLayer raptorLayer;

    @Inject
    private IExtendedGlobeManager globeManager;

    private SurfaceImageLayer surfaceImageLayer;
    private BufferedImage image;
    private final ControlPointListModel pointListModel = new ControlPointListModel();
    private boolean draggingEnabled;
    private int longitude;
    private int latitude;
    private final Cursor moveCursor = getPredefinedCursor(MOVE_CURSOR);
    private final Cursor crosshairCursor = getPredefinedCursor(CROSSHAIR_CURSOR);
    private final IGeoRectifyListener geoRectifyListener;

    private static final Map<IRaptorLayer, JDialog> visibleDialogs = new HashMap<>();

    @Override
    public void intervalAdded(ListDataEvent e)
    {
        updateOkButtonState();
    }

    @Override
    public void intervalRemoved(ListDataEvent e)
    {
        updateOkButtonState();
    }

    @Override
    public void contentsChanged(ListDataEvent e)
    {
    }

    private void updateOkButtonState()
    {
        okButton.setEnabled(pointListModel.getSize() >= 3);
    }

    public interface IGeoRectifyListener
    {
        void setSurfaceImageEditable(boolean editable);
    }

    public static void showGeoRectifyDialog(IRaptorLayer layer)
    {
        showGeoRectifyDialog(layer, null);
    }

    public static void showGeoRectifyDialog(IRaptorLayer layer, IGeoRectifyListener listener)
    {
        if (!visibleDialogs.containsKey(layer))
        {
            new GeoRectifyDialog(layer, listener).setVisible(true);
        } else
        {
            // If the dialog is already visible, request focus
            visibleDialogs.get(layer).requestFocus();
        }
    }

    private GeoRectifyDialog(IRaptorLayer layer, IGeoRectifyListener listener)
    {
        raptorLayer = layer;
        geoRectifyListener = listener;
    }

    @PostConstruct
    private void postConstruct()
    {
        surfaceImageLayer = (SurfaceImageLayer) globeManager.getPrimaryGlobeDelegate().getGlobeLayerManager().getGlobeLayer(raptorLayer);
        image = surfaceImageLayer.getImage();

        initComponents();

        pointListModel.addListDataListener(this);
        populatePointListModelWithExistingControlPoints();

        getRootPane().setDefaultButton(okButton);
        pack();

    }

    /**
     * Populates the current point list model with any existing control points used to
     * geo-rectify the image
     *
     * @since 5.1.0
     */
    private void populatePointListModelWithExistingControlPoints()
    {
        for (SurfaceImageLayerState.ControlPoint cp : surfaceImageLayer.getControlPoints())
        {
            pointListModel.add(cp.point(), cp.location());
        }
    }

    @Override
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);
        updateVisibleDialogMap(visible);
    }

    /**
     * Updates the map of visible dialogs so a dialog is not recreated if it already is being shown to the user.
     *
     * @param visible True if this dialog is being made visible
     * @since 3.3
     */
    private void updateVisibleDialogMap(boolean visible)
    {
        if (visible)
        {
            visibleDialogs.put(surfaceImageLayer.getRaptorLayer(), this);
        } else
        {
            visibleDialogs.remove(surfaceImageLayer.getRaptorLayer());
        }
    }

    private void addControlPoint(Point point)
    {
        if (pointListModel.getSize() != 4)
        {
            new GeoRectifyControlPointDialog(this, point, pointListModel).setVisible(true);
        } else
        {
            AlwaysOnTopJOptionPane.showMessageDialog(this, "Geo Rectify only works with 3 or 4 points.", "Too Many Points", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private int decimalToPercent(double decimal)
    {
        return (int) (decimal * 100d);
    }

    private double percentToDecimal(int percent)
    {
        return percent / 100d;
    }

    private void zoomableImagePanelMouseDragged(MouseEvent event)
    {
        if (!draggingEnabled)
        {
            return;
        }

        if (zoomableImagePanel.getCursor().getType() != MOVE_CURSOR)
        {
            zoomableImagePanel.setCursor(moveCursor);
        }

        JViewport viewport = imageScrollPane.getViewport();
        Point position = viewport.getViewPosition().getLocation();

        position.translate(longitude - event.getX(), latitude - event.getY());

        Point max = new Point(zoomableImagePanel.getWidth() - viewport.getWidth(), zoomableImagePanel.getHeight() - viewport.getHeight());

        if (position.x < 0)
        {
            position.x = 0;
        } else if (position.x > max.x)
        {
            position.x = max.x;
        }

        if (position.y < 0)
        {
            position.y = 0;
        } else if (position.y > max.y)
        {
            position.y = max.y;
        }

        viewport.setViewPosition(position);
    }

    /**
     * User is setting the locations on the map, require double click to add a point.
     */
    private void zoomableImagePanelMouseClicked(MouseEvent event)
    {
        if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() == 2)
        {
            Point point = zoomableImagePanel.convertPointFromPanelToImage(event.getPoint());

            if (point != null)
            {
                addControlPoint(point);
            }
        }
    }

    private void zoomableImagePanelMousePressed(MouseEvent event)
    {
        if (event.getButton() == MouseEvent.BUTTON1)
        {
            draggingEnabled = true;
            longitude = event.getX();
            latitude = event.getY();
        } else
        {
            zoomableImagePanelMousePressedOrReleased(event);
        }
    }

    private void zoomableImagePanelMousePressedOrReleased(MouseEvent event)
    {
        if (event.isPopupTrigger())
        {
            final Point point = zoomableImagePanel.convertPointFromPanelToImage(event.getPoint());

            if (point == null)
            {
                return;
            }

            JPopupMenu menu = new JPopupMenu();

            menu.add(new AbstractAction("Add Point")
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    addControlPoint(point);
                }
            });
            menu.show(event.getComponent(), event.getX(), event.getY());
        }
    }

    /**
     * Change the cursor to a crosshair cursor for more accurate point location selection
     * When dragging, we don't want the cross-hairs.
     */
    private void zoomableImagePanelMouseReleased(MouseEvent event)
    {
        if (event.getButton() == MouseEvent.BUTTON1 && draggingEnabled)
        {
            draggingEnabled = false;

            if (zoomableImagePanel.getCursor().getType() != CROSSHAIR_CURSOR)
            {
                // If we don't do this check, when the user clicks out of bounds, it will change the cursor
                // to the cross-hairs cursor even when the cursor isn't in bounds.
                if (zoomableImagePanel.isPointFromPanelOnImage(new Point(event.getX(), event.getY())))
                {
                    zoomableImagePanel.setCursor(crosshairCursor);
                }
            }
        } else
        {
            zoomableImagePanelMousePressedOrReleased(event);
        }
    }

    /**
     * Support mouse wheel scrolling
     */
    private void zoomableImagePanelMouseWheelMoved()
    {
        zoomSlider.setValue(decimalToPercent(zoomableImagePanel.getZoom()));
    }

    /**
     * Support manual zoom change from the zoom slider
     */
    private void zoomSliderStateChanged()
    {
        zoomableImagePanel.setZoom(percentToDecimal(zoomSlider.getValue()));
    }

    /**
     * Listen for changes to the points list and disable the edit/remove button if the list is empty
     * or if none of the points are selected.
     */
    private void pointsListValueChanged()
    {
        removeButton.setEnabled(pointsList.getSelectedIndex() != -1);
        editPointButton.setEnabled(pointsList.getSelectedIndex() != -1);
    }

    /**
     * The user has clicked in the points list, if they double-click a point, give them the dialog
     * to edit the location of that point.
     */
    private void pointsListMouseClicked(MouseEvent event)
    {
        int index = pointsList.getSelectedIndex();

        if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() == 2 && index != -1)
        {
            showPointEditingDialog();
        }
    }

    /**
     * Show the user the dialog to edit the selected point
     */
    private void showPointEditingDialog()
    {
        int index = pointsList.getSelectedIndex();

        if (index != -1)
        {
            new GeoRectifyControlPointDialog(this, index, pointListModel).setVisible(true);
        }
    }

    /**
     * Mouse events inside the points list. Adds an Edit/Delete option for right clicks
     * and selects the user-selected item from the list based on the click.
     */
    private void pointsListMousePressedOrReleased(MouseEvent event)
    {
        if (event.isPopupTrigger())
        {
            final int index = pointsList.locationToIndex(event.getPoint());

            if (!pointsList.isSelectedIndex(index))
            {
                pointsList.setSelectedIndex(index);
            }

            if (pointsList.getSelectedIndex() == -1)
            {
                return;
            }

            JPopupMenu menu = new JPopupMenu();

            menu.add(new AbstractAction("Edit")
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    new GeoRectifyControlPointDialog(GeoRectifyDialog.this, index, pointListModel).setVisible(true);
                }
            });
            menu.add(new AbstractAction("Delete")
            {
                @Override
                public void actionPerformed(ActionEvent event)
                {
                    removeButtonActionPerformed();
                }
            });
            menu.show(event.getComponent(), event.getX(), event.getY());
        }
    }

    /**
     * Remove the highlighted point from the point list.
     */
    private void removeButtonActionPerformed()
    {
        pointListModel.remove(pointsList.getSelectedIndex());
    }

    /**
     * User has clicked ok, assuming the points are valid, save them to the model and close the dialog
     */
    private void okButtonActionPerformed()
    {
        surfaceImageLayer.warpImageWithControlPoints(pointListModel.getPoints(), pointListModel.getLocations());
        if (geoRectifyListener != null)
        {
            geoRectifyListener.setSurfaceImageEditable(false);
        }

        dispose();
    }

    /**
     * User clicked cancel, dispose the dialog and don't save anything.
     */
    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    /**
     * Handles updating the visibleDialogs list, applies any control points used to geo-rectify the image,
     * and removes the point list data listener from the point list model
     *
     * @since 3.3
     */
    @Override
    protected void onWindowClosing()
    {
        // Apply the control points to the layer
        surfaceImageLayer.setControlPoints(List.of(pointListModel.getPoints()), List.of(pointListModel.getLocations()));

        pointListModel.removeListDataListener(this);

        updateVisibleDialogMap(false);
    }

    /**
     * Edit the selected point.
     */
    private void editPointButtonActionPerformed()
    {
        showPointEditingDialog();
    }

    /**
     * If the mouse is outside the bounds of the image, disable the cursor so that the user knows they
     * can't click anything there. Also show a warning that they cannot select a point outside the bounds
     * of the image.
     */
    private void zoomableImagePanelMouseMoved(MouseEvent e)
    {
        if (!zoomableImagePanel.isPointFromPanelOnImage(new Point(e.getX(), e.getY())))
        {
            setCursorMoveOutOfBounds();
        } else
        {
            setCursorMovedInBounds();
        }
    }

    protected void setCursorMoveOutOfBounds()
    {
        // Cursor is 48x48, so set the focal point of it to the center.
        Image disabledCursor = Toolkit.getDefaultToolkit().getImage(UIResources.getUrl(PLATFORM_ICONS, "none.png"));
        zoomableImagePanel.setCursor(getToolkit().createCustomCursor(disabledCursor, new Point(24, 24), "Selection Out Of Bounds"));
        outOfBoundsLabel.setVisible(true);
    }

    protected void setCursorMovedInBounds()
    {
        zoomableImagePanel.setCursor(getPredefinedCursor(CROSSHAIR_CURSOR));
        outOfBoundsLabel.setVisible(false);
    }

    private class ControlPointListCellRenderer extends DefaultListCellRenderer
    {
        private final Icon globeIcon = UIResources.getStandardSizedIcon(RESOURCES, "globe.png");

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            setIcon(globeIcon);

            SurfaceImageLayerState.ControlPoint controlPoint = (SurfaceImageLayerState.ControlPoint) value;
            Point2D point = controlPoint.point();

            setText("<html><body>" + CoordinateSystem.getLocationString(controlPoint.location(), pointListModel.getCoordinateSystem())
                    + "<br/>(" + (int) point.getX() + ", " + (int) point.getY() + ")</body></html>");

            return this;
        }
    }

    /**
     * Point List Model to hold any control points used to geo-rectify the image
     */
    public static class ControlPointListModel extends ACoordinateSystemListModel<SurfaceImageLayerState.ControlPoint>
    {
        private final List<Point2D> pointsList = new ArrayList<>();
        private final List<ILocation> locationsList = new ArrayList<>();

        public Point getPoint(int index)
        {
            return (Point) pointsList.get(index);
        }

        public Point2D[] getPoints()
        {
            return pointsList.toArray(new Point2D[0]);
        }

        public ILocation getLocation(int index)
        {
            return locationsList.get(index);
        }

        public ILocation[] getLocations()
        {
            return locationsList.toArray(new ILocation[0]);
        }

        /**
         * Save a point at the given location to the list of points. Make sure the remove button is enabled since
         * we are guaranteed to have one or more points in the list.
         *
         * @param point The point to add
         * @param location The location of the point
         */
        public void add(Point2D point, ILocation location)
        {
            int index = pointsList.size();

            pointsList.add(point);
            locationsList.add(location);

            fireIntervalAdded(this, index, index);
        }

        public void set(int index, ILocation location)
        {
            locationsList.set(index, location);
            fireContentsChanged(this, index, index);
        }

        /**
         * Remove the selected point and if there are no points left, disable the remove button.
         *
         * @param index The index of the point to remove
         */
        public void remove(int index)
        {
            pointsList.remove(index);
            locationsList.remove(index);

            fireIntervalRemoved(this, index, index);
        }

        @Override
        public int getSize()
        {
            return pointsList.size();
        }

        @Override
        public SurfaceImageLayerState.ControlPoint getElementAt(int index)
        {
            return new SurfaceImageLayerState.ControlPoint(pointsList.get(index), locationsList.get(index));
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPanel = new JPanel();
        var contentPanel = new JPanel();
        var instructionsLabel = new JLabel();
        outOfBoundsLabel = new JLabel();
        var imageAndZoomControlPanel = new JPanel();
        imageScrollPane = new JScrollPane();
        zoomableImagePanel = new ZoomableImagePanel();
        var zoomControlsPanel = new JPanel();
        var zoomSliderLabel = new JLabel();
        zoomSlider = new JSlider();
        var pointsLabel = new JLabel();
        var pointsListScrollPane = new JScrollPane();
        pointsList = new JList<>();
        var editRemoveButtonPanel = new JPanel();
        removeButton = new JButton();
        editPointButton = new JButton();
        var buttonPanel = new JPanel();
        okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Geo Rectify");
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPanel ========
        {
            dialogPanel.setBorder(Borders.DIALOG_BORDER);
            dialogPanel.setPreferredSize(new Dimension(900, 700));
            dialogPanel.setName("dialogPanel");
            dialogPanel.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "2*(default, $lgap), fill:default:grow, $lgap, default, $lgap, fill:default, $lgap, default"));

                //---- instructionsLabel ----
                instructionsLabel.setText("Double click a point on the image to rectify it with coordinates on the globe. There must be three or four points set.");
                instructionsLabel.setName("instructionsLabel");
                contentPanel.add(instructionsLabel, cc.xy(1, 1));

                //---- outOfBoundsLabel ----
                outOfBoundsLabel.setText("Warning: Please select a location inside the bounds of the image.");
                outOfBoundsLabel.setVisible(false);
                outOfBoundsLabel.setName("outOfBoundsLabel");
                contentPanel.add(outOfBoundsLabel, cc.xy(1, 3, CellConstraints.CENTER, CellConstraints.DEFAULT));

                //======== imageAndZoomControlPanel ========
                {
                    imageAndZoomControlPanel.setName("imageAndZoomControlPanel");
                    imageAndZoomControlPanel.setLayout(new FormLayout(
                            "default:grow, $lcgap, default",
                            "fill:default:grow"));

                    //======== imageScrollPane ========
                    {
                        imageScrollPane.setAutoscrolls(true);
                        imageScrollPane.setName("imageScrollPane");

                        //---- zoomableImagePanel ----
                        zoomableImagePanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                        zoomableImagePanel.setName("zoomableImagePanel");
                        zoomableImagePanel.addMouseWheelListener(e -> zoomableImagePanelMouseWheelMoved());
                        zoomableImagePanel.addMouseListener(new MouseAdapter()
                        {
                            @Override
                            public void mouseClicked(MouseEvent e)
                            {
                                zoomableImagePanelMouseClicked(e);
                            }

                            @Override
                            public void mousePressed(MouseEvent e)
                            {
                                zoomableImagePanelMousePressed(e);
                            }

                            @Override
                            public void mouseReleased(MouseEvent e)
                            {
                                zoomableImagePanelMouseReleased(e);
                            }
                        });
                        zoomableImagePanel.addMouseMotionListener(new MouseMotionAdapter()
                        {
                            @Override
                            public void mouseDragged(MouseEvent e)
                            {
                                zoomableImagePanelMouseDragged(e);
                            }

                            @Override
                            public void mouseMoved(MouseEvent e)
                            {
                                zoomableImagePanelMouseMoved(e);
                            }
                        });
                        zoomableImagePanel.setImage(image);
                        imageScrollPane.setViewportView(zoomableImagePanel);
                    }
                    imageAndZoomControlPanel.add(imageScrollPane, cc.xy(1, 1));

                    //======== zoomControlsPanel ========
                    {
                        zoomControlsPanel.setName("zoomControlsPanel");
                        zoomControlsPanel.setLayout(new FormLayout(
                                "default",
                                "default, fill:default:grow"));

                        //---- zoomSliderLabel ----
                        zoomSliderLabel.setText("Zoom:");
                        zoomSliderLabel.setName("zoomSliderLabel");
                        zoomControlsPanel.add(zoomSliderLabel, cc.xy(1, 1));

                        //---- zoomSlider ----
                        zoomSlider.setMaximum(decimalToPercent(zoomableImagePanel.getZoomMax()));
                        zoomSlider.setMinimum(decimalToPercent(zoomableImagePanel.getZoomMin()));
                        zoomSlider.setValue(decimalToPercent(zoomableImagePanel.getZoom()));
                        zoomSlider.setOrientation(SwingConstants.VERTICAL);
                        zoomSlider.setName("zoomSlider");
                        zoomSlider.addChangeListener(e -> zoomSliderStateChanged());
                        zoomControlsPanel.add(zoomSlider, cc.xy(1, 2));
                    }
                    imageAndZoomControlPanel.add(zoomControlsPanel, cc.xy(3, 1));
                }
                contentPanel.add(imageAndZoomControlPanel, cc.xy(1, 5));

                //---- pointsLabel ----
                pointsLabel.setText("Points:");
                pointsLabel.setName("pointsLabel");
                contentPanel.add(pointsLabel, cc.xy(1, 7));

                //======== pointsListScrollPane ========
                {
                    pointsListScrollPane.setName("pointsListScrollPane");

                    //---- pointsList ----
                    pointsList.setVisibleRowCount(4);
                    pointsList.setName("pointsList");
                    pointsList.addListSelectionListener(e -> pointsListValueChanged());
                    pointsList.addMouseListener(new MouseAdapter()
                    {
                        @Override
                        public void mouseClicked(MouseEvent e)
                        {
                            pointsListMouseClicked(e);
                        }

                        @Override
                        public void mousePressed(MouseEvent e)
                        {
                            pointsListMousePressedOrReleased(e);
                        }

                        @Override
                        public void mouseReleased(MouseEvent e)
                        {
                            pointsListMousePressedOrReleased(e);
                        }
                    });
                    pointsList.setModel(pointListModel);
                    pointsList.setCellRenderer(new ControlPointListCellRenderer());
                    pointsListScrollPane.setViewportView(pointsList);
                }
                contentPanel.add(pointsListScrollPane, cc.xy(1, 9));

                //======== editRemoveButtonPanel ========
                {
                    editRemoveButtonPanel.setName("editRemoveButtonPanel");
                    editRemoveButtonPanel.setLayout(new FormLayout(
                            "default, $lcgap, default",
                            "default"));

                    //---- removeButton ----
                    removeButton.setText("Remove Point");
                    removeButton.setEnabled(false);
                    removeButton.setToolTipText("Remove the selected point");
                    removeButton.setName("removeButton");
                    removeButton.addActionListener(e -> removeButtonActionPerformed());
                    editRemoveButtonPanel.add(removeButton, cc.xy(1, 1));

                    //---- editPointButton ----
                    editPointButton.setText("Edit Point");
                    editPointButton.setToolTipText("Edit the selected point.");
                    editPointButton.setEnabled(false);
                    editPointButton.setName("editPointButton");
                    editPointButton.addActionListener(e -> editPointButtonActionPerformed());
                    editRemoveButtonPanel.add(editPointButton, cc.xy(3, 1));
                }
                contentPanel.add(editRemoveButtonPanel, cc.xy(1, 11));
            }
            dialogPanel.add(contentPanel, BorderLayout.CENTER);

            //======== buttonPanel ========
            {
                buttonPanel.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonPanel.setName("buttonPanel");
                buttonPanel.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "pref"));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setEnabled(false);
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonPanel.add(okButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonPanel.add(cancelButton, cc.xy(4, 1));
            }
            dialogPanel.add(buttonPanel, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel outOfBoundsLabel;
    private JScrollPane imageScrollPane;
    private ZoomableImagePanel zoomableImagePanel;
    private JSlider zoomSlider;
    private JList<SurfaceImageLayerState.ControlPoint> pointsList;
    private JButton removeButton;
    private JButton editPointButton;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
