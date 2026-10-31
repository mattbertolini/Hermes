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

import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.ConstantsWithLookup;
import com.google.gwt.i18n.client.Messages;
import com.google.gwt.i18n.client.Messages.AlternateMessage;
import com.google.gwt.i18n.client.Messages.DefaultMessage;
import com.ibm.icu.util.ULocale;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Hermes {
    private static final char DOT = '.';
    private static final String PROPERTIES_EXT = ".properties";

    private static final Map<LocaleMapKey, Object> CACHE = new ConcurrentHashMap<>();

    private Hermes() {
        throw new UnsupportedOperationException("Hermes is a utility class and cannot be instantiated");
    }

    /**
     * For a given interface and locale ,retrieves the GWT i18n interface as a
     * dynamic proxy for use on the server-side. If no locale is given, the
     * default properties file will be loaded. This method caches proxy classes
     * that it has created so it is safe to call multiple times. Locale IDs
     * are case-sensitive (i.e. en_us is not the same as en_US).
     *
     * @param clazz     The GWT i18n interface to get the proxy for.
     * @param localeStr The locale ID string for the locale being requested.
     * @return A dynamic proxy representing the given GWT i18n interface and
     * locale.
     * @throws IOException If an error occurs finding, opening, or reading the
     *                     GWT properties file associated with the given interface.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> clazz, String localeStr) throws IOException {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null.");
        }
        I18nType type = I18nType.of(clazz).orElseThrow(() -> new IllegalArgumentException(
                "Class " + clazz.getName() + " does not implement required interfaces."));

        ULocale locale = null;
        if (localeStr != null && !localeStr.isEmpty()) {
            locale = ULocale.createCanonical(localeStr);
        }
        LocaleMapKey key = new LocaleMapKey(clazz.getName(), locale);
        T proxy = (T) CACHE.get(key);
        if (proxy == null) {
            proxy = createProxy(clazz, type, locale);
            CACHE.put(key, proxy);
        }
        return proxy;
    }

    @SuppressWarnings("unchecked")
    private static <T> T createProxy(Class<T> clazz, I18nType type, ULocale locale) throws IOException {
        PropertiesLocalePair result = getPropertiesForClass(clazz, locale);
        Properties properties = result.getProperties();
        // This is the actual locale the proxy classes will be given. It may be 
        // changed from the locale given by the user if a locale fallback is 
        // required.
        ULocale realLocale = result.getLocale();
        InvocationHandler handler = type.create(clazz, realLocale, properties);
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, handler);
    }

    private static <T> PropertiesLocalePair getPropertiesForClass(Class<T> clazz, ULocale locale) throws IOException {
        if (I18nType.isRootInterface(clazz)) {
            return new PropertiesLocalePair(new Properties(), locale);
        }
        // Get parent properties first - allowing for multiple inheritance
        Properties properties = new Properties();
        for (Class<?> parent : clazz.getInterfaces()) {
            if (isValidSubclass(parent)) {
                properties.putAll(getPropertiesForClass(parent, locale).getProperties());
            }
        }

        // This is the actual locale the proxy classes will be given. It may be
        // changed from the locale given by the user if a locale fallback is
        // required.
        ULocale realLocale = locale;
        // Build list of locales from least to most specific, including default null locale
        List<ULocale> localeChain = new ArrayList<>();
        localeChain.add(realLocale);
        ULocale parentLocale = locale;
        while (parentLocale != null) {
            parentLocale = parentLocale.getFallback();
            localeChain.add(0, parentLocale);
        }

        // Build properties with more specific locale overriding less specific locale
        boolean propertiesFileFound = false;
        for (ULocale currentLocale : localeChain) {
            try (InputStream is = getPropertiesFile(clazz, currentLocale)) {
                if (is != null) {
                    try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        properties.load(reader);
                        realLocale = currentLocale;
                        propertiesFileFound = true;
                    }
                }
            }
        }
        if (realLocale == null) {
            realLocale = ULocale.getDefault();
        }
        if (!propertiesFileFound) {
            checkPropertyFileNeeded(clazz);
        }
        return new PropertiesLocalePair(properties, realLocale);
    }

    // If a property file has not been provided, check all properties have valid annotations
    private static void checkPropertyFileNeeded(Class<?> clazz) throws FileNotFoundException {
        boolean allMethodsHaveDefaults = I18nType.of(clazz)
                .map(type -> type.hasDefaultsForAllMethods(clazz))
                .orElse(false);
        if (!allMethodsHaveDefaults) {
            // No annotation found for a property - we are missing a property file
            throw new FileNotFoundException("Could not find any properties files matching the class: " + clazz.getName());
        }
    }

    /**
     * Get the properties file associated with the given interface and locale
     * as an InputStream. Use the return value in an InputStream reader to load
     * the properties file data into a Properties object.
     *
     * @param clazz  The Class representing one of the i18n interface.
     * @param locale The locale to get the properties for.
     * @return Returns an InputStream of the file data suitable for loading
     * into a Properties object or null if no properties file for the file
     * path is found.
     */
    private static <T> InputStream getPropertiesFile(Class<T> clazz, ULocale locale) {
        return clazz.getResourceAsStream(getPropertiesFilename(clazz, locale));
    }

    private static <T> String getPropertiesFilename(Class<T> clazz, ULocale locale) {
        String localeStr = "";
        if (locale != null) {
            localeStr = '_' + locale.getName();
        }
        return getPropertiesClassname(clazz) + localeStr + PROPERTIES_EXT;
    }

    // Return class name without package prefix
    // Works for nested classes where getSimpleName() doesn't
    private static <T> String getPropertiesClassname(Class<T> clazz) {
        String classname = clazz.getName();
        int lastDot = classname.lastIndexOf(DOT);
        if (lastDot >= 0 && lastDot < classname.length() + 1) {
            classname = classname.substring(classname.lastIndexOf(DOT) + 1);
        }
        return classname;
    }

    private static boolean isValidSubclass(Class<?> clazz) {
        return I18nType.of(clazz).isPresent();
    }

    static class PropertiesLocalePair {
        private final Properties properties;
        private final ULocale locale;

        public PropertiesLocalePair(Properties properties, ULocale locale) {
            this.properties = properties;
            this.locale = locale;
        }

        public Properties getProperties() {
            return properties;
        }

        public ULocale getLocale() {
            return locale;
        }
    }

    @FunctionalInterface
    private interface ProxyFactory {
        AbstractI18nProxy create(Class<?> clazz, ULocale locale, Properties props);
    }

    private enum I18nType {
        // Order matters. Most specific first
        CONSTANTS_WITH_LOOKUP(ConstantsWithLookup.class, ConstantsWithLookupProxy::new, ConstantsProxy.defaultAnnotations()),
        CONSTANTS(Constants.class, ConstantsProxy::new, ConstantsProxy.defaultAnnotations()),
        MESSAGES(Messages.class, MessagesProxy::new, new HashSet<>(Arrays.asList(DefaultMessage.class, AlternateMessage.class)));

        private final Class<?> rootInterface;
        private final ProxyFactory factory;
        private final Set<Class<? extends Annotation>> defaultAnnotations;

        I18nType(Class<?> rootInterface, ProxyFactory factory, Set<Class<? extends Annotation>> defaultAnnotations) {
            this.rootInterface = rootInterface;
            this.factory = factory;
            this.defaultAnnotations = defaultAnnotations;
        }

        AbstractI18nProxy create(Class<?> clazz, ULocale locale, Properties properties) {
            return factory.create(clazz, locale, properties);
        }

        boolean hasDefaultsForAllMethods(Class<?> clazz) {
            return Arrays.stream(clazz.getDeclaredMethods()).allMatch(this::hasDefault);
        }

        private boolean hasDefault(Method method) {
            return Arrays.stream(method.getDeclaredAnnotations())
                    .map(Annotation::annotationType)
                    .anyMatch(defaultAnnotations::contains);
        }

        static Optional<I18nType> of(Class<?> clazz) {
            return Arrays.stream(values()).filter(type -> type.rootInterface.isAssignableFrom(clazz)).findFirst();
        }

        static boolean isRootInterface(Class<?> clazz) {
            return Arrays.stream(values()).anyMatch(type -> type.rootInterface == clazz);
        }
    }
}
