package gov.takx.core.globe;

import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.EditorFailedEvent;
import gov.takx.api.event.FinalizeShapeEvent;
import gov.takx.api.event.UserShapeCreatedEvent;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.api.globe.IGlobeMapObjectEditor;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.globe.editor.AGlobeShapeEditor;
import gov.takx.core.globe.tak.TakLayerManager;
import gov.takx.core.globe.tak.editor.ArrowControlPointController;
import gov.takx.core.globe.tak.editor.CircleControlPointController;
import gov.takx.core.globe.tak.editor.CompositeRenderer;
import gov.takx.core.globe.tak.editor.ControlPointRenderer;
import gov.takx.core.globe.tak.editor.FreehandControlPointController;
import gov.takx.core.globe.tak.editor.GlobeArrowMapObjectEditorSource;
import gov.takx.core.globe.tak.editor.GlobeBullseyeMapObjectEditorSource;
import gov.takx.core.globe.tak.editor.GlobeCircleMapObjectEditorSource;
import gov.takx.core.globe.tak.editor.GlobeEllipseMapObjectEditorSource;
import gov.takx.core.globe.tak.editor.GlobePolygonMapObjectEditorSource;
import gov.takx.core.globe.tak.editor.GlobePolylineMapObjectEditorSource;
import gov.takx.core.globe.tak.editor.GlobeRangeFanMapObjectEditorSource;
import gov.takx.core.globe.tak.editor.GlobeRectangleMapObjectEditorSource;
import gov.takx.core.globe.tak.editor.GlobeRouteMapObjectEditorSource;
import gov.takx.core.globe.tak.editor.GlobeShapeMapObjectRenderer;
import gov.takx.core.globe.tak.editor.GlobeSquareMapObjectEditorSource;
import gov.takx.core.globe.tak.editor.IShapeEditor;
import gov.takx.core.globe.tak.editor.PointAndClickControlPointController;
import gov.takx.core.globe.tak.editor.RectangleControlPointController;
import gov.takx.core.globe.tak.editor.ReticleControlPointRenderer;
import gov.takx.core.globe.tak.editor.RouteControlPointController;
import gov.takx.core.globe.tak.editor.RouteControlPointRenderer;
import gov.takx.core.globe.tak.editor.ShapeEditor;
import gov.takx.core.globe.tak.editor.ShapeEditorEventAdapter;
import gov.takx.core.globe.tak.editor.SinglePointControlPointController;
import gov.takx.platform.KeyUtils;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * GlobeShapeMapObjectEditor is an implementation of {@link IGlobeMapObjectEditor} which accepts
 * and edits references to {@link IGlobeShapeMapObject}.
 * <p/>
 * Implementation note: this uses an internal {@link IShapeEditor} for the presentation and
 * handling of a user-interface based editor components.
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class GlobeShapeMapObjectEditor extends AGlobeShapeEditor
{
    @Inject
    private TakLayerManager takLayerManager;

    @Inject
    protected IMapObjectManager mapObjectManager;

    @Inject
    @PlatformUnderTest
    private boolean underTest;

    private final boolean drawOnly;
    protected IShapeEditor shapeEditor;
    private boolean clearingEditor;

    /**
     * Constructs a new GlobeShapeMapObjectEditor which displays its editor on the specified World Window. The new
     * editor has no associated IGlobeShapeMapObject
     */
    public GlobeShapeMapObjectEditor(boolean drawOnly)
    {
        this.drawOnly = drawOnly;
    }

    @Override
    protected boolean editorCreated()
    {
        return shapeEditor != null;
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

    protected IShapeEditor createShapeEditor()
    {
        CompositeRenderer renderer = new CompositeRenderer(new GlobeShapeMapObjectRenderer(), new ControlPointRenderer());
        return switch (globeMapObject.getMapToolType())
        {
            case DRAWING_ARROW -> new ShapeEditor(new GlobeArrowMapObjectEditorSource(globeMapObject), new ArrowControlPointController(), new CompositeRenderer(new GlobeShapeMapObjectRenderer(), new ReticleControlPointRenderer()));
            case DRAWING_POLYGON -> new ShapeEditor(new GlobePolygonMapObjectEditorSource(globeMapObject), new PointAndClickControlPointController(), renderer);
            case DRAWING_FREEHAND_POLYGON -> new ShapeEditor(new GlobePolygonMapObjectEditorSource(globeMapObject), new FreehandControlPointController(), renderer);
            case DRAWING_CIRCLE -> new ShapeEditor(new GlobeCircleMapObjectEditorSource(globeMapObject), new CircleControlPointController(), renderer);
            case DRAWING_ELLIPSE -> new ShapeEditor(new GlobeEllipseMapObjectEditorSource(globeMapObject), new CircleControlPointController(), renderer);
            case DRAWING_SQUARE -> new ShapeEditor(new GlobeSquareMapObjectEditorSource(globeMapObject), new RectangleControlPointController(), renderer);
            case DRAWING_QUAD -> new ShapeEditor(new GlobeRectangleMapObjectEditorSource(globeMapObject), new RectangleControlPointController(), renderer);
            case DRAWING_PATH -> new ShapeEditor(new GlobePolylineMapObjectEditorSource(globeMapObject), new PointAndClickControlPointController(), renderer);
            case DRAWING_FREEHAND_PATH -> new ShapeEditor(new GlobePolylineMapObjectEditorSource(globeMapObject), new FreehandControlPointController(), renderer);
            case DRAWING_ROUTE -> new ShapeEditor(new GlobeRouteMapObjectEditorSource(globeMapObject), new RouteControlPointController(), new CompositeRenderer(new GlobeShapeMapObjectRenderer(), new RouteControlPointRenderer()));
            case DRAWING_RANGE_FAN -> new ShapeEditor(new GlobeRangeFanMapObjectEditorSource(globeMapObject), new SinglePointControlPointController(), renderer);
            case DRAWING_BULLSEYE -> new ShapeEditor(new GlobeBullseyeMapObjectEditorSource(globeMapObject), new PointAndClickControlPointController(), renderer);
            case DRAWING_ANNOTATION -> throw new IllegalArgumentException("Attempt to create drawing tool from Annotation map tool type");
            case DRAWING_FLAT_ANNOTATION -> throw new IllegalArgumentException("Attempt to create drawing tool from Flat Annotation map tool type");
            case NONE -> throw new IllegalArgumentException("Attempt to create drawing tool from NONE map tool type");
        };
    }

    @Override
    protected void createEditor()
    {
        shapeEditor = createShapeEditor();
        shapeEditor.setEnabled(true);
        shapeEditor.setArmed(creatingNewShape);
        shapeEditor.addShapeEditorEventListener(
                new ShapeEditorEventAdapter()
                {
                    @Override
                    public void onDisarm()
                    {
                        if (underTest) return;
                        if (shapeEditor.getShapeEditorSource().getSource() instanceof IGlobeShapeMapObject globeShapeMapObject)
                        {
                            IMutableShape shapeModel = globeShapeMapObject.getShapeModel();
                            globeManager.getPrimaryGlobeDelegate().getGlobeMapObjectManager().setMapTool(MapToolType.NONE);
                            mapObjectManager.setSelected(null);
                            if (shapeModel != null)
                            {
                                final String key = globeShapeMapObject.requireShapeModel().getKey();
                                mapObjectManager.setSelected(key);
                            }
                        }
                        super.onDisarm();
                    }
                });
    }

    @Override
    public void setGlobeMapObject(IGlobeMapObject globeMapObject, boolean creatingNewShape)
    {
        super.setGlobeMapObject(globeMapObject, creatingNewShape);
        if (creatingNewShape && globeMapObject instanceof IGlobeShapeMapObject globeShapeMapObject)
        {
            final MapToolType mapToolType = globeShapeMapObject.getMapToolType();
            final String mapToolString = mapToolType.toString();

            final IMutableShape shapeModel = mapToolType.getShapeType().createEmptyModel();
            final String key = KeyUtils.getUniqueKey(KeyUtils.TOOL_FAMILY, mapToolString, UUID.randomUUID().toString());
            shapeModel.setKey(key);

            globeShapeMapObject.setShapeModel(shapeModel);
        }
    }

    @Override
    protected boolean applyUpdatesToModel()
    {
        if (clearingEditor) return false;

        final IMutableShape model = getGlobeMapObject().getShapeModel();
        if (!shapeEditor.applyChangesToSource())
        {
            // When an editor is being disarmed, and we're creating a new shape and there have been no
            // control points added, then we should NOT report this, allowing the user to switch between
            // drawing tools without being annoyed by an "incomplete shape" dialog.
            final boolean shouldReportError = !creatingNewShape || shapeEditor.getShapeEditorSource().hasControlPoints();

            clearingEditor = true; // Prevent recursive invocation when the editor is disposed
            disposeEditor();
            clearingEditor = false;

            if (shouldReportError) showIncompletePolyShapeDialog();

            String key = model == null ? null : model.getKey();
            uiEventBus.post(new EditorFailedEvent(key));
            return false;
        } else
        {
            uiEventBus.post(new FinalizeShapeEvent());
            uiEventBus.post(new UserShapeCreatedEvent(model));
            return !drawOnly; // returning false will prevent MO creation in draw only mode
        }
    }

    protected void showIncompletePolyShapeDialog()
    {
        if (underTest) return;

        final var item = globeMapObject != null ? globeMapObject.getMapToolType() : "shape";
        final var msg = "Insufficient control points to define the " + item + ".\n\nRemoving partial shape from the globe.";

        AlwaysOnTopJOptionPane.showErrorDialog(msg, "Invalid " + item);
    }
}
