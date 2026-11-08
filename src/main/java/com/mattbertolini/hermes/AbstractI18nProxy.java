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

import com.ibm.icu.util.ULocale;
import com.mattbertolini.hermes.internal.reflect.DefaultMethods;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractI18nProxy implements InvocationHandler {
    /**
     * Constant for an empty array so we can avoid passing null for proxied methods without arguements.
     */
    private static final @Nullable Object[] NO_ARGS = new Object[0];

    private final Class<?> clazz;
    private final ResourceList resources;
    private final ULocale locale;
    private final Map<Method, MethodHandler> handlers;
    
    protected AbstractI18nProxy(Class<?> clazz, ULocale locale, ResourceList resources) {
        this.clazz = clazz;
        this.resources = resources;
        this.locale = locale;
        handlers = new ConcurrentHashMap<>();
    }
    
    @Override
    public final @Nullable Object invoke(Object proxy, Method method, @Nullable Object @Nullable [] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return invokeObjectMethod(proxy, method, normalizeArgs(args));
        }
        if (method.isDefault()) {
            return DefaultMethods.invokeDefault(proxy, method, args);
        }
        return handlerFor(method).handle(normalizeArgs(args));
    }

    protected ResourceList getResources() {
        return this.resources;
    }
    
    protected ULocale getLocale() {
        return this.locale;
    }
    
    protected Class<?> getI18nClass() {
        return this.clazz;
    }

    protected final MethodHandler handlerFor(Method method) {
        return handlers.computeIfAbsent(method, this::createHandler);
    }

    protected String keyFor(Method method) {
        return Keys.keyFor(this.clazz, method);
    }

    protected abstract MethodHandler createHandler(Method method);

    @Override
    public String toString() {
        return this.getClass().getName() + ": " + this.clazz.getName();
    }

    private Object invokeObjectMethod(Object proxy, Method method, @Nullable Object[] args) {
        switch (method.getName()) {
            case "equals":
                return proxy == args[0];
            case "hashCode":
                return System.identityHashCode(proxy);
            case "toString":
                return this.toString();
            default:
                throw new UnsupportedOperationException("Unsupported method: " + method);
        }
    }

    private static @Nullable Object[] normalizeArgs(@Nullable Object @Nullable [] args) {
        return args == null ? NO_ARGS : args;
    }
}
