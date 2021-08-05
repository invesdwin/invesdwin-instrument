package de.invesdwin.instrument.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import de.invesdwin.instrument.internal.compile.DynamicInstrumentationClassInfo;
import de.invesdwin.instrument.internal.compile.DynamicInstrumentationCompiler;

// @Immutable
public final class DynamicInstrumentationAgentCompiler {

    private static final String TEMPLATE = "DynamicInstrumentationAgent.java.template";

    private DynamicInstrumentationAgentCompiler() {
    }

    public static DynamicInstrumentationClassInfo compile(final String uuid) {
        final String template = readTemplate().replace("<UUID>", uuid);
        final List<DynamicInstrumentationClassInfo> compiled;
        try {
            compiled = DynamicInstrumentationCompiler.compile(Arrays.asList(template));
        } catch (final IOException e) {
            throw new RuntimeException("Error during compilation of: " + TEMPLATE, e);
        }
        if (compiled.size() != 1) {
            throw new RuntimeException(
                    "Expecting exactly one but got [" + compiled.size() + "] compiled classes for: " + TEMPLATE);
        }
        return compiled.get(0);
    }

    private static String readTemplate() {
        try (InputStream resourceAsStream = DynamicInstrumentationAgentCompiler.class.getResourceAsStream(TEMPLATE)) {
            return readFile(resourceAsStream);
        } catch (final IOException e) {
            throw new RuntimeException("Error reading: " + TEMPLATE, e);
        }
    }

    private static String readFile(final InputStream in) {
        final StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder.append(sCurrentLine).append("\n");
            }
        } catch (final IOException e) {
            //ignore
        }
        return contentBuilder.toString();
    }

}
