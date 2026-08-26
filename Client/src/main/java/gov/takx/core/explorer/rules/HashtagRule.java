package gov.takx.core.explorer.rules;

import gov.takx.api.explorer.AExplorableRule;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorableRuleFactory;
import gov.takx.api.explorer.IExplorableRuleViewComponent;
import gov.takx.api.state.IRestorableMap;
import gov.takx.core.explorer.controller.ExplorerHashtagController;
import gov.takx.core.view.gui.explorer.rules.HashtagRulePanel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Objects;

/**
 * A rule group rule that matches on hashtags. This currently relies heavily on logic from {@link ExplorerHashtagController} to control which
 * properties are relevant and how hashtags are detected.
 *
 * @since 5.8
 */
public class HashtagRule extends AExplorableRule
{
    public static final String TYPE = "Hashtag";
    private static final String HASHTAG_NAME_KEY = "hashtag";

    private String name;

    /**
     * Construct a new rule with no specified hashtag name.
     */
    public HashtagRule()
    {
        super(TYPE, TYPE);
    }

    /**
     * Construct a new rule that matches on the given hashtag name.
     *
     * @param name the hashtag name to match
     */
    public HashtagRule(@Nonnull String name)
    {
        this();
        setName(name);
    }

    /**
     * @param name the hashtag name to match on
     */
    public void setName(@Nonnull String name)
    {
        name = name.toLowerCase();
        if (!Objects.equals(this.name, name))
        {
            this.name = name;
            fireRuleChanged();
        }
    }

    /**
     * @return the hashtag name to match on
     */
    @Nullable
    public String getName()
    {
        return name;
    }

    @Override
    public boolean isMatch(IExplorable explorable)
    {
        return ExplorerHashtagController.containsHashtag(explorable, name);
    }

    @Nonnull
    @Override
    public IExplorableRuleViewComponent createViewComponent()
    {
        return new HashtagRulePanel(this);
    }

    @Nonnull
    @Override
    public Collection<String> getSensitiveProperties()
    {
        return ExplorerHashtagController.SUPPLIERS.keySet();
    }

    @Override
    public void store(IRestorableMap map)
    {
        super.store(map);
        map.setString(HASHTAG_NAME_KEY, name);
    }

    @Override
    public void restore(IRestorableMap map)
    {
        super.restore(map);
        setName(map.getString(HASHTAG_NAME_KEY, "INVALID_KEY"));
    }

    /**
     * @return explorable rule factory for HashtagRule.
     */
    public static IExplorableRuleFactory<HashtagRule> getFactory()
    {
        return new IExplorableRuleFactory<>()
        {
            @Override
            public HashtagRule createRule()
            {
                return new HashtagRule();
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
