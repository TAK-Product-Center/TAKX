package gov.takx.api.messages;

import gov.takx.commons.constants.CommServiceType;

/**
 * @since 1.3
 */
public interface IPrePersistRaptorCommandMessage extends IRaptorCommandMessage, IPrePersistRaptorMessage
{
    void setSourceName(String sourceName);

    void setSourceType(CommServiceType sourceType);
}
