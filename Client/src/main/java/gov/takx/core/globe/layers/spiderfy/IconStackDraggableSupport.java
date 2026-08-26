package gov.takx.core.globe.layers.spiderfy;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.drag.DragContext;
import gov.nasa.worldwind.drag.DraggableSupport;
import gov.takx.api.globe.spiderfy.IIconStack;
import gov.takx.api.globe.spiderfy.IconStackExpansionShape;

/**
 * Drag support that enables dragging icons that are visually displaced.
 *
 * @since 2.26
 */
public class IconStackDraggableSupport extends DraggableSupport
{
    IconStackDraggableSupport(Object dragObject, int altitudeMode)
    {
        super(dragObject, altitudeMode);
    }

    @Override
    public void dragScreenSizeConstant(DragContext dragContext)
    {
        //Change the default initial point to prevent challenges where a user drags the mouse quickly before
        //the initial offset has been calculated. The dragContext's previous point will refer to the location
        //where the mouse drag event started and will ensure that the mouse and icon move together while
        //dragging.
        dragContext.setInitialPoint(dragContext.getPreviousPoint());

        if (shouldCalculateInitialOffset(dragContext))
        {
            initialScreenPointOffset = computeScreenOffsetFromReferencePosition(getReferencePosition(), dragContext);
        }

        super.dragScreenSizeConstant(dragContext);
    }

    /**
     * Evaluates if the initial offset of an icon should be calculated. If the initial offset is not calculated
     * then the icon will not be draggable.
     *
     * @param dragContext Data wrapper for information about dragging information and mouse inputs.
     * @return True if the initial offset of an icon should be calculated.
     */
    private boolean shouldCalculateInitialOffset(DragContext dragContext)
    {
        boolean parentHidden = true;
        boolean isDraggableShape = true;

        if (dragObject instanceof IIconStack)
        {
            IIconStack iconStack = ((IIconStack) dragObject);

            parentHidden = iconStack.getParentIconStack() == null || !iconStack.getParentIconStack().isVisible();
            isDraggableShape = iconStack.getParentIconStack() == null || iconStack.getParentIconStack().getExpansionShape() == IconStackExpansionShape.NONE;
        }

        return (parentHidden || isDraggableShape) && initialScreenPointOffset == null
                && dragContext != null && !dragContext.getDragState().equals(AVKey.DRAG_BEGIN);
    }
}
