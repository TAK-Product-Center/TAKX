package gov.takx.core.view.gui.maptools.elevationtools.contour;

import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.maps.coords.GeoCalculations;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.contour.ContourStyle;
import gov.tak.platform.contour.ContourLineGroup;
import jakarta.annotation.Nonnull;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import java.awt.Color;
import java.awt.Font;
import java.util.concurrent.CompletableFuture;

/**
 * Panel representing a single {@link ContourLineGroup} in {@link ContourListPanel}.
 *
 * @since 5.8
 */
public class ContourPanel extends JPanel
{
    private final ContourLineGroup contourLineGroup;

    /**
     * @param contourLineGroup Contour group to display.
     */
    public ContourPanel(@Nonnull ContourLineGroup contourLineGroup)
    {
        this.contourLineGroup = contourLineGroup;

        initComponents();

        refreshGraphic();
        refreshLabels();

        Envelope bounds = contourLineGroup.getBoundingBox();
        GeoPoint midpoint = GeoCalculations.midPointCartesian(
                new GeoPoint(bounds.minY, bounds.minX),
                new GeoPoint(bounds.maxY, bounds.maxX),
                bounds.crossesIDL());
        String ns = midpoint.getLatitude() > 0 ? "N" : "S";
        String ew = midpoint.getLongitude() > 0 ? "E" : "W";

        locationLabel.setText(String.format("%.3f° %s, %.3f° %s", midpoint.getLatitude(), ns, midpoint.getLongitude(), ew));
    }

    /**
     * Refresh the area description labels.
     */
    public void refreshLabels()
    {
        ContourStyle style = contourLineGroup.getStyle();
        Envelope bounds = contourLineGroup.getBoundingBox();

        double minMeters = contourLineGroup.getMinElevationMeters();
        double maxMeters = contourLineGroup.getMaxElevationMeters();
        double areaMetersSquared = EnvelopeUtils.areaMetersSquared(bounds);

        Span units = style.getLabelUnits();
        elevationMinLabel.setText(style.getContourLabelTextMapper().apply(minMeters, units));
        elevationMaxLabel.setText(style.getContourLabelTextMapper().apply(maxMeters, units));
        areaLabel.setText(String.format("%,d %s²", (long) SpanUtilities.convert(areaMetersSquared, Span.METER, units), units.getPlural()));
    }

    /**
     * Refresh the 2D contour area visualization.
     */
    public void refreshGraphic()
    {
        CompletableFuture.runAsync(() -> graphicHolder.setIcon(new ImageIcon(ContourMapRenderer.renderContourMap(contourLineGroup, 96))));
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        graphicHolder = new JLabel();
        var locationPrefixLabel = new JLabel();
        locationLabel = new JLabel();
        var elevationMinPrefixLabel = new JLabel();
        elevationMinLabel = new JLabel();
        var elevationMaxPrefixLabel = new JLabel();
        elevationMaxLabel = new JLabel();
        var areaPrefixLabel = new JLabel();
        areaLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setName("this");
        setLayout(new FormLayout(
                "[96px,default], $lcgap, default, $lcgap, default:grow",
                "fill:default, 3*($lgap, default)"));

        //---- graphicHolder ----
        graphicHolder.setBorder(new LineBorder(Color.lightGray));
        graphicHolder.setName("graphicHolder");
        add(graphicHolder, cc.xywh(1, 1, 1, 7));

        //---- locationPrefixLabel ----
        locationPrefixLabel.setText("Location");
        locationPrefixLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        locationPrefixLabel.setName("locationPrefixLabel");
        add(locationPrefixLabel, cc.xy(3, 1));

        //---- locationLabel ----
        locationLabel.setText("text");
        locationLabel.setName("locationLabel");
        add(locationLabel, cc.xy(5, 1));

        //---- elevationMinPrefixLabel ----
        elevationMinPrefixLabel.setText("Min Elevation:");
        elevationMinPrefixLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        elevationMinPrefixLabel.setName("elevationMinPrefixLabel");
        add(elevationMinPrefixLabel, cc.xy(3, 3));

        //---- elevationMinLabel ----
        elevationMinLabel.setText("text");
        elevationMinLabel.setName("elevationMinLabel");
        add(elevationMinLabel, cc.xy(5, 3));

        //---- elevationMaxPrefixLabel ----
        elevationMaxPrefixLabel.setText("Max Elevation:");
        elevationMaxPrefixLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        elevationMaxPrefixLabel.setName("elevationMaxPrefixLabel");
        add(elevationMaxPrefixLabel, cc.xy(3, 5));

        //---- elevationMaxLabel ----
        elevationMaxLabel.setText("text");
        elevationMaxLabel.setName("elevationMaxLabel");
        add(elevationMaxLabel, cc.xy(5, 5));

        //---- areaPrefixLabel ----
        areaPrefixLabel.setText("Area:");
        areaPrefixLabel.setFont(areaPrefixLabel.getFont().deriveFont(areaPrefixLabel.getFont().getStyle() | Font.BOLD));
        areaPrefixLabel.setName("areaPrefixLabel");
        add(areaPrefixLabel, cc.xy(3, 7));

        //---- areaLabel ----
        areaLabel.setText("text");
        areaLabel.setName("areaLabel");
        add(areaLabel, cc.xy(5, 7));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel graphicHolder;
    private JLabel locationLabel;
    private JLabel elevationMinLabel;
    private JLabel elevationMaxLabel;
    private JLabel areaLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
