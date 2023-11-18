package de.invesdwin.instrument;

import java.io.File;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import javax.annotation.concurrent.Immutable;

import org.apache.commons.io.FilenameUtils;
import org.springframework.instrument.InstrumentationSavingAgent;

@Immutable
public final class DynamicInstrumentationReflections {

    private static final URL[] EMPTY_URLS = new URL[0];
    private static Set<String> pathsAddedToSystemClassLoader = java.util.Collections
            .synchronizedSet(new HashSet<String>());

    private DynamicInstrumentationReflections() {}

    @SuppressWarnings({ "restriction", "unchecked" })
    public static URL[] getURLs(final ClassLoader classLoader) {
        if (classLoader == null) {
            throw new NullPointerException("classLoader should not be null");
        }

        if (classLoader instanceof URLClassLoader) {
            //java 8 and below
            return ((URLClassLoader) classLoader).getURLs();
        }

        // jdk9 or above
        try {
            //normal way: --add-opens java.base/jdk.internal.loader=ALL-UNNAMED
            //hacker way: https://javax0.wordpress.com/2017/05/03/hacking-the-integercache-in-java-9/
            final sun.misc.Unsafe unsafe = getUnsafe();
            //jdk.internal.loader.ClassLoaders.AppClassLoader.ucp
            final Field ucpField = getField(classLoader.getClass(), "ucp");
            final Object ucp = unsafe.getObject(classLoader, unsafe.objectFieldOffset(ucpField));

            //jdk.internal.loader.URLClassPath.addUrl(...)
            synchronized (ucp) {
                final Field pathField = ucp.getClass().getDeclaredField("path");
                final ArrayList<URL> path = (ArrayList<URL>) unsafe.getObject(ucp, unsafe.objectFieldOffset(pathField));
                return path.toArray(EMPTY_URLS);
            }
        } catch (final NoSuchFieldException e) {
            throw new RuntimeException(classLoader.getClass().getName(), e);
        }
    }

    /**
     * http://stackoverflow.com/questions/1010919/adding-files-to-java-classpath-at-runtime
     */
    public static void addPathToSystemClassLoader(final File dirOrJar) {
        if (dirOrJar == null) {
            return;
        }
        final ClassLoader systemClassLoader = getSystemClassLoader();
        try {
            final String normalizedPath = FilenameUtils.normalize(dirOrJar.getAbsolutePath());
            if (!pathsAddedToSystemClassLoader.add(normalizedPath)) {
                throw new IllegalStateException("Path [" + normalizedPath + "] has already been added before!");
            }
            final URL url = new File(normalizedPath).toURI().toURL();
            if (isBeforeJava9()) {
                addUrlToURLClassLoader(systemClassLoader, url);
            } else {
                //we are in java 9 or above
                addUrlToAppClassLoaderURLClassPath(systemClassLoader, url);
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
            throw new RuntimeException(systemClassLoader.getClass().getName(), e);
        }
    }

    public static boolean isBeforeJava9() {
        return getSystemClassLoader() instanceof URLClassLoader;
    }

    private static void addUrlToURLClassLoader(final ClassLoader systemClassLoader, final URL url)
            throws NoSuchMethodException, MalformedURLException, IllegalAccessException, InvocationTargetException {
        final Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        method.setAccessible(true);
        method.invoke(systemClassLoader, url);
    }

    private static Field getField(final Class<?> startClazz, final String fieldName) throws NoSuchFieldException {
        Field field = null;
        Class<?> clazz = startClazz;
        do {
            try {
                field = clazz.getDeclaredField(fieldName);
            } catch (final NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
                if (clazz == null) {
                    throw e;
                }
            }
        } while (field == null);
        return field;
    }

    @SuppressWarnings({ "unchecked", "restriction" })
    private static void addUrlToAppClassLoaderURLClassPath(final ClassLoader systemClassLoader, final URL url)
            throws NoSuchFieldException, SecurityException {
        //normal way: --add-opens java.base/jdk.internal.loader=ALL-UNNAMED
        //hacker way: https://javax0.wordpress.com/2017/05/03/hacking-the-integercache-in-java-9/
        final sun.misc.Unsafe unsafe = getUnsafe();
        //jdk.internal.loader.ClassLoaders.AppClassLoader.ucp
        final Field ucpField = getField(systemClassLoader.getClass(), "ucp");
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
        return java.util.Collections.unmodifiableSet(pathsAddedToSystemClassLoader);
    }

    /**
     * http://stackoverflow.com/questions/11134159/how-to-load-attachprovider-attach-dll-dynamically
     * 
     * https://stackoverflow.com/a/70126075
     */
    public static void addPathToJavaLibraryPath(final File dir) {
        if (dir == null) {
            return;
        }
        final String javaLibraryPathKey = "java.library.path";
        //CHECKSTYLE:OFF
        final String existingJavaLibraryPath = System.getProperty(javaLibraryPathKey);
        //CHECKSTYLE:ON
        final String newJavaLibraryPath;
        if (!org.springframework.util.StringUtils.hasLength(existingJavaLibraryPath)) {
            newJavaLibraryPath = existingJavaLibraryPath + File.pathSeparator + dir.getAbsolutePath();
        } else {
            newJavaLibraryPath = dir.getAbsolutePath();
        }
        //CHECKSTYLE:OFF
        System.setProperty(javaLibraryPathKey, newJavaLibraryPath);
        //CHECKSTYLE:ON
        try {
            final Class<?> nativeLibrariesClass = Class.forName("jdk.internal.loader.NativeLibraries");
            final Class<?>[] declClassArr = nativeLibrariesClass.getDeclaredClasses();
            final Class<?> libraryPaths = java.util.Arrays.stream(declClassArr)
                    .filter(klass -> klass.getSimpleName().equals("LibraryPaths"))
                    .findFirst()
                    .get();
            final Field field = libraryPaths.getDeclaredField("USER_PATHS");
            field.setAccessible(true);
            removeFinalModifier(field);

            final String[] paths = (String[]) field.get(null);
            final String[] newPaths = new String[paths.length + 1];
            for (int i = 0; i < paths.length; i++) {
                newPaths[i] = paths[i];
            }
            newPaths[newPaths.length - 1] = dir.getAbsolutePath();
            field.set(null, newPaths);
        } catch (final NoSuchFieldException | ClassNotFoundException e) {
            //retry different approach, this might happen on older JVMs
            addPathToJavaLibraryPathJavaOld();
        } catch (final SecurityException e) {
            throw new RuntimeException(e);
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void removeFinalModifier(final Field field) throws IllegalAccessException, NoSuchFieldException {
        final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup());
        final VarHandle varHandle = lookup.findVarHandle(Field.class, "modifiers", int.class);
        varHandle.set(field, field.getModifiers() & ~Modifier.FINAL);
    }

    private static void addPathToJavaLibraryPathJavaOld() {
        try {
            final Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
            fieldSysPath.setAccessible(true);
            fieldSysPath.set(ClassLoader.class, null);
        } catch (final NoSuchFieldException e) {
            /*
             * ignore, happens on ibm-jdk-8 and adoptopenjdk-8-openj9 but has no influence since there is no cached
             * field that needs a reset
             */
            return;
        } catch (final SecurityException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        } catch (final IllegalArgumentException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        } catch (final IllegalAccessException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        }
        try {
            final Method methodInitLibraryPaths = ClassLoader.class.getDeclaredMethod("initLibraryPaths");
            methodInitLibraryPaths.setAccessible(true);
            methodInitLibraryPaths.invoke(ClassLoader.class);
        } catch (final NoSuchMethodException e) {
            //ignore, this might happen on older JVMs
        } catch (final SecurityException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        } catch (final IllegalAccessException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        } catch (final IllegalArgumentException e) {
            org.springframework.util.ReflectionUtils.handleReflectionException(e);
        } catch (final InvocationTargetException e) {
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
