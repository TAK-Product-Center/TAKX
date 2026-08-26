package gov.takx.commons.utils;

import gov.takx.commons.constants.State;
import jakarta.annotation.Nonnull;

/**
 */
@SuppressWarnings({"ComparableImplementedButEqualsNotOverridden", "ComparableType", "rawtypes"})
public class Threshold implements Comparable<Object>
{
    private final Comparable maxValue;
    private final State state;
    private final boolean inclusive;
    private final boolean isNumericType;

    public Threshold(@Nonnull Comparable<?> maxValue, @Nonnull State state, boolean inclusive)
    {
        this.maxValue = maxValue;
        this.state = state;
        this.inclusive = inclusive;

        // Determine if we're processing a numeric type, note that Dates are considered a numeric type
        final Class<?> valueClass = maxValue.getClass();
        isNumericType = valueClass != Boolean.class && valueClass != String.class;

        if (!isNumericType && !inclusive)
            throw new IllegalArgumentException("Non-numeric thresholds can not have 'inclusive' == false");
    }

    public Object getMaxThreshold()
    {
        return maxValue;
    }

    public State getState()
    {
        return state;
    }

    public boolean isInclusive()
    {
        return inclusive;
    }

    public boolean matches(Object value)
    {
        final int comp = -compareTo(value);

        // Non-numeric types must compare equal
        return (isNumericType && comp < 0) || (comp == 0 && inclusive);
    }

    /**
     * This implementation of {@code compareTo} allows for comparing against another {@link Threshold} or any other
     * {@link Comparable} value, which is compared against our {@code maxValue}.
     * <br>
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public int compareTo(@Nonnull Object o)
    {
        return o instanceof Threshold
                ? maxValue.compareTo(((Threshold) o).maxValue)
                : maxValue.compareTo(o);
    }

    @Override
    public String toString()
    {
        return "Threshold{" +
                "maxValue=" + maxValue +
                ", state=" + state +
                ", inclusive=" + inclusive +
                '}';
    }
}
