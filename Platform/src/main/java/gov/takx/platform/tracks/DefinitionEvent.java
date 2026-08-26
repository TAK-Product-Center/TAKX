package gov.takx.platform.tracks;

import gov.takx.api.tracks.IConnectingLineDefinition;
import gov.takx.api.tracks.ISmartTrackManager;
import gov.takx.api.tracks.ITrackStyleDefinition;

import java.util.EventObject;

/**
 * Identifies when either a track style or a connecting line definition has been added or removed from the
 * {@link ISmartTrackManager}.
 *
 * @since 1.12
 */
public class DefinitionEvent extends EventObject
{
    public enum DefinitionType
    {
        TrackStyle,
        ConnectingLine
    }

    private final ITrackStyleDefinition trackStyleDefinition;
    private final IConnectingLineDefinition connectingLineDefinition;
    private final DefinitionType definitionType;

    public DefinitionEvent(Object source, ITrackStyleDefinition definition, DefinitionType definitionType)
    {
        super(source);
        this.definitionType = definitionType;
        trackStyleDefinition = definition;
        connectingLineDefinition = null;
    }

    public DefinitionEvent(Object source, IConnectingLineDefinition definition, DefinitionType definitionType)
    {
        super(source);
        this.definitionType = definitionType;
        connectingLineDefinition = definition;
        trackStyleDefinition = null;
    }

    public ITrackStyleDefinition getTrackStyleDefinition()
    {
        return trackStyleDefinition;
    }

    public IConnectingLineDefinition getConnectingLineDefinition()
    {
        return connectingLineDefinition;
    }

    public DefinitionType getDefinitionType()
    {
        return definitionType;
    }
}
