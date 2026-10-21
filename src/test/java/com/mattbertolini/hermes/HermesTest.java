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
import java.util.Locale;

import com.google.gwt.i18n.client.Messages;
import com.ibm.icu.util.ULocale;
import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;


public class HermesTest {

    @BeforeClass
    public static void beforeClass() {
        Locale.setDefault(Locale.US);
        ULocale.setDefault(ULocale.US);
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

    @Test
    public void testSubclassInheritsSuperclassProperties() throws IOException {
        SubMessagesProxyTestData testData = Hermes.get(SubMessagesProxyTestData.class, "fr_CA");
        String expected = "[fr] This is a basic message with a key with a different name.";
        String actual = testData.BasicMessageWithKey();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testClassOverridesSuperclassProperties() throws IOException {
        SubMessagesProxyTestData testData = Hermes.get(SubMessagesProxyTestData.class, "fr_CA");
        String expected = "This is a basic message with no key overridden by subclass.";
        String actual = testData.BasicMessageWithNoKey();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testReturnDefaultLocaleProperties() throws IOException {
        SubMessagesProxyTestData testData = Hermes.get(SubMessagesProxyTestData.class, "fr_CA");
        String expected = "This is a subclass message with no key.";
        String actual = testData.SubClassMessageWithNoKey();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testReturnParentLocaleProperties() throws IOException {
        SubMessagesProxyTestData testData = Hermes.get(SubMessagesProxyTestData.class, "fr_CA");
        String expected = "This is a subclass message from French locale.";
        String actual = testData.SubClassMessageForFrLocaleOverride();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testReturnSpecifiedLocaleProperties() throws IOException {
        SubMessagesProxyTestData testData = Hermes.get(SubMessagesProxyTestData.class, "fr_CA");
        String expected = "This is another subclass message from Canadian French locale.";
        String actual = testData.SubClassMessageForFrCALocaleOverride();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testReturnPropertyFromSecondParentInterface() throws IOException {
        SubMessagesProxyTestData testData = Hermes.get(SubMessagesProxyTestData.class, "en_US");
        String expected = "This is a basic message with no key from another parent interface.";
        String actual = testData.OtherParentMessageWithNoKey();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testMessagesWithoutPropertiesFileDoesNotFail() throws IOException {
        NoPropertiesFileParentTestData testData = Hermes.get(NoPropertiesFileParentTestData.class, "en_US");
        Assert.assertEquals("Parent message from annotation.", testData.ParentMessageFromAnnotation());
    }

    @Test
    public void testParentWithoutPropertiesFileDoesNotFail() throws IOException {
        ChildOfNoPropertiesFileParentTestData testData = Hermes.get(ChildOfNoPropertiesFileParentTestData.class, "en_US");
        Assert.assertEquals("Child message from properties file.", testData.ChildMessage());
        Assert.assertEquals("Parent message from annotation.", testData.ParentMessageFromAnnotation());
    }

    @Test
    public void testParentWithoutPropertiesFileUsesAnnotationDefault() throws IOException {
        ChildOfNoPropertiesFileParentTestData testData = Hermes.get(ChildOfNoPropertiesFileParentTestData.class, "en_US");
        Assert.assertEquals("Parent message from annotation.", testData.ParentMessageFromAnnotation());
    }

    @Test
    public void testNestedInterfaceLoadsBinaryNamedPropertiesFile() throws IOException {
        NestedMessagesTestData testData = Hermes.get(NestedMessagesTestData.class, "en_US");
        Assert.assertEquals("Nested interface message.", testData.NestedMessage());
    }

    public interface NestedMessagesTestData extends Messages {
        String NestedMessage();
    }
}
