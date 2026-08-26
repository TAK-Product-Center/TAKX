package gov.takx.core.explorer.controller.path;

import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorablePathProvider;
import gov.takx.api.explorer.IMapObjectExplorable;
import gov.takx.api.mapobject.IAnnotationMapObject;
import gov.takx.api.mapobject.IFlatAnnotationMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

import static gov.takx.core.explorer.controller.path.ExplorerPathConstants.ANNOTATIONS_PATH;

/**
 * Implementation of {@link IExplorablePathProvider} that handles any {@link IMapObjectExplorable} that contains
 * {@link IAnnotationMapObject} or {@link IFlatAnnotationMapObject} map objects.
 *
 * @since 5.3
 */
public class AnnotationMapObjectPathProvider implements IExplorablePathProvider
{
    @Override
    @Nullable
    public List<String> createDefaultPath(@Nonnull IExplorable explorable)
    {
        if (explorable instanceof IMapObjectExplorable mapObjectExplorable && (mapObjectExplorable.getMapObject() instanceof IAnnotationMapObject || mapObjectExplorable.getMapObject() instanceof IFlatAnnotationMapObject))
        {
            // DCW I suspect there is more complexity needed here soon, so I'm leaving the full if/else block
            return ANNOTATIONS_PATH;
        } else
        {
            return null;
        }
    }
}