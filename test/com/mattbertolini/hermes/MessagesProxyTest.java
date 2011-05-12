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

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

public class MessagesProxyTest {
    private static final TimeZone TEST_TIME_ZONE = TimeZone.getTimeZone("America/New_York");
    
    private MessagesProxyTestData testData;
    private TimeZone originalTimeZone;
    
    @Before
    public void setUp() throws Exception {
        this.testData = Hermes.get(MessagesProxyTestData.class, "en_US");
        this.originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TEST_TIME_ZONE);
    }

    @After
    public void tearDown() throws Exception {
        TimeZone.setDefault(this.originalTimeZone);
    }
    
    @Test
    public void testBasicMessageWithNoKeyTest() {
        String expected = "This is a basic message with no key.";
        String actual = this.testData.BasicMessageWithNoKey();
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testBasicMessageWithKey() {
        String expected = "This is a basic message with a key with a different name.";
        String actual = this.testData.BasicMessageWithKey();
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testBasicMessageCheckDefaultMessage() {
        String expected = "The default message for a basic message test.";
        String actual = this.testData.BasicMessageCheckDefaultMessage();
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testBasicMessageWithOneArgument() {
        String expected = "This is a basic message with one argument. The argument value is Hello.";
        String actual = this.testData.BasicMessageWithOneArgument("Hello");
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testMessageWithEscapeCharacter() {
        String expected = "Where's the beef?";
        String actual = this.testData.MessageWithEscapeCharacter();
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testMessageWithEscapeCharacterInArgument() {
        String expected = "Your name is Tim O'Rielly.";
        String actual = this.testData.MessageWithEscapeCharacterInArgument("Tim O'Rielly");
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testMessageWithSpecialCharacter() {
        String expected = "I see the $ sign.";
        String actual = this.testData.MessageWithSpecialCharacter();
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testMessageWithSpecialCharacterInArgument() {
        String expected = "Your total is US$123.45.";
        String actual = this.testData.MessageWithSpecialCharacterInArgument("US$123.45");
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testNumberFormattedArgument() {
        String expected = "This is a message with a number formatted based on the locale. The number is 12,345.67.";
        String actual = this.testData.MessageWithNumberFormattedArgument(12345.67d);
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testMessageWithCurrencyFormattedArgument() {
        String expected = "Default currency format for locale. The number is $9,876.54.";
        BigDecimal bd = BigDecimal.valueOf(9876.54d);
        String actual = this.testData.MessageWithCurrencyFormattedArgument(bd);
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testMessageWithDateFormattedArgument() {
        String expected = "Default date format for locale. The date is Saturday, March 26, 2011.";
        Calendar cal = Calendar.getInstance(TEST_TIME_ZONE);
        cal.set(Calendar.YEAR, 2011);
        cal.set(Calendar.MONTH, Calendar.MARCH);
        cal.set(Calendar.DAY_OF_MONTH, 26);
        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 25);
        cal.set(Calendar.SECOND, 0);
        String actual = this.testData.MessageWithDateFormattedArgument(cal.getTime());
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testMessageWithCustomDateFormattedArgument() {
        String expected = "Custom date format. The date is 2011-03-26 21:25:00.";
        Calendar cal = Calendar.getInstance(TEST_TIME_ZONE);
        cal.set(Calendar.YEAR, 2011);
        cal.set(Calendar.MONTH, Calendar.MARCH);
        cal.set(Calendar.DAY_OF_MONTH, 26);
        cal.set(Calendar.HOUR_OF_DAY, 21);
        cal.set(Calendar.MINUTE, 25);
        cal.set(Calendar.SECOND, 0);
        String actual = this.testData.MessageWithCustomDateFormattedArgument(cal.getTime());
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testBasicHtmlMessage() {
        String expectedStr = "<a href=\"http://www.google.com/\">Hello World</a>";
        SafeHtml expected = SafeHtmlUtils.fromTrustedString(expectedStr);
        SafeHtml actual = this.testData.BasicHtmlMessage("Hello World");
        Assert.assertEquals(expected.asString(), actual.asString());
    }
    
    @Test
    public void testHtmlMessageWithUnsafeArgument() {
        String expectedStr = "<a href=\"http://www.google.com/\">&lt;script&gt;alert(&quot;Hello World&quot;);&lt;/script&gt;</a>";
        SafeHtml expected = SafeHtmlUtils.fromTrustedString(expectedStr);
        SafeHtml actual = this.testData.HtmlMessageWithUnsafeArgument("<script>alert(\"Hello World\");</script>");
        Assert.assertEquals(expected.asString(), actual.asString());
    }
    
    @Test
    public void testHtmlMessageWithSafeHtmlArgument() {
        String expectedStr = "<div id=\"test\"><p>Hello World</p></div>";
        SafeHtml expected = SafeHtmlUtils.fromTrustedString(expectedStr);
        SafeHtml actual = this.testData.HtmlMessageWithSafeHtmlArgument(SafeHtmlUtils.fromTrustedString("<p>Hello World</p>"));
        Assert.assertEquals(expected.asString(), actual.asString());
    }
    
    @Test(expected = RuntimeException.class)
    public void testNoKeyFound() {
        this.testData.MessageWithNoKey();
    }
    
    @Test
    public void testMessageWithEnumSelect() {
        String maleExpected = "He is not here.";
        String maleActual = this.testData.MessageWithEnumSelect(SelectEnum.MALE);
        Assert.assertEquals(maleExpected, maleActual);
        
        String femaleExpected = "She is not here.";
        String femaleActual = this.testData.MessageWithEnumSelect(SelectEnum.FEMALE);
        Assert.assertEquals(femaleExpected, femaleActual);
        
        String otherExpected = "It is not here.";
        String otherActual = this.testData.MessageWithEnumSelect(null);
        Assert.assertEquals(otherExpected, otherActual);
    }
    
    @Test
    public void testMessageWithStringSelect() {
        String helloExpected = "This is the hello string.";
        String helloActual = this.testData.MessageWithStringSelect("hello");
        Assert.assertEquals(helloExpected, helloActual);
        
        String worldExpected = "This is the world string.";
        String worldActual = this.testData.MessageWithStringSelect("world");
        Assert.assertEquals(worldExpected, worldActual);
        
        String defaultExpected = "This is the default string.";
        String defaultActual = this.testData.MessageWithStringSelect(null);
        Assert.assertEquals(defaultExpected, defaultActual);
    }
    
    @Test
    public void testMessageWithIntSelect() {
        String fortyTwoExpected = "This is the int 42 string.";
        String fortyTwoActual = this.testData.MessageWithIntSelect(42);
        Assert.assertEquals(fortyTwoExpected, fortyTwoActual);
        
        String oneTwentyFourExpected = "This is the int 124 string.";
        String oneTwentyFourActual = this.testData.MessageWithIntSelect(124);
        Assert.assertEquals(oneTwentyFourExpected, oneTwentyFourActual);
        
        String defaultExpected = "This is the int default string.";
        String defaultActual = this.testData.MessageWithIntSelect(24);
        Assert.assertEquals(defaultExpected, defaultActual);
    }
    
    @Test
    public void testMessageWithLongSelect() {
        String fortyTwoExpected = "This is the long 42 string.";
        String fortyTwoActual = this.testData.MessageWithLongSelect(42l);
        Assert.assertEquals(fortyTwoExpected, fortyTwoActual);
        
        String oneTwentyFourExpected = "This is the long 124 string.";
        String oneTwentyFourActual = this.testData.MessageWithLongSelect(124l);
        Assert.assertEquals(oneTwentyFourExpected, oneTwentyFourActual);
        
        String defaultExpected = "This is the long default string.";
        String defaultActual = this.testData.MessageWithLongSelect(24l);
        Assert.assertEquals(defaultExpected, defaultActual);
    }
    
    @Test
    public void testMessageWithFloatSelect() {
        String fourPointTwoExpected = "This is the float 4.2 string.";
        String fourPointTwoActual = this.testData.MessageWithFloatSelect(4.2f);
        Assert.assertEquals(fourPointTwoExpected, fourPointTwoActual);
        
        String twelvePointFourExpected = "This is the float 12.4 string.";
        String twelvePointFourActual = this.testData.MessageWithFloatSelect(12.4f);
        Assert.assertEquals(twelvePointFourExpected, twelvePointFourActual);
        
        String defaultExpected = "This is the float default string.";
        String defaultActual = this.testData.MessageWithFloatSelect(21.2f);
        Assert.assertEquals(defaultExpected, defaultActual);
    }
    
    @Test
    public void testMessageWithShortSelect() {
        String oneExpected = "This is the short 1 string.";
        String oneActual = this.testData.MessageWithShortSelect((short)1);
        Assert.assertEquals(oneExpected, oneActual);
        
        String twoExpected = "This is the short 2 string.";
        String twoActual = this.testData.MessageWithShortSelect((short)2);
        Assert.assertEquals(twoExpected, twoActual);
        
        String defaultExpected = "This is the short default string.";
        String defaultActual = this.testData.MessageWithShortSelect((short)3);
        Assert.assertEquals(defaultExpected, defaultActual);
    }
    
    @Test
    public void testMessageWithDoubleSelect() {
        String fourPointTwoExpected = "This is the double 4.2 string.";
        String fourPointTwoActual = this.testData.MessageWithDoubleSelect(4.2d);
        Assert.assertEquals(fourPointTwoExpected, fourPointTwoActual);
        
        String twelvePointFourExpected = "This is the double 12.4 string.";
        String twelvePointFourActual = this.testData.MessageWithDoubleSelect(12.4d);
        Assert.assertEquals(twelvePointFourExpected, twelvePointFourActual);
        
        String defaultExpected = "This is the double default string.";
        String defaultActual = this.testData.MessageWithDoubleSelect(21.2d);
        Assert.assertEquals(defaultExpected, defaultActual);
    }
    
    @Test
    public void testToString() {
        String expected = "com.mattbertolini.hermes.MessagesProxy: com.mattbertolini.hermes.MessagesProxyTestData, lang=en_US";
        String actual = this.testData.toString();
        Assert.assertEquals(expected, actual);
    }
}
