package gov.takx.platform.query;

import gov.takx.api.query.ASimpleFieldSelector;
import gov.takx.api.query.ComparisonType;
import gov.takx.api.query.FieldConstraint;
import gov.takx.api.query.IConstraint;
import gov.takx.api.query.IQueryConstraints;
import gov.takx.api.query.LogicConstraint;
import gov.takx.api.query.LogicType;
import gov.takx.api.query.ShapeConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Abstract base class for field-based constraint matching.
 *
 * @param <M> The type of object being tested (i.e., the source of the field values to compare against the constraint)
 * @since 1.49
 */
public abstract class AFieldMatcher<M>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Chars in SQL LIKE patterns that need to be escaped when put into a regex
    private static final String REGEX_SPECIAL_CHARS = ".^$*+?|()[]{}\\";

    // Map from SQL LIKE expressions to regexp equivalent
    private static final Map<String, Pattern> sql2regexp = new ConcurrentHashMap<>();

    // Function to extract the field value specified in a FieldConstraint from a given source object
    private final BiFunction<FieldConstraint, M, Object> fieldValueExtractor = (constraint, o) -> constraint.usesExtendedFields()
            ? getExtendedFieldValue(constraint.getField(), o)
            : getCoreFieldValue(constraint.getField(), o);

    private final Predicate<M> allPredicates;

    /**
     * Construct a field matcher using the given query constraints.
     *
     * @param qc Query constraints to match against
     */
    protected AFieldMatcher(IQueryConstraints qc)
    {
        allPredicates = nullToTrue(compileFromQueryConstraints(qc));
    }

    /**
     * Construct a field matcher using the given list of constraints.
     *
     * @param constraints Constraints to match against
     */
    protected AFieldMatcher(List<IConstraint> constraints)
    {
        allPredicates = nullToTrue(compileConstraints(constraints));
    }

    /**
     * Construct a field matcher using the given collection of lists of constraints.
     *
     * @param constraintSets Constraints to match against
     */
    protected AFieldMatcher(Collection<List<IConstraint>> constraintSets)
    {
        allPredicates = nullToTrue(compileConstraintsCollection(constraintSets));
    }

    /**
     * Determine if the target matches all the configured constraints.
     *
     * @param target Target object to test
     * @return {@code true} if all constraints are satisfied
     */
    protected boolean doMatch(M target)
    {
        return allPredicates.test(target);
    }

    /**
     * Compile the given query constraints into a predicate that validates the constraints.
     *
     * @param qc Query constraints to compile
     * @return Compound predicate that validates all constraints
     */
    private Predicate<M> compileFromQueryConstraints(IQueryConstraints qc)
    {
        Predicate<M> predicate = andPredicate(null, compileConstraintsCollection(qc.getCoreFieldConstraints()));
        predicate = andPredicate(predicate, compileConstraintsCollection(qc.getExtendedFieldConstraints()));
        predicate = andPredicate(predicate, compileConstraints(qc.getLogicConstraints()));
        predicate = andPredicate(predicate, compileConstraints(qc.getShapeConstraints()));

        return predicate;
    }

    /**
     * Convert a {@code null} predicate to a 'true' predicate.
     *
     * @param predicate Predicate to convert if needed
     * @return The given predicate if it is non-null, otherwise a predicate that returns {@code true}
     * @since 2.30
     */
    private Predicate<M> nullToTrue(Predicate<M> predicate)
    {
        return predicate == null ? o -> true : predicate;
    }

    /**
     * Produce a predicate that is the conjunction of two predicates, either of which might be {@code null}, and where
     * {@code null} implies true. If both are {@code null}, then return {@code null}.
     *
     * @param p        Initial predicate
     * @param conjunct Predicate to add as conjunct
     * @return predicate
     */
    private Predicate<M> andPredicate(Predicate<M> p, Predicate<M> conjunct)
    {
        return p == null
                ? conjunct
                : conjunct != null ? p.and(conjunct) : p;
    }

    /**
     * Compile a collection of lists of constraints into a predicate that validates all the constraints.
     *
     * @param constraintSets Collection of constraint lists to compile
     * @return matching predicate
     */
    private Predicate<M> compileConstraintsCollection(Collection<List<IConstraint>> constraintSets)
    {
        Predicate<M> p = null;

        for (List<IConstraint> constraintSet : constraintSets)
        {
            p = andPredicate(p, compileConstraints(constraintSet));
        }

        return p;
    }

    /**
     * Compile a list of constraints into a predicate that validates all the constraints.
     *
     * @param constraints constraint list to compile
     * @return matching predicate
     */
    private Predicate<M> compileConstraints(List<IConstraint> constraints)
    {
        Predicate<M> p = null;

        for (IConstraint constraint : constraints)
        {
            p = andPredicate(p, compileConstraint(constraint));
        }

        return p;
    }

    /**
     * Compile a constraint into a predicate that validates it.
     *
     * @param constraint Constraint to compile
     * @return matching predicate
     */
    private Predicate<M> compileConstraint(IConstraint constraint)
    {
        if (constraint instanceof FieldConstraint)
        {
            return compileFieldConstraint((FieldConstraint) constraint);
        } else if (constraint instanceof LogicConstraint)
        {
            return compileLogicConstraint((LogicConstraint) constraint);
        } else if (constraint instanceof ShapeConstraint)
        {
            return compileShapeConstraint((ShapeConstraint) constraint);
        } else
        {
            logger.error("Unknown constraint type: {}", constraint.getClass());
            return null;
        }
    }

    /**
     * Compile a FieldConstraint into a predicate that validates it.
     *
     * @param constraint FieldConstraint to compile
     * @return matching predicate
     */
    private Predicate<M> compileFieldConstraint(FieldConstraint constraint)
    {
        final ComparisonType comparator = constraint.getComparator();

        if (comparator.usesValue())
        {
            if (comparator == ComparisonType.LIKE || comparator == ComparisonType.NOTLIKE)
            {
                final String sqlLikePattern = (String) constraint.getValue();
                return o -> compareLike(fieldValueExtractor.apply(constraint, o), comparator, sqlLikePattern);
            } else
            {
                return constraint.usesExtendedFields()
                        ? o -> testConstraint(this::getExtendedFieldValue, constraint, o)
                        : createCoreFieldComparison(constraint);
            }
        } else
        {
            // Handle NULL / NOTNULL tests
            return comparator == ComparisonType.ISNULL
                    ? (o -> null == fieldValueExtractor.apply(constraint, o))
                    : (o -> null != fieldValueExtractor.apply(constraint, o));
        }
    }

    /**
     * Extract a field value and perform the comparison specified in the field constraint.  Operations use the
     * value as a generic {@link Comparable}, meaning primitive field types are given no special treatment
     * and will be converted to wrapper objects.
     *
     * @param valueExtractor BiFunction to extract the field value, passed the field selector and target object
     * @param constraint     Constraint to test against
     * @param o              Object from which to extract the field value
     * @return {@code true} if the field value matches the constraint, {@code false} otherwise
     * @since 3.0
     */
    protected boolean testConstraint(BiFunction<ASimpleFieldSelector, M, Object> valueExtractor, FieldConstraint constraint, M o)
    {
        //noinspection unchecked
        final Comparable<Object> value = (Comparable<Object>) valueExtractor.apply(constraint.getField(), o);
        return value != null && constraint.getComparator().interpretComparison(value.compareTo(constraint.getValue()));
    }

    /**
     * Compile a LogicConstraint into a predicate that validates it.
     *
     * @param constraint LogicConstraint to compile
     * @return matching predicate
     */
    private Predicate<M> compileLogicConstraint(LogicConstraint constraint)
    {
        final LogicType logicType = constraint.getLogicType();
        final List<IConstraint> subConstraints = constraint.getSubconstraints();

        if (logicType == LogicType.not) // Negate subConstraints
        {
            return compileConstraints(subConstraints).negate();
        } else if (logicType == LogicType.and || logicType == LogicType.or)
        {
            final boolean conjunction = logicType == LogicType.and;
            Predicate<M> p = null;

            for (IConstraint subConstraint : subConstraints)
            {
                final Predicate<M> subPredicate = compileConstraint(subConstraint);
                if (subPredicate != null)
                {
                    p = p == null
                            ? subPredicate
                            : (conjunction ? p.and(subPredicate) : p.or(subPredicate));
                }
            }
            return p;
        } else
        {
            logger.error("Unknown logic type: {}", logicType);
            return o -> false; // Assume fail on error
        }
    }

    /**
     * Compile a ShapeConstraint into a predicate that validates it.
     *
     * @param constraint ShapeConstraint to compile
     * @return matching predicate
     */
    protected abstract Predicate<M> compileShapeConstraint(ShapeConstraint constraint);

    /**
     * Get the value of the indicated core field.
     *
     * @param field           field to retrieve
     * @param predicateTarget The target being tested by the current predicate
     * @return Field value, {@code null} if not present
     */
    protected abstract Object getCoreFieldValue(ASimpleFieldSelector field, M predicateTarget);

    /**
     * Get the value of the indicated extended field.
     *
     * @param field           field to retrieve
     * @param predicateTarget The target being tested by the current predicate
     * @return Field value, {@code null} if not present
     */
    protected abstract Object getExtendedFieldValue(ASimpleFieldSelector field, M predicateTarget);

    /**
     * Create the predicate to compare a core field.  The default implementation treats the field values as objects,
     * resulting in wrapper creation for any primitive types. Derived classes should optimize the operations
     * on primitive types to avoid unnecessary wrapper object creation.
     *
     * @param constraint The Field Constraint to process
     * @return Predicate to test that constraint
     * @since 2.30
     */
    protected Predicate<M> createCoreFieldComparison(FieldConstraint constraint)
    {
        return o -> testConstraint(this::getCoreFieldValue, constraint, o);
    }

    /**
     * Compare a field value using a LIKE/NOT LIKE operator.  Handle conversion from SQL patterns to regex.
     *
     * @param fieldValue     Field value to compare
     * @param comparator     Comparator (LIKE/NOT LIKE)
     * @param sqlLikePattern SQL LIKE Pattern to compare against
     * @return {@code true} if the values match
     */
    protected boolean compareLike(Object fieldValue, ComparisonType comparator, String sqlLikePattern)
    {
        if (fieldValue == null) return comparator == ComparisonType.NOTLIKE; // null is not like anything

        final Pattern regex = sql2regexp.computeIfAbsent(sqlLikePattern, k -> sqlPatternToRegex(sqlLikePattern));
        final boolean matches = regex.matcher(String.valueOf(fieldValue)).matches();

        return (comparator == ComparisonType.LIKE) == matches;
    }

    /**
     * Convert a SQL-style pattern to a regexp pattern.
     *
     * @param sql SQL pattern to convert
     * @return regex pattern equivalent
     */
    protected static Pattern sqlPatternToRegex(String sql)
    {
        StringBuilder sb = new StringBuilder();

        for (int j = 0, len = sql.length(); j < len; j++)
        {
            char nextChar = sql.charAt(j);

            if (nextChar == '%')
            {
                sb.append(".*");
            } else if (nextChar == '_')
            {
                sb.append('.');
            } else if (REGEX_SPECIAL_CHARS.indexOf(nextChar) >= 0)
            {
                sb.append('\\').append(nextChar); // escape special chars
            } else
            {
                sb.append(nextChar);
            }
        }

        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
    }
}
