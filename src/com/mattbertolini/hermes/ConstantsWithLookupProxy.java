/*
 * Hermes - GWT Server-side I18N Library
 * Copyright (C) 2011  Matt Bertolini
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

import java.lang.reflect.Method;
import java.util.MissingResourceException;
import java.util.Properties;

import com.ibm.icu.util.ULocale;

/**
 * 
 * @author Matt Bertolini
 */
public class ConstantsWithLookupProxy extends ConstantsProxy {

    public ConstantsWithLookupProxy(Class<?> clazz, ULocale locale, Properties properties) {
        super(clazz, locale, properties);
    }

    @Override
    public Object parse(Method method, Object[] args) {
        Object retVal;
        if(this.getI18nClass().equals(method.getDeclaringClass()) && (args == null || args.length == 0)) {
            retVal = this.parseConstant(method);
        } else {
            String name = (String) args[0];
            try {
                if(name == null || name.isEmpty()) {
                    throw new MissingResourceException("Null or empty method name given.", this.getI18nClass().getName(), name);
                }
                Method m = this.getI18nClass().getMethod(name, new Class<?>[0]);
                retVal = this.parseConstant(m);
            } catch (SecurityException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new MissingResourceException("No property with given name found.", this.getI18nClass().getName(), name);
            }
        }
        return retVal;
    }
}
