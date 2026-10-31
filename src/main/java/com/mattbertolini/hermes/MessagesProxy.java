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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class MessagesProxy extends AbstractI18nProxy {
    private static final Set<Class<?>> PLURAL_COUNT_TYPES = new HashSet<>(Arrays.<Class<?>>asList(
            int.class, Integer.class, short.class, Short.class));
    private static final Set<Class<?>> SELECT_NUMBER_TYPES = new HashSet<>(Arrays.<Class<?>>asList(
            int.class, long.class, float.class, short.class, double.class));
    private static final char OPEN_BRACKET = '[';
    private static final char CLOSE_BRACKET = ']';
    private static final char SEPARATOR = '|';
    
    private final PluralRules pluralRules;
    
    public MessagesProxy(Class<?> clazz, ULocale locale, Properties properties) {
        super(clazz, locale, properties);
        pluralRules = PluralRules.forLocale(this.getLocale());
    }

    @Override
    protected MethodHandler createHandler(Method method) {
        return new MessageHandler(method);
    }

    @FunctionalInterface
    private interface AlternativeFormSelector {
        String select(Object[] args);
    }

    @FunctionalInterface
    private interface MessageRenderer {
        Object render(MessageFormat formatter, Object[] args);
    }

    private final class MessageHandler implements MethodHandler {
        private final String key;
        private final List<AlternativeFormSelector> selectors;
        private final Map<String, String> alternateMessages;
        private final String defaultMessage;
        private final MessageRenderer renderer;

        public MessageHandler(Method method) {
            key = keyFor(method);
            selectors = buildSelectors(method);
            alternateMessages = buildAlternateMessages(key, method);
            defaultMessage = Optional.ofNullable(method.getAnnotation(DefaultMessage.class))
                    .map(DefaultMessage::value)
                    .orElse(null);
            renderer = method.getReturnType() == SafeHtml.class ? MessagesProxy::formatSafeHtml : MessagesProxy::formatPlain;
        }

        @Override
        public Object handle(Object[] args) {
            List<String> forms = selectors.stream()
                    .map(alternativeFormSelector -> alternativeFormSelector.select(args))
                    .collect(toList());
            String patternName = buildPatternName(key, forms);
            MessageFormat formatter = new MessageFormat(findPattern(patternName), getLocale());
            return renderer.render(formatter, args);
        }

        private String findPattern(String patternName) {
            return Stream.<Supplier<String>>of(
                            () -> getProperties().getProperty(patternName),
                            () -> alternateMessages.get(patternName),
                            () -> getProperties().getProperty(key),
                            () -> defaultMessage)
                    .map(Supplier::get)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No message found for key " + key));
        }
    }

    private List<AlternativeFormSelector> buildSelectors(Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Class<?>[] parameterTypes = method.getParameterTypes();
        List<AlternativeFormSelector> selectors = new ArrayList<>();
        for (int i = 0; i < parameterTypes.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                AlternativeFormSelector selector = selectorFor(annotation, parameterTypes[i], i);
                if (selector != null) {
                    selectors.add(selector);
                }
            }
        }
        return selectors;
    }

    private AlternativeFormSelector selectorFor(Annotation annotation, Class<?> type, int index) {
        if (annotation instanceof PluralCount && isPluralCountType(type)) {
            Class<? extends PluralRule> ruleClass = ((PluralCount) annotation).value();
            if (ruleClass.isInterface()) {
                return args -> GwtPlural.fromNumber(pluralRules, ((Number) args[index]).doubleValue()).getGwtValue();
            }
            PluralRule rule = this.instantiateCustomPluralRuleClass(ruleClass);   // once, not per call
            return args -> CustomPlural.fromNumber(rule, ((Number) args[index]).intValue()).getGwtValue();
        }
        if (annotation instanceof Select) {
            if (Enum.class.isAssignableFrom(type)) {
                return args -> args[index] == null ? "other" : ((Enum<?>) args[index]).name();
            }
            if (String.class == type) {
                return args -> args[index] == null ? "other" : (String) args[index];
            }
            if (isSelectNumberType(type)) {
                return args -> args[index].toString();
            }
        }
        return null;
    }

    private static boolean isPluralCountType(Class<?> type) {
        return PLURAL_COUNT_TYPES.contains(type);
    }

    private static boolean isSelectNumberType(Class<?> type) {
        return SELECT_NUMBER_TYPES.contains(type);
    }

    private static String formatPlain(MessageFormat formatter, Object[] args) {
        return formatter.format(args, new StringBuffer(), null).toString();
    }

    private static SafeHtml formatSafeHtml(MessageFormat formatter, Object[] args) {
        Object[] safeArgs = args == null ? null : Arrays.stream(args).map(MessagesProxy::escape).toArray();
        // Would rather use fromSafeConstant() but doesn't work on server.
        return SafeHtmlUtils.fromTrustedString(formatPlain(formatter, safeArgs));
    }

    private static Object escape(Object arg) {
        if (arg instanceof SafeHtml) {
            return ((SafeHtml) arg).asString();
        }
        if (arg instanceof Number || arg instanceof Date) {
            return arg;   // Sub-format patterns need the real value
        }
        return SafeHtmlUtils.htmlEscape(String.valueOf(arg));
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
        PluralRule retVal;
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
            retVal = baseName + pluralStr;
        }
        return retVal;
    }
    
    private Map<String, String> buildAlternateMessages(String baseName, Method method) {
        Map<String, String> retMap = new LinkedHashMap<>();
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
