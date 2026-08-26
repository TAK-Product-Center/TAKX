package gov.takx.core.toolbar;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Object to represent a change to a toolbar configuration
 *
 * @since 2.0 (Renamed in 3.4)
 */
public record ToolbarChangeEvent(ToolbarConfiguration configuration, ToolbarChangeEventType type, Object source,
        Object oldValue, Object newValue)
{
    public ToolbarChangeEvent(@Nonnull ToolbarConfiguration configuration, @Nonnull ToolbarChangeEventType type,
                              @Nonnull Object source, @Nullable Object oldValue, @Nullable Object newValue)
    {
        this.configuration = configuration;
        this.type = type;
        this.source = source;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}
