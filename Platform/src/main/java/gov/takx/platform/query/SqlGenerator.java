package gov.takx.platform.query;

import gov.takx.api.annotation.Internal;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.messages.IStructure;
import gov.takx.api.messages.NoSuchStructureException;
import gov.takx.api.messages.RaptorDataMessageColumns;
import gov.takx.api.query.AFieldSelector;
import gov.takx.api.query.FieldSelector;
import gov.takx.api.query.IConstraint;
import gov.takx.api.query.IQueryConstraints;
import gov.takx.api.query.LogicConstraint;
import gov.takx.api.query.param.HQLParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Time;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class handles the SQL generation based on a configured {@link QueryConstraints}.  There is a very tight
 * relationship between these two classes and this class directly references protected members in QueryConstraints.
 * <p/>
 * We need to generate subtly different SQL when querying for fields than when querying for RDMs.
 * The main case is dealing with multiple structure versions where we need to add the selected
 * extended field names in each primary clause using the proper table alias (T0, T1, etc.)
 * <p/>
 * If all fields selected are on core fields and there are no constraints on extended fields,
 * then we generate HQL like this:
 * <pre>
 * select
 * rdm.<f1>, rdm.<f2>, ...                            # The core fields selected
 * from MapPoint rdm                                  # Core fields table with alias "rdm"
 * where rdm.mapObject_id NOT IN (select distinct(dmo.mapObjectId) from DeletedMapObject dmo) # MO is not deleted
 *     and rdm.mapobject_id IN (:moIds)               # Limit to only the messages from specified devices*
 *     and rdm.speed &gt; :speed_value0                  # Limit to matching core field values*
 *     and structure_id IN (:structureIdList)         # Limit to the specified messaged type*
 *                                                    # * Optional
 * </pre>
 * If the query is retrieving or constraining on an extended field value, then we generate HQL like this:
 * <pre>
 * select
 * T0.<f1>, T0.<f2>, ...                              # The extended fields selected, per table (version)
 * from SF8D2054EE4CDCABBA7C932F9DB766CBB T0          # Table (named by hash key), containing the extended data, renamed to "T0"
 * left join mappoint rdm on T0.mappoint_id=rdm.id    # Join with the core fields, renamed to "rdm", by rdm id
 * where rdm.mapObject_id NOT IN (select distinct(dmo.mapObjectId) from DeletedMapObject dmo) # MO is not deleted
 *    and T0.c933_Tilt = :FV_0_0                      # Limit to matching extended field values
 *    and rdm.mapobject_id IN (:moIds)                # Limit to only the messages from specified devices
 *    and structure_id IN (:structureIdList)          # Limit to the specified messaged type*
 * # (optionally union with other versions of the extended data table)
 * union all
 * select
 * T1.<f1>, T1.<f2>, ...                              # The extended fields selected, per table (version)
 * ...
 * </pre>
 *
 * @since 1.49
 */
@Internal
public class SqlGenerator
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Marker in the SQL generated to indicate the location of the select clause (in case we need to union)
    private static final String SELECT_CLAUSE = "$$SELECT$$";

    private static final String ALL_RDM_COLUMNS_WITH_ALIAS = RaptorDataMessageColumns.getAllRdmColumnsWithAlias("rdm.");

    // Maps day numbers to day names
    private static final String[] DAY_NAMES = {
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    // Binding name for Map Object Ids
    static final String MO_IDS_BINDING = "moIds";

    private final QueryConstraints qc;
    private final SqlState state = new SqlState(this);

    private String hqlQuery;
    private String hqlCountQuery;

    /**
     * Package protected constructor so only QueryConstraints can instantiate one.
     *
     * @param qc Owning query constraints
     */
    SqlGenerator(QueryConstraints qc)
    {
        this.qc = qc;
    }

    /**
     * Reset to a clean initial state.
     */
    public void reset()
    {
        hqlQuery = null;
        hqlCountQuery = null;
        state.reset();
    }

    /**
     * @return The generated query parameters; the query must have been finalized before you call this method.
     */
    public List<HQLParameter> getParams()
    {
        return state.getParams();
    }

    /**
     * @return The HQL query string to count the number of messages that match our constraints
     */
    public String getHQLForCount()
    {
        finalizeQuery(null);

        String hql = replaceSelectClause(hqlCountQuery, "select count(distinct rdm.id) as cnt ");

        // If this query is a union on multiple tables, there will be multiple
        // rows in the resulting "cnt" column that need to be summed together.
        if (hqlCountQuery.contains("union all"))
        {
            hql = "select sum(cnt) as cnt from ( " + hql + " ) as count_result";
        }

        if (logger.isDebugEnabled()) logger.debug("count HQL: {}\nPARAMS={}", hql, getParams());

        return hql;
    }

    /**
     * @return The HQL query string to retrieve the messages that match our constraints
     */
    public String getHQLForMessages()
    {
        finalizeQuery(null);

        final String hql = replaceSelectClause(hqlQuery, "select distinct " + ALL_RDM_COLUMNS_WITH_ALIAS + " ");

        if (logger.isDebugEnabled()) logger.debug("message HQL: {}\nPARAMS={}", hql, getParams());
        return hql;
    }

    /**
     * @param fieldSelector field selector indicating which fields are to be retrieved by the query
     * @return The HQL query string to retrieve the fields of the messages that match our constraints
     */
    public String getHQLForFields(FieldSelector fieldSelector)
    {
        if (fieldSelector.isEmpty()) throw new IllegalStateException("FieldSelector selects no fields");

        finalizeQuery(fieldSelector);

        if (logger.isDebugEnabled()) logger.debug("Fields HQL: {}\nPARAMS={}", hqlQuery, getParams());
        return hqlQuery;
    }

    /**
     * Replace all occurrences of the SELECT_CLAUSE in the SQL string with the given select clause.
     *
     * @param sql          SQL string to process
     * @param selectClause Select clause to insert
     * @return updated string
     */
    private String replaceSelectClause(String sql, String selectClause)
    {
        return sql.replace(SELECT_CLAUSE, selectClause);
    }

    /**
     * Generate the SQL for this query based on the current constraints and the given, optional, field selector.
     *
     * @param fieldSelector Selected fields, may be {@code null} if none (meaning all RDM fields)
     */
    private void finalizeQuery(FieldSelector fieldSelector)
    {
        final boolean hasFieldSelector = null != fieldSelector;

        if (qc.grouping && !hasFieldSelector)
        {
            throw new IllegalStateException("Use of a GROUP requires use of a FieldSelector");
        }

        reset(); // Always start fresh

        final boolean usesExtendedFields =
                anyExtendedFieldsReferenced() || (hasFieldSelector && fieldSelector.usesExtendedFields());

        if (!usesExtendedFields)
        {
            // No extended fields referenced anywhere, so generate SQL using "core" format
            generateCoreSelectClause(fieldSelector);
        } else
        {
            // For non-core fields, we need to select against the version-specific tables
            generateStructureSelectClause(fieldSelector);
        }

        // We don't need the order by in the count and it will probably break the grammar
        hqlCountQuery = state.sql.toString();

        generateOrderByClause();

        generateOffsetClause(qc.getStartMessageCount());

        hqlQuery = state.sql.toString();

        logger.trace("Finalized WHERE HQL: {}", hqlQuery);
    }

    /**
     * Determine if any of the various constraints reference an extended field.
     *
     * @return {@code true} if any extended field is referenced
     */
    private boolean anyExtendedFieldsReferenced()
    {
        if (!qc.structureFieldConstraints.isEmpty()) return true;

        // Scan the logic constraints and see if any of them reference an extended field
        for (IConstraint constraint : qc.logicConstraints)
        {
            if (constraint.usesExtendedFields()) return true;
        }

        // Scan the group constraints and see if any of them reference an extended field
        for (IConstraint constraint : qc.groupConstraints)
        {
            if (constraint.usesExtendedFields()) return true;
        }

        return false;
    }

    /**
     * Generate the HQL for selecting fields solely relying on core fields. No extended data tables are involved.
     *
     * @param fieldSelector Field selector if we are processing a field-level query, {@code null} otherwise
     */
    private void generateCoreSelectClause(FieldSelector fieldSelector)
    {
        final boolean hasFieldSelector = null != fieldSelector;

        state.setSqlClause(SqlState.SqlClause.Select); // Start at select clause

        state.sql.append(hasFieldSelector ? "select " : SELECT_CLAUSE); // Start with the proper select clause

        if (hasFieldSelector)
        {
            generateSqlForSelectors(fieldSelector.getCoreFieldSelectors());
            generateSqlForSelectors(fieldSelector.getAggregateFieldSelectors());
        }

        state.sql.append("\n from MapPoint rdm");

        generateWhereClause();

        generateGroupByClause(fieldSelector);
    }

    /**
     * Generate the SQL for the structure field constraints specified.  We will be selecting from the structure-specific
     * table(s) for the field constraints specified and joining back to the RDM (mappoint) table.
     *
     * @param fieldSelector selector if querying for fields, else {@code null}
     */
    private void generateStructureSelectClause(FieldSelector fieldSelector)
    {
        // Since the only way to get here is to specify a "single" structure name then we should be able to
        // cycle through the structures with this name and get their extended data class name (which will be the table
        // name) and also to get the actual column name that the requested value(s) will be stored in.

        // We are creating select clauses that work FROM the structure-specific table back to the RDM.
        // We have to union them together in case there are multiple structures with the same name.

        final boolean hasFieldSelector = fieldSelector != null;

        if (hasFieldSelector && fieldSelector.isEmpty()) throw new IllegalStateException("FieldSelector is empty!");

        int tableCount = 0;

        // Iterate over all versions of the structure.
        final Set<IStructure> structures = qc.messageStructureMap.get(qc.fieldStructureName);
        if (structures == null)
        {
            throw new NoSuchStructureException("Unknown message structure: " + qc.fieldStructureName);
        }

        for (IStructure structure : structures)
        {
            // Check to see if this structure contains all the constraint fields.
            // The getKeys() method returns all of the structure's fields' names.
            // The keySet() method returns all of the constraints' fields' names.

            final Collection<String> structureFieldNames = structure.getKeys();

            if (!structureFieldNames.containsAll(qc.structureFieldConstraints.keySet()))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Missing fields structFields={} constraintFields={}", structureFieldNames, qc.structureFieldConstraints.keySet());
                }
                continue;
            }

            // Validate that all fields specified in the field selector (if any) are present on this structure
            if (hasFieldSelector)
            {
                final List<String> selectorExtendedFields = fieldSelector.getExtendedFields();

                if (!structureFieldNames.containsAll(selectorExtendedFields))
                {
                    final String missingFields = selectorExtendedFields
                            .stream()
                            .filter(fn -> !structureFieldNames.contains(fn))
                            .collect(Collectors.joining(", "));

                    final String msg = "Structure [" + structure.getMessageType() + "] does not contain all selected fields.  Missing [" + missingFields + "]";
                    throw new IllegalArgumentException(msg);
                }
            }

            // Push this table data into our SQL generator
            final String tableName = structure.getExtendedDataClassName();
            final String tableAlias = "T" + tableCount++;
            state.setTableAlias(tableAlias);
            state.setStructure(structure);

            state.resetLevelSeparator(); // Each time, so the select list is generated properly
            state.setSqlClause(SqlState.SqlClause.Select); // Back to select clause

            // If this is not the first structure to be added, union the results.
            if (tableCount > 1)
            {
                state.sql.append("\nunion all\n");
            }

            // Get the proper select clause emitted
            if (hasFieldSelector)
            {
                state.sql.append("select ");
                generateSelections(fieldSelector);
            } else
            {
                state.sql.append(SELECT_CLAUSE);
            }

            // The extended data (structure) table aliased as T0, T1, etc.
            state.sql.append("\n from ").append(tableName).append(" ").append(tableAlias);

            // Join back from the structure table to the core RDM table
            state.sql.append("\n left join mappoint rdm on ").append(tableAlias).append(".mappoint_id=rdm.id");

            generateWhereClause();

            generateGroupByClause(fieldSelector);
        }
    }

    /**
     * Generate the SQL for the WHERE clause of this query.
     */
    private void generateWhereClause()
    {
        state.setSqlClause(SqlState.SqlClause.Constraint); // Now generating constraints

        state.sql.append("\n where");

        // No deleted data
        state.sql.append("\n  rdm.mapObject_id NOT IN (select distinct(dmo.mapObjectId) from DeletedMapObject dmo)");

        state.pushLevelSeparator("\n  and ", "\n  and "); // From here on, we "and" our constraints

        generateAdditionalConstraints();
        generateSqlForStructureConstraints();
        generateSqlForConstraintsCollection(qc.coreFieldConstraints.values());
        generateSqlForConstraintsCollection(qc.structureFieldConstraints.values());
        generateSqlForConstraints(qc.shapeConstraints);
        generateSqlForConstraints(qc.logicConstraints);

        state.popLevelSeparator(); // Done and'ing
    }

    /**
     * Generate all non-field-specific constraints, such as time, location and buckets. If there are no field-level
     * constraints, these additional constraints will be applied to the entire query. If there are field-level
     * constraints, they will be applied to each table matching the specified structure.
     *
     * @since 1.18
     */
    private void generateAdditionalConstraints()
    {
        if (!qc.mapObjects.isEmpty())
        {
            state.sql.append("\n  and rdm.mapobject_id in (:" + MO_IDS_BINDING + ")");
            state.add(new HQLParameter(MO_IDS_BINDING, qc.mapObjects));
        }

        if (!qc.messageIdSet.isEmpty())
        {
            state.sql.append("\n  and rdm.id in (:ids)");
            state.add(new HQLParameter("ids", qc.messageIdSet));
        }

        if (qc.hasDeviceDateRange)
        {
            state.sql.append("\n  and (rdm.hasTime=true and rdm.time >= :deviceMinDate)");
            state.sql.append("\n  and (rdm.hasTime=true and rdm.time <= :deviceMaxDate)");
            state.add(new HQLParameter("deviceMinDate", qc.deviceMinDate));
            state.add(new HQLParameter("deviceMaxDate", qc.deviceMaxDate));
        }

        if (qc.hasReceiveDateRange)
        {
            state.sql.append("\n  and rdm.receiveTime >= :receiveMinDate");
            state.sql.append("\n  and rdm.receiveTime <= :receiveMaxDate");
            state.add(new HQLParameter("receiveMinDate", qc.receiveMinDate));
            state.add(new HQLParameter("receiveMaxDate", qc.receiveMaxDate));
        }

        if (qc.weekDays != null)
        {
            // boy it would be cool if I could do "(dow from rdm.relevantTime at time zone 'UTC')"

            // Have to convert the day numbers to day names to keep this consistent across database providers.
            // Why?  HSQLDB uses 1 - 7 and Postgres uses 0 - 6 :(

            final List<String> dayNames = qc.weekDays.stream()
                    .map(day -> DAY_NAMES[day])
                    .collect(Collectors.toList());

            state.sql.append(" and {fn dayname(rdm.relevantTime)} in (:dayNames)");
            state.add(new HQLParameter("dayNames", dayNames));
        }

        if (qc.hasTimeBucketRange)
        {
            generateSqlForTimeBucketRange(qc.minBucketTime, qc.maxBucketTime);
        }

        if (qc.hasLocationRange)
        {
            generateSqlForLocationRange(qc.minLocation, qc.maxLocation, qc.strictCoordinates);
        }
    }

    /**
     * Generate the sql for any structure (message type) constraints.
     *
     * @since 2.38
     */
    private void generateSqlForStructureConstraints()
    {
        final Set<Integer> messageStructureIds = qc.getMessageStructureIds();

        if (!messageStructureIds.isEmpty())
        {
            state.sql.append("\n  and structure_id in (:structureIdList)");
            state.add(new HQLParameter("structureIdList", messageStructureIds));
        }
    }

    /**
     * Generate the hql for a location range constraint.  The min/max may be swapped to ensure that the least
     * longitude is used as the min corner, unless {@code strictCoordinates} is true.  In that case, if the
     * max corner is less than the min corner, SQL will be generated to handle the possible meridian crossing.
     *
     * @param minLocation       Min corner of range
     * @param maxLocation       Max corner of range
     * @param strictCoordinates If {@code true}, do not normalize min/max corners
     */
    private void generateSqlForLocationRange(ILocation minLocation, ILocation maxLocation, boolean strictCoordinates)
    {
        final double minLat = minLocation.getLatitude();
        final double minLon = minLocation.getLongitude();
        final double maxLat = maxLocation.getLatitude();
        final double maxLon = maxLocation.getLongitude();

        if (strictCoordinates)
        {
            // Strict interpretation of the min/max longitude.  This may require the generation of a select
            // that includes two distinct longitude regions.  Consider the case of a min lon of 170 and a
            // max lon of -170.  This region spans the date line, and it requires that we generate two
            // sets of where clauses, one for 170 to 180 and one for -170 to -180.

            if (minLon > maxLon)
            {
                // Handle date-line spanning coordinates
                state.sql.append(" and rdm.longitude >= :minLon");
                state.sql.append(" and rdm.longitude <= 180.0");
                state.add(new HQLParameter("minLon", minLon));

                state.sql.append(" and rdm.longitude <= :maxLon");
                state.sql.append(" and rdm.longitude >= -180.0");
                state.add(new HQLParameter("maxLon", maxLon));
            } else
            {
                // No special handling, just use what they said (strict interpretation)
                state.sql.append(" and rdm.longitude >= :minLon");
                state.add(new HQLParameter("minLon", minLon));

                state.sql.append(" and rdm.longitude <= :maxLon");
                state.add(new HQLParameter("maxLon", maxLon));
            }
        } else
        {
            // Normalize longitude
            state.sql.append(" and rdm.longitude >= :minLon");
            state.sql.append(" and rdm.longitude <= :maxLon");
            state.add(new HQLParameter("minLon", Math.min(minLon, maxLon)));
            state.add(new HQLParameter("maxLon", Math.max(minLon, maxLon)));
        }

        // Always normalize latitude
        state.sql.append(" and rdm.latitude >= :minLat");
        state.sql.append(" and rdm.latitude <= :maxLat");
        state.add(new HQLParameter("minLat", Math.min(minLat, maxLat)));
        state.add(new HQLParameter("maxLat", Math.max(minLat, maxLat)));

        // Only include the elevation if both locations include an elevation
        if (minLocation.hasElevation() && maxLocation.hasElevation())
        {
            // Intentionally avoid a "rdm.hasElevation" test so that messages with no
            // elevation will be included if the minElevation specified is 0

            final double minElev = minLocation.getElevation();
            final double maxElev = maxLocation.getElevation();

            state.sql.append(" and rdm.elevation >= :minElevation");
            state.sql.append(" and rdm.elevation <= :maxElevation");
            state.add(new HQLParameter("minElevation", Math.min(minElev, maxElev)));
            state.add(new HQLParameter("maxElevation", Math.max(minElev, maxElev)));
        }
    }

    /**
     * Generate the SQL fragment for a time bucket constraint.
     *
     * @param minBucketTime Min time of bucket
     * @param maxBucketTime Max time of bucket
     */
    private void generateSqlForTimeBucketRange(Time minBucketTime, Time maxBucketTime)
    {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(minBucketTime);
        int minHour = cal.get(Calendar.HOUR_OF_DAY);
        cal.setTime(maxBucketTime);
        int maxHour = cal.get(Calendar.HOUR_OF_DAY);

        if (minHour > maxHour)
        {
            // Since the times are being given to us in local time we need to adjust for the case that the
            // requested time value spans the midnight boundary. If the user is in AZ (GMT -7) and requests to
            // see tracks from 0600 to 0900 then this will be translated by Java/TAKX/Postgres to be 1100 to
            // 0200 which will result in zero rows.
            state.sql.append(" and ((cast(relevanttime as time) >= :minBucketTime");
            state.sql.append("       and cast(relevanttime as time) <= '23:59:59')");
            state.sql.append("   or (cast(relevanttime as time) >= '00:00:00'");
            state.sql.append("       and cast(relevanttime as time) <= :maxBucketTime))");
        } else
        {
            state.sql.append(" and cast(relevanttime as time) >= :minBucketTime");
            state.sql.append(" and cast(relevanttime as time) <= :maxBucketTime");
        }

        state.add(new HQLParameter("minBucketTime", minBucketTime));
        state.add(new HQLParameter("maxBucketTime", maxBucketTime));
    }

    /**
     * Generate a well formed "order by" clause and add it to the query.
     */
    private void generateOrderByClause()
    {
        if (qc.getSortOn() == null)
        {
            return;
        }
        String ordering = (qc.getSortOrder() == IQueryConstraints.SortOrder.ASCENDING) ? " asc" : " desc";

        switch (qc.getSortOn())
        {
            case DEVICE_DATE:
                state.sql.append(" order by rdm.time").append(ordering);
                break;
            case RECEIVE_DATE:
                state.sql.append(" order by rdm.receiveTime").append(ordering);
                break;
            case RELEVANT_TIME:
                state.sql.append(" order by rdm.relevantTime").append(ordering);
                break;
            case LOCATION:
                state.sql.append(" order by rdm.latitude").append(ordering).append(", rdm.longitude ").append(ordering);
                break;
            case FIELD:
                state.sql.append(" order by ");
                qc.getSortOnFieldSelector().appendSQLFieldName(state);
                state.sql.append(ordering);
        }
    }

    /**
     * Appends the SQL OFFSET clause, if an offset is specified.
     *
     * @param offset The number of items to offset the returned results by.
     */
    private void generateOffsetClause(int offset)
    {
        if (offset != 0)
        {
            state.sql.append(" offset ").append(offset);
        }
    }

    /**
     * Process all the fields specified in a {@link FieldSelector} and generate SQL for
     * them.
     *
     * @param fieldSelector Field selector to process
     */
    private void generateSelections(FieldSelector fieldSelector)
    {
        generateSqlForSelectors(fieldSelector.getCoreFieldSelectors());
        generateSqlForSelectors(fieldSelector.getExtendedFieldSelectors());

        // Don't emit aggregates in the group by clause
        if (state.getSqlClause() != SqlState.SqlClause.GroupBy)
        {
            generateSqlForSelectors(fieldSelector.getAggregateFieldSelectors());
        }
    }

    /**
     * Given a list of field selectors, generate the SQL for them.  These may be part of a select clause or a
     * group by clause.
     *
     * @param selectors Selectors to process
     */
    private void generateSqlForSelectors(List<? extends AFieldSelector> selectors)
    {
        for (AFieldSelector selector : selectors)
        {
            state.addLevelSeparator();
            selector.generateSQL(state);
        }
    }

    /**
     * Generate the SQL for a Collection of lists of Constraint instances.
     *
     * @param constraintSets Collection of lists of constraints to process
     */
    private void generateSqlForConstraintsCollection(Collection<List<IConstraint>> constraintSets)
    {
        constraintSets.forEach(this::generateSqlForConstraints);
    }

    /**
     * Generate the SQL for a list of Constraint instances.
     *
     * @param constraints The constraints to process
     */
    private void generateSqlForConstraints(Collection<IConstraint> constraints)
    {
        for (IConstraint cons : constraints)
        {
            state.addLevelSeparator();
            cons.generateSQL(state);
        }
    }

    /**
     * Generate the SQL for the subconstraints of a logic constraint.  We need slightly special handling here to get
     * the proper level separator in place (to get the logic operator specified).
     *
     * @param constraint The Logic Constraint to process
     */
    public void generateSqlForLogicSubconstraints(LogicConstraint constraint)
    {
        state.pushLevelSeparator("", " " + constraint.getLogicType().name() + " ");
        generateSqlForConstraints(constraint.getSubconstraints());
        state.popLevelSeparator();
    }

    /**
     * Generate the SQL for the "GROUP BY" and "HAVING" clauses.
     *
     * @param fieldSelector The field selector
     */
    private void generateGroupByClause(FieldSelector fieldSelector)
    {
        if (!qc.grouping) return;

        // We must have fields...

        state.setSqlClause(SqlState.SqlClause.GroupBy);

        // The "GROUP BY" fields must be generated identically (re: table names, extended fields, etc) to the
        // "SELECT" fields.  The only difference will be that the "GROUP BY" generation needs to ignore
        // aggregate fields.

        state.pushLevelSeparator("\n group by ", ", ");
        generateSelections(fieldSelector);
        state.popLevelSeparator();

        state.setSqlClause(SqlState.SqlClause.Constraint);

        // The groupConstraints appear in the "HAVING" clause and need to be generated identically to the
        // "WHERE" clause.  The distinction will be that the "HAVING" clause can contain AggregateFieldConstraints.
        // Importantly, parameters in "HAVING" need to augment the "WHERE" parameters and then parameter names
        // in HAVING need to be distinct from names in WHERE.
        state.pushLevelSeparator("\n having ", "\n and ");
        generateSqlForConstraints(qc.groupConstraints);
        state.popLevelSeparator();
    }
}
