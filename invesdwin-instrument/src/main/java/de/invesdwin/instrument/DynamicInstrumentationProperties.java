package de.invesdwin.instrument;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.annotation.concurrent.Immutable;

import org.apache.commons.io.FileUtils;

@Immutable
public final class DynamicInstrumentationProperties {

    /**
     * Process specific temp dir that gets cleaned on exit.
     */
    public static final File TEMP_DIRECTORY;

    static {
        TEMP_DIRECTORY = getTempDirectory();
    }

    private DynamicInstrumentationProperties() {}

    private static File getTempDirectory() {
        //CHECKSTYLE:OFF
        final String systemTempDir = System.getProperty("java.io.tmpdir");
        //CHECKSTYLE:ON
        final File tempDir = new File(systemTempDir, ManagementFactory.getRuntimeMXBean().getName());
        FileUtils.deleteQuietly(tempDir);
        try {
            FileUtils.forceMkdir(tempDir);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                FileUtils.deleteQuietly(tempDir);
            }
        });
        return tempDir;
    }

}
