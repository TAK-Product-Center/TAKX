package gov.takx.core.view.gui.maps.exporters.dataset;

/**
 * Data set builder constants.
 *
 * @since 2.37, was previously an interface named IDataSetBuilderConstants
 */
public class DataSetBuilderConstants
{
    /**
     * Constant for the data set name key.
     */
    public static final String NAME = "Name";

    /**
     * Constant for the key associated with the target directory value.
     */
    public static final String TARGET_DIR = "Target Dir";

    /**
     * The user-defined sector that should be downloaded.
     */
    public static final String SECTOR = "Sector";

    /**
     * The user-defined arbitrary boundary that should be downloaded.
     */
    public static final String BOUNDARY = "Boundary";

    /**
     * The layers to include in the build.
     */
    public static final String LAYERS = "Layers";

    /**
     * Flag that indicates that TAKX should connect to the resulting map set upon completion.
     */
    public static final String CONNECT = "Connect To MapSet";

    private DataSetBuilderConstants()
    {
    }
}
