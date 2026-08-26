package gov.tak.platform.datapackage.content;

import jakarta.annotation.Nonnull;
import jakarta.xml.bind.annotation.XmlAttribute;

import java.util.Objects;

/**
 * Describes a single content element within a data package manifest. Content elements can have parameter values.
 *
 * @since 3.0
 */
public class PackageManifestContent extends ElementWithParameters
{
    // Unique ID within the manifest, labelled "zipEntry" in the Mission Package XML v2
    @SuppressWarnings("unused")
    @XmlAttribute(name = "zipEntry", required = true)
    private String manifestUid;

    @SuppressWarnings("FieldMayBeFinal")
    @XmlAttribute
    private boolean ignore = false;

    /**
     * @return The UID (zip entry) of this Content
     */
    public String getManifestUid()
    {
        return manifestUid;
    }

    /**
     * @param manifestUid The UID (zip entry) of this Content
     *
     * @since 3.5
     */
    public void setManifestUid(@Nonnull String manifestUid)
    {
        this.manifestUid = manifestUid;
    }

    /**
     * @return {@code true} when the manifest content entry should be ignored when importing the containing data package.
     */
    public boolean isIgnore()
    {
        return ignore;
    }

    /**
     * @param ignore {@code true} to indicate this manifest content entry should be ignored when importing the containing data package.
     *
     * @since 3.5
     */
    public void setIgnore(boolean ignore)
    {
        this.ignore = ignore;
    }

    @Override
    public String toString()
    {
        return manifestUid + " (" + super.toString() + ")";
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PackageManifestContent that = (PackageManifestContent) o;

        if (ignore != that.ignore) return false;
        return Objects.equals(manifestUid, that.manifestUid) && super.equals(o);
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + (manifestUid != null ? manifestUid.hashCode() : 0);
        result = 31 * result + (ignore ? 1 : 0);
        return result;
    }
}
