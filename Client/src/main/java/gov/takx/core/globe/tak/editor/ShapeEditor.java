package gov.takx.core.globe.tak.editor;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.globe.ApplicationGlobeManager;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Implementation of {@link IShapeEditor}.
 *
 * @since 5.4
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class ShapeEditor extends AShapeEditor
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private ApplicationGlobeManager globeManager;

    /**
     * Create a shape editor with the provided implementations.
     *
     * @param shapeEditorSource the shape editor source implementation
     * @param controlPointController the control point controller implementation
     * @param shapeEditorRenderer the shape editor renderer implementation
     */
    public ShapeEditor(@Nonnull IShapeEditorSource<?> shapeEditorSource, @Nonnull IControlPointController controlPointController, @Nonnull IShapeEditorRenderer shapeEditorRenderer)
    {
        super(shapeEditorSource, controlPointController, shapeEditorRenderer);
    }

    @Override
    public boolean applyChangesToSource()
    {
        try
        {
            getShapeEditorSource().applyControlPointsToSource(getControlPointController().getControlPoints());
        } catch (IllegalArgumentException e)
        {
            logger.debug("Could not apply changes to source.", e);
            return false;
        }
        onApplyChangesToSource();
        return true;
    }
}
