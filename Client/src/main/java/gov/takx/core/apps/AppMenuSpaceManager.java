package gov.takx.core.apps;

import gov.takx.api.ui.IMenuSpaceManager;
import gov.takx.core.globe.TerrainPopupMenuProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.swing.Action;
import javax.swing.JMenuItem;

import java.awt.Component;
import java.util.List;

/**
 */
@ApplicationScoped
public class AppMenuSpaceManager implements IMenuSpaceManager
{
    private static final String NO_POPUP_ERROR = "There is no popup menu on an App Plugin";

    private final TerrainPopupMenuProvider terrainPopupMenuProvider;

    @Inject
    AppMenuSpaceManager(TerrainPopupMenuProvider terrainPopupMenuProvider)
    {
        this.terrainPopupMenuProvider = terrainPopupMenuProvider;
    }

    @Override
    public void redirectPopupMenuAction(@Nonnull ContextMenuEnum menuEntry, @Nullable Action action)
    {
        throw new IllegalStateException(NO_POPUP_ERROR);
    }

    @Override
    public void setPopupMenuVisible(@Nonnull ContextMenuEnum menuEntry, boolean visible)
    {
        throw new IllegalStateException(NO_POPUP_ERROR);
    }

    @Override
    public void setPopupMenuText(@Nonnull ContextMenuEnum menuEntry, @Nullable String text)
    {
        throw new IllegalStateException(NO_POPUP_ERROR);
    }

    @Override
    public void setMenuOrder(@Nonnull List<ContextMenuEnum> menuEntries)
    {
        throw new IllegalStateException(NO_POPUP_ERROR);
    }

    @Override
    public void addDevicePopupMenuComponent(@Nonnull Component component)
    {
        throw new IllegalStateException(NO_POPUP_ERROR);
    }

    @Override
    public void addDevicePopupMenuComponent(@Nonnull Component component, int index)
    {
        throw new IllegalStateException(NO_POPUP_ERROR);
    }

    @Override
    public void removeDevicePopupMenuComponent(@Nonnull Component component)
    {
        throw new IllegalStateException(NO_POPUP_ERROR);
    }

    @Override
    public void addDevicePopupMenuSeparator()
    {
        throw new IllegalStateException(NO_POPUP_ERROR);
    }

    @Override
    public void addDevicePopupMenuSeparator(int index)
    {
        throw new IllegalStateException(NO_POPUP_ERROR);
    }

    @Override
    public void addTerrainPopupMenuItem(@Nonnull JMenuItem menuItem)
    {
        terrainPopupMenuProvider.addTerrainPopupMenuItem(menuItem);
    }

    @Override
    public void removeTerrainPopupMenuItem(@Nonnull JMenuItem menuItem)
    {
        terrainPopupMenuProvider.removeTerrainPopupMenuItem(menuItem);
    }
}
