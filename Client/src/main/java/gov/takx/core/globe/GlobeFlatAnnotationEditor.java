package gov.takx.core.globe;

import gov.takx.api.globe.IGlobeFlatAnnotationMapObject;
import gov.takx.core.globe.editor.AGlobeMapObjectEditor;
import gov.takx.core.globe.tak.editor.ControlPointRenderer;
import gov.takx.core.globe.tak.editor.GlobeFlatAnnotationMapObjectEditorSource;
import gov.takx.core.globe.tak.editor.IShapeEditor;
import gov.takx.core.globe.tak.editor.ShapeEditor;
import gov.takx.core.globe.tak.editor.SinglePointControlPointController;

/**
 * An implementation of {@link gov.takx.api.globe.IGlobeMapObjectEditor} for Flat Annotation map objects.
 *
 * @since 5.5
 */
public class GlobeFlatAnnotationEditor extends AGlobeMapObjectEditor<IGlobeFlatAnnotationMapObject>
{
    protected IShapeEditor shapeEditor;

    /**
     * Create an annotation editor.
     */
    GlobeFlatAnnotationEditor()
    {
    }

    @Override
    protected void updateMapObject()
    {
    }

    @Override
    protected void disposeEditor()
    {
        if (shapeEditor != null)
        {
            shapeEditor.dispose();
            shapeEditor = null;
        }
    }

    @Override
    protected void createEditor()
    {
        shapeEditor = new ShapeEditor(new GlobeFlatAnnotationMapObjectEditorSource(globeMapObject), new SinglePointControlPointController(), new ControlPointRenderer());
        shapeEditor.setEnabled(true);
        shapeEditor.setArmed(creatingNewShape);
    }

    @Override
    protected boolean editorCreated()
    {
        return shapeEditor != null;
    }
}
