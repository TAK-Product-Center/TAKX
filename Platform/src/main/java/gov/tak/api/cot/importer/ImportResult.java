package gov.tak.api.cot.importer;

import gov.tak.api.cot.detail.ICotDetailHandler;

/**
 * Status of a CoT import.
 *
 * @since 3.0
 */
@SuppressWarnings("deprecation")
public enum ImportResult
{
    /**
     * no problems, but also not handled.
     */
    IGNORE(),

    /**
     * no problems, imported.
     */
    SUCCESS(),

    /**
     * waiting on another item, so try again at the end.
     */
    DEFERRED(),

    /**
     * problems, not importable.
     */
    FAILURE();

    /**
     * Compare this import result with another and return the result with
     * higher priority. This is useful when performing multiple import
     * operations in a method where a single result is returned.
     * IGNORE &lt; SUCCESS &lt; DEFERRED &lt; FAILURE
     *
     * @param other Other result
     * @return The result with higher priority
     */
    public ImportResult getHigherPriority(ImportResult other)
    {
        return ordinal() >= other.ordinal() ? this : other;
    }

    /**
     * @param kernelImportResult The TAK Kernel {@link ICotDetailHandler.ImportResult} to convert to the TAKX equivalent
     * @return The TAKX equivalent of the provided Kernel ImportResult
     * @since 5.3
     */
    public static ImportResult convertKernelResult(ICotDetailHandler.ImportResult kernelImportResult)
    {
        return switch (kernelImportResult)
        {
            case DEFERRED -> DEFERRED;
            case IGNORE -> IGNORE;
            case FAILURE -> FAILURE;
            case SUCCESS -> SUCCESS;
        };
    }

    /**
     * @param importResult The TAKX {@link ImportResult} to convert to the TAK Kernel equivalent
     * @return The TAK Kernel ImportResult equivalent of the provided TAKX ImportResult
     * @since 5.5.1
     */
    public static ICotDetailHandler.ImportResult toKernelResult(ImportResult importResult)
    {
        return switch (importResult)
        {
            case DEFERRED -> ICotDetailHandler.ImportResult.DEFERRED;
            case IGNORE -> ICotDetailHandler.ImportResult.IGNORE;
            case FAILURE -> ICotDetailHandler.ImportResult.FAILURE;
            case SUCCESS -> ICotDetailHandler.ImportResult.SUCCESS;
        };
    }
}
