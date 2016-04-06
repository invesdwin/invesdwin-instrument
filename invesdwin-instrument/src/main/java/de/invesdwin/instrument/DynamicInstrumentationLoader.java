package de.invesdwin.instrument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javax.annotation.concurrent.ThreadSafe;

import org.apache.commons.io.IOUtils;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;

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
                final String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
                final String pid = nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'));
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

    /**
     * Creates a new jar that only contains the DynamicInstrumentationAgent class.
     */
    private static File createTempAgentJar() throws Exception {
        final String agentClassName = DynamicInstrumentationAgent.class.getName();
        final File tempAgentJar = new File(DynamicInstrumentationProperties.TEMP_DIRECTORY, agentClassName + ".jar");
        final Manifest manifest = new Manifest(
                DynamicInstrumentationLoader.class.getResourceAsStream("/META-INF/MANIFEST.MF"));
        manifest.getMainAttributes().putValue("Premain-Class", agentClassName);
        manifest.getMainAttributes().putValue("Agent-Class", agentClassName);
        manifest.getMainAttributes().putValue("Can-Redefine-Classes", String.valueOf(true));
        manifest.getMainAttributes().putValue("Can-Retransform-Classes", String.valueOf(true));
        final JarOutputStream tempAgentJarOut = new JarOutputStream(new FileOutputStream(tempAgentJar), manifest);
        final ZipEntry entry = new ZipEntry(agentClassName.replace(".", "/") + ".class");
        tempAgentJarOut.putNextEntry(entry);
        final InputStream agentClassIn = getAgentClassInputStream();
        IOUtils.copy(agentClassIn, tempAgentJarOut);
        tempAgentJarOut.closeEntry();
        tempAgentJarOut.close();
        return tempAgentJar;
    }

    private static InputStream getAgentClassInputStream() throws ClassNotFoundException {
        try {
            final InputStream agentClassIn = DynamicInstrumentationAgent.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .openStream();
            return agentClassIn;
        } catch (final Throwable e) {
            final String message = "Unable to find class [de.invesdwin.instrument.internal.DynamicInstrumentationAgent] in classpath."
                    + "\nPlease make sure you have added invesdwin-instrument.jar to your classpath properly,"
                    + "\nor make sure you have embedded it correctly into your fat-jar."
                    + "\nThey can be created e.g. with \"maven-shade-plugin\"."
                    + "\nPlease be aware that \"one-jar\" might now not work well due to classloader issues.";
            throw new ClassNotFoundException(message, e);
        }
    }

}
