package gov.takx.core.structures;

import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.structures.IIntersectable;
import gov.takx.api.structures.ISurfaceStructure;
import gov.takx.api.structures.ISurfaceStructureService;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.List;

/**
 * Extended surface structure service offering additional operations to the core.
 *
 * @since 3.0
 */
public interface ICoreSurfaceStructureService extends ISurfaceStructureService
{
    /**
     * Notify listeners of a metadata change on the given intersectable object.
     *
     * @param structure   Structure of interest
     * @param metaDataKey key of metadata value that changed
     * @param oldValue    Old metadata value
     * @param newValue    New metadata value
     */
    void notifyListenersOfMetaDataChange(@Nonnull IIntersectable structure, @Nonnull String metaDataKey, String oldValue, String newValue);

    /**
     * Notify listeners of a metadata change on the given shape map object.
     *
     * @param shape       Shape map object of interest
     * @param metaDataKey key of metadata value that changed
     * @param oldValue    Old metadata value
     * @param newValue    New metadata value
     */
    void notifyListenersOfMetaDataChange(@Nonnull IShapeMapObject<?> shape, @Nonnull String metaDataKey, String oldValue, String newValue);

    /**
     * Notify listeners of a property change on the given globe shape map object.
     *
     * @param globeShapeMapObject Shape map object of interest
     * @param evt Source PropertyChangeEvent
     */
    void notifyListenerOfShapePropertyChange(@Nonnull IGlobeShapeMapObject globeShapeMapObject, @Nonnull PropertyChangeEvent evt);

    /**
     * Notification of import completion from StructureFileImporter .  Essentially, this method just forwards to
     * fireEvent().  The difference is that it checks the imported structure count before firing the event.  If no
     * structures were imported, the associated layer is removed and a warning is logged/displayed.  The user is then
     * given the option to import the file instead a simple overlay.
     *
     * @param sourceFile                file from which structures were imported.
     * @param structureLayer            WorldWind layer.
     * @param importedSurfaceStructures list of imported surface structures.
     * @since 1.43 (interface is new in 3.0)
     */
    void importComplete(@Nonnull File sourceFile, @Nonnull IStructureLayer structureLayer, @Nonnull List<ISurfaceStructure> importedSurfaceStructures);
}
