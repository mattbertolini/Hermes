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

import com.google.gwt.i18n.client.Constants.DefaultBooleanValue;
import com.google.gwt.i18n.client.Constants.DefaultDoubleValue;
import com.google.gwt.i18n.client.Constants.DefaultFloatValue;
import com.google.gwt.i18n.client.Constants.DefaultIntValue;
import com.google.gwt.i18n.client.Constants.DefaultStringArrayValue;
import com.google.gwt.i18n.client.Constants.DefaultStringMapValue;
import com.google.gwt.i18n.client.Constants.DefaultStringValue;
import com.ibm.icu.util.ULocale;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ConstantsProxy extends AbstractI18nProxy {

    public ConstantsProxy(Class<?> clazz, ULocale locale, Properties properties) {
        super(clazz, locale, properties);
    }

    @Override
    protected MethodHandler createHandler(Method method) {
        ConstantType constantType = ConstantType.forReturnType(method.getReturnType());
        Object value = Optional.ofNullable(getProperties().getProperty(keyFor(method)))
                .map(constantValue -> constantType.parse(constantValue, getProperties()))
                .orElseGet(() -> constantType.defaultValue(method));
        return args -> constantType.copy(value);
    }

    static Set<Class<? extends Annotation>> defaultAnnotations() {
        return Arrays.stream(ConstantType.values())
                .map(constantType -> constantType.defaultAnnotation)
                .collect(Collectors.toSet());
    }

    private enum ConstantType {
        INT(DefaultIntValue.class, int.class::equals) {
            @Override
            Object defaultValue(Method method) {
                return annotation(method, DefaultIntValue.class).value();
            }

            @Override
            Object parse(String value, Properties properties) {
                return Integer.valueOf(value);
            }
        },
        FLOAT(DefaultFloatValue.class, float.class::equals) {
            @Override
            Object defaultValue(Method method) {
                return annotation(method, DefaultFloatValue.class).value();
            }

            @Override
            Object parse(String value, Properties properties) {
                return Float.valueOf(value);
            }
        },
        DOUBLE(DefaultDoubleValue.class, double.class::equals) {
            @Override
            Object defaultValue(Method method) {
                return annotation(method, DefaultDoubleValue.class).value();
            }

            @Override
            Object parse(String value, Properties properties) {
                return Double.valueOf(value);
            }
        },
        BOOLEAN(DefaultBooleanValue.class, boolean.class::equals) {
            @Override
            Object defaultValue(Method method) {
                return annotation(method, DefaultBooleanValue.class).value();
            }

            @Override
            Object parse(String value, Properties properties) {
                return Boolean.valueOf(value);
            }
        },
        STRING_ARRAY(DefaultStringArrayValue.class, type -> type.isArray() && type.getComponentType() == String.class) {
            @Override
            Object defaultValue(Method method) {
                return annotation(method, DefaultStringArrayValue.class).value();
            }

            @Override
            Object parse(String value, Properties properties) {
                return value.split(", *");
            }

            @Override
            Object copy(Object value) {
                return ((String[]) value).clone();
            }
        },
        STRING_MAP(DefaultStringMapValue.class, Map.class::equals) {
            @Override
            Object defaultValue(Method method) {
                String[] keysValues = annotation(method, DefaultStringMapValue.class).value();
                Map<String, String> map = new HashMap<>();
                for (int i = 0; i < keysValues.length; i += 2) {
                    map.put(keysValues[i], keysValues[i + 1]);
                }
                return map;
            }

            @Override
            Object parse(String value, Properties properties) {
                return Arrays.stream(value.split(","))
                        .map(String::trim)
                        .collect(HashMap::new, (map, key) -> map.put(key, properties.getProperty(key)), HashMap::putAll);
            }

            @Override
            Object copy(Object value) {
                return new HashMap<>((Map<?, ?>) value);
            }
        },
        STRING(DefaultStringValue.class, String.class::equals) {
            @Override
            Object defaultValue(Method method) {
                return annotation(method, DefaultStringValue.class).value();
            }

            @Override
            Object parse(String value, Properties properties) {
                return value;
            }
        };

        private final Class<? extends Annotation> defaultAnnotation;
        private final Predicate<Class<?>> matcher;

        ConstantType(Class<? extends Annotation> defaultAnnotation, Predicate<Class<?>> matcher) {
            this.defaultAnnotation = defaultAnnotation;
            this.matcher = matcher;
        }

        abstract Object parse(String value, Properties properties);
        abstract Object defaultValue(Method method);

        Object copy(Object value) {
            return value;
        }

        static ConstantType forReturnType(Class<?> type) {
            return Arrays.stream(values())
                    .filter(constantType -> constantType.matcher.test(type))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported Constants return type: " + type));
        }

        private static <A extends Annotation> A annotation(Method method, Class<A> annotationType) {
            return Optional.ofNullable(method.getAnnotation(annotationType))
                    .orElseThrow(() -> new MissingResourceException(
                            "No property value or @" + annotationType.getSimpleName() + " found for " + method.getName(),
                            method.getDeclaringClass().getName(),
                            keyFor(method)));
        }
    }
}
