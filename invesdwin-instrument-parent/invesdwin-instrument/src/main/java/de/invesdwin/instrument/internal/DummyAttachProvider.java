package de.invesdwin.instrument.internal;

import java.io.IOException;
import java.util.List;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

// @Immutable
public class DummyAttachProvider extends com.sun.tools.attach.spi.AttachProvider {

    @Override
    public VirtualMachine attachVirtualMachine(final String arg0) throws AttachNotSupportedException, IOException {
        return null;
    }

    @Override
    public List<VirtualMachineDescriptor> listVirtualMachines() {
        return null;
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public String type() {
        return null;
    }

}
