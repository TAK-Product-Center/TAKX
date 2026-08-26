package gov.takx.api.event;

import jakarta.annotation.Nullable;

/**
 * Event to be fired when a map object editor fails, such as when invalid parameters or insufficient points are provided.
 *
 * @since 3.3
 */
public record EditorFailedEvent(@Nullable String mapObjectKey)
{
}
