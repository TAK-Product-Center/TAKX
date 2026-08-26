package gov.takx.core.globe.imagerywizard;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.util.FileTree;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 */
public class FileSetFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter
{
    @Override
    public boolean accept(File file)
    {
        return true;
    }

    @Override
    public String getDescription()
    {
        return "";
    }

    public Iterator<File> filterDirectory(File directory)
    {
        FileTree tree = new FileTree(directory);
        tree.setMode(FileTree.FILES_ONLY);
        return tree.iterator(this);
    }

    public List<FileSet> createFileSets(List<File> files)
    {
        // Create one FileSet for each file.
        return files.stream().map(this::createFileSet).collect(Collectors.toList());
    }

    protected FileSet createFileSet(File file)
    {
        FileSet fileSet = new FileSet();
        fileSet.setFiles(Collections.singletonList(file));

        Object o = FileSet.makeTitle(fileSet);
        fileSet.setValue(AVKey.TITLE, o);
        fileSet.setValue(AVKey.DISPLAY_NAME, o);

        o = FileSet.makePath(fileSet);
        fileSet.setValue(InstallConstants.PATH, o);

        return fileSet;
    }
}
