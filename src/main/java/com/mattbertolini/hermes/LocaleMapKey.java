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

import java.util.Objects;

/**
 * Wrapper class around a class name, the requested ULocale and the formatting
 * ULocale for use in a cache map. This class is immutable.
 */
public final class LocaleMapKey {
    private final String className;
    private final ULocale locale;
    private final ULocale formattingLocale;

    public LocaleMapKey(String className, ULocale locale, ULocale formattingLocale) {
        this.className = className;
        this.locale = locale;
        this.formattingLocale = formattingLocale;
    }

    public String getClassName() {
        return this.className;
    }

    public ULocale getLocale() {
        return this.locale;
    }

    public ULocale getFormattingLocale() {
        return this.formattingLocale;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LocaleMapKey)) {
            return false;
        }
        LocaleMapKey that = (LocaleMapKey) o;
        return Objects.equals(className, that.className) && Objects.equals(locale, that.locale)
                && Objects.equals(formattingLocale, that.formattingLocale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, locale, formattingLocale);
    }
}
