package gov.tak.platform.datapackage.content;

import jakarta.annotation.Nonnull;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the list of content elements from the package manifest.
 *
 * @see PackageManifestContent
 * @since 3.0
 */
public class PackageManifestContents
{
    @SuppressWarnings("FieldMayBeFinal")
    @XmlElement(name = "Content")
    private List<PackageManifestContent> contents = new ArrayList<>();

    /**
     * @return Collection of PackageManifestContent elements
     */
    @Nonnull
    public List<PackageManifestContent> getContents()
    {
        return Collections.unmodifiableList(contents);
    }

    /**
     * Add the given content element.
     *
     * @param content Item to add to the manifest.
     */
    public void addContent(@Nonnull PackageManifestContent content)
    {
        contents.add(content);
    }

    /**
     * Remove the given content element.
     *
     * @param content Item to remove from the manifest.
     *
     * @since 3.5
     */
    public void removeContent(@Nonnull PackageManifestContent content)
    {
        contents.remove(content);
    }
}
