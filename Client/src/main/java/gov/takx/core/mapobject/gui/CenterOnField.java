package gov.takx.core.mapobject.gui;

import com.google.common.annotations.VisibleForTesting;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.api.ui.event.GlobeCreatedEvent;
import gov.takx.api.ui.event.GlobeRemovedEvent;
import gov.takx.api.ui.event.MapObjectTrackingEvent;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.commons.swing.FormLayoutUtils;
import gov.takx.commons.swing.SimpleAction;
import gov.takx.commons.swing.SplitButton;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.CompoundBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A field that allows selecting globes to be centered on. The field displays a simple checkbox if only one globe exists and a SplitButton if multiple
 * globes exist. It monitors the addition / removal of globes so that the field contents remains valid.
 * <p>
 * It can optionally monitor the centered-on state for a given map object and update the field value as necessary. While this type of monitoring would
 * normally be handled outside the field, the specificity of the monitoring, which requires subscribing to the {@link EventBus}, makes it worth having
 * as a feature of this component. When used in a non-immediate form, that feature should not be used, or else the field contents can change after
 * the user has set them. It should also be noted that a {@link ChangeEvent} is <i>not</i> fired when this field is updated due to an external
 * tracking event change.
 *
 * @implNote This field could also be used to manage the "Keep in View" setting, with a few minor modifications.
 * @since 5.8
 */
@PseudoBean
public class CenterOnField extends JPanel implements IResourceCleaner
{
    private final ChangeEvent changeEvent = new ChangeEvent(this);
    private final List<ChangeListener> changeListeners = new ArrayList<>();
    private final Set<Integer> selectedGlobeIds = new HashSet<>();
    @VisibleForTesting
    final Map<Integer, JCheckBoxMenuItem> popupItems = new HashMap<>();

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    @UiEventBus
    private EventBus uiEventBus;

    private IMapObject monitoringObject;

    /**
     * Construct a new empty component.
     */
    public CenterOnField()
    {
        initComponents();
        // Try to reduce the empty border normally present around text-only buttons
        splitButton.setBorder(new CompoundBorder(splitButton.getBorder(), Borders.createEmptyBorder("0px,-3dlu,0px,-6dlu")));
    }

    @PostConstruct
    private void postConstruct()
    {
        globeManager.getAllGlobeDelegates().forEach(g -> addOrRemoveGlobe(g.getGlobeId(), true));
        uiEventBus.register(this);
    }

    @Override
    public void releaseResources()
    {
        uiEventBus.unregister(this);
    }

    /**
     * @param e the event
     */
    @Subscribe
    @InvokeOnEDT
    public void onGlobeCreated(GlobeCreatedEvent e)
    {
        addOrRemoveGlobe(e.getGlobeId(), true);
    }

    /**
     * @param e the event
     */
    @Subscribe
    @InvokeOnEDT
    public void onGlobeRemoved(GlobeRemovedEvent e)
    {
        addOrRemoveGlobe(e.getGlobeId(), false);
    }

    /**
     * @param e the tracking event
     */
    @Subscribe
    @InvokeOnEDT
    public void onTrackingEvent(MapObjectTrackingEvent e)
    {
        if (monitoringObject == null || e.getTrackingType() != MapObjectTrackingEvent.TrackingType.CENTER_ON || e.getMapObjectController().getMapObject() != monitoringObject) return;
        addOrRemoveSelectedGlobeIdInternal(e.getGlobeId(), e.getTrackingState() == MapObjectTrackingEvent.TrackingState.STARTED, false);
    }

    /**
     * @param mapObject the map object to derive selected globe ids from
     * @param monitor {@code true} to monitor the given object, otherwise {@code false}
     */
    public void setSelectedGlobeIds(@Nonnull IMapObject mapObject, boolean monitor)
    {
        monitoringObject = monitor ? mapObject : null;
        popupItems.keySet().forEach(globeId -> {
            var globe = globeManager.getGlobeDelegate(globeId);
            if (globe != null) addOrRemoveSelectedGlobeIdInternal(globeId, globe.isCenterOn(mapObject), false);
        });
    }

    /**
     * @return the Set of currently selected globe ids
     */
    @Nonnull
    public Set<Integer> getSelectedGlobeIds()
    {
        return selectedGlobeIds;
    }

    /**
     * @param newSelectedGlobeIds the Set of globe ids to select
     */
    public void setSelectedGlobeIds(@Nonnull Set<Integer> newSelectedGlobeIds)
    {
        selectedGlobeIds
                .stream()
                .filter(id -> !newSelectedGlobeIds.contains(id))
                .toList() // Needed since we might modify the collection
                .forEach(id -> addOrRemoveSelectedGlobeIdInternal(id, false, false));

        newSelectedGlobeIds
                .stream()
                .filter(id -> !selectedGlobeIds.contains(id))
                .forEach(id -> addOrRemoveSelectedGlobeIdInternal(id, true, false));
    }

    /**
     * @param listener the listener to add
     */
    public void addChangeListener(@Nonnull ChangeListener listener)
    {
        changeListeners.add(listener);
    }

    /**
     * @param listener the listener to remove
     */
    @SuppressWarnings("unused")
    public void removeChangeListener(@Nonnull ChangeListener listener)
    {
        changeListeners.remove(listener);
    }

    private void addOrRemoveGlobe(int globeId, boolean add)
    {
        if (add)
        {
            var item = new JCheckBoxMenuItem(new SimpleAction("Globe " + globeId, event -> popupAction(globeId)));
            popupMenu.add(item);
            popupItems.put(globeId, item);
        } else
        {
            addOrRemoveSelectedGlobeIdInternal(globeId, false, false);
            popupMenu.remove(popupItems.remove(globeId));
        }
        update();
    }

    private void popupAction(int globeId)
    {
        addOrRemoveSelectedGlobeIdInternal(globeId, popupItems.get(globeId).isSelected(), true);
    }

    private void update()
    {
        var multiMode = popupItems.size() > 1;
        FormLayoutUtils.hideOrRestoreColumn(splitButtonPanel, !multiMode);
        FormLayoutUtils.hideOrRestoreColumn(checkBox, multiMode);
        revalidate();
    }

    private void addOrRemoveSelectedGlobeIdInternal(int globeId, boolean add, boolean fire)
    {
        if (add == selectedGlobeIds.contains(globeId)) return;
        if (add)
        {
            selectedGlobeIds.add(globeId);
        } else
        {
            selectedGlobeIds.remove(globeId);
        }
        checkBox.setSelected(!selectedGlobeIds.isEmpty());
        popupItems.get(globeId).setSelected(add);
        // For some reason we have to right pad the split button when the text is short e.g. 1 character
        splitButton.setText(selectedGlobeIds.isEmpty() ? "None" : StringUtils.rightPad(String.join(",", selectedGlobeIds.stream().map(Object::toString).toList()), 5));
        update();
        if (fire) fireChangeEvent();
    }

    private void fireChangeEvent()
    {
        ExceptionUtils.safeDoOperation(changeListeners, listener -> listener.stateChanged(changeEvent));
    }

    private void checkBoxAction()
    {
        //noinspection OptionalGetWithoutIsPresent
        addOrRemoveSelectedGlobeIdInternal(popupItems.keySet().stream().findFirst().get(), checkBox.isSelected(), true);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        checkBox = new JCheckBox();
        splitButtonPanel = new JPanel();
        var label1 = new JLabel();
        splitButton = new SplitButton();
        popupMenu = new JPopupMenu();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default",
                "fill:default:grow"));

        //---- checkBox ----
        checkBox.setText("Center On Globe");
        checkBox.setName("checkBox");
        checkBox.addActionListener(e -> checkBoxAction());
        add(checkBox, cc.xy(1, 1));

        //======== splitButtonPanel ========
        {
            splitButtonPanel.setName("splitButtonPanel");
            splitButtonPanel.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "fill:default:grow"));

            //---- label1 ----
            label1.setText("Center On Globe");
            label1.setName("label1");
            splitButtonPanel.add(label1, cc.xy(1, 1));

            //---- splitButton ----
            splitButton.setText("None");
            splitButton.setPopupMenu(popupMenu);
            splitButton.setName("splitButton");
            splitButtonPanel.add(splitButton, cc.xy(3, 1));
        }
        add(splitButtonPanel, cc.xy(3, 1));

        //======== popupMenu ========
        {
            popupMenu.setName("popupMenu");
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JCheckBox checkBox;
    private JPanel splitButtonPanel;
    private SplitButton splitButton;
    private JPopupMenu popupMenu;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
