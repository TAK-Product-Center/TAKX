package gov.takx.platform.ui;

import gov.takx.api.annotation.Experimental;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.api.ui.IStandardDialog;
import gov.takx.commons.swing.KeyActionHandler;
import jakarta.annotation.Nullable;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static gov.takx.commons.swing.KeyActionHandler.ESCAPE_KEY;

/**
 * Abstract base for standard dialogs, this includes cleanup of PseudoBean observers and simpler close notifications.
 *
 * @since 2.37
 */
@Experimental
@PseudoBean
public abstract class AStandardDialog extends JDialog implements IStandardDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Map<Consumer<JDialog>, Boolean> closeHandlers = new IdentityHashMap<>();
    private final List<IResourceCleaner> resourceCleaners = new ArrayList<>();

    /**
     * Default constructor using the main frame as an owner.
     */
    protected AStandardDialog()
    {
        this(JFrame.getFrames()[0]);
    }

    /**
     * @param owner Dialog owner, can be {@code null} to set the main frame as the owner.
     */
    protected AStandardDialog(@Nullable Window owner)
    {
        super(owner == null ? JFrame.getFrames()[0] : owner);
        init();
    }

    /**
     * Uses main frame as the owner and the given title.
     *
     * @param title Dialog title
     */
    protected AStandardDialog(String title)
    {
        super(JFrame.getFrames()[0], title);
        init();
    }

    /**
     * Add a handler to be invoked when this dialog is closing.
     *
     * @param handler Handler to invoke
     */
    public void addCloseHandler(Consumer<JDialog> handler)
    {
        closeHandlers.put(handler, Boolean.TRUE);
    }

    /**
     * Remove a previously added close handler.
     *
     * @param handler Handler to remove
     */
    public void removeCloseHandler(Consumer<JDialog> handler)
    {
        closeHandlers.remove(handler);
    }

    /**
     * Add a resource to be cleaned when this dialog is closing.
     *
     * @param resourceCleaner Resource cleaner to invoke when closing
     */
    public void addResourceCleaner(IResourceCleaner resourceCleaner)
    {
        resourceCleaners.add(resourceCleaner);
    }

    /**
     * Subclasses may override this to perform operations when the window is closing.  This is intended as
     * a simple, internal mechanism, as compared to registering close handlers.
     *
     * @see #addCloseHandler(Consumer)
     */
    protected void onWindowClosing()
    {
        // Do nothing by default
    }

    /**
     * Called when the user presses the X button in the corner of the dialog. Subclasses may override this if they need
     * to take specific action if the dialog was closed this way.
     * @since 3.3
     */
    protected void onClosedByXButton()
    {
        // Do nothing by default
    }

    /**
     * Called when the user presses the Escape key when this dialog has the focus.  The default action is to
     * dispose of the dialog (via {@link #dispose()}).
     */
    protected void onEscape()
    {
        dispose();
    }

    /**
     * Overridden to call {@link #handleClosing()} when the dialog is disposed.
     */
    @Override
    public void dispose()
    {
        handleClosing();
        super.dispose();
    }

    /**
     * Install default settings, and arrange for {@link #pack()} to be called later on the EDT.
     */
    private void init()
    {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Even though dispose on close is already taken care of, the extending class may have specific actions it needs
        // to take only when the dialog is dismissed, so overriding onWindowClosing or adding a close handler wouldn't
        // work since that happens even if the user closed the dialog by, for example, clicking OK.
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                onClosedByXButton();
            }
        });

        new KeyActionHandler(getRootPane(), "StandardDialog.escape", ESCAPE_KEY, this::onEscape).install();

        // In many cases, especially HTML rendering in text areas, the final sizing is not known until the window
        // is visible.  In that case, a call to pack prior to being made visible gets the wrong preferred sizes
        // for some components.  So, we push a call to pack onto the event queue so it happens after we're visible.
        SwingUtilities.invokeLater(() -> {
            pack();

            // And of course, the size may change as a result of the pack call, so reposition the dialog to the center
            // of the screen relative to the owner.
            setLocationRelativeTo(getOwner());
        });
    }

    /**
     * Handle the project closing, and close this dialog.
     */
    @InvokeOnEDT
    private void onProjectClosing(@SuppressWarnings("unused") @Observes ProjectClosing event)
    {
        dispose();
    }

    /**
     * Remove any observer registrations and notify our handlers.
     */
    @RemoveObserver
    private void handleClosing()
    {
        try
        {
            onWindowClosing();

            resourceCleaners.forEach(IResourceCleaner::releaseResources);
            closeHandlers.keySet().forEach(handler -> handler.accept(this));
        } catch (Exception e)
        {
            logger.error("Error during dialog close", e);
        } finally
        {
            resourceCleaners.clear();
            closeHandlers.clear();
        }
    }
}
