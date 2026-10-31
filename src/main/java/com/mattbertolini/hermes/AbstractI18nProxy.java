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

import com.google.gwt.i18n.client.LocalizableResource.Key;
import com.ibm.icu.util.ULocale;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractI18nProxy implements InvocationHandler {
    private final Class<?> clazz;
    private final Properties properties;
    private final ULocale locale;
    private final Map<Method, MethodHandler> handlers;
    
    public AbstractI18nProxy(Class<?> clazz, ULocale locale, Properties properties) {
        this.clazz = clazz;
        this.properties = properties;
        this.locale = locale;
        handlers = new ConcurrentHashMap<>();
    }
    
    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return invokeObjectMethod(proxy, method, args);
        }
        return handlerFor(method).handle(args);
    }
    
    protected Properties getProperties() {
        return this.properties;
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

    protected abstract MethodHandler createHandler(Method method);

    protected static String keyFor(Method method) {
        return Optional.ofNullable(method.getAnnotation(Key.class))
                .map(Key::value)
                .orElse(method.getName());
    }

    @Override
    public String toString() {
        return this.getClass().getName() + ": " + this.clazz.getName();
    }

    private Object invokeObjectMethod(Object proxy, Method method, Object[] args) {
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
}
