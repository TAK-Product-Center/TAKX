package gov.takx.api.globe;

/**
 */
public interface IHighlightable
{
    /**
     * Set the highlighted state according to {@code highlighted} parameter
     *
     * @param highlighted New highlighted state
     */
    void setHighlighted(boolean highlighted);

    /**
     * @return True if the object is currently highlighted, false otherwise
     */
    boolean isHighlighted();
}
