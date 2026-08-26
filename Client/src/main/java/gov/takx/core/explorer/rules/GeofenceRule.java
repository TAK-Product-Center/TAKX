package gov.takx.core.explorer.rules;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.explorer.AExplorableRule;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorableRuleFactory;
import gov.takx.api.explorer.IExplorableRuleViewComponent;
import gov.takx.api.explorer.IGeofenceRule;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.shapes.ICircle;
import gov.takx.api.shapes.IPolygon;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.IShapeServiceListener;
import gov.takx.api.state.IRestorableMap;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.view.gui.explorer.rules.GeofenceRulePanel;
import gov.takx.dataservices.shapes.PublishedShapeTracker;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.naming.OperationNotSupportedException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Explorable rule for matching explorables contained or not contained within a specific geofence.
 *
 * @since 1.48
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class GeofenceRule extends AExplorableRule implements IGeofenceRule
{
    public static final String TYPE = "Geofence Rule";

    private static final String SELECTED_SHAPE_KEY = "selectedShape";
    private static final String INSIDE_MODE_KEY = "insideMode";

    @Inject
    private IGlobeManager globeManager;

    @Inject
    private PublishedShapeTracker publishedShapeTracker;

    private final AtomicReference<IShape> shape;
    private final AtomicBoolean insideMode;
    private IGlobeDelegate primaryGlobeDelegate;

    public GeofenceRule(String name)
    {
        super(name, TYPE);
        shape = new AtomicReference<>(null);
        insideMode = new AtomicBoolean(true);
    }

    /**
     * Initialize globe delegate once globe manager has been injected.
     *
     * @since 2.31
     */
    @PostConstruct
    private void postConstruct()
    {
        primaryGlobeDelegate = globeManager.getPrimaryGlobeDelegate();
    }

    @Override
    public boolean isMatch(IExplorable explorable)
    {
        IShape selectedShape = getSelectedShape();
        if (selectedShape == null)
        {
            return false;
        }

        // Shapes should not match against themselves.
        if (selectedShape.getKey().equals(explorable.getUniqueKey()))
        {
            return false;
        }

        // Some explorables don't have a location, so they won't match
        if (!explorable.isGeolocated())
        {
            return false;
        }

        // The globe is used to perform the geofence comparison so don't attempt the check if the globe isn't ready
        if (!primaryGlobeDelegate.isGlobeComponentReady())
        {
            return false;
        }

        try
        {
            boolean contained = selectedShape.contains(explorable.getLocation());
            return isInsideMode() == contained;
        } catch (OperationNotSupportedException e)
        {
            return false;
        }
    }

    @Nonnull
    @Override
    public IExplorableRuleViewComponent createViewComponent()
    {
        return new GeofenceRulePanel(this, getSelectableValues(), getSelectedShape(), isInsideMode());
    }

    @Override
    public void store(IRestorableMap map)
    {
        super.store(map);
        IShape selectedShape = getSelectedShape();
        map.setString(SELECTED_SHAPE_KEY, selectedShape != null ? selectedShape.getKey() : "");
        map.setBoolean(INSIDE_MODE_KEY, isInsideMode());
    }

    @Override
    public void restore(IRestorableMap map)
    {
        super.restore(map);
        setInsideMode(map.getBoolean(INSIDE_MODE_KEY, isInsideMode()));
        String selectedShapeKey = map.getString(SELECTED_SHAPE_KEY, null);

        if (!StringUtils.isEmpty(selectedShapeKey))
        {
            // Because the plugins get started after the services, attempting to get the shape for the key here will
            // fail. Instead, add a listener so we can set the selected shape once it's added during plugin startup.
            publishedShapeTracker.addShapeServiceListener(new ShapeListener(selectedShapeKey));
        }
    }

    @Override
    public void setSelectedShape(IShape shape)
    {
        this.shape.set(shape);
        fireRuleChanged();
    }

    @Override
    public IShape getSelectedShape()
    {
        return shape.get();
    }

    @Override
    public void setInsideMode(boolean insideMode)
    {
        this.insideMode.set(insideMode);
        fireRuleChanged();
    }

    @Override
    public boolean isInsideMode()
    {
        return insideMode.get();
    }

    protected List<IShape> getSelectableValues()
    {
        List<IShape> usableShapes = new ArrayList<>();
        for (IShape shape : publishedShapeTracker.getShapes())
        {
            // The implementation of Ellipse.contains() is screwy, so don't use it
            if (shape instanceof IRectangle || shape instanceof IPolygon || shape instanceof ICircle)
            {
                usableShapes.add(shape);
            }
        }
        return usableShapes;
    }

    /**
     * Create an explorable rule factory for GeofenceRule.
     *
     * @return explorable rule factory for GeofenceRule.
     */
    public static IExplorableRuleFactory<IGeofenceRule> getFactory()
    {
        return new IExplorableRuleFactory<>()
        {
            @Override
            public IGeofenceRule createRule()
            {
                return new GeofenceRule(getDisplayName());
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
     * Shape service listener which will take care of resolving the selected shape key to a shape once that shape has
     * been restored.
     *
     * @since 2.31
     */
    private class ShapeListener implements IShapeServiceListener
    {
        private final String selectedShapeKey;

        private ShapeListener(String selectedShapeKey)
        {
            this.selectedShapeKey = selectedShapeKey;
        }

        @Override
        public void onShapePublished(IShape shape)
        {
            if (shape.getKey().equals(selectedShapeKey))
            {
                setSelectedShape(shape);
                publishedShapeTracker.removeShapeServiceListener(this);
            }
        }
    }
}
