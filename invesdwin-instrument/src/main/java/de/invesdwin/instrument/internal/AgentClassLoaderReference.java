package de.invesdwin.instrument.internal;

// @NotThreadSafe
public final class AgentClassLoaderReference {

    private static ClassLoader agentClassLoader;

    private AgentClassLoaderReference() {
    }

    public static ClassLoader getAgentClassLoader() {
        final ClassLoader classLoader = agentClassLoader;
        //remove the reference as soon as possible so we don't create long holding security problems
        AgentClassLoaderReference.agentClassLoader = null;
        return classLoader;
    }

    public static void setAgentClassLoader(final ClassLoader agentClassLoader) {
        AgentClassLoaderReference.agentClassLoader = agentClassLoader;
    }
}
