package com.headius.invoke.binder;

import com.headius.invoke.binder.transform.Cast;
import com.headius.invoke.binder.transform.Convert;
import com.headius.invoke.binder.transform.Drop;
import com.headius.invoke.binder.transform.Fold;
import com.headius.invoke.binder.transform.Insert;
import com.headius.invoke.binder.transform.Permute;
import com.headius.invoke.binder.transform.Spread;
import com.headius.invoke.binder.transform.Transform;

import java.io.PrintStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * The Binder class provides a DSL for building a chain of MethodHandles using
 * various of the adaptations provided by java.lang.invoke.MethodHandles. The
 * transformations are pushed into a stack, allowing the DSL to operate forward
 * from an incoming signature rather than backward from a target handle. This
 * is often conceptually easier to understand, and certainly easier to read.
 *
 * The transformations are also applied simultaneously to the starting
 * java.lang.invoke.MethodType, allowing Binder to check at each step whether
 * the adaptation is valid.
 *
 * Here's a typical use, starting with a signature that takes two Strings and
 * returns a String, dropping and inserting arguments, casting to a target
 * signature, and finally calling a target handle with that signature.
 *
 * <pre>
 * MethodHandle mh = Binder
 *     .from(String.class, String.class, String.class) // String w(String, String)
 *     .drop(1, String.class) // String x(String)
 *     .insert(0, 'hello') // String y(String, String)
 *     .cast(String.class, CharSequence.class, Object.class) // String z(CharSequence, Object)String
 *     .invoke(someTargetHandle);
 * </pre>
 */
public class Binder {

    private final Logger logger = Logger.getLogger("Invoke Binder");
    private final List<Transform> transforms = new ArrayList<>();
    private final List<MethodType> types = new ArrayList<>();
    private final MethodType start;

    /**
     * Construct a new Binder, starting from a given MethodType.
     *
     * @param start the starting MethodType, for calls entering the eventual chain
     */
    public Binder(MethodType start) {
        this.start = start;
        this.types.add(0, start);
    }

    /**
     * Construct a new Binder, starting from a given MethodType.
     *
     * @param start the starting MethodType, for calls entering the eventual chain
     * @return the Binder object
     */
    public static Binder from(MethodType start) {
        return new Binder(start);
    }

    /**
     * Construct a new Binder using a return type and argument types.
     *
     * @param returnType the return type of the incoming signature
     * @param argTypes the argument types of the incoming signature
     * @return the Binder object
     */
    public static Binder from(Class returnType, Class... argTypes) {
        return from(MethodType.methodType(returnType, argTypes));
    }

    /**
     * Add a Transform to the chain.
     *
     * @param transform
     */
    private void add(Transform transform) {
        add(transform, transform.down(types.get(0)));
    }

    /**
     * Add a Transform with an associated MethodType target to the chain.
     * @param transform
     * @param target
     */
    private void add(Transform transform, MethodType target) {
        types.add(0, target);
        transforms.add(0, transform);
    }

    /**
     * The current MethodType, were the handle chain to terminate at this point.
     *
     * @return the current MethodType
     */
    public MethodType type() {
        return types.get(0);
    }

    /**
     * Println the current MethodType to the given stream.
     *
     * @return this Binding
     */
    public Binder printType(PrintStream ps) {
        ps.println(types.get(0));
        return this;
    }

    /**
     * Println the current MethodType to stdout.
     *
     * @return this Binding
     */
    public Binder printType() {
        return printType(System.out);
    }

    /**
     * Insert at the given index the given argument value(s).
     *
     * @param index the index at which to insert the argument value
     * @param values the value(s) to insert
     * @return this Binder
     */
    public Binder insert(int index, Object... values) {
        add(new Insert(index, values));
        return this;
    }

    /**
     * Drop a single argument at the given index.
     *
     * @param index the index at which to drop an argument
     * @return this Binder
     */
    public Binder drop(int index) {
        return drop(index, 1);
    }

    /**
     * Drop from the given index a number of arguments.
     *
     * @param index the index at which to start dropping
     * @param count the number of arguments to drop
     * @return this Binder
     */
    public Binder drop(int index, int count) {
        add(new Drop(index, Arrays.copyOfRange(type().parameterArray(), index, index + count)));
        return this;
    }

    /**
     * Convert the incoming arguments to the given MethodType. The conversions
     * applied are equivalent to those in MethodHandle.asType(MethodType).
     *
     * @param target the target MethodType
     * @return this Binder
     */
    public Binder convert(MethodType target) {
        add(new Convert(type()), target);
        return this;
    }

    /**
     * Convert the incoming arguments to the given MethodType. The conversions
     * applied are equivalent to those in MethodHandle.asType(MethodType).
     *
     * @param returnType the target return type
     * @param argTypes the target argument types
     * @return this Binder
     */
    public Binder convert(Class returnType, Class... argTypes) {
        add(new Convert(type()), MethodType.methodType(returnType, argTypes));
        return this;
    }

    /**
     * Cast the incoming arguments to the given MethodType. The casts
     * applied are equivalent to those in MethodHandles.explicitCastArguments(mh, MethodType).
     *
     * @param type the target MethodType
     * @return this Binder
     */
    public Binder cast(MethodType type) {
        add(new Cast(type()), type);
        return this;
    }

    /**
     * Cast the incoming arguments to the given MethodType. The casts
     * applied are equivalent to those in MethodHandle.explicitCastArguments(MethodType).
     *
     * @param returnType the target return type
     * @param argTypes the target argument types
     * @return this Binder
     */
    public Binder cast(Class returnType, Class... argTypes) {
        add(new Cast(type()), MethodType.methodType(returnType, argTypes));
        return this;
    }

    /**
     * Spread a trailing Object[] into the specified argument types.
     *
     * @param spreadTypes the types into which to spread the incoming Object[]
     * @return this Binder
     */
    public Binder spread(Class... spreadTypes) {
        add(new Spread(type(), spreadTypes));
        return this;
    }

    /**
     * Permute the incoming arguments to a new sequence specified by the given values.
     *
     * Arguments may be duplicated or dropped in this sequence.
     *
     * @param reorder the int offsets of the incoming arguments in the desired permutation
     * @return this Binder
     */
    public Binder permute(int... reorder) {
        add(new Permute(type(), reorder));
        return this;
    }

    /**
     * Process the incoming arguments using the given handle, inserting the result
     * as the first argument.
     *
     * @param foldFunction the function that will process the incoming arguments. Its
     *                     signature must match the current signature's arguments exactly.
     * @return this Binder
     */
    public Binder fold(MethodHandle foldFunction) {
        add(new Fold(foldFunction));
        return this;
    }

    /**
     * Apply the tranforms, binding them to a constant value that will
     * propagate back through the chain. The chain's expected return type
     * at that point must be compatible with the given value's type.
     *
     * @param value the constant value to put at the end of the chain
     * @return a handle that has all transforms applied in sequence up to the constant
     */
    public MethodHandle constant(Object value) {
        return invoke(MethodHandles.constant(type().returnType(), value));
    }

    /**
     * Apply the tranforms, binding them to a handle that will simply return its sole
     * argument as its return value. The endpoint signature must have a single argument
     * of the same type as its return type.
     *
     * @return a handle that has all transforms applied in sequence
     */
    public MethodHandle identity() {
        return invoke(MethodHandles.identity(type().parameterType(0)));
    }

    /**
     * Apply the chain of transforms with the target method handle as the final
     * endpoint. Produces a handle that has the transforms in given sequence.
     *
     * @param target the endpoint handle to bind to
     * @return a handle that has all transforms applied in sequence up to endpoint
     */
    public MethodHandle invoke(MethodHandle target) {
        MethodHandle current = target;
        for (Transform t : transforms) {
            MethodHandle previous = current;
            current = t.up(current);
        }
        assert current.type().equals(start) : "incoming " + start + " does not match target " + current.type();

        return current;
    }

    /**
     * Apply the chain of transforms and bind them to a static method specified
     * using the end signature plus the given class and method. The method will
     * be retrieved using the given Lookup and must match the end signature
     * exactly.
     */
    public MethodHandle invoke(MethodHandles.Lookup lookup, Method method) throws NoSuchMethodException, IllegalAccessException {
        return invoke(lookup.unreflect(method));
    }

    /**
     * Apply the chain of transforms and bind them to a static method specified
     * using the end signature plus the given class and name. The method will
     * be retrieved using the given Lookup and must match the end signature
     * exactly.
     */
    public MethodHandle invokeStatic(MethodHandles.Lookup lookup, Class target, String name) throws NoSuchMethodException, IllegalAccessException {
        return invoke(lookup.findStatic(target, name, type()));
    }

    /**
     * Apply the chain of transforms and bind them to a virtual method specified
     * using the end signature plus the given class and name. The method will
     * be retrieved using the given Lookup and must match the end signature
     * exactly.
     */
    public MethodHandle invokeVirtual(MethodHandles.Lookup lookup, Class target, String name) throws NoSuchMethodException, IllegalAccessException {
        return invoke(lookup.findVirtual(target, name, type().dropParameterTypes(0, 1)));
    }

    /**
     * Apply the chain of transforms and bind them to a special method specified
     * using the end signature plus the given class and name. The method will
     * be retrieved using the given Lookup and must match the end signature
     * exactly.
     */
    public MethodHandle invokeSpecial(MethodHandles.Lookup lookup, Class target, String name, Class caller) throws NoSuchMethodException, IllegalAccessException {
        return invoke(lookup.findSpecial(target, name, type().dropParameterTypes(0, 1), caller));
    }

    /**
     * Apply the chain of transforms and bind them to a constructor specified
     * using the end signature plus the given class. The constructor will
     * be retrieved using the given Lookup and must match the end signature's
     * arguments exactly.
     */
    public MethodHandle invokeConstructor(MethodHandles.Lookup lookup, Class target) throws NoSuchMethodException, IllegalAccessException {
        return invoke(lookup.findConstructor(target, type().changeReturnType(void.class)));
    }

    /**
     * Apply the chain of transforms and bind them to an object field retrieval specified
     * using the end signature plus the given class and name. The field must
     * match the end signature's return value and the end signature must take
     * the target class or a subclass as its only argument.
     */
    public MethodHandle getField(MethodHandles.Lookup lookup, Class target, String name) throws NoSuchFieldException, IllegalAccessException {
        return invoke(lookup.findGetter(target, name, type().returnType()));
    }

    /**
     * Apply the chain of transforms and bind them to a static field retrieval specified
     * using the end signature plus the given class and name. The field must
     * match the end signature's return value and the end signature must take
     * no arguments.
     */
    public MethodHandle getStatic(MethodHandles.Lookup lookup, Class target, String name) throws NoSuchFieldException, IllegalAccessException {
        return invoke(lookup.findStaticGetter(target, name, type().returnType()));
    }

    /**
     * Apply the chain of transforms and bind them to an object field assignment specified
     * using the end signature plus the given class and name. The end signature must take
     * the target class or a subclass and the field's type as its arguments, and its return
     * type must be compatible with void.
     */
    public MethodHandle setField(MethodHandles.Lookup lookup, Class target, String name) throws NoSuchFieldException, IllegalAccessException {
        return invoke(lookup.findSetter(target, name, type().returnType()));
    }

    /**
     * Apply the chain of transforms and bind them to an object field assignment specified
     * using the end signature plus the given class and name. The end signature must take
     * the target class or a subclass and the field's type as its arguments, and its return
     * type must be compatible with void.
     */
    public MethodHandle setStatic(MethodHandles.Lookup lookup, Class target, String name) throws NoSuchFieldException, IllegalAccessException {
        return invoke(lookup.findStaticSetter(target, name, type().returnType()));
    }

}
