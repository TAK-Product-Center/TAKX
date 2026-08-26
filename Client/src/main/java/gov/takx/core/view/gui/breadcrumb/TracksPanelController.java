package gov.takx.core.view.gui.breadcrumb;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.maps.Marker;
import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.breadcrumb.event.TrackBreadcrumbsUpdated;
import gov.takx.api.breadcrumb.event.TrackCreated;
import gov.takx.api.breadcrumb.event.TrackDeleted;
import gov.takx.api.breadcrumb.event.TrackUpdated;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.platform.utils.TimeBoundedTask;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Manages the {@link TracksPanel}.
 *
 * @since 5.3
 */
@ApplicationScoped
public class TracksPanelController implements PropertyChangeListener
{
    private static final List<String> PROPERTIES = List.of(IMapObject.NAME, IMapObject.ICON);

    private final Set<Marker> markers = new TreeSet<>(Comparator.comparing(Marker::getName));
    private final DefaultListModel<Marker> model = new DefaultListModel<>();
    private final boolean headless;
    private TracksPanel panel;

    @VisibleForTesting
    final TimeBoundedTask modelUpdater = new TimeBoundedTask("Tracks Model Updater", 250, 5_000, this::updateModel);

    @Inject
    TracksPanelController(IMapObjectFinder mapObjectFinder, @Headless boolean headless)
    {
        this.headless = headless;

        // Find all markers
        final var markers = mapObjectFinder.findAllMapObjects().stream()
                .filter(Marker.class::isInstance)
                .map(Marker.class::cast)
                .toList();

        // Add the marker's the list model
        SwingUtilities.invokeLater(() -> markers.forEach(this::addMarker));
    }

    /**
     * Update the tracks model from our current set of markers.
     * @since 5.5.1
     */
    @InvokeOnEDT
    private void updateModel()
    {
        synchronized (markers)
        {
            model.clear();
            model.addAll(markers);
        }
    }

    /**
     * @return The {@link TracksPanel} managed by this controller
     */
    @Nonnull
    public ATitledPanel getPanel()
    {
        return panel;
    }

    @VisibleForTesting
    DefaultListModel<Marker> getModel()
    {
        return model;
    }

    @SuppressWarnings({"unused", "SameParameterValue"})
    @VisibleForTesting
    void startService(@Observes @Priority(ServiceOrdering.START_DockingSystem - 1) StartProjectServices event)
    {
        // create before dock is initialized and getPanel() is called
        if (!headless) panel = new TracksPanel(model);
    }

    @SuppressWarnings({"unused", "SameParameterValue"})
    @VisibleForTesting
    void stopService(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices event)
    {
        synchronized (markers)
        {
            markers.clear();
            model.clear();
        }

        if (!headless && panel != null)
        {
            panel.releaseResources();
            panel = null;
        }
    }

    @SuppressWarnings({"unused", "SameParameterValue"})
    @VisibleForTesting
    void mapObjectAdded(@Observes MapObjectAdded event)
    {
        if (!headless && event.getMapObject() instanceof Marker marker)
        {
            addMarker(marker);
            marker.addDeletionHandler(mapItem -> deleteMarker(marker));
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        panel.markerInfoUpdated((Marker) evt.getSource());
    }

    @SuppressWarnings("unused")
    private void onTrackCreated(@Observes TrackCreated event)
    {
        if (panel != null)
        {
            panel.refreshMarkerList(); // Refresh the list of markers to update track counts
            panel.refreshTrackList(event.track().getMarkerUid()); // Update the marker's list of tracks if it's showing
        }
    }

    @SuppressWarnings("unused")
    private void onTrackBreadcrumbsUpdated(@Observes TrackBreadcrumbsUpdated event)
    {
        // Refresh the track if it's being displayed
        if (panel != null) panel.updateTrack(event.track());
    }

    @SuppressWarnings("unused")
    private void onTrackUpdated(@Observes TrackUpdated event)
    {
        // Refresh the track if it's being displayed
        if (panel != null) panel.updateTrack(event.track());
    }

    @SuppressWarnings("unused")
    private void onTrackDeleted(@Observes TrackDeleted event)
    {
        if (panel != null)
        {
            panel.refreshMarkerList(); // Refresh the list of markers to update the track counts
            panel.refreshTrackList(event.track().getMarkerUid()); // Remove it from the track list if it's showing
        }
    }

    /**
     * Adds a marker map object to the model in alphabetical order and adds a property change listener.
     *
     * @param marker The marker to add
     */
    @InvokeOnEDT
    private void addMarker(@Nonnull Marker marker)
    {
        synchronized (markers)
        {
            markers.add(marker);
            modelUpdater.request();
        }

        marker.addPropertyChangeListener(PROPERTIES, this);
    }

    /**
     * Removes the property change listener and cleans up the UI when a {@link Marker} is deleted.
     * 
     * @param marker The marker to delete
     */
    @VisibleForTesting
    @InvokeOnEDT
    void deleteMarker(@Nonnull Marker marker)
    {
        synchronized (markers)
        {
            markers.remove(marker);
            modelUpdater.request();
        }

        // markers may still be deleted after this controller is shut down
        if (panel != null) panel.onMarkerDeleted(marker.getUid());
    }
}
