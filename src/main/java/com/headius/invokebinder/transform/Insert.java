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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

/**
 * An argument insertion transform.
 *
 * Equivalent call: MethodHandles.insertArguments(MethodHandle, int, Object...).
 */
public class Insert extends Transform {

    private final int position;
    private final Class<?>[] types;
    private final Object[] values;

    public Insert(int position, Object... values) {
        this.position = position;
        this.values = values;
        Class<?>[] types = new Class<?>[values.length];
        for (int i = 0; i < values.length; i++) {
            types[i] = values[i].getClass();
        }
        this.types = types;
    }

    public Insert(int position, boolean value) {
        this.position = position;
        this.values = new Object[]{value};
        this.types = new Class[]{boolean.class};
    }

    public Insert(int position, byte value) {
        this.position = position;
        this.values = new Object[]{value};
        this.types = new Class[]{byte.class};
    }

    public Insert(int position, short value) {
        this.position = position;
        this.values = new Object[]{value};
        this.types = new Class[]{short.class};
    }

    public Insert(int position, char value) {
        this.position = position;
        this.values = new Object[]{value};
        this.types = new Class[]{char.class};
    }

    public Insert(int position, int value) {
        this.position = position;
        this.values = new Object[]{value};
        this.types = new Class[]{int.class};
    }

    public Insert(int position, long value) {
        this.position = position;
        this.values = new Object[]{value};
        this.types = new Class[]{long.class};
    }

    public Insert(int position, float value) {
        this.position = position;
        this.values = new Object[]{value};
        this.types = new Class[]{float.class};
    }

    public Insert(int position, double value) {
        this.position = position;
        this.values = new Object[]{value};
        this.types = new Class[]{double.class};
    }

    public Insert(int position, Class<?>[] types, Object... values) {
        this.position = position;
        this.values = values;
        this.types = types;
    }

    public MethodHandle up(MethodHandle target) {
        return MethodHandles.insertArguments(target, position, values);
    }

    public MethodType down(MethodType type) {
        return type.insertParameterTypes(position, types);
    }

    public String toString() {
        return "insert " + Arrays.toString(types()) + " at " + position;
    }

    public String toJava(MethodType incoming) {
        StringBuilder builder = new StringBuilder("handle = MethodHandles.insertArguments(handle, ");
        builder
                .append(position)
                .append(", ");

        // we cast all arguments since natural type will frequently be wrong
        boolean second = false;
        for (int i = 0; i < types.length; i++) {
            if (second) builder.append(", ");
            second = true;

            buildClassCast(builder, types[i]);
            if (types[i].isPrimitive()) {
                buildPrimitiveJava(builder, values[i]);
            } else {
                builder.append("value").append(i + 1);
            }
        }
        builder.append(");");

        return builder.toString();
    }

    private Class<?>[] types() {
        Class<?>[] types = new Class<?>[values.length];
        for (int i = 0; i < types.length; i++) {
            types[i] = values[i].getClass();
        }
        return types;
    }
}
