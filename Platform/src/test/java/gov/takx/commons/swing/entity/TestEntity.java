package gov.takx.commons.swing.entity;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @since 5.7
 */
public class TestEntity
{
    public static final String NAME_PROP = "TEST";

    PropertyChangeListener listener; // This odd way of handling the listener is so we dont have to include support classes here

    private String name;
    private Integer id;
    private String empty;
    private String details;

    public TestEntity(String name, Integer id)
    {
        this.name = name;
        this.id = id;
        details = name + " details";
    }

    public String getEmpty()
    {
        return empty;
    }

    public void setEmpty(String empty)
    {
        this.empty = empty;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        var old = this.name;
        this.name = name;
        listener.propertyChange(new PropertyChangeEvent(this, NAME_PROP, old, name));
    }

    public Integer getId()
    {
        return id;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public String getDetails()
    {
        return details;
    }

    public void setDetails(String details)
    {
        this.details = details;
    }
}
