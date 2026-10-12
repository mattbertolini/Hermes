/*
 * Hermes - GWT Server-side I18N Library
 * Copyright (C) 2012  Matt Bertolini
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.ConstantsWithLookup;
import com.google.gwt.i18n.client.Messages;
import com.ibm.icu.util.ULocale;

/**
 *
 * @author Matt Bertolini
 */
public class Hermes {
    private static final char DOT = '.';
    private static final char SLASH = '/';
    private static final String PROPERTIES_EXT = ".properties";
    private static final String UTF_8 = "UTF-8";

    private static Map<LocaleMapKey, Object> cache = new HashMap<LocaleMapKey, Object>();

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
        checkValidSubclass(clazz);
        ULocale locale = null;
        if (localeStr != null && !localeStr.isEmpty()) {
            locale = ULocale.createCanonical(localeStr);
        }
        LocaleMapKey key = new LocaleMapKey(clazz.getName(), locale);
        T proxy = (T) cache.get(key);
        if (proxy == null) {
            proxy = createProxy(clazz, locale);
            cache.put(key, proxy);
        }
        return proxy;
    }

    @SuppressWarnings("unchecked")
    private static <T> T createProxy(Class<T> clazz, ULocale locale) throws IOException {
        PropertiesLocalePair result = getPropertiesForClass(clazz, locale);
        Properties properties = result.getProperties();
        // This is the actual locale the proxy classes will be given. It may be 
        // changed from the locale given by the user if a locale fallback is 
        // required.
        ULocale realLocale = result.getLocale();
        InvocationHandler handler = null;
        if (ConstantsWithLookup.class.isAssignableFrom(clazz)) {
            handler = new ConstantsWithLookupProxy(clazz, realLocale, properties);
        } else if (Constants.class.isAssignableFrom(clazz)) {
            handler = new ConstantsProxy(clazz, realLocale, properties);
        } else if (Messages.class.isAssignableFrom(clazz)) {
            handler = new MessagesProxy(clazz, realLocale, properties);
        }
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, handler);
    }

    private static <T> PropertiesLocalePair getPropertiesForClass(Class<T> clazz, ULocale locale) throws IOException {
        if (clazz.equals(ConstantsWithLookup.class) || clazz.equals(Constants.class) || clazz.equals(Messages.class)) {
            return new PropertiesLocalePair(new Properties(), locale);
        }
        // Get parent properties first - assuming single inheritance
        Properties properties = getPropertiesForClass(clazz.getInterfaces()[0], locale).getProperties();

        InputStream is = null;
        InputStreamReader reader = null;
        // This is the actual locale the proxy classes will be given. It may be
        // changed from the locale given by the user if a locale fallback is
        // required.
        ULocale realLocale = locale;
        try {
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
                is = getPropertiesFile(clazz, currentLocale);
                if (is != null) {
                    reader = new InputStreamReader(is, UTF_8);
                    properties.load(reader);
                    realLocale = currentLocale;
                    propertiesFileFound = true;
                }
            }
            if (realLocale == null) {
                realLocale = ULocale.getDefault();
            }
            if (!propertiesFileFound) {
                throw new FileNotFoundException("Could not find any properties files matching the given class.");
            }
            return new PropertiesLocalePair(properties, realLocale);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not found.", e);
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (is != null) {
                is.close();
            }
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
        return clazz.getSimpleName().replace(DOT, SLASH) + localeStr + PROPERTIES_EXT;
    }

    private static void checkValidSubclass(Class<?> clazz) {
        if (!ConstantsWithLookup.class.isAssignableFrom(clazz)
                && !Constants.class.isAssignableFrom(clazz)
                && !Messages.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Class " + clazz.getName() + " does not implement required interfaces.");
        }
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
}
