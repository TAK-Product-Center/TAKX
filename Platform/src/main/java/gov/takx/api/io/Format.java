package gov.takx.api.io;

/**
 * Descriptor for an Import/Export format.
 *
 * @since 1.3
 */
public class Format
{
    /**
     * Format identifier for programmatic use (for example, "csv").
     */
    private String identifier;

    /**
     * The title of the format, to display in the GUI (for example "Comma Separated Value (CSV)".
     */
    protected String title;

    /**
     * File extensions commonly assigned to files of this format. Panels that allow the user to
     * select files may use this list to create file filters, but it is not required.
     */
    protected String[] fileTypes;

    /**
     * Any notes related to how this format is supported (limitations etc.) This will be displayed
     * in the GUI when the user selects the format.
     */
    protected String notes;

    /**
     * Create an Import/Export format.
     *
     * @param formatIdentifier Format identifier for programmatic use (for example, "csv").
     * @param title            Friendly identifier to display in GUI (for example "Comma Separated Value (CSV)".
     * @param notes            Any notes related to how this format is supported (limitations etc.) This will be displayed
     *                         in the GUI when the user selects the format.
     */
    public Format(String formatIdentifier, String title, String notes)
    {
        this(formatIdentifier, title, notes, new String[]{});
    }

    /**
     * Create an Import/Export format.
     *
     * @param formatIdentifier Format identifier for programmatic use (for example, "csv").
     * @param title            Friendly identifier to display in GUI (for example "Comma Separated Value (CSV)".
     * @param notes            Any notes related to how this format is supported (limitations etc.) These will be displayed
     *                         in the GUI when the user selects the format.
     * @param fileExtension    File extension assigned to files of this format. Panels that allow the user to
     *                         select files may use value list to create file filters, but it is not required. May return {@code null}.
     */
    public Format(String formatIdentifier, String title, String notes, String fileExtension)
    {
        this(formatIdentifier, title, notes, new String[]{fileExtension});
    }

    /**
     * Create an Import/Export format.
     *
     * @param formatIdentifier Format identifier for programmatic use (for example, "csv").
     * @param title            Friendly identifier to display is GUI (for example "Comma Separated Value (CSV)".
     * @param notes            Any notes related to how this format is supported (limitations etc.) This will be displayed
     *                         in the GUI when the user selects the format.
     * @param fileTypes        File extensions commonly assigned to files of this format. Panels that allow the user to
     *                         select files may use this list to create file filters, but it is not required. May return {@code null}.
     */
    public Format(String formatIdentifier, String title, String notes, String[] fileTypes)
    {
        identifier = formatIdentifier;
        this.title = title;
        this.fileTypes = fileTypes;
        this.notes = notes;
    }

    /**
     * Get the identifier for the format. This identifier is for programmatic use, to identify the format. It is not
     * intended for display in the user interface.
     *
     * @return Format identifier
     */
    public String getIdentifier()
    {
        return identifier;
    }

    /**
     * Get the title for this format. Ths is a user friendly string suitable for display in a GUI
     *
     * @return The title
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * Get any notes that may be associated with the format (limitations, etc.)
     *
     * @return Format notes.
     */
    public String getNotes()
    {
        return notes;
    }

    /**
     * Get the file types allowable for this format.
     *
     * @return Array of file types.
     */
    public String[] getFileTypes()
    {
        return fileTypes;
    }

    /**
     * Returns a formatted string suitable for display in a GUI.
     *
     * @return Format title.
     */
    @Override
    public String toString()
    {
        return title;
    }
}
