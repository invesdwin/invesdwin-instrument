package de.invesdwin.instrument.internal;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

// @Immutable
public final class DynamicInstrumentationAgent {

    public static final String UUID_FILE_NAME = "uuid.txt";

    private DynamicInstrumentationAgent() {
    }

    public static void premain(final String args, final Instrumentation inst) throws Exception {
        final String uuid = readUuid();
        ClassLoader agentClassLoader = AgentClassLoaderReference.getAgentClassLoader(uuid);
        if (agentClassLoader == null) {
            //fallback to contextClassLoader, don't use external dependencies
            agentClassLoader = Thread.currentThread().getContextClassLoader();
        }
        final Class<?> agentInstrumentationInitializer = agentClassLoader.loadClass(
                DynamicInstrumentationAgent.class.getPackage().getName() + ".AgentInstrumentationInitializer");
        final Method initializeMethod = agentInstrumentationInitializer.getDeclaredMethod("initialize", String.class,
                Instrumentation.class);
        initializeMethod.invoke(null, args, inst);
    }

    private static String readUuid() {
        try (InputStream in = DynamicInstrumentationAgent.class.getResourceAsStream("/" + UUID_FILE_NAME)) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            final String uuid = reader.readLine().trim();
            return uuid;
        } catch (final Throwable t) {
            return null;
        }
    }

    public static void agentmain(final String args, final Instrumentation inst) throws Exception {
        premain(args, inst);
    }

}
