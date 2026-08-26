package gov.takx.core.persist;

import gov.takx.api.persist.IPersistable;
import gov.takx.api.query.param.HQLParameter;
import gov.takx.api.query.param.SQLTransformer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A parsed and prepared query ready for execution with a provided set of parameter bindings.
 * This query object produces the final SQL and binds the parameters into a prepared statement for execution by
 * the persist service.
 * <p>
 * This class is thread safe.
 *
 * @since 2.14
 */
public class RDMQuery
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String originalHql;
    private final String sql;
    private final List<String> bindings;
    private final boolean customSelect;
    private final String newClassName;

    /**
     * Construct a new RDM query.
     *
     * @param originalHql  The original SQL/HQL (before any parsing)
     * @param sql          The full sql for the query (still contains parameter bindings, like {@code :foo}
     * @param bindings     The ordered list of bindings to process into the sql
     * @param customSelect Pass {@code true} if selecting custom fields, {@code false} for RDM selects
     */
    public RDMQuery(String originalHql, String sql, List<String> bindings, boolean customSelect)
    {
        this(originalHql, sql, bindings, customSelect, null);
    }

    /**
     * Construct a new RDM query that will create new objects of type {@code newClassName}.
     *
     * @param originalHql  The original SQL/HQL (before any parsing)
     * @param sql          The full sql for the query (still contains parameter bindings, like {@code :foo}
     * @param bindings     The ordered list of bindings to process into the sql
     * @param newClassName Name of custom class to instantiate from query results
     */
    public RDMQuery(String originalHql, String sql, List<String> bindings, String newClassName)
    {
        this(originalHql, sql, bindings, true, newClassName);
    }

    /**
     * Construct a new RDM query.
     *
     * @param originalHql  The original SQL/HQL (before any parsing)
     * @param sql          The SQL generated from the original HQL (if any was performed)
     * @param bindings     The ordered list of bindings to process into the sql
     * @param customSelect Pass{@code true} if this is a "custom" select (not producing RDMs)
     * @param newClassName Name of custom class to instantiate from query results, may be {@code null}
     */
    private RDMQuery(String originalHql, String sql, List<String> bindings, boolean customSelect, String newClassName)
    {
        this.originalHql = originalHql;
        this.sql = sql;
        this.bindings = new ArrayList<>(bindings);
        this.customSelect = customSelect;
        this.newClassName = newClassName;
    }

    /**
     * @return The parsed SQL
     */
    public String getSql()
    {
        return sql;
    }

    /**
     * @return The input HQL
     */
    public String getOriginalHql()
    {
        return originalHql;
    }

    /**
     * @return {@code true} if this query represents a "custom" select {@code false} if it will return the columns
     * needed to produce RDMs.
     */
    public boolean isCustomSelect()
    {
        return customSelect;
    }

    /**
     * @return The name of the class to produce instead of raw results or RDMs, returns {@code null} if not specified
     */
    public String getNewClassName()
    {
        return newClassName;
    }

    /**
     * @return {@code true} if we are creating a specified class instead of raw values or RDMs
     */
    public boolean isConstructNewClass()
    {
        return newClassName != null;
    }

    /**
     * Construct a PreparedStatement and prepare it for execution by binding in the given parameters.
     *
     * @param connection Connection to use to create the PreparedStatement
     * @param parameters Parameters to bind
     * @return PreparedStatement
     */
    public PreparedStatement prepareQuery(Connection connection, HQLParameter... parameters) throws SQLException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Prepare HQL: {}", originalHql);
            logger.debug("parameters : {}", Arrays.toString(parameters));
        }

        // Construct a map from parameter name to value so we can process the bindings in the order they appear
        // in the original sql. Prefix param names with a ':'.

        // Using a stream and Collectors.toMap can lead to "Duplicate key" exceptions, so do it old-school
        final Map<String, HQLParameter> parameterMap = new HashMap<>(parameters.length);
        for (HQLParameter parameter : parameters)
        {
            // Ignore transformer entries
            if (!(parameter instanceof SQLTransformer))
            {
                parameterMap.put(":" + parameter.name, parameter);
            }
        }

        final PreparedStatement statement = connection.prepareStatement(expandBindings(parameterMap));

        bindParameters(statement, parameterMap); // Bind in the parameter values

        return statement;
    }

    /**
     * Expand the bindings in the SQL to '?' placeholders.
     *
     * @param parameterMap Map of parameter name (like ":foo") to parameter binding
     * @return new SQL with named bindings replaced with '?' placeholders
     */
    private String expandBindings(Map<String, HQLParameter> parameterMap) throws SQLException
    {
        String querySql = sql;

        for (String bindingName : bindings)
        {
            final HQLParameter parameter = parameterMap.get(bindingName);
            if (logger.isDebugEnabled()) logger.debug("binding: '{}', param={}", bindingName, parameter);

            if (parameter == null)
            {
                throw new SQLException("unknown parameter '" + bindingName + "' in: " + querySql);
            }

            // Determine if this is a single or multi-valued binding
            String expansion = "? ";

            if (parameter.value instanceof Collection)
            {
                final int count = ((Collection<?>) parameter.value).size();
                expansion = StringUtils.repeat("?,", count);
                expansion = expansion.substring(0, expansion.length() - 1) + " "; // remove trailing comma
            }

            // In order to handle bindings that are prefixes of other bindings, like :foo and :foobar, the SQL
            // has been updated to ensure that all binding references have a space after them (this was done by
            // RDMQueryParser), so add a space to the bindingName being replaced to ensure an exact match.

            querySql = StringUtils.replace(querySql, bindingName + " ", expansion);
        }

        if (logger.isDebugEnabled()) logger.debug("Expanded SQL: {}", querySql);

        return querySql;
    }

    /**
     * Bind the parameter values into the prepared statement.
     *
     * @param statement    Prepared statement to process
     * @param parameterMap Map of parameters to bind
     */
    private void bindParameters(PreparedStatement statement, Map<String, HQLParameter> parameterMap) throws SQLException
    {
        int index = 1;

        for (String bindingName : bindings)
        {
            final Object value = parameterMap.get(bindingName).value;

            if (value instanceof Collection)
            {
                index += bindCollection(statement, (Collection<?>) value, index);
            } else
            {
                index += bindScalar(statement, value, index);
            }
        }
    }

    /**
     * Bind a collection of values into the statement.  The collection can contain numbers, strings, or persistable
     * objects (which will be bound as their Id).
     *
     * @param statement Statement being prepared
     * @param values    The collection to bind
     * @param index     The initial index at which to bind values
     * @return The number of values bound
     */
    private int bindCollection(PreparedStatement statement, Collection<?> values, int index) throws SQLException
    {
        for (Object value : values)
        {
            // For now, we don't support collections of UUID
            if (value instanceof UUID) throw new SQLException("Collections of UUID are not supported");

            index += bindScalar(statement, value, index);
        }
        return values.size();
    }

    /**
     * Bind all scalar values.  This handles:
     * <ul>
     *     <li>UUID - binds as 2 values, MSB and LSB</li>
     *     <li>{@link IPersistable} entities - bind as their id, using {@link IPersistable#getId()}</li>
     *     <li>Enum - bind as their ordinal value</li>
     *     <li>Date - bind as {@link Timestamp}</li>
     *     <li>All others (like all wrapper types, Integer, Float, etc.) - bind as "object" using {@link PreparedStatement#setObject(int, Object)}</li>
     * </ul>
     *
     * @param statement Statement being prepared
     * @param value     The value to bind
     * @param index     The initial index at which to bind values
     * @return The number of values bound
     */
    @SuppressWarnings("UseOfObsoleteDateTimeApi")
    private int bindScalar(PreparedStatement statement, Object value, int index) throws SQLException
    {
        int used = 1;

        if (value instanceof UUID)
        {
            // Need to bind the two halves of the UUID
            final UUID uuid = (UUID) value;
            statement.setLong(index, uuid.getMostSignificantBits());
            statement.setLong(index + 1, uuid.getLeastSignificantBits());
            used = 2;

            if (logger.isDebugEnabled()) logger.debug("bind {} : {}", index, uuid);
        } else if (value instanceof IPersistable)
        {
            final Integer id = ((IPersistable) value).getId();
            statement.setInt(index, id);

            if (logger.isDebugEnabled()) logger.debug("bind {} : {} : {}", index, value.getClass(), id);
        } else if (value instanceof Date)
        {
            // Bind as a timestamp
            final Timestamp timestamp = new Timestamp(((Date) value).getTime());
            statement.setTimestamp(index, timestamp);

            if (logger.isDebugEnabled()) logger.debug("bind {} : {}", index, timestamp);
        } else if (value instanceof Enum)
        {
            // Bind as a integer (ordinal)
            final Enum<?> anEnum = (Enum<?>) value;
            final int ordinal = anEnum.ordinal();
            statement.setInt(index, ordinal);

            if (logger.isDebugEnabled()) logger.debug("bind {} : {} : {}", index, anEnum.name(), ordinal);
        } else
        {
            final Class<?> cls = value != null ? value.getClass() : null;
            statement.setObject(index, value);

            if (logger.isDebugEnabled()) logger.debug("bind {} : {}, {}", index, value, cls);
        }

        return used;
    }
}
