package gov.takx.core.globe.tak.editor;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.InitializeDrawToolEvent;
import gov.takx.api.globe.IGlobeFlatAnnotationMapObject;
import gov.takx.api.mapobject.IFlatAnnotationMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.shapes.IShape.AltitudeMode;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.api.ui.event.MapObjectSelectedEvent;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;

import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link IShapeEditorSource} for {@link IGlobeFlatAnnotationMapObject} that represent a flat annotation.
 *
 * @since 5.5
 */
@PseudoBean
public class GlobeFlatAnnotationMapObjectEditorSource extends AShapeEditorSource<IGlobeFlatAnnotationMapObject>
{
    private static final List<String> PROPERTIES = List.of(ShapeProperties.EDITABLE, ShapeProperties.LOCATION);

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    @UiEventBus
    private EventBus uiEventBus;

    public GlobeFlatAnnotationMapObjectEditorSource(IGlobeFlatAnnotationMapObject source)
    {
        super(source);
    }

    @Override
    public List<ControlPointDefinition> createControlPointDefinitions()
    {
        IGlobeFlatAnnotationMapObject globeMapObject = getSource();
        if (globeMapObject.getMapObject() == null) return Collections.emptyList();
        return List.of(new ControlPointDefinition(globeMapObject.getMapObject().getLocation(), IControlPoint.CONTROL_POINT_CENTER, true, AltitudeMode.ClampToGround));
    }

    @Override
    public void onControlPointAdded(@Nonnull ILocation location, @Nonnull Map<String, Object> metadata)
    {
        final MapObjectSpec spec = MapObjectSpec.builder(KeyUtils.TOOL_FAMILY, IFlatAnnotationMapObject.FLAT_ANNOTATION_TYPE)
                .location(location)
                .initializer(mo -> uiEventBus.post(new InitializeDrawToolEvent(mo)))
                .build();

        final IMapObject mapObject = mapObjectManager.getOrCreateMapObject(spec);
        getSource().setMapObject(mapObject);

        // Force the DrawToolbarTabWrapper to deselect everything from the top down
        uiEventBus.post(new MapObjectSelectedEvent(null));

        // Select the object because this is presumably what the user wants to work on
        mapObjectManager.setSelected(mapObject.getKey());

        onSourceUpdated(getSource());
        super.onControlPointAdded(location, metadata);
    }

    @Override
    public void onControlPointDragged(@Nonnull String controlPointType, @Nonnull ILocation location)
    {
        getSource().setLocation(location);
        onSourceUpdated(getSource());
    }

    @Override
    public void applyControlPointsToSource(List<IControlPoint> controlPoints)
    {
    }

    @Override
    protected void addSourcePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        IMapObject mapObject = getSource().getMapObject();
        if (mapObject != null) mapObject.addPropertyChangeListener(PROPERTIES, listener);
    }

    @Override
    protected void removeSourcePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        IMapObject mapObject = getSource().getMapObject();
        if (mapObject != null) mapObject.removePropertyChangeListener(PROPERTIES, listener);
    }
}
