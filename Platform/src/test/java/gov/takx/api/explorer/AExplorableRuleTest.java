package gov.takx.api.explorer;

import org.junit.jupiter.api.Test;

import static gov.takx.test.utils.ExplorerTestConstants.GROUP_1;
import static gov.takx.test.utils.ExplorerTestConstants.NAME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class AExplorableRuleTest
{
    @Test
    public void basicAExplorableRuleTest()
    {
        IExplorableRule rule = createDefaultRule();
        assertThat(rule.getDisplayName()).isEqualTo(NAME);
        assertThat(rule.getType()).isEqualTo(GROUP_1);

        rule.setEnabled(false);
        assertThat(rule.isEnabled()).isFalse();
        rule.setEnabled(true);
        assertThat(rule.isEnabled()).isTrue();
    }

    protected IExplorableRule createDefaultRule()
    {
        return new AExplorableRule(NAME, GROUP_1)
        {
            @Override
            public boolean isMatch(IExplorable explorable)
            {
                return false;
            }

            @Override
            public IExplorableRuleViewComponent createViewComponent()
            {
                return null;
            }
        };
    }
}
