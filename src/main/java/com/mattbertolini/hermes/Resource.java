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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * One source of translations - a properties file for one locale, or the
 * annotations on one interface - like GWT's AbstractResource. Alternate forms
 * are stored as {@code key[form]} entries.
 */
public final class Resource {
    private static final char OPEN_BRACKET = '[';
    private static final char CLOSE_BRACKET = ']';

    private final Map<String, String> values;
    private final Map<String, Set<String>> forms = new HashMap<>();

    Resource(Map<String, String> values) {
        this.values = values;
        for (String name : values.keySet()) {
            int open = name.indexOf(OPEN_BRACKET);
            if (open > 0 && name.charAt(name.length() - 1) == CLOSE_BRACKET) {
                forms.computeIfAbsent(name.substring(0, open), ignored -> new LinkedHashSet<>())
                        .add(name.substring(open + 1, name.length() - 1));
            }
        }
    }

    public String get(String key) {
        return values.get(key);
    }

    public String getForm(String key, String form) {
        return values.get(formKey(key, form));
    }

    /** The alternate form names defined for a key in this resource. */
    public Set<String> getForms(String key) {
        return forms.getOrDefault(key, Collections.emptySet());
    }

    boolean isEmpty() {
        return values.isEmpty();
    }

    static String formKey(String key, String form) {
        return key + OPEN_BRACKET + form + CLOSE_BRACKET;
    }
}
