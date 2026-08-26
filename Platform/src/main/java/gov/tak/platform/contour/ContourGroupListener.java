package gov.tak.platform.contour;

import jakarta.annotation.Nonnull;

/**
 * Listener that is notified of additions and removals from {@link IContourLinesService#getContourGroups()}.
 *
 * @since 5.8
 */
public interface ContourGroupListener
{
    /**
     * @param group Contour group created.
     */
    void onContourGroupCreated(@Nonnull ContourLineGroup group);

    /**
     * @param group Contour group removed.
     */
    void onContourGroupRemoved(@Nonnull ContourLineGroup group);
}
