/*
 * Copyright 2012-2014 headius.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.headius.invokebinder.transform;

import com.headius.invokebinder.Binder;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

/**
 * An argument-boxing transform with a fixed incoming size.
 *
 * Equivalent call: MethodHandle.asCollector(Class, int)
 */
public class Collect extends Transform {

    private final MethodType source;
    private final int index;
    private final int count;
    private final Class<?> arrayType;

    public Collect(MethodType source, int index, Class<?> arrayType) {
        this.source = source;
        this.index = index;
        this.count = source.parameterCount() - index;
        this.arrayType = arrayType;
    }

    public Collect(MethodType source, int index, int count, Class<?> arrayType) {
        this.source = source;
        this.index = index;
        this.count = count;
        this.arrayType = arrayType;
    }

    public MethodHandle up(MethodHandle target) {
        if (onlyTail()) {
            // fast path for tail args
            return target.asCollector(arrayType, count);
        } else {
            Permutes permutes = buildPermutes(source, target.type());

            Binder binder = preparePermuteBinder(permutes);
            return binder.invoke(target);
        }
    }

    private Binder preparePermuteBinder(Permutes permutes) {
        return Binder.from(source)
                .permute(permutes.movePermute)
                .collect(source.parameterCount() - count, arrayType)
                .permute(permutes.moveBackPermute);
    }

    public MethodType down(MethodType type) {
        assertTypesAreCompatible();

        return type
                .dropParameterTypes(index, index + count)
                .insertParameterTypes(index, arrayType);
    }

    private void assertTypesAreCompatible() {
        Class<?> componentType = arrayType.getComponentType();
        for (int i = index; i < index + count; i++) {
            Class<?> in = source.parameterType(i);
            assert in.isAssignableFrom(componentType)
                    : "incoming type " + in.getName() + " not compatible with " + componentType.getName() + "[]";
        }
    }

    public String toString() {
        return "collect at " + index + " into " + arrayType.getName();
    }

    public String toJava(MethodType incoming) {
        StringBuilder builder = new StringBuilder();
        if (onlyTail()) {
            builder.append("handle = handle.asCollector(");
            buildClassArgument(builder, arrayType);
            builder
                    .append(", ")
                    .append(count)
                    .append(");");
        } else {
            Permutes permutes = buildPermutes(source, incoming);

            Binder binder = preparePermuteBinder(permutes);
            return binder.toJava(incoming);
        }
        return builder.toString();
    }

    private boolean onlyTail() {
        return index + count == source.parameterCount();
    }

    private static class Permutes {
        private final int[] movePermute;
        private final int[] moveBackPermute;

        private Permutes(MethodType source, MethodType target, int index, int count) {
            movePermute = new int[source.parameterCount()];
            moveBackPermute = new int[target.parameterCount()];
            // pre
            for (int i = 0; i < index; i++) {
                movePermute[i] = i;
                moveBackPermute[i] = i;
            }

            // post
            int shifted = 0;
            for (int i = index; i + count < movePermute.length; i++, shifted++) movePermute[i] = i + count;
            for (int i = index; i + 1 < moveBackPermute.length; i++) moveBackPermute[i + 1] = i;

            // collected args
            for (int i = index + shifted; i < movePermute.length; i++) movePermute[i] = i - shifted;
            moveBackPermute[index] = moveBackPermute.length - 1;
        }
    }

    private Permutes buildPermutes(MethodType source, MethodType target) {
        return new Permutes(source, target, index, count);
    }
}
