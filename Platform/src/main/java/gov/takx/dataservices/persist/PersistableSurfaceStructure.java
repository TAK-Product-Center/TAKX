package gov.takx.dataservices.persist;

import gov.tak.api.util.AttributeSet;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.structures.IPersistableSurfaceStructure;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.dataservices.utils.AttributeSetConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import org.hibernate.Length;

import java.util.UUID;

/**
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class PersistableSurfaceStructure implements IPersistableSurfaceStructure
{
    // Hibernate 6 fails to properly initialize the metadata for this class hierarchy with the original "default"
    // strategy for @GeneratedValue.  I had to make it use "IDENTITY" to get this to work. No, there was nothing I could
    // find online about the failure, this was just a guess from the wording used in the error message.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    protected UUID uuid = UUIDUtils.generateType1UUID();
    protected String name = null;
    protected String description = null;
    protected String url = null;
    protected RaptorLocation location;

    @ManyToOne(targetEntity = RaptorLayer.class)
    protected IRaptorLayer layer;

    @Convert(converter = AttributeSetConverter.class)
    @Column(length = Length.LONG32)
    private AttributeSet metadata = new AttributeSet();

    protected PersistableSurfaceStructure()
    {
    }

    @Override
    public UUID getUUID()
    {
        return uuid;
    }

    @Override
    public Integer getId()
    {
        return id;
    }

    @Override
    public IRaptorLayer getLayer()
    {
        return layer;
    }

    @Override
    public void setLayer(IRaptorLayer layer)
    {
        this.layer = layer;
    }

    @Override
    public ILocation getLocation()
    {
        return location;
    }

    @Override
    public void setLocation(RaptorLocation location)
    {
        this.location = location;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public void setDescription(String description)
    {
        this.description = trimToLimit(description);
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void setName(String name)
    {
        this.name = trimToLimit(name);
    }

    @Override
    public String getUrl()
    {
        return url;
    }

    @Override
    public void setUrl(String url)
    {
        this.url = trimToLimit(url);
    }

    @Override
    public String toString()
    {
        return name != null ? name : uuid.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return uuid.equals(((PersistableSurfaceStructure) o).uuid);
    }

    @Override
    public int hashCode()
    {
        return uuid.hashCode();
    }

    @Override
    public AttributeSet getMetadata()
    {
        return metadata;
    }

    /**
     * Trim a given string to the default length limit on strings in the DB (which is 255)
     *
     * @param s String to process
     * @return The original s if {@code null} or its length is < 255, otherwise the string truncated at 255 characters
     * @since 3.0
     */
    private String trimToLimit(String s)
    {
        return s != null && s.length() > 255
                ? s.substring(0, 255)
                : s;
    }
}
