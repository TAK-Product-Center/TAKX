package gov.takx.core.globe.editor;

import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.event.project.ProjectSaving;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.api.globe.IGlobeMapObjectEditor;
import gov.takx.api.mapobject.IMapObject;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class AGlobeMapObjectEditor<T extends IGlobeMapObject>
        implements IGlobeMapObjectEditor
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    protected IExtendedGlobeManager globeManager;

    private final PropertyChangeListener visibilityListener = this::mapObjectVisibilityChanged;

    protected T globeMapObject;
    protected boolean creatingNewShape;

    protected AGlobeMapObjectEditor()
    {
    }

    protected abstract void disposeEditor();

    protected abstract void createEditor();

    protected abstract void updateMapObject();

    /**
     * @return {@code true} if our actual editor has been created
     */
    protected abstract boolean editorCreated();

    @Override
    public boolean isGlobeMapObject(String key)
    {
        return globeMapObject != null
                && globeMapObject.getMapObject() != null
                && globeMapObject.getMapObject().getKey().equals(key);
    }

    @Override
    public T getGlobeMapObject()
    {
        return globeMapObject;
    }

    @Override
    public boolean isArmed()
    {
        return editorCreated();
    }

    @Override
    @InvokeOnEDT
    public void arm()
    {
        disposeEditor();
        createEditor();
    }

    @Override
    public void disarm()
    {
        disarm(false);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Must wait for completion since we can be called during a shutdown and we don't want the rest of the world
     * being torn down before we get to run.
     */
    @Override
    @RemoveObserver
    @InvokeAndWaitOnEDT
    public void disarm(final boolean noUpdate)
    {
        if (editorCreated())
        {
            logger.debug("disarming editor for: {}", globeMapObject);

            if (globeMapObject != null)
            {
                if (!noUpdate) updateMapObject();

                clearMapObject();
            }

            disposeEditor();
        }
    }

    @InvokeOnEDT
    public void mapObjectVisibilityChanged(PropertyChangeEvent pce)
    {
        boolean visible = (boolean) pce.getNewValue();

        // Disarm the editor if the shape is no longer visible.
        if (!visible && globeMapObject != null && editorCreated())
        {
            disarm();
        }
    }

    /**
     * Clear out our reference to the shape MO and unregister listeners as needed.
     */
    protected void clearMapObject()
    {
        if (globeMapObject != null)
        {
            final IMapObject mo = globeMapObject.getMapObject();
            if (mo != null) mo.removePropertyChangeListener(IMapObject.VISIBILITY, visibilityListener);

            globeMapObject = null;
            creatingNewShape = false;
        }
    }

    /**
     * Sets the IGlobeMapObject assigned to this editor to the specified shapeMapObject. If creatingNewShape is true, then
     * this the editor treats the shape a new shape. When a shape is new, the editor enters a mode which makes it
     * easier to construct a new shape from scratch.
     *
     * @param globeMapObject the editor's IGlobeMapObject. A {@code null} value is permitted.
     * @param creatingNewShape {@code true} to specify that the map object is a new (or empty) shape; {@code false} otherwise.
     */
    @Override
    public void setGlobeMapObject(IGlobeMapObject globeMapObject, boolean creatingNewShape)
    {
        if (this.globeMapObject == globeMapObject) return;

        if (this.globeMapObject != null) clearMapObject();

        //noinspection unchecked
        this.globeMapObject = (T) globeMapObject;
        this.creatingNewShape = creatingNewShape;

        if (this.globeMapObject != null)
        {
            final IMapObject mo = globeMapObject.getMapObject();
            if (mo != null) mo.addPropertyChangeListener(IMapObject.VISIBILITY, visibilityListener);
        }
    }

    /**
     * Ensure that the shape is fully updated prior to saving.
     *
     * @since 2.37
     */
    private void onProjectSaving(@Observes ProjectSaving event)
    {
        if (isArmed()) updateMapObject();
    }

    /**
     * The invocation of this method is all that is required to remove us as an event listener.
     *
     * @since 2.37
     */
    @RemoveObserver
    private void onProjectClosing(@Observes ProjectClosing event)
    {
    }
}
