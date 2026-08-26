package gov.tak.api.icons;

import gov.tak.api.cot.detail.DetailConstants;

/**
 * Constants related to the {@value DetailConstants#ATTR_ICONSET_PATH} attribute of the {@value DetailConstants#USER_ICON} detail.
 *
 * @since 5.3
 */
public class IconSetConstants
{
    /** Icon set path indicating a spot map icon. */
    public static final String COT_MAPPING_SPOTMAP = "COT_MAPPING_SPOTMAP";

    /** spot map color indicating to show only the label and not an icon. */
    public static final String LABEL = "LABEL";

    /** Icon set path indicating a MIL-2525 symbol should be used based on CoT type. */
    public static final String COT_MAPPING_2525C = "COT_MAPPING_2525C";
}
