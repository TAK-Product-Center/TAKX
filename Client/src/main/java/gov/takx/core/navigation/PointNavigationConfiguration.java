package gov.takx.core.navigation;

import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IPointMapObject;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * NavigationConfiguration class for navigating between two point map objects.
 *
 * @since 5.7
 */
public class PointNavigationConfiguration extends NavigationConfiguration
{
    /**
     * Construct a new navigation configuration from the first map object to the second.
     *
     * @param sourceMapObject the source map object
     * @param targetMapObject the target map object
     */
    public PointNavigationConfiguration(@Nonnull IPointMapObject sourceMapObject, @Nonnull IPointMapObject targetMapObject)
    {
        super(sourceMapObject, targetMapObject);

        var targetArrow = navigationManager.createTargetArrow(sourceMapObject, targetMapObject);
        var intercept = navigationManager.createInterceptPoint(sourceMapObject, targetMapObject);
        var interceptArrow = navigationManager.createInterceptArrow(sourceMapObject, intercept);

        var navigationTarget = new NavigationTarget(this, targetMapObject, targetArrow, intercept, interceptArrow);
        listenerManager.addPropertyChangeListener(navigationTarget, List.of(ETA), propertyChangeSupport::firePropertyChangeCopy);
        listenerManager.addPropertyChangeListener(targetMapObject, List.of(IMapObject.LOCATION), ignored -> update());

        colorEnabled = true;
        flashEnabled = true;
        targets.add(navigationTarget);
        navigationTarget.setVisible(true);
        navigationTarget.update();
        update();
    }
}
