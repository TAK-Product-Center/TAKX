package gov.takx.core.view.gui.maptools;

import com.atakmap.map.elevation.ElevationData;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.commons.swing.BoundedDoubleTextField;
import gov.takx.core.globe.ElevationProfileLocationManager;
import gov.takx.core.globe.ElevationProfileService;
import gov.takx.core.globe.ViewshedManager;
import gov.takx.core.globe.tak.layer.ViewshedLayer;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.StandardTimeFormatting;
import gov.takx.platform.utils.units.AltitudeUnits;
import gov.takx.platform.utils.units.RangeUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Displays an {@link gov.takx.core.globe.ElevationProfileService.ElevationProfile}.
 *
 * @since 5.6
 * @see gov.takx.core.globe.ElevationProfileService.ElevationProfile
 */
@PseudoBean
public class ElevationProfilePanel extends ATitledPanel
{
    private final IPreferenceMonitor<AltitudeUnits> altitudeUnitsMonitor =
            UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS.newMonitor(AltitudeUnits.class, p -> onAltitudeUnitsChanged());
    private final IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor =
            UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, p -> refreshPointLabels());
    private final IPreferenceMonitor<Boolean> localTimeMonitor =
            UserPreferenceEntry.DISPLAY_LOCAL_TIME.newBooleanMonitor(b -> refreshPointLabels());
    private final IPreferenceMonitor<RangeUnits> rangeUnitsMonitor =
            UserPreferenceEntry.DISPLAY_RANGE_UNITS.newMonitor(RangeUnits.class, p -> onRangeUnitsChanged());
    private final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    @Inject
    private ICoreExecutorService executorService;
    @Inject
    private ElevationProfileLocationManager locationManager;
    @Inject
    private StandardTimeFormatting timeFormatting;
    @Inject
    private ViewshedManager viewshedManager;

    private XYSeriesCollection dataset;
    private NumberAxis domainAxis;
    private CustomMouseListener mouseListener;
    private XYPlot plot;
    private PointData pointData;
    private ElevationProfileService.ElevationProfile profile;
    private NumberAxis rangeAxis;
    private ViewshedLayer viewshed;

    public ElevationProfilePanel()
    {
        super(ButtonType.ELEVATION_PROFILER.getDisplayText());

        initComponents();
    }

    @PostConstruct
    private void postConstruct()
    {
        final var chartPanel = createChartPanel();
        plot = chartPanel.getChart().getXYPlot();

        dataset = (XYSeriesCollection) plot.getDataset();
        domainAxis = (NumberAxis) plot.getDomainAxis();
        rangeAxis = (NumberAxis) plot.getRangeAxis();

        add(chartPanel, new CellConstraints(1, 3));

        initViewshedComponents(); // uses injected fields
    }

    @Override
    public void releaseResources()
    {
        if (viewshed != null) viewshedManager.deleteViewshed(viewshed);
        altitudeUnitsMonitor.dispose();
        coordinateSystemMonitor.dispose();
        localTimeMonitor.dispose();
        rangeUnitsMonitor.dispose();
    }

    @InvokeOnEDT
    @SuppressWarnings("unused")
    public void showProfile(@Observes ElevationProfileService.ElevationProfile profile)
    {
        // Initialize the viewshed layer on first use
        if (viewshed == null) viewshed = viewshedManager.createViewshed(true);

        this.profile = profile;

        dataset.removeAllSeries();
        mouseListener.resetCrosshair();

        final var lengthInMeters = profile.length();

        final var rangeUnits = rangeUnitsMonitor.getCurrentValue();
        final var rangeMultiplier = rangeUnits.getMultiplier(lengthInMeters);
        final var altitudeUnits = altitudeUnitsMonitor.getCurrentValue();
        final var altitudeMultiplier = altitudeUnits.getMultiplier();
        final var length = lengthInMeters * rangeMultiplier;

        final List<? extends IGeoPoint> points = profile.points();
        final var pointCount = points.size();
        final var distanceBetweenPoints = length / pointCount;
        final var distanceMetersBetweenPoints = lengthInMeters / pointCount;

        int index = 0;
        double gainMeters = 0;
        double lastAltitudeMeters = 0;
        double totalGainMeters = 0;
        double totalLossMeters = 0;

        final var series = new XYSeries("Elevation Series");

        for (int i = 0; i < pointCount; i++)
        {
            final var first = i == 0;
            final var last = i == pointCount - 1;

            final IGeoPoint point = points.get(i);
            var altitudeMeters = point.getAltitude();
            final var validData = !Double.isNaN(altitudeMeters);
            altitudeMeters = validData ? altitudeMeters : 0;
            final var altitudeChangeMeters = altitudeMeters - lastAltitudeMeters;
            gainMeters += first ? 0 : validData ? altitudeChangeMeters : gainMeters;
            final var slopePercent = first ? 0 : (int) ((altitudeChangeMeters / distanceMetersBetweenPoints) * 100);

            if (altitudeChangeMeters > 0) totalGainMeters += altitudeChangeMeters;
            if (altitudeChangeMeters < 0) totalLossMeters += Math.abs(altitudeChangeMeters);

            series.add(new PointData(point, i * distanceBetweenPoints, altitudeMeters * altitudeMultiplier,
                    // Generate the point label Strings here once, not dynamically/repeatedly as the user scrolls
                    validData ? altitudeUnits.format(altitudeMeters, 2) + " MSL" : "(unknown)",
                    (gainMeters < 0 ? "Loss: " : "Gain: ") + altitudeUnits.format(Math.abs(gainMeters), 2),
                    first ? "Start" : last ? "End" : "--",
                    first ? profile.start() : last ? profile.end() : null,
                    "Slope: " + slopePercent + "%"));

            lastAltitudeMeters = altitudeMeters;
        }

        plot.setDomainGridlinesVisible(true);
        domainAxis.setTickLabelsVisible(true);
        domainAxis.setLowerBound(0);
        domainAxis.setUpperBound(length + length * 0.02); // add padding
        domainAxis.setLabel("Distance From Start [" + rangeUnits.getAbbreviation(lengthInMeters) + "]");

        final var minElevationMeters = profile.minElevation();
        final var maxElevationMeters = profile.maxElevation();
        final var minElevation = minElevationMeters * altitudeMultiplier;
        final var maxElevation = maxElevationMeters * altitudeMultiplier;
        final var span = maxElevation - minElevation;

        rangeAxis.setLabel("Elevation [" + altitudeUnits.getAbbreviation() + " MSL]");
        rangeAxis.setTickUnit(new NumberTickUnit(Math.round(span / 400) * 100));
        rangeAxis.setLowerBound(minElevation);
        rangeAxis.setUpperBound(maxElevation + (span * .05)); // add padding

        dataset.addSeries(series);

        totalDistanceLabel.setText(rangeUnits.format(lengthInMeters, 2));
        maxElevationLabel.setText(altitudeUnits.format(maxElevationMeters, 2) + " MSL");
        minElevationLabel.setText(altitudeUnits.format(minElevationMeters, 2) + " MSL");
        totalGainLabel.setText(altitudeUnits.format(totalGainMeters, 0));
        totalLossLabel.setText(altitudeUnits.format(totalLossMeters, 0));

        locationManager.setVisible(true);
        setSelectedPoint((PointData) series.getDataItem(0), false);
    }

    /**
     * Updates the point displayed on the map and the data displayed in the chart labels according to the current
     * position of the domain crosshair.
     *
     * @param pointData Data about the point currently selected by the domain crosshair
     * @param updating Pass {@code true} if the location is actively being updated by the user
     * @see ElevationProfileLocationManager
     */
    private void setSelectedPoint(@Nonnull PointData pointData, boolean updating)
    {
        this.pointData = pointData;

        executorService.execute(() -> {
            Thread.currentThread().setName("Elevation Profile Location and Viewshed Updater");
            locationManager.setLocation(pointData.point);
            viewshed.setPointOfInterest(pointData.point, updating);
        });

        updatePointLabels(pointData);
    }

    /**
     * @param pointData Data about the point currently selected by the domain crosshair
     */
    private void updatePointLabels(@Nonnull PointData pointData)
    {
        final var point = pointData.point;
        final var coordinateSystem = coordinateSystemMonitor.getCurrentValue();
        pointLabel.setText(CoordinateSystem.getLocationString(point.getLatitude(), point.getLongitude(), coordinateSystem));
        altitudeLabel.setText(pointData.altitudeString);
        timeLabel.setText(pointData.time != null ? timeFormatting.formatDateTimeWithCurrent(pointData.time.toEpochMilli()) : "--");
        gainLabel.setText(pointData.gainString);
        otherLabel.setText(pointData.otherString);
        slopeLabel.setText(pointData.slopeString);
    }

    /**
     * Updates the viewshed height units and refreshes the graph.
     *
     * @since 5.6
     */
    private void onAltitudeUnitsChanged()
    {
        heightUnitsComboBox.setSelectedItem(altitudeUnitsMonitor.getCurrentValue());
        if (profile != null) showProfile(profile);
    }

    /**
     * Updates the viewshed range units and refreshes the graph.
     *
     * @since 5.6
     */
    private void onRangeUnitsChanged()
    {
        radiusUnitsComboBox.setSelectedItem(rangeUnitsMonitor.getCurrentValue());
        if (profile != null) showProfile(profile);
    }

    /**
     * Updates the point label values.
     */
    private void refreshPointLabels()
    {
        if (pointData != null) updatePointLabels(pointData);
    }

    private ChartPanel createChartPanel()
    {
        final Color background = getBackground();
        final Color foreground = getForeground();
        final Color seriesColor = Color.GREEN;

        final var renderer = new XYSplineRenderer();
        renderer.setDefaultFillPaint(seriesColor);
        renderer.setDefaultShapesVisible(false);
        renderer.setFillType(XYSplineRenderer.FillType.TO_ZERO);
        renderer.setUseFillPaint(true);
        renderer.setSeriesPaint(0, seriesColor);
        renderer.setSeriesFillPaint(0, seriesColor);
        renderer.setDefaultToolTipGenerator(null); // improves performance

        final var dataset = new XYSeriesCollection();

        final var chart = ChartFactory.createXYLineChart(null, "Distance From Start", "Elevation", dataset);
        chart.setBackgroundPaint(background);
        chart.getLegend().setVisible(false);

        final var plot = (XYPlot) chart.getPlot();
        plot.setDomainPannable(true);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlineStroke(new BasicStroke());
        plot.setDomainGridlinePaint(foreground.darker());
        plot.setBackgroundPaint(background);
        plot.setRenderer(renderer);

        final var domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setTickLabelsVisible(false);
        domainAxis.setTickMarksVisible(false);
        domainAxis.setLabelPaint(foreground);
        domainAxis.setTickLabelPaint(foreground);
        domainAxis.setNumberFormatOverride(DECIMAL_FORMAT);

        final var rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickMarksVisible(false);
        rangeAxis.setLabelPaint(foreground);
        rangeAxis.setTickLabelPaint(foreground);
        rangeAxis.setNumberFormatOverride(DECIMAL_FORMAT);

        final var domainCrosshair = new Crosshair(0.0);
        domainCrosshair.setVisible(true);
        domainCrosshair.setPaint(foreground);

        final var crosshairOverlay = new CustomCrosshairOverlay(domainCrosshair);
        crosshairOverlay.addDomainCrosshair(domainCrosshair);

        final var chartPanel = new ChartPanel(chart);
        chartPanel.setDisplayToolTips(false); // improves performance
        chartPanel.setMouseZoomable(false);
        chartPanel.setZoomInFactor(0.8);
        chartPanel.setZoomOutFactor(1.2);

        chartPanel.setRangeZoomable(false);
        chartPanel.addOverlay(crosshairOverlay);

        mouseListener = new CustomMouseListener(chartPanel, domainCrosshair);
        chartPanel.addMouseListener(mouseListener);
        chartPanel.addMouseMotionListener(mouseListener);
        chartPanel.addMouseWheelListener(mouseListener);

        return chartPanel;
    }

    /**
     * Initializes the viewshed UI components.
     *
     * @since 5.6
     */
    private void initViewshedComponents()
    {
        viewShedPanel.setVisible(false);
        viewShedCheckBox.addActionListener(e -> {
            final var selected = viewShedCheckBox.isSelected();
            viewShedPanel.setVisible(selected);
            viewshed.setVisible(selected);
        });

        heightUnitsComboBox.setComponentAccessors(heightTextField::getDoubleValue, heightTextField::setValue);
        heightUnitsComboBox.setSelectedItem(altitudeUnitsMonitor.getCurrentValue());
        heightTextField.setBounds(0d, null);
        heightUnitsComboBox.fromMeters(ViewshedLayer.DEFAULT_VIEWSHED_HEIGHT);
        heightTextField.setChangeListener(d -> {
            if (viewshed != null) viewshed.setViewHeightAboveMarker(heightUnitsComboBox.toMeters());
        });

        radiusUnitsComboBox.setComponentAccessors(radiusTextField::getDoubleValue, radiusTextField::setValue);
        radiusUnitsComboBox.setSelectedItem(rangeUnitsMonitor.getCurrentValue());
        radiusTextField.setBounds(0.1, null);
        radiusTextField.setChangeListener(d -> {
            if (viewshed != null) viewshed.setRadius(radiusUnitsComboBox.toMeters());
        });

        intensitySlider.setValue(ViewshedLayer.DEFAULT_VIEWSHED_INTENSITY);
        intensityLabel.setText(ViewshedLayer.DEFAULT_VIEWSHED_INTENSITY + "%");
        intensitySlider.addChangeListener(e -> {
            final var value = intensitySlider.getValue();
            viewshed.setOpacity(value);
            intensityLabel.setText(value + "%");
        });

        terrainRadioButton.addItemListener(e -> {
            //noinspection deprecation
            viewshed.setElevationModel(terrainRadioButton.isSelected()
                    ? ElevationData.MODEL_TERRAIN
                    : ElevationData.MODEL_SURFACE);
        });
    }

    /**
     * Add support for moving the domain crosshair, dragging to pan and using the scroll wheel to zoom.
     */
    private class CustomMouseListener extends MouseAdapter
    {
        private final ChartPanel chartPanel;
        private final Crosshair domainCrosshair;
        private boolean draggingHandle;
        private Point lastClickPoint;
        private PointData lastCrosshairPoint;

        public CustomMouseListener(@Nonnull ChartPanel chartPanel, @Nonnull Crosshair domainCrosshair)
        {
            this.chartPanel = chartPanel;
            this.domainCrosshair = domainCrosshair;
        }

        /**
         * Restore the domain crosshair to the zero position
         */
        public void resetCrosshair()
        {
            domainCrosshair.setValue(0);
        }

        @Override
        public void mousePressed(MouseEvent event)
        {
            // No data or not left button: bail
            if (plot.getSeriesCount() == 0 || !SwingUtilities.isLeftMouseButton(event)) return;

            lastClickPoint = event.getPoint();

            final var domainEdge = plot.getDomainAxisEdge();
            final var domainAxis = plot.getDomainAxis();

            // Determine if the mouse click is near the crosshair
            double crosshairX = domainAxis.valueToJava2D(domainCrosshair.getValue(), chartPanel.getScreenDataArea(), domainEdge);
            if (Math.abs(event.getX() - crosshairX) < 10) draggingHandle = true;
        }

        @Override
        public void mouseDragged(MouseEvent event)
        {
            // No data, not left button, not dragging, not panning: bail
            if (plot.getSeriesCount() == 0 || !SwingUtilities.isLeftMouseButton(event) || (!draggingHandle && lastClickPoint == null)) return;

            //noinspection unchecked
            final List<PointData> pointDataList = (((XYSeriesCollection) plot.getDataset()).getSeries(0)).getItems();
            final RectangleEdge domainEdge = plot.getDomainAxisEdge();
            final ValueAxis domainAxis = plot.getDomainAxis();
            final Rectangle2D screenDataArea = chartPanel.getScreenDataArea();
            final int eventX = event.getX();

            final double domainValue;

            // Drag crosshair
            if (draggingHandle)
            {
                final var domainRange = domainAxis.getRange();

                // Mouse coordinate to domain value, constrained by visible min/max
                domainValue = domainRange.constrain(domainAxis.java2DToValue(eventX, screenDataArea, domainEdge));

                moveCrosshair(domainValue, pointDataList);
            }
            // Pan chart
            else
            {
                final double crosshairX = domainAxis.valueToJava2D(domainCrosshair.getValue(), screenDataArea, domainEdge);
                final double dx = eventX - lastClickPoint.getX();

                // Calculate percentage of movement relative to plot area
                final PlotRenderingInfo plotInfo = chartPanel.getChartRenderingInfo().getPlotInfo();
                final Rectangle2D dataArea = plotInfo.getDataArea();
                final double domainPercent = -dx / dataArea.getWidth();

                // Move chart
                plot.panDomainAxes(domainPercent, plotInfo, lastClickPoint);

                // Move crosshair to keep its location fixed relative to the chart
                moveCrosshair(crosshairX, pointDataList, screenDataArea, domainEdge);

                lastClickPoint = event.getPoint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            draggingHandle = false;
            lastClickPoint = null;

            if (lastCrosshairPoint != null)
            {
                setSelectedPoint(lastCrosshairPoint, false);
                lastCrosshairPoint = null;
            }
        }

        /**
         * {@inheritDoc}
         * <p>
         * Responds to wheel movements by zooming in or out of the chart while maintaining the crosshair position
         * relative to the chart boundaries (not the domain value).
         *
         * @param e the event to be processed
         */
        @Override
        public void mouseWheelMoved(MouseWheelEvent e)
        {
            // No data: bail
            if (plot.getSeriesCount() == 0) return;

            final int rotation = e.getWheelRotation();

            if (rotation == 0) return; // This can happen; see getWheelRotation() JavaDoc

            final Rectangle2D screenDataArea = chartPanel.getScreenDataArea();
            final RectangleEdge domainEdge = plot.getDomainAxisEdge();
            final double crosshairX = domainAxis.valueToJava2D(domainCrosshair.getValue(), screenDataArea, domainEdge);
            //noinspection unchecked
            final List<PointData> pointDataList = (((XYSeriesCollection) plot.getDataset()).getSeries(0)).getItems();

            if (rotation > 0)
            {
                chartPanel.zoomOutDomain(0.5, 0.5);
            } else
            {
                chartPanel.zoomInDomain(1.5, 1.5);
            }

            // Move crosshair to keep its location fixed relative to the chart
            moveCrosshair(crosshairX, pointDataList, screenDataArea, domainEdge);
        }

        /**
         * Moves the crosshair to the specified x coordinate and updates the corresponding {@link IGeoPoint point}
         * visuals.
         * 
         * @param crosshairX The crosshair's x coordinate 
         * @param pointDataList The list of points in the series
         * @param screenDataArea The chart rectangle
         * @param domainEdge The domain's edge info
         * @see #setSelectedPoint(PointData,boolean)
         */
        private void moveCrosshair(double crosshairX, @Nonnull List<PointData> pointDataList,
                                   @Nonnull Rectangle2D screenDataArea, @Nonnull RectangleEdge domainEdge)
        {
            // Translate the desired x coordinate to its domain value and update
            moveCrosshair(domainAxis.java2DToValue(crosshairX, screenDataArea, domainEdge), pointDataList);
        }

        /**
         * Moves the crosshair to the specified domain value and updates the corresponding {@link IGeoPoint point}
         * visuals.
         *
         * @param domainValue The domain value  
         * @param pointDataList The list of points in the series
         * @see #setSelectedPoint(PointData,boolean)
         */
        private void moveCrosshair(double domainValue, @Nonnull List<PointData> pointDataList)
        {
            domainCrosshair.setValue(domainValue);

            final PointData closestPoint = findClosest(pointDataList, domainValue);
            if (closestPoint != null)
            {
                lastCrosshairPoint = closestPoint;
                setSelectedPoint(closestPoint, true);
            }
        }

        /**
         * @param sortedList  The list of points, ordered by domain value
         * @param domainValue The target value
         * @return The {@link PointData point} closest to the domain value
         */
        @Nullable
        private PointData findClosest(@Nonnull List<PointData> sortedList, double domainValue)
        {
            if (sortedList.isEmpty()) return null;

            final Comparator<PointData> comparator = Comparator.comparingDouble(pd -> pd.getX().doubleValue());
            final var searchKey = new PointData(GeoPoint.UNKNOWN_POINT, domainValue, 0, "", "", "", null, "");

            int index = Collections.binarySearch(sortedList, searchKey, comparator);

            if (index >= 0) return sortedList.get(index); // match

            int insertionPoint = -(index + 1);

            if (insertionPoint == 0) return sortedList.get(0); // before first
            if (insertionPoint == sortedList.size()) return sortedList.get(sortedList.size() - 1); // after last

            // Compare the points before and after the insertion point to determine which is closer
            final var prev = sortedList.get(insertionPoint - 1);
            final var next = sortedList.get(insertionPoint);

            return Math.abs(domainValue - prev.getX().doubleValue()) <= Math.abs(domainValue - next.getX().doubleValue()) ? prev : next;
        }
    }

    /**
     * Custom crosshair overlay that draws a domain crosshair with a handle at the top.
     */
    private static class CustomCrosshairOverlay extends CrosshairOverlay
    {
        private static final Color COLOR = UIResources.BORDER_HIGHLIGHT_COLOR;
        private static final Color CYAN_WITH_ALPHA =
                new Color(COLOR.getRed(), COLOR.getGreen(), COLOR.getBlue(), 50);
        private final Crosshair domainCrosshair;

        public CustomCrosshairOverlay(@Nonnull Crosshair domainCrosshair)
        {
            this.domainCrosshair = domainCrosshair;
            addDomainCrosshair(domainCrosshair);
        }

        @Override
        public void paintOverlay(Graphics2D g2, ChartPanel chartPanel)
        {
            super.paintOverlay(g2, chartPanel);

            if (domainCrosshair.isVisible())
            {
                XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
                ValueAxis domainAxis = plot.getDomainAxis();
                RectangleEdge domainEdge = plot.getDomainAxisEdge();
                Rectangle2D dataArea = chartPanel.getScreenDataArea();

                double x = domainAxis.valueToJava2D(domainCrosshair.getValue(), dataArea, domainEdge);
                double y = dataArea.getMinY();

                // Draw a handle's background
                g2.setPaint(CYAN_WITH_ALPHA);
                g2.fillOval((int) x - 15, (int) y, 30, 30);

                // Draw a handle's foreground
                g2.setPaint(COLOR);
                g2.fillOval((int) x - 8, (int) y + 8, 16, 16);
            }
        }
    }

    /**
     * Extends {@link XYDataItem} to include the original {@link IGeoPoint point} and additional metadata that is used
     * to update the UI as the user scrolls the crosshair through the domain.
     */
    private static class PointData extends XYDataItem
    {
        private final IGeoPoint point;
        private final String altitudeString;
        private final String gainString;
        private final String otherString;
        private final Instant time;
        private final String slopeString;

        public PointData(@Nonnull IGeoPoint point, double x, double y, @Nonnull String altitudeString,
                         @Nonnull String gainString, @Nonnull String otherString, @Nullable Instant time,
                         @Nonnull String slopeString)
        {
            super(x, y);
            this.point = point;
            this.altitudeString = altitudeString;
            this.gainString = gainString;
            this.otherString = otherString;
            this.time = time;
            this.slopeString = slopeString;
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var pointDetailsPanel = new JPanel();
        pointLabel = new JLabel();
        altitudeLabel = new JLabel();
        timeLabel = new JLabel();
        gainLabel = new JLabel();
        slopeLabel = new JLabel();
        otherLabel = new JLabel();
        var pathDetailsPanel = new JPanel();
        var label1 = new JLabel();
        totalDistanceLabel = new JLabel();
        var label2 = new JLabel();
        maxElevationLabel = new JLabel();
        var label3 = new JLabel();
        minElevationLabel = new JLabel();
        var label4 = new JLabel();
        totalGainLabel = new JLabel();
        var label5 = new JLabel();
        totalLossLabel = new JLabel();
        viewShedCheckBox = new JCheckBox();
        viewShedPanel = new JPanel();
        var label6 = new JLabel();
        heightTextField = new BoundedDoubleTextField();
        heightUnitsComboBox = new UnitOfMeasureComboBox();
        var label7 = new JLabel();
        radiusTextField = new BoundedDoubleTextField();
        radiusUnitsComboBox = new UnitOfMeasureComboBox();
        var label9 = new JLabel();
        terrainRadioButton = new JRadioButton();
        var surfaceRadioButton = new JRadioButton();
        var label8 = new JLabel();
        var intensitySliderPanel = new JPanel();
        intensitySlider = new JSlider();
        intensityLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(Borders.DLU4_BORDER);
        setName("this");
        setLayout(new FormLayout(
                "default:grow, $lcgap, 140dlu",
                "default, $rgap, fill:default:grow"));

        //======== pointDetailsPanel ========
        {
            pointDetailsPanel.setName("pointDetailsPanel");
            pointDetailsPanel.setLayout(new FormLayout(
                    "5*(default:grow, $lcgap), default:grow",
                    "default"));

            //---- pointLabel ----
            pointLabel.setText("--");
            pointLabel.setHorizontalAlignment(SwingConstants.CENTER);
            pointLabel.setName("pointLabel");
            pointDetailsPanel.add(pointLabel, cc.xy(1, 1));

            //---- altitudeLabel ----
            altitudeLabel.setText("--");
            altitudeLabel.setHorizontalAlignment(SwingConstants.CENTER);
            altitudeLabel.setName("altitudeLabel");
            pointDetailsPanel.add(altitudeLabel, cc.xy(3, 1));

            //---- timeLabel ----
            timeLabel.setText("--");
            timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
            timeLabel.setName("timeLabel");
            pointDetailsPanel.add(timeLabel, cc.xy(5, 1));

            //---- gainLabel ----
            gainLabel.setText("--");
            gainLabel.setHorizontalAlignment(SwingConstants.CENTER);
            gainLabel.setName("gainLabel");
            pointDetailsPanel.add(gainLabel, cc.xy(7, 1));

            //---- slopeLabel ----
            slopeLabel.setText("--");
            slopeLabel.setHorizontalAlignment(SwingConstants.CENTER);
            slopeLabel.setName("slopeLabel");
            pointDetailsPanel.add(slopeLabel, cc.xy(9, 1));

            //---- otherLabel ----
            otherLabel.setText("--");
            otherLabel.setHorizontalAlignment(SwingConstants.CENTER);
            otherLabel.setName("otherLabel");
            pointDetailsPanel.add(otherLabel, cc.xy(11, 1));
        }
        add(pointDetailsPanel, cc.xywh(1, 1, 3, 1));

        //======== pathDetailsPanel ========
        {
            pathDetailsPanel.setBorder(Borders.DLU7_BORDER);
            pathDetailsPanel.setName("pathDetailsPanel");
            pathDetailsPanel.setLayout(new FormLayout(
                    "default, $lcgap, 30dlu:grow",
                    "6*(default, $lgap), default"));

            //---- label1 ----
            label1.setText("Total Distance");
            label1.setHorizontalAlignment(SwingConstants.TRAILING);
            label1.setName("label1");
            pathDetailsPanel.add(label1, cc.xy(1, 1));

            //---- totalDistanceLabel ----
            totalDistanceLabel.setText("--");
            totalDistanceLabel.setName("totalDistanceLabel");
            pathDetailsPanel.add(totalDistanceLabel, cc.xy(3, 1));

            //---- label2 ----
            label2.setText("Max Elevation");
            label2.setHorizontalAlignment(SwingConstants.TRAILING);
            label2.setName("label2");
            pathDetailsPanel.add(label2, cc.xy(1, 3));

            //---- maxElevationLabel ----
            maxElevationLabel.setText("--");
            maxElevationLabel.setName("maxElevationLabel");
            pathDetailsPanel.add(maxElevationLabel, cc.xy(3, 3));

            //---- label3 ----
            label3.setText("Min Elevation");
            label3.setHorizontalAlignment(SwingConstants.TRAILING);
            label3.setName("label3");
            pathDetailsPanel.add(label3, cc.xy(1, 5));

            //---- minElevationLabel ----
            minElevationLabel.setText("--");
            minElevationLabel.setName("minElevationLabel");
            pathDetailsPanel.add(minElevationLabel, cc.xy(3, 5));

            //---- label4 ----
            label4.setText("Gain");
            label4.setHorizontalAlignment(SwingConstants.TRAILING);
            label4.setName("label4");
            pathDetailsPanel.add(label4, cc.xy(1, 7));

            //---- totalGainLabel ----
            totalGainLabel.setText("--");
            totalGainLabel.setName("totalGainLabel");
            pathDetailsPanel.add(totalGainLabel, cc.xy(3, 7));

            //---- label5 ----
            label5.setText("Loss");
            label5.setHorizontalAlignment(SwingConstants.TRAILING);
            label5.setName("label5");
            pathDetailsPanel.add(label5, cc.xy(1, 9));

            //---- totalLossLabel ----
            totalLossLabel.setText("--");
            totalLossLabel.setName("totalLossLabel");
            pathDetailsPanel.add(totalLossLabel, cc.xy(3, 9));

            //---- viewShedCheckBox ----
            viewShedCheckBox.setText("Show Viewshed");
            viewShedCheckBox.setToolTipText("Display the elevation viewshed heatmap");
            viewShedCheckBox.setName("viewShedCheckBox");
            pathDetailsPanel.add(viewShedCheckBox, cc.xywh(1, 11, 3, 1));

            //======== viewShedPanel ========
            {
                viewShedPanel.setName("viewShedPanel");
                viewShedPanel.setLayout(new FormLayout(
                        "2*(40dlu, $lcgap), 40dlu",
                        "3*(default, $lgap), default"));

                //---- label6 ----
                label6.setText("<html>Height<br>above path");
                label6.setHorizontalAlignment(SwingConstants.TRAILING);
                label6.setName("label6");
                viewShedPanel.add(label6, cc.xy(1, 1));

                //---- heightTextField ----
                heightTextField.setText("2");
                heightTextField.setName("heightTextField");
                viewShedPanel.add(heightTextField, cc.xy(3, 1));

                //---- heightUnitsComboBox ----
                heightUnitsComboBox.setName("heightUnitsComboBox");
                viewShedPanel.add(heightUnitsComboBox, cc.xy(5, 1));

                //---- label7 ----
                label7.setText("Radius");
                label7.setHorizontalAlignment(SwingConstants.TRAILING);
                label7.setName("label7");
                viewShedPanel.add(label7, cc.xy(1, 3));

                //---- radiusTextField ----
                radiusTextField.setText("3000");
                radiusTextField.setName("radiusTextField");
                viewShedPanel.add(radiusTextField, cc.xy(3, 3));

                //---- radiusUnitsComboBox ----
                radiusUnitsComboBox.setName("radiusUnitsComboBox");
                viewShedPanel.add(radiusUnitsComboBox, cc.xy(5, 3));

                //---- label9 ----
                label9.setText("Source");
                label9.setHorizontalAlignment(SwingConstants.TRAILING);
                label9.setName("label9");
                viewShedPanel.add(label9, cc.xy(1, 5));

                //---- terrainRadioButton ----
                terrainRadioButton.setText("Terrain");
                terrainRadioButton.setSelected(true);
                terrainRadioButton.setName("terrainRadioButton");
                viewShedPanel.add(terrainRadioButton, cc.xy(3, 5));

                //---- surfaceRadioButton ----
                surfaceRadioButton.setText("Surface");
                surfaceRadioButton.setName("surfaceRadioButton");
                viewShedPanel.add(surfaceRadioButton, cc.xy(5, 5));

                //---- label8 ----
                label8.setText("Intensity");
                label8.setHorizontalAlignment(SwingConstants.TRAILING);
                label8.setName("label8");
                viewShedPanel.add(label8, cc.xy(1, 7));

                //======== intensitySliderPanel ========
                {
                    intensitySliderPanel.setName("intensitySliderPanel");
                    intensitySliderPanel.setLayout(new FormLayout(
                            "default:grow, $lcgap, 20dlu",
                            "default"));

                    //---- intensitySlider ----
                    intensitySlider.setName("intensitySlider");
                    intensitySliderPanel.add(intensitySlider, cc.xy(1, 1));

                    //---- intensityLabel ----
                    intensityLabel.setText("100%");
                    intensityLabel.setName("intensityLabel");
                    intensitySliderPanel.add(intensityLabel, cc.xy(3, 1));
                }
                viewShedPanel.add(intensitySliderPanel, cc.xywh(3, 7, 3, 1));
            }
            pathDetailsPanel.add(viewShedPanel, cc.xywh(1, 13, 3, 1));
        }
        add(pathDetailsPanel, cc.xy(3, 3));

        //---- sourceButtonGroup ----
        var sourceButtonGroup = new ButtonGroup();
        sourceButtonGroup.add(terrainRadioButton);
        sourceButtonGroup.add(surfaceRadioButton);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel pointLabel;
    private JLabel altitudeLabel;
    private JLabel timeLabel;
    private JLabel gainLabel;
    private JLabel slopeLabel;
    private JLabel otherLabel;
    private JLabel totalDistanceLabel;
    private JLabel maxElevationLabel;
    private JLabel minElevationLabel;
    private JLabel totalGainLabel;
    private JLabel totalLossLabel;
    private JCheckBox viewShedCheckBox;
    private JPanel viewShedPanel;
    private BoundedDoubleTextField heightTextField;
    private UnitOfMeasureComboBox heightUnitsComboBox;
    private BoundedDoubleTextField radiusTextField;
    private UnitOfMeasureComboBox radiusUnitsComboBox;
    private JRadioButton terrainRadioButton;
    private JSlider intensitySlider;
    private JLabel intensityLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
