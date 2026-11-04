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
import com.google.gwt.i18n.shared.GwtLocale;
import com.ibm.icu.util.ULocale;
import com.mattbertolini.hermes.constants.ConstantsProxy;
import com.mattbertolini.hermes.constants.ConstantsWithLookupProxy;
import com.mattbertolini.hermes.internal.locale.GwtLocales;
import com.mattbertolini.hermes.messages.MessagesProxy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Hermes {
    private static final char DOT = '.';
    private static final char LOCALE_SEPARATOR = '_';
    private static final String PROPERTIES_EXT = ".properties";

    // Keyed by Class (not name) so same-named classes from different class
    // loaders don't collide, and cached proxies don't pin unloaded loaders.
    private static final ClassValue<Map<LocaleMapKey, Object>> CACHE = new ClassValue<Map<LocaleMapKey, Object>>() {
        @Override
        protected Map<LocaleMapKey, Object> computeValue(Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    private Hermes() {
        throw new UnsupportedOperationException("Hermes is a utility class and cannot be instantiated");
    }

    /**
     * For a given interface and locale ,retrieves the GWT i18n interface as a
     * dynamic proxy for use on the server-side. If no locale is given, the
     * default properties file will be loaded. This method caches proxy classes
     * that it has created so it is safe to call multiple times. Locale IDs
     * are canonicalized before use (i.e. en_us is treated as en_US), and
     * properties files are searched in GWT's order, including script, region
     * and alias fallbacks (e.g. zh_TW uses zh_Hant files).
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

        GwtLocale gwtLocale = GwtLocales.fromString(localeStr);
        ULocale locale = gwtLocale.isDefault() ? null : new ULocale(gwtLocale.getAsString());
        // Formatting and plural rules always follow the requested locale, even
        // when the text falls back to a less specific properties file. With no
        // locale requested, the current default is part of the cache key so a
        // later default change gets a proxy that formats with the new default.
        ULocale formattingLocale = locale != null ? ULocale.createCanonical(localeStr) : ULocale.getDefault();
        LocaleMapKey key = new LocaleMapKey(clazz.getName(), locale, formattingLocale);
        Map<LocaleMapKey, Object> proxies = CACHE.get(clazz);
        Object proxy = proxies.get(key);
        if (proxy == null) {
            // Built outside the map so resource I/O doesn't hold its lock; a racing
            // duplicate build is harmless and the first one stored wins.
            Object created = createProxy(clazz, type, gwtLocale, formattingLocale);
            proxy = proxies.putIfAbsent(key, created);
            if (proxy == null) {
                proxy = created;
            }
        }
        return (T) proxy;
    }

    @SuppressWarnings("unchecked")
    private static <T> T createProxy(Class<T> clazz, I18nType type, GwtLocale locale, ULocale formattingLocale)
            throws IOException {
        ResourceList resources = getResourcesForClass(clazz, locale);
        InvocationHandler handler = type.create(clazz, formattingLocale, resources);
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, handler);
    }

    /**
     * Builds GWT's resource search order: for each locale, most specific
     * first, each interface (child before parents) contributes its properties
     * file and then, at the default locale and the interface's own
     * {@code @DefaultLocale}, its annotations.
     */
    private static ResourceList getResourcesForClass(Class<?> clazz, GwtLocale locale) throws IOException {
        Set<Class<?>> hierarchy = new LinkedHashSet<>();
        collectHierarchy(clazz, hierarchy);
        Map<Class<?>, Resource> annotations = new HashMap<>();
        Map<Class<?>, GwtLocale> annotationLocales = new HashMap<>();
        for (Class<?> current : hierarchy) {
            I18nType type = I18nType.of(current).orElseThrow(IllegalStateException::new);
            Resource resource = AnnotationResources.forClass(current, type.isConstants());
            if (!resource.isEmpty()) {
                annotations.put(current, resource);
                annotationLocales.put(current, AnnotationResources.annotationLocale(current));
            }
        }

        List<Resource> resources = new ArrayList<>();
        Set<Class<?>> classesWithFiles = new HashSet<>();
        for (GwtLocale searchLocale : GwtLocales.searchList(locale)) {
            for (Class<?> current : hierarchy) {
                Resource file = loadPropertiesFile(current, searchLocale);
                if (file != null) {
                    resources.add(file);
                    classesWithFiles.add(current);
                }
                Resource annotationResource = annotations.get(current);
                if (annotationResource != null
                        && (searchLocale.isDefault() || searchLocale.equals(annotationLocales.get(current)))) {
                    resources.add(annotationResource);
                }
            }
        }
        ResourceList resourceList = new ResourceList(resources);
        for (Class<?> current : hierarchy) {
            if (!classesWithFiles.contains(current)) {
                checkPropertyFileNeeded(clazz, current, resourceList);
            }
        }
        return resourceList;
    }

    // Depth-first, child before parents, each interface once - GWT's walkInheritanceTree
    private static void collectHierarchy(Class<?> clazz, Set<Class<?>> hierarchy) {
        if (I18nType.isRootInterface(clazz) || !hierarchy.add(clazz)) {
            return;
        }
        for (Class<?> parent : clazz.getInterfaces()) {
            if (isValidSubclass(parent)) {
                collectHierarchy(parent, hierarchy);
            }
        }
    }

    /**
     * An interface without a properties file is fine as long as every one of
     * its methods resolves somewhere: in its annotations, or (as GWT allows)
     * in another interface's file or annotations, such as a child's.
     */
    private static void checkPropertyFileNeeded(Class<?> requested, Class<?> clazz, ResourceList resources)
            throws FileNotFoundException {
        for (Method method : clazz.getDeclaredMethods()) {
            // Default and static interface methods have bodies, so need no message
            if (Modifier.isAbstract(method.getModifiers())
                    && !resolves(Keys.keyOrNull(requested, method), resources)) {
                throw new FileNotFoundException("Could not find any properties files matching the class: "
                        + clazz.getName());
            }
        }
    }

    // A key the generator can't compute (MD5 without default text) resolves nowhere
    private static boolean resolves(String key, ResourceList resources) {
        return key != null && resources.getString(key) != null;
    }

    /**
     * Loads the properties file for the given interface and locale, or
     * returns null if there is none. Like GWT, a nested interface's file may
     * be named Outer$Inner or Outer_Inner.
     */
    private static Resource loadPropertiesFile(Class<?> clazz, GwtLocale locale) throws IOException {
        String suffix = locale.isDefault() ? "" : LOCALE_SEPARATOR + locale.getAsString();
        String classname = getPropertiesClassname(clazz);
        InputStream is = clazz.getResourceAsStream(classname + suffix + PROPERTIES_EXT);
        if (is == null && classname.indexOf('$') >= 0) {
            is = clazz.getResourceAsStream(classname.replace('$', LOCALE_SEPARATOR) + suffix + PROPERTIES_EXT);
        }
        if (is == null) {
            return null;
        }
        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.load(reader);
            Map<String, String> values = new LinkedHashMap<>();
            for (String name : properties.stringPropertyNames()) {
                values.put(name, properties.getProperty(name));
            }
            return new Resource(values);
        }
    }

    // Return class name without package prefix
    // Works for nested classes where getSimpleName() doesn't
    private static <T> String getPropertiesClassname(Class<T> clazz) {
        String classname = clazz.getName();
        return classname.substring(classname.lastIndexOf(DOT) + 1);
    }

    private static boolean isValidSubclass(Class<?> clazz) {
        return I18nType.of(clazz).isPresent();
    }

    @FunctionalInterface
    private interface ProxyFactory {
        AbstractI18nProxy create(Class<?> clazz, ULocale locale, ResourceList resources);
    }

    private enum I18nType {
        // Order matters. Most specific first
        CONSTANTS_WITH_LOOKUP(ConstantsWithLookup.class, ConstantsWithLookupProxy::new),
        CONSTANTS(Constants.class, ConstantsProxy::new),
        MESSAGES(Messages.class, MessagesProxy::new);

        private final Class<?> rootInterface;
        private final ProxyFactory factory;

        I18nType(Class<?> rootInterface, ProxyFactory factory) {
            this.rootInterface = rootInterface;
            this.factory = factory;
        }

        AbstractI18nProxy create(Class<?> clazz, ULocale locale, ResourceList resources) {
            return factory.create(clazz, locale, resources);
        }

        boolean isConstants() {
            return this != MESSAGES;
        }

        static Optional<I18nType> of(Class<?> clazz) {
            return Arrays.stream(values()).filter(type -> type.rootInterface.isAssignableFrom(clazz)).findFirst();
        }

        static boolean isRootInterface(Class<?> clazz) {
            return Arrays.stream(values()).anyMatch(type -> type.rootInterface == clazz);
        }
    }
}
