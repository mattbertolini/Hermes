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

import com.ibm.icu.util.ULocale;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.function.Predicate;

public class ConstantsProxy extends AbstractI18nProxy {

    ConstantsProxy(Class<?> clazz, ULocale locale, ResourceList resources) {
        super(clazz, locale, resources);
    }

    @Override
    protected MethodHandler createHandler(Method method) {
        ConstantType constantType = ConstantType.forReturnType(method.getReturnType());
        Object value = constantType.parse(keyFor(method), this);
        return args -> constantType.copy(value);
    }

    /**
     * Splits a String[] constant or a Map constant's key list exactly as GWT's
     * ConstantsStringArrayMethodCreator.split does: on unescaped commas, with
     * {@code \,} as a literal comma, trimming each item and keeping trailing
     * empty items.
     */
    static String[] split(String target) {
        // An artificial end character keeps split() from dropping a trailing empty item
        String[] items = (target + "~").split("(?<![\\\\]),");
        int last = items.length - 1;
        items[last] = items[last].substring(0, items[last].length() - 1);
        for (int i = 0; i < items.length; i++) {
            items[i] = items[i].replaceAll("\\\\,", ",").trim();
        }
        return items;
    }

    private String requiredString(String key) {
        String value = getResources().getString(key);
        if (value == null) {
            throw new MissingResourceException("No property value or default annotation found for key " + key,
                    getI18nClass().getName(), key);
        }
        return value;
    }

    private enum ConstantType {
        INT(int.class::equals) {
            @Override
            Object parse(String key, ConstantsProxy proxy) {
                return Integer.valueOf(proxy.requiredString(key));
            }
        },
        FLOAT(float.class::equals) {
            @Override
            Object parse(String key, ConstantsProxy proxy) {
                return Float.valueOf(proxy.requiredString(key));
            }
        },
        DOUBLE(double.class::equals) {
            @Override
            Object parse(String key, ConstantsProxy proxy) {
                return Double.valueOf(proxy.requiredString(key));
            }
        },
        BOOLEAN(boolean.class::equals) {
            @Override
            Object parse(String key, ConstantsProxy proxy) {
                return Boolean.valueOf(proxy.requiredString(key));
            }
        },
        STRING_ARRAY(type -> type.isArray() && type.getComponentType() == String.class) {
            @Override
            Object parse(String key, ConstantsProxy proxy) {
                return split(proxy.requiredString(key));
            }

            @Override
            Object copy(Object value) {
                return ((String[]) value).clone();
            }
        },
        STRING_MAP(Map.class::equals) {
            // Like GWT's ConstantsMapMethodCreator: each value comes from "map[key]",
            // falling back to a plain "key" entry, and the map keeps declaration order.
            @Override
            Object parse(String key, ConstantsProxy proxy) {
                Map<String, String> map = new LinkedHashMap<>();
                for (String mapKey : split(proxy.requiredString(key))) {
                    if (!mapKey.isEmpty()) {
                        String value = proxy.getResources().getForm(key, mapKey);
                        map.put(mapKey, value != null ? value : proxy.requiredString(mapKey));
                    }
                }
                return Collections.unmodifiableMap(map);
            }
        },
        STRING(String.class::equals) {
            @Override
            Object parse(String key, ConstantsProxy proxy) {
                return proxy.requiredString(key);
            }
        };

        private final Predicate<Class<?>> matcher;

        ConstantType(Predicate<Class<?>> matcher) {
            this.matcher = matcher;
        }

        abstract Object parse(String key, ConstantsProxy proxy);

        Object copy(Object value) {
            return value;
        }

        static ConstantType forReturnType(Class<?> type) {
            return Arrays.stream(values())
                    .filter(constantType -> constantType.matcher.test(type))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported Constants return type: " + type));
        }
    }
}
