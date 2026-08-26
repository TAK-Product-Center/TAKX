package gov.takx.core.view.gui.cachecleaner;

import gov.nasa.worldwindx.examples.util.FileStoreDataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class CacheCleanerTask
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private BackgroundCleaner cleaner;

    /**
     * Constructor
     *
     * @param cleanInfo list of data sets along with their clean-after month value
     * @param listener  a listener to be notified when the task has begun and completed.
     */
    public CacheCleanerTask(List<DataSetCleanInfo> cleanInfo, CacheCleanerTaskListener listener)
    {
        cleaner = new BackgroundCleaner(cleanInfo, listener);
    }

    /**
     * Start cleaning up the caches if the cleaner is not currently alive.
     */
    public void beginClean()
    {
        if (!cleaner.isAlive())
        {
            cleaner.start();
        }
    }

    /**
     * Background thread which iterates through the list of data sets and cleans out the old/unwanted data
     */
    private static class BackgroundCleaner extends Thread
    {
        private List<DataSetCleanInfo> cleanInfo;
        private CacheCleanerTaskListener listener;

        public BackgroundCleaner(List<DataSetCleanInfo> cleanInfoList, CacheCleanerTaskListener listener)
        {
            this.listener = listener;

            //filter out any data sets that may not currently exist (were already cleaned/removed)
            this.cleanInfo = new ArrayList<DataSetCleanInfo>();
            for (DataSetCleanInfo info : cleanInfoList)
            {
                File f = new File(info.dataSet.getPath());
                if (f.exists())
                {
                    this.cleanInfo.add(info);
                }
            }
        }

        public void run()
        {
            listener.cacheCleanerTaskStarted();

            long totalSize = 0;
            long elapsedSize = 0;

            try
            {
                for (DataSetCleanInfo dsc : cleanInfo)
                {
                    if (dsc.months > 0)
                    {
                        totalSize += dsc.dataSet.getOutOfScopeSize(FileStoreDataSet.MONTH, dsc.months);
                    }

                    if (dsc.months < 0 || dsc.dataSet.getSize() == 0)
                    {
                        totalSize += dsc.dataSet.getSize();
                    }
                }

                for (DataSetCleanInfo dsc : cleanInfo)
                {
                    if (dsc.months > 0) // delete only dated files
                    {
                        dsc.dataSet.deleteOutOfScopeFiles(FileStoreDataSet.MONTH, dsc.months, false);
                        elapsedSize += dsc.dataSet.getOutOfScopeSize(FileStoreDataSet.MONTH, dsc.months);
                    }

                    if (dsc.months < 0 || dsc.dataSet.getSize() == 0) // delete all files, or data set now empty
                    {
                        dsc.dataSet.delete(false); // removes all data plus the root folder
                        elapsedSize += dsc.dataSet.getSize();
                    }

                    if (logger.isDebugEnabled())
                    {
                        logger.debug("cache cleaner completed: {} of {}", (int) (elapsedSize / 1e6), (int) (totalSize / 1e6));
                    }
                }

                if (logger.isDebugEnabled())
                {
                    String sizeString = String.format("%5.1f", ((float) totalSize) / 1e6);
                    logger.debug("Cleaned {} datasets, {} MB", cleanInfo.size(), sizeString);
                }
                listener.cacheCleanerTaskCompleted(true);
            } catch (Exception e)
            {
                logger.error("could not complete cache cleaning.", e);
                listener.cacheCleanerTaskCompleted(false);
            }
        }
    }

    /**
     * A wrapper class to hold both the data set and its clean-after months value.
     */
    public static class DataSetCleanInfo
    {
        public final FileStoreDataSet dataSet;
        public final int months;

        public DataSetCleanInfo(FileStoreDataSet dataSet, int months)
        {
            this.dataSet = dataSet;
            this.months = months;
        }
    }
}
