package de.invesdwin.instrument.internal;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

// @Immutable
public final class DynamicInstrumentationLoadAgentMain {

    private DynamicInstrumentationLoadAgentMain() {}

    public static void main(final String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: " + DynamicInstrumentationLoadAgentMain.class.getSimpleName()
                    + " <pid> <agentJarAbsolutePath>");
        }
        final String pid = args[0];
        final String agentJarAbsolutePath = args[1];
        loadAgent(pid, agentJarAbsolutePath);
    }

    /**
     * https://github.com/jmockit/jmockit1/blob/8ea057ffe08a3ef9788ee383db695a3a404d3c86/main/src/mockit/internal/startup/AgentLoader.java
     * 
     * http://tuhrig.de/implementing-interfaces-and-abstract-classes-on-the-fly/
     */
    public static void loadAgent(final String pid, final String agentJarAbsolutePath) {
        //use reflection since tools.jar has been added to the classpath dynamically
        try {
            final Class<?> attachProviderClass = Class.forName("com.sun.tools.attach.spi.AttachProvider");
            final List<?> providers = (List<?>) attachProviderClass.getMethod("providers").invoke(null);
            final Class<?> virtualMachineClass;
            final Object virtualMachine;
            if (providers.isEmpty()) {
                final String virtualMachineClassName = findVirtualMachineClassNameAccordingToOS();
                virtualMachineClass = Class.forName(virtualMachineClassName);
                final Constructor<?> vmConstructor = virtualMachineClass.getDeclaredConstructor(attachProviderClass,
                        String.class);
                vmConstructor.setAccessible(true);
                final Object dummyAttachProvider = Class.forName("de.invesdwin.instrument.internal.DummyAttachProvider")
                        .getDeclaredConstructor()
                        .newInstance();
                virtualMachine = vmConstructor.newInstance(dummyAttachProvider, pid);
            } else {
                virtualMachineClass = Class.forName("com.sun.tools.attach.VirtualMachine");
                virtualMachine = virtualMachineClass.getMethod("attach", String.class).invoke(null, pid);
            }
            virtualMachineClass.getMethod("loadAgent", String.class).invoke(virtualMachine, agentJarAbsolutePath);
            virtualMachineClass.getMethod("detach").invoke(virtualMachine);
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (final IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (final InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (final NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (final SecurityException e) {
            throw new RuntimeException(e);
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException(newHelpMessageForNonHotSpotVM(), e);
        } catch (final InstantiationException e) {
            throw new RuntimeException(e);
        } catch (final NoClassDefFoundError e) {
            throw new IllegalStateException("Native library for Attach API not available in this JRE", e);
        } catch (final UnsatisfiedLinkError e) {
            throw new IllegalStateException("Native library for Attach API not available in this JRE", e);
        }
    }

    private static String findVirtualMachineClassNameAccordingToOS() {
        if (File.separatorChar == '\\') {
            return "sun.tools.attach.WindowsVirtualMachine";
        }

        //CHECKSTYLE:OFF
        final String osName = System.getProperty("os.name");
        //CHECKSTYLE:ON

        if (osName.startsWith("Linux") || osName.startsWith("LINUX")) {
            return "sun.tools.attach.LinuxVirtualMachine";
        }

        if (osName.contains("FreeBSD") || osName.startsWith("Mac OS X")) {
            return "sun.tools.attach.BsdVirtualMachine";
        }

        if (osName.startsWith("Solaris") || osName.contains("SunOS")) {
            return "sun.tools.attach.SolarisVirtualMachine";
        }

        if (osName.contains("AIX")) {
            return "sun.tools.attach.AixVirtualMachine";
        }

        throw new IllegalStateException("Cannot use Attach API on unknown OS: " + osName);
    }

    private static String newHelpMessageForNonHotSpotVM() {
        //CHECKSTYLE:OFF
        final String vmName = System.getProperty("java.vm.name");
        //CHECKSTYLE:ON
        String helpMessage = "To run on " + vmName;

        if (vmName.contains("J9")) {
            helpMessage += ", add <IBM SDK>/lib/tools.jar to the runtime classpath (before invesdwin-instrument), or";
        }

        return helpMessage + " use -javaagent:invesdwin-instrument.jar";
    }

}
