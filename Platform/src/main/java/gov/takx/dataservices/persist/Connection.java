package gov.takx.dataservices.persist;

import gov.takx.api.persist.IPersistable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Represents a Connection established by the user.
 *
 * @since 1.3
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"uniqueKey"}))
public class Connection implements IPersistable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String family;
    private String type;
    private String name;

    /**
     * Used to retrieve connection plugin state.
     */
    private String uniqueKey;

    public Connection()
    {
    }

    public Connection(String family, String type, String uniqueKey, String name)
    {
        logger.debug("Creating connection: key={}, name={}", uniqueKey, name);

        this.family = family;
        this.type = type;
        this.uniqueKey = uniqueKey;
        this.name = name;
    }

    @Override
    public Integer getId()
    {
        return id;
    }

    public String getFamily()
    {
        return family;
    }

    public String getUniqueKey()
    {
        return uniqueKey;
    }

    public String getType()
    {
        return type;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return "Family: " + family + ", Type: " + type + ", Unique Key: " + uniqueKey + ", Name: " + name;
    }

    @Override
    public int hashCode()
    {
        return uniqueKey.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Connection that = (Connection) o;

        return uniqueKey.equals(that.uniqueKey);
    }
}
