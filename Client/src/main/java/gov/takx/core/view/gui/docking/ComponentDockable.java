package gov.takx.core.view.gui.docking;

import bibliothek.gui.dock.common.DefaultMultipleCDockable;
import bibliothek.gui.dock.common.action.predefined.CBlank;
import bibliothek.gui.dock.common.intern.CDockable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Component;
import java.awt.Dimension;

/**
 * A dockable component within TAKX. The component is typically a panel contributed by the core or a plugin. We wrap the
 * component with a "dockable" to allow it to work with the Docking Frames system.
 *
 * @since 2.25
 */
public class ComponentDockable extends DefaultMultipleCDockable
{
    private final Component component;

    public ComponentDockable(@Nullable ComponentDockableFactory factory, @Nonnull Component component)
    {
        super(factory, component.getName(), component);
        this.component = component;

        // Setting the action to blank for the normalize button will cause that button to be hidden. This is
        // desirable because the button behavior is not intuitive: it adds the panel to the center frame but the
        // location is typically north of the globe panel. This is likely not where the user wants it, so the user
        // has to drag it to the desired location anyway, which is exactly what they have to do if the button is
        // hidden. An alternative to hiding the button is overriding this action so the panel is put into a logical
        // place. However, that would take a lot of extra code, and removing the button has the added benefit of
        // helping to declutter the UI.
        putAction(CDockable.ACTION_KEY_NORMALIZE, CBlank.BLANK);
    }

    /**
     * @return The wrapped component.
     * @since 5.6
     */
    @Nonnull
    public Component getComponent()
    {
        return component;
    }

    @Nonnull
    public String getName()
    {
        return getTitleText();
    }

    @Override
    public String toString()
    {
        return "ComponentDockable{" +
                "component=" + component +
                '}';
    }

    /**
     * Sets the size of an unpinned expanded panel. If the new size is smaller than the component's minimum size then the
     * larger of the two values will be used.
     *
     * @param size The new dimensions of the dockable panel.
     * @since 3.1.0
     */
    @Override
    public void setMinimizedSize(@Nonnull Dimension size)
    {
        if (component != null && component.getMinimumSize() != null)
        {
            Dimension minimumSize = component.getMinimumSize();

            size.setSize(Math.max(size.getWidth(), minimumSize.getWidth()), Math.max(size.getHeight(), minimumSize.getHeight()));
        }

        super.setMinimizedSize(size);
    }
}
