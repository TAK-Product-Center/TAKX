package gov.takx.core.view.gui.preferences;

import gov.takx.commons.swing.ImageFileChooserFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.TransferHandler;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.InvalidDnDOperationException;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 */
public class GlyphSelectionPanelTransferHandler extends TransferHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private GlyphSelectionPanel glyphSelectionPanel;
    private ImageFileChooserFilter fileFilter;

    public GlyphSelectionPanelTransferHandler(GlyphSelectionPanel glyphSelectionPanel, ImageFileChooserFilter fileFilter)
    {
        this.glyphSelectionPanel = glyphSelectionPanel;
        this.fileFilter = fileFilter;
    }

    public boolean canImport(TransferHandler.TransferSupport support)
    {
        if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
        {
            try
            {
                for (File file : (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor))
                {
                    if (file.isDirectory() || !isFileValid(file))
                    {
                        return false;
                    }
                }
            } catch (InvalidDnDOperationException exception)
            {
                return true;
            } catch (UnsupportedFlavorException exception)
            {
                logger.error("Error:", exception);
            } catch (IOException exception)
            {
                logger.error("Error:", exception);
            }

            return true;
        }

        return false;
    }

    private boolean isFileValid(File f)
    {
        return fileFilter.accept(f);
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support)
    {
        if (canImport(support))
        {
            try
            {
                List<File> fileList = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                for (File file : fileList)
                {
                    glyphSelectionPanel.addImage(file.getPath());
                }
                return true;
            } catch (Exception exception)
            {
                logger.error("Error:", exception);
            }
        }
        return false;
    }
}
