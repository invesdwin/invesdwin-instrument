package de.invesdwin.instrument;

import javax.annotation.concurrent.NotThreadSafe;

import org.burningwave.core.assembler.StaticComponentContainer;
import org.junit.jupiter.api.Test;

@NotThreadSafe
public class DynamicInstrumentationLoaderTest {

    @Test
    public void test() {
        StaticComponentContainer.Modules.exportAllToAll();
        DynamicInstrumentationLoader.waitForInitialized();
        DynamicInstrumentationLoader.initLoadTimeWeavingContext();
    }

}
