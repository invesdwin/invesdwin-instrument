package com.sample.api.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import de.invesdwin.instrument.DynamicInstrumentationLoader;

@SpringBootApplication(scanBasePackages = "com.sample.api.server")
public class Application extends SpringBootServletInitializer {
	
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(Application.class);
	}
	
	public static void main(String[] args) {
		//dynamically attach java agent to jvm if not already present
		DynamicInstrumentationLoader.waitForInitialized();
		
		//weave all classes before they are loaded as beans
        DynamicInstrumentationLoader.initLoadTimeWeavingContext();
		
		SpringApplication.run(Application.class, args);
	}
}
