package gov.takx.core.view.gui.pointdropper;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.symbology.Affiliation;
import gov.tak.api.symbology.ShapeType;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.milsym.CotSidcAdapter;
import gov.takx.api.milsym.IMilStd2525Service;
import gov.takx.api.milsym.IMilStd2525ServiceProvider;
import gov.takx.api.milsym.ISymbolChooser;
import gov.takx.api.milsym.ISymbolSelection;
import gov.takx.api.milsym.MilStd2525Variant;
import gov.takx.api.plugin.synthetic.ISyntheticPluginDefinition;
import gov.takx.api.pointdropper.CotIcon;
import gov.takx.commons.swing.WrapLayout;
import gov.takx.core.plugin.synthetic.SyntheticPluginDefinition;
import gov.takx.core.view.gui.pointdropper.PointDropperPanel.PointDropperSelection;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.jdesktop.swingx.HorizontalLayout;

import javax.swing.AbstractButton;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static gov.takx.api.mapobject.MapObjectOptions.STANDARD_RESOURCE_OPTIONS;
import static gov.takx.api.milsym.CotSidcAdapter.DEFAULT_COT_ASSUMED_FRIEND;
import static gov.takx.api.milsym.CotSidcAdapter.DEFAULT_COT_EXERCISE_ASSUMED_FRIEND;
import static gov.takx.api.milsym.CotSidcAdapter.DEFAULT_COT_EXERCISE_FRIEND;
import static gov.takx.api.milsym.CotSidcAdapter.DEFAULT_COT_EXERCISE_NEUTRAL;
import static gov.takx.api.milsym.CotSidcAdapter.DEFAULT_COT_EXERCISE_PENDING;
import static gov.takx.api.milsym.CotSidcAdapter.DEFAULT_COT_EXERCISE_UNKNOWN;
import static gov.takx.api.milsym.CotSidcAdapter.DEFAULT_COT_FAKER;
import static gov.takx.api.milsym.CotSidcAdapter.DEFAULT_COT_FRIEND;
import static gov.takx.api.milsym.CotSidcAdapter.DEFAULT_COT_HOSTILE;
import static gov.takx.api.milsym.CotSidcAdapter.DEFAULT_COT_JOKER;
import static gov.takx.api.milsym.CotSidcAdapter.DEFAULT_COT_NEUTRAL;
import static gov.takx.api.milsym.CotSidcAdapter.DEFAULT_COT_PENDING;
import static gov.takx.api.milsym.CotSidcAdapter.DEFAULT_COT_SUSPECT;
import static gov.takx.api.milsym.CotSidcAdapter.DEFAULT_COT_UNKNOWN;
import static gov.takx.api.milsym.CotSidcAdapter.cotTypeFromMil2525C;
import static gov.takx.api.milsym.CotSidcAdapter.mil2525CFromCotType;
import static gov.takx.api.milsym.MilStd2525Variant.CHARLIE;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Icon selection panel for MIL-STD-2525 icons.
 *
 * @since 5.5 (renamed after making class abstract)
 */
@PseudoBean
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
public abstract class AMil2525IconSelectionPanel extends APointDropperIconSelectionPanel
{
    private static final Dimension BUTTON_SIZE = new Dimension(38, 38);
    private static final int ICON_SIZE = 32;

    private final Map<String, ISyntheticPluginDefinition> cotTypeToDefinitionMap = new HashMap<>();
    private final DefaultComboBoxModel<MilStdUnit> typeComboBoxModel = new DefaultComboBoxModel<>();
    private final SymbolToggleButton[] symbolButtons = new SymbolToggleButton[4];
    private final MilStdUnit defaultUnit = provideDefaultUnit();
    private final List<MilStdUnit> units = provideUnits();
    private final MilStd2525Variant milStd2525Variant;

    @Inject
    protected IMilStd2525ServiceProvider milStd2525ServiceProvider;

    private ISymbolChooser symbolChooser;
    private MilStdUnit selectedUnit;
    private Affiliation lastAffiliation = Affiliation.Unknown;

    /**
     * New selection panel.
     */
    AMil2525IconSelectionPanel(@Nonnull MilStd2525Variant milStd2525Variant, @Nonnull Consumer<PointDropperSelection> selectionConsumer)
    {
        super(selectionConsumer);

        this.milStd2525Variant = milStd2525Variant;

        initComponents();
    }

    @PostConstruct
    private void postConstruct()
    {
        // This has to happen in an @PC instead of in the constructor to avoid a race condition where the EDT
        // picks up the event *before* the injection processing happens at the end of the constructor.
        SwingUtilities.invokeLater(this::lateInit);
    }

    @SuppressWarnings("DataFlowIssue")
    private void lateInit()
    {
        // Create the default MIL-STD-2525 buttons and add them to the UI.
        symbolButtons[0] = newSymbolButton(convertToTarget2525(mil2525CFromCotType(defaultCotFor(Affiliation.Unknown))));
        symbolButtons[1] = newSymbolButton(convertToTarget2525(mil2525CFromCotType(defaultCotFor(Affiliation.Friend))));
        symbolButtons[2] = newSymbolButton(convertToTarget2525(mil2525CFromCotType(defaultCotFor(Affiliation.Neutral))));
        symbolButtons[3] = newSymbolButton(convertToTarget2525(mil2525CFromCotType(defaultCotFor(Affiliation.Hostile))));
        cotButtonList.setLayout(new WrapLayout(DEFAULT_WIDTH));
        for (SymbolToggleButton button : symbolButtons) addButton(button);

        // Create the MIL-STD-2525 picker button and add it to the UI.
        ImageIcon pickerIcon = UIResources.getSizedIcon(PLATFORM_ICONS, "search.png", new Dimension(ICON_SIZE, ICON_SIZE), false);
        JToggleButton pickerButton = new JToggleButton(pickerIcon);
        pickerButton.addActionListener(e -> {
            // Open the symbol chooser, user will click on their desired symbol.
            // We'll cache the existing chooser so that repeated use of the search button
            // will "remember" where we previously left off.
            if (symbolChooser == null)
            {
                IMilStd2525Service service = get2525Service();
                symbolChooser = service.newSymbolChooser();
                symbolChooser.setAffiliation(lastAffiliation);
                symbolChooser.setRequireCotCompatibility(true);
                symbolChooser.setShapeType(ShapeType.Point);
                symbolChooser.setSymbolConsumer(selection -> {
                    String sidc = selection.getSymbolCode();
                    lastAffiliation = service.getAffiliation(sidc);

                    final var unit = unitOf(selection);
                    updateSelection(unit);
                    for (SymbolToggleButton button : symbolButtons) adaptSymbolCode(unit, button);

                    // Following the click, the point dropper is activated with the chosen symbol.
                    String cotType = unit.cotType(lastAffiliation);
                    applySelection(new PointDropperSelection(definitionOf(cotType), cotType, sidc));
                });
            } else
            {
                // Update the affiliation since that can change since initially creating the symbol chooser.
                symbolChooser.setAffiliation(lastAffiliation);
            }
            symbolChooser.show(false);
        });
        addButton(pickerButton);

        // Setup unit combo-box.
        typeComboBoxModel.addAll(units);
        typeComboBoxModel.setSelectedItem(defaultUnit);
        typeCombo.addItemListener(e -> {
            // Update selected unit field so that we can populate the point prefix with the unit name.
            MilStdUnit unit = (MilStdUnit) typeComboBoxModel.getSelectedItem();
            updateSelection(unit);

            // Update all buttons to render their icons with the newly selected unit.
            for (SymbolToggleButton button : symbolButtons) adaptSymbolCode(unit, button);
            revalidate();
            repaint();
        });
        typeCombo.setModel(typeComboBoxModel);
        typeCombo.setRenderer(new UnitCellRenderer());
        selectedUnit = defaultUnit;
    }

    private void addButton(@Nonnull JToggleButton button)
    {
        addButtonToGroup(button);
        cotButtonList.add(button);
    }

    @Nonnull
    protected IMilStd2525Service get2525Service()
    {
        return milStd2525ServiceProvider.get2525Service(milStd2525Variant);
    }

    @Nonnull
    protected abstract MilStdUnit provideDefaultUnit();

    @Nonnull
    protected abstract List<MilStdUnit> provideUnits();

    @Nonnull
    protected String defaultCotFor(@Nonnull Affiliation affiliation)
    {
        return switch (affiliation)
        {
            case Pending, SimulatedPending -> DEFAULT_COT_PENDING;
            case Unknown, SimulatedUnknown -> DEFAULT_COT_UNKNOWN;
            case Friend, SimulatedFriend -> DEFAULT_COT_FRIEND;
            case Neutral, SimulatedNeutral -> DEFAULT_COT_NEUTRAL;
            case Hostile, SimulatedHostile -> DEFAULT_COT_HOSTILE;
            case AssumedFriend, SimulatedAssumedFriend -> DEFAULT_COT_ASSUMED_FRIEND;
            case Suspect, SimulatedSuspect -> DEFAULT_COT_SUSPECT;
            case ExercisePending -> DEFAULT_COT_EXERCISE_PENDING;
            case ExerciseUnknown -> DEFAULT_COT_EXERCISE_UNKNOWN;
            case ExerciseAssumedFriend -> DEFAULT_COT_EXERCISE_ASSUMED_FRIEND;
            case ExerciseFriend -> DEFAULT_COT_EXERCISE_FRIEND;
            case ExerciseNeutral -> DEFAULT_COT_EXERCISE_NEUTRAL;
            case Joker -> DEFAULT_COT_JOKER;
            case Faker -> DEFAULT_COT_FAKER;
        };
    }

    /**
     * Updates what values the user last interacted with.
     *
     * @param unit Value to assign to {@link #selectedUnit}.
     * @since 5.1
     */
    private void updateSelection(@Nullable MilStdUnit unit)
    {
        selectedUnit = unit;
        if (unit != null)
        {
            updateTypeComboDisplay(unit);
        } else
        {
            // When the unit is null, we still want to refresh the UI
            // so that things like the affiliation can be visually updated.
            invalidate();
            repaint();
        }
    }

    /**
     * Set the unit display in the combo box if it is not already shown.
     *
     * @param unit Unit to set as the combo box's current selection.
     */
    @InvokeOnEDT
    private void updateTypeComboDisplay(@Nonnull MilStdUnit unit)
    {
        typeComboBoxModel.setSelectedItem(unit);

        Arrays.stream(symbolButtons).filter(AbstractButton::isSelected).findFirst().ifPresent(
                button -> {
                    ISyntheticPluginDefinition pluginDefinition = definitionOf(button);
                    applySelection(new PointDropperSelection(pluginDefinition, pluginDefinition.getGroup(), button.getSymbolCode()));
                });

        invalidate();
        repaint();
    }

    /**
     * @param symbolCode MIL-STD-2525 symbol to create a button for.
     *
     * @return Toggle button to represent the given symbol.
     *
     * @since 5.1
     */
    @Nonnull
    private SymbolToggleButton newSymbolButton(@Nonnull String symbolCode)
    {
        IMilStd2525Service service = milStd2525ServiceProvider.get2525ServiceForSymbol(symbolCode);
        SymbolToggleButton button = new SymbolToggleButton(service, symbolCode);
        button.setPreferredSize(BUTTON_SIZE);
        button.setFocusable(false);
        button.addActionListener(e -> {
            final ISyntheticPluginDefinition definition = definitionOf(button);

            // Refresh the unit type display to match this button's affiliation.
            lastAffiliation = service.getAffiliation(symbolCode);
            updateSelection(selectedUnit);

            // If for some reason the user has the symbol chooser open, update the displayed affiliation as well.
            if (symbolChooser != null) symbolChooser.setAffiliation(lastAffiliation);

            toggleButtonActionPerformed(definition, (SymbolToggleButton) e.getSource());
        });
        return button;
    }

    private void toggleButtonActionPerformed(@Nonnull ISyntheticPluginDefinition iconDefinition, @Nonnull SymbolToggleButton button)
    {
        var selectedIconDefinition = button.isSelected() ? iconDefinition : null;
        String cotType = iconDefinition.getGroup();
        String sidc = button.getSymbolCode();
        applySelection(new PointDropperSelection(selectedIconDefinition, cotType, sidc));
    }

    /**
     * @param button Button representing a MIL-STD-2525 symbol.
     *
     * @return Generated synthetic plugin definition.
     * @since 5.1
     */
    @Nonnull
    private ISyntheticPluginDefinition definitionOf(@Nonnull SymbolToggleButton button)
    {
        // Adapt the button's stored SIDC to a CoT type and get its definition.
        String symbolCode = button.getSymbolCode();
        String cotType = Objects.requireNonNullElse(cotTypeFromMil2525C(symbolCode), defaultCotFor(Affiliation.Unknown));
        return definitionOf(cotType);
    }

    /**
     * @param cotType CoT type to look up the synthetic plugin definition of.
     *
     * @return Generated synthetic plugin definition.
     * @since 5.1
     */
    @Nonnull
    private ISyntheticPluginDefinition definitionOf(@Nonnull String cotType)
    {
        // Cache the definitions for each cot type. We want to use the same instances since the definitions
        // do not have object equality defined.
        return cotTypeToDefinitionMap.computeIfAbsent(cotType,
                type -> new SyntheticPluginDefinition(KeyUtils.TOOL_FAMILY, CotIcon.TYPE, type, null, null, STANDARD_RESOURCE_OPTIONS));
    }

    /**
     * @param symbolCode Symbol code of any 2525 specification.
     * @return Equivalent 2525C symbol code.
     * @since 5.5
     */
    @Nonnull
    private String convertTo2525c(@Nonnull String symbolCode)
    {
        // The symbol chooser will be configured to only display items that can be converted to C so this will never fail.
        String convertedSymbolCode = milStd2525ServiceProvider.get2525Service(CHARLIE).convertFromOther2525Variant(symbolCode);
        return Objects.requireNonNull(convertedSymbolCode, "Failed to convert '" + symbolCode + "' to " + MilStd2525Variant.CHARLIE.name());
    }

    /**
     * @param symbolCode Symbol code of any 2525 specification.
     * @return Equivalent 2525 symbol code in this panel's target variant.
     * @since 5.5
     */
    @Nonnull
    private String convertToTarget2525(@Nonnull String symbolCode)
    {
        IMilStd2525Service service = get2525Service();
        String convertedSymbolCode = service.convertFromOther2525Variant(symbolCode);
        return Objects.requireNonNull(convertedSymbolCode, "Failed to convert '" + symbolCode + "' to " + service.getVariant().name());
    }

    /**
     * @param selection Symbol selection to extract unit information from.
     * @return New unit from the given selection.
     * @since 5.5
     */
    private MilStdUnit unitOf(@Nonnull ISymbolSelection selection)
    {
        String symbolCode = selection.getSymbolCode();
        String symbolCodeC = convertTo2525c(symbolCode);
        String cotSuffix = cotTypeFromMil2525C(symbolCodeC).substring(4);
        String name = selection.getSymbolEntry().getName();
        return new MilStdUnit(cotSuffix, name);
    }

    /**
     * @param unit Unit to pull details from.
     * @param button Button to update with unit details.
     */
    private void adaptSymbolCode(@Nonnull MilStdUnit unit, @Nonnull SymbolToggleButton button)
    {
        String symbolCode = button.getSymbolCode();
        String symbolCodeC = convertTo2525c(symbolCode);
        String cotType = Objects.requireNonNullElse(cotTypeFromMil2525C(symbolCodeC), defaultCotFor(Affiliation.Unknown));
        String cotPrefix = cotType.substring(0, 4); // The 'a-<affiliation-char>-' portion of the type.
        String newCotType = cotPrefix + unit.cotSuffix(); // Combine with the unit type suffix so that we get the affiliated symbol.
        String newSymbolCodeC = mil2525CFromCotType(newCotType);
        if (newSymbolCodeC == null) return;

        button.setSymbolCode(convertToTarget2525(newSymbolCodeC));
    }

    /**
     * Wrapper to hold a CoT type suffix + display name pair for a unit type.
     * @param cotSuffix CoT type suffix.
     * @param displayName Display name for CoT type.
     * @since 5.5 (renamed for clarity, and transformed into a record)
     */
    protected record MilStdUnit(@Nonnull String cotSuffix, @Nonnull String displayName)
    {
        /**
         * @param affiliation Affiliation of the type.
         * @return Affiliated CoT type of this unit.
         * @since 5.5
         */
        @Nonnull
        public String cotType(@Nonnull Affiliation affiliation)
        {
            return CotSidcAdapter.getCotAtomPrefix(affiliation) + cotSuffix;
        }

        @Override
        public String toString()
        {
            return displayName + " " + cotSuffix;
        }
    }

    /**
     * A toggle button extension that holds a MIL-STD-2525 symbol code.
     *
     * @since 5.1
     */
    private static class SymbolToggleButton extends JToggleButton
    {
        private final IMilStd2525Service milStd2525Service;
        private String symbolCode;

        /**
         * @param milStd2525Service MIL-STD-2525 service for for symbol rendering.
         * @param symbolCode Initial MIL-STD-2525 symbol assigned to this button.
         */
        public SymbolToggleButton(@Nonnull IMilStd2525Service milStd2525Service, @Nonnull String symbolCode)
        {
            this.milStd2525Service = milStd2525Service;

            // Initialize held symbol + create the icon for it.
            setSymbolCode(symbolCode);
        }

        /**
         * @return MIL-STD-2525 symbol assigned to this button.
         */
        @Nonnull
        public String getSymbolCode()
        {
            return symbolCode;
        }

        /**
         * @param symbolCode MIL-STD-2525 symbol to assign to this button.
         */
        public void setSymbolCode(@Nonnull String symbolCode)
        {
            if (!Objects.equals(this.symbolCode, symbolCode))
            {
                this.symbolCode = symbolCode;

                // Update icon to match new symbol.
                BufferedImage adaptedImage = milStd2525Service.renderSinglePointIcon(symbolCode, ICON_SIZE);
                setIcon(new ImageIcon(adaptedImage));
            }
        }
    }

    /**
     * Renderer for {@link MilStdUnit}.
     *
     * @since 5.5 (renamed for clarity)
     */
    private class UnitCellRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean selected, boolean focused)
        {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
            if (value instanceof MilStdUnit unit)
            {
                String symbolCodeC = mil2525CFromCotType(unit.cotType(lastAffiliation));
                if (symbolCodeC != null)
                {
                    label.setIcon(new ImageIcon(milStd2525ServiceProvider.get2525Service(CHARLIE).renderSinglePointIcon(symbolCodeC, ICON_SIZE)));
                }
                label.setText(unit.displayName());
            }
            return label;
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        cotButtonList = new JPanel();
        var propertiesPanel = new JPanel();
        var typeLabel = new JLabel();
        typeCombo = new JComboBox<>();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "top:default:grow, bottom:default"));

        //======== cotButtonList ========
        {
            cotButtonList.setName("cotButtonList");
            cotButtonList.setLayout(new HorizontalLayout());
        }
        add(cotButtonList, cc.xy(1, 1));

        //======== propertiesPanel ========
        {
            propertiesPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            propertiesPanel.setName("propertiesPanel");
            propertiesPanel.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "default"));

            //---- typeLabel ----
            typeLabel.setText("Unit type");
            typeLabel.setName("typeLabel");
            propertiesPanel.add(typeLabel, cc.xy(1, 1));

            //---- typeCombo ----
            typeCombo.setName("typeCombo");
            propertiesPanel.add(typeCombo, cc.xy(3, 1));
        }
        add(propertiesPanel, cc.xy(1, 2));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel cotButtonList;
    private JComboBox<MilStdUnit> typeCombo;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
