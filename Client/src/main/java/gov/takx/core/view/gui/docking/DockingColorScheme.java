package gov.takx.core.view.gui.docking;

import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.ColorMap;
import bibliothek.gui.dock.common.MultipleCDockable;
import bibliothek.gui.dock.common.intern.CDockable;
import bibliothek.gui.dock.util.Priority;
import bibliothek.gui.dock.util.color.ColorManager;
import jakarta.annotation.Nonnull;

import javax.swing.UIDefaults;
import javax.swing.UIManager;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * This class handles the installation of our desired color scheme on the docking system.  Monitor for skin
 * changes and update the docking system colors accordingly.
 *
 * @since 2.25
 */
class DockingColorScheme // implements SkinChangeListener // TODO KMB: Fix for FlatLAF (TAKX-739)
{
    private static final String DOCK_CONTROLLER_PROPERTY_PREFIX = "DockController.";
    private static final int DOCK_CONTROLLER_PREFIX_CHAR_COUNT = DOCK_CONTROLLER_PROPERTY_PREFIX.length();
    private static final String DOCKABLE_PROPERTY_PREFIX = "Dockable.";
    private static final int DOCKABLE_PREFIX_CHAR_COUNT = DOCKABLE_PROPERTY_PREFIX.length();

    private final CControl dockControl;
    private final Map<String, Color> dockControllerColorMap = new HashMap<>();
    private final Map<String, Color> dockableColorMap = new HashMap<>();

    /**
     * Constructor for the given dock control.
     *
     * @param dockControl Our dock control to use to set colors
     */
    DockingColorScheme(CControl dockControl)
    {
        this.dockControl = dockControl;

//        SubstanceLookAndFeel.registerSkinChangeListener(this); // TODO KMB: Fix for FlatLaf (TAKX-739)

        setControlColors();
    }

    /**
     * Apply our colors to the given dock control and all its current dockables.  This is used at initialization
     * and when a skin change occurs so we update all extant dockables.
     */
    private void setControlColors()
    {
        internColors();

        final ColorManager colorManager = dockControl.getController().getColors();
        dockControllerColorMap.forEach((key, value) -> colorManager.put(Priority.CLIENT, key, value));

        // And apply to all existing dockables (this handles the case of a skin change)
        int count = dockControl.getCDockableCount();
        for (int i = 0; i < count; i++)
        {
            final CDockable dockable = dockControl.getCDockable(i);

            if (dockable instanceof MultipleCDockable) setupDockableColors((MultipleCDockable) dockable);
        }
    }

    /**
     * Apply our color scheme to the given dockable.
     *
     * @param dockable Dockable to update
     */
    void setupDockableColors(@Nonnull MultipleCDockable dockable)
    {
        final ColorMap colorMap = dockable.getColors();
        dockableColorMap.forEach(colorMap::setColor);
    }

    // TODO KMB: Fix for FlatLAF (TAKX-739)
//    @Override
//    public void skinChanged()
//    {
//        setControlColors();
//    }

    /**
     * Pull the colors we care about from the current skin.
     */
    private void internColors()
    {
        // Some colors get set on the ColorMap of individual dockables, and some get set with ColorManager for the
        // overall docking controller, so use two different prefixes and two maps
        dockControllerColorMap.clear();
        dockableColorMap.clear();
        final UIDefaults uiDefaults = UIManager.getDefaults();

        for (Object key : uiDefaults.keySet())
        {
            if (key instanceof String stringKey)
            {
                if (stringKey.startsWith(DOCKABLE_PROPERTY_PREFIX))
                {
                    dockableColorMap.put(stringKey.substring(DOCKABLE_PREFIX_CHAR_COUNT), uiDefaults.getColor(stringKey));
                }

                if (stringKey.contains(DOCK_CONTROLLER_PROPERTY_PREFIX))
                {
                    dockControllerColorMap.put(stringKey.substring(DOCK_CONTROLLER_PREFIX_CHAR_COUNT), uiDefaults.getColor(stringKey));
                }
            }
        }
    }
}
