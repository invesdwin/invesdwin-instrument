package de.invesdwin.instrument.internal;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

// @Immutable
public final class DynamicInstrumentationAgent {

    public static final String DEFAULT_UUID = "DEFAULT";

    private DynamicInstrumentationAgent() {}

    public static void premain(final String args, final Instrumentation inst) throws Exception {
        final Class<AgentClassLoaderReference> agentClassLoaderReferenceClass = AgentClassLoaderReference.class;
        final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        final Class<?> systemAgentClassLoaderReferenceClass = systemClassLoader
                .loadClass(agentClassLoaderReferenceClass.getName());
        final Method getAgentClassLoaderMethod = systemAgentClassLoaderReferenceClass
                .getDeclaredMethod("getAgentClassLoader", String.class);
        ClassLoader agentClassLoader = (ClassLoader) getAgentClassLoaderMethod.invoke(null, DEFAULT_UUID);
        if (agentClassLoader == null) {
            //fallback to contextClassLoader, don't use external dependencies
            agentClassLoader = Thread.currentThread().getContextClassLoader();
        }
        if (agentClassLoader == null) {
            agentClassLoader = systemClassLoader;
        }
        final Class<?> agentInstrumentationInitializer = agentClassLoader.loadClass(
                DynamicInstrumentationAgent.class.getPackage().getName() + ".AgentInstrumentationInitializer");
        final Method initializeMethod = agentInstrumentationInitializer.getDeclaredMethod("initialize", String.class,
                Instrumentation.class);
        initializeMethod.invoke(null, args, inst);
    }

    public static void agentmain(final String args, final Instrumentation inst) throws Exception {
        premain(args, inst);
    }

}
