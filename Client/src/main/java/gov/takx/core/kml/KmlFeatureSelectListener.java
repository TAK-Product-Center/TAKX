package gov.takx.core.kml;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.pick.PickedObject;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.commons.swing.MouseClickedListener;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.kml.KmlVisualManager.KmlModelAndFeature;
import gov.takx.core.kml.properties.KmlFeaturePropertiesPanel;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.view.gui.CoreComponentType;
import gov.takx.core.view.gui.IExtendedUISpaceManager;
import gov.takx.core.view.gui.propertiespanel.APropertiesItem;
import gov.takx.core.view.gui.propertiespanel.IPropertiesItem;
import gov.takx.core.view.gui.propertiespanel.PropertiesPanel;
import gov.takx.core.view.gui.propertiespanel.RemovePropertiesItem;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Service to listen for left-clicks on KML Features and to present them in the Properties panel.
 *
 * @see KmlFeaturePropertiesPanel
 * @since 5.5
 */
@ApplicationScoped
public class KmlFeatureSelectListener implements SelectListener
{
    private static final String NO_NAME = "[no name]";

    private final IExtendedUISpaceManager uiSpaceManager;
    private final IExtendedGlobeManager globeManager;
    private final IMapObjectManager mapObjectManager;
    private final EventBus uiEventBus;

    private volatile KmlModelAndFeature currentPropertiesSelection;

    @Inject
    KmlFeatureSelectListener(IExtendedUISpaceManager uiSpaceManager, IExtendedGlobeManager globeManager, IMapObjectManager mapObjectManager, @UiEventBus EventBus uiEventBus)
    {
        this.uiSpaceManager = uiSpaceManager;
        this.globeManager = globeManager;
        this.mapObjectManager = mapObjectManager;
        this.uiEventBus = uiEventBus;
    }

    private void startService(@Observes @Priority(ServiceOrdering.START_GenericCoreService) StartProjectServices ignored)
    {
        PropertiesPanel.getInstance().addPropertiesItemListener(this::propertiesItemChanged);

        globeManager.doWhenGlobeReady(globe -> {
            final var wwd = globe.getWorldWindow();

            wwd.addSelectListener(this); // So we can detect clicks on our items

            // Listen for clicks on the globe surface so we can clear our selected item
            wwd.addMouseListener((MouseClickedListener) e -> {
                if (e.isConsumed()) return;

                // If one of our items is showing, and the top object is not one of ours, then remove our current item
                if (currentPropertiesSelection != null && !(wwd.getObjectsAtCurrentPosition().getTopObject() instanceof KmlModelAndFeature))
                {
                    uiEventBus.post(new RemovePropertiesItem(currentPropertiesSelection));
                }
            });
        });
    }

    /**
     * Handle a change in the properties item being displayed in the properties panel.  We need to track when something
     * other than one of our items is selected to avoid clearing it later.
     */
    private void propertiesItemChanged(@Nullable IPropertiesItem propertiesItem)
    {
        currentPropertiesSelection = propertiesItem != null && propertiesItem.getIdentityObject() instanceof KmlModelAndFeature kmlModelAndFeature
                ? kmlModelAndFeature
                : null;
    }

    /**
     * If one of our items is selected, show the properties for it.
     */
    @Override
    public void selected(SelectEvent event)
    {
        if (!event.isConsumed() && event.isLeftClick() && event.getTopObject() instanceof KmlModelAndFeature)
        {
            final var selected = getSelectedFeature(event);
            if (selected != null)
            {
                mapObjectManager.setSelected(null); // Clear any current selection
                uiSpaceManager.showComponent(CoreComponentType.PROPERTIES_PANEL);
                currentPropertiesSelection = selected;
                uiEventBus.post(new KmlFeaturePropertiesItem(selected));
            }
        }
    }

    /**
     * If a single feature was selected, return that.  If more than one, show a dialog to let the user pick one.
     *
     * @return selected feature, or {@code null} if the user cancels the selection
     * @since 5.5.1
     */
    @Nullable
    private KmlModelAndFeature getSelectedFeature(@Nonnull SelectEvent event)
    {
        final var features = event.getObjects().stream()
                .map(PickedObject::getObject)
                .filter(KmlModelAndFeature.class::isInstance)
                .map(KmlModelAndFeature.class::cast)
                .toList();

        return features.size() == 1 ? features.get(0) : new FeatureSelectionDialog(features).getSelectedFeature();
    }

    /**
     * Properties item to provide the panel to display when a KML feature is clicked.
     */
    private static class KmlFeaturePropertiesItem extends APropertiesItem
    {
        static String makeLabel(Feature kmlFeature)
        {
            final String name = kmlFeature.getName();
            return "KML : " + kmlFeature.getClass().getSimpleName() + " " + (StringUtils.isBlank(name) ? name : NO_NAME);
        }

        KmlFeaturePropertiesItem(KmlModelAndFeature modelAndFeature)
        {
            super(new KmlFeaturePropertiesPanel(modelAndFeature), makeLabel(modelAndFeature.feature()), modelAndFeature, "Kml Feature");
        }

        @Override
        public boolean isDialogOpen()
        {
            return false;
        }

        @Override
        public boolean addScrollPane()
        {
            return false;
        }
    }

    /**
     * Simple dialog to display a list of features and allow the user to select one.
     * @since 5.5.1
     */
    private static class FeatureSelectionDialog extends AStandardDialog
    {
        private final JList<KmlModelAndFeature> featureList = new JList<>();
        private final JButton okButton = new JButton("OK");
        private KmlModelAndFeature selectedFeature;

        FeatureSelectionDialog(@Nonnull List<KmlModelAndFeature> features)
        {
            initComponents();

            final var listModel = new DefaultListModel<KmlModelAndFeature>();
            listModel.addAll(features);
            featureList.setModel(listModel);

            // Allow double click to select an item
            featureList.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    if (e.getClickCount() == 2)
                    {
                        int index = featureList.locationToIndex(e.getPoint());
                        if (index != -1)
                        {
                            selectedFeature = featureList.getModel().getElementAt(index);
                            dispose();
                        }
                    }
                }
            });

            // Render the feature name
            featureList.setCellRenderer(new DefaultListCellRenderer()
            {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
                {
                    final String name = value != null ? ((KmlModelAndFeature) value).feature().getName() : null;
                    final String display = StringUtils.isBlank(name) ? NO_NAME : name;
                    return super.getListCellRendererComponent(list, display, index, isSelected, cellHasFocus);
                }
            });

            pack();
        }

        KmlModelAndFeature getSelectedFeature()
        {
            setVisible(true);
            return selectedFeature;
        }

        private void initComponents()
        {
            setTitle("Select from the features below");
            setModal(true);
            setMinimumSize(new Dimension(150, 200));
            setPreferredSize(null);
            setName("this");
            getContentPane().setLayout(new BorderLayout());

            featureList.setName("featureList");
            featureList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            final var scrollPane = new JScrollPane(featureList);
            scrollPane.setName("scrollPane");
            scrollPane.setPreferredSize(new Dimension(250, 150));
            add(scrollPane, BorderLayout.CENTER);

            okButton.addActionListener(e -> {
                selectedFeature = featureList.getSelectedValue();
                dispose();
            });

            final var buttonPanel = new JPanel();
            buttonPanel.add(okButton);
            add(buttonPanel, BorderLayout.SOUTH);
        }
    }
}
