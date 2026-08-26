package gov.takx.core.explorer.rules;

import gov.nasa.worldwind.geom.LatLon;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.explorer.AExplorableRule;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorableRuleFactory;
import gov.takx.api.explorer.IExplorableRuleViewComponent;
import gov.takx.api.explorer.IExplorerDelegateListener;
import gov.takx.api.explorer.IProximityRule;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.api.state.IRestorableMap;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.explorer.IExplorerManager;
import gov.takx.core.globe.utils.GlobeRuler;
import gov.takx.core.view.gui.explorer.rules.ProximityRulePanel;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Explorable rule for proximity matching. A locatable explorable is selected and any other locatable explorables within
 * a given radius are positively matched.
 *
 * @since 1.48
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class ProximityRule extends AExplorableRule implements IProximityRule
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String TYPE = "Proximity";

    private static final Set<String> SENSITIVE_PROPERTIES = Set.of(IMapObject.LOCATION, ShapeProperties.LOCATIONS);
    private static final String SELECTED_EXPLORABLE_KEY = "selectedExplorable";
    private static final String MAX_DISTANCE_KEY = "maxDistance";
    private final AtomicReference<IExplorable> selectedExplorable;
    private final AtomicInteger maxDistanceMeters;

    @Inject
    private IExplorerManager explorerManager;

    public ProximityRule(String name)
    {
        super(name, TYPE);
        selectedExplorable = new AtomicReference<>(null);
        maxDistanceMeters = new AtomicInteger(1000);
    }

    @Override
    public boolean isMatch(IExplorable explorable)
    {
        IExplorable selected = getSelectedExplorable();
        if (selected == null || selected.equals(explorable)) return false;

        ILocation selectedLocation = selected.getLocation();
        if (selectedLocation == null) return false;

        ILocation location = explorable.getLocation();
        if (location == null) return false;

        ArrayList<? extends LatLon> latLons = new ArrayList<>(Arrays.asList(
                LatLon.fromDegrees(selectedLocation.getLatitude(), selectedLocation.getLongitude()),
                LatLon.fromDegrees(location.getLatitude(), location.getLongitude())));
        GlobeRuler ruler = new GlobeRuler();
        ruler.setPositions(latLons, 0);
        return ruler.computeLength() <= getMaxDistanceMeters();
    }

    @Nonnull
    @Override
    public IExplorableRuleViewComponent createViewComponent()
    {
        return new ProximityRulePanel(this, getSelectedExplorable(), getLocatedExplorables(), getMaxDistanceMeters());
    }

    @Override
    public void setSelectedExplorable(IExplorable explorable)
    {
        selectedExplorable.set(explorable);
        fireRuleChanged();
    }

    @Override
    public IExplorable getSelectedExplorable()
    {
        return selectedExplorable.get();
    }

    @Override
    public void setMaxDistanceMeters(int maxDistanceMeters)
    {
        this.maxDistanceMeters.set(maxDistanceMeters);
        fireRuleChanged();
    }

    @Override
    public int getMaxDistanceMeters()
    {
        return maxDistanceMeters.get();
    }

    @Nonnull
    @Override
    public Collection<String> getSensitiveProperties()
    {
        return SENSITIVE_PROPERTIES;
    }

    @Override
    public void store(IRestorableMap map)
    {
        super.store(map);
        IExplorable selectedExplorable = getSelectedExplorable();
        map.setString(SELECTED_EXPLORABLE_KEY, selectedExplorable != null ? selectedExplorable.getUniqueKey() : "");
        map.setInt(MAX_DISTANCE_KEY, maxDistanceMeters.get());
    }

    @Override
    public void restore(IRestorableMap map)
    {
        super.restore(map);
        setMaxDistanceMeters(map.getInt(MAX_DISTANCE_KEY, getMaxDistanceMeters()));
        String selectedExplorableKey = map.getString(SELECTED_EXPLORABLE_KEY, null);

        if (!StringUtils.isEmpty(selectedExplorableKey))
        {
            if (logger.isDebugEnabled()) logger.debug("Restore waiting for explorable to be recreated: {}", selectedExplorableKey);

            // Because the plugins get started after the services, attempting to get the explorable for the key here
            // would fail. Instead, add a listener so we can set the selected explorable once it's added during plugin
            // startup.
            explorerManager.getDefaultExplorerDelegate().addExplorerDelegateListener(new ExplorerDelegateListener(selectedExplorableKey));
        }
    }

    protected List<IExplorable> getLocatedExplorables()
    {
        List<IExplorable> locatedExplorables = new ArrayList<>();
        Collection<IExplorable> explorables = explorerManager.getDefaultExplorerDelegate().getController().getExplorables();
        for (IExplorable explorable : explorables)
        {
            if (explorable.isGeolocated()) locatedExplorables.add(explorable);
        }

        locatedExplorables.sort(Comparator.comparing(IExplorable::getDisplayName));
        return locatedExplorables;
    }

    /**
     * Create an explorable rule factory for ProximityRule.
     *
     * @return explorable rule factory for ProximityRule.
     */
    public static IExplorableRuleFactory<IProximityRule> getFactory()
    {
        return new IExplorableRuleFactory<>()
        {
            @Override
            public ProximityRule createRule()
            {
                return new ProximityRule(TYPE);
            }

            @Override
            public String getDisplayName()
            {
                return TYPE;
            }

            @Override
            public String getType()
            {
                return TYPE;
            }
        };
    }

    /**
     * Explorer delegate listener which will take care of resolving the selected explorable key to an explorable once
     * that explorable has been restored.
     *
     * @since 2.31
     */
    private class ExplorerDelegateListener implements IExplorerDelegateListener
    {
        private final String selectedExplorableKey;

        private ExplorerDelegateListener(String selectedExplorableKey)
        {
            this.selectedExplorableKey = selectedExplorableKey;
        }

        @Override
        public void onExplorerEvent(String eventName, IExplorable affectedExplorable, Object oldValue, Object newValue,
                                    boolean isGroupTreeSource)
        {
            if (eventName.equals(EXPLORABLE_ADDED_EVENT) && affectedExplorable.getUniqueKey().equals(selectedExplorableKey))
            {
                if (logger.isDebugEnabled()) logger.debug("Explorable recreated (restore complete): {}", affectedExplorable);

                setSelectedExplorable(affectedExplorable);
                explorerManager.getDefaultExplorerDelegate().removeExplorerDelegateListener(this);
            }
        }
    }
}
