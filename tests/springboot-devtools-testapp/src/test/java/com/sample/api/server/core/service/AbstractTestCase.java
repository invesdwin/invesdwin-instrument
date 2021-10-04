package com.sample.api.server.core.service;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;

import de.invesdwin.instrument.DynamicInstrumentationLoader;

@SpringBootTest
public class AbstractTestCase {
	
	@BeforeAll
	static void beforeAll() {
		DynamicInstrumentationLoader.waitForInitialized();
        DynamicInstrumentationLoader.initLoadTimeWeavingContext();
	}

}
