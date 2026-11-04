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

package com.mattbertolini.hermes.messages;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.ULocale;

import java.util.HashMap;
import java.util.Map;

/**
 * GWT's {@code {n,localdatetime,predef:NAME}}, where NAME is a
 * DateTimeFormat.PredefinedFormat. Formats are taken from ICU for the
 * locale; the skeletons are the fields of GWT's DefaultDateTimeFormatInfo
 * patterns, and ISO_8601/RFC_2822 use GWT's fixed English patterns.
 */
final class PredefinedDateArgument implements ExtraArgument {
    private static final Map<String, Integer> STYLES = new HashMap<>();
    private static final Map<String, String> SKELETONS = new HashMap<>();

    static {
        STYLES.put("FULL", DateFormat.FULL);
        STYLES.put("LONG", DateFormat.LONG);
        STYLES.put("MEDIUM", DateFormat.MEDIUM);
        STYLES.put("SHORT", DateFormat.SHORT);

        SKELETONS.put("DAY", "d");
        SKELETONS.put("HOUR_MINUTE", "hm");
        SKELETONS.put("HOUR_MINUTE_SECOND", "hms");
        SKELETONS.put("HOUR24_MINUTE", "Hm");
        SKELETONS.put("HOUR24_MINUTE_SECOND", "Hms");
        SKELETONS.put("MINUTE_SECOND", "ms");
        SKELETONS.put("MONTH", "LLLL");
        SKELETONS.put("MONTH_ABBR", "LLL");
        SKELETONS.put("MONTH_ABBR_DAY", "MMMd");
        SKELETONS.put("MONTH_DAY", "MMMMd");
        SKELETONS.put("MONTH_NUM_DAY", "MMdd");
        SKELETONS.put("MONTH_WEEKDAY_DAY", "MMMMEEEEd");
        SKELETONS.put("YEAR", "y");
        SKELETONS.put("YEAR_MONTH", "yMMMM");
        SKELETONS.put("YEAR_MONTH_ABBR", "yMMM");
        SKELETONS.put("YEAR_MONTH_ABBR_DAY", "yMMMd");
        SKELETONS.put("YEAR_MONTH_DAY", "yMMMMd");
        SKELETONS.put("YEAR_MONTH_NUM", "yMM");
        SKELETONS.put("YEAR_MONTH_NUM_DAY", "yMd");
        SKELETONS.put("YEAR_MONTH_WEEKDAY_DAY", "yMMMEd");
        SKELETONS.put("YEAR_QUARTER", "yQQQQ");
        SKELETONS.put("YEAR_QUARTER_ABBR", "yQ");
    }

    private final int index;
    private final DateFormat format;

    PredefinedDateArgument(int index, String name, ULocale locale) {
        this.index = index;
        this.format = createFormat(name, locale);
    }

    @Override
    public Object format(Object[] args, boolean safeHtml) {
        // DateFormat isn't thread-safe
        return ((DateFormat) format.clone()).format(args[index]);
    }

    private static DateFormat createFormat(String name, ULocale locale) {
        switch (name) {
            case "ISO_8601":
                // GWT's ZZZ is an ISO offset such as -08:00, which is ICU's ZZZZZ
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ", ULocale.ENGLISH);
            case "RFC_2822":
                return new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", ULocale.ENGLISH);
            default:
                break;
        }
        if (SKELETONS.containsKey(name)) {
            return DateFormat.getInstanceForSkeleton(SKELETONS.get(name), locale);
        }
        int separator = name.lastIndexOf('_');
        Integer style = separator < 0 ? null : STYLES.get(name.substring(separator + 1));
        if (style != null) {
            switch (name.substring(0, separator)) {
                case "DATE":
                    return DateFormat.getDateInstance(style, locale);
                case "TIME":
                    return DateFormat.getTimeInstance(style, locale);
                case "DATE_TIME":
                    return DateFormat.getDateTimeInstance(style, style, locale);
                default:
                    break;
            }
        }
        throw new IllegalArgumentException("Unrecognized predefined format 'predef:" + name + "'");
    }
}
