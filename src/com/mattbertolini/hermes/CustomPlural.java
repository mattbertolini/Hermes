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
import com.google.gwt.i18n.client.PluralRule;

/**
 * 
 * @author Matt Bertolini
 */
public class CustomPlural implements Plural {
    private static final String EMPTY_STRING = "";
    private static final String OTHER = "other";
    
    private String gwtValue;
    
    public CustomPlural(String gwtValue) {
        this.gwtValue = gwtValue;
    }

    @Override
    public String buildPatternName(String baseName) {
        String retVal = null;
        if(this.gwtValue == null || this.gwtValue.equals(EMPTY_STRING) 
                || this.gwtValue.equals(OTHER)) {
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
                Plural key = new CustomPlural(values[i]);
                defaultValues.put(key, values[i + 1]);
            }
            DefaultMessage defaultMessage = method.getAnnotation(DefaultMessage.class);
            if(defaultMessage != null) {
                defaultValues.put(GwtPlural.OTHER, defaultMessage.value());
            }
        }
        return defaultValues;
    }
    
    public static Plural fromNumber(PluralRule rule, int num) {
        Plural retVal = null;
        int index = rule.select(num);
        String ruleName = rule.pluralForms()[index].getName();
        if(ruleName == null || ruleName.equals(EMPTY_STRING) || ruleName.equals(OTHER)) {
            retVal = GwtPlural.OTHER;
        } else {
            retVal = new CustomPlural(ruleName);
        }
        return retVal;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((gwtValue == null) ? 0 : gwtValue.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CustomPlural other = (CustomPlural) obj;
        if (gwtValue == null) {
            if (other.gwtValue != null) {
                return false;
            }
        } else if (!gwtValue.equals(other.gwtValue)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "CustomPlural [gwtValue=" + gwtValue + "]";
    }
}
