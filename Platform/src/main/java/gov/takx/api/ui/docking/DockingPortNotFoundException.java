package gov.takx.api.ui.docking;

/**
 */
public class DockingPortNotFoundException extends Exception
{
    public DockingPortNotFoundException()
    {
        super();
    }

    public DockingPortNotFoundException(String msg)
    {
        super(msg);
    }

    public DockingPortNotFoundException(String msg, Throwable t)
    {
        super(msg, t);
    }

    public DockingPortNotFoundException(Throwable t)
    {
        super(t);
    }
}
