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

import java.util.Arrays;


public class CustomPluralRule_fr extends CustomPluralRule {
    private static PluralForm[] rules = {
        new PluralForm("other", "Default plural form."),
        new PluralForm("one", "Singlar plural form."),
        new PluralForm("custom", "Custom plural form when number is 24.")
    };
    
    @Override
    public PluralForm[] pluralForms() {
        return Arrays.copyOf(rules, rules.length);
    }

    @Override
    public int select(int n) {
        int retVal;
        if(n == 1) {
            retVal = 1;
        } else if(n == 24) {
            retVal = 2;
        } else {
            retVal = 0;
        }
        return retVal;
    }

}
