package de.invesdwin.instrument.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// @ThreadSafe
public final class AgentClassLoaderReference {

    public static final int FIRST_PRECOMPILED_UUID = 1;
    public static final int MAX_PRECOMPILED_UUID = 25;
    private static int nextPrecompiledUuid = FIRST_PRECOMPILED_UUID;

    private static final Map<String, ClassLoader> UUID_AGENTCLASSLOADER = new ConcurrentHashMap<>();

    private AgentClassLoaderReference() {}

    public static synchronized String nextPrecompiledUuid() {
        if (nextPrecompiledUuid > MAX_PRECOMPILED_UUID) {
            return null;
        }
        final String uuid = String.valueOf(nextPrecompiledUuid);
        nextPrecompiledUuid++;
        return uuid;
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

    public static boolean containsAgentClassLoader(final String uuid) {
        return UUID_AGENTCLASSLOADER.containsKey(uuid);
    }

    public static void setAgentClassLoader(final String uuid, final ClassLoader agentClassLoader) {
        UUID_AGENTCLASSLOADER.put(uuid, agentClassLoader);
    }
}
