package gov.takx.api.messages;

import gov.takx.commons.constants.CommServiceType;

/**
 * @since 1.3
 */
public interface IRaptorCommandMessage extends IRaptorMessage
{
    String getSourceName();

    CommServiceType getSourceType();
}
