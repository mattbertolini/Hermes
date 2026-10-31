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

import com.google.gwt.i18n.client.ConstantsWithLookup;
import com.ibm.icu.util.ULocale;

import java.lang.reflect.Method;
import java.util.MissingResourceException;
import java.util.Properties;

public class ConstantsWithLookupProxy extends ConstantsProxy {

    public ConstantsWithLookupProxy(Class<?> clazz, ULocale locale, Properties properties) {
        super(clazz, locale, properties);
    }

    @Override
    protected MethodHandler createHandler(Method method) {
        if (method.getDeclaringClass() == ConstantsWithLookup.class) {
            return this::lookup;
        }
        return super.createHandler(method);
    }

    private Object lookup(Object[] args) {
        String name = (String) args[0];
        if (name == null || name.isEmpty()) {
            throw new MissingResourceException("Null or empty method name given.", getI18nClass().getName(), name);
        }
        return handlerFor(findConstantMethod(name)).handle(null);
    }

    private Method findConstantMethod(String name) {
        try {
            return getI18nClass().getMethod(name);
        } catch (NoSuchMethodException e) {
            throw new MissingResourceException("No property with given name found.", getI18nClass().getName(), name);
        }
    }
}
