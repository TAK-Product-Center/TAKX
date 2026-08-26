package gov.takx.core.explorer.rules;

import gov.takx.api.explorer.AExplorableRule;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorableRuleFactory;
import gov.takx.api.explorer.IExplorableRuleViewComponent;
import gov.takx.api.explorer.IMapObjectExplorable;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.state.IRestorableMap;
import gov.takx.core.view.gui.explorer.rules.MapObjectRulePanel;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @since 2.6.1
 */
public class MapObjectRule extends AExplorableRule
{
    public static final String WILDCARD = "*";

    private static final String TYPE = "Map Object Match";
    private static final String INSTANCE_KEY = "INSTANCE_KEY";

    private String matchFamily;
    private String matchType;
    private String matchUnitId;

    public MapObjectRule()
    {
        super(TYPE, TYPE);
    }

    @Override
    public void store(IRestorableMap map)
    {
        super.store(map);
        map.setStringList(INSTANCE_KEY, getMatchParts());
    }

    @Override
    public void restore(IRestorableMap map)
    {
        super.restore(map);
        List<String> path = map.getStringList(INSTANCE_KEY, new ArrayList<>());
        matchFamily = path.get(0);
        matchType = path.get(1);
        matchUnitId = path.get(2);
    }

    @Override
    public boolean isMatch(IExplorable explorable)
    {
        if (explorable instanceof IMapObjectExplorable)
        {
            final IMapObject mapObjectToTest = ((IMapObjectExplorable) explorable).getMapObject();

            if (matchMapObject(mapObjectToTest)) return true;

            // If it's a shape check to see if it was created by a device since the shape would not match the rule
            if (mapObjectToTest instanceof IShapeMapObject)
            {
                return matchMapObject(getParentIfDevice(mapObjectToTest));
            }
        }

        return false;
    }

    /**
     * Test the given Map Object's key parts to our configuration.
     *
     * @param mapObjectToTest MO to test
     * @return {@code true} if the family/type/unitId match our configuration
     * @since 3.0
     */
    private boolean matchMapObject(IMapObject mapObjectToTest)
    {
        if (matchFamily.equals(WILDCARD)) return true;
        if (!matchFamily.equals(mapObjectToTest.getFamily())) return false;

        if (matchType.equals(WILDCARD)) return true;
        if (!matchType.equals(mapObjectToTest.getType())) return false;

        if (matchUnitId.equals(WILDCARD)) return true;
        return matchUnitId.equals(mapObjectToTest.getUnitID());
    }

    /**
     * Get the device that created the shape if there is one
     *
     * @param mo The shape map object that may have been created by a device
     * @return The parent device of the shape or the original shape map object if it was not created by a device
     * @since 2.36
     */
    private IMapObject getParentIfDevice(IMapObject mo)
    {
        final IMapObject mapObjectOwner = mo.getOwner();
        return mapObjectOwner != null && mapObjectOwner.isDevice() ? mapObjectOwner : mo;
    }

    @Nonnull
    @Override
    public IExplorableRuleViewComponent createViewComponent()
    {
        return new MapObjectRulePanel(this);
    }

    /**
     * @return A list containing our family, type, and unit Id, in that order
     * @since 3.0
     */
    public List<String> getMatchParts()
    {
        return Arrays.asList(matchFamily, matchType, matchUnitId);
    }

    /**
     * Set the match parts for this rule.
     *
     * @param matchFamily Family to match (or wildcard)
     * @param matchType   Type to match (or wildcard)
     * @param matchUnitId Unit Id to match (or wildcard)
     * @since 3.0
     */
    public void setMatchParts(String matchFamily, String matchType, String matchUnitId)
    {
        this.matchFamily = matchFamily;
        this.matchType = matchType;
        this.matchUnitId = matchUnitId;
    }

    @Override
    public String toString()
    {
        return "MapObjectRule{" +
                "pattern=" + String.join(".", getMatchParts()) +
                '}';
    }

    public static IExplorableRuleFactory<MapObjectRule> getFactory()
    {
        return new IExplorableRuleFactory<>()
        {
            @Override
            public MapObjectRule createRule()
            {
                return new MapObjectRule();
            }

            @Override
            public String getDisplayName()
            {
                return TYPE;
            }

            @Override
            public String getType()
            {
                return TYPE;
            }
        };
    }
}
