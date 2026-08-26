package gov.takx.core.globe;

import gov.takx.api.globe.IGlobeAnnotationMapObject;
import gov.takx.core.globe.editor.AGlobeMapObjectEditor;
import gov.takx.core.globe.tak.editor.ControlPointRenderer;
import gov.takx.core.globe.tak.editor.GlobeAnnotationMapObjectEditorSource;
import gov.takx.core.globe.tak.editor.IShapeEditor;
import gov.takx.core.globe.tak.editor.ShapeEditor;
import gov.takx.core.globe.tak.editor.SinglePointControlPointController;

/**
 */
public class GlobeAnnotationEditor extends AGlobeMapObjectEditor<IGlobeAnnotationMapObject>
{
    protected IShapeEditor shapeEditor;

    /**
     * Create an annotation editor.
     */
    GlobeAnnotationEditor()
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
        shapeEditor = new ShapeEditor(new GlobeAnnotationMapObjectEditorSource(globeMapObject), new SinglePointControlPointController(), new ControlPointRenderer());
        shapeEditor.setEnabled(true);
        shapeEditor.setArmed(creatingNewShape);
    }

    @Override
    protected boolean editorCreated()
    {
        return shapeEditor != null;
    }
}
