package gov.takx.dataservices.persist;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.mapobject.MapEntityStructuresUpdated;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.IStructure;
import gov.takx.platform.KeyUtils;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 */
@Internal
@PseudoBean
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"family", "type"}))
public class MapEntity implements IMapEntity
{
    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @SuppressWarnings("unused")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String family;
    private String type;

    @Transient
    private int hashCode;
    @Transient
    private String uniqueKey;

    // All our "current" known structures.
    @Transient
    private final Map<String, IStructure> currentStructures = new ConcurrentHashMap<>();

    // collection of all structures with the same name but different versions.
    @Transient
    private final ListMultimap<String, IStructure> versionedStructures = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());

    @Transient
    private final Map<String, IRaptorDataStructure> rdsCache = new ConcurrentHashMap<>();

    @Inject
    @Transient
    private Event<MapEntityStructuresUpdated> structuresRedefinedEvent;

    protected MapEntity()
    {
    }

    /**
     * Construct a MapEntity with the given values.
     *
     * @param family Map Object family
     * @param type   Map Object type
     * @since 2.29
     */
    public MapEntity(String family, String type)
    {
        this.family = family;
        this.type = type;
        uniqueKey = KeyUtils.getUniqueKey(this);
        updateHashCode();
    }

    @Override
    public Integer getId()
    {
        return id;
    }

    @Override
    public String getUniqueKey()
    {
        return uniqueKey != null ? uniqueKey : (uniqueKey = KeyUtils.getUniqueKey(this));
    }

    @Override
    public String getFamily()
    {
        return family;
    }

    @Override
    public String getType()
    {
        return type;
    }

    @Override
    public synchronized boolean addStructures(Collection<IStructure> structs)
    {
        boolean anyAdded = false;
        for (IStructure structure : structs) anyAdded |= addStructure(structure);

        // Fire the event to notify of the new structures
        if (anyAdded) structuresRedefinedEvent.fire(new MapEntityStructuresUpdated(this));

        return anyAdded;
    }

    @Override
    public synchronized boolean addStructure(IStructure struct)
    {
        final Structure s = (Structure) struct;

        boolean aS = internalAddStructure(s);
        boolean aD = s.internalAddMapEntity(this);

        if (logger.isDebugEnabled() && (aD || aS))
        {
            logger.debug("Added {}Structure{}:{} to {}MapEntity{}:{}", aS ? "<<" : "", aS ? ">>" : "", struct, aD ? "<<" : "", aD ? ">>" : "", this);
        }

        return aS;
    }

    /**
     * Internal method to add a structure to our collection if it is not already present.
     * Package access so {@link Structure#addDefinition(IMapEntity)} can also access it.
     *
     * @param structure Structure to add
     * @return {@code true} if the structure was added. {@code false} if already present
     */
    boolean internalAddStructure(IStructure structure)
    {
        logger.debug("add structure: {}, {}", this, structure);

        // If we have this structure, see if they have different hashes and then deal with version differences
        final String name = structure.getMessageType();
        final IStructure currentStructure = currentStructures.get(name);

        if (currentStructure == null)
        {
            // Simple case, new structure
            currentStructures.put(name, structure);
            versionedStructures.put(name, structure);
            return true;
        } else
        {
            // Replace our "current" with this new one
            if (!currentStructure.equals(structure))
            {
                if (logger.isDebugEnabled()) logger.debug("Replace current: {} on {}", structure, getUniqueKey());
                currentStructures.put(name, structure);
            }

            // Add to the versioned list if it is not already present
            final String version = structure.getVersion();
            final String hashValue = structure.getHashValue();
            boolean found = false;

            synchronized (versionedStructures)
            {
                for (IStructure s : versionedStructures.get(name))
                {
                    if (s.getVersion().equals(version))
                    {
                        if (!s.getHashValue().equals(hashValue))
                        {
                            logger.warn("Multiple structures named '{}' and version {} with different hash values!", name, version);
                        } else
                        {
                            found = true;
                            break;
                        }
                    }
                }
            }

            if (!found)
            {
                logger.debug("Add: {}", structure);
                versionedStructures.put(name, structure);
            }

            return !found;
        }
    }

    @Override
    public boolean hasMessageType(String messageType)
    {
        return currentStructures.containsKey(messageType);
    }

    @Override
    public Set<IStructure> getDataStructures()
    {
        // Gather all versions of all structures
        final Set<IStructure> allStructures = new HashSet<>(versionedStructures.size() + currentStructures.size());
        allStructures.addAll(currentStructures.values());
        synchronized (versionedStructures)
        {
            allStructures.addAll(versionedStructures.values());
        }

        return allStructures;
    }

    @Override
    public Set<IRaptorDataStructure> getRaptorDataStructures()
    {
        return currentStructures.values().stream()
                .map(this::makeRDS)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<IRaptorDataStructure> getAllRaptorDataStructures()
    {
        return getDataStructures().stream()
                .map(this::makeRDS)
                .collect(Collectors.toSet());
    }

    @Override
    public IRaptorDataStructure getRaptorDataStructure(String messageType)
    {
        return makeRDS(currentStructures.get(messageType));
    }

    @Override
    public IRaptorDataStructure getRaptorDataStructure(String name, String version)
    {
        return makeRDS(findStructure(name, version));
    }

    private IRaptorDataStructure makeRDS(IStructure struct)
    {
        if (struct == null) return null;

        final String hashValue = struct.getHashValue();

        // This is called a lot, so avoid the use of computeIfAbsent since it allocates a lambda object on every use,
        // and we don't need the atomic guarantee of that method.

        IRaptorDataStructure rds = rdsCache.get(hashValue);

        if (rds == null)
        {
            rds = new RaptorDataStructure(family, type, struct);
            rdsCache.put(hashValue, rds);
        }
        return rds;
    }

    /**
     * Locate the structure with a given name and version number.
     *
     * @param name    Structure name (message type)
     * @param version version number to match
     * @return Matched structure, {@code null} if none found
     */
    private IStructure findStructure(String name, String version)
    {
        synchronized (versionedStructures)
        {
            for (IStructure structure : versionedStructures.get(name))
            {
                if (structure.getVersion().equals(version)) return structure;
            }
        }

        return null;
    }

    public String toString()
    {
        return String.format("%s{id=%s, key=%s:%s}", getClass().getSimpleName(), id, family, type);
    }

    public String fullString(boolean includeCollections)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(":").append(family).append("/").append(type);
        if (includeCollections)
        {
            for (IStructure me : currentStructures.values())
            {
                sb.append("\r    ");
                sb.append(((Structure) me).fullString(false));
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MapEntity mapEntity = (MapEntity) o;

        return family.equals(mapEntity.family) && type.equals(mapEntity.type);
    }

    @Override
    public int hashCode()
    {
        return hashCode != 0 ? hashCode : updateHashCode();
    }

    /**
     * Set the hash code according to our family/type.
     *
     * @return Updated hash code
     * @since 2.29
     */
    private int updateHashCode()
    {
        return hashCode = family.hashCode() * 31 + type.hashCode();
    }
}
