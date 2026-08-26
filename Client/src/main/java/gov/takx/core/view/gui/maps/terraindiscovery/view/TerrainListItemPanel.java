package gov.takx.core.view.gui.maps.terraindiscovery.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.ITerrainDiscoveryController;
import gov.takx.platform.ui.UIResources;
import net.java.dev.colorchooser.ColorChooser;
import net.java.dev.colorchooser.Palette;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 */
public class TerrainListItemPanel extends JPanel
{
    private static final String NOT_AVAILABLE = "N/A";
    private static final ImageIcon NETWORKED_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "network.png");
    private static final ImageIcon NOT_NETWORKED_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "network_disconnected.png");
    private static final ImageIcon MOVE_TO_TOP_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "align_top.png");

    private final ITerrainDiscoveryController controller;
    private final IRaptorLayer elevationLayer;
    private boolean alternateBackgroundEnabled = false;

    private static final int NUM_HUES = 32;
    private static final Palette HUE_PALETTE;

    static
    {
        float maxHue = NUM_HUES - 1;

        Color[] paletteColors = new Color[NUM_HUES];
        for (int i = 0; i < NUM_HUES; i++)
        {
            float hue = i / maxHue;
            paletteColors[i] = new Color(Color.HSBtoRGB(hue, 1, 1));
        }
        HUE_PALETTE = Palette.createPredefinedPalette("Colors", paletteColors, new String[NUM_HUES]);
    }

    public TerrainListItemPanel(ITerrainDiscoveryController controller, IRaptorLayer elevationLayer)
    {
        this.controller = controller;
        this.elevationLayer = elevationLayer;
        initComponents();
        topButton.setIcon(MOVE_TO_TOP_ICON);

        surfaceColor.setPalettes(new Palette[]{HUE_PALETTE});
        nameLabel.setText(elevationLayer.getDisplayName());
        networkedIconLabel.setText("");

        if (elevationLayer.isNetworkRetrievalEnabled())
        {
            networkedIconLabel.setIcon(NETWORKED_ICON);
            networkedIconLabel.setToolTipText("This elevation layer is network enabled");
        } else
        {
            networkedIconLabel.setIcon(NOT_NETWORKED_ICON);
            networkedIconLabel.setToolTipText("This elevation layer is NOT network enabled");
        }

        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                enableCheckBox.setSelected(!enableCheckBox.isSelected());
            }
        }, this);
        enableCheckBox.addChangeListener(e -> setElevationLayerVisible(enableCheckBox.isSelected()));
    }

    public void setElevation(String elevation)
    {
        elevationValueLabel.setText(elevation != null ? elevation : NOT_AVAILABLE);
    }

    public void resetView()
    {
        coverageValueLabel.setText(NOT_AVAILABLE);
        resolutionValueLabel.setText(NOT_AVAILABLE);
        elevationValueLabel.setText(NOT_AVAILABLE);
    }

    public void setAlternateBackgroundEnabled(boolean enable)
    {
        alternateBackgroundEnabled = enable;
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        if (alternateBackgroundEnabled)
        {
            g.setColor(getBackground().brighter());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    public void setTopEnabled(boolean enable)
    {
        topButton.setVisible(enable);
    }

    public IRaptorLayer getElevationLayer()
    {
        return elevationLayer;
    }

    public void setSurfaceColor(Color surfaceColor)
    {
        this.surfaceColor.setColor(surfaceColor);
        controller.setElevationLayerColor(elevationLayer, surfaceColor);
    }

    public void setCoverageText(String coverageText)
    {
        coverageValueLabel.setText(coverageText);
    }

    public void setResolutionText(String resolutionText)
    {
        resolutionValueLabel.setText(resolutionText);
    }

    public void dispose()
    {

    }

    public void setEnableCheckBoxSelected(boolean selected)
    {
        if (enableCheckBox.isSelected() != selected)
        {
            enableCheckBox.setSelected(selected);
        }
    }

    private void surfaceColorActionPerformed()
    {
        controller.setElevationLayerColor(elevationLayer, surfaceColor.getColor());
    }

    private void setElevationLayerVisible(boolean visible)
    {
        controller.setElevationLayerVisible(elevationLayer, visible);
    }

    private void addMouseListener(MouseListener mouseListener, Component c)
    {
        if (c != surfaceColor && c != enableCheckBox && c != topButton)
        {
            c.addMouseListener(mouseListener);
        }
        if (c instanceof Container)
        {
            for (Component c1 : ((Container) c).getComponents())
            {
                addMouseListener(mouseListener, c1);
            }
        }
    }

    private void topButtonActionPerformed()
    {
        controller.setElevationLayerToTop(elevationLayer);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        topSeparator = new JSeparator();
        fullPane = new JPanel();
        enableCheckBox = new JCheckBox();
        separator2 = new JSeparator();
        detailsPane = new JPanel();
        basicContentPane = new JPanel();
        surfaceColor = new ColorChooser();
        nameLabel = new JLabel();
        networkedIconLabel = new JLabel();
        lowerContentPane = new JPanel();
        panel4 = new JPanel();
        resolutionLabel = new JLabel();
        resolutionValueLabel = new JLabel();
        coverageLabel = new JLabel();
        coverageValueLabel = new JLabel();
        panel1 = new JPanel();
        elevationLabel = new JLabel();
        elevationValueLabel = new JLabel();
        topButtonPanel = new JPanel();
        topButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "$lgap, default"));

        //---- topSeparator ----
        topSeparator.setName("topSeparator");
        add(topSeparator, cc.xy(1, 1));

        //======== fullPane ========
        {
            fullPane.setOpaque(false);
            fullPane.setName("fullPane");
            fullPane.setLayout(new FormLayout(
                    "default, $rgap, default:grow, $lcgap, default",
                    "default"));

            //---- enableCheckBox ----
            enableCheckBox.setDoubleBuffered(true);
            enableCheckBox.setSelected(true);
            enableCheckBox.setOpaque(false);
            enableCheckBox.setName("enableCheckBox");
            fullPane.add(enableCheckBox, cc.xy(1, 1));

            //---- separator2 ----
            separator2.setOrientation(SwingConstants.VERTICAL);
            separator2.setName("separator2");
            fullPane.add(separator2, cc.xy(2, 1, CellConstraints.CENTER, CellConstraints.FILL));

            //======== detailsPane ========
            {
                detailsPane.setOpaque(false);
                detailsPane.setName("detailsPane");
                detailsPane.setLayout(new FormLayout(
                        "default:grow",
                        "default, $lgap, default"));

                //======== basicContentPane ========
                {
                    basicContentPane.setOpaque(false);
                    basicContentPane.setName("basicContentPane");
                    basicContentPane.setLayout(new FormLayout(
                            "default, $lcgap, default:grow, $lcgap, default",
                            "default"));

                    //---- surfaceColor ----
                    surfaceColor.setMaximumSize(new Dimension(14, 14));
                    surfaceColor.setMinimumSize(new Dimension(14, 14));
                    surfaceColor.setPreferredSize(new Dimension(14, 14));
                    surfaceColor.setName("surfaceColor");
                    surfaceColor.addActionListener(e -> surfaceColorActionPerformed());
                    basicContentPane.add(surfaceColor, cc.xy(1, 1));

                    //---- nameLabel ----
                    nameLabel.setText("Elevation Layer");
                    nameLabel.setDoubleBuffered(true);
                    nameLabel.setFont(nameLabel.getFont().deriveFont(nameLabel.getFont().getStyle() | Font.BOLD));
                    nameLabel.setName("nameLabel");
                    basicContentPane.add(nameLabel, cc.xy(3, 1));

                    //---- networkedIconLabel ----
                    networkedIconLabel.setDoubleBuffered(true);
                    networkedIconLabel.setName("networkedIconLabel");
                    basicContentPane.add(networkedIconLabel, cc.xy(5, 1));
                }
                detailsPane.add(basicContentPane, cc.xy(1, 1));

                //======== lowerContentPane ========
                {
                    lowerContentPane.setOpaque(false);
                    lowerContentPane.setName("lowerContentPane");
                    lowerContentPane.setLayout(new FormLayout(
                            "default:grow",
                            "2*(default, $lgap)"));

                    //======== panel4 ========
                    {
                        panel4.setOpaque(false);
                        panel4.setName("panel4");
                        panel4.setLayout(new FormLayout(
                                "default, $lcgap, default, default:grow, default, $lcgap, default",
                                "default"));

                        //---- resolutionLabel ----
                        resolutionLabel.setText("Resolution: ");
                        resolutionLabel.setDoubleBuffered(true);
                        resolutionLabel.setName("resolutionLabel");
                        panel4.add(resolutionLabel, cc.xy(1, 1));

                        //---- resolutionValueLabel ----
                        resolutionValueLabel.setText("N/A");
                        resolutionValueLabel.setDoubleBuffered(true);
                        resolutionValueLabel.setName("resolutionValueLabel");
                        panel4.add(resolutionValueLabel, cc.xy(3, 1));

                        //---- coverageLabel ----
                        coverageLabel.setText("Coverage: ");
                        coverageLabel.setDoubleBuffered(true);
                        coverageLabel.setName("coverageLabel");
                        panel4.add(coverageLabel, cc.xy(5, 1));

                        //---- coverageValueLabel ----
                        coverageValueLabel.setText("N/A");
                        coverageValueLabel.setDoubleBuffered(true);
                        coverageValueLabel.setName("coverageValueLabel");
                        panel4.add(coverageValueLabel, cc.xy(7, 1));
                    }
                    lowerContentPane.add(panel4, cc.xy(1, 1));

                    //======== panel1 ========
                    {
                        panel1.setOpaque(false);
                        panel1.setName("panel1");
                        panel1.setLayout(new FormLayout(
                                "default, $lcgap, default",
                                "default"));

                        //---- elevationLabel ----
                        elevationLabel.setText("Elevation at Mouse Location: ");
                        elevationLabel.setName("elevationLabel");
                        panel1.add(elevationLabel, cc.xy(1, 1));

                        //---- elevationValueLabel ----
                        elevationValueLabel.setText("N/A");
                        elevationValueLabel.setName("elevationValueLabel");
                        panel1.add(elevationValueLabel, cc.xy(3, 1));
                    }
                    lowerContentPane.add(panel1, cc.xy(1, 3));
                }
                detailsPane.add(lowerContentPane, cc.xy(1, 3));
            }
            fullPane.add(detailsPane, cc.xy(3, 1));

            //======== topButtonPanel ========
            {
                topButtonPanel.setVisible(false);
                topButtonPanel.setName("topButtonPanel");
                topButtonPanel.setLayout(new FormLayout(
                        "24px",
                        "fill:default:grow"));

                //---- topButton ----
                topButton.setToolTipText("Move to top of visualization");
                topButton.setFocusable(false);
                topButton.setOpaque(false);
                topButton.setName("topButton");
                topButton.addActionListener(e -> topButtonActionPerformed());
                topButtonPanel.add(topButton, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
            }
            fullPane.add(topButtonPanel, cc.xy(5, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        }
        add(fullPane, cc.xy(1, 2));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JSeparator topSeparator;
    private JPanel fullPane;
    private JCheckBox enableCheckBox;
    private JSeparator separator2;
    private JPanel detailsPane;
    private JPanel basicContentPane;
    private ColorChooser surfaceColor;
    private JLabel nameLabel;
    private JLabel networkedIconLabel;
    private JPanel lowerContentPane;
    private JPanel panel4;
    private JLabel resolutionLabel;
    private JLabel resolutionValueLabel;
    private JLabel coverageLabel;
    private JLabel coverageValueLabel;
    private JPanel panel1;
    private JLabel elevationLabel;
    private JLabel elevationValueLabel;
    private JPanel topButtonPanel;
    private JButton topButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
