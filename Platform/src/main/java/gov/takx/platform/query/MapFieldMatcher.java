package gov.takx.platform.query;

import gov.takx.api.query.ASimpleFieldSelector;
import gov.takx.api.query.IConstraint;
import gov.takx.api.query.IQueryConstraints;
import gov.takx.api.query.ShapeConstraint;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Field-based constraint matching for data maps (field name to value maps).
 * <p>
 * Instances of this matcher are intended to be reused on arbitrary data maps, so the constraints are compiled up front
 * to minimize overhead on subsequent {@link #matches(Map)} operations.
 *
 * @since 1.49
 */
public class MapFieldMatcher extends AFieldMatcher<Map<String, Object>>
{
    /**
     * Construct a field matcher using the given list of constraints.
     *
     * @param constraints Constraints to match against
     */
    public MapFieldMatcher(List<IConstraint> constraints)
    {
        super(constraints);
    }

    /**
     * Construct a field matcher using the given collection of lists of constraints.
     *
     * @param constraintSets Constraints to match against
     */
    public MapFieldMatcher(Collection<List<IConstraint>> constraintSets)
    {
        super(constraintSets);
    }

    /**
     * Construct a field matcher using the given query constraints.
     *
     * @param qc Query constraints to match against
     */
    public MapFieldMatcher(IQueryConstraints qc)
    {
        super(qc);
    }

    /**
     * Determine if the map data matches all the configured constraints.
     *
     * @param map Data to test
     * @return {@code true} if all constraints are satisfied
     */
    public boolean matches(Map<String, Object> map)
    {
        return doMatch(map);
    }

    @Override
    protected Predicate<Map<String, Object>> compileShapeConstraint(ShapeConstraint constraint)
    {
        throw new IllegalStateException("Can't test shape constraints without a message");
    }

    @Override
    protected Object getCoreFieldValue(ASimpleFieldSelector field, Map<String, Object> map)
    {
        return map.get(field.getAlias());
    }

    @Override
    protected Object getExtendedFieldValue(ASimpleFieldSelector field, Map<String, Object> map)
    {
        return map.get(field.getAlias());
    }
}
