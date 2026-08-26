package gov.takx.api.plugin.io;

import gov.takx.api.messages.IPrePersistRaptorDataMessage;

/**
 * Listener that monitors a data import operation. The monitor can abort the operation at any point.
 *
 * @since 1.3
 */
public interface IImportMonitor
{
    /**
     * Possible directives to return from {@link #onDataMessage}.
     */
    public enum ImportDirective
    {
        /**
         * Continue the import operation.
         */
        CONTINUE,

        /**
         * Abort the import operation, do not import any more data points.
         */
        ABORT
    }

    /**
     * Called for each data message processed by the wizard.
     *
     * @param msg The message about to be imported.
     * @return An {@link ImportDirective} that determines what happens next. If ABORT is returned, the import
     * operation will stop and no further data points will be imported.
     */
    public ImportDirective onDataMessage(IPrePersistRaptorDataMessage msg);
}
