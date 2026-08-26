package gov.takx.dataservices.persist;

import gov.takx.api.eventlog.EventLogType;
import gov.takx.api.eventlog.IRaptorEventLog;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.constants.State;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Date;

/**
 */
@Entity
@Immutable
@Table(indexes = @Index(name = "idx_el_mo_id", columnList = "mapObjectId"))
public class RaptorEventLog implements IRaptorEventLog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String mapObjectFamily;
    private String mapObjectType;
    private String mapObjectUnitId;
    private String message;
    private String details;
    private Date logDate;
    private int type;
    private String target;
    private String name;
    private State state;
    private String annotation;

    private Integer mapObjectId;

    @SuppressWarnings("unused") // For hibernate
    protected RaptorEventLog()
    {
    }

    public RaptorEventLog(@Nonnull Date date, IMapObject mapObject, @Nonnull String message, String details, @Nonnull EventLogType type)
    {
        this(date, mapObject, message, details, type, null, null);
    }

    public RaptorEventLog(@Nonnull Date date, IMapObject mapObject, @Nonnull String message, String details, @Nonnull EventLogType type, String target)
    {
        this(date, mapObject, message, details, type, target, null);
    }

    public RaptorEventLog(@Nonnull Date date, IMapObject mapObject, @Nonnull String message, String details, @Nonnull EventLogType type, String target,
                          State state)
    {
        this(date, mapObject.getFamily(), mapObject.getType(), mapObject.getUnitID(), message, details, type, target,
                mapObject.getName());

        this.state = state;
        mapObjectId = mapObject.getId();
    }

    public RaptorEventLog(@Nonnull Date logDate, String mapObjectFamily, String mapObjectType, String mapObjectUnitId,
                          @Nonnull String message, String details, @Nonnull EventLogType type, String target, String name)
    {
        if (message.length() > 255)
        {
            logger.warn("Event log message is being cropped to 255. Original message was {}", message);
            message = message.substring(0, 255);
        }

        if (null != details && details.length() > 255)
        {
            logger.warn("Event log details is being cropped to 255.  Original message was {}", details);
            details = details.substring(0, 255);
        }

        this.mapObjectFamily = mapObjectFamily;
        this.mapObjectType = mapObjectType;
        this.mapObjectUnitId = mapObjectUnitId;
        this.logDate = (Date) logDate.clone();
        this.message = message;
        this.details = details != null ? details : "";
        this.type = type.ordinal();
        this.target = target;
        this.name = name;
        annotation = "";
    }

    @Override
    public String getSource()
    {
        return mapObjectFamily + "/" + mapObjectType + "/" + mapObjectUnitId;
    }

    @Override
    public String getMapObjectFamily()
    {
        return mapObjectFamily;
    }

    @Override
    public String getMapObjectType()
    {
        return mapObjectType;
    }

    @Override
    public String getMapObjectUnitId()
    {
        return mapObjectUnitId;
    }

    @Override
    public int getType()
    {
        return type;
    }

    @Override
    public String getMessage()
    {
        return message;
    }

    @Override
    public String getDetails()
    {
        return details;
    }

    @Override
    public Date getDate()
    {
        return (Date) logDate.clone();
    }

    @Override
    public Integer getId()
    {
        return id;
    }

    @Override
    public String getTarget()
    {
        return target;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public State getState()
    {
        return state;
    }

    @Override
    public Integer getMapObjectId()
    {
        return mapObjectId;
    }

    @Override
    public String getAnnotation()
    {
        return annotation;
    }

    @Override
    public void setAnnotation(String annotation)
    {
        this.annotation = annotation;
    }

    @Override
    public String toString()
    {
        return "RaptorEventLog{" +
                EventLogType.values()[type].name() + ": " +
                "key='" + KeyUtils.getUniqueKey(mapObjectFamily, mapObjectType, mapObjectUnitId) +
                ", moId=" + mapObjectId +
                ", message='" + message + '\'' +
                ", details='" + details + '\'' +
                ", logDate=" + logDate +
                ", target='" + target + '\'' +
                ", name='" + name + '\'' +
                ", state=" + state +
                ", annotation=" + annotation +
                '}';
    }
}
