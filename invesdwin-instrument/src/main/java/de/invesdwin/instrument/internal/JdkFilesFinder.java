package de.invesdwin.instrument.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class JdkFilesFinder {

    private final Set<File> potentialFolders;

    public JdkFilesFinder() {
        //determine the java home via the system variables
        final Set<File> javaHomes = new LinkedHashSet<File>();
        //CHECKSTYLE:OFF
        for (final String javaHomeStr : new String[] { System.getenv("JAVA_HOME"), System.getProperty("java.home") }) {
            //CHECKSTYLE:ON
            if (!org.springframework.util.StringUtils.isEmpty(javaHomeStr)) {
                final File javaHome = new File(javaHomeStr);
                if (javaHome.exists()) {
                    javaHomes.add(javaHome);
                }
            }
        }
        //add parent directories on windows to get the jdk from the jre folder
        final List<File> potentialOtherJavaHomes = new ArrayList<File>();
        for (final File javaHome : new ArrayList<File>(javaHomes)) {
            if (javaHome.getAbsolutePath().contains("jre")) {
                for (final File dir : javaHome.getParentFile().listFiles()) {
                    potentialOtherJavaHomes.add(dir);
                }
            }
        }
        //sort file names descending to have the highest jdk version be searched first
        Collections.sort(potentialOtherJavaHomes, new Comparator<File>() {
            @Override
            public int compare(final File o1, final File o2) {
                return o1.getName().compareTo(o2.getName()) * -1;
            }
        });
        javaHomes.addAll(potentialOtherJavaHomes);
        //search for special subfolders that might contain the desired files
        this.potentialFolders = new LinkedHashSet<File>();
        for (final File javaHome : javaHomes) {
            if (!org.springframework.util.StringUtils.isEmpty(javaHome)) {
                for (final String folderName : new String[] { "bin", "lib" }) {
                    addPotentialFolderIfExists(new File(javaHome, folderName));
                    addPotentialFolderIfExists(new File(javaHome, "../" + folderName));
                    addPotentialFolderIfExists(new File(javaHome, "jre/" + folderName));
                    addPotentialFolderIfExists(new File(javaHome, "../jre/" + folderName));
                }
            }
        }
    }

    private void addPotentialFolderIfExists(final File e) {
        if (e.exists()) {
            potentialFolders.add(e);
        }
    }

    public File findToolsJar() {
        File toolsJar = null;
        final String potentialFileName = "tools.jar";
        for (final File dir : potentialFolders) {
            toolsJar = findFileRecursive(dir, potentialFileName);
            if (toolsJar != null) {
                break;
            }
        }
        assertFileFound(toolsJar, potentialFileName);
        return toolsJar;
    }

    private void assertFileFound(final File toolsJar, final Object potentialFileNames) {
        if (!toolsJar.exists()) {
            throw new IllegalStateException("No " + potentialFileNames + " found in " + potentialFolders
                    + ". Please make sure a JDK is installed and JAVA_HOME points there.");
        }
    }

    public File findAttachLib() {
        File attachLib = null;
        final List<String> potentialFileNames = Arrays.asList("attach.dll", "libattach.so", "libattach.dylib");
        OUTER: for (final File dir : potentialFolders) {
            for (final String attachLibFileName : potentialFileNames) {
                attachLib = findFileRecursive(dir, attachLibFileName);
                if (attachLib != null) {
                    break OUTER;
                }
            }
        }
        assertFileFound(attachLib, potentialFileNames);
        return attachLib;
    }

    private File findFileRecursive(final File rootDir, final String fileName) {
        if (!rootDir.exists()) {
            return null;
        }
        final File[] files = rootDir.listFiles();
        final List<File> directories = new ArrayList<File>(files.length);
        for (final File file : files) {
            if (file.getName().equals(fileName)) {
                return file;
            } else if (file.isDirectory()) {
                directories.add(file);
            }
        }

        for (final File directory : directories) {
            final File file = findFileRecursive(directory, fileName);
            if (file != null) {
                return file;
            }
        }

        return null;
    }

}
