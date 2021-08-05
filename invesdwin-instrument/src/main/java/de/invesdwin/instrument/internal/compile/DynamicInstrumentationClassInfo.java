package de.invesdwin.instrument.internal.compile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

// @NotThreadSafe
public class DynamicInstrumentationClassInfo {
    /**
     * fully qualified classname in a format suitable for Class.forName
     */
    private final String className;

    /**
     * bytecode for the class
     */
    private final byte[] bytes;

    public DynamicInstrumentationClassInfo(final String aClassName, final byte[] aBytes) {
        className = aClassName.replace('/', '.');
        // className = aClassName.replace('.', '/');
        bytes = aBytes;
    }

    public String getSimpleName() {
        return className.substring(className.lastIndexOf(".") + 1);
    }

    public byte[] getBytes() {
        return bytes;
    }

    public InputStream newInputStream() {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public String toString() {
        return className;
    }

    @Override
    public int hashCode() {
        return className.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj instanceof DynamicInstrumentationClassInfo) && ((DynamicInstrumentationClassInfo) obj).className.equals(this.className)) {
            return true;
        }
        return false;
    }
}