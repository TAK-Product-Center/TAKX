package gov.takx.platform.io.panels;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.IStructure;
import gov.takx.api.plugin.io.AWizardPanel;
import gov.takx.api.query.IDeviceQueryService;
import gov.takx.api.query.IQueryConstraints;
import gov.takx.commons.swing.ErrorReportingSwingWorker;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.jdesktop.swingx.JXList;
import org.jdesktop.swingx.sort.DefaultSortController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.border.TitledBorder;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static gov.takx.api.messages.IRaptorDataStructure.METADATA_EXCLUDE_FROM_FILTERED_EXPORT_LIST;

/**
 * Wizard panel to choose a type of message to export. Some data
 * formats (for example, CSV) can only represent on data structure, so
 * it necessary for the user to choose what type of message they want
 * to export for a given map object.
 * <p/>
 * <p>The panel reads the map object from the wizard property map key
 * {@link CommonWizardSettings#SELECTED_MAP_OBJECTS}. If more than one
 * map object has been selected, only the first is used.
 *
 */
@PseudoBean
public class ChooseMessageTypePanel extends AWizardPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Identifier for this wizard panel.
     */
    public static final String IDENTIFIER = "platform.io.panels.ChooseMessageTypePanel";

    private static final String NO_ADDITIONAL_DETAILS = "- No additional details available -";
    private static final String SELECT_MESSAGE = "<--- Select message type to view additional details";

    @Inject
    private IMapObjectManager mapObjectManager;

    private DefaultListModel<StructWrapper> listModel;

    private boolean allZeroCounts = true;
    private final StructureFilter filter = new StructureFilter();
    private final Set<IMapObject> mapObjects;
    private CountWorker countWorker;

    /**
     * Constructor.
     *
     * @since 3.0 (delegate parameter removed)
     */
    public ChooseMessageTypePanel()
    {
        this(null);
    }

    /**
     * Constructor.
     *
     * @param mapObjects The selected map objects for which to display the message types
     * @since 3.0 (delegate parameter removed)
     */
    public ChooseMessageTypePanel(@Nullable Set<IMapObject> mapObjects)
    {
        initComponents();

        this.mapObjects = mapObjects;
    }

    @Override
    public String getIdentifier()
    {
        return IDENTIFIER;
    }

    private void validateContents()
    {
        wizard.getModel().setNextButtonEnabled(msgList.getSelectedIndex() != -1);
    }

    /**
     * Load the data structures and place them in the table.
     */
    private void loadData()
    {
        //noinspection unchecked
        Set<IMapObject> mapObjects = this.mapObjects == null
                ? (Set<IMapObject>) wizard.getModel().getProperty(CommonWizardSettings.SELECTED_MAP_OBJECTS)
                : this.mapObjects;

        countWorker = new CountWorker(mapObjects);
        countWorker.execute(); // Do this in the background
    }

    /**
     * Install the given structure wrappers into the UI controls.
     *
     * @param structWrappers Wrappers to add
     * @since 2.30
     */
    private void installStructures(Collection<StructWrapper> structWrappers)
    {
        listModel = new DefaultListModel<>();

        for (StructWrapper sw : structWrappers)
        {
            listModel.addElement(sw);

            if (sw.messages != 0) allZeroCounts = false;
        }

        msgList.setModel(listModel);
        msgList.setComparator(DefaultSortController.COMPARABLE_COMPARATOR); // Secret sauce to get sorting to work
        msgList.setAutoCreateRowSorter(true); // Let JXList make the sorter
        msgList.setSortOrder(SortOrder.ASCENDING);
        msgList.setRowFilter(filter);

        // If the comparator works correctly, the first in the list should have the most tracks in it.  Also, you
        // can't just check the model for elements available since some could be filtered out due to message type
        if (msgList.getElementCount() > 0)
        {
            msgList.setSelectedIndex(0);
        }

        presetShowAll();
    }

    /**
     * Scan the model and determine if the "Show All" checkbox should be preset to true or false.
     */
    private void presetShowAll()
    {
        boolean setShowAllOn = true;

        for (int i = 0; i < listModel.getSize(); i++)
        {
            if (isStructureExcluded(i))
            {
                // One or more structures has been marked to be filtered from the list by default,
                // so filter the list before it is displayed to the user.
                setShowAllOn = false;

                break;
            }
        }

        checkBoxShowAll.setSelected(setShowAllOn);
    }

    /**
     * Determine if a given structure in the model is tagged for exclusion.  Also want to exclude by default structures
     * with no messages.
     * <p>
     * In the case that all structures have a 0 count, don't filter entries that have 0 counts.
     *
     * @param row Model row to test
     * @return {@code true} if the structure at that row is tagged for exclusion
     */
    private boolean isStructureExcluded(int row)
    {
        final StructWrapper wrapper = listModel.get(row);
        final IStructure struct = wrapper.struct;

        final String filterOption = struct.getMetadata().getMetadataValue(METADATA_EXCLUDE_FROM_FILTERED_EXPORT_LIST);

        return (Boolean.parseBoolean(filterOption)) ||
                (wrapper.messages == 0 && !allZeroCounts);
    }

    @Override
    public void aboutToDisplayPanel()
    {
        validateContents();
    }

    @Override
    public void displayingPanel()
    {
        loadData();
    }

    @Override
    public void aboutToHidePanel()
    {
        StructWrapper sw = (StructWrapper) msgList.getSelectedValue();
        if (sw != null)
        {
            wizard.getModel().setProperty(CommonWizardSettings.SELECTED_MAP_OBJECT_FINAL, new HashSet<>(sw.mapObjects));
            wizard.getModel().setProperty(CommonWizardSettings.SELECTED_DATA_STRUCTURE, sw.struct);
        }
    }

    private void msgListSelectionChanged()
    {
        final StructWrapper sw = (StructWrapper) msgList.getSelectedValue();

        textPaneDescription.setText(sw != null
                ? sw.details + "\n\n" + sw.messages + " track points available for export"
                : SELECT_MESSAGE);

        validateContents();
    }

    private void checkBoxShowAllItemStateChanged()
    {
        msgList.setRowFilter(filter);  // Just re-install the filter to re-apply it
    }

    private void skipCountButtonActionPerformed()
    {
        countWorker.skipCounts(); // Skip remaining count operations
        countWorker = null;
    }

    /**
     * Simple filter to exclude structures from the list if they have the EXCLUDED metadata tag set "true" unless
     * the "Show All" checkbox is selected, in which case we show them all.
     */
    private class StructureFilter extends RowFilter<DefaultListModel, Integer>
    {
        @Override
        public boolean include(Entry<? extends DefaultListModel, ? extends Integer> entry)
        {
            return checkBoxShowAll.isSelected() || !isStructureExcluded(entry.getIdentifier());
        }
    }

    /**
     * Swing worker to perform the message counts and install the located structures.
     *
     * @since 2.30
     */
    private final class CountWorker extends ErrorReportingSwingWorker<Collection<StructWrapper>, Integer>
    {
        private final Map<IMapObject, Set<IStructure>> structuresPerMapObject = new HashMap<>();
        private final Map<IStructure, StructWrapper> structureWrappers = new HashMap<>();
        private int countsCompleted = 0;
        private volatile boolean skipCounts = false;

        public CountWorker(Set<IMapObject> mapObjects)
        {
            super("Failed to count messages", "Failed to count messages");

            int totalStructureCount = 0;

            for (IMapObject mapObject : mapObjects)
            {
                final Set<IStructure> structures = mapObject.getMapEntity().getDataStructures();
                structuresPerMapObject.put(mapObject, structures);
                totalStructureCount += structures.size();
            }

            countProgressBar.setMaximum(totalStructureCount);

            progressPanel.setVisible(true);
        }

        /**
         * Skip performing any further counts, and install 0 for any structures not yet counted.
         */
        void skipCounts()
        {
            skipCounts = true;
        }

        @Override
        protected void onSuccess(Collection<StructWrapper> result)
        {
            installStructures(result);
        }

        @Override
        protected void onFailure()
        {
            installStructures(Collections.emptyList());
        }

        @Override
        protected void cleanup()
        {
            progressPanel.setVisible(false);
        }

        /**
         * Perform the count operations and create all the StructureWrapper instances.
         *
         * @return Collection of structure wrappers
         */
        @Override
        protected Collection<StructWrapper> doInBackground()
        {
            structuresPerMapObject.forEach((mapObject, structures) -> {
                final var moc = mapObject.getController();
                structures.forEach(struct -> countForStructure(moc, struct));
            });

            return structureWrappers.values();
        }

        /**
         * Get the count of messages for a given MapObject and Structure.
         *
         * @param moc    MapObject controller on which to query
         * @param struct The structure to count
         */
        private void countForStructure(IMapObjectController moc, IStructure struct)
        {
            final IMapObject mapObject = moc.getMapObject();
            final long count;

            // If we've been cancelled, just set the count to 0
            if (skipCounts)
            {
                count = 0;
            } else
            {
                final IDeviceQueryService queryService = moc.getQueryService();
                final IQueryConstraints constraints = queryService.createEmptyConstraints();
                constraints.setStructuresFromMapObject(mapObject.getFamily(), mapObject.getType());
                constraints.addMessageStructureConstraint(struct.getMessageType());
                constraints.setMapObject(mapObject);

                count = queryService.count(constraints);
            }

            // If the structure wrapper already exists, then merge the MO and count, otherwise create a new wrapper
            final StructWrapper wrapper = structureWrappers.get(struct);

            if (wrapper != null)
            {
                wrapper.add(mapObject, count);
            } else
            {
                createStructureWrapper(struct, count, mapObject);
            }

            publish(++countsCompleted);
        }

        /**
         * Create a structure wrapper for the given structure and count.
         *
         * @param struct    Structure to wrap
         * @param count     Count of messages associated with this structure
         * @param mapObject Map Object using structure
         */
        private void createStructureWrapper(IStructure struct, long count, IMapObject mapObject)
        {
            // New structure to add.  Could be an upgraded version of the same message type, and if so,
            // we want to mark both of them so that when they are displayed in the UI that their version
            // number is also displayed.
            final StructWrapper wrapper = new StructWrapper(struct, count, mapObject);

            structureWrappers.forEach((structure, structWrapper) -> {
                if (struct.getMessageType().equals(structure.getMessageType()))
                {
                    structWrapper.setDuplicateName();
                    wrapper.setDuplicateName();
                }
            });

            structureWrappers.put(struct, wrapper);
        }

        @Override
        protected void process(List<Integer> chunks)
        {
            countProgressBar.setValue(chunks.get(chunks.size() - 1)); // The last value is all we care about
        }
    }

    /**
     * This is what we put in the list of structures.  This allows us to control the text shown and the sort order.
     */
    private static class StructWrapper implements Comparable<StructWrapper>
    {
        private final IStructure struct;

        // label and detail-panel text; will not be null.
        private String description;
        private String details;
        private long messages;
        private boolean duplicateName = false;
        private final List<IMapObject> mapObjects = new ArrayList<>();

        StructWrapper(IStructure s, long messages, IMapObject mapObject)
        {
            struct = s;
            this.messages = messages;
            mapObjects.add(mapObject);
            generateDescription();
        }

        @Override
        public String toString()
        {
            return description;
        }

        @Override
        public int compareTo(StructWrapper o)
        {
            if (messages == o.messages)
            {
                return struct.getMessageType().compareTo(o.struct.getMessageType());
            }

            return messages < o.messages ? 1 : -1;
        }

        /**
         * Add the given MO and count ito this wrapper.
         *
         * @param mapObject MO to add
         * @param count     Count to add
         * @since 2.30
         */
        void add(IMapObject mapObject, long count)
        {
            mapObjects.add(mapObject);
            messages += count;
            generateDescription();
        }

        void setDuplicateName()
        {
            duplicateName = true;
            generateDescription();
        }

        private void generateDescription()
        {
            final String structDescription = struct.getMetadata().getMetadataValue(IRaptorDataStructure.METADATA_STRUCTURE_DESCRIPTION);
            final String structDetails = struct.getMetadata().getMetadataValue(IRaptorDataStructure.METADATA_STRUCTURE_DETAILS);

            // many plugins appear to ignore the "details" field and use "description" for a verbose explanation.
            // this is not suitable for a short description field, so only use "description" if "details" is also set.
            // Otherwise the description will be used in the "details" view instead.
            // TODO: There HAS to be a better way of dealing with this.. much too hacky.
            if (structDetails != null && structDescription != null)
            {
                description = structDescription;
                details = structDetails;
            } else
            {
                description = struct.getMessageType();
                details = structDescription == null ? NO_ADDITIONAL_DETAILS : structDescription;
            }
            if (duplicateName)
            {
                description = description + " " + struct.getVersion();
            }

            final String quantifier = messages == 1 ? "track" : "tracks";
            if (messages > 0) description += " (" + messages + " " + quantifier + ")";
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var label1 = new JLabel();
        var panel2 = new JPanel();
        var scrollPane1 = new JScrollPane();
        msgList = new JXList();
        var panel3 = new JPanel();
        var scrollPane2 = new JScrollPane();
        textPaneDescription = new JTextPane();
        checkBoxShowAll = new JCheckBox();
        progressPanel = new JPanel();
        countProgressBar = new JProgressBar();
        var skipCountButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default, $lgap, fill:default:grow, $lgap, default, $lgap, fill:default"));

        //---- label1 ----
        label1.setText("Please select the message type you would like to export:");
        label1.setName("label1");
        add(label1, cc.xy(1, 1));

        //======== panel2 ========
        {
            panel2.setName("panel2");
            panel2.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "fill:default:grow, $lgap, default"));

            //======== scrollPane1 ========
            {
                scrollPane1.setName("scrollPane1");

                //---- msgList ----
                msgList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                msgList.setName("msgList");
                msgList.addListSelectionListener(e -> msgListSelectionChanged());
                scrollPane1.setViewportView(msgList);
            }
            panel2.add(scrollPane1, cc.xy(1, 1));

            //======== panel3 ========
            {
                panel3.setBorder(new TitledBorder("Description"));
                panel3.setName("panel3");
                panel3.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default:grow"));

                //======== scrollPane2 ========
                {
                    scrollPane2.setName("scrollPane2");

                    //---- textPaneDescription ----
                    textPaneDescription.setEditable(false);
                    textPaneDescription.setName("textPaneDescription");
                    scrollPane2.setViewportView(textPaneDescription);
                }
                panel3.add(scrollPane2, cc.xy(1, 1));
            }
            panel2.add(panel3, cc.xy(3, 1));
        }
        add(panel2, cc.xy(1, 3));

        //---- checkBoxShowAll ----
        checkBoxShowAll.setText("Show All Available Message Types");
        checkBoxShowAll.setName("checkBoxShowAll");
        checkBoxShowAll.addItemListener(e -> checkBoxShowAllItemStateChanged());
        add(checkBoxShowAll, cc.xy(1, 5));

        //======== progressPanel ========
        {
            progressPanel.setName("progressPanel");
            progressPanel.setLayout(new FormLayout(
                    "default:grow, $rgap, $button",
                    "fill:default"));

            //---- countProgressBar ----
            countProgressBar.setStringPainted(true);
            countProgressBar.setString("Counting messages per structure");
            countProgressBar.setName("countProgressBar");
            progressPanel.add(countProgressBar, cc.xy(1, 1));

            //---- skipCountButton ----
            skipCountButton.setText("Skip Counting");
            skipCountButton.setName("skipCountButton");
            skipCountButton.addActionListener(e -> skipCountButtonActionPerformed());
            progressPanel.add(skipCountButton, cc.xy(3, 1));
        }
        add(progressPanel, cc.xy(1, 7));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JXList msgList;
    private JTextPane textPaneDescription;
    private JCheckBox checkBoxShowAll;
    private JPanel progressPanel;
    private JProgressBar countProgressBar;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
