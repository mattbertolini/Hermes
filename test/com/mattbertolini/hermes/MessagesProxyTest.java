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
    public void testToString() {
        String expected = "com.mattbertolini.hermes.MessagesProxy: com.mattbertolini.hermes.MessagesProxyTestData, lang=en_US";
        String actual = this.testData.toString();
        Assert.assertEquals(expected, actual);
    }
}
