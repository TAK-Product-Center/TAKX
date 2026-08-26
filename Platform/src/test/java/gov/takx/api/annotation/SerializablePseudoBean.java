package gov.takx.api.annotation;

import gov.takx.api.configuration.IProgramEnvironment;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Class used to test that a class marked @PseudoBean is properly injected when deserialized.  This class does
 * NOT declare a {@code readResolve()} method.
 *
 * @since 2.25
 */
@PseudoBean
public class SerializablePseudoBean implements Serializable
{
    static int pcCount = 0;
    int foo;

    @Inject
    transient IProgramEnvironment programEnvironment; // not part of serialized content

    @PostConstruct
    private void postConstruct()
    {
        pcCount += 1;
    }
}

/**
 * Test class that declares a {@code readResolve()} method to ensure both forms are matched by the aspects.
 */
@PseudoBean
class SerializablePseudoBeanReadResolve implements Serializable
{
    static int pcCount = 0;
    int foo;

    @Inject
    transient IProgramEnvironment programEnvironment; // not part of serialized content

    @PostConstruct
    private void postConstruct()
    {
        pcCount += 1;
    }

    private Object readResolve() throws ObjectStreamException
    {
        return this;
    }
}
