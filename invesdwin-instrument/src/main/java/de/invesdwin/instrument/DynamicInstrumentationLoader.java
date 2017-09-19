package de.invesdwin.instrument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.annotation.concurrent.ThreadSafe;

import org.apache.commons.io.IOUtils;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;

import de.invesdwin.instrument.internal.AgentClassLoaderReference;
import de.invesdwin.instrument.internal.DynamicInstrumentationAgent;
import de.invesdwin.instrument.internal.JdkFilesFinder;

@ThreadSafe
public final class DynamicInstrumentationLoader {

    private static volatile Throwable threadFailed;
    /**
     * keeping a reference here so it is not garbage collected
     */
    private static GenericXmlApplicationContext ltwCtx;

    private DynamicInstrumentationLoader() {}

    public static boolean isInitialized() {
        return InstrumentationLoadTimeWeaver.isInstrumentationAvailable();
    }

    public static void waitForInitialized() {
        try {
            while (!isInitialized() && threadFailed == null) {
                TimeUnit.MILLISECONDS.sleep(1);
            }
            if (threadFailed != null) {
                throw new RuntimeException(threadFailed);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static synchronized GenericXmlApplicationContext initLoadTimeWeavingContext() {
        org.assertj.core.api.Assertions.assertThat(isInitialized()).isTrue();
        if (ltwCtx == null) {
            final GenericXmlApplicationContext ctx = new GenericXmlApplicationContext();
            ctx.load(new ClassPathResource("/META-INF/ctx.spring.weaving.xml"));
            ctx.refresh();
            ltwCtx = ctx;
        }
        return ltwCtx;
    }

    static {
        if (!isInitialized()) {
            try {
                final File tempAgentJar = createTempAgentJar();
                setAgentClassLoaderReference();
                final String pid = DynamicInstrumentationProperties.getProcessId();
                final Thread loadAgentThread = new Thread() {

                    @Override
                    public void run() {
                        try {
                            //use reflection since tools.jar has been added to the classpath dynamically
                            final Class<?> vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
                            final Object vm = vmClass.getMethod("attach", String.class).invoke(null,
                                    String.valueOf(pid));
                            vmClass.getMethod("loadAgent", String.class).invoke(vm, tempAgentJar.getAbsolutePath());
                        } catch (final Throwable e) {
                            threadFailed = e;
                            throw new RuntimeException(e);
                        }
                    }
                };

                DynamicInstrumentationReflections.addPathToSystemClassLoader(tempAgentJar);

                final JdkFilesFinder jdkFilesFinder = new JdkFilesFinder();
                final File toolsJar = jdkFilesFinder.findToolsJar();
                DynamicInstrumentationReflections.addPathToSystemClassLoader(toolsJar);

                final File attachLib = jdkFilesFinder.findAttachLib();
                DynamicInstrumentationReflections.addPathToJavaLibraryPath(attachLib.getParentFile());

                loadAgentThread.start();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }

        }
    }

    private static void setAgentClassLoaderReference() throws Exception {
        final Class<AgentClassLoaderReference> agentClassLoaderReferenceClass = AgentClassLoaderReference.class;
        final File tempAgentClassLoaderJar = createTempJar(agentClassLoaderReferenceClass, false);
        DynamicInstrumentationReflections.addPathToSystemClassLoader(tempAgentClassLoaderJar);
        final ClassLoader systemClassLoader = DynamicInstrumentationReflections.getSystemClassLoader();
        final Class<?> systemAgentClassLoaderReferenceClass = systemClassLoader
                .loadClass(agentClassLoaderReferenceClass.getName());
        final Method setAgentClassLoaderMethod = systemAgentClassLoaderReferenceClass
                .getDeclaredMethod("setAgentClassLoader", ClassLoader.class);
        setAgentClassLoaderMethod.invoke(null, DynamicInstrumentationReflections.getContextClassLoader());
    }

    private static File createTempAgentJar() throws ClassNotFoundException {
        try {
            return createTempJar(DynamicInstrumentationAgent.class, true);
        } catch (final Throwable e) {
            final String message = "Unable to find class [de.invesdwin.instrument.internal.DynamicInstrumentationAgent] in classpath."
                    + "\nPlease make sure you have added invesdwin-instrument.jar to your classpath properly,"
                    + "\nor make sure you have embedded it correctly into your fat-jar."
                    + "\nThey can be created e.g. with \"maven-shade-plugin\"."
                    + "\nPlease be aware that some fat-jar solutions might not work well due to classloader issues.";
            throw new ClassNotFoundException(message, e);
        }
    }

    /**
     * Creates a new jar that only contains the DynamicInstrumentationAgent class.
     */
    private static File createTempJar(final Class<?> clazz, final boolean agent) throws Exception {
        final String className = clazz.getName();
        final File tempAgentJar = new File(DynamicInstrumentationProperties.TEMP_DIRECTORY, className + ".jar");
        final Manifest manifest = new Manifest(clazz.getResourceAsStream("/META-INF/MANIFEST.MF"));
        if (agent) {
            manifest.getMainAttributes().putValue("Premain-Class", className);
            manifest.getMainAttributes().putValue("Agent-Class", className);
            manifest.getMainAttributes().putValue("Can-Redefine-Classes", String.valueOf(true));
            manifest.getMainAttributes().putValue("Can-Retransform-Classes", String.valueOf(true));
        }
        final JarOutputStream tempJarOut = new JarOutputStream(new FileOutputStream(tempAgentJar), manifest);
        final JarEntry entry = new JarEntry(className.replace(".", "/") + ".class");
        tempJarOut.putNextEntry(entry);
        final InputStream classIn = DynamicInstrumentationReflections.getClassInputStream(clazz);
        IOUtils.copy(classIn, tempJarOut);
        tempJarOut.closeEntry();
        tempJarOut.close();
        return tempAgentJar;
    }

}
