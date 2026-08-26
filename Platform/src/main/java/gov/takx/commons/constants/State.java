package gov.takx.commons.constants;

import java.util.List;

/**
 */
public enum State
{
    Unknown, Inactive, Active, Warning, Serious, Critical;

    private static final State[] VALUES = values();

    public static State getHighestState(List<State> stateList)
    {
        State highestState = State.Unknown;
        for (State state : stateList)
        {
            if (state.ordinal() > highestState.ordinal())
            {
                highestState = state;
            }
        }
        return highestState;
    }

    public static State stringToState(String name)
    {
        if (name == null) return Unknown;

        for (State state : VALUES)
        {
            if (state.toString().equalsIgnoreCase(name))
            {
                return state;
            }
        }
        return Unknown;
    }
}
