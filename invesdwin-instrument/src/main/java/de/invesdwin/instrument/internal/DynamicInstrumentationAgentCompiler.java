package de.invesdwin.instrument.internal;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import de.invesdwin.instrument.internal.compile.DynamicInstrumentationClassInfo;
import de.invesdwin.instrument.internal.compile.DynamicInstrumentationCompiler;

// @Immutable
public final class DynamicInstrumentationAgentCompiler {

    public static final String UUID_PLACEHOLDER = "<UUID>";
    public static final String TEMPLATE = DynamicInstrumentationAgent.class.getSimpleName() + ".java.template";

    private static final int MAX_PRECOMPILED = 9;
    private static int nextPrecompiled = 1;

    private DynamicInstrumentationAgentCompiler() {
    }

    public static DynamicInstrumentationClassInfo precompiled(final String uuid) {
        final String className = DynamicInstrumentationAgent.class.getSimpleName() + "_" + uuid;
        final String fqdn = DynamicInstrumentationAgent.class.getName() + "_" + uuid;
        final byte[] bytes = readPrecompiled(className);
        return new DynamicInstrumentationClassInfo(fqdn, bytes);
    }

    public static synchronized String nextPrecompiledUuid() {
        if (nextPrecompiled > MAX_PRECOMPILED) {
            return null;
        }
        final String uuid = String.valueOf(nextPrecompiled);
        nextPrecompiled++;
        return uuid;
    }

    public static String nextCompileUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static DynamicInstrumentationClassInfo compile(final String uuid) {
        final String template = readTemplate().replace(UUID_PLACEHOLDER, uuid);
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
            return readToString(resourceAsStream);
        } catch (final IOException e) {
            throw new RuntimeException("Error reading: " + TEMPLATE, e);
        }
    }

    private static String readToString(final InputStream in) throws IOException {
        final StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder.append(sCurrentLine).append("\n");
            }
        }
        return contentBuilder.toString();
    }

    private static byte[] readPrecompiled(final String className) {
        try (InputStream resourceAsStream = DynamicInstrumentationAgentCompiler.class
                .getResourceAsStream(className + ".class")) {
            return readToBytes(resourceAsStream);
        } catch (final IOException e) {
            throw new RuntimeException("Error reading: " + className, e);
        }
    }

    private static byte[] readToBytes(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buf = new byte[8192];
        //convert file into array of bytes
        long count = 0;
        int n;
        while (-1 != (n = in.read(buf))) {
            out.write(buf, 0, n);
            count += n;
        }
        if (count == 0) {
            return null;
        }
        return out.toByteArray();
    }

}
