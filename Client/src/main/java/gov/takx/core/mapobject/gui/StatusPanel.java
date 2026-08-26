package gov.takx.core.mapobject.gui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.commons.constants.State;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.platform.utils.DataMonitorUtils;
import gov.takx.platform.utils.StateIconUtils;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class StatusPanel extends JPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int SIZE = 32;

    @Inject
    private StateIconUtils stateIconUtils;

    private final IDataMonitor dataMonitor;
    private final Map<String, BufferedImage> statusImages = new HashMap<>();
    private final LayeredLabel statusSymbolLabel;
    private BufferedImage customImage;
    private BufferedImage muteImage;

    public StatusPanel(IDataMonitor dataMonitor)
    {
        initComponents();

        this.dataMonitor = dataMonitor;

        setName(dataMonitor.getFieldName());
        statusNameLabel.setText(dataMonitor.getDisplayName());
        statusValueLabel.setVisible(dataMonitor.getShowValueOnDisplay());

        // Add the label that will contain the status image(s).
        statusSymbolLabel = new LayeredLabel();
        statusSymbolPanel.add(statusSymbolLabel, new CellConstraints().xy(1, 1));

        // If there is a custom overlay icon, read it in and
        // create a custom status image.
        URL customIcon = dataMonitor.getCustomIcon();
        if (customIcon != null)
        {
            try
            {
                customImage = ImageIO.read(customIcon);
            } catch (IOException ioe)
            {
                logger.error("Error reading custom icon image.", ioe);
            }
        }
        muteImage = null;
    }

    @PostConstruct
    private void postConstruct()
    {
        // Update the status image.
        updateStatus(dataMonitor, dataMonitor.getCurrentState(), dataMonitor.getUnclearedAlarmState());

        Object value = dataMonitor.getValue();
        if (value != null)
        {
            updateValue(value);
        }
    }

    protected void updateStatus(IDataMonitor monitor, State newState, State visualState)
    {
        final Color stateColor = stateIconUtils.getColorForState(newState);
        final GraphicsUtils.Shape shape = stateIconUtils.getShapeForState(newState);

        BufferedImage statusImage = statusImages.get(shape.toString() + stateColor.toString());

        if (statusImage == null)
        {
            statusImage = GraphicsUtils.createShape(shape, SIZE, 200, stateColor);

            if (customImage != null)
            {
                // The result of this method will be a statusImage
                // with a scaled customImage overlaid onto it.
                DataMonitorUtils.overlayImage(statusImage, customImage);
            }
            statusImages.put(shape.toString() + stateColor, statusImage);
        }

        if (visualState != newState) statusImage = stateIconUtils.overlayVisualState(statusImage, visualState);

        if (monitor.isMuted()) statusImage = overlayMutedState(statusImage);

        // Draw a border around the image.
        if (shape == GraphicsUtils.Shape.TRIANGLE)
        {
            statusImage = GraphicsUtils.createOutlinedImage(statusImage, stateColor.getRGB(), 150);
        }

        statusSymbolLabel.setIcon(new ImageIcon(statusImage));
    }

    protected void updateValue(Object propertyValue)
    {
        String val = propertyValue.toString();
        statusValueLabel.setText(val);
    }

    private BufferedImage overlayMutedState(BufferedImage statusImage)
    {
        if (muteImage == null)
        {
            muteImage = DataMonitorUtils.getMutedStateImage(statusImage.getWidth() / 2);
        }
        return DataMonitorUtils.overlayMuteStatus(statusImage, muteImage);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        statusSymbolPanel = new JPanel();
        nameValuePanel = new JPanel();
        statusNameLabel = new JLabel();
        statusValueLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default:grow",
                "$lgap, fill:default:grow, $lgap"));

        //======== statusSymbolPanel ========
        {
            statusSymbolPanel.setName("statusSymbolPanel");
            statusSymbolPanel.setLayout(new FormLayout(
                    "default:grow",
                    "fill:default:grow"));
        }
        add(statusSymbolPanel, cc.xy(1, 2));

        //======== nameValuePanel ========
        {
            nameValuePanel.setName("nameValuePanel");
            nameValuePanel.setLayout(new FormLayout(
                    "default:grow",
                    "2*(default)"));

            //---- statusNameLabel ----
            statusNameLabel.setName("statusNameLabel");
            nameValuePanel.add(statusNameLabel, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));

            //---- statusValueLabel ----
            statusValueLabel.setName("statusValueLabel");
            nameValuePanel.add(statusValueLabel, cc.xy(1, 2, CellConstraints.FILL, CellConstraints.FILL));
        }
        add(nameValuePanel, cc.xy(3, 2));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel statusSymbolPanel;
    private JPanel nameValuePanel;
    private JLabel statusNameLabel;
    private JLabel statusValueLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
