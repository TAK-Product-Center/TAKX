package gov.takx.api.query.param;

import gov.takx.api.annotation.Internal;
import org.hibernate.query.TupleTransformer;

/**
 * This class is used in a parameter list to specify a transformer to apply to the query results.
 * If you specify this more than once in the parameter list, then the last one specified will be used.
 *
 * @since 1.25
 */
@Internal
public class SQLTransformer extends HQLParameter
{
    public TupleTransformer<?> transformer; // In this derived type, the value is a ResultTransformer

    /**
     * @param transformer The transformer to apply to the query results
     * @since 3.4 (type of parameter changed in migration to Hibernate 6)
     */
    public SQLTransformer(TupleTransformer<?> transformer)
    {
        super("transformer", transformer);
        this.transformer = transformer;
    }
}
