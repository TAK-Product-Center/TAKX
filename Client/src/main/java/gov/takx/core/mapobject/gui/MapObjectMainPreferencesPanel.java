package gov.takx.core.mapobject.gui;

import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.plugin.IMutableRaptorPanel;
import gov.takx.commons.swing.IPanelStateListener;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.AMapObjectPanel;

/**
 */
public abstract class MapObjectMainPreferencesPanel extends AMapObjectPanel implements IMutableRaptorPanel
{
    private final String title;
    private final IPanelStateListener panelListener;

    protected MapObjectMainPreferencesPanel(String title, IMapObjectController mapObjectController, IPanelStateListener panelListener)
    {
        super(mapObjectController);
        this.title = title;
        this.panelListener = panelListener;
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    @Override
    public void applyAction()
    {
        applyActionPerformed();
    }

    @Override
    public void releaseResources()
    {
        // Nothing to release here
    }

    protected abstract void applyActionPerformed();

    protected void setPanelDirty()
    {
        if (panelListener != null)
        {
            panelListener.setPanelDirty();
        }
    }

    protected void setPanelClean()
    {
        panelListener.setPanelClean();
    }
}
