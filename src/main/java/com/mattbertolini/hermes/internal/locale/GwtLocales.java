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

package com.mattbertolini.hermes.internal.locale;

import com.google.gwt.i18n.server.GwtLocaleFactoryImpl;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.i18n.shared.GwtLocaleFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * GWT's own locale handling, so properties files and plural rule classes are
 * searched in the same order GWT uses: script and region inheritance
 * (zh_TW -> zh_Hant, es_MX -> es_419) and legacy aliases (iw/he, in/id).
 */
public final class GwtLocales {
    // GwtLocaleFactoryImpl caches instances in an unsynchronized map
    private static final GwtLocaleFactory FACTORY = new GwtLocaleFactoryImpl();

    private GwtLocales() {
        throw new UnsupportedOperationException("GwtLocales is a utility class and cannot be instantiated");
    }

    /**
     * @param localeStr a locale ID, or null/empty for the default locale
     */
    public static GwtLocale fromString(String localeStr) {
        synchronized (FACTORY) {
            return FACTORY.fromString(localeStr == null || localeStr.isEmpty() ? null : localeStr);
        }
    }

    /**
     * The locales to search, most specific first and ending with the default
     * locale, without the duplicates GWT's list can contain.
     */
    public static List<GwtLocale> searchList(GwtLocale locale) {
        synchronized (FACTORY) {
            return new ArrayList<>(new LinkedHashSet<>(locale.getCompleteSearchList()));
        }
    }
}
