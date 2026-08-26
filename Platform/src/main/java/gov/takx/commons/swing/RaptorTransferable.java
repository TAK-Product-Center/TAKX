package gov.takx.commons.swing;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class RaptorTransferable implements Transferable
{
    private final BufferedImage image;
    protected final Map<DataFlavor, Object> objects = new HashMap<DataFlavor, Object>();

    public RaptorTransferable()
    {
        this(null);
    }

    public RaptorTransferable(BufferedImage image)
    {
        this.image = image;
    }

    public BufferedImage getBufferedImage()
    {
        return image;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors()
    {
        return objects.keySet().toArray(new DataFlavor[objects.size()]);
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
        return objects.containsKey(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
    {
        Object object = objects.get(flavor);

        if (object == null)
        {
            throw new UnsupportedFlavorException(flavor);
        }

        return object;
    }

    public void setTransferData(DataFlavor flavor, Object object)
    {
        objects.put(flavor, object);
    }
}
