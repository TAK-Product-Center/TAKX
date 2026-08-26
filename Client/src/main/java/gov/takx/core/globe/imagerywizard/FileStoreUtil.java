package gov.takx.core.globe.imagerywizard;

import gov.nasa.worldwind.Factory;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import gov.nasa.worldwind.util.DataConfigurationUtils;
import gov.nasa.worldwind.util.Level;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.util.WWXML;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.preferences.IUserPreferenceManager;
import gov.takx.platform.cdi.InjectableContainerSupport;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;

import java.io.File;
import java.io.FileFilter;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class FileStoreUtil
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long ONE_KILOBYTE = 1024L;
    private static final long ONE_MEGABYTE = ONE_KILOBYTE * 1024L;
    private static final long ONE_GIGABYTE = ONE_MEGABYTE * 1024L;
    private static final long ONE_TERABYTE = ONE_GIGABYTE * 1024L;
    private static final long ONE_PETABYTE = ONE_TERABYTE * 1024L;

    private static final InjectableContainerSupport BEAN_ACCESSOR = new InjectableContainerSupport();

    public enum WMSType
    {
        IMAGERY_WMS_SERVERS(UserPreferenceEntry.WMS_IMAGERY_SERVERS, UserPreferenceEntry.WCS_IMAGERY_SERVERS, "ImageryWMSServers"),
        ELEVATION_WMS_SERVERS(UserPreferenceEntry.WMS_ELEVATION_SERVERS, UserPreferenceEntry.WCS_ELEVATION_SERVERS, "ElevationWMSServers");

        private final String name;
        private final UserPreferenceEntry wmsPreferenceEntry;
        private final UserPreferenceEntry wcsPreferenceEntry;

        WMSType(UserPreferenceEntry wmsPreferenceEntry, UserPreferenceEntry wcsPreferenceEntry, String name)
        {
            this.wmsPreferenceEntry = wmsPreferenceEntry;
            this.wcsPreferenceEntry = wcsPreferenceEntry;
            this.name = name;
        }

        public String getName()
        {
            return name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

    private static IUserPreferenceManager getUserPreferenceManager()
    {
        return BEAN_ACCESSOR.getBean(IUserPreferenceManager.class);
    }

    public static void persistServers(Iterable<WMSInfo> servers, WMSType wmsType)
    {
        StringBuilder wmsServerList = new StringBuilder();
        StringBuilder wcsServerList = new StringBuilder();
        for (WMSInfo wmsInfo : servers)
        {
            switch (wmsInfo.getServiceType())
            {
                case WMS ->
                {
                    if (!wmsServerList.isEmpty()) wmsServerList.append(", ");
                    wmsServerList.append(wmsInfo.getOriginalURI());
                }
                case WCS ->
                {
                    if (!wcsServerList.isEmpty()) wcsServerList.append(", ");
                    wcsServerList.append(wmsInfo.getOriginalURI());
                }
                default -> logger.warn("Unexpected service type: {}", wmsInfo.getServiceType());
            }
        }

        wmsType.wmsPreferenceEntry.put(wmsServerList.toString());
        wmsType.wcsPreferenceEntry.put(wcsServerList.toString());

        getUserPreferenceManager().savePreferences();
    }

    public static Map<String, WMSInfo> registerDefaultServers(Map<String, WMSInfo> wmsInfoMap, WMSType wmsType)
    {
        parseServerList(wmsInfoMap, WMSInfo.ServiceType.WMS, wmsType.wmsPreferenceEntry.getStringValue());
        parseServerList(wmsInfoMap, WMSInfo.ServiceType.WCS, wmsType.wcsPreferenceEntry.getStringValue());

        return wmsInfoMap;
    }

    /**
     * Parse a comma separated list of server URLs.
     *
     * @param wmsInfoMap   Map to receive parsed server info.
     * @param serviceType  Type of service (WMS or WCS).
     * @param serverString Comma separated list of server URLs.
     * @since 1.43
     */
    private static void parseServerList(Map<String, WMSInfo> wmsInfoMap, WMSInfo.ServiceType serviceType,
                                        String serverString)
    {
        String[] serverNames = serverString.split(",");
        for (String serverName : serverNames)
        {
            if (!"".equals(serverName))
            {
                serverName = serverName.trim();
                WMSInfo wmsInfo = new WMSInfo(serverName, serviceType);
                // in the case of a proxy server, set to override with the proxy url.
                wmsInfo.setValue(AVKey.GET_CAPABILITIES_URL, serverName);
                wmsInfo.setValue(AVKey.GET_MAP_URL, serverName);
                wmsInfoMap.put(wmsInfo.getServerURI(), wmsInfo);
            }
        }
    }

    public static void registerInstalledWMSObjects(Map<String, WMSInfo> wmsInfoMap)
    {
        HashMap<String, List<LayerInfo>> installedObjectMap = new HashMap<>();

        // Find the objects whose service property matches one of the known URIs of a WMS. Create a list of LayerInfo
        // references for each WMS, which will represent the objects installed on that WMS.

        final WorldWindow worldWindow = BEAN_ACCESSOR.getBean(IGlobeManager.class).getPrimaryGlobeDelegate().getWorldWindow();

        Iterable<Layer> layers = worldWindow.getModel().getLayers();
        for (Layer layer : layers) registerWMSObject(layer, wmsInfoMap, installedObjectMap);

        ElevationModel model = worldWindow.getModel().getGlobe().getElevationModel();
        if (!(model instanceof CompoundElevationModel compoundModel)) throw new IllegalStateException("Elevation Model is not a compound model");

        for (ElevationModel subModel : compoundModel.getElevationModels()) registerWMSObject(subModel, wmsInfoMap, installedObjectMap);

        //flush all installed layers for each WMS - installed layers will be refreshed in the next step
        for (WMSInfo wmsInfo : wmsInfoMap.values()) wmsInfo.setInstalledLayers(null);

        //refresh the set of installed layers for each WMS
        for (Map.Entry<String, List<LayerInfo>> entry : installedObjectMap.entrySet())
        {
            WMSInfo wmsInfo = wmsInfoMap.get(entry.getKey());
            if (wmsInfo != null) wmsInfo.setInstalledLayers(entry.getValue());
        }
    }

    private static void registerWMSObject(AVList params,
                                          Map<String, WMSInfo> wmsInfoMap, HashMap<String, List<LayerInfo>> installedObjectMap)
    {
        String service = params.getStringValue(AVKey.SERVICE);
        String layerNames = params.getStringValue(AVKey.LAYER_NAMES);
        String styleNames = params.getStringValue(AVKey.STYLE_NAMES);
        if (service != null && layerNames != null)
        {
            WMSInfo wmsInfo = wmsInfoMap.get(service);
            //wms info may be null if a WMS layer is added, then its server url was removed from the wmsInfoMap
            //via the connect to wms server dialog remove button
            if (wmsInfo == null) return;
            installedObjectMap.computeIfAbsent(service, k -> new ArrayList<>()).add(new LayerInfo(wmsInfo, layerNames, styleNames));
        }
    }

    /**
     * Simple utility method to get an URL from a string (and silently ignore exceptions)
     *
     * @param stringUrl string url
     * @return actual URL object
     */
    public static URL getURL(String stringUrl)
    {
        try
        {
            return new URL(stringUrl);
        } catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Get an array of layer names from a raw WMS URL.
     *
     * @param wmsURL url to a WMS server specifying one or more layers
     * @return an array of layer names (as strings), or {@code null} if invalid wmsURL.
     */
    public static String[] getLayerNames(URL wmsURL)
    {
        String queryString = wmsURL.getQuery();
        if (queryString != null)
        {
            for (String q : queryString.split("&"))
            {
                if (q.startsWith("layers=")) return q.substring(7).split(",");
            }
        }
        return null;
    }

    /**
     * Create a World Wind layer from a configuration document and a parameter list. This method will copy metadata
     * values from the parameter list to the new Layer's AVList. The following metadata keys will be copied from the
     * parameters: {@link AVKey#SECTOR}, {@link InstallConstants#DATA_SOURCE}, {@link InstallConstants#RESOLUTION},
     * {@link InstallConstants#SIZE}, {@link AVKey#DATA_CACHE_NAME}.
     *
     * @param domElement Layer configuration document.
     * @param params     List of layer construction parameters. May contain metadata keys, which will be copied to the new
     *                   layer's AVList, as described above.
     * @return the new Layer, or {@code null} if the layer could not be created.
     */
    public static Layer createWWLayer(Element domElement, AVList params)
    {
        Layer layer = null;
        try
        {
            Factory factory = (Factory) WorldWind.createConfigurationComponent(AVKey.LAYER_FACTORY);
            layer = (Layer) factory.createFromConfigSource(domElement, params);
        } catch (Exception e)
        {
            logger.error("Creation from configuration failed {}", DataConfigurationUtils.getDataConfigDisplayName(domElement));
        }

        if (layer == null) return null;

        layer.setEnabled(true); // TODO: BasicLayerFactory creates layer which is intially disabled

        // Assign properties to the layer so it can be identified as derived from this DataConfiguration.
        setInstalledDataID(params, domElement, layer);

        // Copy metadata from the parameter list to the layer's AVList, so that it may later be retrieved and displayed
        // in the UI.
        copyMetadataParameters(params, layer);

        return layer;
    }

    public static void setFileStoreParams(FileStoreInfo info)
    {
        Object o = estimateSizeInBytes(info.getDataSets());
        info.setValue(InstallConstants.SIZE, o);
        info.firePropertyChange(InstallConstants.SIZE, null, o);

        o = makeTitle(info);
        info.setValue(AVKey.DISPLAY_NAME, o);
        info.firePropertyChange(AVKey.DISPLAY_NAME, null, o);

        info.setValue(AVKey.TITLE, o);
        info.firePropertyChange(AVKey.TITLE, null, o);
    }

    public static Iterable<FileStoreInfo> getDefaultFileStores()
    {
        ArrayList<FileStoreInfo> list = new ArrayList<>();
        for (File location : WorldWind.getDataFileStore().getLocations())
        {
            if (location.exists() && WorldWind.getDataFileStore().isInstallLocation(location.getPath()))
            {
                list.add(new FileStoreInfo(location.getPath(), true));
            }
        }
        return list;
    }

    public static ArrayList<ImportedDataSet> findImportedDataSets(@Nonnull File dir, @Nullable FileFilter filter)
    {
        String[] names = listDescendantFilenames(dir, filter, false);
        if (names == null || names.length == 0) return null;

        ArrayList<ImportedDataSet> dataSetList = new ArrayList<>();

        for (String pathname : names)
        {
            File file = new File(dir, pathname);

            Document doc = WWXML.openDocument(file);
            doc = DataConfigurationUtils.convertToStandardDataConfigDocument(doc);
            Element domElement = doc.getDocumentElement();

            // Populate the ImportedDataSet's parameter list.
            ImportedDataSet ds = new ImportedDataSet(domElement);

            String parent = WWIO.getParentFilePath(pathname);

            //Attempt to sanitize the parent folder.  If it is null or empty, then the user has selected the actual
            //cache folder and the parent/pathname Strings must be rebuilt.
            if (parent == null || parent.isEmpty())
            {
                parent = dir.getName();
                pathname = parent + File.separator + pathname;
                dir = dir.getParentFile();
            }
            setFallbackParams(domElement, pathname, ds);
            ds.setValue(AVKey.DISPLAY_NAME, DataConfigurationUtils.getDataConfigDisplayName(domElement));
            ds.setValue(InstallConstants.PATH, parent);
            ds.setValue(InstallConstants.SIZE, estimateSizeInBytes(dir, domElement, ds));
            ds.setValue(InstallConstants.INSTALLED_DATA_ID, parent);

            dataSetList.add(ds);
        }

        if (Thread.currentThread().isInterrupted()) return null;

        return dataSetList;
    }

    public static void setInstalledDataID(AVList source, Element domElement, AVList params)
    {
        String s = source.getStringValue(AVKey.DATA_CACHE_NAME);
        if (WWUtil.isEmpty(s)) s = WWXML.getText(domElement, "CacheName", null);
        params.setValue(InstallConstants.INSTALLED_DATA_ID, s);
        params.setValue(AVKey.DATA_CACHE_NAME, s);
    }

    /**
     * Copy metadata values from an imported data set descriptor to an layer or elevation model.
     *
     * @param srcList  List from which to copy metadata.
     * @param destList Object to which to add metadata. Typically an instance of {@link Layer} or {@link ElevationModel}.
     * @since 1.43
     */
    private static void copyMetadataParameters(AVList srcList, AVList destList)
    {
        // List of AV keys to copy. Any of these may be missing from the source list. If the destination list already
        // contains the key, then the value is /not/ overwritten.
        final String[] metadataKeys = {
                AVKey.SECTOR,
                InstallConstants.SIZE,
                InstallConstants.RESOLUTION,
                InstallConstants.DATA_SOURCE
        };
        WWUtil.copyValues(srcList, destList, metadataKeys, false);
    }

    protected static void setFallbackParams(Element domElement, String filename, AVList params)
    {
        XPath xpath = WWXML.makeXPath();

        // If the data configuration document doesn't define a cache name, then compute one using the file's path
        // relative to its file cache directory.
        String s = WWXML.getText(domElement, "DataCacheName", xpath);
        if (s == null || s.isEmpty()) DataConfigurationUtils.getDataConfigCacheName(filename, params);

        // If the data configuration document doesn't define the data's extreme elevations, provide default values using
        // the minimum and maximum elevations of Earth.
        String type = DataConfigurationUtils.getDataConfigType(domElement);
        if (type.equalsIgnoreCase("ElevationModel"))
        {
            if (WWXML.getDouble(domElement, "ExtremeElevations/@min", xpath) == null) params.setValue(AVKey.ELEVATION_MIN, Earth.ELEVATION_MIN);
            if (WWXML.getDouble(domElement, "ExtremeElevations/@max", xpath) == null) params.setValue(AVKey.ELEVATION_MAX, Earth.ELEVATION_MAX);
        }
    }

    /**
     * Estimates the size in bytes of the data on disk referenced by the specified data set. The returned value may be
     * smaller or larger than the actual size on disk.
     *
     * @param root       the data set's root directory.
     * @param domElement the data configuration document to estimate a size for.
     * @param params     the parameter list which extends or overrides parameters in the data configuration document.
     * @return the estimated size in bytes of the specified data set.
     */
    protected static long estimateSizeInBytes(File root, Element domElement, AVList params)
    {
        try
        {
            AVList levelSetParams = new AVListImpl();
            DataConfigurationUtils.getLevelSetConfigParams(domElement, levelSetParams);
            levelSetParams.setValues(params);

            LevelSet levelSet = new LevelSet(levelSetParams);
            // If the level set is empty, then its size in bytes is zero and we have nothing to compute.
            if (levelSet.getNumLevels() == 0) return 0;

            int topLevelNumber = levelSet.getFirstLevel().getLevelNumber();
            long tileSize = estimateTileSizeInBytes(root, levelSet, topLevelNumber, topLevelNumber + 1);
            long numTiles = getTileCount(levelSet);

            return tileSize * numTiles;
        } catch (Exception e)
        {
            logger.error("Invalid level descriptor fields: {}", DataConfigurationUtils.getDataConfigDisplayName(domElement));
        }

        return 0;
    }

    /**
     * Estimate size in bytes of a tile in the specified level set. Tiles are sampled from the specified levels to
     * compute the estimate.
     *
     * @param root     the file system root under which the level set data is located.
     * @param levelSet the level set which to estimate the tile size of.
     * @param levels   the level numbers which are used to compute the estimate.
     * @return the estimated tile size in bytes.
     */
    protected static long estimateTileSizeInBytes(File root, LevelSet levelSet, int... levels)
    {
        Sector sector = levelSet.getSector();

        long totalSize = 0L;
        int numTiles = 0;

        for (int levelNumber : levels)
        {
            if (levelNumber < 0 || levelNumber >= levelSet.getNumLevels()) continue;

            Level level = levelSet.getLevel(levelNumber);
            if (level.isEmpty()) continue;

            Angle dLat = level.getTileDelta().getLatitude();
            Angle dLon = level.getTileDelta().getLongitude();
            Angle latOrigin = levelSet.getTileOrigin().getLatitude();
            Angle lonOrigin = levelSet.getTileOrigin().getLongitude();
            int firstRow = Tile.computeRow(dLat, sector.getMinLatitude(), latOrigin);
            int firstCol = Tile.computeColumn(dLon, sector.getMinLongitude(), lonOrigin);
            int lastRow = Tile.computeRow(dLat, sector.getMaxLatitude(), latOrigin);
            int lastCol = Tile.computeColumn(dLon, sector.getMaxLongitude(), lonOrigin);

            Angle p1 = Tile.computeRowLatitude(firstRow, dLat, latOrigin);
            for (int row = firstRow; row <= lastRow; row++)
            {
                Angle p2 = p1.add(dLat);
                Angle t1 = Tile.computeColumnLongitude(firstCol, dLon, lonOrigin);
                for (int col = firstCol; col <= lastCol; col++)
                {
                    Angle t2 = t1.add(dLon);

                    Tile tile = new Tile(new Sector(p1, p2, t1, t2), level, row, col);
                    // We're only interested in the size of tiles which resize in the filesystem, therefore we do not
                    // search the class path. If the tile cannot be found, or cannot be represented as a file path,
                    // then it does not contribute to the computation.
                    File file = new File(root, tile.getPath());
                    if (file.exists())
                    {
                        long length = file.length();
                        if (length > 0L)
                        {
                            totalSize += length;
                            numTiles++;
                        }
                    }

                    t1 = t2;
                }
                p1 = p2;
            }
        }

        return (long) Math.ceil(totalSize / (double) numTiles);
    }

    /**
     * Computes total number of tiles in all non-empty levels of the specified level set. Tiles at any level which do
     * not intersect the level set's sector are not counted.
     *
     * @param levelSet the level set to compute the number of tiles for.
     * @return the number of tiles in the specified level set.
     */
    protected static long getTileCount(LevelSet levelSet)
    {
        Sector sector = levelSet.getSector();

        long numTiles = 0;
        for (Level level : levelSet.getLevels())
        {
            if (level.isEmpty()) continue;

            Angle dLat = level.getTileDelta().getLatitude();
            Angle dLon = level.getTileDelta().getLongitude();
            Angle latOrigin = levelSet.getTileOrigin().getLatitude();
            Angle lonOrigin = levelSet.getTileOrigin().getLongitude();
            int firstRow = Tile.computeRow(dLat, sector.getMinLatitude(), latOrigin);
            int firstCol = Tile.computeColumn(dLon, sector.getMinLongitude(), lonOrigin);
            int lastRow = Tile.computeRow(dLat, sector.getMaxLatitude(), latOrigin);
            int lastCol = Tile.computeColumn(dLon, sector.getMaxLongitude(), lonOrigin);
            numTiles += (long) (lastRow - firstRow + 1) * (lastCol - firstCol + 1);
        }

        return numTiles;
    }

    private static long estimateSizeInBytes(Iterable<?> values)
    {
        long sizeInBytes = 0L;
        for (Object val : values)
        {
            if (val instanceof AVList)
            {
                Object o = ((AVList) val).getValue(InstallConstants.SIZE);
                if (o instanceof Number) sizeInBytes += ((Number) o).longValue();
            }
        }
        return sizeInBytes;
    }

    private static String makeTitle(FileStoreInfo info)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><b>");
        sb.append(info.getLocation());
        Object o = info.getValue(InstallConstants.SIZE);
        if (o instanceof Number)
        {
            // Size in bytes is an estimate, so precede the value with a tilde character.
            long sizeInBytes = ((Number) o).longValue();
            sb.append(" (~").append(formatSize(sizeInBytes)).append(")");
        }
        sb.append("</b>");

        if (info.isDefaultFileStore()) sb.append("<br><i><font size='-1'>Standard File Store Location</font></i>");

        sb.append("</html>");
        return sb.toString();
    }

    private static String formatSize(long sizeInBytes)
    {
        if (sizeInBytes >= ONE_PETABYTE)
        {
            return String.format("%5.2f PB", (double) sizeInBytes / ONE_PETABYTE);
        } else if (sizeInBytes >= ONE_TERABYTE)
        {
            return String.format("%5.2f TB", (double) sizeInBytes / ONE_TERABYTE);
        } else if (sizeInBytes >= ONE_GIGABYTE)
        {
            return String.format("%5.2f GB", (double) sizeInBytes / ONE_GIGABYTE);
        } else if (sizeInBytes >= ONE_MEGABYTE)
        {
            return String.format("%5.2f MB", (double) sizeInBytes / ONE_MEGABYTE);
        } else if (sizeInBytes >= ONE_KILOBYTE)
        {
            return String.format("%5.2f KB", (double) sizeInBytes / ONE_KILOBYTE);
        }
        return String.format("%d bytes", sizeInBytes);
    }

    /**
     * Returns an array of relative file paths naming the files and directories in the directory tree rooted by the
     * specified file, that satisfy the specified filter. If the parameter {@code recurseAfterMatch} is false, then
     * this ignores any directory branches beneath a matched file. This has the effect of listing the top matches in the
     * directory tree. The returned paths are relative to the specified file. If the filter is {@code null}, then all files and
     * directories are accepted. This returns {@code null} if the specified file is not a directory.
     *
     * @param file              the directory tree whose contents to list. Must not be {@code null}.
     * @param filter            a file filter.
     * @param recurseAfterMatch true to list the contents of directory branches beneath a match; false to ignore
     *                          branches beneath a match.
     * @return an array of relative file paths naming the files and directories in the directory tree rooted by the
     * specified file, or {@code null} if the specified file is not a directory.
     */
    public static String[] listDescendantFilenames(@Nonnull File file, FileFilter filter, boolean recurseAfterMatch)
    {
        // List the file and directory names in the specified file. If the returned array is null, then the specified
        // file does not denote a directory.
        if (file.list() == null) return null;

        ArrayList<String> matches = new ArrayList<>();
        listDescendantFilenames(file, null, filter, recurseAfterMatch, matches);

        if (Thread.currentThread().isInterrupted()) return null;

        return matches.toArray(new String[0]);
    }

    protected static void listDescendantFilenames(File parent, String pathname, FileFilter filter,
                                                  boolean recurseAfterMatch, Collection<String> matches)
    {
        if (Thread.currentThread().isInterrupted()) return;
        // Create a file pointing to the file denoted by the parent file and child pathname string. Use the parent file
        // if the pathname string is null.
        File file = (pathname != null) ? new File(parent, pathname) : parent;

        // List the file and directory names in the specified file. Exit if the returned filename array is null,
        // indicating that the specified file does not denote a directory.
        String[] names = file.list();
        if (names == null) return;

        boolean haveMatch = false;

        // Collect the non-null pathnames which match the specified filter, and collect the non-null directory names
        // in a temporary list.
        for (String filename : names)
        {
            // Ignore null or empty filenames.
            if (filename == null || filename.isEmpty()) continue;

            // If the filter is null, then all pathnames are accepted.
            if (filter != null && !filter.accept(new File(file, filename))) continue;

            matches.add(appendPathPart(pathname, filename));
            haveMatch = true;
        }

        // Exit if any of the file or directories in the specified file match the file filter, and the caller has
        // specified to stop recursing after a match.
        if (haveMatch && !recurseAfterMatch) return;

        // Recursively process the contents of each path .
        for (String filename : names)
        {
            if (Thread.currentThread().isInterrupted()) return;
            listDescendantFilenames(parent, appendPathPart(pathname, filename), filter, recurseAfterMatch, matches);
        }
    }

    public static String appendPathPart(String firstPart, String secondPart)
    {
        if (secondPart == null || secondPart.isEmpty()) return firstPart;
        if (firstPart == null || firstPart.isEmpty()) return secondPart;
        return WWIO.stripTrailingSeparator(firstPart) + File.separator + WWIO.stripLeadingSeparator(secondPart);
    }
}
