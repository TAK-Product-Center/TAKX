package gov.takx.core.globe.imagerywizard;

import com.atakmap.map.layer.model.ModelInfoFactory;

import java.io.File;

/**
 * Extension of {@link FileSetFilter} that checks with TAK kernel to determine if it supports the 3D model dataset.
 *
 * @since 5.0
 */
public class ModelFileSetFilter extends FileSetFilter
{
    @Override
    public String getDescription()
    {
        return "3D Model";
    }

    @Override
    public boolean accept(File file)
    {
        return ModelInfoFactory.isSupported(file.getAbsolutePath());
    }
}
