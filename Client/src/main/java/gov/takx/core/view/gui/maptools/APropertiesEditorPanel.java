package gov.takx.core.view.gui.maptools;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.event.InitializeDrawToolEvent;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IWorldWindowGL;
import gov.takx.api.plugin.IMutableRaptorPanel;
import gov.takx.commons.swing.ColorChooserUtils;
import gov.takx.commons.swing.ComponentUtils;
import gov.takx.commons.swing.IPanelStateListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import net.java.dev.colorchooser.ColorChooser;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;
import javax.swing.JSpinner;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.function.Consumer;

/**
 * Abstract base for properties panels that can be used in both the Properties side panel and the Map Object
 * Preferences dialog.
 *
 * @param <T> The type of "shape" being edited.  Sadly, there is no unifying interface to constrain this type
 *           parameter, so it is left unconstrained.  However, it is either going to be a Map Object or some form
 *           of shape.
 * @since 3.3
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class APropertiesEditorPanel<T> extends JPanel implements IMutableRaptorPanel, IToolEditorPanel, PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    protected IExtendedGlobeManager globeManager;

    @Inject
    @UiEventBus
    protected EventBus uiEventBus;

    private final String title;
    protected final IPanelStateListener panelListener;

    // This flag controls whether this panel is used for the initial creation of a shape.
    // The initial creator use case is unique in that the user will pre-populate the controls with values they want
    // and then click on the globe.  At that point, we need to install the UI settings into the newly crated shape.
    protected final boolean initialCreationOnly;

    // In "direct action mode" any change made to a control is immediately applied to the shape.  Otherwise, we
    // wait for the "Apply" action to be invoked.
    protected final boolean directActionMode;

    protected boolean updatingPanel; // used to avoid updating the object as we load it
    protected boolean ignoreEvent; // Used to avoid responding to events we generate

    protected T target;

    /**
     * @param title Title of panel
     * @param target Target object (the one being edited), {@code null} if performing an initial object creation
     * @param directActionMode Pass {@code true} to immediately apply changes to UI controls
     * @param panelListener Listener for "panel dirty" events, {@code null} if not being used
     */
    protected APropertiesEditorPanel(@Nonnull String title, @Nullable T target, boolean directActionMode,
                                     @Nullable IPanelStateListener panelListener)
    {
        this.title = title;
        this.target = target;
        this.panelListener = panelListener;
        this.directActionMode = directActionMode;
        initialCreationOnly = target == null;
    }

    @Override
    public JPanel getPanel()
    {
        return this;
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    @Override
    public URL getIcon()
    {
        return null;
    }

    /**
     * Register for {@link InitializeDrawToolEvent} events, if in initialCreationOnly mode.
     */
    protected void registerForEvents()
    {
        if (initialCreationOnly) uiEventBus.register(this);
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void releaseResources()
    {
        if (uiEventBus.isRegistered(this)) uiEventBus.unregister(this);
        target = null;
    }

    /**
     * Catch the initialization event and set the initial properties.
     * @see #updateTargetProperties()
     * @see #initializeNewInstance()
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void setInitialAttributeProperties(InitializeDrawToolEvent evt)
    {
        //noinspection unchecked
        target = (T) evt.mapObject();

        logger.debug("Set initial properties on {}", target);

        ignoreEvent = true;
        initializeNewInstance();
        updateTargetProperties();
        ignoreEvent = false;

        globeManager.repaintAllGlobes();
    }

    /**
     * Perform any additional operations for a newly created instance.
     */
    protected void initializeNewInstance()
    {
    }

    @Override
    public void applyAction()
    {
        ignoreEvent = true;
        updateTargetProperties();
        ignoreEvent = false;
        globeManager.repaintAllGlobes();

        if (!directActionMode) setPanelClean();
    }

    /**
     * Mark the panel as dirty (only matters when NOT in {@code directActionMode}).
     */
    protected void setPanelDirty()
    {
        if (panelListener != null) panelListener.setPanelDirty();
    }

    /**
     * Mark the panel as clean (only matters when NOT in {@code directActionMode}).
     */
    protected void setPanelClean()
    {
        if (panelListener != null) panelListener.setPanelClean();
    }

    @Override
    public String validateContents()
    {
        return null;
    }

    /**
     * Apply the values from the UI controls to the target object.
     */
    protected abstract void updateTargetProperties();

    /**
     * If we have a target, and we are not {@code updatingPanel}, then process a property update.
     * In {@code directActionMode}, execute the given {@code updater}. Otherwise, simply call {@link #setPanelDirty}.
     *
     * @param updater Update action to perform
     */
    protected void updateProperty(Runnable updater)
    {
        if (target != null && !updatingPanel)
        {
            if (directActionMode)
            {
                ignoreEvent = true;
                updater.run();
                globeManager.repaintAllGlobes();
                ignoreEvent = false;
            } else
            {
                setPanelDirty();
            }
        }
    }

    /**
     * Commit the given list of spinners.
     *
     * @see ComponentUtils#commitSpinner(JSpinner)
     */
    protected void commitSpinners(JSpinner... spinners)
    {
        for (JSpinner spinner : spinners) ComponentUtils.commitSpinner(spinner);
    }

    /**
     * Prepare a color chooser by setting the default palettes and adding the given consumer as a "color" property
     * change handler.
     */
    protected void setupColorChooser(ColorChooser colorChooser, Consumer<Color> handler)
    {
        colorChooser.setPalettes(ColorChooserUtils.getDefaultPalettes());
        colorChooser.addPropertyChangeListener("color", pce -> handler.accept((Color) pce.getNewValue()));
    }

    /**
     * @return The last focused World Window
     */
    protected IWorldWindowGL getWwd()
    {
        return globeManager.getLastFocusedGlobe().getWorldWindow();
    }
}
