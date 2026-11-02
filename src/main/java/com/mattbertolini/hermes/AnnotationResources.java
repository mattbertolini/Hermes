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
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;
import com.google.gwt.i18n.client.Messages.AlternateMessage;
import com.google.gwt.i18n.client.Messages.DefaultMessage;
import com.google.gwt.i18n.client.Messages.PluralText;
import com.google.gwt.i18n.shared.GwtLocale;
import com.mattbertolini.hermes.internal.util.Assert;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the resource holding an interface's default-value annotations, as
 * GWT's AnnotationsResource does. Values are stored as the same text a
 * properties file would hold, so both are parsed identically.
 */
final class AnnotationResources {
    private AnnotationResources() {
        throw new UnsupportedOperationException("AnnotationResources is a utility class and cannot be instantiated");
    }

    static Resource forClass(Class<?> clazz, boolean isConstants) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (!Modifier.isAbstract(method.getModifiers())) {
                continue;
            }
            String text = isConstants ? constantText(method, values) : messageText(method);
            if (text == null) {
                continue;   // No default value annotation
            }
            String key = Keys.keyFor(clazz, method);
            values.put(key, text);
            if (!isConstants) {
                addAlternateMessages(key, method, values);
            }
        }
        return new Resource(values);
    }

    /**
     * The locale the annotations are written in, besides the default locale:
     * {@code @DefaultLocale}, or a locale embedded in the name (Foo_fr).
     */
    static GwtLocale annotationLocale(Class<?> clazz) {
        String localeStr = null;
        String simpleName = clazz.getSimpleName();
        int underscore = simpleName.indexOf('_');
        if (underscore >= 0) {
            localeStr = simpleName.substring(underscore + 1);
        }
        DefaultLocale defaultLocale = Keys.classAnnotation(clazz, DefaultLocale.class);
        if (defaultLocale != null) {
            localeStr = defaultLocale.value();
        }
        return GwtLocales.fromString(localeStr);
    }

    private static String messageText(Method method) {
        DefaultMessage message = method.getAnnotation(DefaultMessage.class);
        return message == null ? null : message.value();
    }

    @SuppressWarnings("deprecation") // PluralText is deprecated, but GWT still supports it
    private static void addAlternateMessages(String key, Method method, Map<String, String> values) {
        String[] forms = method.isAnnotationPresent(AlternateMessage.class)
                ? method.getAnnotation(AlternateMessage.class).value()
                : method.isAnnotationPresent(PluralText.class) ? method.getAnnotation(PluralText.class).value() : new String[0];
        requireEven(forms, method);
        for (int i = 0; i < forms.length; i += 2) {
            values.put(Resource.formKey(key, forms[i]), forms[i + 1]);
        }
    }

    // String arrays and map key lists are comma-joined with backslash escaping,
    // and map entries become plain keys, exactly as GWT encodes them.
    private static String constantText(Method method, Map<String, String> values) {
        if (method.isAnnotationPresent(DefaultStringValue.class)) {
            return method.getAnnotation(DefaultStringValue.class).value();
        } else if (method.isAnnotationPresent(DefaultIntValue.class)) {
            return Integer.toString(method.getAnnotation(DefaultIntValue.class).value());
        } else if (method.isAnnotationPresent(DefaultFloatValue.class)) {
            return Float.toString(method.getAnnotation(DefaultFloatValue.class).value());
        } else if (method.isAnnotationPresent(DefaultDoubleValue.class)) {
            return Double.toString(method.getAnnotation(DefaultDoubleValue.class).value());
        } else if (method.isAnnotationPresent(DefaultBooleanValue.class)) {
            return Boolean.toString(method.getAnnotation(DefaultBooleanValue.class).value());
        } else if (method.isAnnotationPresent(DefaultStringArrayValue.class)) {
            return Arrays.stream(method.getAnnotation(DefaultStringArrayValue.class).value())
                    .map(AnnotationResources::escapeComma)
                    .collect(Collectors.joining(","));
        } else if (method.isAnnotationPresent(DefaultStringMapValue.class)) {
            String[] keysValues = method.getAnnotation(DefaultStringMapValue.class).value();
            requireEven(keysValues, method);
            StringBuilder keys = new StringBuilder();
            for (int i = 0; i < keysValues.length; i += 2) {
                values.put(keysValues[i], keysValues[i + 1]);
                keys.append(i == 0 ? "" : ",").append(escapeComma(keysValues[i]));
            }
            return keys.toString();
        }
        return null;
    }

    private static String escapeComma(String value) {
        return value.replace("\\", "\\\\").replace(",", "\\,");
    }

    private static void requireEven(String[] values, Method method) {
        Assert.argument(values.length % 2 == 0, "Odd number of strings supplied to an annotation on "
                + method.getDeclaringClass().getName() + "." + method.getName()
                + ": must be pairs of names and values");
    }
}
