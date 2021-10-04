package lombok.launch;

import java.lang.instrument.Instrumentation;

import org.burningwave.core.assembler.StaticComponentContainer;

public class AgentAccessor {

	static {
		if (StaticComponentContainer.Modules != null) {
			// Java 16 workaround
			StaticComponentContainer.Modules.exportAllToAll();
		}
	}

	public static void agentmain(final String agentArgs, final Instrumentation instrumentation) throws Throwable {
		Agent.agentmain(agentArgs, instrumentation);
	}

	public static void premain(final String agentArgs, final Instrumentation instrumentation) throws Throwable {
		Agent.premain(agentArgs, instrumentation);
	}

}
