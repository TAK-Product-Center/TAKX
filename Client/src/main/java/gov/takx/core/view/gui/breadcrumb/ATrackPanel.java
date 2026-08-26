package gov.takx.core.view.gui.breadcrumb;

import com.atakmap.coremap.maps.coords.GeoBounds;
import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.breadcrumb.ITrack;
import gov.takx.api.breadcrumb.event.TrackVisibilityChanged;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.swing.ActionPopupFactory;
import gov.takx.core.breadcrumb.BreadcrumbManager;
import gov.takx.core.breadcrumb.TrackExportService;
import gov.takx.core.persist.breadcrumb.Track;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.units.AltitudeUnits;
import gov.takx.platform.utils.units.RangeUnits;
import gov.takx.platform.utils.units.SpeedUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;

import java.awt.Component;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Provides common functionality for panels that display track info.
 *
 * @since 5.3
 */
@PseudoBean
public abstract class ATrackPanel extends JPanel implements IResourceCleaner
{
    protected static final Icon VISIBLITY_OFF_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "visibility_off.png");
    protected static final Icon VISIBLITY_ON_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "visibility.png");

    @Inject
    protected BreadcrumbManager breadcrumbManager;
    @Inject
    protected IGlobeManager globeManager;
    @Inject
    protected TrackExportService trackExportService;

    protected IPreferenceMonitor<AltitudeUnits> altitudeUnitsMonitor = UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS.newMonitor(AltitudeUnits.class, u -> refreshAll());
    protected IPreferenceMonitor<RangeUnits> rangeUnitsMonitor = UserPreferenceEntry.DISPLAY_RANGE_UNITS.newMonitor(RangeUnits.class, u -> refreshAll());
    protected IPreferenceMonitor<SpeedUnits> speedUnitsMonitor = UserPreferenceEntry.DISPLAY_SPEED_UNITS.newMonitor(SpeedUnits.class, u -> refreshAll());
    protected Track track;

    @PostConstruct
    private void postConstruct()
    {
        refreshAll();
    }

    @RemoveObserver
    @Override
    public void releaseResources()
    {
        altitudeUnitsMonitor.dispose();
        rangeUnitsMonitor.dispose();
        speedUnitsMonitor.dispose();
    }

    /**
     * Refreshes all {@link gov.takx.api.breadcrumb.ITrack} values displayed by this panel.
     */
    public abstract void refreshAll();

    /**
     * Configures the visibility button by setting the icon and adding the listener that will update the 
     * {@link gov.takx.api.breadcrumb.ITrack track's} visibility.
     * 
     * @param track The track to update
     * @param button The button to configure
     */
    protected void prepareVisibilityButton(@Nonnull Track track, @Nonnull JButton button)
    {
        for (var actionListener : button.getActionListeners()) button.removeActionListener(actionListener);

        setVisibilityButtonIcon(track.isVisible());

        button.addActionListener(e -> {
            track.setVisible(!track.isVisible());
            if (track.isVisible()) gotoTrack(track);
        });
    }

    @SuppressWarnings("unused")
    private void onTrackVisibilityChanged(@Observes TrackVisibilityChanged event)
    {
        if (track != null && track.getUid().equals(event.track().getUid()))
        {
            setVisibilityButtonIcon(track.isVisible());
        }
    }

    /**
     * Sets the visibility button's icon to reflect the track's current state.
     * 
     * @param visible {@code true} if the track is visible
     */
    protected abstract void setVisibilityButtonIcon(boolean visible);

    /**
     * @param meters The length in meters
     * @param decimalPoints The number od decimal points to include
     * @return A string representation of the length using the current range units preference
     */
    protected String getLengthString(double meters, int decimalPoints)
    {
        return rangeUnitsMonitor.getCurrentValue().format(meters, decimalPoints);
    }

    /**
     * @param source Component source to use for relative positioning of the popup
     * @param exportRequestConsumer The consumer that will handle the user's selection
     */
    protected void showExportPopup(@Nonnull Component source, @Nonnull ITrack track,
                                   @Nonnull Consumer<TrackExportService.FileType> exportRequestConsumer)
    {
        final var popupBuilder = ActionPopupFactory.builder("Export", source);
        final var publishingSupported = trackExportService.publishingSupported(track.getMarkerUid());

        Arrays.stream(TrackExportService.FileType.values())
                .filter(fileType -> fileType != TrackExportService.FileType.SERVER || publishingSupported)
                .sorted(Comparator.comparing(Enum::name))
                .forEach(fileType -> popupBuilder.action(fileType.name(), () -> exportRequestConsumer.accept(fileType)));

        popupBuilder.build().show();
    }

    /**
     * Navigates to a {@link ITrack track's} sector
     *
     * @param track The track to navigate to
     * @since 5.6
     */
    private void gotoTrack(@Nonnull ITrack track)
    {
        final GeoBounds geoBounds = ((Track) track).getGeoBounds();

        if (geoBounds != null)
        {
            final var sector = Sector.fromDegrees(geoBounds.getSouth(), geoBounds.getNorth(), geoBounds.getEast(), geoBounds.getWest());
            //noinspection DataFlowIssue
            globeManager.getLastFocusedGlobe().goTo(sector);
        }
    }
}
