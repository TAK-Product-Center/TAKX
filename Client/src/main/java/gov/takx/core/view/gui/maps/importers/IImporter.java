package gov.takx.core.view.gui.maps.importers;

/**
 */
public interface IImporter
{
    /**
     * Begin the import process for this importer.  Typically, this method will spawn a new thread to do the work.
     */
    public void beginImport();
}
