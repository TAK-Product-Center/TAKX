package gov.takx.core.view.gui.wizard;

import gov.takx.commons.wizard.IWizardProperties;
import jakarta.annotation.Nullable;

import java.util.Map;

/**
 */
public class WizardProperties extends gov.nasa.worldwind.util.wizard.WizardProperties implements IWizardProperties
{
    public WizardProperties()
    {
    }

    /**
     * Constructor.
     *
     * @param preconfigProps Properties to use to initialize the properties map
     * @since 3.0
     */
    public WizardProperties(@Nullable Map<String, Object> preconfigProps)
    {
        if (preconfigProps != null) preconfigProps.forEach(this::setProperty);
    }
}
