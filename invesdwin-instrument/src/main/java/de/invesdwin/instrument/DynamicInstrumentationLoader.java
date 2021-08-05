package de.invesdwin.instrument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.annotation.concurrent.ThreadSafe;

import org.apache.commons.io.IOUtils;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import de.invesdwin.instrument.internal.AgentClassLoaderReference;
import de.invesdwin.instrument.internal.DynamicInstrumentationAgentCompiler;
import de.invesdwin.instrument.internal.DynamicInstrumentationLoadAgentMain;
import de.invesdwin.instrument.internal.JdkFilesFinder;
import de.invesdwin.instrument.internal.compile.DynamicInstrumentationClassInfo;

@ThreadSafe
public final class DynamicInstrumentationLoader {

    private static volatile Throwable threadFailed;
    private static volatile String toolsJarPath;
    private static volatile String attachLibPath;
    /**
     * keeping a reference here so it is not garbage collected
     */
    private static GenericXmlApplicationContext ltwCtx;

    private DynamicInstrumentationLoader() {
    }

    public static boolean isInitialized() {
        return InstrumentationLoadTimeWeaver.isInstrumentationAvailable();
    }

    public static void waitForInitialized() {
        try {
            while (!isInitialized() && threadFailed == null) {
                TimeUnit.MILLISECONDS.sleep(1);
            }
            if (threadFailed != null) {
                final String javaVersion = getJavaVersion();
                final String javaHome = getJavaHome();
                throw new RuntimeException("Additional information: javaVersion=" + javaVersion + "; javaHome="
                        + javaHome + "; toolsJarPath=" + toolsJarPath + "; attachLibPath=" + attachLibPath,
                        threadFailed);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static synchronized GenericXmlApplicationContext initLoadTimeWeavingContext() {
        if (!isInitialized()) {
            throw new IllegalStateException("Should be initialized");
        }
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
                final String uuid = UUID.randomUUID().toString().replace("-", "");
                final File tempAgentJar = createTempAgentJar(uuid);
                setAgentClassLoaderReference(uuid);
                final String pid = DynamicInstrumentationProperties.getProcessId();
                final Thread loadAgentThread = new Thread() {

                    @Override
                    public void run() {
                        try {
                            loadAgent(tempAgentJar, pid, uuid);
                        } catch (final Throwable e) {
                            threadFailed = e;
                            throw new RuntimeException(e);
                        }
                    }
                };

                DynamicInstrumentationReflections.addPathToSystemClassLoader(tempAgentJar);

                final JdkFilesFinder jdkFilesFinder = new JdkFilesFinder();

                if (DynamicInstrumentationReflections.isBeforeJava9()) {
                    final File toolsJar = jdkFilesFinder.findToolsJar();
                    DynamicInstrumentationReflections.addPathToSystemClassLoader(toolsJar);
                    DynamicInstrumentationLoader.toolsJarPath = toolsJar.getAbsolutePath();

                    final File attachLib = jdkFilesFinder.findAttachLib();
                    DynamicInstrumentationReflections.addPathToJavaLibraryPath(attachLib.getParentFile());
                    DynamicInstrumentationLoader.attachLibPath = attachLib.getAbsolutePath();
                }

                loadAgentThread.start();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }

        }
    }

    private static void loadAgent(final File tempAgentJar, final String pid, final String uuid) throws Exception {
        if (DynamicInstrumentationReflections.isBeforeJava9()) {
            DynamicInstrumentationLoadAgentMain.loadAgent(pid, tempAgentJar.getAbsolutePath());
        } else {
            //-Djdk.attach.allowAttachSelf https://www.bountysource.com/issues/45231289-self-attach-fails-on-jdk9
            //workaround this limitation by attaching from a new process
            final File loadAgentJar = createTempJar(DynamicInstrumentationLoadAgentMain.class, uuid, false,
                    de.invesdwin.instrument.internal.DummyAttachProvider.class);
            final String javaExecutable = getJavaHome() + File.separator + "bin" + File.separator + "java";
            final List<String> command = new ArrayList<String>();
            command.add(javaExecutable);
            command.add("-classpath");
            command.add(loadAgentJar.getAbsolutePath()); //tools.jar not needed since java9
            command.add(DynamicInstrumentationLoadAgentMain.class.getName());
            command.add(pid);
            command.add(tempAgentJar.getAbsolutePath());
            new ProcessExecutor().command(command)
                    .destroyOnExit()
                    .exitValueNormal()
                    .redirectOutput(Slf4jStream.of(DynamicInstrumentationLoader.class).asInfo())
                    .redirectError(Slf4jStream.of(DynamicInstrumentationLoader.class).asWarn())
                    .execute();
        }
    }

    private static String getJavaHome() {
        //CHECKSTYLE:OFF
        return System.getProperty("java.home");
        //CHECKSTYLE:ON
    }

    private static String getJavaVersion() {
        //CHECKSTYLE:OFF
        return System.getProperty("java.version");
        //CHECKSTYLE:ON
    }

    private static void setAgentClassLoaderReference(final String uuid) throws Exception {
        final Class<AgentClassLoaderReference> agentClassLoaderReferenceClass = AgentClassLoaderReference.class;
        final File tempAgentClassLoaderJar = createTempJar(agentClassLoaderReferenceClass, null, false);
        DynamicInstrumentationReflections.addPathToSystemClassLoader(tempAgentClassLoaderJar);
        final ClassLoader systemClassLoader = DynamicInstrumentationReflections.getSystemClassLoader();
        final Class<?> systemAgentClassLoaderReferenceClass = systemClassLoader
                .loadClass(agentClassLoaderReferenceClass.getName());
        final Method setAgentClassLoaderMethod = systemAgentClassLoaderReferenceClass
                .getDeclaredMethod("setAgentClassLoader", String.class, ClassLoader.class);
        setAgentClassLoaderMethod.invoke(null, uuid, DynamicInstrumentationReflections.getContextClassLoader());
    }

    private static File createTempAgentJar(final String uuid) throws ClassNotFoundException {
        try {
            final DynamicInstrumentationClassInfo classInfo = DynamicInstrumentationAgentCompiler.compile(uuid);
            final String className = classInfo.toString();
            try (InputStream classIn = classInfo.newInputStream()) {
                return createTempJar(className, classIn, uuid, true);
            }
        } catch (final Throwable e) {
            final String message = "Unable to find file [de.invesdwin.instrument.internal.DynamicInstrumentationAgent.java.template] in classpath."
                    + "\nPlease make sure you have added invesdwin-instrument.jar to your classpath properly,"
                    + "\nor make sure you have embedded it correctly into your fat-jar."
                    + "\nThey can be created e.g. with \"maven-shade-plugin\"."
                    + "\nPlease be aware that some fat-jar solutions might not work well due to classloader issues.";
            throw new ClassNotFoundException(message, e);
        }
    }

    private static File createTempJar(final Class<?> clazz, final String uuid, final boolean agent,
            final Class<?>... additionalClasses) throws Exception {
        final String className = clazz.getName();
        try (InputStream classIn = DynamicInstrumentationReflections.getClassInputStream(clazz)) {
            return createTempJar(className, classIn, uuid, agent, additionalClasses);
        }
    }

    /**
     * Creates a new jar that only contains the DynamicInstrumentationAgent class.
     */
    private static File createTempJar(final String className, final InputStream classIn, final String uuid,
            final boolean agent, final Class<?>... additionalClasses) throws Exception {
        final StringBuilder fileName = new StringBuilder(className);
        if (uuid != null) {
            fileName.append("_");
            fileName.append(uuid);
        }
        fileName.append(".jar");
        final File tempAgentJar = new File(DynamicInstrumentationProperties.TEMP_DIRECTORY, fileName.toString());
        final Manifest manifest = new Manifest(
                DynamicInstrumentationLoader.class.getResourceAsStream("/META-INF/MANIFEST.MF"));
        if (agent) {
            manifest.getMainAttributes().putValue("Premain-Class", className);
            manifest.getMainAttributes().putValue("Agent-Class", className);
            manifest.getMainAttributes().putValue("Can-Redefine-Classes", String.valueOf(true));
            manifest.getMainAttributes().putValue("Can-Retransform-Classes", String.valueOf(true));
        }
        final JarOutputStream tempJarOut = new JarOutputStream(new FileOutputStream(tempAgentJar), manifest);
        final JarEntry entry = new JarEntry(className.replace(".", "/") + ".class");
        tempJarOut.putNextEntry(entry);
        IOUtils.copy(classIn, tempJarOut);
        tempJarOut.closeEntry();
        if (additionalClasses != null) {
            for (final Class<?> additionalClazz : additionalClasses) {
                final String additionalClassName = additionalClazz.getName();
                final JarEntry additionalEntry = new JarEntry(additionalClassName.replace(".", "/") + ".class");
                tempJarOut.putNextEntry(additionalEntry);
                final InputStream additionalClassIn = DynamicInstrumentationReflections
                        .getClassInputStream(additionalClazz);
                IOUtils.copy(additionalClassIn, tempJarOut);
                tempJarOut.closeEntry();
            }
        }
        tempJarOut.close();
        return tempAgentJar;
    }

}
