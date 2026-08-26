package gov.takx.module;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Provides the set of packages that are forbidden to exist within a plugin deployment. The inclusion of any of these
 * packages in a deployment is indicative of a broken build where the developer ended up including classes from the TAKX
 * libs in their own jar file.
 *
 * @since 2.0.0
 */
public class ForbiddenPackages
{
    private static final List<String> FORBIDDEN_PACKAGES =
            ImmutableList.<String>builder()
                    .add("gov/takx/api")
                    .add("gov/takx/platform")
                    .add("gov/takx/commons")
                    .add("gov/takx/dataservices")
                    .add("gov/takx/core")
                    .add("gov/takx/client")
                    .add("gov/takx/bootstrap")
                    .add("gov/takx/module")
                    .add("gov/takx/cdi")
                    .add("gov/nasa") // And the WW classes too
                    .build();

    /**
     * @return The list of forbidden packages
     */
    public static List<String> getForbiddenPackages()
    {
        return FORBIDDEN_PACKAGES;
    }
}
