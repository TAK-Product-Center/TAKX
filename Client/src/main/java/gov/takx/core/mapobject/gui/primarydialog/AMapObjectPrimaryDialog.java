package gov.takx.core.mapobject.gui.primarydialog;

import com.jgoodies.forms.factories.Borders;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.ui.DevicePanelMetadataKeys;
import gov.takx.commons.swing.ComponentUtils;
import gov.takx.core.mapobject.gui.AMapObjectDialog;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.IMapObjectPanel;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.MapObjectHeaderPanel;
import gov.takx.core.view.gui.propertiespanel.PropertiesPanel;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for all Map Object Primary dialogs.
 *
 * @param <T> The type of MO panel contained in the dialog
 * @since 2.37
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class AMapObjectPrimaryDialog<T extends IMapObjectPanel> extends AMapObjectDialog implements PropertyChangeListener, IMapObjectPrimaryDialog<T>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AtomicBoolean closeRequestProcessed = new AtomicBoolean(false);

    protected final T contentPanel;
    protected final IMapObject mapObject;

    protected MapObjectHeaderPanel headerPanel;

    @Inject
    private MapObjectDialogUtils mapObjectDialogUtils;

    /**
     * Constructor.
     *
     * @param controller        MO Controller
     * @param contentPanel      Content panel, specific to MO type
     */
    protected AMapObjectPrimaryDialog(IMapObjectController controller, T contentPanel)
    {
        super(controller);

        initComponents();

        mapObject = mapObjectController.getMapObject();

        if (mapObject.getMetaBoolean(DevicePanelMetadataKeys.SHOW_HEADER, true))
        {
            headerPanel = new MapObjectHeaderPanel(controller);
            addResourceCleaner(headerPanel);
        }

        this.contentPanel = contentPanel;
        addResourceCleaner(contentPanel);
        contentPanel.setCloseHandler(this::closeRequested);

        mapObject.addPropertyChangeListener(IMapObject.NAME, this);

        addStandardPanels();
    }

    @PostConstruct
    private void postConstruct()
    {
        setTitle(mapObjectDialogUtils.createTitleForMapObjectController(mapObjectController));
    }

    /**
     * Add the standard panels to our layout. This only adds panels to the north and center, so extending classes are
     * able to add a custom panel to the south if needed using {@link #addSouthPanel(JPanel)}.
     */
    private void addStandardPanels()
    {
        if (headerPanel != null) dialogPane.add(headerPanel.getPanel(), BorderLayout.NORTH);
        dialogPane.add(contentPanel.getPanel(), BorderLayout.CENTER);
    }

    /**
     * Adds the provided panel to the south portion of the border layout.
     * @param panel The panel to add
     * @since 3.3
     */
    protected void addSouthPanel(JPanel panel)
    {
        dialogPane.add(panel, BorderLayout.SOUTH);
    }

    /**
     * Set the text on the label next to the location field.
     *
     * @param text Text to display
     * @since 3.0.1
     */
    @InvokeOnEDT
    public void setLocationLabelText(String text)
    {
        if (headerPanel != null) headerPanel.setLocationLabelText(text);
    }

    @Override
    @InvokeOnEDT
    public void openDialog()
    {
        // Limit max height to ensure buttons can be accessed
        final var preferredSize = getPreferredSize();
        final var screenDimensions = ComponentUtils.getActiveMonitorScreenSize();
        final var maxHeight = (int) (screenDimensions.height * 0.95);

        if (preferredSize.height > maxHeight) setPreferredSize(new Dimension(preferredSize.width, maxHeight));

        pack();
        setLocationRelativeTo(getOwner());

        PropertiesPanel.getInstance().onDialogOpening(this); // Notify the properties panel that we are opening

        setVisible(true);
    }

    @Override
    protected void onWindowClosing()
    {
        PropertiesPanel.getInstance().onDialogClosing(this); // Notify the properties panel that we are closing

        mapObject.removePropertyChangeListener(IMapObject.NAME, this);

        super.onWindowClosing();
    }

    @Override
    @InvokeOnEDT
    public void propertyChange(final PropertyChangeEvent event)
    {
        final String propertyName = event.getPropertyName();

        if (propertyName.equals(IMapObject.NAME))
        {
            setTitle(mapObjectDialogUtils.createTitleForMapObjectController(mapObjectController));
        } else
        {
            logger.warn("Unexpected property change: {}", event);
        }
    }

    /**
     * Process a request to close this dialog.  Only the first such request will actually dispose of this dialog.
     */
    private void closeRequested()
    {
        if (closeRequestProcessed.compareAndSet(false, true))
        {
            dispose();
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();

        //======== this ========
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout(5, 5));
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel dialogPane;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
