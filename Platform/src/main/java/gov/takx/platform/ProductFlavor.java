package gov.takx.platform;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * Enum for determining the build flavor.
 *
 * @since 5.0
 * @deprecated Flavor is now represented by an arbitrary String
 */
@Deprecated(since = "5.5", forRemoval = true)
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public enum ProductFlavor
{
    CIV,
    MIL,
}
