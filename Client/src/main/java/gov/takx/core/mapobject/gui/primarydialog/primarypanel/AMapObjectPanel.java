package gov.takx.core.mapobject.gui.primarydialog.primarypanel;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.plugin.IRaptorPanel;
import gov.takx.core.mapobject.gui.primarydialog.MapObjectDialogUtils;
import gov.takx.core.uibridge.SwingUIBinder;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class AMapObjectPanel
        extends JPanel
        implements IRaptorPanel, IMapObjectPanel, PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private MapObjectDialogUtils mapObjectDialogUtils;

    @Inject
    protected SwingUIBinder swingUIBinder;

    private final List<String> propertyList = new ArrayList<>();
    protected final IMapObject mapObject;
    protected final IMapObjectController mapObjectController;

    private Runnable closeHandler = () -> {
    };

    protected AMapObjectPanel(IMapObjectController mapObjectController)
    {
        this.mapObjectController = mapObjectController;
        mapObject = mapObjectController != null ? mapObjectController.getMapObject() : null;
    }

    @Override
    public void setCloseHandler(Runnable closeHandler)
    {
        this.closeHandler = closeHandler;
    }

    /**
     * Request a "close" of the containing dialog, if any.
     *
     * @since 2.37
     */
    protected void requestClose()
    {
        closeHandler.run();
    }

    @Override
    public JPanel getPanel()
    {
        return this;
    }

    @Override
    public String getTitle()
    {
        return mapObjectDialogUtils.createTitleForMapObjectController(mapObjectController);
    }

    @Override
    public URL getIcon()
    {
        return null;
    }

    @Override
    public void propertyChange(final PropertyChangeEvent propertyChangeEvent)
    {
        final String propertyName = propertyChangeEvent.getPropertyName();

        // let the extending panel override this
        if (!propertyList.contains(propertyName))
        {
            logger.warn("Unexpected property change event: {}", propertyName);
        }
    }

    protected final IMapObject getMapObject()
    {
        return mapObject;
    }

    protected IMapObjectController getMapObjectController()
    {
        return mapObjectController;
    }

    protected final void addProperties(List<String> properties)
    {
        mapObject.removePropertyChangeListener(propertyList, this);
        propertyList.addAll(properties);
        mapObject.addPropertyChangeListener(propertyList, this);
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void releaseResources()
    {
        if (mapObject != null) mapObject.removePropertyChangeListener(propertyList, this);
    }
}
