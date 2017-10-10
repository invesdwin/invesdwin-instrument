package de.invesdwin.instrument;

import javax.annotation.concurrent.NotThreadSafe;

import org.junit.Test;

@NotThreadSafe
public class DynamicInstrumentationLoaderTest {

    @Test
    public void test() {
        DynamicInstrumentationLoader.waitForInitialized();
        DynamicInstrumentationLoader.initLoadTimeWeavingContext();
    }

}
