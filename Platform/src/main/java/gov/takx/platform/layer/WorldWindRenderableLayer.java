package gov.takx.platform.layer;

import com.atakmap.map.layer.AbstractLayer;
import gov.nasa.worldwind.render.Renderable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Provide a collection of {@link Renderable} to be submitted for rendering.
 *
 * @since 5.2
 */
public class WorldWindRenderableLayer extends AbstractLayer
{
    // synchronized access only
    private final Collection<Renderable> renderables = new ArrayList<>();
    private volatile boolean pickEnabled = true;

    /**
     * @param name the layer name
     */
    public WorldWindRenderableLayer(String name)
    {
        super(name);
    }

    /**
     * @param name the layer name
     * @param renderables the initial collection of {@link Renderable}
     */
    public WorldWindRenderableLayer(String name, Collection<Renderable> renderables)
    {
        super(name);
        setRenderables(renderables);
    }

    /**
     * Get the collection of {@link Renderable} represented by this layer. This method is thread safe.
     * @return the collection of {@link Renderable}
     */
    public synchronized Collection<Renderable> getRenderables()
    {
        return Collections.unmodifiableCollection(new ArrayList<>(renderables));
    }

    /**
     * Set the collection of {@link Renderable} represented by this layer. This method is thread safe.
     * @param renderables the collection of {@link Renderable}
     */
    public synchronized void setRenderables(Collection<Renderable> renderables)
    {
        this.renderables.clear();
        this.renderables.addAll(renderables);
    }

    /**
     * Add a {@link Renderable} to the collection represented by this layer. This method is thread safe.
     * @param renderable the {@link Renderable} to add
     */
    public synchronized void addRenderable(Renderable renderable)
    {
        renderables.add(renderable);
    }

    /**
     * Remove a {@link Renderable} from the collection represented by this layer. This method is thread safe.
     * @param renderable the {@link Renderable} to remove
     */
    public synchronized void removeRenderable(Renderable renderable)
    {
        renderables.remove(renderable);
    }

    /**
     * Remove all renderables from the collection represented by this layer. This method is thread safe.
     */
    public synchronized void removeAllRenderables()
    {
        renderables.clear();
    }

    /**
     * @param enabled {@code true} if picking is enabled for this layer
     */
    public void setPickEnabled(boolean enabled)
    {
        pickEnabled = enabled;
    }

    /**
     * @return {@code true} if picking is enabled for this layer
     */
    public boolean isPickEnabled()
    {
        return pickEnabled;
    }
}