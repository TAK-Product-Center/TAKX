package gov.takx.core.mapobject;

import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.pick.PickedObject;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.swing.KeyPressedListener;
import gov.takx.core.mapobject.gui.GlobeMapObjectSelectionDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.event.EventListenerList;

import java.awt.event.KeyEvent;
import java.util.EventListener;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A listener that allows the user to select one {@link IMapObject} from the globe.
 * Can be cancelled by pressing {@code ESC}.
 *
 * @since 5.0
 */
@PseudoBean
public class CancelableMapObjectSelectionListener implements SelectListener, KeyPressedListener
{
    private static final String DEFAULT_CANCEL_MESSAGE = "Press ESC to cancel.";
    private static final String DEFAULT_SELECT_MESSAGE = "Click to select a map item.";

    private final EventListenerList listenerList = new EventListenerList();
    private final String selectMessage;
    private final Predicate<IMapObject> filter;
    private final boolean multiple = false;
    @Inject
    private IExtendedGlobeManager globeManager;

    private List<IMapObject> selected;

    /**
     * Construct a new instance with the given select listener, no filter, and the default select message.
     *
     * @param listener an optional listener to call when a selection has been made
     * @since 5.6
     */
    public CancelableMapObjectSelectionListener(@Nullable SelectedListener listener)
    {
        this(listener, null, null);
    }

    /**
     * Construct a new instance with the given listener, filter, and message.
     *
     * @param listener an optional listener to call when a selection has been made
     * @param filter a predicate to use to filter any selections
     * @param selectMessage globe message to use or {@code null} to use default
     * @since 5.6
     */
    public CancelableMapObjectSelectionListener(@Nullable SelectedListener listener, @Nullable Predicate<IMapObject> filter, @Nullable String selectMessage)
    {
        this.selectMessage = selectMessage == null ? DEFAULT_SELECT_MESSAGE : selectMessage;
        this.filter = filter;
        if (listener != null)
        {
            listenerList.add(SelectedListener.class, listener);
        }
    }

    /**
     * Initiates the selection process.
     */
    public void register()
    {
        globeManager.showGlobeMessage(selectMessage + " " + DEFAULT_CANCEL_MESSAGE);
        globeManager.getAllGlobeDelegates().forEach(globe -> {
            globe.setSelectListenerActive(false);
            globe.getWorldWindow().getComponent().requestFocus();
            globe.getWorldWindow().addKeyListener(this);
            globe.getWorldWindow().addSelectListener(this);
        });
    }

    @Override
    public void keyPressed(@Nonnull KeyEvent e)
    {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) cancel();
    }

    @Override
    public void selected(SelectEvent event)
    {
        if (event.isLeftClick())
        {
            List<IGlobeMapObject> filteredList = event.getObjects().stream()
                    .map(PickedObject::getObject)
                    .filter(object -> object instanceof IGlobeMapObject gmo && gmo.getMapObject() != null && gmo.getMapObject().isSelectable())
                    .map(IGlobeMapObject.class::cast)
                    .distinct()
                    .filter(gmo -> filter == null || filter.test(gmo.getMapObject()))
                    .toList();

            if (filteredList.isEmpty()) return;

            if (!multiple && filteredList.size() > 1)
            {
                IGlobeMapObject dialogSelected = new GlobeMapObjectSelectionDialog<>(filteredList).getSelectedObject();
                if (dialogSelected == null) return;
                selected = List.of(dialogSelected.getMapObject());
            } else
            {
                selected = filteredList.stream().map(IGlobeMapObject::getMapObject).toList();
            }

            event.consume();
            complete();
        }
    }

    private void cancel()
    {
        Stream.of(listenerList.getListeners(CancelListener.class)).forEach(CancelListener::cancelled);
        complete();
    }

    private void complete()
    {
        globeManager.getAllGlobeDelegates().forEach(globe -> {
            globe.getWorldWindow().removeKeyListener(this);
            globe.getWorldWindow().removeSelectListener(this);
            globe.setSelectListenerActive(true);

        });
        globeManager.hideGlobeMessage();

        Stream.of(listenerList.getListeners(SelectedListener.class)).forEach(l -> l.selected(selected));
    }

    /**
     * Listener to handle selection events.
     *
     * @since 5.6
     */
    public interface SelectedListener extends EventListener
    {
        /**
         * @param list the selected map objects, or {@code null} if selection was cancelled
         */
        void selected(@Nullable List<IMapObject> list);
    }

    /**
     * Listener to handle cancellation.
     *
     * @since 5.6
     */
    public interface CancelListener extends EventListener
    {
        /**
         * Called when the selection process is cancelled by the user.
         */
        void cancelled();
    }
}
