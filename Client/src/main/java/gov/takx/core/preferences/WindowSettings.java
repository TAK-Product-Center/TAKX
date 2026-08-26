package gov.takx.core.preferences;

import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.PreferencesNode;

import java.awt.Dimension;

/**
 * Class to hold the window configuration as saved to/restored from preferences.
 *
 * @since 2.0.0
 */
public class WindowSettings
{
    /**
     * Default application window size.
     *
     * @since 2.37
     */
    private static final Dimension DEFAULT_APPLICATION_WINDOW_SIZE = new Dimension(1024, 768);

    public static final String BW = "windowBoundsW";
    public static final String BH = "windowBoundsH";
    public static final String BX = "windowBoundsX";
    public static final String BY = "windowBoundsY";
    public static final String SAVE = "saveWindowSettings";

    private boolean saveWindowSettings = true;
    private int x;
    private int y;
    private int width = DEFAULT_APPLICATION_WINDOW_SIZE.width;
    private int height = DEFAULT_APPLICATION_WINDOW_SIZE.height;

    private boolean hadSavedWindowDimensions = false;

    @SuppressWarnings("unused") // Called via reflection
    private WindowSettings(PreferencesNode node)
    {
        if (node != null)
        {
            hadSavedWindowDimensions = node.getInt(BW, -1) >= 0;

            saveWindowSettings = node.getBoolean(SAVE, true);
            x = node.getInt(BX, 0);
            y = node.getInt(BY, 0);
            width = node.getInt(BW, width);
            height = node.getInt(BH, height);
        }
    }

    public WindowSettings(int x, int y, int width, int height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * @return True if there were valid window dimensions saved, false if the values contained in this object are generated defaults.
     * @since 2.31
     */
    public boolean hadSavedWindowDimensions()
    {
        return hadSavedWindowDimensions;
    }

    public int getHeight()
    {
        return height;
    }

    public int getWidth()
    {
        return width;
    }

    public int getX()
    {
        return x;
    }

    public int getY()
    {
        return y;
    }

    public void save()
    {
        final PreferencesNode node = PreferenceEntry.WINDOW_SETTINGS_NODE.node();

        node.put(SAVE, saveWindowSettings);
        node.put(BX, x);
        node.put(BY, y);
        node.put(BW, width);
        node.put(BH, height);
    }
}
