package gov.takx.api.messages;

/**
 * Test Field processor that ignores any object and returns a constant string for testing.
 *
 */
public class TestFieldDisplayProcessor implements IFieldDisplayProcessor
{
    @Override
    public String getDisplayString(Object fieldValue)
    {
        return "SugarMagnolia";
    }
}
