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

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class DefaultMethodsTest {

    @Test
    public void testSelectsStrategyForRuntimeVersion() {
        Class<?> expected;
        int version = javaVersion();
        if (version >= 16) {
            expected = DefaultMethods.Java16Invoker.class;
        } else if (version >= 9) {
            expected = DefaultMethods.Java9Invoker.class;
        } else {
            expected = DefaultMethods.Java8Invoker.class;
        }
        Assert.assertEquals(expected, DefaultMethods.runtimeInvoker().getClass());
    }

    @Test
    public void testForRuntimeMatchesCachedStrategy() {
        Assert.assertEquals(DefaultMethods.runtimeInvoker().getClass(), DefaultMethods.Invoker.forRuntime().getClass());
    }

    @Test
    public void testJava16InvokerUnsupportedBeforeJava16() {
        assertSupported(javaVersion() >= 16, DefaultMethods.Java16Invoker::new);
    }

    @Test
    public void testJava9InvokerUnsupportedBeforeJava9() {
        assertSupported(javaVersion() >= 9, DefaultMethods.Java9Invoker::new);
    }

    @Test
    public void testJava8InvokerUnsupportedFromJava16() {
        // Java 9-15 still allow the Lookup constructor (with an illegal-access warning)
        assertSupported(javaVersion() < 16, DefaultMethods.Java8Invoker::new);
    }

    @Test
    public void testInvokeDefaultUsesRuntimeStrategy() throws Throwable {
        Named proxy = newProxy();
        Assert.assertEquals("Hello, World", DefaultMethods.invokeDefault(proxy, Named.class.getMethod("greet")));
    }

    @Test
    public void testInvokeDefaultWithArguments() throws Throwable {
        Named proxy = newProxy();
        Method method = Named.class.getMethod("greet", String.class);
        Assert.assertEquals("Hi, World", DefaultMethods.invokeDefault(proxy, method, "Hi"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvokeDefaultRejectsAbstractMethod() throws Throwable {
        DefaultMethods.invokeDefault(newProxy(), Named.class.getMethod("name"));
    }

    @Test
    public void testCannotInstantiate() throws Exception {
        Constructor<DefaultMethods> constructor = DefaultMethods.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            Assert.fail("Expected UnsupportedOperationException");
        } catch (InvocationTargetException e) {
            Assert.assertTrue(e.getCause() instanceof UnsupportedOperationException);
        }
    }

    private interface InvokerFactory {
        DefaultMethods.Invoker create() throws Exception;
    }

    private static void assertSupported(boolean expectedSupported, InvokerFactory factory) {
        boolean supported;
        try {
            factory.create();
            supported = true;
        } catch (Exception e) {
            supported = false;
        }
        Assert.assertEquals(expectedSupported, supported);
    }

    // "1.8" on Java 8, then "9", "10", ... from Java 9
    private static int javaVersion() {
        String version = System.getProperty("java.specification.version");
        return version.startsWith("1.") ? Integer.parseInt(version.substring(2)) : Integer.parseInt(version);
    }

    private static Named newProxy() {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.isDefault()) {
                return DefaultMethods.invokeDefault(proxy, method, args);
            }
            return "World";
        };
        return (Named) Proxy.newProxyInstance(Named.class.getClassLoader(), new Class<?>[]{Named.class}, handler);
    }

    public interface Named {
        String name();

        default String greet() {
            return greet("Hello");
        }

        default String greet(String greeting) {
            return greeting + ", " + name();
        }
    }
}
