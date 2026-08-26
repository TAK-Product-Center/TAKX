package gov.takx.core.view.gui;

import gov.takx.api.event.StopProjectServices;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages {@link AStandardDialog} instances, ensuring only a single instance of any given type is displayed.
 *
 * @since 5.5.1
 */
@ApplicationScoped
public class StandardDialogManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Map<Class<? extends AStandardDialog>, AStandardDialog> dialogMap = new HashMap<>();

    @Inject
    private INotificationService notificationService;

    @SuppressWarnings("unused")
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices event)
    {
        dialogMap.clear();
    }

    /**
     * Show the dialog specified by the provided class. Create it if it does not already exist or bring it to the front
     * if it does.
     *
     * @param dialogClass The dialog's class; must have a no-arg constructor
     */
    public synchronized void showDialog(@Nonnull Class<? extends AStandardDialog> dialogClass)
    {
        var dialog = dialogMap.get(dialogClass);

        if (dialog != null)
        {
            dialog.toFront();
        } else
        {
            try
            {
                dialog = dialogClass.getConstructor().newInstance();
                dialog.addCloseHandler(d -> dialogMap.remove(dialogClass));
                dialogMap.put(dialogClass, dialog);

                dialog.setVisible(true);
            } catch (ReflectiveOperationException e)
            {
                logger.error("Error creating dialog for class {}", dialogClass, e);

                notificationService.show(Notification.builder("Error creating dialog", Notification.Type.Error).build());
            }
        }
    }
}
