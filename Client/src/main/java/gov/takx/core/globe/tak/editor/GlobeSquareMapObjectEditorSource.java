package gov.takx.core.globe.tak.editor;

import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.dataservices.shapes.Square;

/**
 * An implementation of {@link IShapeEditorSource} for {@link IGlobeShapeMapObject} that represents a {@link Square} object.
 *
 * @since 5.7
 */
public class GlobeSquareMapObjectEditorSource extends GlobeRectangleMapObjectEditorSource
{
    public GlobeSquareMapObjectEditorSource(IGlobeShapeMapObject source)
    {
        super(source);
    }
}
