package gov.takx.core.view.gui.maptools.tacticalgraphics;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.symbology.Modifier;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.platform.symbology.milstd2525.MilSymStandardAttributes;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.milsym.IMilStd2525Service;
import gov.takx.api.milsym.IMilStd2525ServiceProvider;
import gov.takx.api.milsym.ISymbolChooser;
import gov.takx.api.milsym.SymbolWrapper;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.view.gui.CoreComponentType;
import gov.takx.core.view.gui.UISpaceManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Displays the modifiers associated with a symbol code for viewing and editing.
 *
 * @see ISymbologyProvider#getModifiers(String)
 * @since 5.5 (Renamed from ShapeTacticalGraphicsEditorPanel)
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class TacticalGraphicsEditorPanel extends JPanel implements IResourceCleaner, PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final RowSpec DEFAULT_ROW_SPEC = RowSpec.decode("default");

    private final CellConstraints cellConstraints = new CellConstraints();
    private final PropertyChangeListener propertyChangeListener;
    private final Supplier<Boolean> ignoreEventSupplier;
    private final boolean directActionMode;
    private final List<AModifierPanel<?>> panels = new ArrayList<>();
    private IMapObject mapObject;
    private String symbolCode;
    private boolean commitUpdating;
    private ISymbolChooser symbolChooser;

    @Inject
    private UISpaceManager uiSpaceManager;
    @Inject
    private IMilStd2525ServiceProvider milStd2525ServiceProvider;
    private IMilStd2525Service milStd2525Service;

    /**
     * @param propertyChangeListener {@link gov.takx.core.view.gui.maptools.ShapePropertiesEditorPanel} listener that is
     *                               notified when modifier values change
     * @param ignoreEventSupplier Supplies the boolean that indicates if new events should be ignored while updates are
     *                            being processed
     * @param directActionMode {@code true} to immediately apply changes to the map object.
     *                         {@code false} to wait until the parent editor panel calls {@link #commit()}.
     */
    public TacticalGraphicsEditorPanel(@Nonnull PropertyChangeListener propertyChangeListener,
                                       @Nonnull Supplier<Boolean> ignoreEventSupplier,
                                       boolean directActionMode)
    {
        this.propertyChangeListener = propertyChangeListener;
        this.ignoreEventSupplier = ignoreEventSupplier;
        this.directActionMode = directActionMode;

        initComponents();
    }

    /**
     * @param mapObject The map object with modifiers to display
     */
    public void setMapObject(@Nonnull IMapObject mapObject)
    {
        this.mapObject = mapObject;
        this.mapObject.getAttributeSet().addPropertyChangeListener(this);

        milStd2525Service = milStd2525ServiceProvider.get2525ServiceForMapObject(mapObject);
        final String existingSymbolCode = milStd2525Service.getSymbolCode(mapObject);

        setSymbolCode(existingSymbolCode);
    }

    /**
     * Clears this panel's contents and re-initializes it with the new symbol code.
     *
     * @param symbolCode The new symbol code, or {@code null} when removing a symbol code.
     */
    public void updateSymbolCode(@Nullable String symbolCode)
    {
        if (mapObject == null) throw new IllegalStateException("A map object must be set before this panel can be used");

        if (Objects.equals(this.symbolCode, symbolCode)) return;

        clearUI();
        setSymbolCode(symbolCode);
        revalidate();
        repaint();
    }

    @Override
    public void releaseResources()
    {
        if (mapObject != null) mapObject.getAttributeSet().removePropertyChangeListener(this);

        if (symbolChooser != null) symbolChooser.hide();
    }

    /**
     * @param enabled true if this component and its children should be enabled, false otherwise
     * @since 5.2
     */
    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
        for (Component c : getComponents()) c.setEnabled(enabled);
    }

    /**
     * @param symbolCode The symbol code for the {@link gov.tak.api.symbology.ISymbolTable.Symbol} that should be
     *                   displayed in this panel. May be {@code null} if the map object does not yet have an associated symbol
     */
    private void setSymbolCode(@Nullable String symbolCode)
    {
        this.symbolCode = symbolCode;

        // This panel's button will activate the symbol chooser UI and update this the map object's symbol. This panel
        // will be notified of the change when it receives the MO's AttributeSet PCE
        final String symbolName = symbolCode != null
                ? milStd2525Service.getSymbol(symbolCode).getSymbol().getName()
                : mapObject != null ? mapObject.getType() : "<unknown type>";
        final SymbolPanel symbolPanel = new SymbolPanel(symbolName, getSymbolClearActionListener(), getSymbolSelectionActionListener());

        // Add the symbol selection button panel
        symbolPanel.setEnabled(isEnabled());
        addComponent(symbolPanel);

        if (symbolCode != null)
        {
            // Add the modifiers
            milStd2525Service.getModifiers(symbolCode).stream()
                    .sorted(Comparator.comparing(Modifier::getName))
                    .forEach(this::addModifier);
        }
    }

    /**
     * @return An ActionListener that will respond when the user clicks the symbol button to remove the map object's symbol.
     * @since 5.5
     */
    @Nonnull
    private ActionListener getSymbolClearActionListener()
    {
        return e -> {
            milStd2525Service.clearSymbolCode(mapObject);

            // When we assign a 2525 symbol code to a single-point map object we update the CoT type.
            // Thus, when we remove it we will strip it back down to the basic type of its respective atom group.
            //
            // This *CURRENTLY* does not apply to shape map objects, as there CURRENTLY no multi-point graphics in the 2525 trees we support that map to CoT types.
            // If this ever changes, we will want to possibly update this.
            if (!(mapObject instanceof IShapeMapObject<?>)) mapObject.setCotType(mapObject.getCotType().substring(0, 5));

            // Update the UI state to reflect the removal of the symbol code.
            updateSymbolCode(null);
        };
    }

    /**
     * @return An ActionListener that will respond when the user clicks the symbol button to change the map object's symbol.
     */
    @Nonnull
    private ActionListener getSymbolSelectionActionListener()
    {
        return e -> {
            // Panel that displays an instance of the mil sym selection panel
            if (symbolChooser == null)
            {
                symbolChooser = milStd2525Service.newSymbolChooser()
                        .initFromMapObject(mapObject)
                        .setRequireCotCompatibility(!(mapObject instanceof IShapeMapObject<?>))
                        .setSymbolConsumer(selection -> {
                            // Apply changes immediately
                            String selectedSymbolCode = selection.getSymbolCode();
                            if (directActionMode)
                            {
                                clearMapObjectModifiers();

                                // This will trigger a PCE that will trigger a call to updateSymbolCode()
                                milStd2525Service.setSymbolCode(mapObject, selectedSymbolCode);

                                // Change the name according to the new symbol
                                final SymbolWrapper symbol = milStd2525Service.getSymbol(symbolCode);
                                mapObject.setName(symbol.getName());

                                // If the symbol chooser was opened via the Properties panel (as indicated by the
                                // directActionMode value), the panel may have closed if it wasn't pinned. Reopen it.
                                uiSpaceManager.showComponent(CoreComponentType.PROPERTIES_PANEL);
                            }
                            // Just update the UI
                            else
                            {
                                updateSymbolCode(selectedSymbolCode);
                                propertyChangeListener.propertyChange(
                                        new PropertyChangeEvent(this,
                                                StandardAttributeNames.MIL_SYM, symbolCode, selectedSymbolCode));
                            }
                        })
                        .setCancelAction(() -> {
                            // If the symbol chooser was opened via the Properties panel (as indicated by the
                            // directActionMode value), the panel may have closed if it wasn't pinned. Reopen it.
                            if (directActionMode) uiSpaceManager.showComponent(CoreComponentType.PROPERTIES_PANEL);
                        });
            }
            symbolChooser.show(false);
        };
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (ignoreEventSupplier.get() || commitUpdating) return;

        final String propertyName = evt.getPropertyName();

        // Symbol change
        if (propertyName.equals(StandardAttributeNames.MIL_SYM))
        {
            updateSymbolCode((String) evt.getNewValue());
        }
        // Modifier change
        else if (propertyName.startsWith(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX))
        {
            // The modifier id will appear after the last "."
            final String modifierId = milStd2525Service.getModifierIdFromPropertyName(propertyName);

            // There may be more than one panel that matches if the modifier has multiple fields
            panels.stream().filter(panel -> panel.getModifierId().equals(modifierId)).forEach(AModifierPanel::refresh);
        }
    }

    /**
     * Persists each modifier's editor value to the map object's attributes.
     */
    public void commit()
    {
        // Panel is not active
        if (symbolCode == null) return;

        commitUpdating = true;

        if (!symbolCode.equals(milStd2525Service.getSymbolCode(mapObject)))
        {
            clearMapObjectModifiers();

            // Set the MO's symbol and name according the new symbol
            milStd2525Service.setSymbolCode(mapObject, symbolCode);
            final SymbolWrapper symbol = milStd2525Service.getSymbol(symbolCode);
            mapObject.setName(symbol.getName());
        }

        // Update the modifiers
        panels.forEach(AModifierPanel::commit);

        commitUpdating = false;
    }

    /**
     * @return Current selected mil-sym SIDC.
     * @since 5.5
     */
    @Nullable
    public String getSymbolCode()
    {
        return symbolCode;
    }

    /**
     * @return Attribute set containing current editor input values.
     * @since 5.5
     */
    @Nonnull
    public AttributeSet getCurrentAttributeValues()
    {
        AttributeSet set = new AttributeSet();
        for (AModifierPanel<?> panel : panels)
        {
            String modifierKey = MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + panel.getModifierId();
            String currentValue = panel.getCurrentValue();
            if (!StringUtils.isEmpty(currentValue))
            {
                set.setAttribute(modifierKey, currentValue);
            }
        }
        return set;
    }

    /**
     * Clears the values for all modifiers associated with the map object's symbol code.
     */
    private void clearMapObjectModifiers()
    {
        final String symbolCode = milStd2525Service.getSymbolCode(mapObject);

        if (symbolCode == null) return; // may be null when initializing the UI

        // Clear the old values
        milStd2525Service.getModifiers(symbolCode).forEach(modifier -> milStd2525Service.removeModifier(mapObject, modifier.getId()));
    }

    /**
     * Clears this panel by removing all components and layout rows. This should be done before setting a new symbol.
     */
    private void clearUI()
    {
        removeAll();

        panels.clear();

        final FormLayout formLayout = (FormLayout) getLayout();
        int rowCount;
        while ((rowCount = formLayout.getRowCount()) > 0) formLayout.removeRow(rowCount);
    }

    /**
     * Adds a modifier to the panel. If the modifier has multiple fields, a new panel will be added for each field.
     *
     * @param modifier The modifier to add
     */
    private void addModifier(@Nonnull Modifier modifier)
    {
        final int numFields = modifier.getNumFields();

        for (int fieldIndex = 0; fieldIndex < numFields; fieldIndex++)
        {
            final AModifierPanel<?> modifierPanel = createModifierPanel(modifier, fieldIndex);

            if (modifierPanel != null)
            {
                modifierPanel.setEnabled(isEnabled());
                addComponent(modifierPanel);
                panels.add(modifierPanel);
            }
        }
    }

    /**
     * Creates a new panel for a modifier based on its class type.
     *
     * @param modifier The modifier
     * @param fieldIndex The modifier field index
     * @return a new modifier panel
     */
    @Nullable
    private AModifierPanel<?> createModifierPanel(@Nonnull Modifier modifier, int fieldIndex)
    {
        final String modifierId = modifier.getId();
        final Class<?> type = modifier.getParsedValueType();

        // null indicates text; ATAK allows users to enter text for number modifiers
        if (type == null)
        {
            return new TextModifierPanel(modifier.getId(),
                    // Specifies the modifier name, taking multiple fields into account
                    modifier.getName(fieldIndex),
                    // Passes a boolean indicating if the modifier has multiple fields
                    modifier.getNumFields() > 1,
                    // Takes an updated value and rebuilds the modifier's full value, taking multiple fields into account
                    value -> buildModifierValue(modifier, fieldIndex, value),
                    // Retrieves the persisted modifier value, returning just the field value if there are multiple fields
                    () -> getModifierField(modifier, fieldIndex),
                    // Responds when the modifier value changes by firing a PCE that may perist the change or set the panel dirty
                    value -> fireModifierPropertyChangeEvent(modifierId, buildModifierValue(modifier, fieldIndex, value)),
                    // Responds to requests to persist a modifier value. The value should be the full modifier value
                    (k, v) -> milStd2525Service.setModifier(mapObject, k, v));
        }

        if (type == Number.class)
        {
            return new NumberModifierPanel(modifier.getId(),
                    // Specifies the modifier name, taking multiple fields into account
                    modifier.getName(fieldIndex),
                    // Takes an updated value and rebuilds the modifier's full value, taking multiple fields into account
                    value -> buildModifierValue(modifier, fieldIndex, value),
                    // Retrieves the persisted modifier value, returning just the field value if there are multiple fields
                    () -> getModifierField(modifier, fieldIndex),
                    // Responds when the modifier value changes by firing a PCE that may perist the change or set the panel dirty
                    value -> fireModifierPropertyChangeEvent(modifierId, buildModifierValue(modifier, fieldIndex, value)),
                    // Responds to requests to persist a modifier value. The value should be the full modifier value
                    (k, v) -> milStd2525Service.setModifier(mapObject, k, v));
        }

        if (type == Date.class)
        {
            return new DateModifierPanel(modifier.getId(), modifier.getName(),
                    () -> getModifierField(modifier, fieldIndex),
                    value -> fireModifierPropertyChangeEvent(modifierId, value),
                    (k, v) -> milStd2525Service.setModifier(mapObject, k, v));
        }

        if (type == Boolean.class)
        {
            return new BooleanModifierPanel(modifier.getId(), modifier.getName(),
                    () -> getModifierField(modifier, fieldIndex),
                    value -> fireModifierPropertyChangeEvent(modifierId, buildModifierValue(modifier, fieldIndex, value)),
                    (k, v) -> milStd2525Service.setModifier(mapObject, k, v));
        }

        logger.debug("Unknown modifier type: {}", type);

        return null;
    }

    /**
     * @param modifier The modifier to inspect
     * @param fieldIndex The field to retrieve
     * @return The field value
     */
    private String getModifierField(@Nonnull Modifier modifier, int fieldIndex)
    {
        // Get the unmodified root symbol codes for comparison to get rid of fields that don't affect the resulting symbol type
        final String rootSymbolCode = milStd2525Service.getBaseSymbolCode(symbolCode);

        // If this is a plain map object being converted to one with a symbol, the current symbol code will be null
        final String mapObjectSymbolCode = milStd2525Service.getSymbolCode(mapObject);
        final String mapObjectRootSymbolCode =
                mapObjectSymbolCode != null ? milStd2525Service.getBaseSymbolCode(mapObjectSymbolCode) : null;

        // If the config panel is in the middle of changing symbols and the change hasn't been applied yet, don't pull
        // the value from the MO
        final boolean changingSymbols = !Objects.equals(rootSymbolCode, mapObjectRootSymbolCode);

        final String fullValue = changingSymbols ? null : milStd2525Service.getModifier(mapObject, modifier.getId());
        final int fieldCount = modifier.getNumFields();

        // Unset or single field: return the entire modifier value
        if (fieldCount == 1 || Strings.isEmpty(fullValue)) return fullValue;

        // Retrieve the field value. -1 will include empty Strings
        final String[] parts = fullValue.split(",", -1);
        return parts[fieldIndex];
    }

    /**
     * Builds or rebuilds a modifier value when a field value changes.
     *
     * @param modifier The modifier
     * @param fieldIndex The index of the field that changed
     * @param value The field value
     * @return The full modifier value
     */
    private String buildModifierValue(@Nonnull Modifier modifier, int fieldIndex, @Nullable String value)
    {
        final int fieldCount = modifier.getNumFields();

        // If there's only one field, the value is the full modifier value
        if (fieldCount == 1) return value;

        // If the value is null or empty then keep it as-is
        if (StringUtils.isEmpty(value)) return value;

        // Get the current full modifier value
        final String currentValue = milStd2525Service.getModifier(mapObject, modifier.getId());

        // Split full value into its field parts, or create a new array if the modifier hasn't been set yet
        final String[] parts = currentValue != null && !currentValue.isEmpty()
                ? currentValue.split(",", -1) // -1 limit will include empty Strings
                : new String[fieldCount];

        // Add empty Strings to a new array
        if (StringUtils.isEmpty(currentValue)) Arrays.fill(parts, modifier.getParsedValueType() == Number.class ? "0" : "");

        // Set the field value
        parts[fieldIndex] = value;

        // Rebuild the full modifier value
        return String.join(",", parts);
    }

    /**
     * @param modifierId the modifier id
     * @param newValue   the new modifier value; if the modifier has multiple fields, this should be the full value
     */
    private void fireModifierPropertyChangeEvent(@Nonnull String modifierId, @Nullable String newValue)
    {
        propertyChangeListener.propertyChange(
                new PropertyChangeEvent(this, modifierId, milStd2525Service.getModifier(mapObject, modifierId), newValue));
    }

    /**
     * @param component The component to add to this panel's FormLayout
     */
    private void addComponent(@Nonnull Component component)
    {
        final FormLayout formLayout = (FormLayout) getLayout();
        formLayout.appendRow(DEFAULT_ROW_SPEC);
        add(component, cellConstraints.xy(1, formLayout.getRowCount()));
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off

        //======== this ========
        setBorder(Borders.DLU2_BORDER);
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                ""));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
