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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Properties;

import com.ibm.icu.util.ULocale;

/**
 * 
 * @author Matt Bertolini
 */
public abstract class AbstractI18nProxy implements InvocationHandler {
    private Class<?> clazz;
    private Properties properties;
    private ULocale locale;
    
    public AbstractI18nProxy(Class<?> clazz, ULocale locale, Properties properties) {
        this.clazz = clazz;
        this.properties = properties;
        this.locale = locale;
    }
    
    public abstract Object parse(Method method, Object[] args);
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object retVal;
        if(method.getDeclaringClass().equals(Object.class) 
                && method.getName().equals("toString") 
                && method.getParameterTypes().length == 0 
                && method.getReturnType().equals(String.class)) {
            retVal = this.toString();
        } else {
            retVal = this.parse(method, args);
        }
        return retVal;
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

    @Override
    public String toString() {
        return this.getClass().getName() + ": " + this.clazz.getName();
    }
}
