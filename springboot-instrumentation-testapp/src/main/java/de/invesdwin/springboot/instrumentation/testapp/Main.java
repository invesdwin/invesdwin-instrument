package de.invesdwin.springboot.instrumentation.testapp;

import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;

import de.invesdwin.instrument.DynamicInstrumentationLoader;

public class Main {
	public static void main(String[] args) {
		DynamicInstrumentationLoader.waitForInitialized();
		DynamicInstrumentationLoader.initLoadTimeWeavingContext();
		if (!InstrumentationLoadTimeWeaver.isInstrumentationAvailable()) {
			throw new IllegalStateException("Instrumentation not available!");
		} else {
			System.out.println("Instrumentation available!");
		}
	}
}