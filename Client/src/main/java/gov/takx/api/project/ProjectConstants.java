package gov.takx.api.project;

import java.util.function.Predicate;

/**
 * Constants related to projects.
 *
 * @since 2.37
 */
public class ProjectConstants
{
    /**
     * File extension indicating a "packaged" project.
     */
    public static final String PACKAGE_EXTENSION = "pkgx";

    /**
     * File extension indicating a standard project definition file.
     */
    public static final String PROJECT_EXTENSION = "projx";

    /**
     * Characters, in addition to alphanumerics, that are legal in a project name.
     *
     * @since 3.0
     */
    public static final String LEGAL_PROJECT_NAME_CHARS = "_-.,+= ()";

    /**
     * Predicate to validate characters as legal in a project name.  Allows letters, digits, spaces, and chars in
     * {@link #LEGAL_PROJECT_NAME_CHARS}.
     *
     * @since 3.0
     */
    public static final Predicate<Character> PROJECT_NAME_CHAR_VALIDATOR =
            c -> Character.isLetterOrDigit(c) || Character.isSpaceChar(c) || LEGAL_PROJECT_NAME_CHARS.indexOf(c) >= 0;

    private ProjectConstants()
    {
    }
}
