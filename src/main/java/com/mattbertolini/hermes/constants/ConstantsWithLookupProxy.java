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

package com.mattbertolini.hermes.constants;

import com.google.gwt.i18n.client.ConstantsWithLookup;
import com.ibm.icu.util.ULocale;
import com.mattbertolini.hermes.MethodHandler;
import com.mattbertolini.hermes.ResourceList;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.MissingResourceException;

public class ConstantsWithLookupProxy extends ConstantsProxy {
    private static final @Nullable Object[] NO_ARGS = new Object[0];

    public ConstantsWithLookupProxy(Class<?> clazz, ULocale locale, ResourceList resources) {
        super(clazz, locale, resources);
    }

    @Override
    protected MethodHandler createHandler(Method method) {
        if (method.getDeclaringClass() == ConstantsWithLookup.class) {
            return args -> lookup(method, args);
        }
        return super.createHandler(method);
    }

    private Object lookup(Method lookupMethod, @Nullable Object[] args) {
        String name = (String) args[0];
        if (name == null || name.isEmpty()) {
            throw new MissingResourceException("Null or empty method name given.", getI18nClass().getName(), name);
        }
        Method constantMethod = findConstantMethod(name);
        if (constantMethod.getReturnType() != lookupMethod.getReturnType()) {
            throw new MissingResourceException("Property " + name + " is not of type "
                    + lookupMethod.getReturnType().getSimpleName() + ".", getI18nClass().getName(), name);
        }
        return handlerFor(constantMethod).handle(NO_ARGS);
    }

    private Method findConstantMethod(String name) {
        Method method;
        try {
            method = getI18nClass().getMethod(name);
        } catch (NoSuchMethodException e) {
            throw new MissingResourceException("No property with given name found.", getI18nClass().getName(), name);
        }
        // Default and static methods have bodies, not property values
        if (!Modifier.isAbstract(method.getModifiers())) {
            throw new MissingResourceException("Method " + name + " is not a constant.", getI18nClass().getName(), name);
        }
        return method;
    }
}
