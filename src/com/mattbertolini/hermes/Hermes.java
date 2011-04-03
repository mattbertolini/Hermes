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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
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
    
    private static Map<String, Object> cache = new HashMap<String, Object>();
    
    /**
     * For a given interface and locale ,retrieves the GWT i18n interface as a 
     * dynamic proxy for use on the server-side. If no locale is given, the 
     * default properties file will be loaded. This method caches proxy classes 
     * that it has created so it is safe to call multiple times.
     * 
     * @param clazz The GWT i18n interface to get the proxy for.
     * @param lang The IETF language tag for the locale being requested.
     * @return A dynamic proxy representing the given GWT i18n interface and 
     * locale.
     * @throws IOException If an error occurs finding, opening, or reading the 
     * GWT properties file associated with the given interface.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> clazz, String lang) throws IOException {
        if(clazz == null) {
            throw new IllegalArgumentException("Class cannot be null.");
        }
        String fileName = clazz.getName() + (lang == null || lang.isEmpty() ? "" : ("_" + lang));
        fileName = fileName.replace(DOT, SLASH);
        fileName = fileName + PROPERTIES_EXT;
        T proxy = (T) cache.get(fileName);
        if(proxy == null) {
            proxy = createProxy(clazz, lang, fileName);
            cache.put(fileName, proxy);
        }
        return proxy;
    }
    
    @SuppressWarnings("unchecked")
    private static <T> T createProxy(Class<T> clazz, String lang, String fileName) throws IOException {
        String defaultFileName = (clazz.getName().replace(DOT, SLASH)) + PROPERTIES_EXT;
        ClassLoader classLoader = clazz.getClassLoader();
        InputStream is = null;
        InputStreamReader reader = null;
        Properties properties = new Properties();
        try {
            is = classLoader.getResourceAsStream(fileName);
            if(is == null) {
                is = classLoader.getResourceAsStream(defaultFileName);
                if(is == null) {
                    throw new FileNotFoundException("Could not find any properties files matching the given class.");
                }
            }
            reader = new InputStreamReader(is, UTF_8);
            properties.load(reader);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not found.", e);
        } finally {
            if(reader != null) {
                reader.close();
            }
            if(is != null) {
                is.close();
            }
        }
        ULocale locale = ULocale.createCanonical(lang);
        InvocationHandler handler;
        if(ConstantsWithLookup.class.isAssignableFrom(clazz)) {
            handler = new ConstantsWithLookupProxy(clazz, lang, locale, properties);
        } else if(Constants.class.isAssignableFrom(clazz)) {
            handler = new ConstantsProxy(clazz, lang, locale, properties);
        } else if(Messages.class.isAssignableFrom(clazz)) {
            handler = new MessagesProxy(clazz, lang, locale, properties);
        } else {
            throw new IllegalArgumentException("Class " + clazz.getName() + " does not implement required interfaces.");
        }
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] {clazz}, handler);
    }
}
