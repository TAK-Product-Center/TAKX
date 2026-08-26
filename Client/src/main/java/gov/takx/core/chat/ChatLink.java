package gov.takx.core.chat;

import jakarta.annotation.Nonnull;

/**
 * Holds info about a single chat data package entry. File entries are expected to have a name but no UID. Map items will
 * have both a name and UID.
 *
 * @param timestamp The data package received timestamp
 * @param uid The data package entry's UID if the entry is a map item, or an empty String if the entry is a file
 * @param name The data package entry's name
 * @since 5.2 (Added timestamp in 5.7)
 */
public record ChatLink(long timestamp, @Nonnull String uid, @Nonnull String name)
{
}
