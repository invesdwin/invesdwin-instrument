package lombok.launch;

import java.lang.instrument.Instrumentation;

public class AgentAccessor {

	public static void agentmain(final String agentArgs, final Instrumentation instrumentation) throws Throwable {
		Agent.agentmain(agentArgs, instrumentation);
	}

	public static void premain(final String agentArgs, final Instrumentation instrumentation) throws Throwable {
		Agent.premain(agentArgs, instrumentation);
	}

}
