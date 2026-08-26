package gov.takx.core.view.gui.explorer.rules;

import com.google.common.collect.Streams;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.explorer.AExplorableRulePanel;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.synthetic.ISyntheticPluginDefinition;
import gov.takx.api.registry.ISyntheticPluginRegistry;
import gov.takx.core.explorer.rules.MapObjectRule;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * @since 2.0.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MapObjectRulePanel extends AExplorableRulePanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String ALL = "All"; // Combo box entry for ALL (wildcard) selection

    // Function to get a stream of type strings from synthetic plugin definitions with a given family
    private static final BiFunction<Stream<ISyntheticPluginDefinition>, String, Stream<String>> TYPES_FOR_SYNTHETIC =
            (definitions, family) -> definitions.filter(definition -> definition.getFamily().equals(family))
                    .map(ISyntheticPluginDefinition::getType);

    // Function to get a stream of type strings from map entities with a given family
    private static final BiFunction<Stream<IMapEntity>, String, Stream<String>> TYPES_FOR_MAP_ENTITY =
            (mapEntities, family) -> mapEntities.filter(mapEntity -> mapEntity.getFamily().equals(family))
                    .map(IMapEntity::getType);

    /**
     * The rule being edited
     */
    private final MapObjectRule rule;

    @Inject
    private IMapObjectPersist mapObjectPersist;

    @Inject
    private ISyntheticPluginRegistry syntheticPluginRegistry;

    /**
     * Map entities for all map objects
     */
    private Collection<IMapEntity> mapEntities;

    /**
     * Plugin definitions for all non-device Map Entities
     */
    private Collection<ISyntheticPluginDefinition> syntheticDefinitions;

    /**
     * Create the panel with the given rule.
     *
     * @param rule Rule to be edited
     */
    public MapObjectRulePanel(MapObjectRule rule)
    {
        this.rule = rule;
        initComponents();

        // Add default "All" items
        familyComboBox.addItem(ALL);
        typeComboBox.addItem(ALL);
        unitIdComboBox.addItem(ALL);
    }

    @PostConstruct
    private void postConstruct()
    {
        // Get all device/non-device map entities
        mapEntities = mapObjectPersist.findAllMapEntities();
        syntheticDefinitions = syntheticPluginRegistry.getPluginDefinitions();

        populateFamilies();

        final List<String> matchParts = rule.getMatchParts();

        if (logger.isDebugEnabled()) logger.debug("match parts: {}", matchParts);

        // Each selection will cause the subordinate combo box to populate automatically
        setSelectedItem(familyComboBox, matchParts.get(0));
        setSelectedItem(typeComboBox, matchParts.get(1));
        setSelectedItem(unitIdComboBox, matchParts.get(2));
    }

    private void setSelectedItem(JComboBox<String> comboBox, String value)
    {
        if (value != null) comboBox.setSelectedItem(value);
    }

    private void populateFamilies()
    {
        populateComboBox(familyComboBox,
                Streams.concat(
                        mapEntities.stream().map(IMapEntity::getFamily),
                        syntheticDefinitions.stream().map(IPluginDefinition::getFamily)));
    }

    private void populateTypes(int familyIndex)
    {
        final String family = familyComboBox.getItemAt(familyIndex);

        populateComboBox(typeComboBox,
                KeyUtils.isNonDevice(family)
                        ? TYPES_FOR_SYNTHETIC.apply(syntheticDefinitions.stream(), family)
                        : TYPES_FOR_MAP_ENTITY.apply(mapEntities.stream(), family));
    }

    private void populateUnitIds(int familyIndex, int typeIndex)
    {
        final String family = familyComboBox.getItemAt(familyIndex);
        final String type = typeComboBox.getItemAt(typeIndex);

        // Get all unit IDs with the given family/type
        populateComboBox(unitIdComboBox,
                mapObjectPersist.findMapObjectsByFamilyAndType(family, type).stream().map(IMapObject::getUnitID));
    }

    /**
     * Populate the entries in the given combo box from the stream of strings.  Add the "ALL" entry and then
     * all the strings (sorted and distinct).
     *
     * @param comboBox Combo box to process
     * @param strings  Stream of strings to install
     * @since 3.0
     */
    private void populateComboBox(JComboBox<String> comboBox, Stream<String> strings)
    {
        comboBox.removeAllItems();
        comboBox.addItem(ALL);

        strings.sorted().distinct().forEach(comboBox::addItem);
    }

    private void onFamilyChanged()
    {
        final int familyIndex = familyComboBox.getSelectedIndex();
        if (familyIndex < 0) return;

        final boolean notAll = familyIndex > 0; // Something other than "ALL"

        if (notAll) populateTypes(familyIndex);

        typeComboBox.setSelectedItem(ALL);
        typeComboBox.setEnabled(notAll);

        unitIdComboBox.setSelectedItem(ALL);
        unitIdComboBox.setEnabled(false);

        setDirty();
    }

    private void onTypeChanged()
    {
        final int familyIndex = familyComboBox.getSelectedIndex();
        final int typeIndex = typeComboBox.getSelectedIndex();

        if (familyIndex < 0 || typeIndex < 0) return;

        final boolean notAll = typeIndex > 0; // Something other than "ALL"

        if (notAll) populateUnitIds(familyIndex, typeIndex);

        unitIdComboBox.setSelectedItem(ALL);
        unitIdComboBox.setEnabled(notAll);

        setDirty();
    }

    private void onUnitIdChanged()
    {
        setDirty();
    }

    @Override
    public void applyAction()
    {
        rule.setMatchParts(getSelectionOrWildcard(familyComboBox),
                getSelectionOrWildcard(typeComboBox),
                getSelectionOrWildcard(unitIdComboBox));
    }

    /**
     * Get the current selection text from the given combo box. Return the WILDCARD string if there is no selection
     * or "ALL" is selected.
     *
     * @param comboBox Combo box to process
     * @return selection text or {@link MapObjectRule#WILDCARD}
     * @since 3.0
     */
    private String getSelectionOrWildcard(JComboBox<String> comboBox)
    {
        final String selectedItem = (String) comboBox.getSelectedItem();

        //noinspection StringEquality
        return selectedItem != null && selectedItem != ALL
                ? selectedItem
                : MapObjectRule.WILDCARD;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        JLabel label1 = new JLabel();
        familyComboBox = new JComboBox<>();
        JLabel label2 = new JLabel();
        typeComboBox = new JComboBox<>();
        JLabel label3 = new JLabel();
        unitIdComboBox = new JComboBox<>();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default:grow",
                "2*(default, $lgap), default"));

        //---- label1 ----
        label1.setText("Family");
        label1.setName("label1");
        add(label1, cc.xy(1, 1));

        //---- familyComboBox ----
        familyComboBox.setName("familyComboBox");
        familyComboBox.addActionListener(e -> onFamilyChanged());
        add(familyComboBox, cc.xy(3, 1));

        //---- label2 ----
        label2.setText("Type");
        label2.setName("label2");
        add(label2, cc.xy(1, 3));

        //---- typeComboBox ----
        typeComboBox.setEnabled(false);
        typeComboBox.setName("typeComboBox");
        typeComboBox.addActionListener(e -> onTypeChanged());
        add(typeComboBox, cc.xy(3, 3));

        //---- label3 ----
        label3.setText("Unit ID");
        label3.setName("label3");
        add(label3, cc.xy(1, 5));

        //---- unitIdComboBox ----
        unitIdComboBox.setEnabled(false);
        unitIdComboBox.setName("unitIdComboBox");
        unitIdComboBox.addActionListener(e -> onUnitIdChanged());
        add(unitIdComboBox, cc.xy(3, 5));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JComboBox<String> familyComboBox;
    private JComboBox<String> typeComboBox;
    private JComboBox<String> unitIdComboBox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
