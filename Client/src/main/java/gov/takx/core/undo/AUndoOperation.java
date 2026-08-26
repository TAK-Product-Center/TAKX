package gov.takx.core.undo;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.globe.IGlobeMapObjectEditor;
import jakarta.inject.Inject;

/**
 * Abstract base for undo operations, providing access to injected services.
 *
 * @since 2.37
 */
@PseudoBean
public abstract class AUndoOperation implements UndoOperation
{
    @Inject
    protected IGlobeManager globeManager;

    protected final IGlobeMapObjectEditor shapeEditor;

    protected AUndoOperation()
    {
        this(null);
    }

    protected AUndoOperation(IGlobeMapObjectEditor shapeEditor)
    {
        this.shapeEditor = shapeEditor;
    }
}
