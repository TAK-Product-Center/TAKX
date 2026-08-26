package gov.takx.core.mapobject.gui;

import com.atakmap.commoncommo.CommoException;
import com.atakmap.commoncommo.Contact;
import gov.tak.api.comms.takserver.IExtendedDataPackageManager;
import gov.tak.core.comms.commo.ContactManager;
import gov.tak.core.comms.takserver.DataPackageManager;
import gov.takx.api.annotation.PseudoBean;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.io.File;
import java.util.List;
import java.util.function.Supplier;

/**
 * Presents the user with options to broadcast a DataPackage or send it to specific groups or users.
 *
 * @since 5.3
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class SendAsDataPackage extends ASendAsDialog
{
    private final Supplier<File> dataPackageSupplier;
    private final Runnable nullDataPackageErrorHandler;
    private final Runnable sendingDataPackageErrorHandler;

    @Inject
    private ContactManager contactManager;

    @Inject
    private IExtendedDataPackageManager dataPackageManager;

    /**
     * @param itemName            The name of the item to be sent, used in the dialog title
     * @param dataPackageSupplier Supplies the DataPackage to be sent after the user chooses recipients
     * @param nullDataPackageErrorHandler Called if the dataPackageSupplier returns a {@code null} DataPackage
     * @param sendingDataPackageErrorHandler Called if the DataPackage cannot be sent due to an exception
     */
    public SendAsDataPackage(@Nonnull String itemName, @Nonnull Supplier<File> dataPackageSupplier,
                             @Nonnull Runnable nullDataPackageErrorHandler, @Nonnull Runnable sendingDataPackageErrorHandler)
    {
        super("Send As DataPackage: " + itemName);
        this.dataPackageSupplier = dataPackageSupplier;
        this.nullDataPackageErrorHandler = nullDataPackageErrorHandler;
        this.sendingDataPackageErrorHandler = sendingDataPackageErrorHandler;
    }

    /**
     * Pass the selected {@link Contact Contacts} to the consumer
     */
    @Override
    protected void send()
    {
        processRequest(getSelectedContactUids().stream().map(c -> contactManager.findContactByUid(c)).toList());
    }

    /**
     * Pass all connected {@link Contact Contacts} to the consumer
     */
    @Override
    protected void broadcast()
    {
        processRequest(contactManager.getAvailableContacts().stream().toList());
    }

    /**
     * Tries to retrieve the DataPackage and pass it to the {@link DataPackageManager} to send.
     * <p>
     * If the DataPackage cannot be retrieved, the {@link #nullDataPackageErrorHandler} is called.
     * If the DataPackage cannot be sent, the {@link #sendingDataPackageErrorHandler} is called.
     *
     * @param contactList the list of {@link Contact Contacts} to send the DataPackage to
     */
    private void processRequest(List<Contact> contactList)
    {
        final File dataPackage = dataPackageSupplier.get();
        if (dataPackage != null)
        {
            try
            {
                dataPackageManager.sendPackageToContacts(dataPackage, contactList);
            } catch (CommoException e)
            {
                sendingDataPackageErrorHandler.run();
            }
        } else
        {
            nullDataPackageErrorHandler.run();
        }
        dispose();
    }
}
