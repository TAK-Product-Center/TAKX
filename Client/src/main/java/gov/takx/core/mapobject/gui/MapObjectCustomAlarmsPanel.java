package gov.takx.core.mapobject.gui;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.persist.ICustomDataMonitorPersist;
import gov.takx.commons.swing.AListAndDetailPanel;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.IPanelStateListener;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.mapobject.MapObjectController;
import gov.takx.core.mapobject.gui.alarms.AAlarmPanel;
import gov.takx.core.mapobject.gui.alarms.DataMonitorAlarmPanel;
import gov.takx.core.mapobject.gui.alarms.GeofenceAlarmPanel;
import gov.takx.dataservices.persist.CustomDataMonitor;
import gov.takx.dataservices.persist.CustomDataMonitor.AlarmType;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MapObjectCustomAlarmsPanel extends MapObjectMainPreferencesPanel
{
    private static final String TITLE = "Custom Alarms";

    @Inject
    private ICustomDataMonitorPersist customDataMonitorPersist;

    private final DataMonitorPanel dataMonitorPanel;
    private List<CustomDataMonitor> initialDataMonitors;

    public MapObjectCustomAlarmsPanel(IMapObjectController mapObjectController, IPanelStateListener panelListener)
    {
        super(TITLE, mapObjectController, panelListener);

        initialDataMonitors = getMapObjectController().getCustomDataMonitors();

        setLayout(new BorderLayout());

        dataMonitorPanel = new DataMonitorPanel(panelListener);
        dataMonitorPanel.initialize(initialDataMonitors);

        add(dataMonitorPanel);
        revalidate();
    }

    public void addAlarm(@Nonnull CustomDataMonitor alarm)
    {
        dataMonitorPanel.addEntry(alarm);
    }

    /**
     * @return Concrete reference to our MapObjectController.
     * @since 3.4
     */
    @Override
    protected MapObjectController getMapObjectController()
    {
        return (MapObjectController) mapObjectController;
    }

    @Override
    protected void applyActionPerformed()
    {
        dataMonitorPanel.applyActions();
    }

    @Override
    public String validateContents()
    {
        return dataMonitorPanel.validateContents();
    }

    @Override
    public URL getIcon()
    {
        return UIResources.getUrl(PLATFORM_ICONS, "led_red.png");
    }

    /**
     * This is the real meat of the panel.  Our outer class has to extend MapObjectMainPreferencesPanel, and
     * thus cannot extend AListAndDetailPanel.  All the work of displaying the data monitors and their
     * detail panels is handled here.
     *
     * @since 3.4
     */
    private class DataMonitorPanel extends AListAndDetailPanel<CustomDataMonitor, AAlarmPanel>
    {
        protected DataMonitorPanel(IPanelStateListener panelStateListener)
        {
            super(panelStateListener);
            setEntryTypeNames("Alarm", "Alarms");
        }

        @Override
        protected DataMonitorEntryProxy createEntryProxy(CustomDataMonitor dataMonitor)
        {
            return new DataMonitorEntryProxy(dataMonitor);
        }

        @Override
        public void applyActions()
        {
            String errorMessage = checkForDuplicateDataMonitorNames();
            if (errorMessage == null)
            {
                commit(); // Commit all the updates to our model

                // Iterate over all the data monitors and add/remove/update them from the MOC as appropriate.
                final List<String> originalKeys = initialDataMonitors.stream().map(cdm -> cdm.getDataMonitor().getUniqueKey()).toList();
                final List<String> currentKeys = rows.stream().map(row -> row.entryProxy.getEntry().getDataMonitor().getUniqueKey()).toList();

                final MapObjectController mapObjectController = getMapObjectController();

                // Handle new/existing data monitors
                getEntries().forEach(cdm -> {
                    final String key = cdm.getDataMonitor().getUniqueKey();
                    if (!originalKeys.contains(key))
                    {
                        mapObjectController.addCustomDataMonitor(cdm); // Add new one
                    } else
                    {
                        customDataMonitorPersist.persistDataMonitor(cdm); // Persist changes on existing entity
                    }
                });

                // Handle removed monitors
                initialDataMonitors.forEach(cdm -> {
                    final String key = cdm.getDataMonitor().getUniqueKey();
                    if (!currentKeys.contains(key)) mapObjectController.removeCustomDataMonitor(cdm);
                });

                initialDataMonitors = getEntries(); // Update our "initial" list
            } else
            {
                // The validation failed; present the user with the display name(s) that need attention
                AlwaysOnTopJOptionPane.showErrorDialogLater(errorMessage, "Error");
            }
        }

        /**
         * Show the add custom alarm menu IF the controller defines data structures.  Otherwise, display the
         * info dialog.  This method should only be called from the EDT.
         */
        @Override
        protected void addButtonActionPerformed()
        {
            if (areDataStructuresValid(mapObjectController.getDataStructures()))
            {
                showAddActions(getMapObjectController().requestAddAlarmActions(MapObjectCustomAlarmsPanel.this));
            } else
            {
                AlwaysOnTopJOptionPane.showMessageDialog("This device does not support custom alarms", "Unsupported Operation", JOptionPane.INFORMATION_MESSAGE);
            }
        }

        private boolean areDataStructuresValid(Collection<IRaptorDataStructure> structures)
        {
            return structures != null
                    && structures.stream()
                            .anyMatch(structure -> !DataStructureBuilder.LOCATION_MESSAGE_TYPE.equalsIgnoreCase(structure.getMessageType()));
        }

        @Override
        public String validateContents()
        {
            StringBuilder feedbackMessage = new StringBuilder();
            for (TableRow row : rows)
            {
                String tempFeedbackMessage = row.validationHandler.validateContents();
                if (tempFeedbackMessage != null) feedbackMessage.append(tempFeedbackMessage);
            }

            if (feedbackMessage.isEmpty())
            {
                // Now that we know all the view component display names are valid, it is safe to check for duplicates
                String tempFeedbackMessage = checkForDuplicateDataMonitorNames();
                if (tempFeedbackMessage != null) feedbackMessage.append(tempFeedbackMessage);
            }

            return feedbackMessage.isEmpty() ? null : feedbackMessage.toString();
        }

        /**
         * This method assumes that all display names are not {@code null}. This method checks to see if any data
         * monitors have the same name, including the ones added via the API.
         *
         * @return {@code null} if no duplicate names are found, else an error message if duplicates are found
         */
        private String checkForDuplicateDataMonitorNames()
        {
            // Collect all the current display names in use
            final Set<String> displayNames = mapObjectController.getDataMonitors().stream()
                    .map(IDataMonitor::getDisplayName)
                    .collect(Collectors.toSet());

            // Since the MapObjectController's dataMonitors list also stores customDataMonitors, we need to remove those before checking for duplicates
            getMapObjectController().getCustomDataMonitors().stream()
                    .map(CustomDataMonitor::getDisplayName)
                    .forEach(displayNames::remove);

            // Now see if there are any duplicates in the custom alarms rows
            final List<String> duplicates = rows.stream()
                    .map(row -> row.validationHandler.getDisplayName())
                    .filter(displayName -> !displayNames.add(displayName))
                    .toList();

            // Return an error message if a duplicate name was found, else null
            return duplicates.isEmpty()
                    ? null
                    : "The following duplicate display names were found, please change them: \n"
                            + StringUtils.join(duplicates, "\n", null); // No duplicate names detected
        }

        /**
         * Proxy to stand in between our superclass' table entries and {@link CustomDataMonitor} instances.
         */
        protected class DataMonitorEntryProxy extends EntryProxy
        {
            private static final Map<AlarmType, String> ALARM_TYPE_PREFIX = Map.of(
                    AlarmType.Geofence, "GF",
                    AlarmType.DataMonitor, "DM");

            private final CustomDataMonitor customDataMonitor;
            private final String typePrefix;
            public final AAlarmPanel viewComponent;

            protected DataMonitorEntryProxy(CustomDataMonitor customDataMonitor)
            {
                this.customDataMonitor = customDataMonitor;
                typePrefix = ALARM_TYPE_PREFIX.get(customDataMonitor.getAlarmType()) + ": ";

                viewComponent = switch (customDataMonitor.getAlarmType())
                {
                    case DataMonitor -> new DataMonitorAlarmPanel(customDataMonitor, mapObjectController.getDataStructures());
                    case Geofence -> new GeofenceAlarmPanel(customDataMonitor);
                };
            }

            @Override
            public CustomDataMonitor getEntry()
            {
                return customDataMonitor;
            }

            @Override
            protected String getKey()
            {
                return customDataMonitor.getDataMonitor().getUniqueKey();
            }

            @Override
            protected boolean isEnabled()
            {
                return customDataMonitor.isEnabled();
            }

            @Override
            protected void setEnabled(boolean enabled)
            {
                customDataMonitor.setEnabled(enabled);
            }

            @Override
            protected String getDisplayName()
            {
                return typePrefix + customDataMonitor.getDisplayName();
            }

            @Override
            protected AAlarmPanel getValidationHandler()
            {
                return viewComponent;
            }

            @Override
            protected JPanel getDetailPanel()
            {
                return viewComponent;
            }

            @Override
            protected void applyAction()
            {
                viewComponent.applyAction();
            }
        }
    }
}
