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

package com.mattbertolini.hermes.internal.reflect;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.Callable;

/**
 * Runs the same checks against every DefaultMethods strategy. A strategy the
 * running JVM does not support is skipped, so run the suite on Java 8, 9-15
 * and 16+ to cover all of them.
 */
@RunWith(Parameterized.class)
public class DefaultMethodsInvokerTest {
    private final Callable<DefaultMethods.Invoker> factory;
    private DefaultMethods.Invoker invoker;
    private LoudGreeter proxy;

    public DefaultMethodsInvokerTest(String name, Callable<DefaultMethods.Invoker> factory) {
        this.factory = factory;
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> strategies() {
        return Arrays.asList(new Object[][]{
                {"Java16Invoker", (Callable<DefaultMethods.Invoker>) DefaultMethods.Java16Invoker::new},
                {"Java9Invoker", (Callable<DefaultMethods.Invoker>) DefaultMethods.Java9Invoker::new},
                {"Java8Invoker", (Callable<DefaultMethods.Invoker>) DefaultMethods.Java8Invoker::new},
        });
    }

    @Before
    public void setUp() {
        try {
            invoker = factory.call();
        } catch (Exception e) {
            Assume.assumeNoException("Strategy not supported on Java " + System.getProperty("java.version"), e);
        }
        proxy = newProxy(LoudGreeter.class);
    }

    @Test
    public void testInvokesDefaultMethodWithNoParameters() throws Throwable {
        Assert.assertEquals("Hello, World", invoker.invokeDefault(proxy, Greeter.class.getMethod("greet")));
    }

    @Test
    public void testDefaultMethodCallsBackIntoProxy() throws Throwable {
        // greet(String) calls the abstract name(), which the proxy's handler answers
        Assert.assertEquals("Hi, World", invoker.invokeDefault(proxy, Greeter.class.getMethod("greet", String.class), "Hi"));
    }

    @Test
    public void testNullArgsTreatedAsNoParameters() throws Throwable {
        Assert.assertEquals("Hello, World", invoker.invokeDefault(proxy, Greeter.class.getMethod("greet"), (Object[]) null));
    }

    @Test
    public void testPassesReferenceParameters() throws Throwable {
        Method method = Greeter.class.getMethod("greet", String.class);
        Assert.assertEquals("Bonjour, World", invoker.invokeDefault(proxy, method, "Bonjour"));
    }

    @Test
    public void testNullReferenceParameterAllowed() throws Throwable {
        Method method = Greeter.class.getMethod("greet", String.class);
        Assert.assertEquals("null, World", invoker.invokeDefault(proxy, method, (Object) null));
    }

    @Test
    public void testUnboxesPrimitiveParametersAndBoxesResult() throws Throwable {
        Method method = Greeter.class.getMethod("add", int.class, int.class);
        Assert.assertEquals(5, invoker.invokeDefault(proxy, method, 2, 3));
    }

    @Test
    public void testVoidDefaultMethodReturnsNull() throws Throwable {
        Assert.assertNull(invoker.invokeDefault(proxy, Greeter.class.getMethod("doNothing")));
    }

    @Test
    public void testVarargsParameterPassedAsArray() throws Throwable {
        Method method = Greeter.class.getMethod("join", String[].class);
        Assert.assertEquals("a-b-c", invoker.invokeDefault(proxy, method, (Object) new String[]{"a", "b", "c"}));
    }

    @Test
    public void testInvokesDefaultMethodInheritedFromSuperinterface() throws Throwable {
        // The proxy implements LoudGreeter; greet() is declared on its superinterface Greeter
        Assert.assertTrue(Arrays.asList(proxy.getClass().getInterfaces()).contains(LoudGreeter.class));
        Assert.assertEquals("Hello, World", invoker.invokeDefault(proxy, Greeter.class.getMethod("greet")));
    }

    @Test
    public void testInvokesDefaultMethodOfNonPublicInterface() throws Throwable {
        // LoudGreeter is package-private
        Assert.assertEquals("HELLO, WORLD!", invoker.invokeDefault(proxy, LoudGreeter.class.getMethod("shout")));
    }

    @Test
    public void testCallsThroughProxyRunDefaultBodies() {
        Assert.assertEquals("Hello, World", proxy.greet());
        Assert.assertEquals("HELLO, WORLD!", proxy.shout());
        Assert.assertEquals(7, proxy.add(3, 4));
    }

    @Test
    public void testCheckedExceptionPropagatesUnwrapped() throws Throwable {
        try {
            invoker.invokeDefault(proxy, Greeter.class.getMethod("failChecked"));
            Assert.fail("Expected IOException");
        } catch (IOException e) {
            Assert.assertEquals("checked", e.getMessage());
        }
    }

    @Test
    public void testUncheckedExceptionPropagatesUnwrapped() throws Throwable {
        try {
            invoker.invokeDefault(proxy, Greeter.class.getMethod("failUnchecked"));
            Assert.fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            Assert.assertEquals("unchecked", e.getMessage());
        }
    }

    @Test
    public void testErrorPropagatesUnwrapped() throws Throwable {
        try {
            invoker.invokeDefault(proxy, Greeter.class.getMethod("failError"));
            Assert.fail("Expected AssertionError");
        } catch (AssertionError e) {
            Assert.assertEquals("error", e.getMessage());
        }
    }

    @Test(expected = NullPointerException.class)
    public void testNullProxyRejected() throws Throwable {
        invoker.invokeDefault(null, Greeter.class.getMethod("greet"));
    }

    @Test(expected = NullPointerException.class)
    public void testNullMethodRejected() throws Throwable {
        invoker.invokeDefault(proxy, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonProxyInstanceRejected() throws Throwable {
        Greeter notAProxy = () -> "Plain";
        invoker.invokeDefault(notAProxy, Greeter.class.getMethod("greet"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAbstractMethodRejected() throws Throwable {
        invoker.invokeDefault(proxy, Greeter.class.getMethod("name"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testObjectMethodRejected() throws Throwable {
        invoker.invokeDefault(proxy, Object.class.getMethod("toString"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMethodFromInterfaceProxyDoesNotImplementRejected() throws Throwable {
        invoker.invokeDefault(proxy, Unrelated.class.getMethod("other"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTooFewArgumentsRejected() throws Throwable {
        invoker.invokeDefault(proxy, Greeter.class.getMethod("add", int.class, int.class), 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTooManyArgumentsRejected() throws Throwable {
        invoker.invokeDefault(proxy, Greeter.class.getMethod("greet"), "extra");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullArgsRejectedWhenParametersExpected() throws Throwable {
        invoker.invokeDefault(proxy, Greeter.class.getMethod("greet", String.class), (Object[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongReferenceArgumentTypeRejected() throws Throwable {
        invoker.invokeDefault(proxy, Greeter.class.getMethod("greet", String.class), 42);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongPrimitiveArgumentTypeRejected() throws Throwable {
        invoker.invokeDefault(proxy, Greeter.class.getMethod("add", int.class, int.class), "2", 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullPrimitiveArgumentRejected() throws Throwable {
        invoker.invokeDefault(proxy, Greeter.class.getMethod("add", int.class, int.class), null, 3);
    }

    // Answers name() and sends default methods called through the proxy back to the invoker under test
    @SuppressWarnings("unchecked")
    private <T> T newProxy(Class<T> type) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.isDefault()) {
                return invoker.invokeDefault(proxy, method, args);
            }
            if (method.getName().equals("name")) {
                return "World";
            }
            throw new UnsupportedOperationException(method.toString());
        };
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    public interface Greeter {
        String name();

        default String greet() {
            return greet("Hello");
        }

        default String greet(String greeting) {
            return greeting + ", " + name();
        }

        default int add(int a, int b) {
            return a + b;
        }

        default void doNothing() {
        }

        default String join(String... parts) {
            return String.join("-", parts);
        }

        default void failChecked() throws IOException {
            throw new IOException("checked");
        }

        default void failUnchecked() {
            throw new IllegalStateException("unchecked");
        }

        default void failError() {
            throw new AssertionError("error");
        }
    }

    interface LoudGreeter extends Greeter {
        default String shout() {
            return greet().toUpperCase(Locale.ROOT) + "!";
        }
    }

    public interface Unrelated {
        default String other() {
            return "other";
        }
    }
}
