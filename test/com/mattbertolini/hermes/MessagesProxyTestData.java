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
import java.util.Date;

import com.google.gwt.i18n.client.Messages;
import com.google.gwt.safehtml.shared.SafeHtml;

public interface MessagesProxyTestData extends Messages {
    String BasicMessageWithNoKey();
    
    @Key("BasicMessageWithDifferentKeyName")
    String BasicMessageWithKey();
    
    @DefaultMessage("The default message for a basic message test.")
    String BasicMessageCheckDefaultMessage();
    
    String BasicMessageWithOneArgument(String argument);
    String MessageWithNumberFormattedArgument(double number);
    String MessageWithCurrencyFormattedArgument(BigDecimal money);
    String MessageWithDateFormattedArgument(Date date);
    String MessageWithCustomDateFormattedArgument(Date date);
    String MessageWithEscapeCharacter();
    String MessageWithEscapeCharacterInArgument(String argument);
    String MessageWithSpecialCharacter();
    String MessageWithSpecialCharacterInArgument(String argument);
    SafeHtml BasicHtmlMessage(String linkText);
    SafeHtml HtmlMessageWithUnsafeArgument(String unsafeText);
    SafeHtml HtmlMessageWithSafeHtmlArgument(SafeHtml safeHtml);
    String MessageWithNoKey();
    String MessageWithEnumSelect(@Select SelectEnum arg);
    String MessageWithStringSelect(@Select String arg);
    String MessageWithIntSelect(@Select int arg);
    String MessageWithLongSelect(@Select long arg);
    String MessageWithFloatSelect(@Select float arg);
    String MessageWithShortSelect(@Select short arg);
    String MessageWithDoubleSelect(@Select double arg);
}
