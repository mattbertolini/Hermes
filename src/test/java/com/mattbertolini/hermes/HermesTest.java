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

import java.io.FileNotFoundException;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class HermesTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNullClassArgument() throws IOException {
        Hermes.get(null, "en_US");
    }
    
    @Test
    public void testEmptyStringLangArgument() throws IOException {
        MessagesProxyTestData proxy = Hermes.get(MessagesProxyTestData.class, "");
        Assert.assertNotNull(proxy);
    }
    
    @Test
    public void testNullLangArgument() throws IOException {
        MessagesProxyTestData proxy = Hermes.get(MessagesProxyTestData.class, null);
        Assert.assertNotNull(proxy);
    }
    
    @Test(expected = FileNotFoundException.class)
    public void testPropertiesFileNotFound() throws IOException {
        Hermes.get(PropertiesFileNotFound.class, "en_US");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testClassNoImplementInterface() throws IOException {
        Hermes.get(NoI18nInterface.class, "en_US");
    }
    
    @Test
    public void testEncodingExceptionNotThrown() throws IOException {
        try {
            Hermes.get(MessagesProxyTestData.class, "en_US");
        } catch(RuntimeException e) {
            Assert.fail("A runtime exception was thrown. Encoding is broken.");
        }
    }
    
    @Test
    public void testMessageWithPrivateUseTag() throws IOException {
        MessagesProxyTestData testData = Hermes.get(MessagesProxyTestData.class, "en_US_X_PRIV");
        String expected = "This is loaded from the private extension file.";
        String actual = testData.BasicMessageWithNoKey();
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testFallbackFileLoading() throws IOException {
        MessagesProxyTestData testData = Hermes.get(MessagesProxyTestData.class, "fr_CA");
        String expected = "[fr] This is a basic message with a key with a different name.";
        String actual = testData.BasicMessageWithKey();
        Assert.assertEquals(expected, actual);
    }
}
