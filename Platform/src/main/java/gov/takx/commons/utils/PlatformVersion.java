package gov.takx.commons.utils;

import gov.takx.bootstrap.Version;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Comparator;

/**
 * Identifies a specific TAKX version.  This may be the currently running version, or it may
 * be a version specified for other purposes, such as by the TAKX core to identify if a plugin's
 * minimum platform version requirements are met by the current running instance.
 *
 * @since 1.3
 */
@SuppressWarnings({"PMD.StaticFieldsUpperCaseExceptLogger", "UnusedDeclaration"})
public final class PlatformVersion implements Comparable<PlatformVersion>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The current version as a string.
     *
     * @since 3.0
     */
    public static final String CURRENT_VERSION_STRING = Version.getVersionString();

    /**
     * The current PlatformVersion.
     *
     * @since 3.0
     */
    public static final PlatformVersion CURRENT_PLATFORM_VERSION = new PlatformVersion(CURRENT_VERSION_STRING);

    public static final PlatformVersion Version_3_0 = new PlatformVersion(3, 0, 1);

    // Used to compare two versions by major, minor, and then patch
    private static final Comparator<PlatformVersion> PLATFORM_VERSION_COMPARATOR = Comparator
            .comparingInt(PlatformVersion::getMajor)
            .thenComparingInt(PlatformVersion::getMinor)
            .thenComparingInt(PlatformVersion::getPatch);

    private static final String SPLIT_PATTERN = "\\.";

    private static final String MALFORMED_VERSION_FORMAT =
            "Malformed version string '%s'; must be in the form of MAJOR[.MINOR[.PATCH]] with all parts numeric.";

    private final int major;
    private final int minor;
    private final int patch;

    /**
     * Constructs an immutable PlatformVersion instance.
     *
     * @param major Major version number
     * @param minor Minor version number
     * @param patch Release number
     */
    public PlatformVersion(int major, int minor, int patch)
    {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * Constructor accepting a string with an expected format of MAJOR[.MINOR[.PATCH]].  The major version must be
     * specified, missing minor and patch values will be defaulted to 0.
     *
     * @param versionString Version string to parse
     */
    public PlatformVersion(@Nonnull String versionString)
    {
        final String[] parts = versionString.split(SPLIT_PATTERN);

        // Allow 1 - 3 parts, defaulting to a minor and patch of 0 if not given
        if (parts.length < 1 || parts.length > 3)
        {
            throw new IllegalArgumentException(String.format(MALFORMED_VERSION_FORMAT, versionString));
        }

        try
        {
            major = Integer.parseInt(parts[0]);
            minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        } catch (NumberFormatException e)
        {
            throw new IllegalArgumentException(String.format(MALFORMED_VERSION_FORMAT, versionString));
        }
    }

    /**
     * @return major version number
     */
    public int getMajor()
    {
        return major;
    }

    /**
     * @return minor version number
     */
    public int getMinor()
    {
        return minor;
    }

    /**
     * @return patch version number
     */
    public int getPatch()
    {
        return patch;
    }

    @Override
    public int hashCode()
    {
        return (37 * major) + (37 * minor) + patch;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlatformVersion that = (PlatformVersion) o;

        return major == that.major && minor == that.minor && patch == that.patch;
    }

    @Override
    public String toString()
    {
        return major + "." + minor + "." + patch;
    }

    @Override
    public int compareTo(@Nonnull PlatformVersion o)
    {
        return PLATFORM_VERSION_COMPARATOR.compare(this, o);
    }

    /**
     * Compare two version strings.
     *
     * @param v1 Version string
     * @param v2 Version string to compare against
     * @return Comparison result: &lt; 0, 0, or &gt; 0
     * @see #PlatformVersion(String)
     * @see #compareTo(PlatformVersion)
     */
    public static int compare(@Nonnull String v1, @Nonnull String v2)
    {
        return new PlatformVersion(v1).compareTo(new PlatformVersion(v2));
    }
}
