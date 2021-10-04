package com.sample.api.server;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.aspectj.weaver.loadtime.Agent;
import org.burningwave.core.assembler.StaticComponentContainer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.instrument.InstrumentationSavingAgent;

import com.sample.api.server.test.TestAspectUseCase;

import de.invesdwin.instrument.DynamicInstrumentationLoader;

@SpringBootApplication(scanBasePackages = "com.sample.api.server")
public class Application extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(final SpringApplicationBuilder application) {
		return application.sources(Application.class);
	}

	public static void main(final String[] args) throws Throwable {
		load();

		/*
		 * Start (or restart inside devtools) the spring application
		 */
		SpringApplication.run(Application.class, args);
	}

	private static void load()
			throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		if (StaticComponentContainer.Modules != null) {
			// Java 16 workaround for add-opens
			StaticComponentContainer.Modules.exportAllToAll();
		}

		// dynamically attach java agent to jvm if not already present
		DynamicInstrumentationLoader.waitForInitialized();

		// weave all classes before they are loaded as beans
		DynamicInstrumentationLoader.initLoadTimeWeavingContext();

		/*
		 * make sure aspectj-LTW is loaded also into
		 * org.springframework.boot.devtools.restart.RestartLauncher classloader
		 */
		org.aspectj.weaver.loadtime.Agent.agentmain("", InstrumentationSavingAgent.getInstrumentation());
		// test that the aspect works fine
		new TestAspectUseCase().testAspect();

		/*
		 * try to load lombok
		 */
		final Class<?> lombokAgentClass = Class.forName("lombok.launch.Agent");
		final Method agentMainMethod = lombokAgentClass.getDeclaredMethod("agentmain", String.class,
				Instrumentation.class);
		// java 16 workaround
		StaticComponentContainer.Methods.setAccessible(agentMainMethod, true);
		agentMainMethod.invoke(null, "", InstrumentationSavingAgent.getInstrumentation());
		/*
		 * causes java 16 module errors inside
		 * org.springframework.boot.devtools.restart.RestartLauncher
		 * 
		 */
//		AgentAccessor.agentmain("", InstrumentationSavingAgent.getInstrumentation());
	}
}
