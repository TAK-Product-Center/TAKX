package gov.takx.core.view.gui.propertiespanel;

/**
 * Event fired to request that the properties panel remove the display of a given properties item,
 * if it is currently being displayed.  This is unique to properties associated with anything other
 * than a Map Object, which is handled via MO selection changes.
 *
 * @since 2.37
 */
public class RemovePropertiesItem
{
    private final Object identityObject;

    /**
     * @param identityObject Identity object of properties to remove
     */
    public RemovePropertiesItem(Object identityObject)
    {
        this.identityObject = identityObject;
    }

    /**
     * @return Identity object of properties to remove
     */
    public Object getIdentityObject()
    {
        return identityObject;
    }
}
