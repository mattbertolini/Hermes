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

import com.google.gwt.i18n.client.PluralRule;

import java.util.Objects;

public class CustomPlural implements Plural {
    private static final String EMPTY_STRING = "";
    private static final String OTHER = "other";
    
    private final String gwtValue;
    
    public CustomPlural(String gwtValue) {
        this.gwtValue = gwtValue;
    }
    
    @Override
    public String getGwtValue() {
        return this.gwtValue;
    }
    
    public static Plural fromNumber(PluralRule rule, int num) {
        Plural retVal;
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
    public boolean equals(Object o) {
        if (!(o instanceof CustomPlural)) {
            return false;
        }
        CustomPlural that = (CustomPlural) o;
        return Objects.equals(gwtValue, that.gwtValue);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(gwtValue);
    }

    @Override
    public String toString() {
        return "CustomPlural [gwtValue=" + gwtValue + "]";
    }
}
