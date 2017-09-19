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
        if (dirOrJar == null) {
            return;
        }
        try {
            final String normalizedPath = FilenameUtils.normalize(dirOrJar.getAbsolutePath());
            org.assertj.core.api.Assertions.assertThat(pathsAddedToSystemClassLoader.add(normalizedPath))
                    .as("Path [%s] has already been added before!", normalizedPath)
                    .isTrue();
            final Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            final URL url = new File(normalizedPath).toURI().toURL();
            method.invoke(getSystemClassLoader(), url);
        } catch (final NoSuchMethodException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        } catch (final SecurityException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        } catch (final IllegalAccessException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        } catch (final IllegalArgumentException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        } catch (final MalformedURLException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        } catch (final InvocationTargetException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        }
    }

    public static ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public static ClassLoader getSystemClassLoader() {
        return ClassLoader.getSystemClassLoader();
    }

    public static Set<String> getPathsAddedToSystemClassLoader() {
        return Collections.unmodifiableSet(pathsAddedToSystemClassLoader);
    }

    /**
     * http://stackoverflow.com/questions/11134159/how-to-load-attachprovider-attach-dll-dynamically
     */
    public static void addPathToJavaLibraryPath(final File dir) {
        if (dir == null) {
            return;
        }
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
        } catch (final NoSuchFieldException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        } catch (final SecurityException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        } catch (final IllegalArgumentException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        } catch (final IllegalAccessException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        }
    }

}
