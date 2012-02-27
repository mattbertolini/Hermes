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

import java.util.Map;

import com.google.gwt.i18n.client.Constants;

public interface ConstantsProxyTestData extends Constants {
    String StringConstant();
    @DefaultStringValue("Default string")
    String StringConstantDefaultValue();
    @Key("DifferentKeyStringConstant")
    String StringContantWithDifferentKey();
    
    int IntConstant();
    @DefaultIntValue(24)
    int IntConstantDefaultValue();
    
    float FloatConstant();
    @DefaultFloatValue(2.4f)
    float FloatConstantDefaultValue();
    
    double DoubleConstant();
    @DefaultDoubleValue(24.24d)
    double DoubleConstantDefaultValue();
    
    boolean BooleanConstant();
    @DefaultBooleanValue(false)
    boolean BooleanConstantDefaultValue();
    
    String[] StringArrayConstant();
    @DefaultStringArrayValue({"Default1", "Default2"})
    String[] StringArrayConstantDefaultValue();
    
    Map<String, String> MapConstant();
    @DefaultStringMapValue({"DefaultKey1", "DefaultValue1", "DefaultKey2", "DefaultValue2"})
    Map<String, String> MapConstantDefaultValue();
}
