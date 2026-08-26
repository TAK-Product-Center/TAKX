package gov.takx.core.explorer.rules;

import gov.takx.api.explorer.AExplorableRule;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorableRuleFactory;
import gov.takx.api.explorer.IExplorableRuleViewComponent;
import gov.takx.api.explorer.INameRule;
import gov.takx.api.state.IRestorableMap;
import gov.takx.core.view.gui.explorer.rules.NameRulePanel;
import jakarta.annotation.Nonnull;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Explorable rule for matching explorables based on their name. The rule will match any explorables whose name contains
 * the specified match string.
 *
 * @since 1.48
 */
public class NameRule extends AExplorableRule implements INameRule
{
    public static final String TYPE = "Name Match";

    private static final String NAME_MATCH_KEY = "nameMatch";
    private final AtomicReference<String> matchString;

    public NameRule(String name)
    {
        this(name, "");
    }

    public NameRule(String name, String matchString)
    {
        super(name, TYPE);
        this.matchString = new AtomicReference<>(matchString.trim().toLowerCase());
    }

    @Override
    public boolean isMatch(IExplorable explorable)
    {
        String matchString = this.matchString.get();
        return explorable != null &&
                !explorable.isGroup() &&
                explorable.getDisplayName().toLowerCase().contains(matchString.toLowerCase());
    }

    @Override
    public void setMatchString(String matchString)
    {
        if (matchString == null)
        {
            return;
        }
        this.matchString.set(matchString.trim());
        fireRuleChanged();
    }

    @Override
    public String getMatchString()
    {
        return matchString.get();
    }

    @Override
    public void store(IRestorableMap map)
    {
        super.store(map);
        map.setString(NAME_MATCH_KEY, getMatchString());
    }

    @Override
    public void restore(IRestorableMap map)
    {
        super.restore(map);
        setMatchString(map.getString(NAME_MATCH_KEY, getMatchString()));
    }

    @Nonnull
    @Override
    public IExplorableRuleViewComponent createViewComponent()
    {
        return new NameRulePanel(this);
    }

    /**
     * Create an explorable rule factory for NameRule.
     *
     * @return explorable rule factory for NameRule.
     */
    public static IExplorableRuleFactory<INameRule> getFactory()
    {
        return new IExplorableRuleFactory<>()
        {
            @Override
            public INameRule createRule()
            {
                return new NameRule(getDisplayName());
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
