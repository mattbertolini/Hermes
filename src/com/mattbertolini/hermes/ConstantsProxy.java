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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.google.gwt.i18n.client.Constants.DefaultBooleanValue;
import com.google.gwt.i18n.client.Constants.DefaultDoubleValue;
import com.google.gwt.i18n.client.Constants.DefaultFloatValue;
import com.google.gwt.i18n.client.Constants.DefaultIntValue;
import com.google.gwt.i18n.client.Constants.DefaultStringArrayValue;
import com.google.gwt.i18n.client.Constants.DefaultStringMapValue;
import com.google.gwt.i18n.client.Constants.DefaultStringValue;
import com.google.gwt.i18n.client.LocalizableResource.Key;
import com.ibm.icu.util.ULocale;

/**
 * 
 * @author Matt Bertolini
 */
public class ConstantsProxy extends AbstractI18nProxy {

    public ConstantsProxy(Class<?> clazz, String lang, ULocale locale, Properties properties) {
        super(clazz, lang, locale, properties);
    }

    @Override
    public Object parse(Method method, Object[] args) {
        return this.parseConstant(method);
    }
    
    protected Object parseConstant(Method method) {
        String constantName;
        Key keyAnnotation = method.getAnnotation(Key.class);
        if(keyAnnotation == null) {
            constantName = method.getName();
        } else {
            constantName = keyAnnotation.value();
        }
        
        Object retVal;
        String constantValue = this.getProperties().getProperty(constantName);
        if(constantValue == null) {
            retVal = this.getDefaultValue(method);
        } else {
            retVal = this.getValue(constantValue, method);
        }
        return retVal;
    }
    
    private Object getValue(String constantValue, Method method) {
        //
        Object retVal;
        Class<?> returnType = method.getReturnType();
        if(int.class.equals(returnType)) {
            retVal = Integer.valueOf(constantValue);
        } else if(float.class.equals(returnType)) {
            retVal = Float.valueOf(constantValue);
        } else if(double.class.equals(returnType)) {
            retVal = Double.valueOf(constantValue);
        } else if(boolean.class.equals(returnType)) {
            retVal = Boolean.valueOf(constantValue);
        } else if(returnType.isArray() && String.class.equals(returnType.getComponentType())) {
            retVal = constantValue.split(", *");
        } else if(Map.class.equals(returnType)) {
            //
            String[] keys = constantValue.split(",");
            Map<String, String> strMap = new HashMap<String, String>(keys.length);
            for(String key : keys) {
                //
                key = key.trim();
                String keyVal = this.getProperties().getProperty(key);
                strMap.put(key, keyVal);
            }
            retVal = strMap;
        } else {
            retVal = constantValue;
        }
        return retVal;
    }
    
    private Object getDefaultValue(Method method) {
        //
        Object retVal;
        Class<?> returnType = method.getReturnType();
        if(int.class.equals(returnType)) {
            //
            DefaultIntValue defaultValue = method.getAnnotation(DefaultIntValue.class);
            retVal = Integer.valueOf(defaultValue.value());
        } else if(float.class.equals(returnType)) {
            DefaultFloatValue defaultValue = method.getAnnotation(DefaultFloatValue.class);
            retVal = Float.valueOf(defaultValue.value());
        } else if(double.class.equals(returnType)) {
            DefaultDoubleValue defaultValue = method.getAnnotation(DefaultDoubleValue.class);
            retVal = Double.valueOf(defaultValue.value());
        } else if(boolean.class.equals(returnType)) {
            DefaultBooleanValue defaultValue = method.getAnnotation(DefaultBooleanValue.class);
            retVal = Boolean.valueOf(defaultValue.value());
        } else if(returnType.isArray() && String.class.equals(returnType.getComponentType())) {
            DefaultStringArrayValue defaultValue = method.getAnnotation(DefaultStringArrayValue.class);
            retVal = defaultValue.value();
        } else if(Map.class.equals(returnType)) {
            //
            DefaultStringMapValue defaultValue = method.getAnnotation(DefaultStringMapValue.class);
            String[] keysValues = defaultValue.value();
            Map<String, String> strMap = new HashMap<String, String>();
            for(int i = 0; i < keysValues.length; i += 2) {
                //
                strMap.put(keysValues[i], keysValues[i + 1]);
            }
            retVal = strMap;
        } else {
            DefaultStringValue defaultValue = method.getAnnotation(DefaultStringValue.class);
            retVal = defaultValue.value();
        }
        return retVal;
    }
}
