package gov.takx.core.toolbar;

import gov.takx.api.plugin.IPlugin;
import gov.takx.api.toolbar.IToolbarSeparator;
import gov.takx.api.toolbar.TabType;
import gov.takx.commons.utils.UUIDUtils;
import jakarta.annotation.Nonnull;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

/**
 * Model representing a vertical separator on the toolbar.
 *
 * @since 3.4
 */
@XmlAccessorType(XmlAccessType.NONE)
public class ToolbarSeparator extends AToolbarComponent implements IToolbarSeparator
{
    /**
     * No-arg constructor for JAXB.
     */
    @SuppressWarnings("unused") // Used by JAXB
    private ToolbarSeparator()
    {
    }

    /**
     * Constructor for a separator <b>not</b> created by a plugin or a user.
     * 
     * @param parentTabType The type of the parent tab to which the separator will be added (used to create a unique id)
     * @param indexInTab The initial index of this separator within the parent tab (used to create a unique id; it is ok
     *                   if the index changes later)
     * @since 5.2 (created to provide parameters for generating a reproducible unique id)
     */
    public ToolbarSeparator(@Nonnull TabType parentTabType, int indexInTab)
    {
        super(UUIDUtils.generateType3UUID(parentTabType.name() + ":" + indexInTab), null);
    }

    /**
     * Constructor for a separator created by a plugin.
     *
     * @param plugin Plugin creating the component
     * @param uniqueName A unique name for the separator, such as the display text of the parent tab plus the 
     *                   separator's initial index within the tab, e.g. "Tools:1"
     * @since 5.2 (modified to add unique name param, which is necessary to create a reproducible unique id)
     */
    public ToolbarSeparator(@Nonnull IPlugin plugin, @Nonnull String uniqueName)
    {
        super(UUIDUtils.generateType3UUID(plugin.getUniqueKey() + ":" + uniqueName), plugin);
    }

    /**
     * Constructor for a separator created by a user.
     * 
     * @param parentTabDisplayText The display text of the parent tab to which the separator will be added (used to 
     *                             create a unique id)
     * @param indexInTab The initial index of this separator within the parent tab (used to create a unique id; it is ok
     *                   if the index changes later)
     * @since 5.2
     */
    public ToolbarSeparator(@Nonnull String parentTabDisplayText, int indexInTab)
    {
        super(UUIDUtils.generateType3UUID(TabType.USER_CREATED.name() + ":" + parentTabDisplayText + ":" + indexInTab), null);
    }

    /**
     * Copy constructor.
     *
     * @param origSeparator The separator to copy
     * @see #copy()
     */
    private ToolbarSeparator(@Nonnull ToolbarSeparator origSeparator)
    {
        super(origSeparator);
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Nonnull
    @Override
    public ToolbarSeparator copy()
    {
        return new ToolbarSeparator(this);
    }

    @Override
    public String toString()
    {
        return "ToolbarSeparator@" + Integer.toHexString(hashCode()) + "{id=" + id + "}";
    }
}
