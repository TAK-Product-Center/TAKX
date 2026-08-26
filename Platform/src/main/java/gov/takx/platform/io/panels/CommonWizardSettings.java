package gov.takx.platform.io.panels;

/**
 * Collection of constants used by the standard wizard panels to exchange information.
 *
 */
public class CommonWizardSettings
{
    /**
     * The file selected for import/export.
     */
    public static final String FILE = "io.wizard.file";

    /**
     * The files selected for import/export. Used only with wizards that support the import of multiple files at once.
     */
    public static final String FILES = "io.wizard.files";

    /**
     * The format selected for import/export.
     */
    public static final String FORMAT = "io.wizard.format";

    /**
     * The location for the import.
     */
    public static final String LOCATION = "io.wizard.location";

    /**
     * The file system that will be used for the wizard..
     */
    public static final String VFS = "io.wizard.vfs";

    /**
     * Map objects that the user has selected for export.
     */
    public static final String SELECTED_MAP_OBJECTS = "io.wizard.selected-map-objects";

    /**
     * Finalized selection of map objects, in the case that the import step involving the choosing
     * of a specific message type is used.
     */
    public static final String SELECTED_MAP_OBJECT_FINAL = "io.wizard.selected-map-objects-final";

    /**
     * The data structure (message type) that the user has selected for export.
     */
    public static final String SELECTED_DATA_STRUCTURE = "io.wizard.selected-data-structure";

    /**
     * The map object family of the data to import.
     */
    public static final String MAP_OBJECT_FAMILY = "io.wizard.map-object-family";

    /**
     * The map object type of the data to import.
     */
    public static final String MAP_OBJECT_TYPE = "io.wizard.map-object-type";

    /**
     * The message type of the data to import.
     */
    public static final String MESSAGE_TYPE = "io.wizard.message-type";

    /**
     * The unit ID of the data to import.
     */
    public static final String UNIT_ID = "io.wizard.unit-id";

    /**
     * True if the wizard is creating a new map object
     */
    public static final String NEW_MAP_OBJECT = "io.wizard.new-map-object";

    /**
     * True if the user has selected to exclude exporting data with a (0,0) latitude,longitude
     */
    public static final String EXCLUDE_ZERO_ZERO_POINTS = "io.wizard.exclude-zero-zero-points";

    /**
     * True if the selected file in the wizard should be copied to the VFS prior to any operation.
     * @since 5.7
     */
    public static final String COPY_TO_VFS = "COPY_TO_VFS";

    /**
     * True if the user has indicated the wizard operation should be cancelled.
     * @since 5.7
     */
    public static final String CANCEL_IMPORT = "CANCEL_IMPORT";

    /**
     * Prevent instance creation.
     */
    private CommonWizardSettings()
    {
    }
}
