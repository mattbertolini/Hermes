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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.i18n.client.Messages.DefaultMessage;
import com.google.gwt.i18n.client.Messages.PluralText;
import com.ibm.icu.text.PluralRules;

public enum GwtPlural implements Plural {
    ZERO(PluralRules.KEYWORD_ZERO, "none"),
    ONE(PluralRules.KEYWORD_ONE, "one"),
    TWO(PluralRules.KEYWORD_TWO, "two"),
    FEW(PluralRules.KEYWORD_FEW, "few"),
    MANY(PluralRules.KEYWORD_MANY, "many"),
    OTHER(PluralRules.KEYWORD_OTHER, "");
    
    private String icuValue;
    private String gwtValue;
    
    private GwtPlural(String icuValue, String gwtValue) {
        this.icuValue = icuValue;
        this.gwtValue = gwtValue;
    }
    
    public String getIcuValue() {
        return this.icuValue;
    }
    
    public String getGwtValue() {
        return this.gwtValue;
    }
    
    @Override
    public String buildPatternName(String baseName) {
        String retVal;
        if(GwtPlural.OTHER == this) {
            retVal = baseName;
        } else {
            retVal = baseName + "[" + this.gwtValue + "]";
        }
        return retVal;
    }
    
    @Override
    public Map<Plural, String> buildDefaultPluralValueMap(Method method) {
        PluralText pluralTextAnnotation = method.getAnnotation(PluralText.class);
        Map<Plural, String> defaultValues = null;
        if(pluralTextAnnotation != null) {
            String[] values = pluralTextAnnotation.value();
            defaultValues = new HashMap<Plural, String>();
            for(int i = 0; i < values.length; i += 2) {
                Plural key = GwtPlural.fromGwtValue(values[i]);
                defaultValues.put(key, values[i + 1]);
            }
            DefaultMessage defaultMessage = method.getAnnotation(DefaultMessage.class);
            if(defaultMessage != null) {
                defaultValues.put(GwtPlural.OTHER, defaultMessage.value());
            }
        }
        return defaultValues;
    }
    
    public static Plural fromGwtValue(String gwtValue) {
        Plural retVal = null;
        for(GwtPlural plural : GwtPlural.values()) {
            if(plural.getGwtValue().equals(gwtValue)) {
                retVal = plural;
                break;
            }
        }
        return retVal;
    }
    
    public static Plural fromNumber(PluralRules pluralRules, double number) {
        String icuValue = pluralRules.select(number);
        Plural retVal = null;
        for(GwtPlural plural : GwtPlural.values()) {
            if(plural.getIcuValue().equals(icuValue)) {
                retVal = plural;
                break;
            }
        }
        return retVal;
    }
}
