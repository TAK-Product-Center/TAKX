package gov.takx.core.view.gui.event;

import gov.takx.api.plugin.IPlugin;

import javax.swing.Action;

/**
 */
public class ProxyButtonEvent
{
    private final IPlugin plugin;
    private final boolean addButton;
    private final String iconPath;
    private final String id;
    private final Action action;
    private final String name;

    public ProxyButtonEvent(String id, String name, String iconpath, Action action, boolean add, IPlugin plugin)
    {
        this.plugin = plugin;
        this.addButton = add;
        this.name = name;
        this.id = id;
        this.action = action;
        this.iconPath = iconpath;
    }

    public boolean isAddButton()
    {
        return addButton;
    }

    public Action getAction()
    {
        return action;
    }

    public String getId()
    {
        return id;
    }

    public String getIconPath()
    {
        return iconPath;
    }

    public String getName()
    {
        return name;
    }

    public IPlugin getPlugin()
    {
        return plugin;
    }
}
