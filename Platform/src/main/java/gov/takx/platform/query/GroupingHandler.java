package gov.takx.platform.query;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.annotation.Internal;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.query.ASimpleFieldSelector;
import gov.takx.api.query.AggregateFieldSelector;
import gov.takx.api.query.AggregateType;
import gov.takx.api.query.CoreField;
import gov.takx.api.query.CoreFieldSelector;
import gov.takx.api.query.FieldConstraint;
import gov.takx.api.query.FieldSelector;
import gov.takx.api.query.IConstraint;
import gov.takx.api.query.IQueryConstraints;
import gov.takx.api.query.StructureFieldSelector;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class manages GROUP BY semantics and aggregator functions when a {@link MessageStore} is queried
 * using constraints that include grouping and/or aggregate functions.
 *
 * @since 2.0
 */
@Internal
@Experimental
public class GroupingHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IQueryConstraints constraints;
    private final List<CoreFieldSelector> coreFieldSelectors;
    private final List<StructureFieldSelector> extendedFieldSelectors;
    private final List<AggregateFieldSelector> aggregateFieldSelectors;

    public GroupingHandler(IQueryConstraints constraints, FieldSelector selector)
    {
        this.constraints = constraints;

        coreFieldSelectors = selector.getCoreFieldSelectors();
        extendedFieldSelectors = selector.getExtendedFieldSelectors();
        aggregateFieldSelectors = selector.getAggregateFieldSelectors();
    }

    /**
     * Given an input set of messages, perform the query as configured by our constraints.  Perform all grouping,
     * aggregate calculations, and filtering of results.
     *
     * @param sourceMessages Messages to process
     * @return query results as a map from field alias to value
     */
    public List<Map<String, Object>> performGrouping(Stream<? extends IRaptorDataMessage> sourceMessages)
    {
        // Since we need to process the stream more than once, we need to pull it out into a list :(
        final List<IRaptorDataMessage> messages = sourceMessages.collect(Collectors.toList());

        // Next, extract and organize the data tuples (the group by fields), if any

        final List<Map<String, Object>> results;

        if (!(coreFieldSelectors.isEmpty() && extendedFieldSelectors.isEmpty()))
        {
            final ListMultimap<Tuple, IRaptorDataMessage> rdmsByTuple = MultimapBuilder.hashKeys().arrayListValues().build();
            final List<IConstraint> groupFieldConstraints = getGroupFieldConstraints(constraints);
            final boolean hasGroupFieldConstraints = !groupFieldConstraints.isEmpty();
            final Tuplizer tuplizer = new Tuplizer(coreFieldSelectors, extendedFieldSelectors);

            final MessageFieldMatcher messageFieldMatcher = new MessageFieldMatcher(groupFieldConstraints);
            // As an optimization we can short-circuit the creation of the tuple if any of the group field constraints
            // would rule out a value.  So, test those constraints before building the tuple.

            messages.forEach(rdm -> {
                if (!hasGroupFieldConstraints || messageFieldMatcher.matches(rdm))
                {
                    rdmsByTuple.put(tuplizer.makeTuple(rdm), rdm);
                }
            });

            final int size = rdmsByTuple.keySet().size(); // The number of unique tuples
            results = new ArrayList<>(size);
            final int startIndex = constraints.getStartMessageCount();
            final int limit = constraints.getMaxMessageCount();
            int resultCount = 0;

            // Then, over each group (keyed by tuple), run the aggregator functions

            final MapFieldMatcher rowFieldMatcher = new MapFieldMatcher(constraints.getGroupConstraints());

            for (Map.Entry<Tuple, Collection<IRaptorDataMessage>> entry : rdmsByTuple.asMap().entrySet())
            {
                final Map<String, Object> row = aggregate(entry.getKey(), entry.getValue());

                if (rowFieldMatcher.matches(row))
                {
                    // Test for limit/offset constraints
                    if (resultCount++ >= startIndex && (limit <= 0 || results.size() < limit))
                    {
                        results.add(row);
                    }
                }
            }
        } else
        {
            // Just run the aggregates on the entire collection since there is no GROUP BY in effect
            final MapFieldMatcher rowFieldMatcher = new MapFieldMatcher(constraints.getGroupConstraints());
            final Map<String, Object> row = aggregate(null, messages);

            results = rowFieldMatcher.matches(row)
                    ? Collections.singletonList(row)
                    : Collections.emptyList();
        }

        return results;
    }

    /**
     * Get the collection of simple field constraints from the overall group constraints.
     *
     * @param constraints query constraints
     * @return extracted field constraints, may be empty
     */
    private List<IConstraint> getGroupFieldConstraints(IQueryConstraints constraints)
    {
        return constraints.getGroupConstraints().stream()
                .filter(gc -> gc instanceof FieldConstraint)
                .collect(Collectors.toList());
    }

    /**
     * Produce the result map from the messages associated with a given tuple.  Extract the simple field values from
     * the tuple and run any needed aggregator functions to produce the aggregate field selections.
     *
     * @param tuple   Data tuple
     * @param matches Messages associated with tuple
     * @return map from field alias to result
     */
    private Map<String, Object> aggregate(Tuple tuple, Collection<IRaptorDataMessage> matches)
    {
        final Map<String, Object> resultMap = new HashMap<>();

        // Process any values in our Tuple.  These will be core/extended fields in the Group By clause.
        int i = 0;
        for (CoreFieldSelector selector : coreFieldSelectors)
        {
            resultMap.put(selector.getAlias(), tuple.values[i++]);
        }

        for (StructureFieldSelector selector : extendedFieldSelectors)
        {
            resultMap.put(selector.getAlias(), tuple.values[i++]);
        }

        // Now run the aggregate calculations and store the results

        for (AggregateFieldSelector afs : aggregateFieldSelectors)
        {
            final AggregatorFunction aggregatorFunction = functionFor(afs);

            final ASimpleFieldSelector field = afs.getField();

            // Pass the field values into the aggregator function

            if (field.isCoreField())
            {
                matches.forEach(rdm -> aggregatorFunction.addSample(FieldExtractor.getCoreField(rdm, ((CoreFieldSelector) field).getCoreField())));
            } else
            {
                matches.stream().filter(rdm -> rdm.hasField(afs.getFieldName())) // not worth adding if it wasn't set.  I think.
                        .forEach(rdm -> aggregatorFunction.addSample(rdm.getField(afs.getFieldName())));
            }

            // And store the aggregated value into the result map
            resultMap.put(afs.getAlias(), aggregatorFunction.getValue());
        }

        return resultMap;
    }

    /**
     * Contract for aggregate functions.  Provides for sample addition result retrieval.
     */
    private interface AggregatorFunction
    {
        void addSample(Object sample);

        Object getValue();
    }

    /**
     * Factory method to generate the proper aggregate function based on an aggregate field selector.
     *
     * @param afs Aggregate field selector
     * @return Proper aggregate function
     */
    private AggregatorFunction functionFor(AggregateFieldSelector afs)
    {
        final AggregateType aggregateType = afs.getType();

        switch (aggregateType)
        {
            case AVG:
                return new AvgFunction();
            case COUNT:
                return new CountFunction(afs.isDistinct());
            case MAX:
                return new MaxFunction();
            case MIN:
                return new MinFunction();
            case SUM:
                return new SumFunction();
            case STDDEV_POP:
                return new StddevPopFunction();
            case STDDEV_SAMP:
                return new StddevSampFunction();
            case VAR_POP:
                return new VarPopFunction();
            case VAR_SAMP:
                return new VarSampFunction();
            default:
                logger.error("Unknown AggregateType: {}", aggregateType);
                return new StubFunction();
        }
    }

    /**
     * Generate tuples from RDMs.
     */
    private static final class Tuplizer
    {
        final CoreField[] coreFields;
        final String[] extendedFields;
        final int fieldCount;

        /**
         * Construct a tuplizer using the given core and extended field selectors.
         */
        private Tuplizer(List<CoreFieldSelector> coreFieldSelectors, List<StructureFieldSelector> extendedFieldSelectors)
        {
            coreFields = new CoreField[coreFieldSelectors.size()];
            extendedFields = new String[extendedFieldSelectors.size()];

            fieldCount = coreFields.length + extendedFields.length;

            int i = 0;

            for (CoreFieldSelector fieldSelector : coreFieldSelectors)
            {
                coreFields[i++] = fieldSelector.getCoreField();
            }

            for (StructureFieldSelector fieldSelector : extendedFieldSelectors)
            {
                extendedFields[i++] = fieldSelector.getFieldName();
            }
        }

        /**
         * Generate a tuple from the given message and the configured field selectors.
         *
         * @param rdm Message to process
         * @return Tuple
         */
        private Tuple makeTuple(IRaptorDataMessage rdm)
        {
            final Object[] values = new Comparable[fieldCount];
            int i = 0;

            for (CoreField coreField : coreFields)
            {
                final Object v = FieldExtractor.getCoreField(rdm, coreField);
                values[i++] = v;
            }

            for (String extendedField : extendedFields)
            {
                final Object v = rdm.getField(extendedField);
                values[i++] = v;
            }

            return new Tuple(values);
        }
    }

    /**
     * A simple data tuple.  Supports equality tests so we can perform grouping based on tuples.
     */
    private static final class Tuple
    {
        final Object[] values;
        final int hashCode;

        Tuple(Object[] values)
        {
            this.values = values;
            hashCode = Arrays.hashCode(values); // pre-calculate this since our values are immutable
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Tuple tuple = (Tuple) o;

            return Arrays.equals(values, tuple.values);
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }

        @Override
        public String toString()
        {
            return "Tuple" + Arrays.toString(values);
        }
    }

    private static final class AvgFunction implements AggregatorFunction
    {
        int count = 0;
        double average;

        @Override
        public void addSample(Object sample)
        {
            count++;
            average += (((Number) sample).doubleValue() - average) / count;
        }

        @Override
        public Object getValue()
        {
            return average;
        }
    }

    private static final class SumFunction implements AggregatorFunction
    {
        boolean init = true;
        boolean isIntegral = false;
        long longSum = 0;
        double doubleSum = 0;

        @Override
        public void addSample(Object sample)
        {
            if (sample == null) return;

            if (init)
            {
                init = false;
                final Class<?> cls = sample.getClass();
                isIntegral = !(cls == Double.class || cls == Float.class);
            }

            if (isIntegral)
            {
                longSum += ((Number) sample).longValue();
            } else
            {
                doubleSum += ((Number) sample).doubleValue();
            }
        }

        @Override
        public Object getValue()
        {
            return isIntegral ? longSum : doubleSum;
        }
    }

    private static final class CountFunction implements AggregatorFunction
    {
        long count = 0;
        final boolean distinct;
        Set<Object> distinctValues;

        CountFunction(boolean distinct)
        {
            this.distinct = distinct;

            if (distinct) distinctValues = new HashSet<>();
        }

        @Override
        public void addSample(Object sample)
        {
            if (distinct)
            {
                if (distinctValues.add(sample)) count++;
            } else
            {
                count++;
            }
        }

        @Override
        public Object getValue()
        {
            return count;
        }
    }

    private static final class MaxFunction implements AggregatorFunction
    {
        Comparable<Object> max = null;

        @Override
        public void addSample(Object sample)
        {
            if (sample != null && (max == null || max.compareTo(sample) < 0))
            {
                //noinspection unchecked
                max = (Comparable<Object>) sample;
            }
        }

        @Override
        public Object getValue()
        {
            return max;
        }
    }

    private static final class MinFunction implements AggregatorFunction
    {
        Comparable<Object> min = null;

        @Override
        public void addSample(Object sample)
        {
            if (sample != null && (min == null || min.compareTo(sample) > 0))
            {
                //noinspection unchecked
                min = (Comparable<Object>) sample;
            }
        }

        @Override
        public Object getValue()
        {
            return min;
        }
    }

    private static final class StddevPopFunction implements AggregatorFunction
    {
        private final StandardDeviation stddev = new StandardDeviation(false); // False to use population

        @Override
        public void addSample(Object sample)
        {
            stddev.increment(((Number) sample).doubleValue());
        }

        @Override
        public Object getValue()
        {
            return stddev.getResult();
        }
    }

    private static final class StddevSampFunction implements AggregatorFunction
    {
        private final StandardDeviation stddev = new StandardDeviation();

        @Override
        public void addSample(Object sample)
        {
            stddev.increment(((Number) sample).doubleValue());
        }

        @Override
        public Object getValue()
        {
            // To be like Postgres, we need to return NULL when we don't have a sufficient sample size
            return stddev.getN() > 1 ? stddev.getResult() : null;
        }
    }

    private static final class VarPopFunction implements AggregatorFunction
    {
        final Variance variance = new Variance(false); // False to use population

        @Override
        public void addSample(Object sample)
        {
            variance.increment(((Number) sample).doubleValue());
        }

        @Override
        public Object getValue()
        {
            return variance;
        }
    }

    private static final class VarSampFunction implements AggregatorFunction
    {
        final Variance variance = new Variance();

        @Override
        public void addSample(Object sample)
        {
            variance.increment(((Number) sample).doubleValue());
        }

        @Override
        public Object getValue()
        {
            return variance.getResult();
        }
    }

    private static final class StubFunction implements AggregatorFunction
    {
        private Object value;

        @Override
        public void addSample(Object sample)
        {
            value = sample;
        }

        @Override
        public Object getValue()
        {
            return value;
        }
    }
}
