package gov.takx.core.structures.gui;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.structures.ISurfaceStructure;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.IPanelStateListener;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.structures.ICoreSurfaceStructureService;
import gov.takx.core.structures.SSExtrudedPolygon;
import gov.takx.core.structures.SSShapefileShape;
import gov.takx.core.view.gui.GUIUtils;
import gov.takx.core.view.gui.metadata.MetadataTablePanel;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

import java.awt.Desktop;
import java.awt.Insets;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

/**
 * Surface structure dialog. The user can change the name, description, and URL associated with a surface structure.
 * The URL can be launched using the system's default web browser.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class SurfaceStructureDialog extends AStandardDialog implements IPanelStateListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IGlobeManager globeManager;

    @Inject
    private ICoreSurfaceStructureService service;

    private final ISurfaceStructure structure;
    private final MetadataTablePanel metadataPanel;

    public SurfaceStructureDialog(@Nonnull ISurfaceStructure structure, @Nonnull List<String> metaDataOfInterest)
    {
        initComponents();

        this.structure = structure;

        metadataPanel = new MetadataTablePanel(pce -> setPanelDirty());
        metadataPanel.setMetadata(structure.getMetadata());
        metadataPanel.setMetadataOfInterest(metaDataOfInterest);
        metadataContentPane.add(metadataPanel);

        final String url = structure.getUrl();
        if (!StringUtils.isEmpty(url)) urlTextField.setText(url);

        // set the title to the structures name if it has one, or its uid otherwise
        final UUID uuid = this.structure.getUUID();
        final String name = structure.getName();

        setTitle("Structure - " + (name != null ? name : uuid));

        uidLabel.setText(uuid.toString());
        nameTextField.setText(name);
        descriptionTextField.setText(structure.getDescription());

        final IRaptorLayer layer = structure.getLayer();
        if (layer != null)
        {
            groupLabel.setText(layer.getName());
            groupNameLabel.setText(layer.getDisplayName());
        }

        if (structure instanceof SSExtrudedPolygon polygon)
        {
            heightLabel.setText(polygon.getHeightString());
        } else if (structure instanceof SSShapefileShape shapeFileShape)
        {
            heightLabel.setText(shapeFileShape.getHeightString());
        } else
        {
            heightTitleLabel.setVisible(false);
            heightLabel.setVisible(false);
        }

        GUIUtils.addSingleActionDocumentListener(urlTextField, e -> setPanelDirty());
        GUIUtils.addSingleActionDocumentListener(nameTextField, e -> setPanelDirty());
        GUIUtils.addSingleActionDocumentListener(descriptionTextField, e -> setPanelDirty());

        //TODO add surface structure property listeners if they get them?

        pack();
    }

    @PostConstruct
    private void postConstruct()
    {
        final ILocation loc = structure.getLocation();

        if (null != loc)
        {
            locationLabel.setText(CoordinateSystem.getLocationString(loc, UserPreferenceEntry.COORDINATE_SYSTEM.getValueAs(CoordinateSystem.class)));
            goToButton.setEnabled(true);
        } else
        {
            logger.warn("Structure: {} location was null.", structure.getName());
            goToButton.setEnabled(false);
        }
    }

    @Override
    public void setPanelDirty()
    {
        saveButton.setEnabled(true);
    }

    @Override
    public void setPanelClean()
    {
    }

    /**
     * Navigate to the surface structure on the globe.
     */
    private void goToButtonActionPerformed()
    {
        globeManager.getLastFocusedGlobe().goTo(structure.getLocation());
    }

    /**
     * Open the browser on the contents of the urlTextField.
     * @since 3.3
     */
    private void btnLaunchPressed()
    {
        final String urlText = urlTextField.getText();

        try
        {
            Desktop.getDesktop().browse(new URI(urlText));
        } catch (IOException e)
        {
            logger.warn("Failed to launch browser on '{}'", urlText, e);
            AlwaysOnTopJOptionPane.showErrorDialog("Unable to open URL: " + urlText, "Failed to launch browser");
        } catch (URISyntaxException e)
        {
            AlwaysOnTopJOptionPane.showErrorDialog("Invalid URL: '" + urlText + "'", "Invalid URL");
        }
    }

    /**
     * Update the surface structure with any of its values that have changed.
     */
    private void saveButtonActionPerformed()
    {
        structure.setName(nameTextField.getText());
        structure.setDescription(descriptionTextField.getText());
        structure.setUrl(urlTextField.getText());

        metadataPanel.commit();
        var oldMetadata = metadataPanel.getOriginalMetadata();
        var newMetadata = metadataPanel.getMetadata();
        var shapeMetadata = structure.getMetadata();

        for (String key : metadataPanel.getChangedMetadataKeys())
        {
            final String oldValue = oldMetadata.getStringAttribute(key);
            final String newValue = newMetadata.getStringAttribute(key);

            shapeMetadata.setAttribute(key, newValue);
            service.notifyListenersOfMetaDataChange(structure, key, oldValue, newValue);
        }

        service.persistStructure(structure);

        dispose();
    }

    /**
     * Close the dialog.
     */
    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    @SuppressWarnings("DuplicatedCode")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panel3 = new JPanel();
        var contentPanel = new JPanel();
        var leftPanel = new JPanel();
        var iconLabel = new JLabel();
        var goToPanel = new JPanel();
        goToButton = new JButton();
        var rightPanel = new JPanel();
        var label1 = new JLabel();
        uidLabel = new JLabel();
        var label3 = new JLabel();
        groupLabel = new JLabel();
        var label4 = new JLabel();
        groupNameLabel = new JLabel();
        var label2 = new JLabel();
        locationLabel = new JLabel();
        heightTitleLabel = new JLabel();
        heightLabel = new JLabel();
        var panel7 = new JPanel();
        nameTextField = new JTextField();
        var panel5 = new JPanel();
        var scrollPane3 = new JScrollPane();
        descriptionTextField = new JTextPane();
        var panel6 = new JPanel();
        var panel1 = new JPanel();
        urlTextField = new JTextField();
        var btnLaunch = new JButton();
        var panel4 = new JPanel();
        var scrollPane1 = new JScrollPane();
        metadataContentPane = new JPanel();
        var buttonBar = new JPanel();
        saveButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Structure - ");
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow"));

        //======== panel3 ========
        {
            panel3.setName("panel3");
            panel3.setLayout(new FormLayout(
                    "default:grow",
                    "fill:default, 3*($lgap, default), $lgap, fill:default:grow, $lgap, default"));

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow, $rgap, default:grow",
                        "fill:default:grow"));

                //======== leftPanel ========
                {
                    leftPanel.setName("leftPanel");
                    leftPanel.setLayout(new FormLayout(
                            "default:grow",
                            "default, $lgap, pref:grow"));

                    //---- iconLabel ----
                    iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    iconLabel.setName("iconLabel");
                    leftPanel.add(iconLabel, cc.xy(1, 1));

                    //======== goToPanel ========
                    {
                        goToPanel.setName("goToPanel");
                        goToPanel.setLayout(new BoxLayout(goToPanel, BoxLayout.X_AXIS));

                        //---- goToButton ----
                        goToButton.setText("Go To");
                        goToButton.setIconTextGap(0);
                        goToButton.setHorizontalTextPosition(SwingConstants.CENTER);
                        goToButton.setEnabled(false);
                        goToButton.setName("goToButton");
                        goToButton.addActionListener(e -> goToButtonActionPerformed());
                        goToPanel.add(goToButton);
                    }
                    leftPanel.add(goToPanel, cc.xy(1, 3, CellConstraints.CENTER, CellConstraints.CENTER));
                }
                contentPanel.add(leftPanel, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));

                //======== rightPanel ========
                {
                    rightPanel.setName("rightPanel");
                    rightPanel.setLayout(new FormLayout(
                            "default, 2*($lcgap), default:grow",
                            "$rgap, 5*($lgap, default)"));

                    //---- label1 ----
                    label1.setText("UID:");
                    label1.setName("label1");
                    rightPanel.add(label1, cc.xy(1, 3));

                    //---- uidLabel ----
                    uidLabel.setName("uidLabel");
                    rightPanel.add(uidLabel, cc.xy(4, 3));

                    //---- label3 ----
                    label3.setText("Group:");
                    label3.setName("label3");
                    rightPanel.add(label3, cc.xy(1, 5));

                    //---- groupLabel ----
                    groupLabel.setName("groupLabel");
                    rightPanel.add(groupLabel, cc.xy(4, 5));

                    //---- label4 ----
                    label4.setText("Group Name:");
                    label4.setName("label4");
                    rightPanel.add(label4, cc.xy(1, 7));

                    //---- groupNameLabel ----
                    groupNameLabel.setName("groupNameLabel");
                    rightPanel.add(groupNameLabel, cc.xy(4, 7));

                    //---- label2 ----
                    label2.setText("Location:");
                    label2.setName("label2");
                    rightPanel.add(label2, cc.xy(1, 9, CellConstraints.DEFAULT, CellConstraints.TOP));

                    //---- locationLabel ----
                    locationLabel.setName("locationLabel");
                    rightPanel.add(locationLabel, cc.xy(4, 9));

                    //---- heightTitleLabel ----
                    heightTitleLabel.setText("Height:");
                    heightTitleLabel.setName("heightTitleLabel");
                    rightPanel.add(heightTitleLabel, cc.xy(1, 11));

                    //---- heightLabel ----
                    heightLabel.setName("heightLabel");
                    rightPanel.add(heightLabel, cc.xy(4, 11));
                }
                contentPanel.add(rightPanel, new CellConstraints(3, 1, 1, 1, CellConstraints.FILL, CellConstraints.FILL, new Insets(1, 1, 1, 1)));
            }
            panel3.add(contentPanel, cc.xy(1, 1));

            //======== panel7 ========
            {
                panel7.setBorder(new TitledBorder("Name"));
                panel7.setName("panel7");
                panel7.setLayout(new FormLayout(
                        "default:grow",
                        "default"));

                //---- nameTextField ----
                nameTextField.setName("nameTextField");
                panel7.add(nameTextField, cc.xy(1, 1));
            }
            panel3.add(panel7, cc.xy(1, 3));

            //======== panel5 ========
            {
                panel5.setBorder(new TitledBorder("Description"));
                panel5.setName("panel5");
                panel5.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default"));

                //======== scrollPane3 ========
                {
                    scrollPane3.setName("scrollPane3");

                    //---- descriptionTextField ----
                    descriptionTextField.setName("descriptionTextField");
                    scrollPane3.setViewportView(descriptionTextField);
                }
                panel5.add(scrollPane3, cc.xy(1, 1));
            }
            panel3.add(panel5, cc.xy(1, 5));

            //======== panel6 ========
            {
                panel6.setBorder(new TitledBorder(null, "URL", TitledBorder.LEADING, TitledBorder.TOP));
                panel6.setName("panel6");
                panel6.setLayout(new FormLayout(
                        "default:grow",
                        "default, $lgap, fill:default"));

                //======== panel1 ========
                {
                    panel1.setName("panel1");
                    panel1.setLayout(new FormLayout(
                            "default:grow, $lcgap, default",
                            "default"));

                    //---- urlTextField ----
                    urlTextField.setName("urlTextField");
                    panel1.add(urlTextField, cc.xy(1, 1));

                    //---- btnLaunch ----
                    btnLaunch.setText("Launch");
                    btnLaunch.setName("btnLaunch");
                    btnLaunch.addActionListener(e -> btnLaunchPressed());
                    panel1.add(btnLaunch, cc.xy(3, 1));
                }
                panel6.add(panel1, cc.xy(1, 1));
            }
            panel3.add(panel6, cc.xy(1, 7));

            //======== panel4 ========
            {
                panel4.setName("panel4");
                panel4.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default:grow"));

                //======== scrollPane1 ========
                {
                    scrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                    scrollPane1.setName("scrollPane1");

                    //======== metadataContentPane ========
                    {
                        metadataContentPane.setName("metadataContentPane");
                        metadataContentPane.setLayout(new BoxLayout(metadataContentPane, BoxLayout.Y_AXIS));
                    }
                    scrollPane1.setViewportView(metadataContentPane);
                }
                panel4.add(scrollPane1, cc.xy(1, 1));
            }
            panel3.add(panel4, cc.xy(1, 9));

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, default, $lcgap, default",
                        "fill:default"));

                //---- saveButton ----
                saveButton.setText("Save");
                saveButton.setEnabled(false);
                saveButton.setName("saveButton");
                saveButton.addActionListener(e -> saveButtonActionPerformed());
                buttonBar.add(saveButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Close");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(4, 1));
            }
            panel3.add(buttonBar, cc.xy(1, 11));
        }
        contentPane.add(panel3, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JButton goToButton;
    private JLabel uidLabel;
    private JLabel groupLabel;
    private JLabel groupNameLabel;
    private JLabel locationLabel;
    private JLabel heightTitleLabel;
    private JLabel heightLabel;
    private JTextField nameTextField;
    private JTextPane descriptionTextField;
    private JTextField urlTextField;
    private JPanel metadataContentPane;
    private JButton saveButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
