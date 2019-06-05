package de.invesdwin.instrument;

import java.io.File;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import javax.annotation.concurrent.Immutable;

import org.apache.commons.io.FilenameUtils;
import org.springframework.instrument.InstrumentationSavingAgent;

@Immutable
public final class DynamicInstrumentationReflections {

    private static Set<String> pathsAddedToSystemClassLoader = Collections.synchronizedSet(new HashSet<String>());

    private DynamicInstrumentationReflections() {}

    @SuppressWarnings({ "restriction", "unchecked" })
    public static URL[] getURLs(final ClassLoader classLoader) {
        if (classLoader instanceof URLClassLoader) {
            return ((URLClassLoader) classLoader).getURLs();
        }

        // jdk9 or above
        try {
            //normal way: --add-opens java.base/jdk.internal.loader=ALL-UNNAMED
            //hacker way: https://javax0.wordpress.com/2017/05/03/hacking-the-integercache-in-java-9/
            final sun.misc.Unsafe unsafe = getUnsafe();
            //jdk.internal.loader.ClassLoaders.AppClassLoader.ucp
            final Field ucpField = classLoader.getClass().getDeclaredField("ucp");
            final Object ucp = unsafe.getObject(classLoader, unsafe.objectFieldOffset(ucpField));

            //jdk.internal.loader.URLClassPath.addUrl(...)
            synchronized (ucp) {
                final Field pathField = ucp.getClass().getDeclaredField("path");
                final ArrayList<URL> path = (ArrayList<URL>) unsafe.getObject(ucp, unsafe.objectFieldOffset(pathField));
                return path.toArray(new URL[path.size()]);
            }
        } catch (final NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

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
            final URL url = new File(normalizedPath).toURI().toURL();
            if (isBeforeJava9()) {
                addUrlToURLClassLoader(url);
            } else {
                //we are in java 9 or above
                addUrlToAppClassLoaderURLClassPath(url);
            }
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
        } catch (final NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isBeforeJava9() {
        return getSystemClassLoader() instanceof URLClassLoader;
    }

    private static void addUrlToURLClassLoader(final URL url)
            throws NoSuchMethodException, MalformedURLException, IllegalAccessException, InvocationTargetException {
        final ClassLoader systemClassLoader = getSystemClassLoader();
        final Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        method.setAccessible(true);
        method.invoke(systemClassLoader, url);
    }

    @SuppressWarnings({ "unchecked", "restriction" })
    private static void addUrlToAppClassLoaderURLClassPath(final URL url)
            throws NoSuchFieldException, SecurityException {
        final ClassLoader systemClassLoader = getSystemClassLoader();
        //normal way: --add-opens java.base/jdk.internal.loader=ALL-UNNAMED
        //hacker way: https://javax0.wordpress.com/2017/05/03/hacking-the-integercache-in-java-9/
        final sun.misc.Unsafe unsafe = getUnsafe();
        //jdk.internal.loader.ClassLoaders.AppClassLoader.ucp
        final Field ucpField = systemClassLoader.getClass().getDeclaredField("ucp");
        final Object ucp = unsafe.getObject(systemClassLoader, unsafe.objectFieldOffset(ucpField));

        //jdk.internal.loader.URLClassPath.addUrl(...)
        synchronized (ucp) {
            final Field pathField = ucp.getClass().getDeclaredField("path");
            final ArrayList<URL> path = (ArrayList<URL>) unsafe.getObject(ucp, unsafe.objectFieldOffset(pathField));

            try {
                //java 11 and up
                final Field urlsField = ucp.getClass().getDeclaredField("unopenedUrls");
                final ArrayDeque<URL> urls = (ArrayDeque<URL>) unsafe.getObject(ucp,
                        unsafe.objectFieldOffset(urlsField));
                synchronized (urls) {
                    if (url == null || path.contains(url)) {
                        return;
                    }
                    urls.addFirst(url);
                    path.add(url);
                }
            } catch (final NoSuchFieldException e) {
                //java 9 and 10
                final Field urlsField = ucp.getClass().getDeclaredField("urls");
                final Stack<URL> urls = (Stack<URL>) unsafe.getObject(ucp, unsafe.objectFieldOffset(urlsField));
                synchronized (urls) {
                    if (url == null || path.contains(url)) {
                        return;
                    }
                    urls.add(0, url);
                    path.add(url);
                }
            }
        }
    }

    @SuppressWarnings("restriction")
    public static sun.misc.Unsafe getUnsafe() {
        try {
            final Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            final sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            return unsafe;
        } catch (final Throwable e) {
            throw new RuntimeException(e);
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
            /*
             * ignore, happens on ibm-jdk-8 and adoptopenjdk-8-openj9 but has no influence since there is no cached
             * field that needs a reset
             */
        } catch (final SecurityException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        } catch (final IllegalArgumentException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        } catch (final IllegalAccessException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        }
    }

    public static InputStream getClassInputStream(final Class<?> clazz) throws ClassNotFoundException {
        final String name = "/" + clazz.getName().replace(".", "/") + ".class";
        final InputStream classIn = clazz.getResourceAsStream(name);
        if (classIn == null) {
            throw new NullPointerException("resource input stream should not be null: " + name);
        }
        return classIn;
    }

    public static Instrumentation getInstrumentation() {
        return InstrumentationSavingAgent.getInstrumentation();
    }

}
