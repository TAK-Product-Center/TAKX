package gov.takx.core.toolbar;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.plugin.IPlugin;
import gov.takx.api.toolbar.IToolbarComponent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;

import java.util.Objects;
import java.util.UUID;

/**
 * Model representing a component on a toolbar tab.
 *
 * @since 3.4
 */
@PseudoBean
@XmlTransient
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AToolbarComponent implements IToolbarComponent
{
    @XmlElement
    protected final UUID id;

    // The tab to which this component belongs. A component can only belong to a single tab at a time.
    protected ToolbarTab tab;

    // The name of the plugin that created this component (or null if created by TAKX)
    @XmlElement
    protected final String pluginName;

    // If this component was created by a plugin, this contains the unique key identifying the plugin.
    @XmlElement
    protected final String pluginUniqueKey;

    @SuppressWarnings("unused") // Used by JAXB
    protected AToolbarComponent()
    {
        // JAXB will use reflection to set these values correctly; setting to null here so they can be final
        id = null;
        pluginName = null;
        pluginUniqueKey = null;
    }

    /**
     * Constructor.
     *
     * @param id The id for the component
     * @param plugin Plugin creating the component (or {@code null} for core buttons)
     */
    protected AToolbarComponent(@Nonnull UUID id, @Nullable IPlugin plugin)
    {
        this.id = id;
        pluginName = plugin != null ? plugin.getName() : null;
        pluginUniqueKey = plugin != null ? plugin.getUniqueKey() : null;
    }

    /**
     * Copy constructor.
     *
     * @param origComponent The component to copy
     */
    protected AToolbarComponent(@Nonnull AToolbarComponent origComponent)
    {
        id = origComponent.id;
        tab = null;
        pluginName = origComponent.pluginName;
        pluginUniqueKey = origComponent.pluginUniqueKey;
    }

    /**
     * @return The tab to which this component belongs. May be {@code null} if component has not yet been added to a tab.
     */
    @Nullable
    public ToolbarTab getTab()
    {
        return tab;
    }

    /**
     * @return The tab to which the component belongs.
     * @throws NullPointerException If the tab is {@code null}
     */
    @Nonnull
    public ToolbarTab requireTab()
    {
        return Objects.requireNonNull(tab, "tab is null");
    }

    /**
     * Adds this component to the provided tab.
     *
     * @param tab The tab to which the component belongs (may be {@code null} if removing the component from the tab)
     * @return this component instance (to allow for chaining calls)
     * @throws IllegalArgumentException if this component is already associated with a toolbar tab.
     */
    public AToolbarComponent setTab(@Nullable ToolbarTab tab) throws IllegalArgumentException
    {
        if (tab == this.tab) return this; // No action if trying to set to the current tab

        if (tab != null && this.tab != null)
        {
            throw new IllegalArgumentException("Toolbar component cannot be added to more than one tab");
        }

        this.tab = tab;
        return this;
    }

    /**
     * @return The configuration containing this component or {@code null} if this component is not in a configuration.
     */
    @Nullable
    public ToolbarConfiguration getParentConfiguration()
    {
        return tab != null ? tab.getConfiguration() : null;
    }

    /**
     * @return The configuration containing this component.
     * @throws NullPointerException If the parent tab or configuration is {@code null}
     */
    @Nonnull
    public ToolbarConfiguration requireParentConfiguration()
    {
        return requireTab().requireConfiguration();
    }

    /**
     * @return True if parent tab is present, valid, and visible.
     */
    public boolean isParentVisible()
    {
        return tab != null && tab.isValid() && tab.isVisible();
    }

    @Override
    @Nonnull
    public UUID getId()
    {
        return id;
    }

    @Override
    @Nullable
    public String getPluginUniqueKey()
    {
        return pluginUniqueKey;
    }

    /**
     * Creates a copy of itself.
     * @return The new component that is a copy of the instance on which the call was made.
     */
    @Override
    @Nonnull
    public abstract AToolbarComponent copy();

    /**
     * To allow the managing classes to distinguish between events fired from reference buttons and their copies (which
     * initially have the same values), instances are only equal if they are the same object.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public final boolean equals(Object o)
    {
        return super.equals(o);
    }

    @Override
    public final int hashCode()
    {
        return super.hashCode();
    }
}
