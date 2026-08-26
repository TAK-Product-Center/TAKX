package gov.tak.platform.contact;

import com.atakmap.coremap.log.Log;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.tak.api.annotation.NonNull;
import gov.tak.api.annotation.Nullable;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IPersistableGroupContact;
import gov.tak.api.util.AttributeSet;
import gov.takx.dataservices.utils.AttributeSetConverter;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.hibernate.Length;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Group contact persistent entity implementation.
 * <p>
 * Due to the lack of hibernate annotations on our super-classes, we have to use getter/setter discovery
 * for persistence here.
 *
 * @since 3.2
 */
@Entity
public class GroupContact extends GroupContactBase implements IPersistableGroupContact
{
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Set<IContact> NO_MEMBERS = Collections.emptySet();

    static
    {
        final SimpleModule module = new SimpleModule();
        module.addSerializer(IContact.class, new ContactSerializer());
        module.addDeserializer(IContact.class, new ContactDeserializer());
        mapper.registerModule(module);
    }

    private Integer id;
    private String parentUid;
    private volatile String membersAsJson = "";

    /**
     * For Hibernate.
     */
    protected GroupContact()
    {
        this(UUID.randomUUID().toString());
    }

    protected GroupContact(String uniqueId)
    {
        this(uniqueId, uniqueId, new AttributeSet(), NO_MEMBERS, null);
    }

    /**
     * @param uniqueId      The unique ID of this group contact
     * @param displayName   The display name of this group contact
     * @param attributes    The attributes associated with this contact
     * @param groupMembers  The group members associated with this group contact
     * @param parentContact The parent group if this is a nested group (or else {@code null})
     */
    GroupContact(@NonNull String uniqueId, @NonNull String displayName,
                 @NonNull AttributeSet attributes, @NonNull Set<IContact> groupMembers,
                 @Nullable IGroupContact parentContact)
    {
        super(uniqueId, displayName, attributes, groupMembers, parentContact);

        parentUid = parentContact == null ? null : parentContact.getUid();

        updateMembersJson();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Override
    public Integer getId()
    {
        return id;
    }

    private void setId(Integer id)
    {
        this.id = id;
    }

    @Override
    @NonNull
    @Column(name = "uid", nullable = false, unique = true)
    public String getUid()
    {
        return uniqueId;
    }

    @SuppressWarnings("unused") // Called from hibernate
    private void setUid(String uid)
    {
        this.uniqueId = uid;
    }

    @Override
    @Column(name = "name", nullable = false, length = Length.LONG32)
    public String getName()
    {
        return displayName;
    }

    @Override
    public void setName(@NonNull String name)
    {
        displayName = name;
    }

    @Column(length = Length.LONG32)
    @SuppressWarnings("unused") // Called from hibernate
    private String getMembersAsJson()
    {
        return membersAsJson;
    }

    @SuppressWarnings("unused") // Called from hibernate
    private void setMembersAsJson(String membersAsJson)
    {
        this.membersAsJson = membersAsJson;
    }

    @Override
    public void setGroupMembers(Set<IContact> groupMembers)
    {
        super.setGroupMembers(groupMembers);
        updateMembersJson();
    }

    @Override
    @Convert(converter = AttributeSetConverter.class)
    @Column(length = Length.LONG32)
    public AttributeSet getAttributes()
    {
        return attributes;
    }

    @SuppressWarnings("unused") // Called from hibernate
    private void setAttributes(@NonNull AttributeSet attributes)
    {
        this.attributes = attributes;
    }

    /**
     * Update the JSON representation of our group members before a persist/update.
     */
    @PrePersist
    @PreUpdate
    private void updateMembersJson()
    {
        try
        {
            membersAsJson = mapper.writeValueAsString(
                    new MembersDTO(
                            groupMembers.stream()
                                    // Only persist individual group members
                                    .filter(contact -> !(contact instanceof IGroupContact))
                                    .collect(Collectors.toSet())));
        } catch (JsonProcessingException e)
        {
            Log.e("GroupContact", "Failed to create JSON form of '" + groupMembers + "'", e);
        }
    }

    /**
     * Reify our group members from the JSON representation after being loaded from the DB.
     */
    @PostLoad
    private void postLoad()
    {
        try
        {
            final MembersDTO membersDTO = mapper.readValue(membersAsJson, MembersDTO.class);
            groupMembers.clear();
            groupMembers.addAll(membersDTO.groupMembers);
        } catch (JsonProcessingException e)
        {
            Log.e("GroupContact", "Failed to reify members from '" + membersAsJson + "'", e);
        }
    }

    @Override
    public void setParentContact(IGroupContact parentContact)
    {
        super.setParentContact(parentContact);
        parentUid = parentContact == null ? null : parentContact.getUid();
    }

    @Override
    @Nullable
    public String getParentUid()
    {
        return parentUid;
    }

    @Nonnull
    @Override
    public String toString()
    {
        return "GroupContact{" +
                "name=" + getName() +
                ", UID=" + getUid() +
                ", parent UID=" + getParentUid() +
                ", memberCount=" + groupMembers.size() +
                "]}";
    }

    @SuppressWarnings("unused") // Called from hibernate
    private void setParentUid(String parentUid)
    {
        this.parentUid = parentUid;
    }

    /**
     * DTO class so that we can persist the collection of member contacts.  This is only needed
     * because, currently, individual contacts are NOT persisted.
     */
    private static class MembersDTO
    {
        @JsonProperty
        private Set<IContact> groupMembers;

        @SuppressWarnings("unused") // Called Fom Jackson
        MembersDTO()
        {
        }

        private MembersDTO(Set<IContact> groupMembers)
        {
            this.groupMembers = groupMembers;
        }
    }

    /**
     * Serializer to handle {@link IContact} instances.
     */
    private static final class ContactSerializer extends StdSerializer<IContact>
    {
        ContactSerializer()
        {
            super((Class<IContact>) null);
        }

        @Override
        public void serialize(IContact value, JsonGenerator gen, SerializerProvider provider) throws IOException
        {
            gen.writeStartObject();
            gen.writeStringField("uid", value.getUid());
            gen.writeStringField("name", value.getName());
            gen.writeStringField("attributeSetJson", value.getAttributes().toJson());
            gen.writeEndObject();
        }
    }

    /**
     * Deserializer to handle {@link IContact}, producing {@link IndividualContact} instances.
     */
    private static final class ContactDeserializer extends StdDeserializer<IContact>
    {
        ContactDeserializer()
        {
            super((Class<?>) null);
        }

        @Override
        public IContact deserialize(JsonParser p, DeserializationContext context) throws IOException
        {
            final JsonNode node = p.getCodec().readTree(p);

            return new IndividualContactBuilder()
                    .withUniqueContactId(node.get("uid").asText())
                    .withDisplayName(node.get("name").asText())
                    .withAttributes(new AttributeSet(node.get("attributeSetJson").asText()))
                    .build();
        }
    }
}
