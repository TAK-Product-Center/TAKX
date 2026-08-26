package gov.takx.core.view.gui.maps.importers;

import gov.nasa.worldwind.avlist.AVKey;
import gov.takx.api.layer.builder.ElevationLayerBuilder;
import gov.takx.core.globe.imagerywizard.FileSet;
import gov.takx.core.globe.tak.elevation.ElevationLayerManager;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Imports map elevation data to TAK globe.
 *
 * @since 3.4.1 (renamed from TakConvertElevationMapsImporter)
 */
public class ElevationFilesImporter extends AMapFilesImporter
{
    private final List<FileSet> selectedFileSets;

    public ElevationFilesImporter(List<FileSet> selectedFileSets, Map<String, Object> dataMap)
    {
        super(dataMap);
        this.selectedFileSets = Collections.unmodifiableList(selectedFileSets);
    }

    @Override
    public void beginImport()
    {
        for (FileSet fileSet : selectedFileSets)
        {
            List<File> files = getFiles(fileSet);
            for (File file : files)
            {
                String displayName = files.size() == 1 ? (String) fileSet.getValue(AVKey.DISPLAY_NAME) : file.getName();
                String name = file.getAbsolutePath();

                final var configuration = ElevationLayerBuilder.builder()
                        .setName(name)
                        .setDisplayName(displayName)
                        .setFile(file)
                        .build();

                createLayer(configuration, name);
            }
        }

        reportConflicts();
    }

    private List<File> getFiles(FileSet fileSet)
    {
        if (!fileSet.getFiles().isEmpty() && ElevationLayerManager.isSupportedDtedFile(fileSet.getFiles().get(0)))
        {
            // We only support importing entire dted directories, not individual files
            File rootDtedPath = ElevationLayerManager.getRootDtedPath(fileSet.getFiles().get(0));
            return rootDtedPath != null ? List.of(rootDtedPath) : Collections.emptyList();
        } else
        {
            return fileSet.getFiles();
        }
    }
}
