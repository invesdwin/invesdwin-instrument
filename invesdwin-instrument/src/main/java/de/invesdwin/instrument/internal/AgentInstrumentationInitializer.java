package de.invesdwin.instrument.internal;

import java.lang.instrument.Instrumentation;

import javax.annotation.concurrent.NotThreadSafe;

import org.springframework.instrument.InstrumentationSavingAgent;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;

@NotThreadSafe
public final class AgentInstrumentationInitializer {

    private AgentInstrumentationInitializer() {}

    public static void initialize(final String args, final Instrumentation inst) {
        if (InstrumentationLoadTimeWeaver.isInstrumentationAvailable()) {
            throw new IllegalStateException(
                    "instrumentation is already available, the agent should have been loaded already!");
        }

        InstrumentationSavingAgent.premain(args, inst);
    }

}
