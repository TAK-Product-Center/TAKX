package gov.takx.api.url;

/**
 * Constants defining the URL schemes supported by our custom handlers.
 * 
 * @since 5.2 (moved to Platform in 5.7.1)
 */
public class Schemes
{
    /**
     * Scheme indicating a base64 encoded byte array.
     * @since 5.6
     */
    public static final String BASE64 = "base64";

    /**
     * Scheme indicating an entry in an icon set.
     */
    public static final String ICONSET = "iconset";

    /**
     * Scheme indicating a UI resource.
     * @since 5.7.1
     */
    public static final String UI_RESOURCE = "uiresource";

    /**
     * Scheme indicating a UI resource that is specifically an SVG.
     * @since 5.8
     */
    public static final String UI_RESOURCE_SVG = "uiresourcesvg";

    /**
     * Scheme indicating files stored in the project VFS.
     */
    public static final String VFS = "vfs";

    /**
     * Scheme indicating files stored in the project VFS, including the trailing {@code ':'}.
     */
    public static final String VFS_PREFIX = VFS + ":";
}
