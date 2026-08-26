package gov.takx.api.query;

/**
 * An AggregateType is an enumeration to identity an 'SQL Aggregate Function'.  The best known 'aggregate function'
 * is 'count()' because it is often used as a single result in a SQL query.  Other 'aggregate functions' are
 * commonly used as 'HAVING clauses' in 'GROUP BY' expressions.
 *
 * @since 1.49
 */
public enum AggregateType
{
    // Official SQL Aggregate Functions
    // http://dev.mysql.com/doc/refman/5.0/en/group-by-functions.html
    COUNT,
    SUM,
    AVG,
    MAX,
    MIN,

    // Quasi-Official Statistical and Other Functions
    // These are common to POSTGRES, MYSQL, and HSQLDB, but not DERBY
    VAR_SAMP,
    VAR_POP,
    STDDEV_SAMP,
    STDDEV_POP,
}
