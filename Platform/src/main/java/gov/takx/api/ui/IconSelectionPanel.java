package gov.takx.api.ui;

import gov.takx.api.plugin.IPluginDelegate;

import javax.swing.ImageIcon;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 */
public class IconSelectionPanel extends AIconSelectionPanel
{
    private static final Map<String, ImageIcon> scaledImagesAndAddresses = new ConcurrentHashMap<>();
    private static final Set<String> addedIconsGroup = new ConcurrentSkipListSet<>();

    public IconSelectionPanel()
    {
    }

    public IconSelectionPanel(IPluginDelegate delegate)
    {
        super(delegate);
    }

    @Override
    protected Map<String, ImageIcon> getScaledImagesAndAddresses()
    {
        return scaledImagesAndAddresses;
    }

    @Override
    protected Set<String> getAddedIconsGroup()
    {
        return addedIconsGroup;
    }
}
