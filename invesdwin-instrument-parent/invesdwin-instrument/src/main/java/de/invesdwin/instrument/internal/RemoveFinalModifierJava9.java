package de.invesdwin.instrument.internal;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.annotation.concurrent.Immutable;

@Immutable
public final class RemoveFinalModifierJava9 {

    private RemoveFinalModifierJava9() {}

    public static void removeFinalModifierJava9(final Field field) throws Exception {
        final Method privateLookupInMethod = MethodHandles.class.getDeclaredMethod("privateLookupIn", Class.class,
                MethodHandles.Lookup.class);
        final MethodHandles.Lookup lookup = (MethodHandles.Lookup) privateLookupInMethod.invoke(null, Field.class,
                MethodHandles.lookup());
        final Method findVarHandleMethod = MethodHandles.Lookup.class.getDeclaredMethod("findVarHandle", Class.class,
                String.class, Class.class);
        final java.lang.invoke.VarHandle varHandle = (java.lang.invoke.VarHandle) findVarHandleMethod.invoke(lookup,
                Field.class, "modifiers", int.class);
        varHandle.set(field, field.getModifiers() & ~Modifier.FINAL);
    }

}
