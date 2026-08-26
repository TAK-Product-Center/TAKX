package gov.takx.test.mock.plugin.io;

import gov.takx.api.io.Format;
import gov.takx.api.io.ImportExportResult;
import gov.takx.api.plugin.io.IImportExportPluginDelegate;
import gov.takx.api.plugin.io.IImportExportProgressMonitor;
import gov.takx.api.plugin.io.IImportWizard;
import gov.takx.api.plugin.io.IWizardResultListener;
import gov.takx.commons.wizard.IWizardPanelDescriptor;
import gov.takx.commons.wizard.IWizardProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic Import Wizard for testing API Wizards
 *
 */
public class TestImportWizard2 implements IImportWizard
{
    @Override
    public List<Format> getFormats()
    {
        List<Format> formats = new ArrayList<>();
        formats.add(new Format("abc", "ABC", "Import a ABC file.", new String[]{"abc"}));
        formats.add(new Format("abc", "ABCD", "Import a ABCD file.", new String[]{"abcd"}));
        return formats;
    }

    @Override
    public void setFormat(Format format)
    {
    }

    @Override
    public void setFormats(List<Format> formats)
    {
    }

    @Override
    public Map<String, Object> getPreconfigurationSettings()
    {
        Map<String, Object> map = new HashMap<>();
        map.put("42", new Object());
        return map;
    }

    @Override
    public String getFirstWizardPanel()
    {
        return IWizardPanelDescriptor.FINISH;
    }

    @Override
    public void setDelegate(IImportExportPluginDelegate delegate)
    {
    }

    @Override
    public boolean allowCancel()
    {
        return false;
    }

    @Override
    public void cancel()
    {
    }

    @Override
    public void addResultListener(IWizardResultListener listener)
    {
    }

    @Override
    public void preconfigureWizard(Map<String, Object> configuration)
    {
    }

    @Override
    public ImportExportResult doImport(IWizardProperties props, IImportExportProgressMonitor progressMonitor)
    {
        return null;
    }
}
