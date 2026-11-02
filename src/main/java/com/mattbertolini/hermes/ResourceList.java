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

import java.util.ArrayList;
import java.util.List;

/**
 * Resources in GWT's search order (most specific locale first; within a
 * locale, the interface before its parents), where the first resource
 * defining a key wins. Mirrors GWT's ResourceList.
 */
final class ResourceList {
    private final List<Resource> resources;

    ResourceList(List<Resource> resources) {
        this.resources = new ArrayList<>(resources);
    }

    String getString(String key) {
        Resource entry = getEntry(key);
        return entry == null ? null : entry.get(key);
    }

    /**
     * First {@code key[form]} value in any resource, as GWT's getStringExt.
     */
    String getForm(String key, String form) {
        for (Resource resource : resources) {
            String value = resource.getForm(key, form);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * The first resource defining the key. A message's alternate forms come
     * from this resource only, never from less specific ones.
     */
    Resource getEntry(String key) {
        for (Resource resource : resources) {
            if (resource.get(key) != null) {
                return resource;
            }
        }
        return null;
    }
}
