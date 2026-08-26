/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.takx.core.globe.imagerywizard;

import gov.nasa.worldwind.avlist.AVKey;
import gov.takx.commons.swing.SelectAllTableSupport;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.BorderFactory;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import java.awt.Color;

/**
 */
public class FileSetTable extends AVTable<FileSet>
{
    private static class Model extends AVModel<FileSet>
    {
        private static final long ONE_MEGABYTE = 1048576L;
        private static final String[] COLUMN_TITLES = {"Select", "Name", "Size (MB)", "Folder path"};
        private static final Class<?>[] COLUMN_TYPES = {Boolean.class, String.class, Long.class, String.class};
        private static final String[] COLUMN_ATTRIBUTES = {InstallConstants.SELECTED, AVKey.TITLE, InstallConstants.SIZE, InstallConstants.PATH};

        private final boolean supportSelection;

        /**
         * @param supportSelection Whether the model should include a column for selecting individual files
         */
        private Model(boolean supportSelection)
        {
            super(supportSelection ? 4 : 3,
                    supportSelection ? COLUMN_TITLES : ArrayUtils.subarray(COLUMN_TITLES, 1, COLUMN_TITLES.length),
                    supportSelection ? COLUMN_TYPES : ArrayUtils.subarray(COLUMN_TYPES, 1, COLUMN_TYPES.length),
                    supportSelection ? COLUMN_ATTRIBUTES : ArrayUtils.subarray(COLUMN_ATTRIBUTES, 1, COLUMN_ATTRIBUTES.length));
            this.supportSelection = supportSelection;
        }

        @Override
        protected Object getValue(FileSet values, String attribute)
        {
            //noinspection SwitchStatementWithoutDefaultBranch - no default so selected case will fall through when supportSelection is false
            switch (attribute)
            {
                case InstallConstants.SIZE ->
                {
                    double size = ((Number) values.getValue(attribute)).doubleValue();
                    return String.format("%5.1f", size / ONE_MEGABYTE);
                }
                case InstallConstants.SELECTED ->
                {
                    // When user selection isn't supported, all files should automatically be selected
                    if (!supportSelection) return true;
                }
            }

            return super.getValue(values, attribute);
        }

        @Override
        protected boolean isAttributeEditable(FileSet values, String attribute)
        {
            return switch (attribute)
            {
                case AVKey.TITLE -> true;
                case InstallConstants.SELECTED -> supportSelection;
                default -> false;
            };
        }
    }

    public FileSetTable()
    {
        this(true);
    }

    /**
     * @param supportSelection Whether the table should include a column for selecting individual files
     * @since 3.4.1
     */
    public FileSetTable(boolean supportSelection)
    {
        super(new Model(supportSelection));

        setShowGrid(true);
        setGridColor(Color.BLACK);
        setShowHorizontalLines(true);
        setShowVerticalLines(true);
        final TableColumnModel columnModel = getColumnModel();
        columnModel.setColumnMargin(5);

        setColumnSelectionAllowed(false);
        setRowSelectionAllowed(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        if (supportSelection)
        {
            final TableColumn selectedColumn = columnModel.getColumn(0);
            selectedColumn.setMinWidth(40);
            selectedColumn.setPreferredWidth(40);
            selectedColumn.setMaxWidth(40);
            SelectAllTableSupport.configureSelectAllColumn(this, model, 0, true);
        }
    }
}
