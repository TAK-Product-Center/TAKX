package gov.takx.dataservices.persist;

import gov.takx.api.messages.IPluginState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.Length;

/**
 * Hibernate annotated class to persist and restore the arbitrary state of TAKX plugin.
 *
 * @since 1.3
 */

@Entity
public class PluginState implements IPluginState
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String uniqueKey;

    @Column(length = Length.LONG32)
    private String state;

    /**
     * No-arg constructor for Hibernate.
     */
    protected PluginState()
    {
    }

    /**
     * Construct a new state object.
     *
     * @param uniqueKey Unique key used to index the state
     * @param state     Arbitrary state string
     */
    public PluginState(String uniqueKey, String state)
    {
        this.uniqueKey = uniqueKey;
        this.state = state;
    }

    @Override
    public String getUniqueKey()
    {
        return uniqueKey;
    }

    @Override
    public void setUniqueKey(String uniqueKey)
    {
        this.uniqueKey = uniqueKey;
    }

    @Override
    public String getState()
    {
        return state;
    }

    @Override
    public void setState(String state)
    {
        this.state = state;
    }

    @Override
    public Integer getId()
    {
        return id;
    }
}
