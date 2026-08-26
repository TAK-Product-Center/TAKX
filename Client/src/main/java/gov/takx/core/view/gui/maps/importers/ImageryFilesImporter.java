package gov.takx.core.view.gui.maps.importers;

import gov.nasa.worldwind.avlist.AVKey;
import gov.takx.api.layer.builder.LocalImageryLayerBuilder;
import gov.takx.commons.file.FileUtils;
import gov.takx.core.globe.imagerywizard.FileSet;
import gov.takx.core.globe.imagerywizard.InstallConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Imports map imagery to TAK globe.
 *
 * @since 3.4.1 (renamed from TakConvertImageryMapsImporter)
 */
public class ImageryFilesImporter extends AMapFilesImporter
{
    /**
     * A list of files that must be imported individually
     *
     * @since 5.7
     */
    private static final List<String> INDIVIDUAL_IMPORT_EXTENSIONS = List.of("sqlite", "mbtiles", "tif", "tiff");

    private final List<FileSet> selectedFileSets;

    public ImageryFilesImporter()
    {
        super(new HashMap<>());
        selectedFileSets = Collections.emptyList();
    }

    /**
     * @param selectedFiles the selected files
     * @param displayName the display name
     * @param basePath the base path
     * @since 5.2
     */
    public ImageryFilesImporter(List<File> selectedFiles, String displayName, String basePath)
    {
        super(new HashMap<>());
        getDataMap().put(AVKey.DISPLAY_NAME, displayName);
        getDataMap().put(InstallConstants.PATH, basePath);
        FileSet fileSet = new FileSet();
        fileSet.setFiles(selectedFiles);
        selectedFileSets = Collections.singletonList(fileSet);
    }

    public ImageryFilesImporter(List<FileSet> selectedFileSets, Map<String, Object> dataMap)
    {
        super(dataMap);
        this.selectedFileSets = Collections.unmodifiableList(selectedFileSets);
    }

    @Override
    public void beginImport()
    {
        for (FileSet fileSet : selectedFileSets)
        {
            final var displayName = (String) fileSet.getValue(AVKey.DISPLAY_NAME);
            final var path = fileSet.getStringValue(InstallConstants.PATH);

            // Determine if there are files that need to be imported individually
            final var files = fileSet.getFiles();
            final var fileCount = files.size();
            final var individualFiles = fileCount == 1
                    ? files
                    : files.stream()
                            .filter(f -> INDIVIDUAL_IMPORT_EXTENSIONS.contains(FileUtils.getExtension(f.getName())))
                            .toList();

            final var filesToImport = new ArrayList<>(individualFiles);

            // If there are other files to import (or no individual files), add the top level directory
            if (individualFiles.size() < fileCount) filesToImport.add(0, new File(path));

            final var layerName = filesToImport.size() == 1 ? filesToImport.get(0).getAbsolutePath() : path;

            final var configuration = LocalImageryLayerBuilder.builder()
                    .setName(layerName)
                    .setDisplayName(displayName)
                    .setFiles(filesToImport)
                    .build();

            createLayer(configuration, layerName);
        }

        reportConflicts();
    }
}
