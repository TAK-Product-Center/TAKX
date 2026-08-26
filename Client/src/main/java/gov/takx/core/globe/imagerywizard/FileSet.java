/*
Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.takx.core.globe.imagerywizard;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVListImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A collection of files which may have associated values for the following keys:
 * <ul>
 *     <li>{@link AVKey#DISPLAY_NAME}</li>
 *     <li>{@link InstallConstants#PATH}</li>
 *     <li>{@link InstallConstants#SELECTED}</li>
 *     <li>{@link InstallConstants#SIZE}</li>
 *     <li>{@link AVKey#TITLE}</li>
 * </ul>
 *
 */
public class FileSet extends AVListImpl
{
    private List<File> files = new ArrayList<File>();

    public FileSet()
    {
    }

    public List<File> getFiles()
    {
        return Collections.unmodifiableList(this.files);
    }

    public void setFiles(Collection<File> files)
    {
        this.files.clear();
        this.files.addAll(files);

        long size = this.computeSizeInBytes();
        this.setValue(InstallConstants.SIZE, size);
    }

    protected long computeSizeInBytes()
    {
        long sizeInBytes = 0;
        for (File f : this.files) sizeInBytes += f.length();
        return sizeInBytes;
    }

    public static String makeTitle(FileSet fileSet)
    {
        List<File> files = fileSet.getFiles();
        if (files == null || files.isEmpty()) return null;
        return files.get(0).getName();
    }

    public static String makePath(FileSet fileSet)
    {
        List<File> files = fileSet.getFiles();
        if (files == null || files.isEmpty()) return null;
        List<File> commonElements = new ArrayList<>();
        List<File> elements = new ArrayList<>();
        int count = 0;

        getPathElements(files.get(0), commonElements);
        for (File f : files)
        {
            getPathElements(f, elements);
            count = countCommonElements(elements, commonElements);
            if (count < 1) return null;
            if (count < commonElements.size()) commonElements.subList(count, commonElements.size()).clear();
        }

        if (count < 1) return null;
        return commonElements.get(count - 1).getPath();
    }

    private static void getPathElements(File file, List<File> elements)
    {
        elements.clear();
        while ((file = file.getParentFile()) != null) elements.add(0, file);
    }

    private static int countCommonElements(List<File> list1, List<File> list2)
    {
        int i = 0;
        int length = Math.min(list1.size(), list2.size());
        while (i < length && list1.get(i).equals(list2.get(i))) i++;
        return i;
    }
}
