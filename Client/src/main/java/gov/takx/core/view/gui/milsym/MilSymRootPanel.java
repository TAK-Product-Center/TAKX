package gov.takx.core.view.gui.milsym;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.symbology.Affiliation;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ShapeType;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.milsym.CotSidcAdapter;
import gov.takx.api.milsym.Echelon;
import gov.takx.api.milsym.IMilStd2525Service;
import gov.takx.api.milsym.IMilStd2525ServiceProvider;
import gov.takx.api.milsym.IMilSymIconBuilder;
import gov.takx.api.milsym.ISymbolSelection;
import gov.takx.api.milsym.Presence;
import gov.takx.api.milsym.SymbolWrapper;
import gov.takx.commons.swing.PlaceholderTextField;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.milsym.SymbolSelection;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static gov.takx.api.milsym.MilStd2525Variant.CHARLIE;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Root panel for navigating through available {@link gov.tak.api.symbology.ISymbolTable.Symbol} entries.
 *
 * @since 5.1
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MilSymRootPanel extends JPanel
{
    private static final ImageIcon ICON_POINT = UIResources.getActionButtonIcon(PLATFORM_ICONS, "icon_add_filled.png");
    private static final ImageIcon ICON_LINE = UIResources.getActionButtonIcon(PLATFORM_ICONS, "multi_polyline.png");
    private static final ImageIcon ICON_POLYGON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "polygon.png");
    private static final ImageIcon ICON_RECTANGLE = UIResources.getActionButtonIcon(PLATFORM_ICONS, "square.png");
    private static final ImageIcon ICON_CIRCLE = UIResources.getActionButtonIcon(PLATFORM_ICONS, "circle.png");
    private static final ImageIcon ICON_ELLIPSE = UIResources.getActionButtonIcon(PLATFORM_ICONS, "ellipse.png");
    private static final ImageIcon ICON_SETTINGS = UIResources.getActionButtonIcon(PLATFORM_ICONS, "settings.png");
    private final MilSymSearchResultsPanel searchResultsPanel;
    private final MilSymTreeDelegatePanel symbolTreePanel;
    private final MilSymAmplifierPanel amplifierPanel;
    private final IMilStd2525Service milStd2525Service;
    private Consumer<ISymbolSelection> symbolConsumer;
    private boolean cotSupport;
    private String lastSearchText;

    @Inject
    private IMilStd2525ServiceProvider milStd2525ServiceProvider;

    /**
     * @param milStd2525Service MIL-STD-2525 service to pull symbols from.
     */
    public MilSymRootPanel(@Nonnull IMilStd2525Service milStd2525Service)
    {
        this.milStd2525Service = milStd2525Service;

        searchResultsPanel = new MilSymSearchResultsPanel(milStd2525Service, this::onSelectSymbol);
        symbolTreePanel = new MilSymTreeDelegatePanel(milStd2525Service, this::onSelectSymbol);
        amplifierPanel = new MilSymAmplifierPanel(milStd2525Service, this::scheduleFilterAndAdapterUpdate);

        initComponents();

        searchTextField.setPlaceholderText("Search...");
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
        // We want an array to back our combo-box model that includes 'null' in the first slot since that
        // will be what represents 'any'.
        ShapeType[] shapeTypes = ShapeType.values();
        ShapeType[] selectableShapeTypes = new ShapeType[shapeTypes.length + 1];
        System.arraycopy(shapeTypes, 0, selectableShapeTypes, 1, shapeTypes.length);
        DefaultComboBoxModel<ShapeType> shapeTypeModel = new DefaultComboBoxModel<>(selectableShapeTypes);

        // Adapt the prior selection (if set externally before this initialized) and then update the
        // combo-box with the new model / renderer.
        if (shapeComboBox.getSelectedItem() instanceof ShapeType priorShapeSelection) shapeTypeModel.setSelectedItem(priorShapeSelection);
        shapeComboBox.setModel(shapeTypeModel);
        shapeComboBox.setRenderer(new ShapeComboRenderer());

        // Initially we want to show the selection panel.
        contentPanel.add(symbolTreePanel, new CellConstraints().xy(1, 1));

        // We set the size of the text-field so that it aligns with the size of the combo-boxes.
        searchTextField.setPreferredSize(new Dimension(200, 30));

        // Initialize default display.
        scheduleFilterAndAdapterUpdate();
    }

    /**
     * Sets a custom symbol selection listener. If a custom listener is set, the listener will be notified when a new
     * symbol is selected and no shape will be drawn. The caller should clear the listener by passing {@code null} when
     * its operation is complete if the panel instance will be retained.
     *
     * @param symbolConsumer The symbol consumer
     */
    public void setCustomListener(@Nullable Consumer<ISymbolSelection> symbolConsumer)
    {
        this.symbolConsumer = symbolConsumer;
    }

    /**
     * Updates filtering to ensure only symbols that can be converted to CoT types are shown, if desired.
     *
     * @param cotSupport {@code true} for showing only symbols with CoT support. {@code false} to not consider CoT support.
     */
    public void setCotSupportFiltering(boolean cotSupport)
    {
        this.cotSupport = cotSupport;
    }

    /**
     * Sets the currently selected shape type and optionally locks the shape selection combo box. If a {@code null}
     * shape type is passed, shape selection will always be enabled.
     *
     * @param shapeType The shape type to select or {@code null} to clear the selection
     * @param enableShapeSelection Pass {@code false} if the shape selection combo box should be disabled. If a
     * {@code null} shape type is provided, the combo box will always be enabled.
     */
    public void setShapeType(@Nullable ShapeType shapeType, boolean enableShapeSelection)
    {
        if (shapeType != null)
        {
            shapeComboBox.setEnabled(enableShapeSelection);
            shapeComboBox.setVisible(enableShapeSelection);
            shapeLabel.setVisible(enableShapeSelection);

            // Hide the amplifiers display for non-point shapes.
            boolean showPointModifiers = shapeType == ShapeType.Point;
            amplifiersLabel.setVisible(showPointModifiers);
            amplifiersButton.setVisible(showPointModifiers);
        } else
        {
            shapeComboBox.setEnabled(true);
            shapeComboBox.setVisible(true);
            shapeLabel.setVisible(true);
            amplifiersLabel.setVisible(true);
            amplifiersButton.setVisible(true);
        }

        // Odd swing quirk:
        // We need to update the selection on the model, not the combo-box.
        // If we call this directly on the combo-box before the up-to-date model
        // is assigned to it (which is done above via post-construct) it would be no-op.
        // If we call this on the model (empty default model) our change will stick.
        shapeComboBox.getModel().setSelectedItem(shapeType);
    }

    /**
     * Sets the given symbol as the initial selection in the tree display,
     * and updates the configurable amplifiers to match the states in the given symbol.
     *
     * @param initialSymbol The initial symbol to show as being selected. Can be {@code null} to start at the root of the selection tree.
     */
    public void setInitialSymbol(@Nullable String initialSymbol)
    {
        if (initialSymbol != null)
        {
            symbolTreePanel.setInitialSymbol(initialSymbol);

            // Extract initial amplifier state from the symbol
            Affiliation affiliation = milStd2525Service.getAffiliation(initialSymbol);
            Echelon echelon = milStd2525Service.getEchelon(initialSymbol);
            Presence presence = milStd2525Service.getPresence(initialSymbol);
            setAffiliation(affiliation);
            setEchelon(echelon);
            setPresence(presence);
        }
    }

    /**
     * @param affiliation Affiliation to set.
     * @since 5.3
     */
    public void setAffiliation(@Nonnull Affiliation affiliation)
    {
        amplifierPanel.setAffiliation(affiliation);
    }

    /**
     * @param echelon Echelon to set.
     * @since 5.3
     */
    public void setEchelon(@Nonnull Echelon echelon)
    {
        amplifierPanel.setEchelon(echelon);
    }

    /**
     * @param presence Presence to set.
     * @since 5.3
     */
    public void setPresence(@Nonnull Presence presence)
    {
        amplifierPanel.setPresence(presence);
    }

    /**
     * Called when an {@link MilSymEntryPanel} is clicked on within the display panels.
     *
     * @param symbol Selected symbol.
     */
    private void onSelectSymbol(@Nonnull ISymbolTable.Symbol symbol)
    {
        String symbolCode = symbol.getCode();
        symbolCode = milStd2525Service.setAffiliation(symbolCode, getTargetAffiliation());
        symbolCode = milStd2525Service.setEchelon(symbolCode, getTargetEchelon());
        symbolCode = milStd2525Service.setPresence(symbolCode, getTargetPresence());

        if (symbolConsumer != null)
        {
            symbolConsumer.accept(new SymbolSelection(symbol, symbolCode));
            return;
        }

        milStd2525Service.initiateDraw(symbolCode, symbol.getName(), getTargetShape());
    }

    /**
     * Called when {@link #searchTextField} is updated.
     */
    private void searchTextFieldKeyReleased()
    {
        String searchText = searchTextField.getText();
        if (searchText.equals(lastSearchText))
        {
            // Text hasn't changed from key-release, don't re-filter/update content.
            return;
        } else if (StringUtils.isEmpty(searchText))
        {
            // We have nothing to search.
            displayRegularContent();
        } else
        {
            // We have something specific to search for.
            displaySearchResults();
        }

        lastSearchText = searchText;

        scheduleFilterAndAdapterUpdate();
    }

    /**
     * Called when {@link #shapeComboBox} is updated.
     */
    private void shapeComboBoxItemStateChanged()
    {
        scheduleFilterAndAdapterUpdate();
    }

    /**
     * Called when {@link #amplifiersButton} is clicked.
     */
    private void onRequestAmplifiersPopup()
    {
        // Skip if already showing
        if (amplifierPanel.isShowing()) return;

        // If the amplifiers panel hasn't auto-sized, show it, pack it, so it will be ready for next time.
        // We'll do this all off-screen so the user doesn't see anything.
        // This step is necessary so that we can show it again later at the correct coordinates (which take in the width of the panel)
        if (amplifierPanel.getWidth() <= 5)
        {
            PopupFactory factory = PopupFactory.getSharedInstance();
            Popup popup = factory.getPopup(amplifiersButton, amplifierPanel, Integer.MAX_VALUE / 2, 0);
            popup.show();
            SwingUtilities.invokeLater(() -> {
                popup.hide();
                onRequestAmplifiersPopup();
            });
            return;
        }

        // Show the popover (Ok so, it's a JFrame but its acting like a popup. We need to use JFrame for proper focus lost event handling though)
        Point pos = amplifiersButton.getLocationOnScreen();
        int x = pos.x - amplifierPanel.getWidth() + amplifiersButton.getWidth();
        int y = pos.y + amplifiersButton.getHeight();
        JFrame popover = new JFrame();
        popover.setUndecorated(true);
        popover.setType(Window.Type.UTILITY); // Hide the popup from the taskbar
        popover.add(amplifierPanel);
        popover.setLocation(x, y);
        popover.pack();
        popover.setVisible(true);
        popover.requestFocus();

        // Focus the panel and hide the popup when focus is lost.
        popover.addWindowFocusListener(new WindowAdapter()
        {
            @Override
            public void windowLostFocus(WindowEvent e)
            {
                popover.setVisible(false);
                popover.removeWindowFocusListener(this);
            }
        });
    }

    /**
     * @return {@code true} when the current UI is the {@link #searchResultsPanel}.
     * {@code false} when the current UI is the {@link #symbolTreePanel}.
     */
    private boolean isSearchShowing()
    {
        // If no content is shown yet, we're not searching, as this only happens at startup.
        // In that case we do not want to show the search results panel first.
        if (contentPanel.getComponents().length == 0) return false;

        return contentPanel.getComponent(0) == searchResultsPanel;
    }

    /**
     * Display {@link #symbolTreePanel}.
     */
    private void displayRegularContent()
    {
        // If we had search results previously, display the regular selection panel
        if (!StringUtils.isEmpty(lastSearchText))
        {
            CellConstraints cc = new CellConstraints();
            contentPanel.removeAll();
            contentPanel.add(symbolTreePanel, cc.xy(1, 1));
            scheduleFilterAndAdapterUpdate();
        }
    }

    /**
     * Display {@link #searchResultsPanel}.
     */
    private void displaySearchResults()
    {
        // If we had no search results previously, display the search results panel
        if (StringUtils.isEmpty(lastSearchText))
        {
            CellConstraints cc = new CellConstraints();
            contentPanel.removeAll();
            contentPanel.add(searchResultsPanel, cc.xy(1, 1));
            scheduleFilterAndAdapterUpdate();
        }
    }

    /**
     * Refresh/repaint the UI.
     */
    @InvokeOnEDT
    private void refresh()
    {
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Schedule a call to {@link #updateFilterAndAdapter()} on the next EDT update.
     */
    private void scheduleFilterAndAdapterUpdate()
    {
        // Running async moves off the EDT, then the call will go back on the EDT next update.
        // This is REQUIRED since some of the listener logic calling this happen before values are updated.
        CompletableFuture.runAsync(this::updateFilterAndAdapter);
    }

    /**
     * Applies the current search inputs as a filter / adapter for the currently displayed panel.
     */
    @InvokeOnEDT
    private void updateFilterAndAdapter()
    {
        String lowerSearchText = searchTextField.getText();
        ShapeType shape = getTargetShape();
        Affiliation affiliation = getTargetAffiliation();
        Echelon echelon = getTargetEchelon();
        Presence presence = getTargetPresence();
        Predicate<SymbolWrapper> filter = buildFilter(lowerSearchText, shape);
        Consumer<IMilSymIconBuilder> adapter = iconBuilder -> iconBuilder
                .setShapeType(shape)
                .setEchelon(echelon)
                .setPresence(presence)
                .setAffiliation(affiliation);
        CompletableFuture.runAsync(() -> {
            if (isSearchShowing())
            {
                searchResultsPanel.setIconAdapter(adapter);
                searchResultsPanel.applyNewFilter(filter);
            } else
            {
                symbolTreePanel.setIconAdapter(adapter);
                symbolTreePanel.setShape(shape);
                symbolTreePanel.applyNewFilter(filter);
            }
            refresh();
        });
    }

    /**
     * @return Current target shape.
     */
    @Nullable
    private ShapeType getTargetShape()
    {
        return (ShapeType) shapeComboBox.getSelectedItem();
    }

    /**
     * @return Current target affiliation.
     */
    @Nonnull
    private Affiliation getTargetAffiliation()
    {
        return amplifierPanel.getAffiliation();
    }

    /**
     * @return Current target presence.
     */
    @Nonnull
    public Presence getTargetPresence()
    {
        return amplifierPanel.getPresence();
    }

    /**
     * @return Current target echelon.
     */
    @Nonnull
    private Echelon getTargetEchelon()
    {
        return amplifierPanel.getEchelon();
    }

    /**
     * @param lowerSearchText Lower cased search value.
     * Used to match against symbol names in a case-insensitive manner.
     * @param targetShape The current selected target shape.
     * Used to match against symbols that support the given shape type.
     * If the target shape is {@code null} then any shape type is allowed.
     *
     * @return A predicate matching symbols containing the given search text in their name
     * and that are compatible with the given shape <i>(if one is provided)</i>.
     */
    @Nonnull
    protected Predicate<SymbolWrapper> buildFilter(@Nonnull String lowerSearchText, @Nullable ShapeType targetShape)
    {
        Predicate<SymbolWrapper> predicate = filterByName(lowerSearchText).and(filterByShape(targetShape));
        if (cotSupport) predicate = predicate.and(filterByCotSupport());
        return predicate;
    }

    @Nonnull
    private Predicate<SymbolWrapper> filterByCotSupport()
    {
        return symbolWrapper -> {
            // Convert to 2525C and check if we have CoT support for that type
            String symbolCodeC = milStd2525ServiceProvider.get2525Service(CHARLIE).convertFromOther2525Variant(symbolWrapper.getSymbol().getCode());
            return symbolCodeC != null && CotSidcAdapter.isCotCompatibleMil2525C(symbolCodeC);
        };
    }

    @Nonnull
    private static Predicate<SymbolWrapper> filterByName(@Nonnull String search)
    {
        String[] lowerSearchTerms = search.toLowerCase().split("\\s+");
        return symbolWrapper -> {
            // The kernel annotates 'Entry.getName()' as '@NotNull' but it is null for parent folder names
            // for top level unit types in 2525 D/E.
            String parentFolderName = symbolWrapper.getContainingFolder().getName();
            String lowerSymbolName = symbolWrapper.getSymbol().getName().toLowerCase();
            String lowerFolderName = parentFolderName == null ? "" : parentFolderName.toLowerCase();
            for (String term : lowerSearchTerms)
            {
                if (!lowerSymbolName.contains(term) && !lowerFolderName.contains(term)) return false;
            }
            return true;
        };
    }

    @Nonnull
    private static Predicate<SymbolWrapper> filterByShape(@Nullable ShapeType shapeType)
    {
        if (shapeType == null) return symbolWrapper -> true;
        return symbolWrapper -> symbolWrapper.supportsShape(shapeType);
    }

    /**
     * ComboBox renderer for {@link ShapeType} values.
     */
    private static class ShapeComboRenderer extends BasicComboBoxRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof ShapeType shapeType)
            {
                ImageIcon icon = switch (shapeType)
                {
                    case Point -> ICON_POINT;
                    case LineString -> ICON_LINE;
                    case Polygon -> ICON_POLYGON;
                    case Rectangle -> ICON_RECTANGLE;
                    case Circle -> ICON_CIRCLE;
                    case Ellipse -> ICON_ELLIPSE;
                };
                setIcon(icon);
                setText(shapeType.name());
            } else
            {
                setText("Any");
                setIcon(null);
            }
            return this;
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var searchWrapperPanel = new JPanel();
        var searchLabel = new JLabel();
        shapeLabel = new JLabel();
        amplifiersLabel = new JLabel();
        searchTextField = new PlaceholderTextField();
        shapeComboBox = new JComboBox<>();
        amplifiersButton = new JButton();
        amplifiersButton.setIcon(ICON_SETTINGS);
        contentPanel = new JPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default, fill:default:grow"));

        //======== searchWrapperPanel ========
        {
            searchWrapperPanel.setBorder(new EmptyBorder(6, 6, 6, 6));
            searchWrapperPanel.setName("searchWrapperPanel");
            searchWrapperPanel.setLayout(new FormLayout(
                    "default:grow, 2*($ugap, center:default)",
                    "default, default:grow"));

            //---- searchLabel ----
            searchLabel.setText("Search");
            searchLabel.setBorder(new EmptyBorder(0, 5, 0, 0));
            searchLabel.setName("searchLabel");
            searchWrapperPanel.add(searchLabel, cc.xy(1, 1));

            //---- shapeLabel ----
            shapeLabel.setText("Shape Filter");
            shapeLabel.setName("shapeLabel");
            searchWrapperPanel.add(shapeLabel, cc.xy(3, 1));

            //---- amplifiersLabel ----
            amplifiersLabel.setText("Amplifiers");
            amplifiersLabel.setName("amplifiersLabel");
            searchWrapperPanel.add(amplifiersLabel, cc.xy(5, 1));

            //---- searchTextField ----
            searchTextField.setToolTipText("Filter symbols by name");
            searchTextField.setName("searchTextField");
            searchTextField.addKeyListener(new KeyAdapter()
            {
                @Override
                public void keyReleased(KeyEvent e)
                {
                    searchTextFieldKeyReleased();
                }
            });
            searchWrapperPanel.add(searchTextField, cc.xy(1, 2));

            //---- shapeComboBox ----
            shapeComboBox.setName("shapeComboBox");
            shapeComboBox.addItemListener(e -> shapeComboBoxItemStateChanged());
            searchWrapperPanel.add(shapeComboBox, cc.xy(3, 2));

            //---- amplifiersButton ----
            amplifiersButton.setText("Configure");
            amplifiersButton.setName("amplifiersButton");
            amplifiersButton.addActionListener(e -> onRequestAmplifiersPopup());
            searchWrapperPanel.add(amplifiersButton, cc.xy(5, 2));
        }
        add(searchWrapperPanel, cc.xy(1, 1));

        //======== contentPanel ========
        {
            contentPanel.setName("contentPanel");
            contentPanel.setLayout(new FormLayout(
                    "default:grow",
                    "fill:default:grow"));
        }
        add(contentPanel, cc.xy(1, 2));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel shapeLabel;
    private JLabel amplifiersLabel;
    private PlaceholderTextField searchTextField;
    private JComboBox<ShapeType> shapeComboBox;
    private JButton amplifiersButton;
    private JPanel contentPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
