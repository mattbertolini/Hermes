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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class PluralMessagesTest {
    @Test
    public void testPluralMessageIntParamEnglish() throws IOException {
        PluralMessagesTestData testData = Hermes.get(PluralMessagesTestData.class, "en_US");
        
        String zeroExpected = "There are 0 items in your cart.";
        String zeroActual = testData.PluralMessageIntParam(0);
        Assert.assertEquals(zeroExpected, zeroActual);
        
        String oneExpected = "There is 1 item in your cart.";
        String oneActual = testData.PluralMessageIntParam(1);
        Assert.assertEquals(oneExpected, oneActual);
        
        String twoExpected = "There are 2 items in your cart.";
        String twoActual = testData.PluralMessageIntParam(2);
        Assert.assertEquals(twoExpected, twoActual);
        
        String fewExpected = "There are 4 items in your cart.";
        String fewActual = testData.PluralMessageIntParam(4);
        Assert.assertEquals(fewExpected, fewActual);
        
        String manyExpected = "There are 42 items in your cart.";
        String manyActual = testData.PluralMessageIntParam(42);
        Assert.assertEquals(manyExpected, manyActual);
        
        String otherExcepted = "There are 142 items in your cart.";
        String otherActual = testData.PluralMessageIntParam(142);
        Assert.assertEquals(otherExcepted, otherActual);
    }
    
    @Test
    public void testPluralMessageIntParamFrench() throws IOException {
        PluralMessagesTestData testData = Hermes.get(PluralMessagesTestData.class, "fr");
        
        String zeroExpected = "[fr] There is 0 item in your cart.";
        String zeroActual = testData.PluralMessageIntParam(0);
        Assert.assertEquals(zeroExpected, zeroActual);
        
        String oneExpected = "[fr] There is 1 item in your cart.";
        String oneActual = testData.PluralMessageIntParam(1);
        Assert.assertEquals(oneExpected, oneActual);
        
        String twoExpected = "[fr] There are 2 items in your cart.";
        String twoActual = testData.PluralMessageIntParam(2);
        Assert.assertEquals(twoExpected, twoActual);
        
        String fewExpected = "[fr] There are 4 items in your cart.";
        String fewActual = testData.PluralMessageIntParam(4);
        Assert.assertEquals(fewExpected, fewActual);
        
        String manyExpected = "[fr] There are 42 items in your cart.";
        String manyActual = testData.PluralMessageIntParam(42);
        Assert.assertEquals(manyExpected, manyActual);
        
        String otherExcepted = "[fr] There are 142 items in your cart.";
        String otherActual = testData.PluralMessageIntParam(142);
        Assert.assertEquals(otherExcepted, otherActual);
    }
    
    @Test
    public void testPluralMessageIntParamArabic() throws IOException {
        PluralMessagesTestData testData = Hermes.get(PluralMessagesTestData.class, "ar");
        
        String zeroExpected = "[ar] There are no items in your cart.";
        String zeroActual = testData.PluralMessageIntParam(0);
        Assert.assertEquals(zeroExpected, zeroActual);
        
        String oneExpected = "[ar] There is one item in your cart.";
        String oneActual = testData.PluralMessageIntParam(1);
        Assert.assertEquals(oneExpected, oneActual);
        
        String twoExpected = "[ar] There are two items in your cart.";
        String twoActual = testData.PluralMessageIntParam(2);
        Assert.assertEquals(twoExpected, twoActual);
        
        String fewExpected = "[ar] There are \u0664 items in your cart, which is few.";
        String fewActual = testData.PluralMessageIntParam(4);
        Assert.assertEquals(fewExpected, fewActual);
        
        String manyExpected = "[ar] There are \u0664\u0662 items in your cart, which is many.";
        String manyActual = testData.PluralMessageIntParam(42);
        Assert.assertEquals(manyExpected, manyActual);
        
        String otherExcepted = "[ar] There are \u0661\u0660\u0661 items in your cart.";
        String otherActual = testData.PluralMessageIntParam(101);
        Assert.assertEquals(otherExcepted, otherActual);
    }
    
    @Test
    public void testPluralMessageShortParamEnglish() throws IOException {
        PluralMessagesTestData testData = Hermes.get(PluralMessagesTestData.class, "en_US");
        
        String zeroExpected = "There are 0 items in your cart.";
        String zeroActual = testData.PluralMessageShortParam((short) 0);
        Assert.assertEquals(zeroExpected, zeroActual);
        
        String oneExpected = "There is 1 item in your cart.";
        String oneActual = testData.PluralMessageShortParam((short) 1);
        Assert.assertEquals(oneExpected, oneActual);
        
        String twoExpected = "There are 2 items in your cart.";
        String twoActual = testData.PluralMessageShortParam((short) 2);
        Assert.assertEquals(twoExpected, twoActual);
        
        String fewExpected = "There are 4 items in your cart.";
        String fewActual = testData.PluralMessageShortParam((short) 4);
        Assert.assertEquals(fewExpected, fewActual);
        
        String manyExpected = "There are 42 items in your cart.";
        String manyActual = testData.PluralMessageShortParam((short) 42);
        Assert.assertEquals(manyExpected, manyActual);
        
        String otherExcepted = "There are 142 items in your cart.";
        String otherActual = testData.PluralMessageShortParam((short) 142);
        Assert.assertEquals(otherExcepted, otherActual);
    }
    
    @Test
    public void testPluralMessageShortParamFrench() throws IOException {
        PluralMessagesTestData testData = Hermes.get(PluralMessagesTestData.class, "fr");
        
        String zeroExpected = "[fr] There is 0 item in your cart.";
        String zeroActual = testData.PluralMessageShortParam((short) 0);
        Assert.assertEquals(zeroExpected, zeroActual);
        
        String oneExpected = "[fr] There is 1 item in your cart.";
        String oneActual = testData.PluralMessageShortParam((short) 1);
        Assert.assertEquals(oneExpected, oneActual);
        
        String twoExpected = "[fr] There are 2 items in your cart.";
        String twoActual = testData.PluralMessageShortParam((short) 2);
        Assert.assertEquals(twoExpected, twoActual);
        
        String fewExpected = "[fr] There are 4 items in your cart.";
        String fewActual = testData.PluralMessageShortParam((short) 4);
        Assert.assertEquals(fewExpected, fewActual);
        
        String manyExpected = "[fr] There are 42 items in your cart.";
        String manyActual = testData.PluralMessageShortParam((short) 42);
        Assert.assertEquals(manyExpected, manyActual);
        
        String otherExcepted = "[fr] There are 142 items in your cart.";
        String otherActual = testData.PluralMessageShortParam((short) 142);
        Assert.assertEquals(otherExcepted, otherActual);
    }
    
    @Test
    public void testPluralMessageShortParamArabic() throws IOException {
        PluralMessagesTestData testData = Hermes.get(PluralMessagesTestData.class, "ar");
        
        String zeroExpected = "[ar] There are no items in your cart.";
        String zeroActual = testData.PluralMessageShortParam((short) 0);
        Assert.assertEquals(zeroExpected, zeroActual);
        
        String oneExpected = "[ar] There is one item in your cart.";
        String oneActual = testData.PluralMessageShortParam((short) 1);
        Assert.assertEquals(oneExpected, oneActual);
        
        String twoExpected = "[ar] There are two items in your cart.";
        String twoActual = testData.PluralMessageShortParam((short) 2);
        Assert.assertEquals(twoExpected, twoActual);
        
        String fewExpected = "[ar] There are \u0664 items in your cart, which is few.";
        String fewActual = testData.PluralMessageShortParam((short) 4);
        Assert.assertEquals(fewExpected, fewActual);
        
        String manyExpected = "[ar] There are \u0664\u0662 items in your cart, which is many.";
        String manyActual = testData.PluralMessageShortParam((short) 42);
        Assert.assertEquals(manyExpected, manyActual);
        
        String otherExcepted = "[ar] There are \u0661\u0660\u0661 items in your cart.";
        String otherActual = testData.PluralMessageShortParam((short) 101);
        Assert.assertEquals(otherExcepted, otherActual);
    }
    
    @Test
    public void testDefaultPluralMessagesEnglish() throws IOException {
        PluralMessagesTestData testData = Hermes.get(PluralMessagesTestData.class, "en_US");
        
        String zeroExpected = "You have 0 items in your cart.";
        String zeroActual = testData.PluralMessageDefaultMessages(0);
        Assert.assertEquals(zeroExpected, zeroActual);
        
        String oneExpected = "You have 1 item in your cart.";
        String oneActual = testData.PluralMessageDefaultMessages(1);
        Assert.assertEquals(oneExpected, oneActual);
        
        String twoExpected = "You have 2 items in your cart.";
        String twoActual = testData.PluralMessageDefaultMessages(2);
        Assert.assertEquals(twoExpected, twoActual);
        
        String fewExpected = "You have 4 items in your cart.";
        String fewActual = testData.PluralMessageDefaultMessages(4);
        Assert.assertEquals(fewExpected, fewActual);
        
        String manyExpected = "You have 42 items in your cart.";
        String manyActual = testData.PluralMessageDefaultMessages(42);
        Assert.assertEquals(manyExpected, manyActual);
        
        String otherExcepted = "You have 142 items in your cart.";
        String otherActual = testData.PluralMessageDefaultMessages(142);
        Assert.assertEquals(otherExcepted, otherActual);
    }
    
    @Test
    public void testDefaultPluralMessagesFrench() throws IOException {
        PluralMessagesTestData testData = Hermes.get(PluralMessagesTestData.class, "fr");
        
        String zeroExpected = "You have 0 item in your cart.";
        String zeroActual = testData.PluralMessageDefaultMessages(0);
        Assert.assertEquals(zeroExpected, zeroActual);
        
        String oneExpected = "You have 1 item in your cart.";
        String oneActual = testData.PluralMessageDefaultMessages(1);
        Assert.assertEquals(oneExpected, oneActual);
        
        String twoExpected = "You have 2 items in your cart.";
        String twoActual = testData.PluralMessageDefaultMessages(2);
        Assert.assertEquals(twoExpected, twoActual);
        
        String fewExpected = "You have 4 items in your cart.";
        String fewActual = testData.PluralMessageDefaultMessages(4);
        Assert.assertEquals(fewExpected, fewActual);
        
        String manyExpected = "You have 42 items in your cart.";
        String manyActual = testData.PluralMessageDefaultMessages(42);
        Assert.assertEquals(manyExpected, manyActual);
        
        String otherExcepted = "You have 142 items in your cart.";
        String otherActual = testData.PluralMessageDefaultMessages(142);
        Assert.assertEquals(otherExcepted, otherActual);
    }
    
    @Test
    public void testDefaultPluralMessagesArabic() throws IOException {
        PluralMessagesTestData testData = Hermes.get(PluralMessagesTestData.class, "ar");
        
        String zeroExpected = "You have \u0660 items in your cart.";
        String zeroActual = testData.PluralMessageDefaultMessages(0);
        Assert.assertEquals(zeroExpected, zeroActual);
        
        String oneExpected = "You have \u0661 item in your cart.";
        String oneActual = testData.PluralMessageDefaultMessages(1);
        Assert.assertEquals(oneExpected, oneActual);
        
        String twoExpected = "You have \u0662 items in your cart.";
        String twoActual = testData.PluralMessageDefaultMessages(2);
        Assert.assertEquals(twoExpected, twoActual);
        
        String fewExpected = "You have \u0664 items in your cart, which is few.";
        String fewActual = testData.PluralMessageDefaultMessages(4);
        Assert.assertEquals(fewExpected, fewActual);
        
        String manyExpected = "You have \u0664\u0662 items in your cart, which is many.";
        String manyActual = testData.PluralMessageDefaultMessages(42);
        Assert.assertEquals(manyExpected, manyActual);
        
        String otherExcepted = "You have \u0661\u0660\u0661 items in your cart.";
        String otherActual = testData.PluralMessageDefaultMessages(101);
        Assert.assertEquals(otherExcepted, otherActual);
    }
}
