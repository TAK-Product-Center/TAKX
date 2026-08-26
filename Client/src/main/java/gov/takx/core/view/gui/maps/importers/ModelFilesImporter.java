package gov.takx.core.view.gui.maps.importers;

import gov.nasa.worldwind.avlist.AVKey;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.layer.ILayerService;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.LayerConfiguration;
import gov.takx.api.layer.builder.ModelLayerBuilder;
import gov.takx.commons.file.FileUtils;
import gov.takx.core.globe.imagerywizard.FileSet;
import gov.takx.core.globe.imagerywizard.InstallConstants;
import jakarta.inject.Inject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Imports 3D model data to TAK globe.
 *
 * @since 5.0
 */
@PseudoBean
public class ModelFilesImporter implements IImporter
{
    private static final List<String> INDIVIDUAL_IMPORT_EXTENSIONS = List.of("kml", "kmz");

    @Inject
    private IRaptorLayerManager raptorLayerManager;

    @Inject
    private ILayerService layerService;

    private final List<FileSet> selectedFileSets;

    public ModelFilesImporter(List<FileSet> selectedFileSets)
    {
        this.selectedFileSets = Collections.unmodifiableList(selectedFileSets);
    }

    @Override
    public void beginImport()
    {
        for (FileSet fileSet : selectedFileSets)
        {
            final var displayName = (String) fileSet.getValue(AVKey.DISPLAY_NAME);
            final var path = (String) fileSet.getValue(InstallConstants.PATH);

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

            if (raptorLayerManager.getLayer(layerName) == null)
            {
                LayerConfiguration layerConfiguration = ModelLayerBuilder.builder()
                        .setName(layerName)
                        .setDisplayName(displayName)
                        .setFiles(filesToImport)
                        .build();
                layerService.addLayer(layerConfiguration);
            }
        }
    }
}
