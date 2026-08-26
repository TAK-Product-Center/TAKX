package gov.takx.dataservices.utils;

import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.DataFieldBuilder;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.IStructure;
import gov.takx.platform.KeyUtils;
import gov.takx.test.AContainerTest;
import gov.takx.test.PersistTestSetup;
import gov.takx.test.datasim.BloodHoundSim;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to validate the generation of enhanced facades.
 *
 * @since 2.22
 */
public class EnhancedFacadeGeneratorTest extends AContainerTest
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String BH0_KEY = KeyUtils.getUniqueKey(BloodHoundSim.BLOODHOUND_FAMILY, BloodHoundSim.BLOODHOUND_TYPE, "0");

    private PersistTestSetup test;
    private IRaptorDataStructure rds;
    private IStructure structure;
    private IMapObject bhMo0;

    @BeforeAll
    public void setup()
    {
        if (logger.isDebugEnabled()) logger.debug("{} is starting", getClass().getCanonicalName());

        test = new PersistTestSetup();
        test.setup();

        rds = new DataStructureBuilder(BloodHoundSim.BLOODHOUND_FAMILY, BloodHoundSim.BLOODHOUND_TYPE, "1.0.0", "FACADE_MESSAGE")
                // Previously, this attempt at code injection would have caused a failure when compiling the dynamic getter method.
                // Now, it is properly sanitized, it compiles, and no potential RCE exists.
                .addInt("X(){System\\u002eexit(0);return\\u0020null;}\\u002f\\u002f")
                .addString("key")
                .addBoolean("visible")
                .build();
        structure = rds.getStructure();

        bhMo0 = test.getMapObject(BH0_KEY);
    }

    @AfterAll
    public void teardown()
    {
        test.shutdown();
        destroyProject();
    }

    @Test
    public void testCamelCaseNameGeneration()
    {
        assertThat(getCamelCaseName("UPPER")).isEqualTo("UPPER");
        assertThat(getCamelCaseName("lower")).isEqualTo("Lower");
        assertThat(getCamelCaseName("field with-special.characters/in it")).isEqualTo("FieldWithSpecialCharactersInIt");
        assertThat(getCamelCaseName("foo bar-baz")).isEqualTo("FooBarBaz");

        // A potential attack (now prevented) that could generate a getter method that causes a call to System.exit
        assertThat(getCamelCaseName("X(){System\\u002eexit(0);return\\u0020null;}\\u002f\\u002f")).isEqualTo("XSystemU002eexit0ReturnU0020nullU002fU002f");
    }

    /**
     * Construct a DataField instance and then get the camel case name for that field from the enhanced facade generator.
     *
     * @param fieldName Name of field to use
     * @return camel case version of field name
     * @since 2.35
     */
    private String getCamelCaseName(String fieldName)
    {
        return EnhancedFacadeGenerator.makeCamelCaseName(new DataFieldBuilder(fieldName, 0).build());
    }

    /**
     * Validate that the enhanced facade class can be generated and can produce instances.
     */
    @SuppressLogging(EnhancedFacadeGenerator.class)
    @Test
    public void testFacadeGeneration()
    {
        final var facadeGenerator = EnhancedFacadeGenerator.createFacadeGenerator(structure, BloodHoundFacade.class);

        assertThat(facadeGenerator).isNotNull();

        final IPrePersistRaptorDataMessage rdm = rds.createRaptorDataMessage(bhMo0);
        final BloodHoundFacade facade = facadeGenerator.apply(rdm);

        assertThat(facade.getClass()).isNotSameAs(BloodHoundFacade.class);
        assertThat(facade.rdm()).isSameAs(rdm);
    }

    /**
     * Validate that, when using the enhanced facade, no method invocations take place on the base facade class.
     * This also validates that the setters/getters work as expected.
     */
    @SuppressLogging(EnhancedFacadeGenerator.class)
    @Test
    public void testNoBaseClassMethodInvocations()
    {
        final var facadeGenerator = EnhancedFacadeGenerator.createFacadeGenerator(structure, BloodHoundFacade.class);
        assertThat(facadeGenerator).isNotNull();
        final IPrePersistRaptorDataMessage rdm = rds.createRaptorDataMessage(bhMo0);
        final BloodHoundFacade facade = facadeGenerator.apply(rdm);

        assertThat(facade.isKeyNull()).isTrue();
        facade.setKey("foobar");
        assertThat(facade.getKey()).isEqualTo("foobar");
        assertThat(facade.isKeyNull()).isFalse();
        facade.setKeyNull();
        assertThat(facade.isKeyNull()).isTrue();

        assertThat(facade.isVisibleNull()).isTrue();
        facade.setVisible(true);
        assertThat(facade.getVisible()).isTrue();
        assertThat(facade.isVisibleNull()).isFalse();
        facade.setVisibleNull();
        assertThat(facade.isVisibleNull()).isTrue();

        assertThat(facade.methodCalled).withFailMessage("No methods should have been called").isFalse();
    }

    /**
     * Hard-coded facade (but structurally equivalent to one generated by the BS plugin).
     */
    public static class BloodHoundFacade
    {
        boolean methodCalled; // For tracking method invocations

        private static volatile boolean enhancedGeneratorConstructed;
        private static Function<IPrePersistRaptorDataMessage, ? extends BloodHoundFacade> generator = BloodHoundFacade::new;

        private final IPrePersistRaptorDataMessage rdm;

        /**
         * Static factory method to produce a facade instance from a given RDM.
         */
        public static BloodHoundFacade from(IPrePersistRaptorDataMessage rdm)
        {
            if (!enhancedGeneratorConstructed)
            {
                generator = EnhancedFacadeGenerator.createFacadeGenerator(rdm.getStructure(), BloodHoundFacade.class);
                enhancedGeneratorConstructed = true;
            }
            assertThat(generator).isNotNull();
            return generator.apply(rdm);
        }

        /**
         * Construct facade for a given RDM.
         *
         * @param rdm The source RDM on which to create this facade
         */
        protected BloodHoundFacade(IPrePersistRaptorDataMessage rdm)
        {
            this.rdm = rdm;
        }

        /**
         * @return The source RDM on which this facade was generated
         */
        public IPrePersistRaptorDataMessage rdm()
        {
            return rdm;
        }

        public String getKey()
        {
            methodCalled = true;
            return rdm.getString("Key");
        }

        public void setKey(String value)
        {
            methodCalled = true;
            rdm.setString("Key", value);
        }

        public boolean isKeyNull()
        {
            methodCalled = true;
            return rdm.isNull("Key");
        }

        public void setKeyNull()
        {
            methodCalled = true;
            rdm.setField("Key", null);
        }

        public boolean getVisible()
        {
            methodCalled = true;
            return rdm.getBoolean("Visible");
        }

        public void setVisible(boolean value)
        {
            methodCalled = true;
            rdm.setBoolean("Visible", value);
        }

        public boolean isVisibleNull()
        {
            methodCalled = true;
            return rdm.isNull("Visible");
        }

        public void setVisibleNull()
        {
            methodCalled = true;
            rdm.setField("Visible", null);
        }
    }
}
