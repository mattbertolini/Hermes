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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.gwt.i18n.client.LocalizableResource.Key;
import com.google.gwt.i18n.client.Messages.AlternateMessage;
import com.google.gwt.i18n.client.Messages.DefaultMessage;
import com.google.gwt.i18n.client.Messages.PluralCount;
import com.google.gwt.i18n.client.Messages.PluralText;
import com.google.gwt.i18n.client.Messages.Select;
import com.google.gwt.i18n.client.PluralRule;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ULocale;

/**
 * 
 * @author Matt Bertolini
 */
public class MessagesProxy extends AbstractI18nProxy {
    private static final char OPEN_BRACKET = '[';
    private static final char CLOSE_BRACKET = ']';
    private static final char SEPARATOR = '|';
    
    private PluralRules pluralRules;
    
    public MessagesProxy(Class<?> clazz, String lang, ULocale locale, Properties properties) {
        super(clazz, lang, locale, properties);
        this.pluralRules = PluralRules.forLocale(this.getLocale());
    }
    
    @Override
    public Object parse(Method method, Object[] args) {
        return this.parseMessage(method, args);
    }

    private Object parseMessage(Method method, Object[] args) {
        String messageName;
        Key keyAnnotation = method.getAnnotation(Key.class);
        if(keyAnnotation == null) {
            messageName = method.getName();
        } else {
            messageName = keyAnnotation.value();
        }
        
        List<String> formsNames = new LinkedList<String>();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Class<?>[] parameterTypes = method.getParameterTypes();
        for(int i = 0; i < parameterTypes.length; i++) {
            Annotation[] annotations = parameterAnnotations[i];
            Class<?> type = parameterTypes[i];
            for(Annotation annotation : annotations) {
                if(PluralCount.class.isAssignableFrom(annotation.annotationType()) 
                        && (int.class.isAssignableFrom(type) 
                                || Integer.class.isAssignableFrom(type)
                                || short.class.isAssignableFrom(type)
                                || Short.class.isAssignableFrom(type))) {
                    Number num = (Number) args[i];
                    Plural plural;
                    // Check for a custom plural rule.
                    PluralCount pc = (PluralCount) annotation;
                    Class<? extends PluralRule> pluralRuleClass = pc.value();
                    if(!pluralRuleClass.isInterface() && PluralRule.class.isAssignableFrom(pluralRuleClass)) {
                        PluralRule customRule = this.instantiateCustomPluralRuleClass(pluralRuleClass);
                        plural = CustomPlural.fromNumber(customRule, num.intValue());
                    } else {
                        plural = GwtPlural.fromNumber(this.pluralRules, num.doubleValue());
                    }
                    formsNames.add(plural.getGwtValue());
                } else if(Select.class.isAssignableFrom(annotation.annotationType())) {
                    if(Enum.class.isAssignableFrom(type)) {
                        Enum<?> enumConstant = (Enum<?>) args[i];
                        String name;
                        if(enumConstant == null) {
                            name = "other";
                        } else {
                            name = enumConstant.name();
                        }
                        formsNames.add(name);
                    } else if(String.class.isAssignableFrom(type)) {
                        String str = (String) args[i];
                        if(str == null) {
                            str = "other";
                        }
                        formsNames.add(str);
                    } else if(type.isPrimitive() 
                            && (int.class.isAssignableFrom(type) 
                                    || long.class.isAssignableFrom(type)
                                    || float.class.isAssignableFrom(type) 
                                    || short.class.isAssignableFrom(type)
                                    || double.class.isAssignableFrom(type))) {
                        Number num = (Number) args[i];
                        formsNames.add(num.toString());
                    }
                }
            }
        }
        
        String patternName = this.buildPatternName(messageName, formsNames);
        String pattern = this.getProperties().getProperty(patternName);
        if(pattern == null) {
            Map<String, String> altMsgMap = this.buildAlternateMessageMap(messageName, method);
            pattern = altMsgMap.get(patternName);
            if(pattern == null) {
                pattern = this.getProperties().getProperty(messageName);
                if(pattern == null) {
                    DefaultMessage defaultMessage = method.getAnnotation(DefaultMessage.class);
                    if(defaultMessage != null) {
                        pattern = defaultMessage.value();
                    } else {
                        throw new RuntimeException("No message found for key " + messageName);
                    }
                }
            }
        }
         
        MessageFormat formatter = new MessageFormat(pattern, this.getLocale());
        Object retVal;
        if(method.getReturnType().equals(SafeHtml.class)) {
            Object[] safeArgs = null;
            if(args != null) {
                safeArgs = new Object[args.length];
                for(int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    Class<?> argType = parameterTypes[i];
                    if(SafeHtml.class.isAssignableFrom(argType)) {
                        SafeHtml sh = (SafeHtml) arg;
                        safeArgs[i] = sh.asString();
                    } else if(Number.class.isAssignableFrom(argType) 
                            || Date.class.isAssignableFrom(argType)) {
                        // Because of the subformat pattern of dates and 
                        // numbers, we cannot escape them.
                        safeArgs[i] = arg;
                    } else {
                        safeArgs[i] = SafeHtmlUtils.htmlEscape(arg.toString());
                    }
                }
            }
            String formattedString = formatter.format(safeArgs, new StringBuffer(), null).toString();
            // Would rather use fromSafeConstant() but doesn't work on server.
            retVal = SafeHtmlUtils.fromTrustedString(formattedString);
        } else {
            retVal = formatter.format(args, new StringBuffer(), null).toString();
        }
        
        return retVal;
    }
    
    /**
     * Instantiates the plural rule class given inside the PluralCount 
     * annotation. The pluralRule class that is instantiated is based on the 
     * language originally given to the library. If that language is not found, 
     * defaults to the given class.
     * 
     * @param clazz The PluralRule class
     * @return An instantiated PluralRule class
     */
    private PluralRule instantiateCustomPluralRuleClass(Class<? extends PluralRule> clazz) {
        PluralRule retVal = null;
        try {
            String pluralClassName = clazz.getName() + "_" + this.getLocale().getLanguage();
            try {
                Class<?> pClass = Class.forName(pluralClassName);
                retVal = (PluralRule) pClass.newInstance();
            } catch (ClassNotFoundException e) {
                retVal = clazz.newInstance();
            }
        } catch (InstantiationException e) {
            throw new RuntimeException("Could not instantiate custom PluralRule class. " 
                    + "Make sure the class is not an abstract class or interface and has a default constructor.", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not instantiate custom Plural Rule class.", e);
        }
        return retVal;
    }
    
    private String buildPatternName(String baseName, List<String> pluralNames) {
        String retVal;
        StringBuilder pluralStr = new StringBuilder();
        boolean defaultMessage = true;
        if(pluralNames != null && !pluralNames.isEmpty()) {
            pluralStr.append(OPEN_BRACKET);
            boolean first = true;
            for(String name : pluralNames) {
                if(!name.equals(GwtPlural.OTHER.getGwtValue())) {
                    defaultMessage = false;
                }
                if(first) {
                    first = false;
                } else {
                    pluralStr.append(SEPARATOR);
                }
                pluralStr.append(name);
            }
            pluralStr.append(CLOSE_BRACKET);
        }
        if(defaultMessage) {
            retVal = baseName;
        } else {
            retVal = baseName + pluralStr.toString();
        }
        return retVal;
    }
    
    private Map<String, String> buildAlternateMessageMap(String baseName, Method method) {
        Map<String, String> retMap = new LinkedHashMap<String, String>();
        AlternateMessage altMsgAnnotation = method.getAnnotation(AlternateMessage.class);
        PluralText pluralTextAnnotation = method.getAnnotation(PluralText.class);
        String[] values = new String[0];
        if(altMsgAnnotation != null) {
            values = altMsgAnnotation.value();
        } else if(pluralTextAnnotation != null) {
            // Fall back on the PluralText if no AlternateMessage found.
            values = pluralTextAnnotation.value();
        }
        for(int i = 0; i < values.length; i += 2) {
            retMap.put(baseName + OPEN_BRACKET + values[i] + CLOSE_BRACKET, values[i + 1]);
        }
        return retMap;
    }
}
