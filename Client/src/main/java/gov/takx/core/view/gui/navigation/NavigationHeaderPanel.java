package gov.takx.core.view.gui.navigation;

import com.atakmap.math.MathUtils;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.maps.Marker;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeArrowMapObject;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.mapobject.gui.MapObjectField;
import gov.takx.core.navigation.NavigationTarget;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nullable;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.net.URL;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * Header for the navigation panel, includes heading and speed information, and the compass image.
 *
 * @since 3.4 (significant redesign in 5.7)
 */
@PseudoBean
public class NavigationHeaderPanel extends JPanel implements IResourceCleaner
{
    private static final URL COMPASS_BACKGROUND_ICON = UIResources.getUrl(PLATFORM_ICONS, "compass.png");
    private static final URL COMPASS_ARROW_ICON = UIResources.getUrl(PLATFORM_ICONS, "arrow-offset.png");

    private final HeaderArrowDetailsUpdater arrowDetailsUpdater;
    private NavigationTarget target;
    private int compassSize;
    private int bearingArrowSize;
    private int headingArrowSize;
    private BufferedImage headingImage;
    private BufferedImage bearingImage;
    private boolean rotateSourceImage;

    /**
     * Construct a new panel with a compass size of {@code 150px}.
     */
    public NavigationHeaderPanel()
    {
        initComponents();
        setCompassSize(150);
        arrowDetailsUpdater = new HeaderArrowDetailsUpdater(null, bearingValue, distanceValue, relBearingValue, headingValue, etaValue, speedValue);
    }

    /**
     * @since 5.7
     */
    @Override
    public void releaseResources()
    {
        setTarget(null);
        arrowDetailsUpdater.releaseResources();
    }

    /**
     * @param target the navigation target to display properties for
     * @since 5.7
     */
    @InvokeOnEDT
    public void setTarget(@Nullable NavigationTarget target)
    {
        if (this.target == target) return;
        this.target = target;
        if (target == null)
        {
            sourceField.setSelectedItem(null);
            targetField.setSelectedItem(null);
            arrowDetailsUpdater.setArrow(null);
            headingImage = null;
        } else
        {
            var hasIntercept = target.hasIntercept();
            interceptField.setVisible(hasIntercept);
            interceptField.setSelected(hasIntercept);
            var arrow = hasIntercept ? target.getInterceptArrow() : target.getTargetArrow();
            var sourceMapObject = target.getNavigation().getSourceMapObject();
            var style = sourceMapObject instanceof Marker marker ? marker.getStyle() : 0;
            rotateSourceImage = MathUtils.hasBits(style, Marker.STYLE_ROTATE_HEADING_MASK) && MathUtils.hasBits(style, Marker.STYLE_ROTATE_HEADING_NOARROW_MASK);
            sourceField.setSelectedItem(sourceMapObject);
            //noinspection DataFlowIssue one arrow will always exist
            targetField.setSelectedItem(arrow.getRangeAnchor());
            headingImage = (BufferedImage) sourceMapObject.getSizedIcon(headingArrowSize).getImage();
            arrowDetailsUpdater.setArrow(arrow);
        }
    }

    /**
     * Sets the compass size. This could be exposed as a property to be set in JFD if we ever need to use this panel in other
     * places.
     */
    private void setCompassSize(int size)
    {
        compassSize = size;
        bearingArrowSize = (int) (size * .55);
        headingArrowSize = (int) (size * .15);

        var compassImage = ImageUtils.getScaledImage(COMPASS_BACKGROUND_ICON, size, size, true);
        //noinspection DataFlowIssue
        backgroundIcon.setIcon(new ImageIcon(compassImage));
        bearingImage = ImageUtils.getScaledImage(COMPASS_ARROW_ICON, bearingArrowSize, bearingArrowSize, true);

        // Have to manually set bounds when using JLayeredPane
        headingIcon.setBounds(0, 0, size, size);
        bearingIcon.setBounds(0, 0, size, size);
        backgroundIcon.setBounds(0, 0, size, size);
        compass.setPreferredSize(new Dimension(size, size));
    }

    private void drawSourceImage(double heading)
    {
        Icon icon;
        if (Double.isNaN(heading)) heading = 0;
        var image = new BufferedImage(compassSize, compassSize, TYPE_INT_ARGB);
        var g = image.createGraphics();
        var userOffset = (compassSize - headingArrowSize) / 2;
        var drawHeaderImage = headingImage;

        if (rotateSourceImage)
        {
            // Need to account for corners getting cut off due to rotation on some icons
            var angle = Math.toRadians(heading);
            var sin = Math.abs(Math.sin(angle));
            var cos = Math.abs(Math.cos(angle));
            var newSize = (int) Math.floor(headingArrowSize * cos + headingArrowSize * sin);
            userOffset = (compassSize - newSize) / 2;
            var newOrigin = newSize / 2.0;
            var corner = -headingArrowSize / 2;
            var rotated = new BufferedImage(newSize, newSize, TYPE_INT_ARGB);
            var g2d = rotated.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.translate(newOrigin, newOrigin);
            g2d.rotate(angle);
            g2d.drawImage(headingImage, corner, corner, null);
            g2d.dispose();
            drawHeaderImage = rotated;
        }
        g.drawImage(drawHeaderImage, userOffset, userOffset, null);
        icon = new ImageIcon(image);
        headingIcon.setIcon(icon);
    }

    private void drawBearingImage(double bearing)
    {
        Icon icon = null;
        if (!Double.isNaN(bearing))
        {
            var image = new BufferedImage(compassSize, compassSize, TYPE_INT_ARGB);
            var g = image.createGraphics();
            var arrowOffset = (compassSize - bearingArrowSize) / 2;
            var bearingTransform = AffineTransform.getRotateInstance(Math.toRadians(bearing), bearingArrowSize / 2.0, bearingArrowSize / 2.0);
            var bearingTransformOp = new AffineTransformOp(bearingTransform, AffineTransformOp.TYPE_BILINEAR);
            var transposedBearingImage = bearingTransformOp.filter(bearingImage, null);
            g.drawImage(transposedBearingImage, arrowOffset, arrowOffset, null);
            icon = new ImageIcon(image);
        }
        bearingIcon.setIcon(icon);
    }

    private void interceptFieldActionPerformed()
    {
        var useIntercept = interceptField.isSelected();
        var newArrow = useIntercept ? target.getInterceptArrow() : target.getTargetArrow();
        arrowDetailsUpdater.setArrow(newArrow);
    }

    /**
     * Overrides intercept arrow details with empty strings when target is unreachable.
     *
     * @since 5.7
     */
    private class HeaderArrowDetailsUpdater extends ArrowDetailsUpdater
    {
        public HeaderArrowDetailsUpdater(IShapeArrowMapObject arrow, JLabel bearingLabel, JLabel rangeLabel, JLabel relativeBearingLabel, JLabel headingLabel, JLabel etaLabel, JLabel speedLabel)
        {
            super(arrow, bearingLabel, rangeLabel, relativeBearingLabel, headingLabel, etaLabel, speedLabel);
        }

        @SuppressWarnings("DataFlowIssue") // Arrow should never be null in this method
        @Override
        @InvokeOnEDT
        public void update()
        {
            super.update();
            if (target != null)
            {
                if (!target.canIntercept() && interceptField.isSelected())
                {
                    etaValue.setText("");
                    bearingValue.setText("");
                    relBearingValue.setText("");
                    distanceValue.setText("");
                    drawBearingImage(Double.NaN);
                } else
                {
                    drawBearingImage(getArrow().getBearing());
                }
                drawSourceImage(getArrow().getStartAnchor().getHeading());
            }
        }
    }

    @SuppressWarnings("RedundantExplicitVariableType")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var contentPanel = new JPanel();
        var detailsPanel = new JPanel();
        var headingPanel = new JPanel();
        var sourceLabel = new JLabel();
        sourceField = new MapObjectField<>();
        var headingLabel = new JLabel();
        headingValue = new JLabel();
        var speedLabel = new JLabel();
        speedValue = new JLabel();
        var separator = new JSeparator();
        var targetLabel = new JLabel();
        var targetPanel = new JPanel();
        targetField = new MapObjectField<>();
        interceptField = new JCheckBox();
        var relBearingLabel = new JLabel();
        relBearingValue = new JLabel();
        var bearingLabel = new JLabel();
        bearingValue = new JLabel();
        var distanceLabel = new JLabel();
        distanceValue = new JLabel();
        var etaLabel = new JLabel();
        etaValue = new JLabel();
        var compassPanel = new JPanel();
        var compassLabel = new JLabel();
        compass = new JLayeredPane();
        backgroundIcon = new JLabel();
        bearingIcon = new JLabel();
        headingIcon = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setName("this");
        setLayout(new BorderLayout());

        //======== contentPanel ========
        {
            contentPanel.setName("contentPanel");
            contentPanel.setLayout(new FormLayout(
                    "default:grow, center:default",
                    "default"));

            //======== detailsPanel ========
            {
                detailsPanel.setName("detailsPanel");
                detailsPanel.setLayout(new FormLayout(
                        "default:grow",
                        "default"));

                //======== headingPanel ========
                {
                    headingPanel.setName("headingPanel");
                    headingPanel.setLayout(new FormLayout(
                            "right:default, $lcgap, 64dlu:grow",
                            "3*(default, $lgap), 2*($lgap), 4*(default, $lgap), default"));

                    //---- sourceLabel ----
                    sourceLabel.setText("Source:");
                    sourceLabel.setName("sourceLabel");
                    headingPanel.add(sourceLabel, cc.xy(1, 1));

                    //---- sourceField ----
                    sourceField.setEditable(false);
                    sourceField.setName("sourceField");
                    headingPanel.add(sourceField, cc.xy(3, 1));

                    //---- headingLabel ----
                    headingLabel.setText("Heading:");
                    headingLabel.setName("headingLabel");
                    headingPanel.add(headingLabel, cc.xy(1, 3));

                    //---- headingValue ----
                    headingValue.setName("headingValue");
                    headingPanel.add(headingValue, cc.xy(3, 3));

                    //---- speedLabel ----
                    speedLabel.setText("Speed:");
                    speedLabel.setName("speedLabel");
                    headingPanel.add(speedLabel, cc.xy(1, 5));

                    //---- speedValue ----
                    speedValue.setName("speedValue");
                    headingPanel.add(speedValue, cc.xy(3, 5));

                    //---- separator ----
                    separator.setName("separator");
                    headingPanel.add(separator, cc.xywh(1, 7, 3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

                    //---- targetLabel ----
                    targetLabel.setText("Target:");
                    targetLabel.setName("targetLabel");
                    headingPanel.add(targetLabel, cc.xy(1, 9));

                    //======== targetPanel ========
                    {
                        targetPanel.setName("targetPanel");
                        targetPanel.setLayout(new FormLayout(
                                "default, $ugap, default",
                                "default"));

                        //---- targetField ----
                        targetField.setEditable(false);
                        targetField.setName("targetField");
                        targetPanel.add(targetField, cc.xy(1, 1));

                        //---- interceptField ----
                        interceptField.setText("Intercept");
                        interceptField.setToolTipText("Navigate to the intercept point, if possible");
                        interceptField.setName("interceptField");
                        interceptField.addActionListener(e -> interceptFieldActionPerformed());
                        targetPanel.add(interceptField, cc.xy(3, 1));
                    }
                    headingPanel.add(targetPanel, cc.xy(3, 9));

                    //---- relBearingLabel ----
                    relBearingLabel.setText("Relative Bearing:");
                    relBearingLabel.setName("relBearingLabel");
                    headingPanel.add(relBearingLabel, cc.xy(1, 11));

                    //---- relBearingValue ----
                    relBearingValue.setName("relBearingValue");
                    headingPanel.add(relBearingValue, cc.xy(3, 11));

                    //---- bearingLabel ----
                    bearingLabel.setText("Bearing:");
                    bearingLabel.setName("bearingLabel");
                    headingPanel.add(bearingLabel, cc.xy(1, 13));

                    //---- bearingValue ----
                    bearingValue.setName("bearingValue");
                    headingPanel.add(bearingValue, cc.xy(3, 13));

                    //---- distanceLabel ----
                    distanceLabel.setText("Distance:");
                    distanceLabel.setName("distanceLabel");
                    headingPanel.add(distanceLabel, cc.xy(1, 15));

                    //---- distanceValue ----
                    distanceValue.setName("distanceValue");
                    headingPanel.add(distanceValue, cc.xy(3, 15));

                    //---- etaLabel ----
                    etaLabel.setText("ETA:");
                    etaLabel.setName("etaLabel");
                    headingPanel.add(etaLabel, cc.xy(1, 17));

                    //---- etaValue ----
                    etaValue.setName("etaValue");
                    headingPanel.add(etaValue, cc.xy(3, 17));
                }
                detailsPanel.add(headingPanel, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));
            }
            contentPanel.add(detailsPanel, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.CENTER));

            //======== compassPanel ========
            {
                compassPanel.setName("compassPanel");
                compassPanel.setLayout(new BorderLayout());

                //---- compassLabel ----
                compassLabel.setHorizontalAlignment(SwingConstants.CENTER);
                compassLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
                compassLabel.setName("compassLabel");
                compassPanel.add(compassLabel, BorderLayout.NORTH);

                //======== compass ========
                {
                    compass.setPreferredSize(new Dimension(100, 100));
                    compass.setName("compass");

                    //---- backgroundIcon ----
                    backgroundIcon.setHorizontalAlignment(SwingConstants.CENTER);
                    backgroundIcon.setName("backgroundIcon");
                    compass.add(backgroundIcon, JLayeredPane.DEFAULT_LAYER);
                    backgroundIcon.setBounds(new Rectangle(new Point(0, 0), backgroundIcon.getPreferredSize()));

                    //---- bearingIcon ----
                    bearingIcon.setHorizontalAlignment(SwingConstants.CENTER);
                    bearingIcon.setName("bearingIcon");
                    compass.add(bearingIcon, JLayeredPane.PALETTE_LAYER);
                    bearingIcon.setBounds(new Rectangle(new Point(0, 0), bearingIcon.getPreferredSize()));

                    //---- headingIcon ----
                    headingIcon.setHorizontalAlignment(SwingConstants.CENTER);
                    headingIcon.setName("headingIcon");
                    compass.add(headingIcon, JLayeredPane.MODAL_LAYER);
                    headingIcon.setBounds(new Rectangle(new Point(0, 0), headingIcon.getPreferredSize()));
                }
                compassPanel.add(compass, BorderLayout.CENTER);
            }
            contentPanel.add(compassPanel, cc.xy(2, 1, CellConstraints.CENTER, CellConstraints.CENTER));
        }
        add(contentPanel, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private MapObjectField<IMapObject> sourceField;
    private JLabel headingValue;
    private JLabel speedValue;
    private MapObjectField<IMapObject> targetField;
    private JCheckBox interceptField;
    private JLabel relBearingValue;
    private JLabel bearingValue;
    private JLabel distanceValue;
    private JLabel etaValue;
    private JLayeredPane compass;
    private JLabel backgroundIcon;
    private JLabel bearingIcon;
    private JLabel headingIcon;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
