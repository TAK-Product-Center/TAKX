package gov.takx.core.view.gui.pointdropper;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.icons.IIconSetManager;
import gov.tak.api.icons.IUserIcon;
import gov.tak.api.icons.IUserIconSet;
import gov.tak.api.icons.TakColor;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.plugin.synthetic.ISyntheticPluginDefinition;
import gov.takx.api.pointdropper.PointDropperConfiguration;
import gov.takx.api.pointdropper.PointDropperConfiguration.CountingNameGenerator;
import gov.takx.api.pointdropper.PointDropperConfiguration.INameGenerator;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.ui.location.ILocationInputListener;
import gov.takx.commons.swing.EscapeKeyListener;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.commons.swing.PlaceholderTextField;
import gov.takx.commons.swing.documents.LimitedDocument;
import gov.takx.commons.utils.ParseUtils;
import gov.takx.core.pointdropper.PointDropperService;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.core.view.gui.layers.viewmanager.CollapsiblePanel;
import gov.takx.platform.ui.location.LegacyLocationInputPanel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Panel to display the Point Dropper user interface.
 *
 * @since 3.0
 */
@PseudoBean(singletonObserver = true)
public class PointDropperPanel extends ATitledPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String TITLE = ButtonType.POINT_DROPPER.getDisplayText();
    private static final int ICON_COLUMN_SIZE = 32;
    private static final String COT2525C = "CoT 2525C";
    private static final String COT2525D = "CoT 2525D";
    private static final String COT2525E = "CoT 2525E";
    private static final String SPOT_MAP = "Spot Map";

    private static final List<String> NON_COLORIZABLE_ICON_SETS = List.of(COT2525C, COT2525D, COT2525E, SPOT_MAP);
    private static final IconSetSelection MIL2525C_ICONS = new IconSetSelection(COT2525C);
    private static final IconSetSelection MIL2525D_ICONS = new IconSetSelection(COT2525D);
    private static final IconSetSelection MIL2525E_ICONS = new IconSetSelection(COT2525E);
    private static final IconSetSelection SPOT_MAP_ICONS = new IconSetSelection(SPOT_MAP);

    private final Map<IUserIcon, Icon> iconCache = new HashMap<>();

    private final RecentPointsTableModel recentPointsTableModel = new RecentPointsTableModel();
    private final IconSelectionHandler iconSelectionHandler = new IconSelectionHandler();
    private final ColorSelectionHandler colorSelectionHandler = new ColorSelectionHandler();

    private DefaultIconSelectionPanel defaultIconSelectionPanel;
    private AMil2525IconSelectionPanel mil2525cIconSelectionPanel;
    private AMil2525IconSelectionPanel mil2525dIconSelectionPanel;
    private AMil2525IconSelectionPanel mil2525eIconSelectionPanel;
    private SpotMapIconSelectionPanel spotMapIconSelectionPanel;
    private PointDropperSelection currentPointDropperSelection;
    private ISyntheticPluginDefinition currentIconDefinition;
    private IconSetSelection currentIconSetSelection;
    private IUserIconSet currentIconSet;
    private boolean changingSelection;
    private APointDropperIconSelectionPanel iconSelectionPanel;
    private JPanel colorsPanel;
    private JFrame colorsFrame;
    private Color iconColor = Color.WHITE;

    @Inject
    private IGlobeManager globeManager;

    @Inject
    private IIconSetManager iconSetManager;

    @Inject
    private PointDropperService pointDropper;

    public PointDropperPanel()
    {
        super(TITLE);
        initComponents();
    }

    @PostConstruct
    private void postConstruct()
    {
        // This has to happen in an @PC instead of in the constructor to avoid a race condition where the EDT
        // picks up the event *before* the injection processing happens at the end of the constructor.
        SwingUtilities.invokeLater(this::lateInit);
    }

    private void lateInit()
    {
        iconSelectionScrollPane.getVerticalScrollBar().setUnitIncrement(10);

        mil2525cIconSelectionPanel = new Mil2525cIconSelectionPanel(iconSelectionHandler);
        mil2525dIconSelectionPanel = new Mil2525dIconSelectionPanel(iconSelectionHandler);
        mil2525eIconSelectionPanel = new Mil2525eIconSelectionPanel(iconSelectionHandler);
        spotMapIconSelectionPanel = new SpotMapIconSelectionPanel(iconSelectionHandler);
        defaultIconSelectionPanel = new DefaultIconSelectionPanel(iconSelectionHandler);

        iconSetComboBox.addItem(MIL2525C_ICONS);
        iconSetComboBox.addItem(MIL2525D_ICONS);
        iconSetComboBox.addItem(MIL2525E_ICONS);
        iconSetComboBox.addItem(SPOT_MAP_ICONS);
        iconSetManager.getAllIconSets().forEach(iconSet -> iconSetComboBox.addItem(new IconSetSelection(iconSet.getName())));
        iconSetComboBox.setSelectedIndex(0);

        groupComboBox.setRenderer(new GroupListCellRenderer());

        locationCollapsiblePane.add(makeLocationInputPanel(), BorderLayout.CENTER);

        recentPointsTable.setModel(recentPointsTableModel);
        recentPointsTable.getColumnModel().getColumn(RecentPointsTableModel.Column.Icon.ordinal()).setMaxWidth(ICON_COLUMN_SIZE);
        recentPointsTable.addMouseListener(new RecentPointClickListener());

        importPointsButton.setEnabled(false); // TODO KMB: not yet implemented

        // Limit these fields to only numeric values
        indexTextField.setDocument(new LimitedDocument(Character::isDigit));
        incrementTextField.setDocument(new LimitedDocument(Character::isDigit));
    }

    /**
     * TODO KMB: Will swap to new LocationInputPanel once implemented
     * @return location input panel
     * @since 5.2
     */
    @Nonnull
    private LegacyLocationInputPanel makeLocationInputPanel()
    {
        final var locationInputPanel = new LegacyLocationInputPanel();

        locationInputPanel.addListener(new ILocationInputListener()
        {
            @Override
            public void onLocationSelected(@Nonnull ILocation location, @Nonnull CoordinateSystem coordinateSystem,
                                           @Nullable URI markerIconUri, @Nullable String cotType)
            {
                if (currentIconDefinition != null)
                {
                    // If an icon is selected, that means the point dropper is armed and already has plugin definition
                    // and values for a custom name (if set)
                    pointDropper.dropPoint(location);
                } else
                {
                    // If an icon isn't selected, WinTAK just treats it like a go-to, so let's do that too
                    //noinspection DataFlowIssue
                    globeManager.getLastFocusedGlobe().goTo(location);
                }
            }

            @Override
            public void onCancel()
            {
                // Nothing to do
            }
        });
        return locationInputPanel;
    }

    /**
     * Handles project closing to dispose the panel, since a new panel will be created as needed.
     *
     * @since 3.3
     */
    private void onProjectClosing(@Observes ProjectClosing ignored)
    {
        recentPointsTableModel.dispose();
    }

    /**
     * Activate the point dropper with a configuration built from our UI values.
     *
     * @since 5.1
     */
    private void activatePointDropper()
    {
        final String prefix = prefixTextField.getText().trim();
        final int startIndex = getValue(indexTextField, -1);
        final int increment = getValue(incrementTextField, 1);

        logger.debug("activate: {}", currentPointDropperSelection);

        // Capture these values instead of relying on our mutable internal state
        final var pointDropperSelection = currentPointDropperSelection;
        final var iconDefinition = currentIconDefinition;
        final var currentCotType = pointDropperSelection.cotType;
        final var currentSidc = pointDropperSelection.sidc;

        final var configBuilder = PointDropperConfiguration.builder(iconDefinition)
                .cotType(currentCotType)
                .sidc(currentSidc)
                .deactivator(new PointDropperConfiguration.OffGlobeOrComponentDeactivator(this))
                .initializer(mapObject -> {
                    // TODO: not sure I'm happy with this mechanism for spot markers.  I'll know more from TAKX-3219.

                    // Specified when using the Spot Map icon selection panel
                    final var attributeSet = pointDropperSelection.attributeSet;

                    if (attributeSet != null)
                    {
                        mapObject.getAttributeSet().putAll(attributeSet);
                    } else
                    {
                        // This ensures that the selected icon is properly transferred across CoT
                        final String path = iconDefinition.getIconURI().map(URI::getSchemeSpecificPart).orElse("");
                        mapObject.setMetaString(StandardAttributeNames.ICONSET_PATH, path);
                        mapObject.setMetaInteger(StandardAttributeNames.COLOR, iconColor.getRGB());
                        if (!NON_COLORIZABLE_ICON_SETS.contains(currentIconSetSelection.name) && !Color.WHITE.equals(iconColor))
                        {
                            mapObject.setExternalIconImage(GraphicsUtils.colorImage(iconDefinition.getIcon(), iconColor));
                        }
                    }

                    recentPointsTableModel.addRow(mapObject);
                }).deactivationHandler(() -> {
                    currentIconDefinition = null;
                    if (!changingSelection) iconSelectionPanel.clearSelection();
                });

        if (!prefix.isBlank() || startIndex > 0)
        {
            configBuilder.nameGenerator(new CustomNameGenerator(prefix, startIndex, increment));
        } else if (pointDropperSelection.nameGenerator != null)
        {
            configBuilder.nameGenerator(pointDropperSelection.nameGenerator);
        } else if (currentCotType != null && iconSelectionPanel instanceof AMil2525IconSelectionPanel)
        {
            configBuilder.useCotNamer();
        }

        pointDropper.activatePointDropper(configBuilder.build());
    }

    /**
     * @param textField    The text field from which to get the numerical value
     * @param defaultValue The default value if the text field does not contain a numerical value
     * @return The integer value in the text field or default if none provided
     */
    private int getValue(@Nonnull JTextField textField, int defaultValue)
    {
        return ParseUtils.parseInt(textField.getText(), defaultValue);
    }

    /**
     * Listen for MO removal and remove the MO from the recent points table if present.
     *
     * @since 3.1
     */
    @InvokeOnEDT
    private void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        recentPointsTableModel.removeRow(event.getMapObject());
    }

    /**
     * Clears the values in the text fields for a custom point name/number.
     */
    private void clearButtonActionPerformed()
    {
        prefixTextField.setText("");
        indexTextField.setText("");
        incrementTextField.setText("");
        updatePointDropperIfActive();
    }

    /**
     * If any values change while point dropper is active, we need to reactivate it to set the new values.
     */
    private void updatePointDropperIfActive()
    {
        if (currentIconDefinition != null) activatePointDropper();
    }

    private void iconSetComboBoxActionPerformed()
    {
        var oldIconSetSelection = currentIconSetSelection;
        currentIconSetSelection = (IconSetSelection) iconSetComboBox.getSelectedItem();

        if (!Objects.equals(oldIconSetSelection, currentIconSetSelection))
        {
            // Display the correct icons for the selected icon set
            if (currentIconSetSelection == null)
            {
                defaultIconSelectionPanel.setIcons(Collections.emptyList());
                iconSelectionPanel = defaultIconSelectionPanel;
            } else if (COT2525C.equals(currentIconSetSelection.name))
            {
                iconSelectionPanel = mil2525cIconSelectionPanel;
                groupPanel.setVisible(false);
            } else if (COT2525D.equals(currentIconSetSelection.name))
            {
                iconSelectionPanel = mil2525dIconSelectionPanel;
                groupPanel.setVisible(false);
            } else if (COT2525E.equals(currentIconSetSelection.name))
            {
                iconSelectionPanel = mil2525eIconSelectionPanel;
                groupPanel.setVisible(false);
            } else if (SPOT_MAP.equals(currentIconSetSelection.name))
            {
                iconSelectionPanel = spotMapIconSelectionPanel;
                groupPanel.setVisible(false);
            } else
            {
                iconSelectionPanel = defaultIconSelectionPanel;
                installSelectedIconSet(currentIconSetSelection);
            }

            pointDropper.deactivatePointDropper(); // Just in case it was active (WinTAK deactivates on switch too)
            currentIconDefinition = null;

            // Make sure no icons are selected from a previous use of the panel
            iconSelectionPanel.clearSelection();

            iconSelectionScrollPane.setViewportView(iconSelectionPanel);
        }
    }

    private void installSelectedIconSet(IconSetSelection iconSetSelection)
    {
        // Populate the groups from the newly selected icon set
        groupComboBox.removeAllItems();

        currentIconSet = iconSetManager.getIconSetByName(iconSetSelection.name);
        if (currentIconSet != null)
        {
            groupComboBox.setModel(new DefaultComboBoxModel<>(currentIconSet.getGroups().toArray(new String[0])));
            groupComboBox.setSelectedItem(currentIconSet.getSelectedGroup());
        }

        groupPanel.setVisible(true);
    }

    private void groupComboBoxActionPerformed()
    {
        final String group = (String) groupComboBox.getSelectedItem();
        if (group != null)
        {
            logger.debug("selected {}/{}", currentIconSet.getName(), group);

            currentIconSet.setSelectedGroup(group);
            final var iconsInGroup = currentIconSet.getIconsInGroup(group);

            iconsInGroup.sort(Comparator.comparing(IUserIcon::getFileName));

            defaultIconSelectionPanel.setIcons(iconsInGroup, iconColor);
        }
    }

    /**
     * Name generator that also updates the start index field as names are generated.
     *
     * @since 5.1
     */
    private class CustomNameGenerator extends CountingNameGenerator
    {
        CustomNameGenerator(String prefix, int start, int increment)
        {
            super(prefix, start, increment);
        }

        @Override
        public String generateName()
        {
            final String name = super.generateName();

            final int textFieldIndex = getValue(indexTextField, -1);
            if (textFieldIndex != -1) indexTextField.setText(String.valueOf(currentIndex));

            return name;
        }
    }

    /**
     * Handles a user selection of an icon in an icon selection panel.
     * @since 5.2
     */
    private class IconSelectionHandler implements Consumer<PointDropperSelection>
    {
        @Override
        public void accept(PointDropperSelection selection)
        {
            changingSelection = true;
            pointDropper.deactivatePointDropper(); // In case we're already active and switching icons
            changingSelection = false;

            logger.debug("selected PD: {}", selection.definition);

            currentPointDropperSelection = selection;
            currentIconDefinition = selection.definition;

            if (currentIconDefinition != null) activatePointDropper();
        }
    }

    /**
     * Handles a user selection of a color in a {@link ColorSelector} panel
     * @since 5.3
     */
    private class ColorSelectionHandler implements Consumer<TakColor>
    {
        @Override
        public void accept(TakColor selectedColor)
        {
            iconColor = selectedColor.getColor();
            colorSelectorButton.setBackground(iconColor);
            if (iconSelectionPanel != null && !NON_COLORIZABLE_ICON_SETS.contains(currentIconSetSelection.name))
            {
                if (!(groupComboBox.getSelectedItem() instanceof String currentGroup)) return;
                final var iconsInGroup = currentIconSet.getIconsInGroup(currentGroup).stream().sorted(Comparator.comparing(IUserIcon::getFileName)).toList();
                ((DefaultIconSelectionPanel) iconSelectionPanel).setIcons(iconsInGroup, iconColor);
                colorsFrame.setVisible(false);
            }
        }
    }

    /**
     * Click listener to navigate the globe to the location of a recent point.
     * @since 5.2
     */
    private class RecentPointClickListener extends MouseAdapter
    {
        @Override
        public void mouseClicked(MouseEvent mouseEvent)
        {
            int row = recentPointsTable.rowAtPoint(mouseEvent.getPoint());
            if (row >= 0)
            {
                // TODO don't change the zoom, just center on the location
                //noinspection DataFlowIssue
                globeManager.getLastFocusedGlobe().goTo(recentPointsTableModel.getRowObject(row).getLocation());
            }
        }
    }

    /**
     * Custom renderer to show the icon for a given group.
     * @since 5.2
     */
    private class GroupListCellRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            final String group = (String) value;
            final IUserIcon iconForGroup = group != null ? currentIconSet.getIconForGroup(group) : null;
            final BufferedImage image = iconForGroup != null
                    ? iconSetManager.getIconImage(currentIconSet.getUid(), iconForGroup.getFileName())
                    : null;
            final Icon icon = image != null
                    ? iconCache.computeIfAbsent(iconForGroup, k -> new ImageIcon(image))
                    : null;
            setIcon(icon);

            return this;
        }
    }

    /**
     * @param definition The plugin definition for the selected icon, or {@code null} if none selected
     * @param cotType The CoT type to use for any dropped points (can be {@code null} to use unknown ground)
     * @param sidc The Mil-Std-2525 symbol code for the dropped point. Used to facilitate more detail for icon rendering.
     *             May be {@code null} to skip extra details.
     * @param attributeSet Optional attribute set to override the standard handling of attributes, used by Spot Map icons
     * @param nameGenerator Optional name generator, used by Spot Map icons
     * @since 5.2 (signature change in 5.3)
     */
    record PointDropperSelection(@Nullable ISyntheticPluginDefinition definition, @Nullable String cotType, @Nullable String sidc,
            @Nullable AttributeSet attributeSet, @Nullable INameGenerator nameGenerator)
    {
        PointDropperSelection(@Nullable ISyntheticPluginDefinition definition, @Nullable String cotType, @Nullable String sidc)
        {
            this(definition, cotType, sidc, null, null);
        }
    }

    private record IconSetSelection(String name)
    {
        @Override
        public String toString()
        {
            return name;
        }
    }

    /**
     * Creates a new {@link ColorSelector} panel (if one doesn't exist), and pops it up just under the color selector button
     * @since 5.3
     */
    private void showColorSelector()
    {
        if (colorsPanel != null && colorsPanel.isShowing()) return; // Skip if already showing
        if (colorsFrame == null || colorsPanel == null) createColorSelectorElements();

        Point pos = colorSelectorButton.getLocationOnScreen();
        colorsFrame.add(colorsPanel);
        colorsFrame.setLocation(pos.x - colorsPanel.getWidth() + colorSelectorButton.getWidth(), pos.y + colorSelectorButton.getHeight());
        colorsFrame.pack();
        colorsFrame.setVisible(true);
        colorsFrame.requestFocus();
        colorsFrame.addWindowFocusListener(new WindowAdapter()
        {
            @Override
            public void windowLostFocus(WindowEvent e)
            {
                colorsFrame.setVisible(false);
                colorsFrame.removeWindowFocusListener(this);
            }
        });
    }

    /**
     * Creates and properly sets up the frame and {@link ColorSelector} needed to display the color swatches
     * @since 5.3
     */
    private void createColorSelectorElements()
    {
        colorsFrame = new JFrame();
        colorsFrame.setType(Window.Type.UTILITY);
        colorsFrame.setUndecorated(true);
        colorsFrame.addKeyListener(new EscapeKeyListener(() -> colorsFrame.setVisible(false)));
        colorsPanel = new ColorSelector(0, 5, TakColor.getMarkerPalette(), colorSelectionHandler);
        colorsPanel.revalidate();

        // If not auto-sized, show & pack it to be ready for next time. Do off-screen so it's invisible to the user
        if (colorsPanel.getWidth() <= 5)
        {
            Popup popup = PopupFactory.getSharedInstance().getPopup(colorSelectorButton, colorsPanel, Integer.MAX_VALUE / 2, 0);
            popup.show();
            SwingUtilities.invokeLater(() -> {
                popup.hide();
                showColorSelector();
            });
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var addPointsPane = new CollapsiblePanel();
        var scrollPane1 = new JScrollPane();
        var instructionsLabel = new JTextArea();
        iconSetComboBox = new JComboBox<>();
        var nameFieldsPanel = new JPanel();
        prefixTextField = new PlaceholderTextField();
        indexTextField = new PlaceholderTextField();
        incrementTextField = new PlaceholderTextField();
        var clearButton = new JButton();
        groupPanel = new JPanel();
        groupComboBox = new JComboBox<>();
        colorSelectorButton = new JButton();
        iconSelectionScrollPane = new JScrollPane();
        locationCollapsiblePane = new CollapsiblePanel();
        var recentlyAddedPane = new CollapsiblePanel();
        var buttonsPanel = new JPanel();
        importPointsButton = new JButton();
        var recentlyAddedScrollPane = new JScrollPane();
        recentPointsTable = new JTable();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "2*(default, min), fill:default:grow, min, default"));

        //======== addPointsPane ========
        {
            addPointsPane.setTitle("Add Points");
            addPointsPane.setName("addPointsPane");
            addPointsPane.setLayout(new FormLayout(
                    "$lcgap, default:grow, $lcgap",
                    "$lgap, [40px,default,80px], 3*($lgap, default), $lgap, top:[125px,default], $lgap"));

            //======== scrollPane1 ========
            {
                scrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                scrollPane1.setViewportBorder(null);
                scrollPane1.setBorder(null);
                scrollPane1.setName("scrollPane1");

                //---- instructionsLabel ----
                instructionsLabel.setText("Select an icon to activate globe selection for dropping point(s). Deselect the icon or use ESC to cancel. You may also enter a location below instead of clicking on the globe.");
                instructionsLabel.setLineWrap(true);
                instructionsLabel.setWrapStyleWord(true);
                instructionsLabel.setEditable(false);
                instructionsLabel.setFocusable(false);
                instructionsLabel.setBorder(null);
                instructionsLabel.setName("instructionsLabel");
                scrollPane1.setViewportView(instructionsLabel);
            }
            addPointsPane.add(scrollPane1, cc.xy(2, 2));

            //---- iconSetComboBox ----
            iconSetComboBox.setName("iconSetComboBox");
            iconSetComboBox.addActionListener(e -> iconSetComboBoxActionPerformed());
            addPointsPane.add(iconSetComboBox, cc.xy(2, 4));

            //======== nameFieldsPanel ========
            {
                nameFieldsPanel.setName("nameFieldsPanel");
                nameFieldsPanel.setLayout(new FormLayout(
                        "3*(default:grow, $lcgap), default",
                        "default"));

                //---- prefixTextField ----
                prefixTextField.setPlaceholderText("Custom Prefix");
                prefixTextField.setName("prefixTextField");
                nameFieldsPanel.add(prefixTextField, cc.xy(1, 1));

                //---- indexTextField ----
                indexTextField.setPlaceholderText("Index");
                indexTextField.setName("indexTextField");
                nameFieldsPanel.add(indexTextField, cc.xy(3, 1));

                //---- incrementTextField ----
                incrementTextField.setPlaceholderText("Increment");
                incrementTextField.setName("incrementTextField");
                nameFieldsPanel.add(incrementTextField, cc.xy(5, 1));

                //---- clearButton ----
                clearButton.setText("Clear");
                clearButton.setName("clearButton");
                clearButton.addActionListener(e -> clearButtonActionPerformed());
                nameFieldsPanel.add(clearButton, cc.xy(7, 1));
            }
            addPointsPane.add(nameFieldsPanel, cc.xy(2, 6));

            //======== groupPanel ========
            {
                groupPanel.setName("groupPanel");
                groupPanel.setLayout(new FormLayout(
                        "default:grow, $lcgap, default",
                        "default"));

                //---- groupComboBox ----
                groupComboBox.setName("groupComboBox");
                groupComboBox.addActionListener(e -> groupComboBoxActionPerformed());
                groupPanel.add(groupComboBox, cc.xy(1, 1));

                //---- colorSelectorButton ----
                colorSelectorButton.setPreferredSize(new Dimension(34, 34));
                colorSelectorButton.setBackground(Color.white);
                colorSelectorButton.setName("colorSelectorButton");
                colorSelectorButton.addActionListener(e -> showColorSelector());
                groupPanel.add(colorSelectorButton, cc.xy(3, 1));
            }
            addPointsPane.add(groupPanel, cc.xy(2, 8));

            //======== iconSelectionScrollPane ========
            {
                iconSelectionScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                iconSelectionScrollPane.setBorder(null);
                iconSelectionScrollPane.setName("iconSelectionScrollPane");
            }
            addPointsPane.add(iconSelectionScrollPane, cc.xy(2, 10));
        }
        add(addPointsPane, cc.xy(1, 1));

        //======== locationCollapsiblePane ========
        {
            locationCollapsiblePane.setTitle("Location");
            locationCollapsiblePane.setName("locationCollapsiblePane");
            locationCollapsiblePane.setLayout(new BorderLayout(1, 1));
        }
        add(locationCollapsiblePane, cc.xy(1, 3));

        //======== recentlyAddedPane ========
        {
            recentlyAddedPane.setTitle("Recently Added");
            recentlyAddedPane.setName("recentlyAddedPane");
            recentlyAddedPane.setLayout(new FormLayout(
                    "default:grow",
                    "$lgap, default, $lgap, fill:default:grow"));

            //======== buttonsPanel ========
            {
                buttonsPanel.setName("buttonsPanel");
                buttonsPanel.setLayout(new FormLayout(
                        "$lcgap, default, $lcgap, default:grow, $lcgap, default, $lcgap",
                        "$lgap, default, $lgap"));

                //---- importPointsButton ----
                importPointsButton.setText("Import Points");
                importPointsButton.setName("importPointsButton");
                buttonsPanel.add(importPointsButton, cc.xy(2, 2));
            }
            recentlyAddedPane.add(buttonsPanel, cc.xy(1, 2));

            //======== recentlyAddedScrollPane ========
            {
                recentlyAddedScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                recentlyAddedScrollPane.setName("recentlyAddedScrollPane");

                //---- recentPointsTable ----
                recentPointsTable.setName("recentPointsTable");
                recentlyAddedScrollPane.setViewportView(recentPointsTable);
            }
            recentlyAddedPane.add(recentlyAddedScrollPane, cc.xy(1, 4));
        }
        add(recentlyAddedPane, cc.xy(1, 5));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JComboBox<IconSetSelection> iconSetComboBox;
    private PlaceholderTextField prefixTextField;
    private PlaceholderTextField indexTextField;
    private PlaceholderTextField incrementTextField;
    private JPanel groupPanel;
    private JComboBox<String> groupComboBox;
    private JButton colorSelectorButton;
    private JScrollPane iconSelectionScrollPane;
    private CollapsiblePanel locationCollapsiblePane;
    private JButton importPointsButton;
    private JTable recentPointsTable;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
