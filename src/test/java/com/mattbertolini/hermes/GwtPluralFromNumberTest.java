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
import com.ibm.icu.util.ULocale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class GwtPluralFromNumberTest {
    private final String locale;
    private final double input;
    private final GwtPlural expected;

    @Parameters(name = "Locale: {0}, Input: {1}, Expected: {2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"en", 0, GwtPlural.OTHER},
                {"en", 1, GwtPlural.ONE},
                {"en", 2, GwtPlural.OTHER},
                {"ar", 0, GwtPlural.ZERO},
                {"ar", 1, GwtPlural.ONE},
                {"ar", 2, GwtPlural.TWO},
                {"ar", 3, GwtPlural.FEW},
                {"ar", 11, GwtPlural.MANY},
                {"ar", 100, GwtPlural.OTHER},
        });
    }

    public GwtPluralFromNumberTest(String locale, double input, GwtPlural expected) {
        this.locale = locale;
        this.input = input;
        this.expected = expected;
    }

    @Test
    public void fromNumberMatches() {
        PluralRules usPluralRules = PluralRules.forLocale(new ULocale(locale));
        Plural actual = GwtPlural.fromNumber(usPluralRules, input);
        assertEquals(expected, actual);
    }
}