package gov.takx.core.globe.widgets;

import com.atakmap.coremap.maps.coords.NorthReference;
import gov.tak.api.commons.graphics.Font;
import gov.tak.api.commons.graphics.TextFormat;
import gov.tak.api.widgets.IMapWidget;
import gov.tak.platform.ui.MotionEvent;
import gov.tak.platform.widgets.TextWidget;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.globe.AWidget;
import gov.takx.api.globe.CoreWidgetNames;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.commons.lang.ListenerManager;
import gov.takx.core.navigation.NavigationConfiguration;
import gov.takx.core.navigation.NavigationManager;
import gov.takx.core.navigation.PointNavigationConfiguration;
import gov.takx.platform.utils.units.BearingUnits;
import gov.takx.platform.utils.units.RangeUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;

import static gov.takx.core.navigation.NavigationConstants.ETA_FORMAT;
import static gov.takx.platform.preferences.UserPreferenceEntry.COORDINATE_SYSTEM;
import static gov.takx.platform.preferences.UserPreferenceEntry.DISPLAY_BEARING_UNIT;
import static gov.takx.platform.preferences.UserPreferenceEntry.DISPLAY_NORTH_REFERENCE;
import static gov.takx.platform.preferences.UserPreferenceEntry.DISPLAY_RANGE_UNITS;

/**
 * Widget for displaying the next bloodhound intercept.
 *
 * @since 5.6
 */
@PseudoBean
public class BloodhoundWidget extends AWidget implements NavigationManager.NavigationManagerListener
{
    private static final Font FONT = new Font("Segoe UI", Font.Style.Normal, 14);

    private final IPreferenceMonitor<NorthReference> northMonitor = DISPLAY_NORTH_REFERENCE.newMonitor(NorthReference.class, ignored -> update());
    private final IPreferenceMonitor<RangeUnits> rangeMonitor = DISPLAY_RANGE_UNITS.newMonitor(RangeUnits.class, ignored -> update());
    private final IPreferenceMonitor<BearingUnits> bearingMonitor = DISPLAY_BEARING_UNIT.newMonitor(BearingUnits.class, ignored -> update());
    private final IPreferenceMonitor<CoordinateSystem> coordinateMonitor = COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, ignored -> update());
    private final ListenerManager listenerManager = new ListenerManager();
    private final List<String> LISTENER_PROPERTIES = List.of(NavigationConfiguration.ETA, IMapObject.NAME);
    private final IGlobeDelegate globe;
    private final TextWidget textWidget;

    @Inject
    private NavigationManager navigationManager;

    private NavigationConfiguration configuration;

    /**
     * Construct a new widget using the given globe for "goto" operations.
     *
     * @param globe a globe delegate
     */
    public BloodhoundWidget(@Nonnull IGlobeDelegate globe)
    {
        this.globe = globe;

        textWidget = new TextWidget("", new TextFormat(FONT, 0));
        textWidget.setBackground(TextWidget.TRANSLUCENT_BLACK);
        textWidget.setColor(Color.GREEN.getRGB());
        textWidget.addOnClickListener(new WidgetClickListener());
        setPadding(4);
        addChildWidget(textWidget);
        setName(CoreWidgetNames.BLOODHOUND_NEXT_INTERCEPT);
    }

    @PostConstruct
    private void postConstruct()
    {
        navigationManager.addConfigurationListener(this);
    }

    @Override
    public void releaseResources()
    {
        northMonitor.dispose();
        rangeMonitor.dispose();
        bearingMonitor.dispose();
        coordinateMonitor.dispose();
        navigationManager.removeConfigurationListener(this);
        listenerManager.unregisterAll();
    }

    @Override
    public void added(@Nonnull NavigationConfiguration configuration)
    {
        if (isValidType(configuration))
        {
            listenerManager.addPropertyChangeListener(configuration, LISTENER_PROPERTIES, e -> update(configuration));
            update(configuration);
        }
    }

    @Override
    public void removed(@Nonnull NavigationConfiguration configuration)
    {
        listenerManager.unregister(configuration);
        if (configuration == this.configuration) setConfiguration(getNextArrivingConfiguration());
    }

    private void update(@Nonnull NavigationConfiguration config)
    {
        if (config == configuration)
        {
            setConfiguration(getNextArrivingConfiguration());
        } else if ((configuration == null || config.getEtaSec() < configuration.getEtaSec()))
        {
            setConfiguration(config);
        }
    }

    private void setConfiguration(@Nullable NavigationConfiguration configuration)
    {
        this.configuration = configuration;
        setVisible(configuration != null);
        update();
    }

    @Nullable
    private NavigationConfiguration getNextArrivingConfiguration()
    {
        return navigationManager.getConfigurations()
                .stream()
                .filter(this::isValidType)
                .filter(navigation -> navigation.getEtaSec() > -1)
                .min(Comparator.comparing(NavigationConfiguration::getEtaSec))
                .orElse(null);
    }

    private boolean isValidType(@Nonnull NavigationConfiguration navigation)
    {
        return navigation instanceof PointNavigationConfiguration;
    }

    @InvokeOnEDT
    private void update()
    {
        if (configuration == null || !isVisible()) return;
        var currentTarget = configuration.getCurrentTarget();
        var target = currentTarget.getTarget();
        var primary = currentTarget.getTargetArrow();
        var location = CoordinateSystem.getLocationString(target.getLocation(), coordinateMonitor.getCurrentValue());
        var dir = BearingUnits.Direction.fromDegrees8(primary.getBearing()).toString();
        var range = rangeMonitor.getCurrentValue().format(primary.getRange(), 1);
        var bearing = bearingMonitor.getCurrentValue().format(target.getLocation(), primary.getRange(), primary.getBearing());
        var interceptEtaSec = configuration.getCurrentTarget().getInterceptEtaSec();
        var directEtaSec = configuration.getCurrentTarget().getDirectEtaSec();
        var interceptEtaText = interceptEtaSec < 0 ? "---" : DurationFormatUtils.formatDuration(interceptEtaSec * 1000, ETA_FORMAT);
        var directEtaText = directEtaSec < 0 ? "---" : DurationFormatUtils.formatDuration(directEtaSec * 1000, ETA_FORMAT);
        var text = target.getName() + "\n" +
                location + "\n" +
                dir + " " + range + " " + bearing + "\n" +
                "ETA: " + directEtaText + "\n" +
                "ETA Int: " + interceptEtaText;
        textWidget.setText(text);
    }

    private class WidgetClickListener implements IMapWidget.OnClickListener
    {
        @Override
        public void onMapWidgetClick(IMapWidget widget, MotionEvent event)
        {
            if (configuration != null) globe.goTo(configuration.getCurrentTarget().getTarget().getLocation());
        }
    }
}
