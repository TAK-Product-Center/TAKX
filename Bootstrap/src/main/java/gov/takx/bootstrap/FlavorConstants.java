package gov.takx.bootstrap;

/**
 * Constants concerning the flavor of TAKX at runtime.
 * @since 5.2
 */
public class FlavorConstants
{
    /**
     * Constant indicating the MIL (Military) flavor.
     */
    public static final String MIL = "MIL";

    /**
     * Constant indicating the CIV (Civil) flavor.
     */
    public static final String CIV = "CIV";

    /**
     * Constant indicating the GOV (Government) flavor.
     * @since 5.5
     */
    public static final String GOV = "GOV";

    /**
     * Jar manifest attribute specifying the execution flavor.  If not present, {@link #CIV} is assumed.
     * Must be present and have a value of {@link #MIL} to enable MIL execution.
     */
    public static final String MF_ATTR_TAKX_FLAVOR = "TAKX-Flavor";

}
