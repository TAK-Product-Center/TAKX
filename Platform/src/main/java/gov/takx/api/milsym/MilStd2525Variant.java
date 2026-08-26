package gov.takx.api.milsym;

import java.util.ArrayList;
import java.util.List;

/**
 * 2525 implementation variant of a {@link IMilStd2525Service}.
 *
 * @since 5.5
 */
public enum MilStd2525Variant
{
    //To add new variant, add enum with version number and text name for menu here.
    //Standards that are not numeric digit only codes, like 2525C, need to adopt a version # that is < 10 to intentionally fail the check in MilStdServiceProvider.get2525ServiceForSymbol(
    //Also update getVersionFromName function in this enum.
    //Create IconSelectionPanel, just refactor one of the other ones that extend (like APP6DIconSelectionPanel) and change version# in constructor
    //Update MilStd2525ServiceProvider, add new MilStd2525Service up top and update initServiceCollection()
    //Update PointDropperPanel, add new dropdown values
    //Update MilStd2525Service.convertFromOther2525Variant() as needed
    CHARLIE(9, "Charlie"),
    DELTA(11, "Delta"),
    ECHO(13, "Echo"),
    /**
     * @since 5.8
     */
    ECHOCH1(15, "EchoCh1"),
    /**
     * @since 5.8
     */
    APP6D(10, "APP6D");

    private final int version;
    private final String name;

    MilStd2525Variant(int version, String name)
    {
        this.version = version;
        this.name = name;
    }

    /**
     * @return the version # associated with the MilStd as specified by 2525 and APP6
     * @since 5.8
     */
    public int version()
    {
        return version;
    }

    /**
     * @return String representing the name of tha variant
     * @since 5.8
     */
    public String value()
    {
        return name;
    }

    /**
     *
     * @param name like {@link MilStd2525Variant#value()}
     * @return The associated MilStd2525Variant or {@link MilStd2525Variant#CHARLIE} if no match.
     * @since 5.8
     */
    public static MilStd2525Variant fromName(String name)
    {
        for (var variant : values())
        {
            if (variant.value().equals(name)) return variant;
        }
        return CHARLIE;
    }

    /**
     *
     * @param version as defined in the MilStd and APP6 documents.  For 2525C, use a number less than 10.
     *                For 2525D and 2525Dch1 use 11.  For APP6D use 10. E(13), Ech1(15), APP6Ev2(16)
     * @return If there's a match, returns the variant associated with the version number, otherwise, returns CHARLIE
     * @since 5.8
     */
    public static MilStd2525Variant fromVersion(int version)
    {
        for (var variant : values())
        {
            if (variant.version() == version) return variant;
        }
        return CHARLIE;
    }

    /**
     *
     * @return List of names for the MilStd2525Variants
     * @since 5.8
     */
    public static List<String> getNameList()
    {
        List<String> nameList = new ArrayList<>();
        for (var variant : values()) nameList.add(variant.value());
        return nameList;
    }
}
