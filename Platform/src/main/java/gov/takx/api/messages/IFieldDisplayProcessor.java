package gov.takx.api.messages;

/**
 * Interface for plugins to contribute a class to perform custom processing of objects stored in
 * {@link IRaptorDataMessage} fields into human readable strings. Contributed class is specified in the Data Structure
 * definition XML file as a field "class" attribute.
 * <p>
 * For example:  <BytesField name="SerialNumber" class="gov.takx.plugin.acme.device.SerialNumberFieldProcessor"/>
 * In this case, the SerialNumberFieldProcessor class will control the String value displayed in the device log
 * by implementing this interface.
 *
 * @since 2.29
 */
public interface IFieldDisplayProcessor
{
    /**
     * Return a display string for a given field object.  This is a chance to turn something not-so-human-readable
     * like an array into something the user might actually want to see in the device log.
     *
     * @param fieldValue the field value.
     * @return a string based on processing of the field value.
     */
    String getDisplayString(Object fieldValue);
}
