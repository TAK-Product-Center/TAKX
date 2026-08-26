package gov.takx.core.structures;

import gov.takx.api.structures.ISurfaceStructure;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for structure importers.
 *
 */
public interface IStructureImporter
{
    /**
     * Begin the import process.  The returned future can be used to obtain the result (structure count) of the import.
     *
     * @return Future for obtaining the import count
     * @since 3.0
     */
    CompletableFuture<Integer> beginImport();

    /**
     * @return Task name
     * @since 2.32
     */
    String getName();

    /**
     * Add a structure to the collection of imported structures.
     *
     * @param structure imported structure
     * @since 3.0
     */
    void addImportedStructure(ISurfaceStructure structure);
}
