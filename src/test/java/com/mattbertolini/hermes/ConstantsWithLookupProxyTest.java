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

import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ConstantsWithLookupProxyTest {
    private ConstantsWithLookupProxyTestData testData;
    
    @Before
    public void setUp() throws Exception {
        this.testData = Hermes.get(ConstantsWithLookupProxyTestData.class, "en_US");
    }

    @After
    public void tearDown() throws Exception {
    }
    
    @Test
    public void testLookupString() {
        String expected = "Forty-two";
        String actual = this.testData.getString("LookupString");
        Assert.assertEquals(expected, actual);
        actual = this.testData.LookupString();
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testLookupInt() {
        int expected = 42;
        int actual = this.testData.getInt("LookupInt");
        Assert.assertEquals(expected, actual);
        actual = this.testData.LookupInt();
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testLookupFloat() {
        float expected = 4.2f;
        float actual = this.testData.getFloat("LookupFloat");
        Assert.assertEquals(expected, actual, 0);
        actual = this.testData.LookupFloat();
        Assert.assertEquals(expected, actual, 0);
    }
    
    @Test
    public void testLookupDouble() {
        double expected = 42.42;
        double actual = this.testData.getDouble("LookupDouble");
        Assert.assertEquals(expected, actual, 0);
        actual = this.testData.LookupDouble();
        Assert.assertEquals(expected, actual, 0);
    }
    
    @Test
    public void testLookupBoolean() {
        boolean expected = false;
        boolean actual = this.testData.getBoolean("LookupBoolean");
        Assert.assertEquals(expected, actual);
        actual = this.testData.LookupBoolean();
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testLookupStringArray() {
        String[] expecteds = {"Value 1", "Value 2"};
        String[] actuals = this.testData.getStringArray("LookupStringArray");
        Assert.assertArrayEquals(expecteds, actuals);
        actuals = this.testData.LookupStringArray();
        Assert.assertArrayEquals(expecteds, actuals);
    }
    
    @Test
    public void testLookupMap() {
        Map<String, String> expected = new HashMap<String, String>();
        expected.put("MapKey1", "Map Value 1");
        expected.put("MapKey2", "Map Value 2");
        Map<String, String> actual = this.testData.getMap("LookupMap");
        Assert.assertEquals(expected, actual);
        actual = this.testData.LookupMap();
        Assert.assertEquals(expected, actual);
    }
    
    @Test(expected = MissingResourceException.class)
    public void testNullParameter() {
        this.testData.getString(null);
    }
    
    @Test(expected = MissingResourceException.class)
    public void testNonExistentMethodName() {
        this.testData.getString("notAMethodName");
    }
}
