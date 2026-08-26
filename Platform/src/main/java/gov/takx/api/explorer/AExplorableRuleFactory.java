package gov.takx.api.explorer;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base class implementation of the {@link IExplorableRuleFactory} interface.
 *
 * @since 1.48
 */
public abstract class AExplorableRuleFactory<T extends IExplorableRule> implements IExplorableRuleFactory<T>
{
    private final String displayName;
    private final String typeName;
    private final AtomicInteger instanceCount;

    /**
     * Construct a Rule Factory using the type name of the factory.
     *
     * @param displayName The name displayed to the user.
     * @param type        The type name.
     */
    public AExplorableRuleFactory(String displayName, String type)
    {
        this.displayName = displayName;
        typeName = type;
        instanceCount = new AtomicInteger(0);
    }

    @Override
    public T createRule()
    {
        return createRule(getDisplayName() + " " + instanceCount.incrementAndGet());
    }

    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public String getType()
    {
        return typeName;
    }

    protected abstract T createRule(String name);
}
