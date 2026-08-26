package gov.tak.platform.datapackage.content;

import jakarta.annotation.Nonnull;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

/**
 * Simple name/value holder for use in XML handling.
 *
 * @since 3.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Parameter
{
    @XmlAttribute
    private String name;

    @XmlAttribute
    private String value;

    private Parameter()
    {
    }

    public Parameter(@Nonnull String name, @Nonnull String value)
    {
        this.name = name;
        this.value = value;
    }

    @Nonnull
    public String getName()
    {
        return name;
    }

    @Nonnull
    public String getValue()
    {
        return value;
    }

    public void setValue(@Nonnull String value)
    {
        this.value = value;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Parameter parameter = (Parameter) o;

        if (!name.equals(parameter.name)) return false;
        return value.equals(parameter.value);
    }

    @Override
    public int hashCode()
    {
        int result = name.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return name + "=" + value;
    }
}
