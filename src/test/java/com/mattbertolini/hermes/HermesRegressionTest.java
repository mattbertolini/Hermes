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

import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;

import com.google.gwt.i18n.client.Messages;

public class HermesRegressionTest {

    public interface NestedMessagesTestData extends Messages {
        String NestedMessage();
    }

    @Test
    public void testParentWithoutPropertiesFileDoesNotFail() throws IOException {
        ChildOfNoPropertiesFileParentTestData testData = Hermes.get(ChildOfNoPropertiesFileParentTestData.class, "en_US");
        Assert.assertEquals("Child message from properties file.", testData.ChildMessage());
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
}
