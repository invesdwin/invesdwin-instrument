package de.invesdwin.instrument.burningwave;

import java.lang.instrument.Instrumentation;

import org.burningwave.core.assembler.StaticComponentContainer;

// @Immutable
public final class BurningwaveInstrumentationAgent {

    private BurningwaveInstrumentationAgent() {}

    public static void premain(final String args, final Instrumentation inst) throws Exception {
        StaticComponentContainer.Modules.exportAllToAll();
    }

    public static void agentmain(final String args, final Instrumentation inst) throws Exception {
        premain(args, inst);
    }

}
