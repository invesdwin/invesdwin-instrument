package de.invesdwin.instrument.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;

import de.invesdwin.instrument.internal.compile.DynamicInstrumentationClassInfo;
import de.invesdwin.instrument.internal.compile.DynamicInstrumentationCompiler;

// @Immutable
public final class DynamicInstrumentationAgentCompiler {

    public static final String UUID_PLACEHOLDER = "<UUID>";
    public static final String TEMPLATE = DynamicInstrumentationAgent.class.getSimpleName() + ".java.template";

    public static final int FIRST_PRECOMPILED_UUID = 1;
    public static final int MAX_PRECOMPILED_UUID = 25;
    private static int nextPrecompiledUuid = FIRST_PRECOMPILED_UUID;

    private DynamicInstrumentationAgentCompiler() {
    }

    public static DynamicInstrumentationClassInfo precompiled(final String uuid) {
        final String className = DynamicInstrumentationAgent.class.getSimpleName() + "_" + uuid;
        final String fqdn = DynamicInstrumentationAgent.class.getName() + "_" + uuid;
        final byte[] bytes = readPrecompiled(className);
        return new DynamicInstrumentationClassInfo(fqdn, bytes);
    }

    public static synchronized String nextPrecompiledUuid() {
        if (nextPrecompiledUuid > MAX_PRECOMPILED_UUID) {
            return null;
        }
        final String uuid = String.valueOf(nextPrecompiledUuid);
        nextPrecompiledUuid++;
        return uuid;
    }

    public static String nextCompileUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static DynamicInstrumentationClassInfo compile(final String uuid) {
        final String template = readTemplate().replace(UUID_PLACEHOLDER, uuid);
        final List<DynamicInstrumentationClassInfo> compiled;
        try {
            compiled = DynamicInstrumentationCompiler.compile(java.util.Arrays.asList(template));
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
        final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
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
