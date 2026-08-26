package gov.takx.core.chat;

import gov.tak.api.chat.IChatMessage;
import gov.takx.api.persist.IPersistable;

/**
 * Contract for a persistable {@link IChatMessage}.
 *
 * @since 3.4
 * @see gov.takx.api.persist.IChatMessagePersist
 */
public interface IPersistableChatMessage extends IChatMessage, IPersistable
{
}