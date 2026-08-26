package gov.takx.core.view.gui.maps.terraindiscovery.controller.calculator;

import java.util.concurrent.Callable;

/**
 */
public abstract class TerrainDiscoveryTask implements Callable
{
    private TerrainDiscoveryThreadPool owner;
    private int id;

    public void setOwner(TerrainDiscoveryThreadPool owner, int id)
    {
        this.owner = owner;
        this.id = id;
    }

    protected boolean isCanceled()
    {
        return id != owner.getBatchID();
    }

    @Override
    public Object call() throws Exception
    {
        if (isCanceled())
        {
            return null;
        }
        owner.setProgressText(getDisplayName());
        run();
        owner.incrementTasksComplete(id);
        return null;
    }

    public String getDisplayName()
    {
        return "";
    }

    protected abstract void run() throws Exception;
}
