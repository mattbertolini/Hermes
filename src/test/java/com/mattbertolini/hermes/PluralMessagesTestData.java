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

import java.util.List;

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
    
    @DefaultMessage("You have {0,number} counted items in your cart.")
    @AlternateMessage({"one", "You have one counted item in your cart."})
    String PluralMessageWithCountingRule(@PluralCount(CountingPluralRule.class) int count);
    
    String PluralMessageMultiplePluralCount(@PluralCount int count1, @PluralCount int count2);
    
    @DefaultMessage("You have {0} messages and {1} notifications.")
    @AlternateMessage({"none|none", "You have no messages or notifications.",
                       "none|one", "You have a notification.",
                       "one|none", "You have a message.",
                       "one|one", "You have one message and one notification.",
                       "other|one", "You have {0} messages and one notification.",
                       "one|other", "You have one message and {1} notifications."})
    String PluralMessageMultiplePluralCountDefault(@PluralCount int count1, @PluralCount int count2);

    @DefaultMessage("You have {0} items.")
    @AlternateMessage({"one", "You have one item."})
    String PluralMessageArrayParam(@PluralCount String[] items);

    @DefaultMessage("You have {0} items.")
    @AlternateMessage({"one", "You have one item."})
    String PluralMessageListParam(@PluralCount List<String> items);

    // Example from the GWT @Offset javadoc
    @DefaultMessage("{0}, {1} and {2,number} others have recommended this movie")
    @AlternateMessage({
            "=0", "No one has recommended this movie",
            "=1", "{0} has recommended this movie",
            "=2", "{0} and {1} have recommended this movie",
            "one", "{0}, {1} and one other have recommended this movie"})
    String PluralMessageWithOffset(@Optional String rec1, @Optional String rec2, @PluralCount @Offset(2) int count);

    String PluralMessageExactValue(@PluralCount int count);

    // Intentionally invalid: GWT only allows int, short, arrays and List for @PluralCount.
    @DefaultMessage("Unused.")
    String PluralMessageLongParam(@PluralCount long count);
}
