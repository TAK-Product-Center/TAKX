package gov.takx.core.kml.properties;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.micromata.opengis.kml.v_2_2_0.BalloonStyle;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.swing.TextComponentResizeHandler;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.kml.BalloonTextBuilder;
import gov.takx.core.kml.KmlModel;
import gov.takx.core.kml.KmlVisual;
import gov.takx.core.kml.KmlVisualManager.KmlModelAndFeature;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.HyperlinkEvent;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

import static gov.takx.commons.swing.ImageUtils.SMALL_ICON_HEIGHT;
import static gov.takx.commons.swing.ImageUtils.SMALL_ICON_WIDTH;
import static gov.takx.platform.preferences.UserPreferenceEntry.COORDINATE_SYSTEM;
import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_PNG;

/**
 * Properties panel for a KML Feature (like a Placemark).  It will show basic info (name and location), and the "balloon" text,
 * rendered as HTML.
 * 
 * @since 5.5
 */
public class KmlFeaturePropertiesPanel extends JPanel implements IResourceCleaner
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Dimension ICON_SIZE = new Dimension(SMALL_ICON_WIDTH - 2, SMALL_ICON_HEIGHT - 2);
    private static final ImageIcon COPY_ICON = UIResources.getSizedIcon(TAK_ASSETS_PNG, "Copy_32x32.png", ICON_SIZE, true);

    private final IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor = COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, this::updateLocation);

    private final KmlVisual kmlVisual;

    /**
     * @param kmlModelAndFeature KML Model and Feature to handle
     */
    public KmlFeaturePropertiesPanel(KmlModelAndFeature kmlModelAndFeature)
    {
        final KmlModel kmlModel = kmlModelAndFeature.model();
        final var feature = kmlModelAndFeature.feature();
        kmlVisual = kmlModel.getAttachment(feature, KmlVisual.class);

        initComponents();
        setupLinkHandler();

        copyLocationButton.setIcon(COPY_ICON);
        updateLocation(coordinateSystemMonitor.getCurrentValue());

        final var balloonStyle = kmlModel.findStyleOfType(feature, BalloonStyle.class);

        // Get the balloon text to show in our properties panel
        final String balloonText = new BalloonTextBuilder(kmlModel, feature, balloonStyle).getBalloonText();
        logger.debug("balloonText: {}", balloonText);

        if (balloonStyle != null)
        {
            descriptionEditorPane.setBackground(decodeKmlColor(balloonStyle.getBgColor(), Color.WHITE));
            descriptionEditorPane.setForeground(decodeKmlColor(balloonStyle.getTextColor(), Color.BLACK));
        }

        descriptionEditorPane.setText(balloonText);
        descriptionEditorPane.setCaretPosition(0);

        new TextComponentResizeHandler(scrollPane1, descriptionEditorPane);
    }

    /**
     * @param hexColor Color string to parse
     * @param defaultColor Color to return if the given {@code hexColor} is {@code null} or empty
     * @return Color created from a hex string in {@code AABBGGRR} format (per the KML standard)
     */
    private Color decodeKmlColor(@Nullable String hexColor, @Nonnull Color defaultColor)
    {
        if (StringUtils.isEmpty(hexColor, true)) return defaultColor;

        final long color = Long.parseLong(hexColor, 16);

        final int r = (int) (color & 0xff);
        final int g = (int) ((color >> 8) & 0xFF);
        final int b = (int) ((color >> 16) & 0xFF);
        final int a = (int) ((color >> 24) & 0xFF);

        return new Color(r, g, b, a);
    }

    /**
     * Add a hyperlink listener to handle clicks on URLs embedded in the description.
     */
    private void setupLinkHandler()
    {
        descriptionEditorPane.addHyperlinkListener(event -> {
            // Check if the event type is ACTIVATED, which signifies a link click
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
            {
                try
                {
                    final var url = event.getURL();
                    logger.debug("launch {}", url);

                    Desktop.getDesktop().browse(url.toURI());
                } catch (Exception e)
                {
                    logger.warn("Error handling HTML link", e);
                }
            }
        });
    }

    /**
     * Update the location text according to the given coordinate system.
     */
    private void updateLocation(CoordinateSystem coordinateSystem)
    {
        final var locationString = Optional.ofNullable(kmlVisual)
                .map(KmlVisual::getEnvelope)
                .map(envelope -> new GeoPoint(envelope.minY, envelope.minX))
                .map(point -> CoordinateSystem.getLocationString(point, coordinateSystem))
                .orElse("UNKNOWN");
        locationTextField.setText(locationString);
    }

    /**
     * Send the location text to the system clipboard.
     */
    private void copyLocation()
    {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(locationTextField.getText()), null);
    }

    @Override
    public void releaseResources()
    {
        coordinateSystemMonitor.dispose();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var panel1 = new JPanel();
        scrollPane1 = new JScrollPane();
        descriptionEditorPane = new JEditorPane();
        var panel2 = new JPanel();
        var locationLabel = new JLabel();
        locationTextField = new JTextField();
        copyLocationButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new BorderLayout());

        //======== panel1 ========
        {
            panel1.setName("panel1");
            panel1.setLayout(new BorderLayout());

            //======== scrollPane1 ========
            {
                scrollPane1.setViewportBorder(null);
                scrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                scrollPane1.setName("scrollPane1");

                //---- descriptionEditorPane ----
                descriptionEditorPane.setContentType("text/html");
                descriptionEditorPane.setEditable(false);
                descriptionEditorPane.setBackground(Color.white);
                descriptionEditorPane.setForeground(Color.black);
                descriptionEditorPane.setName("descriptionEditorPane");
                scrollPane1.setViewportView(descriptionEditorPane);
            }
            panel1.add(scrollPane1, BorderLayout.CENTER);

            //======== panel2 ========
            {
                panel2.setBorder(Borders.createEmptyBorder("2dlu, 2dlu, 4dlu, 0dlu"));
                panel2.setName("panel2");
                panel2.setLayout(new FormLayout(
                        "default, $lcgap, default:grow, 4dlu, right:default",
                        "default"));

                //---- locationLabel ----
                locationLabel.setText("Location:");
                locationLabel.setFont(locationLabel.getFont().deriveFont(locationLabel.getFont().getSize() + 1f));
                locationLabel.setName("locationLabel");
                panel2.add(locationLabel, cc.xy(1, 1));

                //---- locationTextField ----
                locationTextField.setEditable(false);
                locationTextField.setFont(locationTextField.getFont().deriveFont(locationTextField.getFont().getSize() + 1f));
                locationTextField.setBorder(null);
                locationTextField.setName("locationTextField");
                panel2.add(locationTextField, cc.xy(3, 1));

                //---- copyLocationButton ----
                copyLocationButton.setToolTipText("Copy location");
                copyLocationButton.setName("copyLocationButton");
                copyLocationButton.addActionListener(e -> copyLocation());
                panel2.add(copyLocationButton, cc.xy(5, 1));
            }
            panel1.add(panel2, BorderLayout.NORTH);
        }
        add(panel1, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JScrollPane scrollPane1;
    private JEditorPane descriptionEditorPane;
    private JTextField locationTextField;
    private JButton copyLocationButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
