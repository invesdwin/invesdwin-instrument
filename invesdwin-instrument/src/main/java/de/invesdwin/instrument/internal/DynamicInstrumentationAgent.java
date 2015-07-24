package de.invesdwin.instrument.internal;

import java.lang.instrument.Instrumentation;

import javax.annotation.concurrent.Immutable;

import org.springframework.instrument.InstrumentationSavingAgent;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;

@Immutable
public final class DynamicInstrumentationAgent {

    private DynamicInstrumentationAgent() {}

    public static void premain(final String args, final Instrumentation inst) throws Exception {
        if (InstrumentationLoadTimeWeaver.isInstrumentationAvailable()) {
            throw new IllegalStateException("instrumentation is already available, the agent should have been loaded!");
        }

        InstrumentationSavingAgent.premain(args, inst);
    }

    public static void agentmain(final String args, final Instrumentation inst) throws Exception {
        premain(args, inst);
    }

}
