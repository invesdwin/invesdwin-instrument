package de.invesdwin.instrument.internal;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

import javax.annotation.concurrent.Immutable;

@Immutable
public final class DynamicInstrumentationAgent {

    private DynamicInstrumentationAgent() {}

    public static void premain(final String args, final Instrumentation inst) throws Exception {
        final ClassLoader agentClassLoader = AgentClassLoaderReference.getAgentClassLoader();
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
