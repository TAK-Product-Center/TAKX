package gov.takx.core.globe;

import bibliothek.gui.dock.common.MultipleCDockableLayout;
import bibliothek.util.xml.XElement;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IFavorite;
import gov.takx.platform.globe.Favorite;
import jakarta.inject.Inject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Layout responsible for saving/restoring {@link GlobeDockable} instances.  The only state maintained in the
 * layout is the globe Id.
 */
@PseudoBean
class GlobeLayout implements MultipleCDockableLayout
{
    @Inject
    private IExtendedGlobeManager globeManager;

    private static final int ANONYMOUS_ID = -1;

    private int globeId = -1;
    private IFavorite favorite;

    GlobeLayout()
    {
    }

    GlobeLayout(int globeId)
    {
        this.globeId = globeId;
    }

    public int getGlobeId()
    {
        return globeId;
    }

    public boolean isAnonymous()
    {
        return globeId == ANONYMOUS_ID;
    }

    public IFavorite getFavorite()
    {
        return favorite;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return globeId == ((GlobeLayout) o).globeId;
    }

    @Override
    public int hashCode()
    {
        return globeId;
    }

    @Override
    public void readStream(DataInputStream in) throws IOException
    {
        globeId = in.readInt();
    }

    @Override
    public void writeStream(DataOutputStream out) throws IOException
    {
        out.writeInt(globeId);
    }

    @Override
    public void writeXML(XElement element)
    {
        IApplicationGlobe globe = globeManager.getGlobeDelegate(globeId);
        if (globe != null)
        {
            element.addElement("globeId").setInt(globeId);
            IFavorite bookmark = globe.getViewBookmark();
            element.addElement("description").setString(bookmark.getDescription());
            element.addElement("owner").setString(bookmark.getOwner());
            element.addElement("latitude").setDouble(bookmark.getLatitude());
            element.addElement("longitude").setDouble(bookmark.getLongitude());
            element.addElement("elevation").setDouble(bookmark.getElevation());
            element.addElement("heading").setDouble(bookmark.getHeading());
            element.addElement("pitch").setDouble(bookmark.getPitch());
            element.addElement("zoom").setDouble(bookmark.getZoom());
        }
    }

    @Override
    public void readXML(XElement element)
    {
        XElement e = element.getElement("globeId");
        if (e != null)
        {
            globeId = e.getInt();
            favorite = Favorite.createViewBookmark(
                    element.getElement("description").getString(),
                    element.getElement("owner").getString(),
                    element.getElement("latitude").getDouble(),
                    element.getElement("longitude").getDouble(),
                    element.getElement("elevation").getDouble(),
                    element.getElement("heading").getDouble(),
                    element.getElement("pitch").getDouble(),
                    element.getElement("zoom").getDouble());
        }
    }
}
