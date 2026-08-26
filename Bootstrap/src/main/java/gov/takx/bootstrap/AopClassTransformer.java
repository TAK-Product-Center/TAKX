package gov.takx.bootstrap;

import org.aspectj.weaver.loadtime.Aj;
import org.aspectj.weaver.loadtime.ClassPreProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandles;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

/**
 * This class provides the class file transformer for runtime weaving of code loaded by our module system.
 * See the aspects defined in Platform in gov/takx/api/aspects for details on what we are weaving in to the code.
 * <p>
 * During tests, we don't have fine-grained control over which classes are transformed (since we don't spin up
 * JBoss Modules, which applies class transformers only on the code we want).  At test time, this class is specified
 * as a java agent, and thus it applies to every class loaded.  So, we need a way to limit the scope of the classes
 * that get transformed.
 * <p>
 * Any class that exists within the {@code gov.takx} are always woven.  In order to support arbitrary package names
 * in plugin tests, any additional package prefixes can be specified using the {@code weaveTestPackages} system
 * property.  Note that this must be specified on the command line to ensure that it is available prior to any
 * classes being transformed.  The value of the property is a list of package prefixes separated by semi-colons.
 * Similar to log4j, the packages specified include any sub-package.  So, e.g., specifying {@code com.foo} would
 * include {@code com.foo.this} and {@code com.foo.that}, etc.
 * <p>
 * For example, to have classes in the {@code com.foo} and {@code com.bar} packages woven at test time, specify
 * {@code -DweaveTestPackages=com.foo;com.bar} on the java invocation when running the tests.  This will often
 * require configuration within the build script, since that is where the JVM command line arguments are configured.
 *
 */
public final class AopClassTransformer implements ClassFileTransformer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String WEAVE_TEST_PACKAGES = "weaveTestPackages";
    private static final String PACKAGE_PREFIX_SEPARATOR = ";";
    private static final String AOP_RESOURCE_DIR_PROP = "org.aspectj.weaver.loadtime.configuration";
    private static final String AOP_RESOURCE_DIR = "aop/aop.xml";
    private static final String JACOCO_DATA_FIELD_NAME = "$jacocoData"; // NOPMD

    // Comment out the previous line and uncomment this one to install custom weaver options
//    private static final String AOP_RESOURCE_DIR = "aop/aop.xml;aop/debug_weaver_options.xml";

    private static boolean testMode; // Are we running the tests?
    private static final ClassPreProcessor AOP_PRE_PROCESSOR;

    // The set of package prefixes that will be woven when running tests
    private static final Set<String> TEST_PACKAGES_TO_WEAVE = new HashSet<>();

    private static final ClassFileTransformer INSTANCE = new AopClassTransformer();

    /**
     * The instrumentation instance
     */
    private static Instrumentation instrumentation;

    static
    {
        // Uncomment these next two lines to enable detail tracing in AspectJ weaving
        System.setProperty("org.aspectj.tracing.factory", "default");
//        System.setProperty("org.aspectj.tracing.enabled", "true");

//        System.setProperty("aj.weaving.verbose", "true");
//        System.setProperty("org.aspectj.weaver.showWeaveInfo", "true");

        // Make Aspectj look where we want (avoid META-INF since resources are filtered out from there by default)
        System.setProperty(AOP_RESOURCE_DIR_PROP, AOP_RESOURCE_DIR);

        // Setup and initialize the load time weaver
        AOP_PRE_PROCESSOR = new Aj();
        AOP_PRE_PROCESSOR.initialize();
    }

    private AopClassTransformer()
    {
        /* no other instances */
    }

    /**
     * JSR-163 preMain Agent entry method, supports use via -javaagent.  Only used for tests.
     *
     * @param options         Ignored
     * @param instrumentation Instrumentation object
     */
    public static void premain(String options, Instrumentation instrumentation)
    {
        agentmain(options, instrumentation);
    }

    /**
     * JSR-163 agent main entry method.  This is only used when running the tests.
     *
     * @param options         Ignored
     * @param instrumentation Instrumentation object
     */
    @SuppressWarnings({"UnusedParameters", "unused"})
    public static void agentmain(String options, Instrumentation instrumentation)
    {
        /* Handle duplicate agents */
        if (AopClassTransformer.instrumentation != null)
        {
            return;
        }

        testMode = true;

        AopClassTransformer.instrumentation = instrumentation;
        AopClassTransformer.instrumentation.addTransformer(INSTANCE);

        initTestPackagesToWeave();
    }

    /**
     * @return The singleton instance of this class transformer
     */
    public static ClassFileTransformer getInstance()
    {
        return INSTANCE;
    }

    /**
     * Determine if the given class name is in a package that will be woven during tests.  The className is
     * expected to be separated by '.' (as you would get from {@link Class#getName()}).
     *
     * @param className Name of class to test
     * @return {@code true} if the class is in a package that will be woven at test run time
     * @since 2.36
     */
    public static boolean isWovenDuringTests(String className)
    {
        // Need to convert the class name to "internal" form, replace '.' with '/'
        return shouldWeaveInTestMode(className.replace('.', '/'));
    }

    /**
     * Apply our aspects.
     */
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] bytes)
    {
        // Javadoc says we should return null if we don't transform the bytes, but, apparently, there's a bug in
        // jboss modules, and returning a null causes an NPE down the line.  So, return the bytes we were given
        // if we don't weave them.
        if (shouldNotWeave(className)) return bytes;

        logger.debug("Load: {} for {}", className, loader);
        return AOP_PRE_PROCESSOR.preProcess(className, bytes, loader, protectionDomain);
    }

    /**
     * Determine if we should NOT weave a given class. We want to avoid weaving the class if:
     * <p>
     * <ol>
     * <li>The className is {@code null} - I don't know why, but we get {@code null} class names sometimes</li>
     * <li>We are in test mode and the class' package is not configured to be woven during testing</li>
     * <li>It is a dynamic class (like those created by Hibernate) which have "javassist" in their class name
     * (we don't want to alter those generated classes)</li>
     * <li>The class starts with 'javax' (some plugins bundle javax classes within their jar) and we don't want to alter JDK classes</li>
     * <li>It is one of our aspects, as this can cause problems in the current, 1.9.4, version of AspectJ
     * when dealing with lambdas on JDK versions higher than 8.</li>
     * </ol>
     *
     * @param className Name of class to test
     * @return {@code true} if the class should NOT be woven, {@code false} if it should
     * @since 2.35
     */
    private boolean shouldNotWeave(String className)
    {
        if (className == null) return true;

        final boolean testModeAndNotConfigured = testMode && !shouldWeaveInTestMode(className);
        final boolean javassistOrJavax = className.contains("javassist") || className.startsWith("javax");
        final boolean oneOfOurAspects = className.startsWith("gov/takx") && className.contains("/aspects/");
        final boolean mockitoMock = className.contains("$MockitoMock$");

        logger.trace("testModeAndNotConfigured={}, javassistOrJavax={}, oneOfOurAspects={}, mockitoMock={}",
                testModeAndNotConfigured, javassistOrJavax, oneOfOurAspects, mockitoMock);

        return testModeAndNotConfigured || javassistOrJavax || oneOfOurAspects || mockitoMock;
    }

    /**
     * Determine if the given class is in a package that should be woven.  If the class name starts with any of the
     * configured package prefixes, then this method will return {@code true}.
     *
     * @param className Class name to test
     * @return {@code true} if the class should be woven
     * @since 2.25
     */
    private static boolean shouldWeaveInTestMode(String className)
    {
        return TEST_PACKAGES_TO_WEAVE.stream().anyMatch(className::startsWith);
    }

    /**
     * Configure the set of package prefixes that should be woven while running tests.  This will always include
     * {@code gov/takx}, {@code gov/tak}, {@code gov/raptor}, plus any packages specified via
     * {@code -DweaveTestPackages}.  Note that the class names seen by the transformer are "internal" names, so they are
     * separated by '/' and not '.', so we convert all '.' to '/' when processing the {@code weaveTestPackages} value.
     *
     * @since 2.25
     */
    private static void initTestPackagesToWeave()
    {
        TEST_PACKAGES_TO_WEAVE.add("gov/takx"); // Always weave our own code
        TEST_PACKAGES_TO_WEAVE.add("gov/tak");
        TEST_PACKAGES_TO_WEAVE.add("gov/raptor");

        final String weaveTestPackages = System.getProperty(WEAVE_TEST_PACKAGES);
        if (weaveTestPackages != null)
        {
            for (String pfx : weaveTestPackages.split(PACKAGE_PREFIX_SEPARATOR))
            {
                pfx = pfx.trim();

                if (!pfx.isEmpty()) TEST_PACKAGES_TO_WEAVE.add(pfx.replace('.', '/'));
            }
        }

        logger.debug("Test packages: {}", TEST_PACKAGES_TO_WEAVE);
    }
}
