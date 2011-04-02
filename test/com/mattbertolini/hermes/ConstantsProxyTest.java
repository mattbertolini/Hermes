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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ConstantsProxyTest {
    private ConstantsProxyTestData testData;
    
    @Before
    public void setUp() throws Exception {
        this.testData = Hermes.get(ConstantsProxyTestData.class, "en_US");
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testStringConstant() {
        String expected = "Hello World!";
        String actual = this.testData.StringConstant();
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testStringConstantDefaultValue() {
        String expected = "Default string";
        String actual = this.testData.StringConstantDefaultValue();
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testStringConstantWithDifferentKey() {
        String expected = "Different";
        String actual = this.testData.StringContantWithDifferentKey();
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testIntConstant() {
        int expected = 42;
        int actual = this.testData.IntConstant();
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testIntConstantDefaultValue() {
        int expected = 24;
        int actual = this.testData.IntConstantDefaultValue();
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testFloatConstant() {
        float expected = 4.2f;
        float actual = this.testData.FloatConstant();
        Assert.assertEquals(expected, actual, 0);
    }
    
    @Test
    public void testFloatConstantDefaultValue() {
        float expected = 2.4f;
        float actual = this.testData.FloatConstantDefaultValue();
        Assert.assertEquals(expected, actual, 0);
    }
    
    @Test
    public void testDoubleConstant() {
        double expected = 42.42d;
        double actual = this.testData.DoubleConstant();
        Assert.assertEquals(expected, actual, 0);
    }
    
    @Test
    public void testDoubleConstantDefaultValue() {
        double expected = 24.24d;
        double actual = this.testData.DoubleConstantDefaultValue();
        Assert.assertEquals(expected, actual, 0);
    }
    
    @Test
    public void testBooleanConstant() {
        boolean expected = true;
        boolean actual = this.testData.BooleanConstant();
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testBooleanConstantDefaultValue() {
        boolean expected = false;
        boolean actual = this.testData.BooleanConstantDefaultValue();
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testStringArrayConstant() {
        String[] expecteds = {"Index1", "Index2", "Index3", "Index4", "Index5"};
        String[] actuals = this.testData.StringArrayConstant();
        Assert.assertEquals(expecteds.length, actuals.length);
        Assert.assertArrayEquals(expecteds, actuals);
    }
    
    @Test
    public void testStringArrayConstantDefaultValue() {
        String[] expecteds = {"Default1", "Default2"};
        String[] actuals = this.testData.StringArrayConstantDefaultValue();
        Assert.assertEquals(expecteds.length, actuals.length);
        Assert.assertArrayEquals(expecteds, actuals);
    }
    
    @Test
    public void testMapConstant() {
        Map<String, String> expected = new HashMap<String, String>(3);
        expected.put("MapKey1", "Map Value 1");
        expected.put("MapKey2", "Map Value 2");
        expected.put("MapKey3", "Map Value 3");
        Map<String, String> actual = this.testData.MapConstant();
        Assert.assertEquals(expected.size(), actual.size());
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testMapConstantDefaultValue() {
        Map<String, String> expected = new HashMap<String, String>(3);
        expected.put("DefaultKey1", "DefaultValue1");
        expected.put("DefaultKey2", "DefaultValue2");
        Map<String, String> actual = this.testData.MapConstantDefaultValue();
        Assert.assertEquals(expected.size(), actual.size());
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testToString() {
        String expected = "com.mattbertolini.hermes.ConstantsProxy: com.mattbertolini.hermes.ConstantsProxyTestData, lang=en_US";
        String actual = this.testData.toString();
        Assert.assertEquals(expected, actual);
    }
}
