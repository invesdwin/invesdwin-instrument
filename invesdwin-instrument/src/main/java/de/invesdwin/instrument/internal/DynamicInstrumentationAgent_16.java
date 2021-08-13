package de.invesdwin.instrument.internal;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

// CHECKSTYLE:OFF
// @Immutable
public final class DynamicInstrumentationAgent_16 {
    //CHECKSTYLE:ON

    private static final String UUID = "16";

    private DynamicInstrumentationAgent_16() {
    }

    public static void premain(final String args, final Instrumentation inst) throws Exception {
        ClassLoader agentClassLoader = AgentClassLoaderReference.getAgentClassLoader(UUID);
        if (agentClassLoader == null) {
            //fallback to contextClassLoader, don't use external dependencies
            agentClassLoader = Thread.currentThread().getContextClassLoader();
        }
        final Class<?> agentInstrumentationInitializer = agentClassLoader.loadClass(
                DynamicInstrumentationAgent_16.class.getPackage().getName() + ".AgentInstrumentationInitializer");
        final Method initializeMethod = agentInstrumentationInitializer.getDeclaredMethod("initialize", String.class,
                Instrumentation.class);
        initializeMethod.invoke(null, args, inst);
    }

    public static void agentmain(final String args, final Instrumentation inst) throws Exception {
        premain(args, inst);
    }

}
