package com.sample.api.server;

import org.aspectj.weaver.loadtime.Agent;
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

	public static void main(final String[] args) {
		// dynamically attach java agent to jvm if not already present
		DynamicInstrumentationLoader.waitForInitialized();

		// weave all classes before they are loaded as beans
		DynamicInstrumentationLoader.initLoadTimeWeavingContext();

		new TestAspectUseCase().testAspect();

		Agent.agentmain("", InstrumentationSavingAgent.getInstrumentation());

		SpringApplication.run(Application.class, args);
	}
}
