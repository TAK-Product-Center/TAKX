package gov.takx.platform.ui.devicelog;

import gov.takx.api.messages.IStructure;
import gov.takx.platform.schema.layout.ColumnType;
import gov.takx.platform.schema.layout.LayoutDefinitionType;
import gov.takx.platform.schema.layout.LayoutType;
import gov.takx.platform.schema.layout.LayoutsDefinitionsCollection;
import gov.takx.platform.schema.layout.ObjectFactory;
import gov.takx.platform.schema.layout.ScopeType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.table.TableColumnExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.TableColumn;
import javax.xml.transform.stream.StreamSource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * This class handles the table layout templates for the device log.  The template stores the column layout
 * for the table, including order, width, and visibility.  This allows the table to restore the prior visual
 * layout the user setup whenever a given structure is loaded into the table.
 *
 * @since 1.49
 */
public class TableLayoutManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String storageDir;

    /**
     * Constructor.
     *
     * @param storageDir The directory in which to store the XML layout files
     */
    public TableLayoutManager(String storageDir)
    {
        this.storageDir = storageDir;
    }

    /**
     * Update the columns to reflect what is stored in the xml layout file for the given structure.
     *
     * @param structure structure represented in the table
     */
    public void updateTableFromTemplate(JXTable rdmTable, IStructure structure)
    {
        if (structure == null) return;

        // Catch the exception here in case the template file is corrupted (from hand editing), or if the
        // ordering in the structure has changed without updating the template file (more likely).
        try
        {
            final File templateFile = getTemplateFile(structure);
            logger.debug("Load column layout: {}", templateFile);

            final LayoutsDefinitionsCollection collection = getColumnStructure(templateFile);

            if (collection != null)
            {
                LayoutType layoutType = collection.getLayoutDefinition().get(0).getLayout();   // There's only going to be 1...
                List<ColumnType> columnTypeList = layoutType.getColumn();

                // First pass, set visibility and widths
                for (ColumnType column : columnTypeList)
                {
                    TableColumnExt columnExt = rdmTable.getColumnExt(column.getName());
                    if (columnExt != null)  // sanity check
                    {
                        columnExt.setPreferredWidth(column.getWidth());
                        columnExt.setVisible(column.isVisible());
                    }
                }

                // Second pass, move them to the correct index / order.
                for (ColumnType column : columnTypeList)
                {
                    if (column.isVisible())
                    {
                        TableColumnExt columnExt = rdmTable.getColumnExt(column.getName());
                        if (columnExt != null)  // sanity check
                        {
                            int from = rdmTable.convertColumnIndexToView(columnExt.getModelIndex());
                            // We actually have to check to see if "from" is not equal to -1, which can happen
                            // on structures that define custom field names that are the same as a core field name.
                            if (from != column.getPosition() && from != -1)
                            {
                                rdmTable.moveColumn(from, column.getPosition());
                            }
                        }
                    }
                }
            }
        } catch (Exception e)
        {
            logger.warn("Error updating column headers from template file.  May be out of date for structure {}", structure.getMessageType());
        }
    }

    /**
     * Get the column structure through JAXB binding to the column layout schema classes.
     *
     * @param templateFile file to query for the column settings
     * @return column layout
     */
    private static LayoutsDefinitionsCollection getColumnStructure(File templateFile)
    {
        try
        {
            InputStream is = new BufferedInputStream(new FileInputStream(templateFile));

            JAXBContext context = JAXBContext.newInstance(LayoutsDefinitionsCollection.class.getPackage().getName());
            Unmarshaller unmarshaller = context.createUnmarshaller();

            JAXBElement<LayoutsDefinitionsCollection> root =
                    unmarshaller.unmarshal(new StreamSource(is), LayoutsDefinitionsCollection.class);

            return root.getValue();
        } catch (FileNotFoundException ignored)
        {
            // The first time, this file won't exist.  Don't worry about it... eat the exception.
        } catch (JAXBException e)
        {
            logger.error("JAXB Exception loading layouts definition collection from {}", templateFile, e);
        }
        return null;
    }

    /**
     * Save the structure of the columns, including visibility and width to an xml file
     *
     * @param structure structure represented in the table
     */
    public void saveColumnStructure(JXTable rdmTable, IStructure structure)
    {
        try
        {
            JAXBContext context = JAXBContext.newInstance(LayoutsDefinitionsCollection.class.getPackage().getName());
            Marshaller marshaller = context.createMarshaller();

            ObjectFactory factory = new ObjectFactory();
            LayoutsDefinitionsCollection collection = factory.createLayoutsDefinitionsCollection();

            LayoutDefinitionType definitionType = factory.createLayoutDefinitionType();
            definitionType.setContext("Device Log Table");
            definitionType.setScope(ScopeType.PROJECT);

            LayoutType layoutType = factory.createLayoutType();

            // First do the visible columns which are in their view order
            short index = 0;
            final List<TableColumn> visibleColumns = rdmTable.getColumns(false);

            for (TableColumn visibleTableColumn : visibleColumns)
            {
                ColumnType col = factory.createColumnType();
                col.setName((String) visibleTableColumn.getHeaderValue());
                col.setVisible(true);
                col.setPosition(index++);
                col.setWidth((short) visibleTableColumn.getPreferredWidth());
                layoutType.getColumn().add(col);
            }

            // Add the invisible columns and use their preferred width in the settings.
            List<TableColumn> allColumns = rdmTable.getColumns(true);
            for (TableColumn column : allColumns)
            {
                if (!((TableColumnExt) column).isVisible())
                {
                    ColumnType col = factory.createColumnType();
                    col.setName((String) column.getHeaderValue());
                    col.setWidth((short) column.getPreferredWidth());
                    col.setVisible(false);
                    layoutType.getColumn().add(col);
                }
            }

            // Save to XML through the JAXB binding, see gov.takx.platform.schema.layout
            definitionType.setLayout(layoutType);
            collection.getLayoutDefinition().add(definitionType);

            File templateFile = getTemplateFile(structure);

            logger.debug("Save column layout: {}", templateFile);

            try (var outputStream = new FileOutputStream(templateFile))
            {
                marshaller.marshal(factory.createLayoutDefinitions(collection), outputStream);
            } catch (IOException e)
            {
                logger.error("Failed to write device log column structure for {} to: {}", structure.getMessageType(), templateFile, e);
            }
        } catch (JAXBException e)
        {
            logger.error("Error saving device log column structure to VFS for structure: {}", structure.getMessageType(), e);
        }
    }

    /**
     * Get the File for the XML storage for the template of the given structure.
     *
     * @param structure Structure of interest
     * @return File that should contain the XML layout
     */
    private File getTemplateFile(IStructure structure)
    {
        return new File(storageDir, structure.getMessageType() + "_" + structure.getHashValue() + ".xml");
    }
}
