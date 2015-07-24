package de.invesdwin.instrument;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import org.apache.commons.io.FilenameUtils;

@Immutable
public final class DynamicInstrumentationReflections {

    private static Set<String> pathsAddedToSystemClassLoader = Collections.synchronizedSet(new HashSet<String>());

    private DynamicInstrumentationReflections() {}

    /**
     * http://stackoverflow.com/questions/1010919/adding-files-to-java-classpath-at-runtime
     */
    public static void addPathToSystemClassLoader(final File dirOrJar) {
        try {
            final String normalizedPath = FilenameUtils.normalize(dirOrJar.getAbsolutePath());
            org.assertj.core.api.Assertions.assertThat(pathsAddedToSystemClassLoader.add(normalizedPath))
                    .as("Path [%s] has already been added before!", normalizedPath)
                    .isTrue();
            final Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            final URL url = new File(normalizedPath).toURI().toURL();
            method.invoke(ClassLoader.getSystemClassLoader(), url);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | MalformedURLException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        }
    }

    public static Set<String> getPathsAddedToSystemClassLoader() {
        return Collections.unmodifiableSet(pathsAddedToSystemClassLoader);
    }

    /**
     * http://stackoverflow.com/questions/11134159/how-to-load-attachprovider-attach-dll-dynamically
     */
    public static void addPathToJavaLibraryPath(final File dir) {
        try {
            final String javaLibraryPathKey = "java.library.path";
            //CHECKSTYLE:OFF
            final String existingJavaLibraryPath = System.getProperty(javaLibraryPathKey);
            //CHECKSTYLE:ON
            final String newJavaLibraryPath;
            if (!org.springframework.util.StringUtils.isEmpty(existingJavaLibraryPath)) {
                newJavaLibraryPath = existingJavaLibraryPath + File.pathSeparator + dir.getAbsolutePath();
            } else {
                newJavaLibraryPath = dir.getAbsolutePath();
            }
            //CHECKSTYLE:OFF
            System.setProperty(javaLibraryPathKey, newJavaLibraryPath);
            //CHECKSTYLE:ON
            final Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
            fieldSysPath.setAccessible(true);
            fieldSysPath.set(ClassLoader.class, null);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        }
    }
}
