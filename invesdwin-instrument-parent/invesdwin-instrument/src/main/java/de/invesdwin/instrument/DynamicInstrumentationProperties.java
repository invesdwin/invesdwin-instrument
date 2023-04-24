package de.invesdwin.instrument;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.Immutable;

@Immutable
public final class DynamicInstrumentationProperties {

    public static final File SYSTEM_TEMP_DIRECTORY;
    /**
     * Process specific temp dir that gets cleaned on exit.
     */
    public static final File USER_TEMP_DIRECTORY;
    public static final File PROCESS_TEMP_DIRECTORY;
    public static final String USER_TEMP_DIRECTORY_NAME;
    private static Runnable deleteTempDirectoryRunner = new Runnable() {
        @Override
        public void run() {
            org.apache.commons.io.FileUtils.deleteQuietly(PROCESS_TEMP_DIRECTORY);
        }
    };

    static {
        //CHECKSTYLE:OFF
        String systemTempDir = System
                .getProperty(DynamicInstrumentationProperties.class.getName() + ".TEMP_DIRECTORY_OVERRIDE");
        if (systemTempDir == null) {
            systemTempDir = System.getProperty("java.io.tmpdir");
        }
        SYSTEM_TEMP_DIRECTORY = new File(systemTempDir);
        final String username = System.getProperty("user.name");
        //CHECKSTYLE:ON
        /*
         * we can not use a shared "invesdwin" folder for all users because permissions would hinder one user from
         * creating another folder in the invesdwin folder, so use a flat structure instead
         */
        USER_TEMP_DIRECTORY_NAME = "invesdwin_temp_" + username.replaceAll("[^a-zA-Z0-9]", "");
        USER_TEMP_DIRECTORY = new File(SYSTEM_TEMP_DIRECTORY, USER_TEMP_DIRECTORY_NAME);
        PROCESS_TEMP_DIRECTORY = newProcessTempDirectory(USER_TEMP_DIRECTORY);
    }

    private DynamicInstrumentationProperties() {}

    public static File newProcessTempDirectory(final File baseDirectory) {
        final File tempDir = findEmptyTempDir(baseDirectory);
        forceMkdirRetry(baseDirectory, tempDir);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                deleteTempDirectoryRunner.run();
            }
        });
        return tempDir;
    }

    private static void forceMkdirRetry(final File baseDirectory, final File tempDir) {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final int maxTries = 3;
        IOException lastException = null;
        for (int tries = 0; tries < maxTries; tries++) {
            try {
                lastException = null;
                org.apache.commons.io.FileUtils.forceMkdir(tempDir);
                baseDirectory.setWritable(true, false);
            } catch (final IOException e) {
                lastException = e;
                try {
                    TimeUnit.MILLISECONDS.sleep(random.nextLong(100));
                } catch (final InterruptedException e1) {
                    throw new RuntimeException(e1);
                }
            }
        }
        if (lastException != null) {
            throw new RuntimeException(lastException);
        }
    }

    public static void setDeleteTempDirectoryRunner(final Runnable deleteTempDirectoryRunner) {
        if (deleteTempDirectoryRunner == null) {
            throw new NullPointerException();
        }
        DynamicInstrumentationProperties.deleteTempDirectoryRunner = deleteTempDirectoryRunner;
    }

    private static File findEmptyTempDir(final File baseDirectory) {
        File tempDir = new File(baseDirectory, ManagementFactory.getRuntimeMXBean().getName());
        int retry = 0;
        while (tempDir.exists() && !org.apache.commons.io.FileUtils.deleteQuietly(tempDir)) {
            //no permission to delete folder (maybe different user had this pid before), choose a different one
            tempDir = new File(tempDir.getAbsolutePath() + "_" + retry);
            retry++;
        }
        return tempDir;
    }

    public static String getProcessId() {
        final String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
        final String pid = nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'));
        return pid;
    }

}
