package gov.takx.api.ui;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

import javax.swing.JMenuItem;

/**
 * The menu space manager responsible for managing popup menus on the globe terrain.
 * <p>
 * This class can be directly injected into a plugin using CDI:
 * <pre>
 *  &#64;Inject
 *  private ITerrainMenuSpaceManager terranMenuSpaceManager;
 *  </pre>
 *
 * @since 2.38 (extracted methods from IMenuSpaceManager)
 */
@SuppressWarnings("UnusedDeclaration") // Used by plugins
@InjectableService
@NoPluginImplementations
public interface ITerrainMenuSpaceManager
{
    /**
     * Add a menu item to the terrain popup menu.  This menu is displayed when the user right-clicks on globe terrain.
     *
     * @param menuItem Menu item to add
     * @see #removeTerrainPopupMenuItem(javax.swing.JMenuItem)
     * @since 1.49 (originally in IMenuSpaceManager)
     */
    void addTerrainPopupMenuItem(@Nonnull JMenuItem menuItem);

    /**
     * Remove a previously added menu item from the terrain popup menu.
     *
     * @param menuItem Menu item to remove
     * @see #addTerrainPopupMenuItem(JMenuItem)
     * @since 1.49 (originally in IMenuSpaceManager)
     */
    void removeTerrainPopupMenuItem(@Nonnull JMenuItem menuItem);
}
