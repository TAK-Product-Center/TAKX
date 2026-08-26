package gov.takx.bootstrap;

/**
 * Enum to represent what level of verification the bootstrap and plugin jars have.
 *
 * @since 5.5
 */
public enum Verification
{
    /**
     * Jar has been signed by our dev cert.
     */
    Developer,
    /**
     * Jar has been signed by the "Trusted" cert.
     */
    Trusted,
    /**
     * Jar has been signed by the "Untrusted" cert.
     */
    Untrusted,
    /**
     * Jar has not been signed by any of the above certs. We do not distinguish between unsigned and signed by an unknown cert.
     */
    None
}