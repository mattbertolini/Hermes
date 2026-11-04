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
import com.mattbertolini.hermes.internal.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * One constant per GWT {@code @Default*Value} annotation, and how GWT writes
 * its value as properties-file text.
 */
enum DefaultValue {
    STRING(DefaultStringValue.class) {
        @Override
        String text(Method method, UnaryOperator<String> escape) {
            return method.getAnnotation(DefaultStringValue.class).value();
        }
    },
    INT(DefaultIntValue.class) {
        @Override
        String text(Method method, UnaryOperator<String> escape) {
            return Integer.toString(method.getAnnotation(DefaultIntValue.class).value());
        }
    },
    FLOAT(DefaultFloatValue.class) {
        @Override
        String text(Method method, UnaryOperator<String> escape) {
            return Float.toString(method.getAnnotation(DefaultFloatValue.class).value());
        }
    },
    DOUBLE(DefaultDoubleValue.class) {
        @Override
        String text(Method method, UnaryOperator<String> escape) {
            return Double.toString(method.getAnnotation(DefaultDoubleValue.class).value());
        }
    },
    BOOLEAN(DefaultBooleanValue.class) {
        @Override
        String text(Method method, UnaryOperator<String> escape) {
            return Boolean.toString(method.getAnnotation(DefaultBooleanValue.class).value());
        }
    },
    STRING_ARRAY(DefaultStringArrayValue.class) {
        @Override
        String text(Method method, UnaryOperator<String> escape) {
            return Arrays.stream(method.getAnnotation(DefaultStringArrayValue.class).value())
                    .map(escape)
                    .collect(Collectors.joining(","));
        }
    },
    STRING_MAP(DefaultStringMapValue.class) {
        // The text is the comma-joined key list
        @Override
        String text(Method method, UnaryOperator<String> escape) {
            String[] keysValues = keysValues(method);
            return IntStream.range(0, keysValues.length / 2)
                    .mapToObj(i -> escape.apply(keysValues[i * 2]))
                    .collect(Collectors.joining(","));
        }

        // GWT also stores each map entry as a plain key
        @Override
        Map<String, String> entries(Method method) {
            String[] keysValues = keysValues(method);
            Map<String, String> entries = new LinkedHashMap<>();
            for (int i = 0; i < keysValues.length; i += 2) {
                entries.put(keysValues[i], keysValues[i + 1]);
            }
            return entries;
        }

        private String[] keysValues(Method method) {
            String[] keysValues = method.getAnnotation(DefaultStringMapValue.class).value();
            AnnotationResources.requireEven(keysValues, method);
            return keysValues;
        }
    };

    private final Class<? extends Annotation> annotationType;

    DefaultValue(Class<? extends Annotation> annotationType) {
        this.annotationType = annotationType;
    }

    /**
     * @param escape how to escape each element of a String[] or map key list
     */
    abstract String text(Method method, UnaryOperator<String> escape);

    /** Extra plain keys the annotation contributes to its resource. */
    Map<String, String> entries(Method method) {
        return Collections.emptyMap();
    }

    /**
     * The method's default-value annotation, if any. Like GWT, more than one
     * is an error.
     */
    static Optional<DefaultValue> of(Method method) {
        List<DefaultValue> present = Arrays.stream(values())
                .filter(value -> method.isAnnotationPresent(value.annotationType))
                .collect(Collectors.toList());
        Assert.argument(present.size() <= 1, "No more than one @Default*Value annotation may be used on "
                + method.getDeclaringClass().getName() + "." + method.getName());
        return present.stream().findFirst();
    }
}
