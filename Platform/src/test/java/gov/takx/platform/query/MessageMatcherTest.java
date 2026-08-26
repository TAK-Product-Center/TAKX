package gov.takx.platform.query;

import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.query.ComparisonType;
import gov.takx.api.query.ConflictingConstraintException;
import gov.takx.api.query.CoreField;
import gov.takx.api.query.FieldConstraint;
import gov.takx.api.query.IQueryConstraints;
import gov.takx.api.query.IQueryService;
import gov.takx.api.query.LogicConstraint;
import gov.takx.api.query.LogicType;
import gov.takx.api.query.NoSuchFieldException;
import gov.takx.api.query.StructureFieldSelector;
import gov.takx.core.messagequery.PluginQueryService;
import gov.takx.test.AContainerTest;
import gov.takx.test.PersistTestSetup;
import gov.takx.test.datasim.BloodHoundSim;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Time;
import java.util.Calendar;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for {@link MessageMatcher}.
 *
 * @since 1.49
 */
public class MessageMatcherTest extends AContainerTest
{
    private PersistTestSetup test;
    private BloodHoundSim sim;
    private IRaptorDataStructure structure;
    private IMapObject mapObject;

    @BeforeAll
    public void setup()
    {
        test = new PersistTestSetup();
        test.init();

        mapObject = test.persistBloodHound(1, PersistTestSetup.OFFSET, 0);
        sim = new BloodHoundSim(false);
        structure = test.getBloodhoundStruct();
    }

    @AfterAll
    public void teardown()
    {
        test.shutdown();
    }

    @Test
    public void testEmptyConstraint()
    {
        IQueryService qs = createPluginQueryService();
        IQueryConstraints qc = qs.createEmptyConstraints();

        final IPrePersistRaptorDataMessage rdm = makeRDM();

        assertThat(qs.matches(rdm, qc)).isTrue();
    }

    @Test
    public void testBasics() throws ConflictingConstraintException
    {
        IQueryService qs = createPluginQueryService();
        IQueryConstraints qc = qs.createEmptyConstraints();

        final long now = System.currentTimeMillis();

        final IPrePersistRaptorDataMessage rdm = makeRDM();
        rdm.setTime(now);
        rdm.setLocation(50, 50);

        qc.addLocationRange(new RaptorLocation(20, 20), new RaptorLocation(60, 60));
        assertThat(qs.matches(rdm, qc)).isTrue();

        qc.clear();
        qc.addDeviceDateRange(now - 10, now + 10);
        assertThat(qs.matches(rdm, qc)).isTrue();

        qc.clear();
        qc.addDeviceDateRange(now - 10, now + 10);
        qc.addLocationRange(new RaptorLocation(20, 20), new RaptorLocation(60, 60));
        assertThat(qs.matches(rdm, qc)).isTrue();

        qc.clear();
        qc.addReceiveDateRange(rdm.getReceiveTime(), rdm.getReceiveTime());
        assertThat(qs.matches(rdm, qc)).isTrue();

        // Day of week tests
        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK) - 1; // We use 0 - 6

        qc.clear();
        qc.addDayOfWeekConstraint(new int[]{dayOfWeek});
        assertThat(qs.matches(rdm, qc)).isTrue();

        rdm.setTime(now + 25 * 60 * 60 * 1000); // +25 hours (different day)
        assertThat(qs.matches(rdm, qc)).isFalse();

        // Time buckets
        qc.clear();
        qc.addTimeBucketRange(new Time(now - 10), new Time(now + 10_000));
        rdm.setTime(now);
        assertThat(qs.matches(rdm, qc)).isTrue();

        rdm.setTime(now + 24 * 60 * 60 * 1000); // +24 hours (same bucket, new day)
        assertThat(qs.matches(rdm, qc)).isTrue();

        rdm.setTime(rdm.getTime() + 60 * 60 * 1000); // +1 more hour (outside bucket)
        assertThat(qs.matches(rdm, qc)).isFalse();
    }

    @Test
    public void testAllComparators() throws ConflictingConstraintException, NoSuchFieldException
    {
        IQueryService qs = createPluginQueryService();
        IQueryConstraints qc = qs.createEmptyConstraints();

        final IPrePersistRaptorDataMessage rdm = makeRDM();
        rdm.setHeading(50.0);
        rdm.setDouble("Voltage", 7.35);

        // Core fields

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.EQ, "TESTING");
        assertThat(qs.matches(rdm, qc)).isTrue();

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.NEQ, "TESTING");
        assertThat(qs.matches(rdm, qc)).isFalse();

        qc.clear();
        qc.addFieldConstraint(CoreField.heading, ComparisonType.GT, 50.0);
        assertThat(qs.matches(rdm, qc)).isFalse();

        qc.clear();
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 50.0);
        assertThat(qs.matches(rdm, qc)).isFalse();

        qc.clear();
        qc.addFieldConstraint(CoreField.heading, ComparisonType.GTE, 50.0);
        assertThat(qs.matches(rdm, qc)).isTrue();

        qc.clear();
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LTE, 50.0);
        assertThat(qs.matches(rdm, qc)).isTrue();

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.ISNULL, null);
        assertThat(qs.matches(rdm, qc)).isFalse();

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.ISNOTNULL, null);
        assertThat(qs.matches(rdm, qc)).isTrue();

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.LIKE, "%TING");
        assertThat(qs.matches(rdm, qc)).isTrue();

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.NOTLIKE, "%TING");
        assertThat(qs.matches(rdm, qc)).isFalse();

        // Structure fields

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Height", ComparisonType.EQ, 42);
        assertThat(qs.matches(rdm, qc)).isTrue();

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Height", ComparisonType.NEQ, 42);
        assertThat(qs.matches(rdm, qc)).isFalse();

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.GT, 7.35);
        assertThat(qs.matches(rdm, qc)).isFalse();

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.LT, 7.35);
        assertThat(qs.matches(rdm, qc)).isFalse();

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.GTE, 7.35);
        assertThat(qs.matches(rdm, qc)).isTrue();

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.LTE, 7.35);
        assertThat(qs.matches(rdm, qc)).isTrue();

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.ISNULL, null);
        assertThat(qs.matches(rdm, qc)).isFalse();

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "Voltage", ComparisonType.ISNOTNULL, null);
        assertThat(qs.matches(rdm, qc)).isTrue();

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "MessageType", ComparisonType.LIKE, "%STATUS%");
        assertThat(qs.matches(rdm, qc)).isTrue();

        qc.clear();
        qc.addFieldConstraint("BH_STATUS_MESSAGE", "MessageType", ComparisonType.NOTLIKE, "%STATUS%");
        assertThat(qs.matches(rdm, qc)).isFalse();
    }

    @Test
    public void testLikeConstraints()
    {
        IQueryService qs = createPluginQueryService();
        IQueryConstraints qc = qs.createEmptyConstraints();

        IPrePersistRaptorDataMessage rdm = structure.createRaptorDataMessage("0");

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.ISNULL, null);
        assertThat(qs.matches(rdm, qc)).isTrue();

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.ISNOTNULL, null);
        assertThat(qs.matches(rdm, qc)).isFalse();

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.NOTLIKE, "%TING");
        assertThat(qs.matches(rdm, qc)).isTrue(); // null always satisfies NOT LIKE

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.LIKE, "%TING");
        assertThat(qs.matches(rdm, qc)).isFalse(); // null never satisfies LIKE

        // Now some more complicated patterns
        rdm.setSourceName("I have a . a / and a $ in me");

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.LIKE, "I%e");
        assertThat(qs.matches(rdm, qc)).isTrue();

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.LIKE, "I HAVE A%IN ME");
        assertThat(qs.matches(rdm, qc)).isTrue();

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.LIKE, "I%.%/%$ in me");
        assertThat(qs.matches(rdm, qc)).isTrue();

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.LIKE, "I have a _ a _ and a _ in me");
        assertThat(qs.matches(rdm, qc)).isTrue();

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.LIKE, "I%.%/%$ in");
        assertThat(qs.matches(rdm, qc)).isFalse();

        // Verify escaping of all special chars
        rdm.setSourceName("All the things: .^$*+?|()[]{}\\ and more");

        qc.clear();
        qc.addFieldConstraint(CoreField.sourceName, ComparisonType.LIKE, "% .^$*+?|()[]{}\\ %");
        assertThat(qs.matches(rdm, qc)).isTrue();
    }

    @Test
    public void testLogicConstraintOnCoreFields()
            throws ConflictingConstraintException, IllegalStateException
    {
        IQueryService qs = createPluginQueryService();
        IQueryConstraints qc = qs.createEmptyConstraints();

        final IPrePersistRaptorDataMessage rdm = makeRDM();
        rdm.setHeading(50.0);

        // NOT (heading < 50)
        qc.addLogicConstraint(LogicType.not, new FieldConstraint(CoreField.heading.selector, ComparisonType.LT, 50.));
        assertThat(qs.matches(rdm, qc)).isTrue();

        //
        // Simple conjunct/disjunct
        //
        qc.clear();
        qc.addLogicConstraints(new LogicConstraint(LogicType.or,
                new FieldConstraint(CoreField.heading.selector, ComparisonType.EQ, 50.),
                new FieldConstraint(CoreField.heading.selector, ComparisonType.EQ, 60.)));
        assertThat(qs.matches(rdm, qc)).isTrue();

        //
        // Negated logic constraint
        //
        qc.clear();
        qc.addLogicConstraint(LogicType.not,
                new LogicConstraint(LogicType.or,
                        new FieldConstraint(CoreField.heading.selector, ComparisonType.LT, 50.),
                        new FieldConstraint(CoreField.heading.selector, ComparisonType.GT, 60.)));
        assertThat(qs.matches(rdm, qc)).isTrue();

        //
        // List of negated constraints
        //
        qc.clear();
        qc.addLogicConstraints(
                new LogicConstraint(LogicType.not, new FieldConstraint(CoreField.heading.selector, ComparisonType.LT, 50.)),
                new LogicConstraint(LogicType.not, new FieldConstraint(CoreField.heading.selector, ComparisonType.GT, 60.)));
        assertThat(qs.matches(rdm, qc)).isTrue();
    }

    @Test
    public void testLogicConstraintOnExtendedFields()
            throws ConflictingConstraintException, IllegalStateException
    {
        IQueryService qs = createPluginQueryService();
        IQueryConstraints qc = qs.createEmptyConstraints();

        final IPrePersistRaptorDataMessage rdm = makeRDM();
        rdm.setHeading(30.0);
        rdm.setDouble("Voltage", 8);
        rdm.setBoolean("Panic", true);

        final StructureFieldSelector panicField = new StructureFieldSelector("BH_STATUS_MESSAGE", "Panic");
        final StructureFieldSelector voltageField = new StructureFieldSelector("BH_STATUS_MESSAGE", "Voltage");

        qc.addLogicConstraint(LogicType.or,
                new LogicConstraint(LogicType.and,
                        new LogicConstraint(LogicType.not, new FieldConstraint(panicField, ComparisonType.NEQ, true)),
                        new FieldConstraint(voltageField, ComparisonType.GT, 7.55),
                        new FieldConstraint(voltageField, ComparisonType.LT, 7.85)),

                new LogicConstraint(LogicType.and,
                        new LogicConstraint(LogicType.not, new FieldConstraint(panicField, ComparisonType.NEQ, false)),
                        new FieldConstraint(voltageField, ComparisonType.GT, 7.65),
                        new FieldConstraint(voltageField, ComparisonType.LT, 7.95)));
        qc.addFieldConstraint(CoreField.heading, ComparisonType.LT, 35.0);

        // Initial fail since neither clause will match
        assertThat(qs.matches(rdm, qc)).isFalse();

        // Now match first or sub-clause
        rdm.setDouble("Voltage", 7.60);
        assertThat(qs.matches(rdm, qc)).isTrue();

        // Now match second or sub-clause
        rdm.setBoolean("Panic", false);
        rdm.setDouble("Voltage", 7.70);
        assertThat(qs.matches(rdm, qc)).isTrue();

        // Now fail on heading
        rdm.setHeading(50);
        assertThat(qs.matches(rdm, qc)).isFalse();
    }

    /**
     * Create a Query Service for the given plugin configuration
     *
     * @return newly minted query service
     */
    private PluginQueryService createPluginQueryService()
    {
        final Set<IRaptorDataStructure> structs = Collections.singleton(structure);
        return new PluginQueryService(test.getMapObject(BloodHoundSim.makeUnitIdKey("0")), structs);
    }

    private IPrePersistRaptorDataMessage makeRDM()
    {
        return sim.makeDataMessage(structure, mapObject);
    }
}
