package gov.takx.api.explorer;

import gov.takx.api.state.IRestorableMap;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract base class for explorable rules.
 *
 * @since 1.48
 */
public abstract class AExplorableRule extends AExplorerEventSupport implements IExplorableRule
{
    static final String DISPLAY_NAME_KEY = "displayName";
    static final String ENABLED_KEY = "enabled";
    static final String RULE_TYPE_KEY = "ruleType";

    protected final String type;
    protected final AtomicBoolean enabled;
    protected final AtomicReference<String> displayName;

    public AExplorableRule(String name, String type)
    {
        enabled = new AtomicBoolean(true);
        this.type = type;
        displayName = new AtomicReference<>(name);
    }

    @Override
    public void setDisplayName(String displayName)
    {
        String old = this.displayName.getAndSet(displayName);
        if (ExplorerUtils.valueChanged(displayName, old))
        {
            firePropertyChange(NAME, old, displayName);
        }
    }

    @Override
    public String getDisplayName()
    {
        return displayName.get();
    }

    @Override
    public String getType()
    {
        return type;
    }

    @Override
    public boolean isEnabled()
    {
        return enabled.get();
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        boolean old = this.enabled.getAndSet(enabled);
        if (enabled != old)
        {
            firePropertyChange(ENABLED, old, enabled);
        }
    }

    @Override
    public void store(IRestorableMap map)
    {
        map.setString(DISPLAY_NAME_KEY, getDisplayName());
        map.setString(RULE_TYPE_KEY, getType());
        map.setBoolean(ENABLED_KEY, isEnabled());
    }

    @Override
    public void restore(IRestorableMap map)
    {
        setDisplayName(map.getString(DISPLAY_NAME_KEY, getDisplayName()));
        setEnabled((map.getBoolean(ENABLED_KEY, isEnabled())));
    }

    protected void fireRuleChanged()
    {
        pcs.firePropertyChange(RULE_CHANGED, null, this);
    }
}
