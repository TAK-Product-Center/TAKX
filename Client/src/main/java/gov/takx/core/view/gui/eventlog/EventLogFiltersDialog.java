package gov.takx.core.view.gui.eventlog;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.eventlog.EventLogType;
import gov.takx.commons.swing.CheckBoxCellRenderer;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Dialog to allow the user to select the set of filters to be applied to the event log panel.
 *
 */
public class EventLogFiltersDialog extends AStandardDialog
{
    private static final SortedMap<String, EventLogType> alphabeticalEventLogTypes = new TreeMap<>();
    private final DefaultListModel<EventLogCheckListItem> filterListModel = new DefaultListModel<>();
    private final EventLogPanel eventLogPanel;

    static
    {
        // Sort the event log filters alphabetically.
        for (EventLogType eventLogType : EventLogType.values())
        {
            alphabeticalEventLogTypes.put(eventLogType.toString(), eventLogType);
        }
    }

    /**
     * Construct the filter selection dialog on the given event log panel.
     *
     * @param eventLogPanel Parent panel
     */
    public EventLogFiltersDialog(EventLogPanel eventLogPanel)
    {
        this.eventLogPanel = eventLogPanel;

        initComponents();
        createFilterCheckBoxes();
    }

    private void createFilterCheckBoxes()
    {
        for (EventLogType eventType : alphabeticalEventLogTypes.values())
        {
            EventLogCheckListItem filter = new EventLogCheckListItem(eventType, eventLogPanel.isActiveEventFilter(eventType));
            filterListModel.addElement(filter);
        }

        // Use a CheckBoxCellRenderer to renderer list cells.
        filterList.setCellRenderer(new CheckBoxCellRenderer<>());
        filterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        filterList.setModel(filterListModel);

        // Add a mouse listener to activate the apply button when something changes
        filterList.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent event)
            {

                //noinspection unchecked
                JList<EventLogCheckListItem> list = (JList<EventLogCheckListItem>) event.getSource();

                // Get item clicked
                int index = list.locationToIndex(event.getPoint());
                EventLogCheckListItem item = list.getModel().getElementAt(index);

                item.setSelected(!item.isSelected()); // Toggle selected state
                list.repaint(list.getCellBounds(index, index)); // Repaint cell

                applyButton.setEnabled(true);
            }
        });
    }

    /**
     * Add/Remove all of the appropriate filters then hide the dialog.
     */
    private void okButtonActionPerformed()
    {
        applyButtonActionPerformed();
        dispose();
    }

    /**
     * Just dispose the dialog on cancel.
     */
    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    /**
     * Add/Remove all the appropriate filters.
     */
    private void applyButtonActionPerformed()
    {
        // Determine the set of selected types
        List<EventLogType> activeFilters = new ArrayList<>();
        int size = filterListModel.size();
        for (int i = 0; i < size; i++)
        {
            EventLogCheckListItem item = filterListModel.elementAt(i);
            if (item.isSelected())
            {
                activeFilters.add(item.getEventType());
            }
        }

        eventLogPanel.setActiveFilters(activeFilters); // Install the selected filters

        applyButton.setEnabled(false);
    }

    /**
     * Reset any check box states before closing.
     *
     * @since 3.3
     */
    @Override
    protected void onWindowClosing()
    {
        setVisible(false);
    }

    /**
     * Override to reset the state of any of the check boxes that were changed, but where apply of ok was not clicked.
     *
     * @param visible visible or not
     */
    @Override
    public void setVisible(boolean visible)
    {
        if (visible)
        {
            final int size = filterListModel.getSize();
            for (int i = 0; i < size; i++)
            {
                EventLogCheckListItem filter = filterListModel.getElementAt(i);
                filter.setSelected(eventLogPanel.isActiveEventFilter(filter.getEventType()));
            }
        }

        applyButton.setEnabled(false);
        super.setVisible(visible);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var scrollPane1 = new JScrollPane();
        filterList = new JList<>();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        var cancelButton = new JButton();
        applyButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Event Log Filter Options");
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default:grow"));

                //======== scrollPane1 ========
                {
                    scrollPane1.setName("scrollPane1");

                    //---- filterList ----
                    filterList.setName("filterList");
                    scrollPane1.setViewportView(filterList);
                }
                contentPanel.add(scrollPane1, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button, $lcgap, default",
                        "pref"));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(4, 1));

                //---- applyButton ----
                applyButton.setText("Apply");
                applyButton.setEnabled(false);
                applyButton.setName("applyButton");
                applyButton.addActionListener(e -> applyButtonActionPerformed());
                buttonBar.add(applyButton, cc.xy(6, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JList<EventLogCheckListItem> filterList;
    private JButton applyButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
