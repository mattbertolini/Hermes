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
import com.google.gwt.i18n.client.LocalizableResource.GenerateKeys;
import com.google.gwt.i18n.client.LocalizableResource.Key;
import com.google.gwt.i18n.client.LocalizableResource.Meaning;
import com.google.gwt.i18n.client.Messages.DefaultMessage;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Computes the properties key for a method the way GWT does: {@code @Key} if
 * present, otherwise the key generator named by {@code @GenerateKeys}
 * (method name when absent).
 */
final class Keys {
    // Uppercase, as GWT's StringUtils.toHexString encodes MD5 keys
    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();
    private static final Map<String, Function<Method, String>> GENERATORS = new HashMap<>();

    static {
        // GWT ships each generator under both its old (rebind) and new (server) package
        for (String pkg : Arrays.asList("com.google.gwt.i18n.rebind.keygen.", "com.google.gwt.i18n.server.keygen.")) {
            GENERATORS.put(pkg + "MethodNameKeyGenerator", Method::getName);
            GENERATORS.put(pkg + "FullyQualifiedMethodNameKeyGenerator",
                    method -> method.getDeclaringClass().getCanonicalName() + "." + method.getName());
            GENERATORS.put(pkg + "MD5KeyGenerator", Keys::md5Key);
        }
    }

    private Keys() {
        throw new UnsupportedOperationException("Keys is a utility class and cannot be instantiated");
    }

    /**
     * @param targetClass the interface whose {@code @GenerateKeys} applies
     * @throws IllegalArgumentException if the key generator can't compute a key
     */
    static String keyFor(Class<?> targetClass, Method method) {
        String key = keyOrNull(targetClass, method);
        if (key == null) {
            throw new IllegalArgumentException("Key generator "
                    + classAnnotation(targetClass, GenerateKeys.class).value()
                    + " requires the default value be specified in an annotation for method " + method.getName());
        }
        return key;
    }

    /**
     * Like {@link #keyFor}, but returns null when the key generator can't
     * compute a key, as GWT's MD5KeyGenerator does without default text.
     */
    static String keyOrNull(Class<?> targetClass, Method method) {
        Key key = method.getAnnotation(Key.class);
        if (key != null) {
            return key.value();
        }
        GenerateKeys generateKeys = classAnnotation(targetClass, GenerateKeys.class);
        if (generateKeys == null) {
            return method.getName();
        }
        Function<Method, String> generator = GENERATORS.get(generateKeys.value());
        if (generator == null) {
            throw new IllegalArgumentException("Unsupported key generator in @GenerateKeys on "
                    + targetClass.getName() + ": " + generateKeys.value());
        }
        return generator.apply(method);
    }

    /** Finds a class annotation on the interface or, failing that, its superinterfaces. */
    static <A extends Annotation> A classAnnotation(Class<?> clazz, Class<A> annotationType) {
        A annotation = clazz.getAnnotation(annotationType);
        for (int i = 0; annotation == null && i < clazz.getInterfaces().length; i++) {
            annotation = classAnnotation(clazz.getInterfaces()[i], annotationType);
        }
        return annotation;
    }

    private static String md5Key(Method method) {
        String text = defaultText(method);
        if (text == null) {
            return null;   // Cannot compute a key if no default text is supplied
        }
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Error initializing MD5", e);
        }
        md5.update(text.getBytes(StandardCharsets.UTF_8));
        Meaning meaning = method.getAnnotation(Meaning.class);
        if (meaning != null) {
            md5.update(meaning.value().getBytes(StandardCharsets.UTF_8));
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : md5.digest()) {
            hex.append(HEX_CHARS[(b >> 4) & 0xF]).append(HEX_CHARS[b & 0xF]);
        }
        return hex.toString();
    }

    /** The text GWT's MD5KeyGenerator hashes (KeyGenMessage.getDefaultMessage). */
    private static String defaultText(Method method) {
        DefaultMessage message = method.getAnnotation(DefaultMessage.class);
        if (message != null) {
            return message.value();
        }
        DefaultStringMapValue map = method.getAnnotation(DefaultStringMapValue.class);
        if (map != null) {
            String[] keysValues = map.value();
            return IntStream.range(0, keysValues.length / 2)
                    .mapToObj(i -> quoteComma(keysValues[i * 2]))
                    .collect(Collectors.joining(","));
        }
        DefaultStringValue string = method.getAnnotation(DefaultStringValue.class);
        if (string != null) {
            return string.value();
        }
        DefaultIntValue intValue = method.getAnnotation(DefaultIntValue.class);
        if (intValue != null) {
            return String.valueOf(intValue.value());
        }
        DefaultBooleanValue booleanValue = method.getAnnotation(DefaultBooleanValue.class);
        if (booleanValue != null) {
            return String.valueOf(booleanValue.value());
        }
        DefaultDoubleValue doubleValue = method.getAnnotation(DefaultDoubleValue.class);
        if (doubleValue != null) {
            return String.valueOf(doubleValue.value());
        }
        DefaultFloatValue floatValue = method.getAnnotation(DefaultFloatValue.class);
        if (floatValue != null) {
            return String.valueOf(floatValue.value());
        }
        DefaultStringArrayValue array = method.getAnnotation(DefaultStringArrayValue.class);
        if (array != null) {
            return Arrays.stream(array.value()).map(Keys::quoteComma).collect(Collectors.joining(","));
        }
        return null;
    }

    private static String quoteComma(String value) {
        return value.replace(",", "\\,");
    }
}
