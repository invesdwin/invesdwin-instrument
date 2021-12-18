package de.invesdwin.instrument.internal.compile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.annotation.concurrent.NotThreadSafe;

import org.junit.jupiter.api.Test;

import de.invesdwin.instrument.internal.DynamicInstrumentationAgent;
import de.invesdwin.instrument.internal.DynamicInstrumentationAgentCompiler;

@NotThreadSafe
public class DynamicInstrumentationAgentGenerator {

    @Test
    public void test() throws IOException {
        final File folder = new File(
                "src/main/java/" + DynamicInstrumentationAgent.class.getPackage().getName().replace(".", "/"));
        final String template = org.apache.commons.io.FileUtils.readFileToString(
                new File(folder, DynamicInstrumentationAgentCompiler.TEMPLATE), Charset.defaultCharset());
        for (int i = DynamicInstrumentationAgentCompiler.FIRST_PRECOMPILED_UUID; i <= DynamicInstrumentationAgentCompiler.MAX_PRECOMPILED_UUID; i++) {
            final File newFile = new File(folder,
                    DynamicInstrumentationAgent.class.getSimpleName() + "_" + i + ".java");
            org.apache.commons.io.FileUtils.writeStringToFile(newFile,
                    template.replace(DynamicInstrumentationAgentCompiler.UUID_PLACEHOLDER, "" + i),
                    Charset.defaultCharset());
        }
    }

}
