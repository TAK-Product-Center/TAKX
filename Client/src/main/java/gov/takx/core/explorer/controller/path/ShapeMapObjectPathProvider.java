package gov.takx.core.explorer.controller.path;

import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.explorer.ExplorerUtils;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorablePathProvider;
import gov.takx.api.explorer.IMapObjectExplorable;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Implementation of {@link IExplorablePathProvider} that handles any {@link IMapObjectExplorable} that
 * contains an {@link IShapeMapObject}.
 *
 * @since 5.3
 */
public class ShapeMapObjectPathProvider implements IExplorablePathProvider
{

    @Override
    @Nullable
    public List<String> createDefaultPath(@Nonnull IExplorable explorable)
    {
        if (explorable instanceof IMapObjectExplorable mapObjectExplorable && mapObjectExplorable.getMapObject() instanceof IShapeMapObject<?> shape)
        {
            if (shape instanceof IRouteMapObject)
            {
                return ExplorerPathConstants.ROUTES_PATH;
            } else if (shape.getMetaBoolean(StandardAttributeNames.RANGE_BEARING, false))
            {
                return ExplorerPathConstants.RANGE_AND_BEARING_PATH;
            } else
            {
                return ExplorerUtils.extendPath(ExplorerPathConstants.SHAPES_DRAWING_OBJECTS_PATH, shape.getAssociatedShape().getType());
            }
        } else
        {
            return null;
        }
    }
}