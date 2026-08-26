package gov.takx.core.persist;

import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.RaptorDataMessageColumns;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * This class handles the parsing of a given HQL query for RDMs. It handles a subset of the full HQL, just enough
 * to handle the typical queries used in TAKX.
 * <p>
 * Query restrictions:
 * <ul>
 * <li>queries MUST include a "from RaptorDataMessage rdm" clause, and 'rdm' must be the table alias</li>
 * <li>All field references must be qualified with the table alias</li>
 * <li>Functions may not reference any special fields (UUIDs, or table links)</li>
 * <li></li>
 * </ul>
 * <p>
 * This class is thread-safe.
 *
 * @see RDMQuery
 * @see RDMQueryProcessor
 * @since 2.14
 */
@ApplicationScoped
public class RDMQueryParser
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Pattern to match comparator use
    private static final Pattern COMPARATOR_PATTERN = Pattern.compile("^([a-zA-Z0-9_.]+)(<|<=|>|>=|=|!=)(.+)$");

    // Pattern to locate bindings in SQL
    private static final Pattern BINDING_PATTERN = Pattern.compile(":[a-zA-Z0-9_]+");

    // Pattern to locate bindings in a token being parsed
    private static final Pattern BINDING_IN_TOKEN_PATTERN = Pattern.compile(".*(:[a-zA-Z0-9_]+).*");

    private static final String[] UUID_FIELD_NAMES = {"uuid", "originuuid"};

    private static final String ALL_RDM_COLUMNS_WITH_ALIAS = RaptorDataMessageColumns.getAllRdmColumnsWithAlias("rdm.");

    // Various parsing tokens
    private static final String COUNT = "count";
    private static final String DISTINCT = "distinct";
    private static final String FROM = "from";
    private static final String GROUP = "group";
    private static final String IS = "is";
    private static final String JOIN = "join";
    private static final String ORDER = "order";
    private static final String RDM = "rdm";
    private static final String SELECT = "select";
    private static final String WHERE = "where";
    private static final String EQUALS = "=";
    private static final String NOT_EQUALS = "!=";
    private static final String NOT = "not";
    private static final String NULL = "null";
    private static final String MSB = "_msb";
    private static final String LSB = "_lsb";
    private static final String RAPTOR_DATA_MESSAGE = "raptordatamessage";
    private static final String STRUCTURE = "structure";
    private static final String MAP_OBJECT = "mapobject";
    private static final String DOT = ".";
    private static final String OPEN_PAREN = "(";
    private static final String CLOSE_PAREN = ")";
    private static final char SPACE = ' ';

    private final Set<String> rdmColumnNames = new HashSet<>();

    @PostConstruct
    private void postConstruct()
    {
        // Grab the RDM "all columns" and convert it into a set we can test against
        Stream.of(RaptorDataMessageColumns.ALL_RDM_COLUMNS_ARRAY)
                .map(String::trim)
                .map(n -> n.endsWith(MSB) || n.endsWith(LSB) ? n.substring(0, n.length() - MSB.length()) : n)
                .forEach(rdmColumnNames::add);
    }

    /**
     * Create an RDMQuery from the given raw SQL.  The given SQL is not parsed for HQL support.
     *
     * @param sql        SQL to process
     * @param asMessages Pass {@code true} if this SQL results should be reified as RDMs, {@code false} for arbitrary selects
     * @return Newly constructed RDMQuery
     * @since 2.35
     */
    RDMQuery fromSql(String sql, boolean asMessages)
    {
        // Process the SQL to do two things:
        //
        // 1. Find all the parameter bindings and construct an ordered list of them
        // 2. Add a space after every use of a binding in the original SQL so we can properly handle
        //    bindings that are prefixes of other bindings.

        final Matcher matcher = BINDING_PATTERN.matcher(sql);
        final List<String> bindings = new ArrayList<>();
        final StringBuilder sqlBuffer = new StringBuilder();

        while (matcher.find())
        {
            bindings.add(matcher.group()); // Add binding
            matcher.appendReplacement(sqlBuffer, "$0 "); // Add a space
        }
        matcher.appendTail(sqlBuffer);

        final String updatedSql = sqlBuffer.toString();

        if (logger.isDebugEnabled())
        {
            logger.debug("sql: {}", updatedSql);
            logger.debug("bindings: {}", bindings);
        }

        return new RDMQuery(updatedSql, updatedSql, bindings, !asMessages);
    }

    /**
     * Parse the given HQL query string into an executable {@link RDMQuery}.
     *
     * @param hql HQL query to parse
     * @return RDMQuery instance
     * @throws SQLException If there are errors parsing the given hql into sql
     */
    public RDMQuery parse(String hql) throws SQLException
    {
        if (logger.isDebugEnabled()) logger.debug("Parse: {}", hql);

        final ParseState state = new ParseState(hql);

        processSelectClause(state);

        if (!state.atEnd())
        {
            // Process any/all join clauses
            while (state.currentToken().equalsIgnoreCase(JOIN)) processJoinClause(state);
        }

        if (!state.atEnd())
        {
            processWhereClause(state);
        }

        while (!state.atEnd())
        {
            state.append(state.takeAndNext()); // Just copy everything after the where (like order by)
        }

        return state.isCreatingNewClass()
                ? new RDMQuery(state.originalHql, state.getQuerySql(), state.getBindings(), state.getNewClassName())
                : new RDMQuery(state.originalHql, state.getQuerySql(), state.getBindings(), state.isCustomSelect());
    }

    /**
     * Process the select portion of the query.  This is an optional "select" clause with field names
     * (select new ClassName(args) is supported) and a non-optional "from" clause that must specify {@link IRaptorDataMessage}.
     *
     * @param state Parser state
     */
    private void processSelectClause(ParseState state) throws SQLException
    {
        boolean selectSeen = false;

        // If the hql starts with the work 'select' then they are either specifying the columns to select, or it
        // might be "select rdm" which still means select the RDM as a whole.

        String token = state.currentToken();

        if (token.equalsIgnoreCase(SELECT))
        {
            selectSeen = true;
            state.next();
            token = state.currentToken();

            switch (token)
            {
                case RDM:
                    state.next(); // Skip 'rdm'
                    state.addDefaultSelect(); // Insert default all columns select
                    break;
                case "new":
                    // Handle 'select new' operation
                    state.next(); // Skip the 'new'
                    processSelectNew(state);
                    break;
                default:
                    // Process custom select clause (deal with multi-column property expansion)
                    processCustomSelect(state);
                    break;
            }
        }

        if (!state.currentToken().equalsIgnoreCase(FROM))
        {
            throw new SQLException("Missing 'from' clause: " + state.originalHql);
        }
        state.next(); // Skip FROM

        final String tableName = state.takeAndNext();
        if (!tableName.equalsIgnoreCase(RAPTOR_DATA_MESSAGE))
        {
            throw new SQLException("Unknown table name '" + tableName + "' in: " + state.originalHql);
        }

        // Allow them to leave out the 'rdm' table alias, we will add it to the raw sql automatically
        if (!state.atEnd())
        {
            if (state.currentToken().equalsIgnoreCase(RDM)) state.next(); // Skip the 'rdm'
        }

        if (!selectSeen)
        {
            state.addDefaultSelect(); // Insert default all columns select
        }
    }

    /**
     * Process a "select new ClassName(f1, f2, ...) from ..." construction.  We need to extract the class name
     * and then process the parameters (f1, f2, ...) as the actual select clause for the overall query.
     * <p>
     * Note: composite field references (UUIDs) are not supported in select new constructs.
     *
     * @param state Parser state
     */
    private void processSelectNew(ParseState state) throws SQLException
    {
        final StringBuilder selectClause = new StringBuilder();

        // 'select' has already been eaten, so add that immediately
        selectClause.append(SELECT).append(SPACE);

        final String className = state.takeAndNext(); // Caller already skipped past "new"
        state.setNewClassName(className);

        if (!state.currentToken().equalsIgnoreCase(OPEN_PAREN))
        {
            throw new SQLException("Missing '(' on SELECT NEW construct: " + state.originalHql);
        }

        state.next();
        int depth = 1; // So we can find the proper closing paren

        while (depth > 0 && !state.atEnd())
        {
            String token = state.currentToken();

            switch (token)
            {
                case OPEN_PAREN:
                    depth += 1;
                    break;
                case CLOSE_PAREN:
                    depth -= 1;
                    break;
                default:
                    if (isFieldRef(token))
                    {
                        final FieldReference fieldReference = new FieldReference(token, state);
                        token = fieldReference.fieldNameForSql;
                    }
                    break;
            }

            // Avoid adding our final closing paren
            if (depth != 0) selectClause.append(token).append(SPACE);
            state.next();
        }

        state.setSelectSql(selectClause.toString());
    }

    /**
     * Process all other forms of select, which will be an arbitrary list of fields.  The selections can include
     * function references (like count, max, min, etc.), but no processing of the functions is performed, so
     * you can not use references to a composite (UUID) field as a function argument.
     *
     * @param state Parser state
     */
    private void processCustomSelect(ParseState state)
    {
        final StringBuilder selectClause = new StringBuilder();

        // 'select' has already been eaten, so add that immediately
        selectClause.append(SELECT).append(SPACE);

        String token;
        while (!state.atEnd() && !(token = state.currentToken()).equalsIgnoreCase(FROM))
        {
            // If it has a dot, then it's a field reference (I know I'm ignoring floating point numbers)
            if (isFieldRef(token))
            {
                final FieldReference fieldReference = new FieldReference(token, state);

                if (isUuidFieldName(fieldReference))
                {
                    // Add in the MSB and LSB components
                    selectClause
                            .append(fieldReference.fieldNameForSql).append(MSB).append(", ")
                            .append(fieldReference.fieldNameForSql).append(LSB);
                } else
                {
                    selectClause.append(fieldReference.fieldNameForSql);
                }
            } else
            {
                // Special handling for count(rdm), convert to count(*)
                if (token.equalsIgnoreCase(COUNT))
                {
                    // Should have 4 or 5 tokens: count ( [distinct] identifier )
                    state.next();
                    state.next();
                    String identifier = state.takeAndNext();
                    String distinct = "";

                    if (identifier.equalsIgnoreCase(DISTINCT))
                    {
                        distinct = DISTINCT + " ";
                        identifier = state.takeAndNext();
                    }

                    if (identifier.equalsIgnoreCase(RDM)) identifier = "*";

                    token = "count" + OPEN_PAREN + distinct + identifier + CLOSE_PAREN;
                }

                selectClause.append(token);
            }

            selectClause.append(SPACE);
            state.next();
        }

        if (logger.isDebugEnabled()) logger.debug("select clause:{}", selectClause);

        state.setSelectSql(selectClause.toString());
    }

    /**
     * Determine if a given token appears to be a field/column reference.  If the token contains a DOT ('.') or
     * it is equal to any of the known RDM field field/column names, then it is considered a field reference.
     *
     * @param token Token to inspect
     * @return {@code true} if it appears to be a field reference
     */
    private boolean isFieldRef(String token)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("isFieldRef: token={} dot={} isCol={}", token, token.contains(DOT), rdmColumnNames.contains(token));
        }

        return token.contains(DOT) || rdmColumnNames.contains(token);
    }

    /**
     * Process an explicit JOIN clause.  Grab the join components and add a {@link JoinDefinition} to the parsing state.
     *
     * @param state Parser state
     */
    private void processJoinClause(ParseState state) throws SQLException
    {
        if (!state.next()) throw new SQLException("Unexpected end looking for join column : " + state.originalHql);
        final String joinColumn = state.currentToken();

        final int dotIndex = joinColumn.indexOf('.');
        final String joinTable = dotIndex < 0 ? joinColumn : joinColumn.substring(dotIndex + 1);

        if (!state.next()) throw new SQLException("Unexpected end looking for join target alias: " + state.originalHql);
        final String joinAlias = state.currentToken();

        if (state.hasJoinToTable(joinTable))
        {
            throw new SQLException("Already have implicit join to '" + joinTable + "': " + state.originalHql);
        }

        state.addJoin(new JoinDefinition(joinTable, joinAlias, joinColumn));

        state.next();
    }

    /**
     * Process a WHERE clause.  Process nested selects, value bindings, and composite field (UUID) references.
     *
     * @param state Parser state
     */
    private void processWhereClause(ParseState state) throws SQLException
    {
        if (!state.currentToken().equalsIgnoreCase(WHERE))
        {
            throw new SQLException("Expecting 'WHERE', got '" + state.currentToken() + "': " + state.originalHql);
        }

        state.append(WHERE);

        while (state.next())
        {
            String token = state.currentToken();
            if (logger.isDebugEnabled()) logger.debug("token: {}", token);

            if (token.equalsIgnoreCase(SELECT) || token.equalsIgnoreCase(OPEN_PAREN + SELECT))
            {
                // Handle nested selects, which must be proper SQL, no name translation or joins are performed
                processNestedSelect(state);
            } else if (token.equalsIgnoreCase(ORDER) || token.equalsIgnoreCase(GROUP))
            {
                break; // That's the end of the where clause
            } else if (token.contains(":"))
            {
                processBinding(token, state);
            } else
            {
                // Our top-level tokenizer jus split on spaces, but there are queries that include things like
                // this: rdm.hasLocation=true, without spaces around the comparator.  So, let's fix that up.
                token = expandComparison(token, state);

                if (isFieldRef(token))
                {
                    // Simple token in the where clause, handle special field names
                    final FieldReference fieldReference = new FieldReference(token, state);

                    if (isUuidFieldName(fieldReference))
                    {
                        processUuidWhereClause(state);
                    } else
                    {
                        // Otherwise, the field ref has the proper sql for us
                        state.append(fieldReference.fieldNameForSql);
                    }
                } else
                {
                    state.append(token);
                }
            }
        }

        if (logger.isDebugEnabled()) logger.debug("WHERE done");
    }

    /**
     * Process a nested select.  We start on the 'select' token, just copy tokens into the main body sql.
     * The only processing we do is to identify and record value bindings.  We process tokens until we have
     * encountered the closing paren for the sub-select.
     *
     * @param state Parser state
     */
    private void processNestedSelect(ParseState state) throws SQLException
    {
        // We start on the 'select' token, just copy tokens into the main body sql.
        // The only processing we do is to identify and record value bindings.
        // We process tokens until we have encountered the closing paren for the sub-select.

        String token = state.takeAndNext();

        final boolean hasOpeningParen = token.startsWith(OPEN_PAREN);
        int parenDepth = StringUtils.countMatches(token, OPEN_PAREN)
                + (hasOpeningParen ? 0 : 1); // Handle implicit '('

        do
        {
            if (token.contains(":"))
            {
                processBinding(token, state);
            } else
            {
                if (token.equalsIgnoreCase(RAPTOR_DATA_MESSAGE)) token = "MapPoint";

                state.append(token);
            }

            parenDepth += StringUtils.countMatches(token, OPEN_PAREN) - StringUtils.countMatches(token, CLOSE_PAREN);
            if (parenDepth > 0) token = state.takeAndNext();
        } while (parenDepth > 0);

        // We leave with the current token being the one after the token with the closing paren.
    }

    /**
     * Process a specific value binding.
     *
     * @param token The token containing the binding
     * @param state Parser state
     */
    private void processBinding(String token, ParseState state) throws SQLException
    {
        final String binding = extractBindingName(token);
        if (binding == null)
        {
            throw new SQLException("Invalid binding reference '" + token + "': " + state.originalHql);
        }

        // To ensure that we properly handle binding names that are prefixes of other bindings, like
        // :foo and :foobar, we append a space to the binding name when we insert it into the sql we're
        // building.
        state.append(token.replace(binding, binding + SPACE));

        state.addBinding(binding);
    }

    /**
     * Extract the binding name from the given token.  It is a ':' followed by one or more alphanumerics or underscore.
     *
     * @param token Token to process
     * @return Extracted binding name, including the ':'
     */
    private String extractBindingName(String token)
    {
        final Matcher matcher = BINDING_IN_TOKEN_PATTERN.matcher(token);
        if (matcher.find())
        {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Process a where clause sub-expression that operates upon a composite (UUID) field.  Handle the two comparison
     * forms we support, nullity tests and equality/inequality tests.
     *
     * @param state Parser state
     */
    private void processUuidWhereClause(ParseState state) throws SQLException
    {
        final String fieldName = state.takeAndNext(); // Current token is the field name

        // Consume tokens for the comparison: support null tests (IS NULL, IS NOT NULL) and equality (=, !=)
        String token = state.takeAndNext();
        switch (token)
        {
            case IS:
                token = state.takeAndNext(); // NOT or NULL

                final boolean isNot = token.equalsIgnoreCase(NOT);
                if (isNot) token = state.takeAndNext(); // Must be "NULL"

                if (!token.equalsIgnoreCase(NULL))
                {
                    throw new SQLException("Expecting 'NULL' got '" + token + "': " + state.originalHql);
                }

                // Construct sql for this test (only need to test MSB for nullity
                state.append(fieldName + MSB).append(isNot ? " IS NOT NULL" : " IS NULL");
                break;

            case EQUALS:
            case NOT_EQUALS:
                final String comparator = token;
                final boolean isEquals = comparator.equalsIgnoreCase(EQUALS);
                final String binding = state.takeAndNext();

                state.append(OPEN_PAREN)
                        .append(fieldName + MSB).append(comparator).append("?")
                        .append(isEquals ? " and " : " or ")
                        .append(fieldName + LSB).append(comparator).append("?")
                        .append(CLOSE_PAREN);

                state.addBinding(binding); // binding, like ":uuid"
                break;

            default:
                throw new SQLException("Unsupported UUID comparator '" + token + "': ", state.originalHql);
        }
    }

    /**
     * Determine if the given field reference represents one of our UUID fields.
     *
     * @param fieldReference Parsed field reference to test
     * @return {@code true} if the token matches one of the UUID fields
     */
    private boolean isUuidFieldName(FieldReference fieldReference)
    {
        return fieldReference.isLocalFieldRef() && isFieldMatch(fieldReference.localFieldName, UUID_FIELD_NAMES);
    }

    /**
     * See if the given name is any of the given {@code wanted} names.
     *
     * @param name   The name to compare
     * @param wanted The list of wanted names
     * @return {@code true} if name is among the wanted list
     */
    private boolean isFieldMatch(String name, String... wanted)
    {
        return Stream.of(wanted).anyMatch(w -> w.equalsIgnoreCase(name));
    }

    /**
     * Given a token, see if it matches our "comparator" pattern, which basically matches an identifier being
     * compared to something, and extracts any opening or closing parentheses as well.  We need to catch
     * parentheses since the top-level split is just on whitespace, and if the query string doesn't include
     * whitespace around all tokens (which is common), then we'll fail to identify an actual column reference.
     * <p>
     * Consider the token "(rdm.hasLocation=true" which could easily be part of a conjugated where clause.  We
     * want to turn that into 4 tokens: '(', 'rdm.hasLocation', '=', and 'true'.
     *
     * @param token Current token
     * @param state Parsing state, used to push back identified tokens
     * @return The new current token (in case we performed push backs)
     */
    private String expandComparison(String token, ParseState state)
    {
        final Matcher matcher = COMPARATOR_PATTERN.matcher(token);
        if (matcher.matches())
        {
            state.next(); // Eat the token we're expanding

            state.pushBack(matcher.group(3), matcher.group(2), matcher.group(1));
        }

        return state.currentToken();
    }

    /**
     * A FieldReference helps pull apart the various forms of a field/column reference that we may encounter
     * in the hql being parsed.  This includes local field/column references and explicit or implicit join references.
     */
    private static final class FieldReference
    {
        final String name;
        final String[] parts;
        final boolean explicitJoinRef;
        final String localFieldName;
        final String fieldNameForSql;

        FieldReference(String name, ParseState state)
        {
            // Special case handling of unqualified RDM column name
            if (!name.contains(DOT)) name = RDM + DOT + name;

            this.name = name;
            parts = name.split("\\.");

            final int nParts = parts.length;

            explicitJoinRef = !parts[0].equalsIgnoreCase(RDM);

            // Yes, the following processing is extremely specific to the current object model, but as described above,
            // this code is all about speed and simplicity, I'm not trying to recreate hibernate.

            if (!explicitJoinRef)
            {
                // See if this is an implicit join ref, like rdm.structure.id or rdm.mapObject.deleted
                final boolean isJoin = nParts > 2;
                localFieldName = parts[1];
                final String remoteFieldName = isJoin ? parts[2] : null;

                if (isJoin)
                {
                    // Add an implicit join to the remote table, if we don't already have one
                    if (!state.hasJoinToTable(localFieldName))
                    {
                        state.addJoin(new JoinDefinition(localFieldName));
                    }
                }

                switch (localFieldName.toLowerCase())
                {
                    case MAP_OBJECT:
                    case STRUCTURE:
                        // If this is a join to a field in the named table, preserve remote field name,
                        // otherwise it is a reference to the <name>_id field

                        fieldNameForSql = RDM + DOT + (isJoin
                                ? localFieldName + DOT + remoteFieldName
                                : localFieldName + "_id");
                        break;

                    default:
                        fieldNameForSql = RDM + DOT + localFieldName;
                        break;
                }
            } else
            {
                localFieldName = null;
                fieldNameForSql = name; // Use the whole thing on explicit joins
            }

            if (logger.isDebugEnabled()) logger.debug("Created: {}", this);
        }

        boolean isLocalFieldRef()
        {
            return localFieldName != null;
        }

        @Override
        public String toString()
        {
            return "FieldReference{" +
                    "name='" + name + '\'' +
                    ", parts=" + Arrays.toString(parts) +
                    ", explicitJoinRef=" + explicitJoinRef +
                    ", localFieldName='" + localFieldName + '\'' +
                    ", fieldNameForSql='" + fieldNameForSql + '\'' +
                    '}';
        }
    }

    /**
     * Holds the definition of a JOIN operation.
     */
    private static final class JoinDefinition
    {
        final String table;
        final String alias;
        final String column;

        JoinDefinition(String fieldName)
        {
            this(fieldName, fieldName, fieldName);
        }

        JoinDefinition(String table, String alias, String column)
        {
            this.table = table;
            this.alias = alias;
            this.column = column;
        }

        /**
         * @return SQL string form of the join
         */
        public String toString()
        {
            // The transformation from hql column to sql is to simply add "_id" - naive, but sufficient for our model
            return JOIN + " " + table + " " + alias + " on " + column + "_id" + " = " + alias + ".id";
        }
    }

    /**
     * Our accumulated parsing state.  This state object externalizes all the dynamic state of the parsing operation,
     * allowing the parser instance to be thread-safe and usable by any number of simultaneous consumers.
     */
    private static final class ParseState
    {
        private static final Pattern WHITE_SPACE = Pattern.compile("\\s+");

        final String originalHql;
        final List<String> tokens;
        final int tokenCount;
        final StringBuilder mainSql = new StringBuilder();

        int tokenIndex = 0;
        boolean customSelect;
        String selectSql;
        final List<JoinDefinition> joinDefinitions = new ArrayList<>();
        final List<String> bindings = new ArrayList<>();
        final Deque<String> pushBack = new ArrayDeque<>();
        String newClassName = null;

        ParseState(String hql)
        {
            originalHql = hql;

            tokens = makeTokens(hql);
            tokenCount = tokens.size();
        }

        /**
         * Perform a simplistic top-level parse of the given HQL and create the tokens we will use as input to the
         * parsing process.  Initially split on whitespace, then break on parentheses and commas, but keep those
         * as distinct tokens.
         *
         * @param hql HQL to split into tokens
         * @return List of tokens
         */
        private List<String> makeTokens(String hql)
        {
            final String[] initialTokens = WHITE_SPACE.split(hql);

            // Now go through and split out any parentheses and commas into their own tokens
            List<String> tokens = new ArrayList<>(initialTokens.length * 2);

            for (String token : initialTokens)
            {
                int len = token.length();
                int start = 0;
                int index = 0;

                while (index < len)
                {
                    final char c = token.charAt(index);

                    if (c == '(' || c == ')' || c == ',')
                    {
                        if (start != index)
                        {
                            tokens.add(token.substring(start, index)); // grab everything before the paren
                        }

                        tokens.add(String.valueOf(c)); // Add paren
                        start = index + 1;
                    }
                    index++;
                }

                if (start != index)
                {
                    tokens.add(token.substring(start, index)); // grab everything after last paren
                }
            }

            return tokens;
        }

        /**
         * @return The final, constructed SQL resulting from parsing and processing the HQL
         */
        String getQuerySql()
        {
            return selectSql + " from MapPoint rdm " + StringUtils.join(joinDefinitions, " ") + " " + mainSql;
        }

        /**
         * @return The current token value
         */
        String currentToken()
        {
            if (!pushBack.isEmpty())
            {
                return pushBack.getFirst();
            } else
            {
                return atEnd() ? "$EOF$" : tokens.get(tokenIndex);
            }
        }

        /**
         * Retrieve the current token and advance the token index to the next token.
         *
         * @return Current token value
         */
        String takeAndNext()
        {
            final String token = currentToken();
            next();
            if (logger.isDebugEnabled()) logger.debug("takeAndNext, token={}", token);
            return token;
        }

        /**
         * Move to the next token.
         *
         * @return {@code true} if there are more tokens, {@code false} if this advance moved us past the last token
         */
        boolean next()
        {
            if (!pushBack.isEmpty())
            {
                pushBack.removeFirst();
            } else
            {
                tokenIndex += 1;
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("next(), token={}, index={}, atEnd={}, PB={}", currentToken(), tokenIndex, atEnd(), pushBack);
            }
            return !atEnd();
        }

        /**
         * @return {@code true} if the current token index is past the end and our push-back queue is empty
         */
        boolean atEnd()
        {
            return pushBack.isEmpty() && tokenIndex >= tokenCount;
        }

        /**
         * Push back a series of tokens.  These tokens will temporarily replace the primary token list
         * and tokens will be referenced from the push back list until all the tokens there are processed
         * via calls to {@link #next()} or {@link #takeAndNext()}.
         *
         * @param tokens List of tokens to push back, in reverse order, meaning the last token in the list
         *               will become the new "current" token
         */
        void pushBack(String... tokens)
        {
            for (String token : tokens)
            {
                pushBack.addFirst(token);
            }
        }

        /**
         * Add a default select statement (one that selects all RDM columns).
         */
        void addDefaultSelect()
        {
            // Insert default all columns select
            selectSql = SELECT + " " + ALL_RDM_COLUMNS_WITH_ALIAS;
            customSelect = false;
        }

        /**
         * Set a custom SQL select clause.
         *
         * @param selectSql The SQL to use as the select portion of the overall query
         */
        void setSelectSql(String selectSql)
        {
            this.selectSql = selectSql;
            customSelect = true;
        }

        /**
         * @return {@code true} if the select clause is not the default all columns select
         */
        boolean isCustomSelect()
        {
            return customSelect;
        }

        /**
         * Set the class name if we are performing a "select new" operation.
         *
         * @param className Class name
         */
        void setNewClassName(String className)
        {
            newClassName = className;
        }

        /**
         * @return Class name that was specified in a "select new"
         */
        public String getNewClassName()
        {
            return newClassName;
        }

        /**
         * @return {@code true} if this was a "select new" type query
         */
        boolean isCreatingNewClass()
        {
            return newClassName != null;
        }

        /**
         * Add a JOIN definition to the query.  It will be used to generate the final SQL.
         *
         * @param joinDefinition Definition of the JOIN
         */
        void addJoin(JoinDefinition joinDefinition)
        {
            joinDefinitions.add(joinDefinition);
        }

        /**
         * Determine if we already have a JOIN defined on the given table name.
         *
         * @param table Table name of interest
         * @return {@code true} if we already have a JoinDefinition containing the given table name
         */
        boolean hasJoinToTable(String table)
        {
            return joinDefinitions.stream().anyMatch(jc -> jc.table.equalsIgnoreCase(table));
        }

        /**
         * Add a binding name to the query state.
         *
         * @param name Binding name to add
         */
        void addBinding(String name)
        {
            bindings.add(name);
        }

        /**
         * @return The ordered list of binding names encountered in the parsed
         */
        @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
        List<String> getBindings()
        {
            return bindings;
        }

        /**
         * Append a string to our "main" SQL body.  The main body collects pretty much everything except the
         * SELECT and JOIN clauses.
         *
         * @param fragment Text to add to main SQL body
         * @return This, for method chaining
         */
        ParseState append(String fragment)
        {
            mainSql.append(fragment).append(SPACE);
            return this;
        }
    }
}
