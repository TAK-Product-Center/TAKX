package gov.takx.core.view.gui.maps.exporters.dataset;

import com.google.common.collect.Maps;
import gov.nasa.worldwind.geom.Sector;
import gov.takx.commons.wizard.IWizardModel;
import gov.takx.commons.wizard.IWizardProperties;
import gov.takx.core.dataset.DataSetBuilderParameters;
import gov.takx.core.view.gui.wizard.WizardModel;

import java.util.List;
import java.util.Map;

/**
 * Builder that creates a collection of map set properties as a map or a {@link IWizardProperties}
 * object.
 *
 * @since 1.43
 */
public class MapSetExportWizardPropertiesBuilder
{
    private String name;
    private String dir;
    private List<DataSetBuilderParameters> sources;
    private boolean autoConnect;
    private Sector sector;
    private Boundary boundary;

    public MapSetExportWizardPropertiesBuilder setName(String name)
    {
        this.name = name;
        return this;
    }

    public MapSetExportWizardPropertiesBuilder setDir(String dir)
    {
        this.dir = dir;
        return this;
    }

    public MapSetExportWizardPropertiesBuilder setSources(List<DataSetBuilderParameters> sources)
    {
        this.sources = sources;
        return this;
    }

    public MapSetExportWizardPropertiesBuilder setSector(Sector sector)
    {
        this.sector = sector;
        return this;
    }

    public MapSetExportWizardPropertiesBuilder setBoundary(Boundary boundary)
    {
        this.boundary = boundary;
        return this;
    }

    public MapSetExportWizardPropertiesBuilder setAutoConnect(boolean autoConnect)
    {
        this.autoConnect = autoConnect;
        return this;
    }

    public IWizardModel createMapSetExportWizardModel()
    {
        final WizardModel model = new WizardModel();

        final Map<String, Object> propMap = getPropertyMap();

        for (Map.Entry<String, Object> e : propMap.entrySet())
        {
            model.setProperty(e.getKey(), e.getValue());
        }

        return model;
    }

    private Map<String, Object> getPropertyMap()
    {
        final Map<String, Object> properties = Maps.newHashMap();

        properties.put(DataSetBuilderConstants.NAME, name);
        properties.put(DataSetBuilderConstants.TARGET_DIR, dir);
        properties.put(DataSetBuilderConstants.LAYERS, sources);
        properties.put(DataSetBuilderConstants.CONNECT, autoConnect);
        properties.put(DataSetBuilderConstants.SECTOR, sector);
        properties.put(DataSetBuilderConstants.BOUNDARY, boundary);

        return properties;
    }
}
