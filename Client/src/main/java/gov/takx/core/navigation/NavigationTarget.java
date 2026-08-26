package gov.takx.core.navigation;

import gov.tak.api.maps.PointMapItem;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.IPointMapObject;
import gov.takx.api.mapobject.IShapeArrowMapObject;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.lang.IPropertyChangeDelegateSupport;
import gov.takx.commons.lang.IPropertyChangeListenerSupport;
import gov.takx.commons.lang.ListenerManager;
import gov.takx.platform.utils.PrioritizedPropertyChangeSupport;
import gov.takx.platform.utils.ShapeUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static gov.takx.core.navigation.NavigationConfiguration.ETA;

/**
 * Represents a single target within a {@link NavigationConfiguration}. Tracks the intercept point, if applicable, and the R&B arrows
 * that track the distance and bearing.
 *
 * @since 5.7
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class NavigationTarget implements IPropertyChangeDelegateSupport, IResourceCleaner
{
    private static final List<String> UPDATE_INTERCEPT_PROPERTIES = List.of(IMapObject.NAME, IMapObject.ICON);
    private static final List<String> UPDATE_PROPERTIES = List.of(IMapObject.NAME);

    private final PrioritizedPropertyChangeSupport propertyChangeSupport = new PrioritizedPropertyChangeSupport(this);
    private final ListenerManager listenerManager = new ListenerManager();
    private final List<IMapObject> mapObjects = new ArrayList<>();
    private final NavigationConfiguration navigation;
    private final IPointMapObject target;
    private final IShapeArrowMapObject targetArrow;
    private final IMarkerMapObject intercept;
    private final IShapeArrowMapObject interceptArrow;

    @Inject
    private NavigationManager navigationManager;

    private Color lineColor;
    private boolean visible;
    private double directEtaSec;
    private double interceptEtaSec;

    /**
     * Construct a new instance that tracks navigational data between the given target and the source object of the given
     * {@link NavigationConfiguration}.
     *
     * @param navigation the parent navigation
     * @param target the target map object
     * @param targetArrow the arrow to the target object
     */
    public NavigationTarget(@Nonnull NavigationConfiguration navigation, @Nonnull IPointMapObject target, @Nonnull IShapeArrowMapObject targetArrow)
    {
        this(navigation, target, targetArrow, null, null);
    }

    /**
     * Construct a new instance that tracks navigational data between the source object of the given {@link NavigationConfiguration} and
     * the target. Optionally track intercept data if the intercept object and arrow are present.
     *
     * @param navigation the parent navigation
     * @param target the target map object
     * @param targetArrow the arrow to the target object
     * @param intercept the intercept object
     * @param interceptArrow the arrow to the intercept object
     */
    public NavigationTarget(@Nonnull NavigationConfiguration navigation,
                            @Nonnull IPointMapObject target,
                            @Nonnull IShapeArrowMapObject targetArrow,
                            @Nullable IMarkerMapObject intercept,
                            @Nullable IShapeArrowMapObject interceptArrow)
    {
        this.navigation = navigation;
        this.target = target;
        this.targetArrow = targetArrow;
        this.intercept = intercept;
        this.interceptArrow = interceptArrow;
        var source = navigation.getSourceMapObject();

        mapObjects.add(targetArrow);

        if (intercept != null)
        {
            mapObjects.add(intercept);
            mapObjects.add(interceptArrow);
            listenerManager.addPropertyChangeListener(target, UPDATE_INTERCEPT_PROPERTIES, event -> {
                intercept.setName(target.getName());
                intercept.setIconImage(target.getIconImageURI());
            });
        }

        listenerManager.addPropertyChangeListener(source, UPDATE_PROPERTIES, ignored -> update());
        listenerManager.addPropertyChangeListener(target, UPDATE_PROPERTIES, ignored -> update());
    }

    @PostConstruct
    private void postConstruct()
    {
        update();
    }

    @Override
    @Nonnull
    public IPropertyChangeListenerSupport getPropertyChangeListenerSupport()
    {
        return propertyChangeSupport;
    }

    @Override
    public void releaseResources()
    {
        listenerManager.unregisterAll();
    }

    /**
     * @return the navigation configuration for this target
     */
    @Nonnull
    public NavigationConfiguration getNavigation()
    {
        return navigation;
    }

    /**
     * @return the target point
     */
    @Nonnull
    public PointMapItem getTarget()
    {
        return target;
    }

    /**
     * @return the arrow going directly to the target point
     */
    @Nonnull
    public IShapeArrowMapObject getTargetArrow()
    {
        return targetArrow;
    }

    /**
     * @return the arrow going to the intercept point
     */
    @Nullable
    public IShapeArrowMapObject getInterceptArrow()
    {
        return interceptArrow;
    }

    /**
     * @return {@code true} if the source can intercept the target based on their relative velocities, otherwise {@code false}
     */
    public boolean canIntercept()
    {
        return hasIntercept() && !Double.isNaN(interceptEtaSec);
    }

    /**
     * @return the ETA to intercept the target, or {@code -1} if it is unreachable 
     */
    public long getInterceptEtaSec()
    {
        return Double.isNaN(interceptEtaSec) ? -1L : (long) interceptEtaSec;
    }

    /**
     * @return the ETA directly to the target, or {@code -1} if it is unreachable 
     */
    public long getDirectEtaSec()
    {
        return Double.isNaN(directEtaSec) ? -1L : (long) directEtaSec;
    }

    /**
     * @return the current targets intercept eta, if present, or else the direct eta
     */
    public long getInterceptOrDirectEtaSec()
    {
        return canIntercept() ? getInterceptEtaSec() : getDirectEtaSec();
    }

    /**
     * @return {@code true} if this target has an intercept arrow, otherwise {@code false}
     */
    public boolean hasIntercept()
    {
        return interceptArrow != null;
    }

    /**
     * @return all map objects that are owned or managed by this target
     */
    @Nonnull
    public List<IMapObject> getMapObjects()
    {
        return List.copyOf(mapObjects);
    }

    /**
     * @param mapObject a map object that is managed by this target instance and should be removed if the target is removed
     */
    public void addMapObject(@Nonnull IMapObject mapObject)
    {
        mapObjects.add(mapObject);
    }

    /**
     * @param visible {@code true} to set this targets map objects as visible, otherwise {@code false}
     */
    public void setVisible(boolean visible)
    {
        if (this.visible != visible)
        {
            targetArrow.setVisible(visible);
            if (hasIntercept())
            {
                intercept.setVisible(visible);
                interceptArrow.setVisible(visible);
            }
            this.visible = visible;
        }
    }

    /**
     * @param lineColor the arrow line color that should be used or {@code null} to use eta derived color
     */
    public void setLineColor(@Nullable Color lineColor)
    {
        this.lineColor = lineColor;
        if (lineColor != null)
        {
            targetArrow.setLineColor(lineColor);
            if (interceptArrow != null) interceptArrow.setLineColor(lineColor);
        }
        update();
    }

    /**
     * Update all properties of this instance.
     */
    public void update()
    {
        var to = target;
        var from = navigation.getSourceMapObject();
        var fromLocation = from.getLocation();
        var toLocation = to.getLocation();
        var fromSpeed = from.getSpeedInMps();
        if (Double.isNaN(fromSpeed)) fromSpeed = from.getMetaDouble(StandardAttributeNames.EST_SPEED, Double.NaN);
        var toSpeed = to.getSpeedInMps();
        if (Double.isNaN(toSpeed)) toSpeed = to.getMetaDouble(StandardAttributeNames.EST_SPEED, Double.NaN);
        var toHeading = to.getHeading();
        if (Double.isNaN(toHeading)) toHeading = to.getMetaDouble(StandardAttributeNames.EST_COURSE, Double.NaN);
        var directEtaSec = fromSpeed > 0.0 ? targetArrow.getRange() / fromSpeed : Double.NaN;
        var interceptEtaSec = ShapeUtils.interceptEta(fromLocation, fromSpeed, toLocation, toSpeed, toHeading);

        propertyChangeSupport.updateAndFire(ETA, this.directEtaSec, directEtaSec, () -> {
            this.directEtaSec = directEtaSec;
            this.interceptEtaSec = interceptEtaSec;
        });

        var canIntercept = canIntercept();
        targetArrow.setVisible(visible);

        if (visible)
        {
            var etaColor = navigationManager.getArrowColor(navigation.isColorEnabled() ? this.interceptEtaSec : Double.MAX_VALUE);
            if (lineColor == null) targetArrow.setLineColor(etaColor);
            if (hasIntercept())
            {
                var displayIntercept = visible && canIntercept && navigationManager.displayIntercept();
                if (lineColor == null) interceptArrow.setLineColor(etaColor);
                intercept.setVisible(displayIntercept);
                interceptArrow.setVisible(displayIntercept);

                if (displayIntercept)
                {
                    var distance = toSpeed * this.interceptEtaSec;
                    var endPoint = ShapeUtils.rhumbEndPositionMeters(toLocation, toHeading, distance);
                    intercept.setLocation(endPoint, false);
                }
            }
        }
    }
}
