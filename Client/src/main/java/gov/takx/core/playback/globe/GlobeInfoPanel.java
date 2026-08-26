package gov.takx.core.playback.globe;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.playback.IDisposable;
import gov.takx.api.playback.IPlaybackLane;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.swing.MouseClickedListener;
import gov.takx.platform.playback.ui.TrackPanel;
import gov.takx.platform.ui.UIResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gov.takx.commons.swing.ImageUtils.SMALL_ICON_HEIGHT;
import static gov.takx.commons.swing.ImageUtils.SMALL_ICON_WIDTH;
import static gov.takx.core.playback.globe.GlobeDevicePanel.FIXED_HEIGHT;

/**
 * This panel displays the information about the Map Objects added to our playback lane.  A sub-panel is created
 * for each Map Object to display the icon and name.
 *
 * @since 1.47
 */
public class GlobeInfoPanel extends JPanel implements PropertyChangeListener, IDisposable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final List<String> MO_PROPERTIES = Arrays.asList(IMapObject.ICON, IMapObject.NAME);

    private final IPlaybackLane lane;
    private final TrackPanel trackPanel;
    private final int firstRowHeight;
    private final Map<String, JLabel> labelMap = new HashMap<>();
    private final List<IMapObject> currentDevices = new ArrayList<>();
    private final MouseClickedListener displayEditorListener = e -> displayEditor();

    public GlobeInfoPanel(IPlaybackLane lane, String label, TrackPanel trackPanel)
    {
        this.lane = lane;
        this.trackPanel = trackPanel;

        initComponents();

        globeLabel.setText(label);
        globeLabel.addMouseListener(displayEditorListener);

        addButton.setIcon(UIResources.SETTINGS_ICON);
        addButton.setContentAreaFilled(false);
        addButton.setBorderPainted(false);
        addButton.setFocusPainted(false);
        addButton.addMouseListener(displayEditorListener);

        firstRowHeight = (int) configPanel.getPreferredSize().getHeight();

        updateHeight();

        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                updateHeight();
            }
        });
    }

    /**
     * @return The height, in pixels, of our first row (which is larger than the FIXED_HEIGHT of all data rows
     * @since 5.7
     */
    public int getFirstRowHeight()
    {
        return firstRowHeight;
    }

    /**
     * Set the label/name shown in this panel.
     *
     * @param label New label text
     */
    @InvokeOnEDT
    public void setLabel(String label)
    {
        globeLabel.setText(label);
    }

    @InvokeOnEDT
    public void setDevices(Collection<IMapObject> mapObjects)
    {
        disposeResources();

        for (IMapObject mapObject : mapObjects)
        {
            currentDevices.add(mapObject);

            addListeners(mapObject);

            final JLabel label = new JLabel(mapObject.getName());
            label.setIcon(getScaledIcon(mapObject.getIconImage()));
            label.addMouseListener(displayEditorListener);
            labelMap.put(mapObject.getKey(), label);
            deviceListPane.add(label);
        }

        updateHeight();
        revalidate();
    }

    /**
     * Update the height on this panel and the neighboring track panel.  Never let the height go below 3 lines of data.
     */
    private void updateHeight()
    {
        int rowCount = Math.max(currentDevices.size(), 2); // At least 2 data rows
        int height = rowCount * FIXED_HEIGHT + firstRowHeight;

        setSizesOnPanel(this, height);
        setSizesOnPanel(trackPanel, height);
    }

    /**
     * Apply our height calculations onto the given panel.
     *
     * @param panel The panel to update
     */
    private void setSizesOnPanel(JPanel panel, int height)
    {
        // Set the max/preferred size
        final Dimension panelMaximumSize = panel.getMaximumSize();
        panel.setMaximumSize(new Dimension(panelMaximumSize.width, height));
        panel.setPreferredSize(panelMaximumSize);

        panel.revalidate();
    }

    /**
     * Create a high-quality scaled image of the given buffered image at 16x16 resolution.
     *
     * @param image Image to scale
     * @return Scaled image
     */
    private ImageIcon getScaledIcon(BufferedImage image)
    {
        return new ImageIcon(ImageUtils.getScaledInstanceRespectingAspectRatio(image, SMALL_ICON_WIDTH, SMALL_ICON_HEIGHT));
    }

    /**
     * Get the label for the given Map Object.
     *
     * @param mapObject Map Object of interest
     * @return JLabel for map object, may be {@code null} if no label is found
     */
    private JLabel getLabelForMapObject(IMapObject mapObject)
    {
        final String key = mapObject.getKey();
        final JLabel label = labelMap.get(key);

        if (label == null)
        {
            logger.error("Missing label for MapObject: {}", key);
        }

        return label;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        switch (evt.getPropertyName())
        {
            case IMapObject.NAME -> updateName((IMapObject) evt.getSource());
            case IMapObject.ICON -> updateIcon((IMapObject) evt.getSource());
            default -> logger.error("Unexpected PCE: {}", evt);
        }
    }

    private void updateName(IMapObject mapObject)
    {
        final JLabel label = getLabelForMapObject(mapObject);
        if (label != null)
        {
            label.setText(mapObject.getName());
        }
    }

    private void updateIcon(IMapObject mapObject)
    {
        final JLabel label = getLabelForMapObject(mapObject);
        if (label != null)
        {
            label.setIcon(getScaledIcon(mapObject.getIconImage()));
        }
    }

    private void addListeners(IMapObject mapObject)
    {
        removeListeners(mapObject); // Just in case, remove any known listeners first.

        mapObject.addPropertyChangeListener(MO_PROPERTIES, this);
    }

    private void removeListeners(IMapObject mapObject)
    {
        mapObject.removePropertyChangeListener(MO_PROPERTIES, this);
    }

    private void displayEditor()
    {
        lane.displayConfigurationEditor();
    }

    @Override
    public void disposeResources()
    {
        if (logger.isTraceEnabled()) logger.trace("GlobeInfoPanel.dispose: {}", globeLabel.getText());

        for (IMapObject mapObject : currentDevices)
        {
            removeListeners(mapObject);
        }

        currentDevices.clear();
        labelMap.clear();
        deviceListPane.removeAll();
    }

    private void panel1MouseEntered()
    {
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    private void deviceListPaneMouseEntered()
    {
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    private void mainPanelMouseEntered()
    {
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        configPanel = new JPanel();
        globeLabel = new JLabel();
        addButton = new JButton();
        deviceListPane = new JPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseEntered(MouseEvent e)
            {
                mainPanelMouseEntered();
            }
        });
        setLayout(new FormLayout(
                "default:grow",
                "default, fill:default:grow"));

        //======== configPanel ========
        {
            configPanel.setName("configPanel");
            configPanel.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseEntered(MouseEvent e)
                {
                    panel1MouseEntered();
                }
            });
            configPanel.setLayout(new FormLayout(
                    "left:min, $rgap, left:default, $lcgap, default:grow",
                    "default"));

            //---- globeLabel ----
            globeLabel.setText(" Devices");
            globeLabel.setName("globeLabel");
            configPanel.add(globeLabel, cc.xy(1, 1));

            //---- addButton ----
            addButton.setHorizontalTextPosition(SwingConstants.CENTER);
            addButton.setFocusable(false);
            addButton.setFocusPainted(false);
            addButton.setToolTipText("Add Map Objects from the Globe");
            addButton.setName("addButton");
            configPanel.add(addButton, cc.xy(3, 1, CellConstraints.CENTER, CellConstraints.CENTER));
        }
        add(configPanel, cc.xy(1, 1));

        //======== deviceListPane ========
        {
            deviceListPane.setName("deviceListPane");
            deviceListPane.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseEntered(MouseEvent e)
                {
                    deviceListPaneMouseEntered();
                }
            });
            deviceListPane.setLayout(new BoxLayout(deviceListPane, BoxLayout.Y_AXIS));
        }
        add(deviceListPane, cc.xy(1, 2));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel configPanel;
    private JLabel globeLabel;
    private JButton addButton;
    private JPanel deviceListPane;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
