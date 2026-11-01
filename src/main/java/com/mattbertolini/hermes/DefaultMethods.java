/*
 * Hermes - GWT Server-side I18N Library
 * Copyright (C) 2011-2026  Matt Bertolini
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

package com.mattbertolini.hermes;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

/**
 * Invokes interface default methods on proxy instances, the same as Java 16's
 * {@code InvocationHandler.invokeDefault}, on every Java version from 8 on:
 * <ul>
 *     <li>Java 16+ calls {@code InvocationHandler.invokeDefault} itself.</li>
 *     <li>Java 9-15 uses a lookup from {@code MethodHandles.privateLookupIn}.</li>
 *     <li>Java 8 uses a private-access lookup built with the {@code Lookup} constructor.</li>
 * </ul>
 * The first strategy the running JVM supports is chosen once, when this class
 * loads.
 * <p>
 * Limitation: on Java 16+, {@code InvocationHandler.invokeDefault} checks that
 * its caller (Hermes) can access the method's interface, so default methods of
 * a non-public interface outside {@code com.mattbertolini.hermes} throw
 * {@code IllegalAccessException}. Java 8-15 allow them. Make i18n interfaces
 * with default methods public.
 */
final class DefaultMethods {
    private static final Invoker INVOKER = Invoker.forRuntime();

    private DefaultMethods() {
        throw new UnsupportedOperationException("DefaultMethods is a utility class and cannot be instantiated");
    }

    /**
     * Invokes the specified default method on the given proxy instance with
     * the given parameters.
     *
     * @param proxy  the proxy instance on which the default method is invoked
     * @param method the default method declared in a proxy interface of the
     *               proxy class, or inherited from its superinterface
     * @param args   the parameters used for the method invocation; may be null
     *               if the method takes no parameters
     * @return the value returned from the method invocation
     * @throws IllegalArgumentException if proxy is not a proxy instance, the
     *                                  method is not a default method of one of its interfaces, or the
     *                                  arguments do not match the method's parameters
     * @throws NullPointerException     if proxy or method is null
     * @throws IllegalAccessException   on Java 16+, if the method's interface is not
     *                                  accessible to Hermes (see the class documentation)
     * @throws Throwable                anything thrown by the default method
     */
    static Object invokeDefault(Object proxy, Method method, Object... args) throws Throwable {
        return runtimeInvoker().invokeDefault(proxy, method, args);
    }

    /**
     * The strategy chosen for the running JVM.
     */
    static Invoker runtimeInvoker() {
        return INVOKER;
    }

    @FunctionalInterface
    interface Invoker {
        Object invokeDefault(Object proxy, Method method, Object... args) throws Throwable;

        /**
         * Picks the newest strategy the running JVM supports.
         */
        static Invoker forRuntime() {
            try {
                return new Java16Invoker();
            } catch (ReflectiveOperationException | RuntimeException java16Unsupported) {
                try {
                    return new Java9Invoker();
                } catch (ReflectiveOperationException | RuntimeException java9Unsupported) {
                    try {
                        return new Java8Invoker();
                    } catch (ReflectiveOperationException | RuntimeException java8Unsupported) {
                        IllegalStateException e = new IllegalStateException(
                                "No supported way to invoke default methods on this JVM", java8Unsupported);
                        e.addSuppressed(java16Unsupported);
                        e.addSuppressed(java9Unsupported);
                        throw e;
                    }
                }
            }
        }
    }

    /**
     * Java 16+: delegates to the built-in {@code InvocationHandler.invokeDefault}.
     */
    static final class Java16Invoker implements Invoker {
        private final MethodHandle invokeDefault;

        Java16Invoker() throws ReflectiveOperationException {
            // Looked up reflectively because Hermes compiles against Java 8
            invokeDefault = MethodHandles.lookup()
                    .findStatic(InvocationHandler.class, "invokeDefault",
                            MethodType.methodType(Object.class, Object.class, Method.class, Object[].class))
                    .asFixedArity();
        }

        @Override
        public Object invokeDefault(Object proxy, Method method, Object... args) throws Throwable {
            return (Object) invokeDefault.invokeExact(proxy, method, args);
        }
    }

    /**
     * Shared by the Java 8 and 9-15 strategies: checks the arguments the way
     * {@code InvocationHandler.invokeDefault} does, then invokes the method
     * through a lookup with private access to its declaring interface.
     */
    abstract static class LookupInvoker implements Invoker {
        private static final Object[] NO_ARGS = new Object[0];

        @Override
        public final Object invokeDefault(Object proxy, Method method, Object... args) throws Throwable {
            Objects.requireNonNull(proxy, "proxy");
            Objects.requireNonNull(method, "method");
            Object[] actualArgs = args == null ? NO_ARGS : args;
            checkInvocation(proxy, method, actualArgs);
            Class<?> declaringClass = method.getDeclaringClass();
            return lookupFor(declaringClass)
                    .unreflectSpecial(method, declaringClass)
                    .bindTo(proxy)
                    .invokeWithArguments(actualArgs);
        }

        abstract MethodHandles.Lookup lookupFor(Class<?> declaringClass) throws Throwable;

        private static void checkInvocation(Object proxy, Method method, Object[] args) {
            if (!Proxy.isProxyClass(proxy.getClass())) {
                throw new IllegalArgumentException("'proxy' is not a proxy instance");
            }
            if (!method.isDefault()) {
                throw new IllegalArgumentException("\"" + method + "\" is not a default method");
            }
            if (!method.getDeclaringClass().isInstance(proxy)) {
                throw new IllegalArgumentException("\"" + method + "\" is not a method declared in a proxy interface of "
                        + proxy.getClass().getName());
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (args.length != parameterTypes.length) {
                throw new IllegalArgumentException("wrong number of parameters: expected " + parameterTypes.length
                        + " but was " + args.length);
            }
            for (int i = 0; i < args.length; i++) {
                if (!isAssignable(parameterTypes[i], args[i])) {
                    throw new IllegalArgumentException("argument " + i + " cannot be assigned to " + parameterTypes[i].getName());
                }
            }
        }

        private static boolean isAssignable(Class<?> parameterType, Object arg) {
            if (!parameterType.isPrimitive()) {
                return arg == null || parameterType.isInstance(arg);
            }
            // Primitives need a non-null instance of their wrapper type (int -> Integer)
            Class<?> wrapperType = MethodType.methodType(parameterType).wrap().returnType();
            return wrapperType.isInstance(arg);
        }
    }

    /**
     * Java 9-15: a private lookup from {@code MethodHandles.privateLookupIn}.
     */
    static final class Java9Invoker extends LookupInvoker {
        private final MethodHandle privateLookupIn;

        Java9Invoker() throws ReflectiveOperationException {
            // Looked up reflectively because Hermes compiles against Java 8
            privateLookupIn = MethodHandles.lookup().findStatic(MethodHandles.class, "privateLookupIn",
                    MethodType.methodType(MethodHandles.Lookup.class, Class.class, MethodHandles.Lookup.class));
        }

        @Override
        MethodHandles.Lookup lookupFor(Class<?> declaringClass) throws Throwable {
            return (MethodHandles.Lookup) privateLookupIn.invokeExact(declaringClass, MethodHandles.lookup());
        }
    }

    /**
     * Java 8: a lookup with private access built with the non-public
     * {@code Lookup(Class, int)} constructor. Later Java versions block this.
     */
    static final class Java8Invoker extends LookupInvoker {
        // Java 8's Lookup.ALL_MODES is private: PUBLIC | PRIVATE | PROTECTED | PACKAGE
        private static final int ALL_MODES = MethodHandles.Lookup.PUBLIC | MethodHandles.Lookup.PRIVATE
                | MethodHandles.Lookup.PROTECTED | MethodHandles.Lookup.PACKAGE;

        private final Constructor<MethodHandles.Lookup> lookupConstructor;

        // Lookup(Class, int) exists on Java 8-13 only (Java 14+ has Lookup(Class, Class, int)), so an
        // IDE using a newer JDK can't resolve it. Its absence there is expected: forRuntime() falls back.
        @SuppressWarnings("JavaReflectionMemberAccess")
        Java8Invoker() throws ReflectiveOperationException {
            lookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
            lookupConstructor.setAccessible(true);
        }

        @Override
        MethodHandles.Lookup lookupFor(Class<?> declaringClass) throws ReflectiveOperationException {
            return lookupConstructor.newInstance(declaringClass, ALL_MODES);
        }
    }
}
