package gov.takx.platform.query;

import gov.takx.api.annotation.Internal;
import gov.takx.api.messages.IDataField;
import gov.takx.api.messages.IStructure;
import gov.takx.api.query.param.HQLParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * This class maintains the state of the SQL generation process.  It allows various components that generate SQL
 * to "communicate" and participate the SQL generation.
 *
 * @since 1.49
 */
@Internal
public class SqlState
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * This enum is used to indicate which SQL clause is currently being generated.  This allows the SQL generators
     * to tailor their output as needed.
     */
    public enum SqlClause
    {
        Select,
        Constraint,
        GroupBy
    }

    private final SqlGenerator generator;
    private String tableAlias;
    private IStructure structure;
    private List<HQLParameter> params = new ArrayList<>();
    private int paramCounter = 0;
    private SqlClause sqlClause = SqlClause.Select;
    private final Deque<Separator> levelSeparators = new ArrayDeque<>();

    /**
     * The SQL buffer collecting all generated code.  Made public to simplify sql generation.
     */
    public StringBuilder sql = new StringBuilder();

    /**
     * Construct the state for the given generator.  Package protected so only the SqlGenerator can instantiate it.
     *
     * @param generator SQL Generator we are working for
     */
    SqlState(SqlGenerator generator)
    {
        this.generator = generator;

        reset();
    }

    /**
     * Completely reset our state and be ready for a new SQL generation run.
     */
    public void reset()
    {
        tableAlias = null;
        structure = null;
        params.clear();
        paramCounter = 0;
        sql.setLength(0);
        sqlClause = SqlClause.Select;
        levelSeparators.clear();
        initializeLevelSeparators();
    }

    /**
     * The table alias is only used when selecting extended fields (structure fields).  It is the alias name generated
     * for the specific structure's table.  This will be something like "T0" or "T1" etc.
     * <p/>
     * This alias is used to generate the fully qualified field selectors (like T0.C10_Panic).
     * <p/>
     * This will only return a valid result if we are generating SQL that references extended fields.
     *
     * @return Current structure table alias
     */
    public String getTableAlias()
    {
        return tableAlias;
    }

    /**
     * Set the current table alias.
     *
     * @param tableAlias Current table alias.
     * @see #getTableAlias()
     */
    public void setTableAlias(String tableAlias)
    {
        this.tableAlias = tableAlias;
    }

    /**
     * Set the structure used to map from structure field named to DB column names.
     *
     * @param structure Structure holding the fields we are processing
     */
    public void setStructure(IStructure structure)
    {
        this.structure = structure;
    }

    /**
     * Map the given field name through the current column map.
     *
     * @param fieldName Field name to map
     * @return lookup value from column map, or {@code null} if no field with the given name exists within the structure
     * @see #setStructure(IStructure)
     */
    public String mapField(String fieldName)
    {
        final IDataField field = structure == null ? null : structure.getField(fieldName);
        return field != null ? field.getNeutralizedFieldName() : null;
    }

    /**
     * Add a bound parameter to the sql buffer.  Generate a unique parameter name, add it to the sql, and generate
     * a parameter binding for it.
     *
     * @param name  Base name of parameter to bind
     * @param value Value of parameter to bind
     */
    public void generateAndBindParameter(String name, Object value)
    {
        final String paramName = generateParamName(name);

        sql.append(" :").append(paramName);
        add(new HQLParameter(paramName, value));
    }

    /**
     * Add a bound parameter to the list.
     *
     * @param param Parameter binding to add
     */
    public void add(HQLParameter param)
    {
        for (HQLParameter hqlParameter : params)
        {
            if (hqlParameter.name.equals(param.name))
            {
                // We will get duplicate "moIds" bindings with multi-version queries, so don't report those
                if (!param.name.equals(SqlGenerator.MO_IDS_BINDING))
                {
                    logger.warn("Duplicate add of param: {}", param.name, new RuntimeException());
                }
            }
        }

        params.add(param);
    }

    /**
     * @return All bound parameters
     */
    public List<HQLParameter> getParams()
    {
        return params;
    }

    /**
     * Generate a bound parameter name.  Take the base name and append "_Vn" where "n" is an incrementing integer.
     *
     * @param baseName Base name to use
     * @return generated name
     */
    private String generateParamName(String baseName)
    {
        return baseName + "_V" + paramCounter++;
    }

    /**
     * @return Our controlling generator
     */
    public SqlGenerator getGenerator()
    {
        return generator;
    }

    /**
     * Retrieve the current SQL clause being generated.  Some SQL generators have to alter their form (most notably
     * the field and aggregate selectors) based on the current clause being generated.
     *
     * @return current sql clause
     */
    public SqlClause getSqlClause()
    {
        return sqlClause;
    }

    /**
     * Set the current SQL clause.
     *
     * @param sqlClause The current SQL clause
     * @see #getSqlClause()
     */
    public void setSqlClause(SqlClause sqlClause)
    {
        this.sqlClause = sqlClause;
    }

    /**
     * Add the current level separator to the SQL buffer.
     */
    public void addLevelSeparator()
    {
        sql.append(levelSeparators.peek().getSeparator());
    }

    /**
     * Push a new level separator on to our stack.  For example, when generating the logic constraints, the current
     * level is using a "and" separator, but the logic constraints are separated by the name of the logic operator.
     * <p/>
     * Always ensure that you call {@link #popLevelSeparator()} afterwards.
     *
     * @param initial    The initial separator string
     * @param thereafter The subsequent separator string
     */
    public void pushLevelSeparator(String initial, String thereafter)
    {
        levelSeparators.push(new Separator(initial, thereafter));
    }

    /**
     * Pop the separator stack.
     */
    public void popLevelSeparator()
    {
        levelSeparators.pop();
    }

    /**
     * Reset the current/top separator.
     */
    public void resetLevelSeparator()
    {
        levelSeparators.peek().reset();
    }

    /**
     * (Re-)initialize the level separator stack.  Clear it and install the default separator.
     */
    public void initializeLevelSeparators()
    {
        levelSeparators.clear();
        pushLevelSeparator(" ", ", "); // Start with a default separator
    }

    /**
     * Simple class to return an appropriate separator where the first use returns a specified "initial" string
     * and subsequent uses return the specified separator string.
     * <p/>
     * This is used to handle the various separators used at different levels in the SQL generation.  At some points
     * you need commas, at others you need "and" and still others need different prefixes on the first element at
     * that level.
     */
    private static final class Separator
    {
        boolean first = true;
        final String initial;
        final String thereafter;

        Separator(String initial, String thereafter)
        {
            this.initial = initial;
            this.thereafter = thereafter;
        }

        String getSeparator()
        {
            if (first)
            {
                first = false;
                return initial;
            } else
            {
                return thereafter;
            }
        }

        void reset()
        {
            first = true;
        }
    }
}
