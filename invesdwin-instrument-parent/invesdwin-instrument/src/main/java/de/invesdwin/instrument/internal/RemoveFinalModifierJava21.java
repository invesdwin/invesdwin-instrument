package de.invesdwin.instrument.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.annotation.concurrent.Immutable;

/**
 * https://stackoverflow.com/a/77705202/67492
 */
@Immutable
public final class RemoveFinalModifierJava21 {

    private RemoveFinalModifierJava21() {}

    public static void removeFinalModifierJava21(final Field field) throws Exception {
        final Method[] classMethods = Class.class.getDeclaredMethods();
        final Method declaredFieldMethod = java.util.Arrays.stream(classMethods)
                .filter(x -> java.util.Objects.equals(x.getName(), "getDeclaredFields0"))
                .findAny()
                .orElseThrow();
        declaredFieldMethod.setAccessible(true);
        final Field[] declaredFieldsOfField = (Field[]) declaredFieldMethod.invoke(Field.class, false);
        final Field modifiersField = java.util.Arrays.stream(declaredFieldsOfField)
                .filter(x -> java.util.Objects.equals(x.getName(), "modifiers"))
                .findAny()
                .orElseThrow();
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    }

    //CHECKSTYLE:OFF
    public static void setRestrictedField(final Field field, final Object value)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
        //CHECKSTYLE:ON
        final Class<?> memberNameClass = Class.forName("java.lang.invoke.MemberName");

        final Constructor<?> memberNameConstructor = memberNameClass.getDeclaredConstructor(Field.class, boolean.class);
        memberNameConstructor.setAccessible(true);

        final Object memberNameInstanceForField = memberNameConstructor.newInstance(field, true);

        final Field memberNameFlagsField = memberNameClass.getDeclaredField("flags");

        memberNameFlagsField.setAccessible(true);

        //Manipulate flags to remove hints to it being final
        memberNameFlagsField.setInt(memberNameInstanceForField,
                memberNameFlagsField.getInt(memberNameInstanceForField) & ~Modifier.FINAL);

        final Method getReferenceKindMethod = memberNameClass.getDeclaredMethod("getReferenceKind");

        getReferenceKindMethod.setAccessible(true);

        final byte getReferenceKind = (byte) getReferenceKindMethod.invoke(memberNameInstanceForField);

        final MethodHandles.Lookup mh = MethodHandles.privateLookupIn(field.getDeclaringClass(),
                MethodHandles.lookup());

        final Method getDirectFieldCommonMethod = mh.getClass()
                .getDeclaredMethod("getDirectFieldCommon", byte.class, Class.class, memberNameClass, boolean.class);

        getDirectFieldCommonMethod.setAccessible(true);

        //Invoke last method to obtain the method handle

        final MethodHandle o = (MethodHandle) getDirectFieldCommonMethod.invoke(mh, getReferenceKind,
                field.getDeclaringClass(), memberNameInstanceForField, false);

        try {
            o.invoke(value);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
