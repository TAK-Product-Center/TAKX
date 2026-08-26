package gov.takx.api.filesystem;

/**
 * Constants used in the Virtual File System.
 *
 * @since 1.3 (renamed from IVirtualFileSystemBase in 3.0)
 */
public class VfsConstants
{
    public static final String DEVICE_NAMESPACE = "Devices";
    public static final String APP_NAMESPACE = "Apps";
    public static final String CONNECTION_NAMESPACE = "Connections";
    public static final String IMPORT_EXPORT_NAMESPACE = "IO";

    /**
     * Root at which attachments (dropped files, files attached as media, etc.) are stored.
     * @since 3.4
     */
    public static final String ATTACHMENTS_NAMESPACE = "Attachments";

    /**
     * Root at which glyphs (for track styling) are stored.
     * @since 3.4
     */
    public static final String GLYPHS_NAMESPACE = "Glyphs";

    public static final String FILES_DIR = "/Files";

    private VfsConstants()
    {
    }
}
