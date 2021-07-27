package de.invesdwin.instrument.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// @NotThreadSafe
public final class AgentClassLoaderReference {

    private static final Map<String, ClassLoader> UUID_AGENTCLASSLOADER = new ConcurrentHashMap<>();;

    private AgentClassLoaderReference() {
    }

    public static ClassLoader getAgentClassLoader(final String uuid) {
        if (uuid == null) {
            //fallback to context without using DynamicInstrumentationReflections
            return Thread.currentThread().getContextClassLoader();
        }
        final ClassLoader classLoader = UUID_AGENTCLASSLOADER.get(uuid);
        if (classLoader == null) {
            //fallback to context without using DynamicInstrumentationReflections
            return Thread.currentThread().getContextClassLoader();
        }
        //remove the reference as soon as possible so we don't create long holding security problems
        UUID_AGENTCLASSLOADER.remove(uuid);
        return classLoader;
    }

    public static void setAgentClassLoader(final String uuid, final ClassLoader agentClassLoader) {
        UUID_AGENTCLASSLOADER.put(uuid, agentClassLoader);
    }
}
