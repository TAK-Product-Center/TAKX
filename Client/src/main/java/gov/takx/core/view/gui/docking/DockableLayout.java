package gov.takx.core.view.gui.docking;

import bibliothek.gui.dock.common.MultipleCDockableLayout;
import bibliothek.util.xml.XElement;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Layout responsible for saving/restoring a specific {@link ComponentDockable} instance to/from a file.
 */
public class DockableLayout implements MultipleCDockableLayout
{
    private static final String ANONYMOUS_PATH = "gov.takx";
    private static final String ANONYMOUS_NAME = "anonymous";

    private String dockableId;

    public DockableLayout()
    {
        this(ANONYMOUS_PATH + ":" + ANONYMOUS_NAME);
    }

    public DockableLayout(String id)
    {
        dockableId = id;
    }

    public String getDockableId()
    {
        return dockableId;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DockableLayout that = (DockableLayout) o;
        return dockableId.equals(that.dockableId);
    }

    @Override
    public int hashCode()
    {
        return dockableId.hashCode();
    }

    @Override
    public void readStream(DataInputStream in) throws IOException
    {
        dockableId = in.readUTF();
    }

    @Override
    public void writeStream(DataOutputStream out) throws IOException
    {
        out.writeUTF(getDockableId());
    }

    @Override
    public void writeXML(XElement element)
    {
        element.addElement("dockableId").setString(getDockableId());
    }

    @Override
    public void readXML(XElement element)
    {
        dockableId = element.getElement("dockableId").getString();
    }

    public String toString()
    {
        return getClass().getSimpleName() + "[" + dockableId + "]";
    }
}
