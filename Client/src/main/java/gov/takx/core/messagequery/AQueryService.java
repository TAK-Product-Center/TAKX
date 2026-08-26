package gov.takx.core.messagequery;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.persist.CompositeType;
import gov.takx.api.persist.IPersistService;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.query.ConflictingConstraintException;
import gov.takx.api.query.CoreField;
import gov.takx.api.query.CoreFieldSelector;
import gov.takx.api.query.FieldSelector;
import gov.takx.api.query.IQueryConstraints;
import gov.takx.api.query.IQueryService;
import gov.takx.api.query.InvalidFieldSelectorException;
import gov.takx.api.query.QueryExecutionException;
import gov.takx.api.query.param.HQLParameter;
import gov.takx.api.query.param.SQLTransformer;
import gov.takx.dataservices.utils.DynamicStructureDBUtils;
import gov.takx.platform.query.MessageMatcher;
import gov.takx.platform.query.QueryConstraints;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.hibernate.query.TupleTransformer;
import org.hibernate.transform.Transformers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract base class for query services.  It provides the standard query method implementations.  Derived
 * classes must implement the {@link #createEmptyConstraints()} method to properly create the constraints
 * object of the proper type.
 *
 */
@SuppressWarnings("rawtypes")
@PseudoBean
public abstract class AQueryService implements IQueryService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IPersistServiceProvider persistServiceProvider;

    @Inject
    private DynamicStructureDBUtils dynamicStructureDBUtils;

    @Override
    public abstract IQueryConstraints createEmptyConstraints();

    @Override
    public boolean matches(IRaptorDataMessage rdm, IQueryConstraints constraints)
    {
        final QueryConstraints qc = prepareQueryConstraints(constraints);

        return new MessageMatcher(qc).matches(rdm);
    }

    @Override
    public long count(IQueryConstraints constraints) throws QueryExecutionException
    {
        final QueryConstraints qc = prepareQueryConstraints(constraints);

        try
        {
            ensureTablesExist(qc);

            final List<?> results = persistServiceProvider.getPersistService()
                    .sqlQueryFromRdm(qc.getHQLForCount(), 0, false, getQueryParameters(qc));

            return ((Number) results.get(0)).longValue();
        } catch (Exception e)
        {
            throw new QueryExecutionException("Failed to get count: " + qc.getHQLForCount(), e);
        }
    }

    @Override
    public List<IRaptorDataMessage> query(IQueryConstraints constraints, boolean prefetch) throws QueryExecutionException
    {
        final QueryConstraints qc = prepareQueryConstraints(constraints);

        try
        {
            String hql = qc.getHQLForMessages();
            HQLParameter[] hqlParams = getQueryParameters(qc);

            ensureTablesExist(qc);

            final IPersistService ps = persistServiceProvider.getPersistService();

            final List<IRaptorDataMessage> messages = ps.sqlQueryFromRdm(hql,
                    constraints.getMaxMessageCount(), true, hqlParams);

            if (prefetch)
            {
                if (logger.isDebugEnabled()) logger.debug("Prefetch extended data for messages");
                ps.bulkLoadExtendedData(messages);
            }

            return messages;
        } catch (Exception e)
        {
            throw new QueryExecutionException("Failed to get messages", e);
        }
    }

    @Override
    public List<IRaptorDataMessage> query(IQueryConstraints constraints) throws QueryExecutionException
    {
        return query(constraints, false);
    }

    @Override
    public Iterator<IRaptorDataMessage> iterator(IQueryConstraints constraints) throws QueryExecutionException
    {
        final QueryConstraints qc = prepareQueryConstraints(constraints);

        try
        {
            String hql = qc.getHQLForMessages();
            HQLParameter[] hqlParams = getQueryParameters(qc);

            ensureTablesExist(qc);

            return persistServiceProvider.getPersistService()
                    .rdmIterator(hql, constraints.getMaxMessageCount(), hqlParams);
        } catch (Exception e)
        {
            throw new QueryExecutionException("Failed to get iterator", e);
        }
    }

    @Override
    public List<Object[]> queryForFields(IQueryConstraints constraints, FieldSelector fieldSelector)
            throws QueryExecutionException, InvalidFieldSelectorException, ConflictingConstraintException
    {
        //noinspection unchecked
        return queryForFieldsInternal(constraints, fieldSelector, null);
    }

    @Override
    public List<Map<String, Object>> queryForFieldsAsMap(IQueryConstraints constraints, FieldSelector fieldSelector)
            throws QueryExecutionException, InvalidFieldSelectorException, ConflictingConstraintException
    {
        //noinspection unchecked
        return (List<Map<String, Object>>) queryForFieldsInternal(constraints, fieldSelector, Transformers.ALIAS_TO_ENTITY_MAP);
    }

    @Override
    public <T> List<T> queryForFieldsAsBean(IQueryConstraints constraints, FieldSelector fieldSelector, Class<T> dtoType)
            throws QueryExecutionException, InvalidFieldSelectorException, ConflictingConstraintException
    {
        //noinspection unchecked
        return queryForFieldsInternal(constraints, fieldSelector, Transformers.aliasToBean(dtoType));
    }

    /**
     * Internal query for fields method accepting a result transformer.
     *
     * @param constraints   Query constraints
     * @param fieldSelector Field selection
     * @param transformer   The result transformer to apply
     * @return List of results as per the transformer
     * @throws QueryExecutionException        If any errors occur during execution of the query
     * @throws InvalidFieldSelectorException  If the field selector is invalid
     * @throws ConflictingConstraintException If the field selector and field constraints are on different structures
     */
    private List queryForFieldsInternal(IQueryConstraints constraints, FieldSelector fieldSelector, TupleTransformer transformer)
            throws QueryExecutionException, InvalidFieldSelectorException, ConflictingConstraintException
    {
        final QueryConstraints qc = prepareQueryConstraints(constraints);

        // Ensure that the field selector is valid
        if (fieldSelector.hasExtendedFields() && fieldSelector.getStructure() == null)
        {
            throw new InvalidFieldSelectorException("A structure must be specified along with extended field names");
        }

        try
        {
            String hql = qc.getHQLForFields(fieldSelector);
            HQLParameter[] hqlParams = getQueryParameters(qc, new SQLTransformer(makeTransformer(transformer, fieldSelector)));

            ensureTablesExist(qc);

            return persistServiceProvider.getPersistService()
                    .sqlQueryFromRdm(hql, constraints.getMaxMessageCount(), false, hqlParams);
        } catch (ConflictingConstraintException e)
        {
            throw e; // Maintain backward compat and don't wrap this exception
        } catch (Exception e)
        {
            throw new QueryExecutionException("Failed to get messages", e);
        }
    }

    @Override
    public void closeIterator(Iterator<IRaptorDataMessage> iterator)
    {
        persistServiceProvider.getPersistService().closeRdmIterator(iterator);
    }

    /**
     * Validate that the query constraints are legal (non-null and of the expected type).
     *
     * @param constraints Constraints to test and prepare
     * @return Properly typed constraints
     * @since 2.29
     */
    private QueryConstraints prepareQueryConstraints(@Nonnull IQueryConstraints constraints)
    {
        // It must be a concrete class that we created, no impostors
        if (!(constraints instanceof QueryConstraints))
        {
            throw new IllegalArgumentException("constraints object was not created by this service");
        }

        return (QueryConstraints) constraints;
    }

    /**
     * It is possible, now that the DB mapping is delayed until an RDM using a given structure is actually
     * about to be persisted, that a plugin could try to perform a query on structures that haven't been mapped
     * yet, which would lead to SQL failures since the tables don't exist.
     * <p>
     * So, this method ensures that any structure table referenced in the query has been mapped into the DB.
     *
     * @param queryConstraints Query constraints to process
     * @since 2.37
     */
    private void ensureTablesExist(QueryConstraints queryConstraints)
    {
        queryConstraints.getStructuresUsed().forEach(dynamicStructureDBUtils::ensureMappedInDb);
    }

    /**
     * Construct the HQL parameters array given the source constraints and any additional parameters required.
     *
     * @param constraints The query constraints from which to obtain the initial parameters
     * @param extraParams Any additional parameters to be added to the final array
     * @return HQL parameters array
     * @since 2.29
     */
    private HQLParameter[] getQueryParameters(QueryConstraints constraints, HQLParameter... extraParams)
    {
        List<HQLParameter> parameterList = constraints.getHQLParametersList();
        HQLParameter[] parametersArray = parameterList.toArray(new HQLParameter[parameterList.size() + extraParams.length]);

        int idx = parameterList.size(); // Where to start adding
        for (HQLParameter param : extraParams)
        {
            parametersArray[idx++] = param;
        }
        return parametersArray;
    }

    /**
     * Factory method to construct an appropriate result transformer.  If there are any composite fields
     * in the given field selector, then instantiate a CompositeTypeTransformer which chains to the given
     * transformer. Otherwise, we just return the given transformer.
     *
     * @param transformer   Chained transformer
     * @param fieldSelector Current field selector being processed
     * @return appropriate transformer to use
     */
    private TupleTransformer makeTransformer(TupleTransformer transformer, FieldSelector fieldSelector)
    {
        // Find the core fields that have composite types from the field selector

        final List<CoreField> compositeFields = fieldSelector.getCoreFieldSelectors().stream()
                .filter(CoreFieldSelector::isCompositeType)
                .map(CoreFieldSelector::getCoreField)
                .collect(Collectors.toList());

        // If there are no composite fields involved, then just return the chained transformer.
        // Otherwise, create a composite type transformer chaining to the given transformer.

        final TupleTransformer targetTransformer = compositeFields.isEmpty()
                ? transformer
                : new CompositeTypeTransformer(transformer, compositeFields);

        // We always install our basic result transformer first
        return new QueryResultTransformer(targetTransformer);
    }

    /**
     * This custom result transformer does 3 things:
     * <ol>
     * <li>prevents the default Hibernate behavior of converting result sets of length 1 to scalars.
     * This ensures that the List returned by a field-level query always contains arrays.</li>
     * <li>Converts any return value of type BigInteger to Long</li>
     * <li>Converts any return value of type Date to Long (milliseconds)</li>
     * </ol>
     * <p>
     * We feel that this will be easier to explain/teach to plugin developers how to use these query types with
     * these changes in place.
     */
    private static final class QueryResultTransformer implements TupleTransformer
    {
        private final TupleTransformer chainedTransformer;

        /**
         * Construct this transformer with a chained transformer.  After this transformer does its work, it will
         * pass the results off to the other transformer to process.
         *
         * @param transformer Transformer to chain our results through
         */
        public QueryResultTransformer(TupleTransformer transformer)
        {
            chainedTransformer = transformer;
        }

        /**
         * {@inheritDoc}
         * Extended to convert BigInteger objects into Long objects.
         */
        @Override
        public Object transformTuple(Object[] tuple, String[] aliases)
        {
            for (int i = 0; i < tuple.length; i++)
            {
                // Use direct equality for speed (no reason to use instanceof)
                final Class cls = tuple[i] != null ? tuple[i].getClass() : null;

                if (cls == BigInteger.class)
                {
                    tuple[i] = ((BigInteger) tuple[i]).longValue();
                } else if (cls == Timestamp.class || cls == java.sql.Date.class)
                {
                    tuple[i] = ((Date) tuple[i]).getTime();
                }
            }
            return chainedTransformer == null ? tuple : chainedTransformer.transformTuple(tuple, aliases);
        }
    }

    /**
     * Result transformer to take decomposed composite type values and reconstruct the composite type.
     */
    private static final class CompositeTypeTransformer implements TupleTransformer
    {
        private final CoreField[] fields;
        private int compression = 0;

        private final TupleTransformer chainedTransformer;

        /**
         * Construct this transformer with a chained transformer.  After this transformer does its work, it will
         * pass the results off to the other transformer to process.
         *
         * @param transformer Transformer to chain our results through
         */
        public CompositeTypeTransformer(TupleTransformer transformer, List<CoreField> compositeFields)
        {
            chainedTransformer = transformer;

            fields = compositeFields.toArray(new CoreField[0]);

            // Determine how many tuple entries will be removed when this transformer runs
            for (CoreField field : fields)
            {
                compression += field.getCompositeType().getPropertyNames().length - 1;
            }
        }

        @Override
        public Object transformTuple(Object[] tuple, String[] aliases)
        {
            final int numResults = tuple.length - compression;
            final Object[] result = new Object[numResults];
            final String[] newAliases = new String[numResults];

            int resultIndex = 0;

            for (int i = 0; i < tuple.length; i++)
            {
                // See if the field alias has the same initial text as one of our composite types
                boolean composite = false;

                for (final CoreField field : fields)
                {
                    final CompositeType compositeType = field.getCompositeType();

                    // If this is the first column name of the composite type, then reify its value
                    if (aliases[i].equalsIgnoreCase(compositeType.getColumnName(0)))
                    {
                        // Ok, process this field
                        composite = true;
                        result[resultIndex] = compositeType.reify(tuple, i);
                        newAliases[resultIndex] = field.name();
                        i += compositeType.getPropertyCount() - 1; // Consumer the additional tuple values
                        break;
                    }
                }

                if (!composite)
                {
                    result[resultIndex] = tuple[i];
                    newAliases[resultIndex] = aliases[i];
                }

                resultIndex += 1;
            }

            return chainedTransformer == null ? result : chainedTransformer.transformTuple(result, newAliases);
        }
    }
}
