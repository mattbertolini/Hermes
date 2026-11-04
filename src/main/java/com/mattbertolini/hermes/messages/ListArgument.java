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

package com.mattbertolini.hermes.messages;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.ibm.icu.text.ListFormatter;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.MessagePattern;
import com.ibm.icu.util.ULocale;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

final class ListArgument implements ExtraArgument {
    private final int index;
    private final MessageFormat elementFormat;
    private final ListFormatter listFormatter;   // Only built for patterns that have a list

    ListArgument(int index, String elementStyle, ULocale locale) {
        this.index = index;
        this.listFormatter = ListFormatter.getInstance(locale);
        this.elementFormat = new MessageFormat("", locale);
        this.elementFormat.applyPattern(elementStyle == null ? "{0}" : "{0," + elementStyle + "}",
                MessagePattern.ApostropheMode.DOUBLE_REQUIRED);
    }

    @Override
    public Object format(Object[] args, boolean safeHtml) {
        List<String> items = new ArrayList<>();
        for (Object element : elements(args[index])) {
            MessageFormat formatter = (MessageFormat) elementFormat.clone();
            items.add(formatter.format(new Object[]{safeHtml ? CompiledMessage.escape(element) : element}));
        }
        String list = listFormatter.format(items);
        return safeHtml ? SafeHtmlUtils.fromTrustedString(list) : list;
    }

    private List<?> elements(Object arg) {
        if (arg == null) {
            return Collections.emptyList();
        }
        if (arg instanceof List) {
            return (List<?>) arg;
        }
        if (arg.getClass().isArray()) {
            return IntStream.range(0, Array.getLength(arg)).mapToObj(i -> Array.get(arg, i)).collect(toList());
        }
        throw new IllegalArgumentException("Parameters formatted as lists must be declared as java.util.List "
                + "or arrays but argument " + index + " was " + arg.getClass().getName());
    }
}
