package gov.takx.core.globe.imagerywizard;

import gov.nasa.worldwind.avlist.AVListImpl;
import jakarta.annotation.Nonnull;
import org.w3c.dom.Element;

/**
 */
public class ImportedDataSet extends AVListImpl
{
    protected Element dataConfigElement;

    public ImportedDataSet(@Nonnull Element dataConfigElement)
    {
        this.dataConfigElement = dataConfigElement;
    }

    public Element getDataConfigElement()
    {
        return dataConfigElement;
    }
}
