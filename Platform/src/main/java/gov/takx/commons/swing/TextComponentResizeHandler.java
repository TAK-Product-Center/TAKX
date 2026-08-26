package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Monitors a given scroll pane's viewport for width changes.  If a change occurs, reset the size of registered
 * text components, allowing them to shrink.  Without this, the text area will grow, but never shrink.
 * <p/>
 * For {@link javax.swing.JTextArea} components, the size will be reset to {@code 0,0}.  For {@link JEditorPane} components
 * the size will be set to that actual viewport size, since setting it to {@code 0,0} makes it invisible.
 * 
 * @since 5.5
 */
@IncubatingApi(since = "5.5")
public class TextComponentResizeHandler
{
    private final JScrollPane scrollPane;
    private final List<JTextComponent> textComponents = new ArrayList<>();

    /**
     * @param scrollPane Scroll pane to monitor for viewport size changes
     * @see #addTextComponent(JTextComponent)
     */
    public TextComponentResizeHandler(@Nonnull JScrollPane scrollPane)
    {
        this.scrollPane = scrollPane;

        scrollPane.getViewport().addChangeListener(new ChangeListener()
        {
            private int lastWidth;

            @Override
            public void stateChanged(ChangeEvent e)
            {
                final var bounds = scrollPane.getViewport().getBounds();
                final int width = bounds.width;

                if (width < lastWidth) doResize();
                lastWidth = width;
            }
        });
    }

    /**
     * @param scrollPane Scroll pane to monitor for viewport size changes
     * @param textComponent Initial text component to manage
     * @see #addTextComponent(JTextComponent)
     */
    public TextComponentResizeHandler(@Nonnull JScrollPane scrollPane, @Nonnull JTextComponent textComponent)
    {
        this(scrollPane);
        addTextComponent(textComponent);

    }

    /**
     * Add the given {@code textComponent} to the collection to be reset when a size change occurs.
     */
    public void addTextComponent(@Nonnull JTextComponent textComponent)
    {
        textComponents.add(textComponent);
    }

    private void doResize()
    {
        final var viewport = scrollPane.getViewport();

        textComponents.forEach(textComponent -> {
            if (textComponent instanceof JEditorPane)
            {
                textComponent.setSize(viewport.getWidth(), viewport.getHeight());
            } else
            {
                textComponent.setSize(0, 0);
            }
        });

        SwingUtilities.invokeLater(() -> {
            scrollPane.revalidate();
            scrollPane.repaint();
        });
    }
}
