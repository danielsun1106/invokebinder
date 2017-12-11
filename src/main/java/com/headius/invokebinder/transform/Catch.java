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

/**
 * An exception-handling transform.
 *
 * Equivalent call: MethodHandles.catchException(MethodHandle, Class, MethodHandle).
 */
public class Catch extends Transform {

    public static final String EXCEPTION_HANDLER_JAVA = "<exception handler>";
    private final Class<? extends Throwable> throwable;
    private final MethodHandle function;

    public Catch(Class<? extends Throwable> throwable, MethodHandle function) {
        this.throwable = throwable;
        this.function = function;
    }

    public MethodHandle up(MethodHandle target) {
        return MethodHandles.catchException(target, throwable, function);
    }

    public MethodType down(MethodType type) {
        return type;
    }

    public String toString() {
        return "catch exception type " + throwable + " using " + function;
    }

    public String toJava(MethodType incoming) {
        StringBuilder builder = new StringBuilder("handle = MethodHandles.catchException(handle, ");
        buildClassArgument(builder, throwable);
        builder.append(", ").append(EXCEPTION_HANDLER_JAVA).append(");");
        return builder.toString();
    }
}
