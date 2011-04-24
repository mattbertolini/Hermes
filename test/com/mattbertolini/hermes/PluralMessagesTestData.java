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

import com.google.gwt.i18n.client.Messages;

public interface PluralMessagesTestData extends Messages {
    @DefaultMessage("You have {0,number} items in your cart.")
    @PluralText({"none", "You have {0,number} items in your cart.",
                "one", "You have {0,number} item in your cart.",
                "two", "You have {0,number} items in your cart.",
                "few", "You have {0,number} items in your cart, which is few.",
                "many", "You have {0,number} items in your cart, which is many."})
    String PluralMessageDefaultMessages(@PluralCount int count);
    String PluralMessageIntParam(@PluralCount int count);
    String PluralMessageShortParam(@PluralCount short count);
    
    String PluralMessageWithCustomRule(@PluralCount(CustomPluralRule.class) int count);
    
    @DefaultMessage("You have {0,number} custom items in your cart.")
    @PluralText({"one", "You have one custom item in your cart.",
                 "custom", "You have forty-two custom items in your cart."})
    String PluralMessageCustomRuleDefault(@PluralCount(CustomPluralRule.class) int count);
    
    String PluralMessageWithCustomRuleNoDefaultConstructor(@PluralCount(NoDefaultConstructorPluralRule.class) int count);
}
