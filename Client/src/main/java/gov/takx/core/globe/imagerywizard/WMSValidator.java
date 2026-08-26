/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.takx.core.globe.imagerywizard;

import gov.nasa.worldwind.ogc.wcs.wcs100.WCS100Capabilities;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Set;

/**
 */
public class WMSValidator
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String[] requiredFormats;

    public WMSValidator(String[] requiredFormats)
    {
        int count = requiredFormats.length;
        this.requiredFormats = new String[count];
        System.arraycopy(requiredFormats, 0, this.requiredFormats, 0, count);
    }

    public String[] getRequiredFormats()
    {
        return this.requiredFormats;
    }

    public String validate(WMSInfo wmsInfo)
    {
        switch (wmsInfo.getServiceType())
        {
            case WMS:
                return validateWMS(wmsInfo);
            case WCS:
                return validateWCS(wmsInfo);
            default:
                logger.warn("Unexpected service type: {}", wmsInfo.getServiceType());
                return InstallConstants.UNKNOWN_ERROR;
        }
    }

    /**
     * Validate a Web Map Service (WMS) server.
     *
     * @param wmsInfo server to validate.
     * @return {@link InstallConstants#OK} if the server is valid, or a string indicating the error if not.
     */
    private String validateWMS(WMSInfo wmsInfo)
    {
        WMSCapabilities caps = (WMSCapabilities) wmsInfo.getCapabilities();
        if (caps == null)
        {
            return InstallConstants.CAPABILITIES_IS_NULL;
        }

        String match = null;
        Set<String> formats = caps.getImageFormats();
        if (formats != null && formats.size() > 0)
        {
            for (String s : this.requiredFormats)
            {
                for (String f : formats)
                {
                    if (f.equalsIgnoreCase(s))
                    {
                        match = f;
                        break;
                    }
                }
            }
        }
        if (match == null)
        {
            return InstallConstants.INCOMPATIBLE_FORMATS;
        }

        return InstallConstants.OK;
    }

    /**
     * Validate a Web Coverage Service (WCS) server.
     *
     * @param wmsInfo server to validate.
     * @return {@link InstallConstants#OK} if the server is valid, or a string indicating the error if not.
     */
    private String validateWCS(WMSInfo wmsInfo)
    {
        WCS100Capabilities caps = (WCS100Capabilities) wmsInfo.getCapabilities();
        if (caps == null || caps.getService() == null)
        {
            return InstallConstants.CAPABILITIES_IS_NULL;
        }

        return InstallConstants.OK; // TODO
    }
}
