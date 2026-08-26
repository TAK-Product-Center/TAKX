package gov.takx.test.extension;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Ensures that the test orderer correctly determines which tests ought to be run before others due to dependencies
 * on other test methods.
 *
 * @see DependsOn Annotation used.
 * @see DependsOnOrdering JUnit method orderer strategy utilizing the annotation to enforce dependent order.
 * @since 3.4.0
 */
@SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
class DependsOnOrderingTest
{
    static int counterA;
    static int counterB;
    static int counterC;
    static int counterD;
    static int counterFiveA;
    static int counterFiveB;

    /**
     * Simple test to validate method declaration order does not affect sorted order.
     */
    @Nested
    @TestMethodOrder(DependsOnOrdering.class)
    class TripleVariantA
    {
        @Test
        @DependsOn("second")
        void last()
        {
            Assertions.assertThat(counterA++).isEqualTo(2);
        }

        @Test
        @DependsOn("first")
        void second()
        {
            Assertions.assertThat(counterA++).isEqualTo(1);
        }

        @Test
        void first()
        {
            Assertions.assertThat(counterA++).isEqualTo(0);
        }
    }

    /**
     * Simple test to validate method declaration order does not affect sorted order.
     */
    @Nested
    @TestMethodOrder(DependsOnOrdering.class)
    class TripleVariantB
    {

        @Test
        @DependsOn("first")
        void second()
        {
            Assertions.assertThat(counterB++).isEqualTo(1);
        }

        @Test
        @DependsOn("second")
        void last()
        {
            Assertions.assertThat(counterB++).isEqualTo(2);
        }

        @Test
        void first()
        {
            Assertions.assertThat(counterB++).isEqualTo(0);
        }
    }

    /**
     * Simple test to validate method declaration order does not affect sorted order.
     */
    @Nested
    @TestMethodOrder(DependsOnOrdering.class)
    class TripleVariantC
    {
        @Test
        void first()
        {
            Assertions.assertThat(counterC++).isEqualTo(0);
        }

        @Test
        @DependsOn("first")
        void second()
        {
            Assertions.assertThat(counterC++).isEqualTo(1);
        }

        @Test
        @DependsOn("second")
        void last()
        {
            Assertions.assertThat(counterC++).isEqualTo(2);
        }
    }

    /**
     * Simple test to validate multiple dependencies for one test are ran in expected order.
     */
    @Nested
    @TestMethodOrder(DependsOnOrdering.class)
    class TripleVariantD
    {
        @Test
        void first()
        {
            Assertions.assertThat(counterD++).isEqualTo(0);
        }

        @Test
        void second()
        {
            Assertions.assertThat(counterD++).isEqualTo(1);
        }

        @Test
        @DependsOn({"first", "second"})
        void last()
        {
            Assertions.assertThat(counterD++).isEqualTo(2);
        }
    }

    /**
     * Expanded test to validate method declaration order does not affect sorted order.
     */
    @Nested
    @TestMethodOrder(DependsOnOrdering.class)
    class FiveA
    {
        @Test
        @DependsOn("test4")
        void test0()
        {
            Assertions.assertThat(counterFiveA++).isEqualTo(4);
        }

        @Test
        void test1()
        {
            Assertions.assertThat(counterFiveA++).isEqualTo(0);
        }

        @Test
        @DependsOn("test3")
        void test2()
        {
            Assertions.assertThat(counterFiveA++).isEqualTo(2);
        }

        @Test
        @DependsOn("test1")
        void test3()
        {
            Assertions.assertThat(counterFiveA++).isEqualTo(1);
        }

        @Test
        @DependsOn("test2")
        void test4()
        {
            Assertions.assertThat(counterFiveA++).isEqualTo(3);
        }
    }

    /**
     * Expanded test of {@link TripleVariantD}.
     * <ul>
     *     <li>0 --> [2, 1]</li>
     *     <li>2 --> [4, 3]</li>
     * </ul>
     * Thus, the dependency tree looks like:
     * <ul>
     *     <li>0
     *     <ul>
     *          <li>2
     *          <ul>
     *              <li>4</li>
     *              <li>3</li>
     *          </ul>
     *          </li>
     *          <li>1</li>
     *     </ul>
     *     </li>
     * </ul>
     * Going up the tree, the order should be: 1 4 3 2 0
     */
    @Nested
    @TestMethodOrder(DependsOnOrdering.class)
    class FiveB
    {
        @Test
        @DependsOn({"test2", "test1"})
        void test0()
        {
            Assertions.assertThat(counterFiveB++).isEqualTo(4);
        }

        @Test
        void test1()
        {
            Assertions.assertThat(counterFiveB++).isEqualTo(0);
        }

        @Test
        @DependsOn({"test4", "test3"})
        void test2()
        {
            Assertions.assertThat(counterFiveB++).isEqualTo(3);
        }

        @Test
        void test3()
        {
            Assertions.assertThat(counterFiveB++).isEqualTo(1);
        }

        @Test
        void test4()
        {
            Assertions.assertThat(counterFiveB++).isEqualTo(2);
        }
    }
}
