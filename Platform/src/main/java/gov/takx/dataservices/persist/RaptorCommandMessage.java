package gov.takx.dataservices.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IPrePersistRaptorCommandMessage;
import gov.takx.api.messages.IStructure;
import gov.takx.commons.constants.CommServiceType;

/**
 */
@Internal
public class RaptorCommandMessage extends ARaptorMessage implements IPrePersistRaptorCommandMessage
{
    private CommServiceType sourceType;
    private String sourceName;

    public RaptorCommandMessage(IStructure structure, IMapObject mapObject)
    {
        super(structure, mapObject);
    }

    public RaptorCommandMessage(IStructure structure, String family, String type, String unitId)
    {
        super(structure, family, type, unitId);
    }

    @Override
    public void setSourceName(String sourceName)
    {
        this.sourceName = sourceName;
    }

    @Override
    public String getSourceName()
    {
        return sourceName;
    }

    @Override
    public void setSourceType(CommServiceType sourceType)
    {
        this.sourceType = sourceType;
    }

    @Override
    public CommServiceType getSourceType()
    {
        return sourceType;
    }

    @Override
    protected String coreFieldsToString()
    {
        return "sourceName=" + sourceName + ", sourceType=" + sourceType;
    }
}
