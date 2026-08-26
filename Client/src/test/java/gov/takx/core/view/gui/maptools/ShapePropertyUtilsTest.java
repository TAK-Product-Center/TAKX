package gov.takx.core.view.gui.maptools;

import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.shapes.IMutableRectangle;
import gov.takx.api.shapes.IMutableSquare;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class ShapePropertyUtilsTest
{
    @Test
    public void testShape2D()
    {
        IMutableSquare square = MapToolType.DRAWING_SQUARE.getShapeType().createEmptyModel();
        IMutableRectangle rectangle = MapToolType.DRAWING_QUAD.getShapeType().createEmptyModel();

        assertThat(ShapePropertyUtils.isSquareShape(square)).isTrue();
        assertThat(ShapePropertyUtils.isRectangleShape(rectangle)).isTrue();
        assertThat(ShapePropertyUtils.isRectangleShape(square)).isFalse();

        assertThat(ShapePropertyUtils.shapeHasInterior(MapToolType.DRAWING_CIRCLE)).isTrue();
        assertThat(ShapePropertyUtils.shapeHasInterior(MapToolType.DRAWING_ELLIPSE)).isTrue();
        assertThat(ShapePropertyUtils.shapeHasInterior(MapToolType.DRAWING_POLYGON)).isTrue();
        assertThat(ShapePropertyUtils.shapeHasInterior(MapToolType.DRAWING_QUAD)).isTrue();
        assertThat(ShapePropertyUtils.shapeHasInterior(MapToolType.DRAWING_SQUARE)).isTrue();

        assertThat(ShapePropertyUtils.shapeHasInterior(MapToolType.DRAWING_ARROW)).isFalse();
        assertThat(ShapePropertyUtils.shapeHasInterior(MapToolType.DRAWING_PATH)).isFalse();
        assertThat(ShapePropertyUtils.shapeHasInterior(MapToolType.DRAWING_ROUTE)).isFalse();

        assertThat(ShapePropertyUtils.supportsOrientation(MapToolType.DRAWING_CIRCLE)).isTrue();
        assertThat(ShapePropertyUtils.supportsOrientation(MapToolType.DRAWING_ELLIPSE)).isTrue();
        assertThat(ShapePropertyUtils.supportsOrientation(MapToolType.DRAWING_QUAD)).isTrue();
        assertThat(ShapePropertyUtils.supportsOrientation(MapToolType.DRAWING_SQUARE)).isTrue();

        assertThat(ShapePropertyUtils.supportsOrientation(MapToolType.DRAWING_ARROW)).isFalse();
        assertThat(ShapePropertyUtils.supportsOrientation(MapToolType.DRAWING_PATH)).isFalse();
        assertThat(ShapePropertyUtils.supportsOrientation(MapToolType.DRAWING_POLYGON)).isFalse();
        assertThat(ShapePropertyUtils.supportsOrientation(MapToolType.DRAWING_ROUTE)).isFalse();
    }
}
