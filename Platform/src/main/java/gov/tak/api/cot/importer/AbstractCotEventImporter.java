package gov.tak.api.cot.importer;

import gov.tak.api.cot.detail.ICotDetailManager;
import gov.takx.api.annotation.PseudoBean;
import jakarta.inject.Inject;

/**
 * Abstract base for CoT Event importers.
 *
 * @since 3.0
 */
@PseudoBean
public abstract class AbstractCotEventImporter implements CotEventImporter
{
    @Inject
    protected ICotDetailManager cotDetailManager;
}
