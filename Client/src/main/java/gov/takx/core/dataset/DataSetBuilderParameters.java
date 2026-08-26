package gov.takx.core.dataset;

import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import gov.takx.core.view.gui.maps.exporters.dataset.Boundary;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Parameters that control what data is added to a data set. These parameters control the geographic region in which
 * data is downloaded, and the resolution at which data is downloaded. Resolution is specified in meters
 * per pixel.
 *
 * @param boundary The boundary in which to download data.
 * @param lowResolution The lowest resolution at which to download data, or {@code null} to set no limit.
 * @param hiResolution The highest resolution at which to download data, or {@code null} to set no limit.
 * @param buildOrder The suggested order that data should be added to the data set.
 * @param datasetDescriptors The dataset descriptors of the layers to download data from.
 * @param elevationSource  The elevation source to download data from.
 * @since 1.42
 */
public record DataSetBuilderParameters(@Nonnull Boundary boundary, Double lowResolution, Double hiResolution, @Nonnull BuildOrder buildOrder, @Nullable DatasetDescriptor[] datasetDescriptors, @Nullable ElevationSource elevationSource)
{
    /**
     * Options for preference the order in which data is added to a data set.
     */
    public enum BuildOrder
    {
        LOW_RESOLUTION_FIRST,
        HIGH_RESOLUTION_FIRST
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private BuildOrder order = BuildOrder.HIGH_RESOLUTION_FIRST;
        private Double hiResolution;
        private Double lowResolution;
        private Boundary boundary;
        private DatasetDescriptor[] datasetDescriptors;
        private ElevationSource elevationSource;

        /**
        * Indicates the preferred order in which data will be added to the data set: either low resolution first or
        * high resolution first. Low-resolution-first (the default) asks the data set builder to add low resolution
        * overviews to the data set before adding high resolution data. In high-resolution-first order the opposite is true:
        * high resolution tiles will be added before overviews.
        * <p/>
        * This setting is intended for cases in which data set construction may be interrupted (such as due to a maximum
        * disk space constraint). The setting allows an application to express a preference for having the entire area of
        * interest in the data set at (potentially) lower resolution, or having as much high resolution imagery available
        * as possible.
        * <p/>
        * Note that this parameter expresses a preference and not a hard requirement. Data set builders are not required
        * to follow the preference.
        *
        * @return The order in which data should be added to the data set.
        */
        public Builder setOrder(DataSetBuilderParameters.BuildOrder order)
        {
            this.order = order;
            return this;
        }

        /**
        * Specifies the highest (finest) resolution at which data will be downloaded.
        *
        * @param hiResolution Highest resolution (in radians of latitude per pixel) at which to download data. May be {@code null}
        *                     to specify the lowest available resolution. May not be less than zero.
        */
        public Builder setHiResolution(Double hiResolution)
        {
            this.hiResolution = hiResolution;
            return this;
        }

        /**
        * Specifies the lowest (coarsest) resolution at which data will be downloaded.
        *
        * @param lowResolution Lowest resolution (in radians of latitude per pixel) at which to download data. May be {@code null}
        *                      to specify the lowest available resolution. May not be less than zero.
        */
        public Builder setLowResolution(Double lowResolution)
        {
            this.lowResolution = lowResolution;
            return this;
        }

        /**
        * Specifies the boundary in which data will be downloaded.
        *
        * @param boundary Boundary in which to download data.
        **/
        public Builder setBoundary(@Nonnull Boundary boundary)
        {
            this.boundary = boundary;
            return this;
        }

        /**
        * Specifies the dataset descriptors for the data to be downloaded.
        *
        * @param datasetDescriptors the dataset descriptors
        * @since 5.0
        */
        public Builder setDatasetDescriptors(@Nullable DatasetDescriptor[] datasetDescriptors)
        {
            this.datasetDescriptors = datasetDescriptors != null ? datasetDescriptors.clone() : null;
            return this;
        }

        /**
         * Specifies the elevation source for the data to be downloaded.
         *
         * @param elevationSource the elevation source
         * @since 5.6
         */
        public Builder setElevationSource(@Nullable ElevationSource elevationSource)
        {
            this.elevationSource = elevationSource;
            return this;
        }

        /**
         * Construct the {@link DataSetBuilderParameters}.
         * @return the parameters object
         * @throws IllegalArgumentException if the boundary is not set
         */
        public DataSetBuilderParameters build()
        {
            if (boundary == null) throw new IllegalArgumentException("Boundary is not set.");

            return new DataSetBuilderParameters(boundary, lowResolution, hiResolution, order, datasetDescriptors, elevationSource);
        }
    }
}
