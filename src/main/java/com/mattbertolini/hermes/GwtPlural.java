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

import com.ibm.icu.text.PluralRules;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public enum GwtPlural implements Plural {
    ZERO(PluralRules.KEYWORD_ZERO, "none"),
    ONE(PluralRules.KEYWORD_ONE, "one"),
    TWO(PluralRules.KEYWORD_TWO, "two"),
    FEW(PluralRules.KEYWORD_FEW, "few"),
    MANY(PluralRules.KEYWORD_MANY, "many"),
    OTHER(PluralRules.KEYWORD_OTHER, "other");

    private static final Map<String, GwtPlural> ICU_VALUE_LOOKUP_MAP = Arrays.stream(values())
            .collect(toMap(GwtPlural::getIcuValue, Function.identity())
    );
    
    private final String icuValue;
    private final String gwtValue;
    
    GwtPlural(String icuValue, String gwtValue) {
        this.icuValue = icuValue;
        this.gwtValue = gwtValue;
    }
    
    public String getIcuValue() {
        return this.icuValue;
    }
    
    @Override
    public String getGwtValue() {
        return this.gwtValue;
    }
    
    public static Plural fromNumber(PluralRules pluralRules, double number) {
        String icuValue = pluralRules.select(number);
        return ICU_VALUE_LOOKUP_MAP.get(icuValue);
    }
}
