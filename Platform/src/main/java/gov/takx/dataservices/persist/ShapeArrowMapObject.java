package gov.takx.dataservices.persist;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeArrowMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.shapes.IMutableArrow;
import gov.takx.api.shapes.ShapeProperties;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

import java.beans.PropertyChangeEvent;
import java.util.List;

/**
 * Configuration of an arrow shape.
 *
 * @since 3.0
 */
@PseudoBean
@Entity
public class ShapeArrowMapObject extends ShapePathMapObject<IMutableArrow> implements IShapeArrowMapObject
{
    // Supports linking the range of this shape to a given map object
    @Transient
    protected IMapObject rangeAnchor;

    @Transient
    private LocationMonitor rangeAnchorLocationMonitor;

    protected Integer rangeAnchorId;

    // Supports linking the start of this shape to a given map object
    @Transient
    protected IMapObject startAnchor;

    @Transient
    private LocationMonitor startAnchorLocationMonitor;

    protected Integer startAnchorId;

    private boolean anchored = false;

    @Transient
    private boolean updatingAnchors = false;

    @Transient
    private boolean updatingVerticesFromAnchor = false;

    @Transient
    @Inject
    private ICoreExecutorService executorService;

    @SuppressWarnings("unused")
    protected ShapeArrowMapObject()
    {
    }

    @SuppressWarnings("unused")
    public ShapeArrowMapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(mapObjectSpec, pluginDefinition);

        setCotType(CotTypes.SHAPE_ARROW);
    }

    @Override
    public void onAllLoaded()
    {
        super.onAllLoaded();

        relinkMapObject(rangeAnchorId, "range anchor", this::setRangeAnchor);
        relinkMapObject(startAnchorId, "start anchor", this::setStartAnchor);
    }

    @Override
    public double getBearing()
    {
        return shape.getBearing();
    }

    @Override
    public void setBearing(double bearing)
    {
        shape.setBearing(bearing);
    }

    @Override
    public double getRange()
    {
        return shape.getRange();
    }

    @Override
    public boolean isDynamic()
    {
        return false;
    }

    @Override
    public void setDynamic(boolean dynamic)
    {

    }

    @Override
    public void setRange(double range)
    {
        shape.setRange(range);
    }

    @Override
    public void setStartingPoint(@Nonnull ILocation startingPoint)
    {
        shape.setStartingPoint(startingPoint);
    }

    @Override
    public void setVertices(@Nonnull List<? extends ILocation> points) throws IllegalArgumentException
    {
        shape.setVertices(points);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        super.propertyChange(evt);

        if (ShapeProperties.LOCATIONS.equals(evt.getPropertyName())) updateAnchorLocations();
    }

    /**
     * Update the location of the start/range anchor Map Objects (if they are set) according to our vertices.
     * @since 3.3
     */
    private void updateAnchorLocations()
    {
        if (!updatingVerticesFromAnchor)
        {
            final List<ILocation> vertices = shape.getVertices();
            final ILocation startingPoint = vertices.get(0);
            final ILocation endingPoint = vertices.get(1);

            updatingAnchors = true;

            if (startAnchor != null && !startAnchor.getLocation().equals(startingPoint))
            {
                startAnchor.getController().sendLocationChangeMessage(startingPoint);
            }

            if (rangeAnchor != null && !rangeAnchor.getLocation().equals(endingPoint))
            {
                rangeAnchor.getController().sendLocationChangeMessage(endingPoint);
            }

            updatingAnchors = false;
        }
    }

    @Override
    public void setRangeAnchor(@Nullable IMapObject rangeAnchor) throws IllegalArgumentException
    {
        checkAnchorAndAnchored(rangeAnchor, "Range");

        final IMapObject oldAnchor = this.rangeAnchor;

        if (this.rangeAnchor != null)
        {
            rangeAnchorLocationMonitor.dispose();
            rangeAnchorLocationMonitor = null;
            rangeAnchorId = null;
        }

        this.rangeAnchor = rangeAnchor;

        if (rangeAnchor != null)
        {
            rangeAnchorId = rangeAnchor.getId();

            setVertices(List.of(getVertices().get(0), rangeAnchor.getLocation()));

            // Track its location so we can adjust our range & bearing if/when it is moved
            rangeAnchorLocationMonitor = new LocationMonitor(rangeAnchor,
                    endPoint -> updateVerticesFromAnchor(List.of(getVertices().get(0), endPoint)));
        }

        if (oldAnchor != rangeAnchor)
        {
            propertyChangeSupport.firePropertyChange(ShapeProperties.RANGE_ANCHOR,
                    oldAnchor == null ? null : oldAnchor.getKey(),
                    rangeAnchor == null ? null : rangeAnchor.getKey());
        }
    }

    @Nullable
    @Override
    public IMapObject getRangeAnchor()
    {
        return rangeAnchor;
    }

    @Override
    public void setStartAnchor(@Nullable IMapObject startAnchor) throws IllegalArgumentException
    {
        checkAnchorAndAnchored(startAnchor, "Start");

        final IMapObject oldAnchor = this.startAnchor;

        if (this.startAnchor != null)
        {
            startAnchorLocationMonitor.dispose();
            startAnchorLocationMonitor = null;
            startAnchorId = null;
        }

        this.startAnchor = startAnchor;

        if (startAnchor != null)
        {
            startAnchorId = startAnchor.getId();

            shape.setStartingPoint(startAnchor.getLocation());

            // Track its location so we can adjust our range & bearing if/when it is moved
            startAnchorLocationMonitor = new LocationMonitor(startAnchor,
                    startPoint -> updateVerticesFromAnchor(List.of(startPoint, getVertices().get(1))));
        }

        if (oldAnchor != startAnchor)
        {
            propertyChangeSupport.firePropertyChange(ShapeProperties.START_ANCHOR,
                    oldAnchor == null ? null : oldAnchor.getKey(),
                    startAnchor == null ? null : startAnchor.getKey());
        }
    }

    @Nullable
    @Override
    public IMapObject getStartAnchor()
    {
        return startAnchor;
    }

    /**
     * Update our vertices, unless we are in the process of updating our anchor locations.
     * @since 3.3
     */
    private void updateVerticesFromAnchor(@Nonnull List<? extends ILocation> vertices)
    {
        if (!updatingVerticesFromAnchor) // If we are updating the vertices due to an anchor location change, we don't want to reciprocally update the anchors
        {
            updatingVerticesFromAnchor = true;
            setVertices(vertices);
            updatingVerticesFromAnchor = false;
        }
    }

    /**
     * Verify that the given anchor and anchored settings align.  If the anchor is non-null, verify that it
     * has a valid location.
     *
     * @throws IllegalArgumentException If preconditions are not met
     */
    private void checkAnchorAndAnchored(@Nullable IMapObject anchor, String anchorName)
    {
        if (anchor == null && anchored)
        {
            throw new IllegalArgumentException(anchorName + " anchor cannot be removed from arrow that is anchored");
        }

        if (anchor != null && !anchor.getLocation().isValid())
        {
            throw new IllegalArgumentException(anchorName + " anchor must have a valid location");
        }
    }

    @Override
    public void setAnchored(boolean anchored)
    {
        if (anchored && (startAnchor == null || rangeAnchor == null))
        {
            throw new IllegalArgumentException("Arrow must have both anchors set; startAnchor = " +
                    startAnchor + ", rangeAnchor = " + rangeAnchor);
        }

        // Only un-anchored arrows should be exportable to CoT
        setMetaBoolean(StandardAttributeNames.NEVER_COT, anchored);

        propertyChangeSupport.updateAndFire(ShapeProperties.ANCHORED, this.anchored, anchored,
                () -> this.anchored = anchored);
    }

    @Override
    public boolean isAnchored()
    {
        return anchored;
    }

    @SuppressWarnings("JpaAttributeMemberSignatureInspection")
    @Override
    public String getBearingLabelText()
    {
        return shape.getBearingLabelText();
    }

    @SuppressWarnings("JpaAttributeMemberSignatureInspection")
    @Override
    public String getRangeLabelText()
    {
        return shape.getRangeLabelText();
    }

    @Override
    public boolean isRangeLocked()
    {
        return shape.isRangeLocked();
    }

    @Override
    public void setRangeLocked(boolean rangeLocked)
    {
        shape.setRangeLocked(rangeLocked);
    }

    /**
     * Handles notification of a map object being removed to determine if this object must be deleted too.
     */
    @SuppressWarnings("unused")
    private void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        final IMapObject removedMapObject = event.getMapObject();
        // If this MO is dependent on having both anchors and one of them is deleted, this MO must be deleted too
        if (anchored)
        {
            if (removedMapObject.equals(startAnchor) || removedMapObject.equals(rangeAnchor))
            {
                // If we delete it on this thread, we can get into an infinite loop, so push off to the executor service
                executorService.execute(() -> {
                    Thread.currentThread().setName("Delete anchored arrow: " + getName());
                    mapObjectManager.deleteMapObject(this);
                });
            }
        } else
        {
            if (removedMapObject.equals(startAnchor))
            {
                setStartAnchor(null);
            } else if (removedMapObject.equals(rangeAnchor))
            {
                setRangeAnchor(null);
            }
        }
    }

    @RemoveObserver
    @Override
    public void shutdown()
    {
        if (startAnchorLocationMonitor != null)
        {
            startAnchorLocationMonitor.dispose();
            startAnchorLocationMonitor = null;
        }

        if (rangeAnchorLocationMonitor != null)
        {
            rangeAnchorLocationMonitor.dispose();
            rangeAnchorLocationMonitor = null;
        }

        super.shutdown();
    }
}
