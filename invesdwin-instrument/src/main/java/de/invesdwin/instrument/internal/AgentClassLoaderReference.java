package de.invesdwin.instrument.internal;

// @NotThreadSafe
public final class AgentClassLoaderReference {

    private static ClassLoader agentClassLoader;

    private AgentClassLoaderReference() {}

    public static ClassLoader getAgentClassLoader() {
        final ClassLoader classLoader = agentClassLoader;
        AgentClassLoaderReference.agentClassLoader = null;
        return classLoader;
    }

    public static void setAgentClassLoader(final ClassLoader agentClassLoader) {
        AgentClassLoaderReference.agentClassLoader = agentClassLoader;
    }
}
