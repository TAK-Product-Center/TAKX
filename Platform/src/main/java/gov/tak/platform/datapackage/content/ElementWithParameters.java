package gov.tak.platform.datapackage.content;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Holder for a list of {@link Parameter} objects.
 *
 * @since 3.0
 */
@SuppressWarnings("FieldMayBeFinal")
@XmlAccessorType(XmlAccessType.FIELD)
public class ElementWithParameters
{
    /**
     * Parameter name of 'name'.
     *
     * @since 3.5
     */
    public final static String PARAMETER_NAME = "name";

    /**
     * Parameter name of 'uid'.
     *
     * @since 3.5
     */
    public final static String PARAMETER_UID = "uid";

    /**
     * Parameter name of 'remarks'.
     *
     * @since 3.5
     */
    public final static String PARAMETER_REMARKS = "remarks";

    @XmlElement(name = "Parameter")
    private List<Parameter> parameters = new ArrayList<>();

    protected ElementWithParameters()
    {
    }

    /**
     * @return Current parameter value for 'name'.
     *
     * @since 3.5
     */
    @Nullable
    public String getName()
    {
        return getParameterValue(PARAMETER_NAME);
    }

    /**
     * @return Current parameter value for 'uid'.
     *
     * @since 3.5
     */
    @Nullable
    public String getUid()
    {
        return getParameterValue(PARAMETER_UID);
    }

    /**
     * @return Current parameter value for 'remarks'.
     *
     * @since 3.5
     */
    @Nullable
    public String getRemarks()
    {
        return getParameterValue(PARAMETER_REMARKS);
    }

    /**
     * @param value New value for the 'name' parameter.
     *
     * @since 3.5
     */
    public void setName(String value)
    {
        setParameter(PARAMETER_NAME, value);
    }

    /**
     * @param value New value for the 'uid' parameter.
     *
     * @since 3.5
     */
    public void setUid(String value)
    {
        setParameter(PARAMETER_UID, value);
    }

    /**
     * @param value New value for the 'remarks' parameter.
     *
     * @since 3.5
     */
    public void setRemarks(String value)
    {
        setParameter(PARAMETER_REMARKS, value);
    }

    /**
     * Get the value of the parameter with the given name.
     *
     * @param name Name of parameter to retrieve
     *
     * @return Parameter value, or {@code null} if not found
     */
    @Nullable
    public String getParameterValue(@Nonnull String name)
    {
        return findParameter(name).map(Parameter::getValue).orElse(null);
    }

    /**
     * Add a parameter, replacing the value on any existing parameter with the same name.
     *
     * @param parameter Parameter to add/update
     */
    public void setParameter(@Nonnull Parameter parameter)
    {
        final Optional<Parameter> found = findParameter(parameter.getName());

        if (found.isPresent())
        {
            found.get().setValue(parameter.getValue());
        } else
        {
            parameters.add(parameter);
        }
    }

    /**
     * Add a parameter with the given name and value.  If a parameter exists with the given name, its value will
     * be updated.
     *
     * @param name Name of Parameter to add/update
     * @param value parameter value
     */
    public void setParameter(@Nonnull String name, @Nonnull String value)
    {
        setParameter(new Parameter(name, value));
    }

    /**
     * @return Optional of Parameter with the given name
     */
    private Optional<Parameter> findParameter(String name)
    {
        return parameters.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof ElementWithParameters)) return false;

        ElementWithParameters that = (ElementWithParameters) o;

        return parameters.equals(that.parameters);
    }

    @Override
    public int hashCode()
    {
        return parameters.hashCode();
    }

    @Override
    public String toString()
    {
        return String.join(", ", parameters.stream().map(Parameter::toString).toList());
    }
}
