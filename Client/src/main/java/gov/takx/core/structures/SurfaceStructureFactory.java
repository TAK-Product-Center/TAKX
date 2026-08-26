package gov.takx.core.structures;

import gov.takx.api.structures.IPersistableSurfaceStructure;
import gov.takx.api.structures.ISurfaceStructure;
import gov.takx.dataservices.persist.PersistableExtrudedLine;
import gov.takx.dataservices.persist.PersistableExtrudedPolygon;
import gov.takx.dataservices.persist.PersistableLine;
import gov.takx.dataservices.persist.PersistablePolygon;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Factory to create instances of {@link ISurfaceStructure} from a persistent entity
 * (the persisted state of the surface structure).
 *
 * @since 3.0
 */
public class SurfaceStructureFactory
{
    private static final Map<Class<? extends IPersistableSurfaceStructure>,
            Function<IPersistableSurfaceStructure, ISurfaceStructure>> SURFACE_STRUCTURE_CONSTRUCTORS = new HashMap<>();

    static
    {
        SURFACE_STRUCTURE_CONSTRUCTORS.put(PersistableExtrudedLine.class, SSExtrudedLine::createFromPersistable);
        SURFACE_STRUCTURE_CONSTRUCTORS.put(PersistableExtrudedPolygon.class, SSExtrudedPolygon::createFromPersistable);
        SURFACE_STRUCTURE_CONSTRUCTORS.put(PersistableLine.class, SSLine::createFromPersistable);
        SURFACE_STRUCTURE_CONSTRUCTORS.put(PersistablePolygon.class, SSPolygon::createFromPersistable);
    }

    /**
     * Construct a surface structure from the given persistable.
     *
     * @param persistable Persistable entity holding the state from which to build
     * @return structure instance
     * @throws IllegalArgumentException if the given persistable type is not one of out known types
     */
    public static ISurfaceStructure createFromPersistable(IPersistableSurfaceStructure persistable)
    {
        final Class<? extends IPersistableSurfaceStructure> structureClass = persistable.getClass();

        if (!SURFACE_STRUCTURE_CONSTRUCTORS.containsKey(structureClass))
        {
            throw new IllegalArgumentException("Unsupported structure type: " + structureClass.getName());
        }

        return SURFACE_STRUCTURE_CONSTRUCTORS.get(structureClass).apply(persistable);
    }
}
