package gov.takx.commons.media;

/**
 */
public enum AudioAlert
{
    ALERTS_ON("Alerts On"),//Play all audio
    ALERTS_OFF("Alerts Off"),//Mute
    CRITICAL_ONLY("Critical Only");//Only play critical alarms

    private static final AudioAlert[] VALUES = values();

    private final String name;

    AudioAlert(final String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return name;
    }

    public static AudioAlert getAudioAlert(final String value)
    {
        for (AudioAlert audioAlert : VALUES)
        {
            if (audioAlert.name.equals(value)) return audioAlert;
        }
        return null;
    }
}
