package gov.takx.core.media;

import gov.takx.api.media.IDecodingOptions;

/**
 * Options to control which streams will be decoded
 *
 * @since 2.8
 */
public class DecodingOptions implements IDecodingOptions
{
    private boolean decodeVideo = true;
    private boolean decodeAudio = true;
    private boolean decodeData = true;

    public DecodingOptions()
    {

    }

    public DecodingOptions(IDecodingOptions options)
    {
        decodeVideo = options.isDecodeVideo();
        decodeAudio = options.isDecodeAudio();
        decodeData = options.isDecodeData();
    }

    @Override
    public boolean isDecodeVideo()
    {
        return decodeVideo;
    }

    @Override
    public void setDecodeVideo(boolean decodeVideo)
    {
        this.decodeVideo = decodeVideo;
    }

    @Override
    public boolean isDecodeAudio()
    {
        return decodeAudio;
    }

    @Override
    public void setDecodeAudio(boolean decodeAudio)
    {
        this.decodeAudio = decodeAudio;
    }

    @Override
    public boolean isDecodeData()
    {
        return decodeData;
    }

    @Override
    public void setDecodeData(boolean decodeData)
    {
        this.decodeData = decodeData;
    }
}
